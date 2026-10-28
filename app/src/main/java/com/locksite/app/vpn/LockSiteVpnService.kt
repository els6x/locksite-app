package com.locksite.app.vpn

import android.app.PendingIntent
import android.content.Intent
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
            controller.publish(ProtectionStatus.ERROR, getString(R.string.vpn_error_tunnel))
            stopSelf()
            return
        }

        tunnel = descriptor
        observeRules()
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
                    getString(R.string.vpn_error_blocklist),
                )
            }
        }
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
        container.vpnController.publish(
            ProtectionStatus.STOPPED,
            getString(R.string.vpn_error_revoked),
        )
        stopSelf()
    }

    override fun onDestroy() {
        // Closing the descriptor is what unblocks the read loop; skip it when the loop has
        // already closed its own streams, so the same fd is never closed twice.
        if (!loopFinished) {
            runCatching { tunnel?.close() }
        }
        readJob?.cancel()
        rulesJob?.cancel()
        startJob?.cancel()
        tunnel = null

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
