package com.locksite.app.domain.model

/**
 * A domain that the user has locked.
 *
 * There is deliberately no `UNLOCKED` state: the product does not support unblocking
 * from the app UI (PRD section 6).
 */
data class LockedDomain(
    val id: Long,
    val hostname: String,
    val includeSubdomains: Boolean,
    val createdAtEpochMillis: Long,
    val state: LockState = LockState.LOCKED,
)

enum class LockState {
    LOCKED,
}
