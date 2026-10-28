package com.locksite.app.domain.model

/**
 * Truthful protection state (PRD FR-030). The UI must render exactly what this says —
 * never a stored boolean.
 *
 * Until the VPN service exists (M2) the app always reports [NotConfigured].
 */
enum class ProtectionStatus {
    NOT_CONFIGURED,
    PERMISSION_PENDING,
    ACTIVE,
    ERROR,
    STOPPED,
    CONFLICT_WITH_OTHER_VPN,
}
