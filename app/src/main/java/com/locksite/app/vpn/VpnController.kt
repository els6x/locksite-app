package com.locksite.app.vpn

import android.content.Context
import android.content.Intent
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
        _status.value = ProtectionStatus.NOT_CONFIGURED
        _detail.value = null
    }

    internal fun publish(status: ProtectionStatus, detail: String? = null) {
        _status.value = status
        _detail.value = detail
    }
}
