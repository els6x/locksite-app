package com.locksite.app.presentation.common

import com.locksite.app.R
import com.locksite.app.domain.model.ProtectionStatus

/**
 * The single mapping from service state to the words the UI shows (PRD FR-030).
 *
 * Keeping it here — instead of inside a screen — means the mapping is exhaustive by
 * construction and can be unit tested without Compose: every status must have a label and
 * an explanatory note, and no two statuses may share the same label.
 */
object ProtectionStatusText {

    fun label(status: ProtectionStatus): Int = when (status) {
        ProtectionStatus.NOT_CONFIGURED -> R.string.status_not_configured
        ProtectionStatus.PERMISSION_PENDING -> R.string.status_permission_pending
        ProtectionStatus.ACTIVE -> R.string.status_active
        ProtectionStatus.ERROR -> R.string.status_error
        ProtectionStatus.STOPPED -> R.string.status_stopped
        ProtectionStatus.CONFLICT_WITH_OTHER_VPN -> R.string.status_conflict
    }

    fun note(status: ProtectionStatus): Int = when (status) {
        ProtectionStatus.NOT_CONFIGURED -> R.string.dashboard_note_idle
        ProtectionStatus.PERMISSION_PENDING -> R.string.dashboard_note_permission
        ProtectionStatus.ACTIVE -> R.string.dashboard_note_active
        ProtectionStatus.ERROR -> R.string.dashboard_note_error
        ProtectionStatus.STOPPED -> R.string.dashboard_note_stopped
        ProtectionStatus.CONFLICT_WITH_OTHER_VPN -> R.string.dashboard_note_conflict
    }
}
