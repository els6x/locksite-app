package com.locksite.app.vpn

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.VpnService
import com.locksite.app.R
import com.locksite.app.domain.model.ProtectionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The single place that knows whether protection is really running.
 *
 * The status is pushed by [LockSiteVpnService] from its own lifecycle, so the UI can only
 * ever display a state the service actually reached. Nothing here is persisted: a stored
 * boolean would survive a crash and claim protection that is not there (PRD FR-023).
 */
class VpnController(private val context: Context) {

    private val _status = MutableStateFlow(ProtectionStatus.NOT_CONFIGURED)
    val status: StateFlow<ProtectionStatus> = _status.asStateFlow()

    /** Human-readable reason for the current state, shown only when it adds information. */
    private val _detail = MutableStateFlow<String?>(null)
    val detail: StateFlow<String?> = _detail.asStateFlow()

    /** Owned by the service: true while its TUN interface exists. */
    private val _ownTunnelUp = MutableStateFlow(false)

    /**
     * The system consent dialog, or `null` when consent was granted earlier.
     * The caller must launch the intent itself: only an Activity can.
     */
    fun prepareIntent(): Intent? = VpnService.prepare(context)

    /** Called after the user accepted the system dialog. */
    fun start() {
        // Re-activating a running filter would only flicker the status.
        if (_status.value == ProtectionStatus.ACTIVE) return
        _status.value = ProtectionStatus.PERMISSION_PENDING
        _detail.value = null
        try {
            context.startService(Intent(context, LockSiteVpnService::class.java))
        } catch (error: IllegalStateException) {
            // Android refuses to start a service from the background on API 26+.
            // Reporting a failure beats leaving the UI stuck on "waiting for permission".
            publish(ProtectionStatus.ERROR, context.getString(R.string.vpn_error_start))
        }
    }

    fun onPermissionDenied() {
        // Denying consent while another VPN runs is not "not configured yet": Android only
        // allows one VPN, so the truthful state is a conflict, not an idle app.
        if (hasForeignVpn()) {
            publish(
                ProtectionStatus.CONFLICT_WITH_OTHER_VPN,
                context.getString(R.string.vpn_error_conflict),
            )
        } else {
            _status.value = ProtectionStatus.NOT_CONFIGURED
            _detail.value = null
        }
    }

    /** True when this app already holds VPN consent. */
    fun hasPermission(): Boolean = prepareIntent() == null

    /**
     * Set by [LockSiteVpnService] when its tunnel is established, cleared when it is gone.
     *
     * This is authoritative for ownership. The service can report [ProtectionStatus.ERROR]
     * while its tunnel is still filtering from the last rule snapshot; inferring ownership
     * from the status alone would then report our own tunnel as another app's VPN.
     */
    internal fun setOwnTunnelUp(up: Boolean) {
        _ownTunnelUp.value = up
    }

    /** `null` when the state could not be read, which is not the same as "no VPN". */
    fun vpnTransportState(): Boolean? = runCatching {
        val manager = context.getSystemService(ConnectivityManager::class.java)
            ?: return@runCatching false
        val network = manager.activeNetwork ?: return@runCatching false
        val capabilities = manager.getNetworkCapabilities(network) ?: return@runCatching false
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
    }.getOrNull()

    fun hasVpnTransport(): Boolean = vpnTransportState() == true

    /** `null` when ownership cannot be determined. */
    fun foreignVpnState(): Boolean? =
        resolveForeignVpn(ownTunnelUp = _ownTunnelUp.value, vpnTransport = vpnTransportState())

    /** True only when another VPN is confirmed active, so this filter cannot run. */
    fun hasForeignVpn(): Boolean = foreignVpnState() == true

    /**
     * Re-evaluates a conflict that may have cleared while the app was in the background.
     *
     * Only ever downgrades [ProtectionStatus.CONFLICT_WITH_OTHER_VPN] to
     * [ProtectionStatus.NOT_CONFIGURED]; a running or starting service is never touched.
     * An unknown transport state keeps the conflict instead of claiming it cleared.
     */
    fun refreshConflictState() {
        if (_status.value != ProtectionStatus.CONFLICT_WITH_OTHER_VPN) return
        if (foreignVpnState() == false) {
            _status.value = ProtectionStatus.NOT_CONFIGURED
            _detail.value = null
        }
    }

    internal fun publish(status: ProtectionStatus, detail: String? = null) {
        _status.value = status
        _detail.value = detail
    }
}

/**
 * Whether the active VPN transport belongs to another app.
 *
 * [ownTunnelUp] is authoritative for our own tunnel. A `null` [vpnTransport] means the
 * transport could not be read; that is "unknown", not "no VPN".
 */
internal fun resolveForeignVpn(ownTunnelUp: Boolean, vpnTransport: Boolean?): Boolean? =
    if (ownTunnelUp) false else vpnTransport
