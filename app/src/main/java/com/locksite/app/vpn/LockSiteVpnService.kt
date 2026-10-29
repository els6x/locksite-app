package com.locksite.app.vpn

import android.app.PendingIntent
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.VpnService
import android.os.ParcelFileDescriptor
import com.locksite.app.LockSiteApplication
import com.locksite.app.MainActivity
import com.locksite.app.R
import com.locksite.app.dns.DnsQuery
import com.locksite.app.dns.DnsQueryHandler
import com.locksite.app.dns.DnsResponses
import com.locksite.app.dns.IpPackets
import com.locksite.app.dns.SocketProtector
import com.locksite.app.dns.UdpDatagram
import com.locksite.app.dns.UdpDnsForwarder
import com.locksite.app.domain.model.LockedDomain
import com.locksite.app.domain.model.ProtectionStatus
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock

/**
 * The local DNS filter (PRD section 4.3).
 *
 * Lifecycle: created by an explicit user action, establishes a TUN interface that only
 * carries traffic for the two virtual DNS addresses, then answers DNS queries either
 * locally (locked names) or by relaying them upstream (everything else).
 *
 * Deliberately not a catch-all VPN: see [VpnConfig] for why no default route is added.
 */
class LockSiteVpnService : VpnService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val rules = AtomicReference<List<LockedDomain>>(emptyList())
    private val writeLock = Mutex()
    private val inFlight = Semaphore(VpnConfig.MAX_CONCURRENT_QUERIES)

    @Volatile
    private var tunnel: ParcelFileDescriptor? = null

    /** Set once the read loop has closed its streams, so shutdown does not close twice. */
    @Volatile
    private var loopFinished = false

    /** True once the system took the VPN away; the resulting read failure is expected. */
    @Volatile
    private var revoked = false

    /** False when the blocklist could not be read: filtering cannot be called "active". */
    @Volatile
    private var rulesHealthy = false

    private var startJob: Job? = null
    private var readJob: Job? = null
    private var rulesJob: Job? = null

    /** The physical network the tunnel sits on; `null` means "platform default". */
    @Volatile
    private var underlyingNetwork: Network? = null

    /** Guards [networkCallback] and [destroyed] against the register/destroy race. */
    private val callbackLock = Any()

    @Volatile
    private var destroyed = false

    @Volatile
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private val handler = DnsQueryHandler { rules.get() }

    private val forwarder: UdpDnsForwarder by lazy {
        UdpDnsForwarder(
            upstreams = VpnConfig.UPSTREAM_RESOLVERS.mapNotNull { address ->
                // Literal addresses only, so this cannot recurse into DNS.
                runCatching { InetAddress.getByName(address) }.getOrNull()
            },
            protector = SocketProtector { socket -> protect(socket) },
            timeoutMillis = VpnConfig.UPSTREAM_TIMEOUT_MS,
        )
    }

    private val ourDnsAddresses: List<ByteArray> by lazy {
        listOf(
            InetAddress.getByName(VpnConfig.IPV4_DNS).address,
            InetAddress.getByName(VpnConfig.IPV6_DNS).address,
        )
    }

    private val container get() = (application as LockSiteApplication).container

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (startJob?.isActive != true) {
            startJob = serviceScope.launch { startTunnel() }
        }
        // A restarted service has no established tunnel and no user gesture behind it;
        // do not silently re-claim protection. M4 revisits boot behaviour.
        return START_NOT_STICKY
    }

    private suspend fun startTunnel() {
        val controller = container.vpnController

        if (tunnel != null) {
            // Already filtering. Re-publish the real state: the caller has just set
            // PERMISSION_PENDING, and leaving it there would hang the UI forever.
            controller.setOwnTunnelUp(true)
            controller.publish(currentStatus(), currentDetail())
            return
        }

        // A blocklist that cannot be read is an error, never "empty and therefore safe".
        val snapshot = try {
            container.blocklistRepository.snapshot()
        } catch (error: Exception) {
            controller.publish(ProtectionStatus.ERROR, getString(R.string.vpn_error_blocklist))
            stopSelf()
            return
        }
        rules.set(snapshot)
        rulesHealthy = true

        val descriptor = try {
            buildTunnel().establish()
        } catch (security: SecurityException) {
            // Consent was withdrawn between the dialog and this call.
            controller.publish(
                ProtectionStatus.NOT_CONFIGURED,
                getString(R.string.vpn_error_permission),
            )
            stopSelf()
            return
        } catch (error: Exception) {
            null
        }

        if (descriptor == null) {
            // Android allows only one VPN. Saying "could not create the interface" would
            // hide the real cause and send the user hunting for a bug that is not there.
            if (controller.hasForeignVpn()) {
                controller.publish(
                    ProtectionStatus.CONFLICT_WITH_OTHER_VPN,
                    getString(R.string.vpn_error_conflict),
                )
            } else {
                controller.publish(ProtectionStatus.ERROR, getString(R.string.vpn_error_tunnel))
            }
            stopSelf()
            return
        }

        tunnel = descriptor
        controller.setOwnTunnelUp(true)
        observeRules()
        registerNetworkCallback()
        // Seed the field with what the builder already declared, and retry if it failed.
        refreshUnderlyingNetwork()
        readJob = serviceScope.launch { readLoop(descriptor) }
        // Published only once the read loop actually exists.
        controller.publish(ProtectionStatus.ACTIVE)
    }

    private fun buildTunnel(): Builder = Builder()
        .setSession(getString(R.string.app_name))
        .setMtu(VpnConfig.MTU)
        .setBlocking(true)
        .addAddress(VpnConfig.IPV4_INTERFACE, VpnConfig.IPV4_INTERFACE_PREFIX)
        .addDnsServer(VpnConfig.IPV4_DNS)
        .addRoute(VpnConfig.IPV4_DNS, VpnConfig.IPV4_DNS_PREFIX)
        .addAddress(VpnConfig.IPV6_INTERFACE, VpnConfig.IPV6_INTERFACE_PREFIX)
        .addDnsServer(VpnConfig.IPV6_DNS)
        .addRoute(VpnConfig.IPV6_DNS, VpnConfig.IPV6_DNS_PREFIX)
        .apply {
            // Apps that treat a DNS-only tunnel as "offline" are excluded entirely; they
            // then use the physical network (and its DNS), so they are not filtered.
            VpnConfig.COMPATIBILITY_BYPASS_PACKAGES.forEach { packageName ->
                runCatching { addDisallowedApplication(packageName) }
            }
            // Declare the physical network underneath the tunnel so Android can carry its
            // validated state over to the VPN network (ADR 0002, M4).
            physicalUnderlyingNetwork()?.let { setUnderlyingNetworks(arrayOf(it)) }
            // Tapping the system VPN notification should land somewhere useful.
            setConfigureIntent(
                PendingIntent.getActivity(
                    this@LockSiteVpnService,
                    0,
                    Intent(this@LockSiteVpnService, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
        }

    /** Keeps the in-memory rule snapshot in step with the database without polling. */
    private fun observeRules() {
        rulesJob = serviceScope.launch {
            try {
                container.blocklistRepository.observeAll().collect { domains ->
                    rules.set(domains)
                    rulesHealthy = true
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                // The tunnel keeps answering from the last good snapshot, but the UI is
                // told something is wrong rather than being shown a comforting "active".
                rulesHealthy = false
                container.vpnController.publish(
                    ProtectionStatus.ERROR,
                    getString(R.string.vpn_error_rules_stale),
                )
            }
        }
    }

    /**
     * Keeps the declared underlying network in step with the physical connection.
     *
     * Without this the tunnel would stay pinned to whichever network was active at start:
     * switching between Wi-Fi and mobile data would leave Android validating the VPN against
     * a dead link, and apps that gate on `NET_CAPABILITY_VALIDATED` would show "offline".
     *
     * Registration is best-effort. If it fails, the network declared at `establish()` time
     * still applies; only live re-evaluation is lost.
     */
    private fun registerNetworkCallback() {
        if (destroyed) return
        val manager = getSystemService(ConnectivityManager::class.java) ?: return
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = safeRefreshUnderlying()

            override fun onLost(network: Network) = safeRefreshUnderlying()

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                // Our own tunnel reports capability changes too; ignore them.
                if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) return
                // Capability updates arrive often (signal, bandwidth). Only re-evaluate when
                // the current underlying link changed or a network just became validated.
                val validated = networkCapabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_VALIDATED,
                )
                if (network != underlyingNetwork && !validated) return
                safeRefreshUnderlying()
            }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
            .build()
        if (runCatching { manager.registerNetworkCallback(request, callback) }.isFailure) return
        synchronized(callbackLock) {
            if (destroyed) {
                // onDestroy ran while we were registering: undo it now instead of leaking.
                runCatching { manager.unregisterNetworkCallback(callback) }
                return
            }
            networkCallback = callback
        }
    }

    /** A callback body must never escape into the Connectivity thread. */
    private fun safeRefreshUnderlying() {
        if (destroyed) return
        runCatching { refreshUnderlyingNetwork() }
    }

    private fun refreshUnderlyingNetwork() {
        val next = physicalUnderlyingNetwork()
        if (next == underlyingNetwork) return
        val applied = runCatching { setUnderlyingNetworks(next?.let { arrayOf(it) }) }.isSuccess
        // Commit only on success: otherwise the next callback would see the new value as
        // already applied and never retry.
        underlyingNetwork = if (applied) next else null
    }

    /**
     * The best physical network to declare as the tunnel's underlying network, or `null`
     * when none is available. Our own VPN transport is never returned.
     *
     * `ConnectivityManager.allNetworks` is deprecated in favour of callback tracking, but it
     * is the only synchronous way to seed the choice at `establish()` time; the callback
     * registered afterwards keeps it current.
     */
    @Suppress("DEPRECATION")
    private fun physicalUnderlyingNetwork(): Network? {
        val manager = getSystemService(ConnectivityManager::class.java) ?: return null
        val active = manager.activeNetwork
        val entries = manager.allNetworks.mapNotNull { network ->
            val capabilities = manager.getNetworkCapabilities(network) ?: return@mapNotNull null
            UnderlyingCandidate(
                id = network.hashCode(),
                isVpn = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN),
                hasInternet = capabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_INTERNET,
                ),
                isValidated = capabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_VALIDATED,
                ),
                isActive = network == active,
            ) to network
        }
        val chosen = selectUnderlying(entries.map { it.first }) ?: return null
        // Identity, not data-class equality: two networks could theoretically share fields.
        return entries.firstOrNull { it.first === chosen }?.second
    }

    private suspend fun readLoop(descriptor: ParcelFileDescriptor) {
        val job = currentCoroutineContext()
        val input = FileInputStream(descriptor.fileDescriptor)
        val output = FileOutputStream(descriptor.fileDescriptor)
        val buffer = ByteArray(VpnConfig.MTU)

        try {
            while (job.isActive) {
                val length = input.read(buffer)
                if (length <= 0) break          // EOF: the interface is gone, stop spinning
                val packet = buffer.copyOf(length)

                // Bounded concurrency. The permit is taken *before* dispatching, so a slow
                // resolver applies backpressure here instead of queueing without limit.
                inFlight.acquire()
                serviceScope.launch {
                    try {
                        handlePacket(packet, output)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        // One malformed packet must never take the tunnel down.
                    } finally {
                        inFlight.release()
                    }
                }
            }
        } catch (closed: IOException) {
            // Reading a closed tunnel throws; that is a normal shutdown path.
        } finally {
            loopFinished = true
            runCatching { input.close() }
            runCatching { output.close() }
        }

        // Reaching this point while still active means the tunnel died without us asking.
        // Staying silent here would leave the UI showing "active" over a dead filter.
        if (job.isActive && !revoked) {
            container.vpnController.setOwnTunnelUp(false)
            container.vpnController.publish(
                ProtectionStatus.ERROR,
                getString(R.string.vpn_error_tunnel_lost),
            )
            stopSelf()
        }
    }

    private suspend fun handlePacket(packet: ByteArray, output: FileOutputStream) {
        val datagram = IpPackets.parseUdp(packet) ?: return
        if (datagram.destinationPort != VpnConfig.DNS_PORT) return
        if (!isOurDnsAddress(datagram.destinationAddress)) return

        val answer = when (val decision = handler.handle(datagram.payload)) {
            is DnsQueryHandler.Decision.Block -> decision.response
            is DnsQueryHandler.Decision.Reject -> decision.response
            is DnsQueryHandler.Decision.Drop -> return
            is DnsQueryHandler.Decision.Forward -> forward(decision.query)
        }

        val reply = IpPackets.buildUdp(
            UdpDatagram(
                sourceAddress = datagram.destinationAddress,
                destinationAddress = datagram.sourceAddress,
                sourcePort = datagram.destinationPort,
                destinationPort = datagram.sourcePort,
                payload = answer,
            ),
        )
        // A failed write is per-packet: the tunnel stays up and the client retries.
        writeLock.withLock { runCatching { output.write(reply) } }
    }

    private fun forward(query: DnsQuery): ByteArray {
        val upstream = forwarder.forward(query.raw)
            ?: return DnsResponses.serverFailure(query)
        // Anything larger than one tunnel packet cannot be delivered over UDP.
        return if (upstream.size > VpnConfig.MAX_DNS_PAYLOAD) {
            DnsResponses.truncated(query)
        } else {
            upstream
        }
    }

    private fun isOurDnsAddress(address: ByteArray): Boolean =
        ourDnsAddresses.any { it.contentEquals(address) }

    private fun currentStatus(): ProtectionStatus = when {
        tunnel == null -> ProtectionStatus.NOT_CONFIGURED
        rulesHealthy -> ProtectionStatus.ACTIVE
        else -> ProtectionStatus.ERROR
    }

    private fun currentDetail(): String? = when {
        tunnel == null -> null
        rulesHealthy -> null
        else -> getString(R.string.vpn_error_blocklist)
    }

    /** The user turned the VPN off from Android settings, or another VPN took over. */
    override fun onRevoke() {
        revoked = true
        container.vpnController.setOwnTunnelUp(false)
        container.vpnController.publish(
            ProtectionStatus.STOPPED,
            getString(R.string.vpn_error_revoked),
        )
        stopSelf()
    }

    override fun onDestroy() {
        destroyed = true
        // Closing the descriptor is what unblocks the read loop; skip it when the loop has
        // already closed its own streams, so the same fd is never closed twice.
        if (!loopFinished) {
            runCatching { tunnel?.close() }
        }
        synchronized(callbackLock) {
            networkCallback?.let { callback ->
                runCatching {
                    getSystemService(ConnectivityManager::class.java)
                        ?.unregisterNetworkCallback(callback)
                }
            }
            networkCallback = null
        }
        readJob?.cancel()
        rulesJob?.cancel()
        startJob?.cancel()
        tunnel = null
        container.vpnController.setOwnTunnelUp(false)

        // A service that died while "starting" must not leave the UI waiting forever.
        val status = container.vpnController.status.value
        if (status == ProtectionStatus.ACTIVE) {
            container.vpnController.publish(ProtectionStatus.STOPPED)
        } else if (status == ProtectionStatus.PERMISSION_PENDING) {
            container.vpnController.publish(ProtectionStatus.NOT_CONFIGURED)
        }

        serviceScope.cancel()
        super.onDestroy()
    }
}
