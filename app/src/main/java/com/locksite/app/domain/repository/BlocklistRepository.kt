package com.locksite.app.domain.repository

import com.locksite.app.domain.model.LockedDomain
import kotlinx.coroutines.flow.Flow

/**
 * Read/write port for the locked blocklist.
 *
 * Note the absence of `delete` and `update`: the product does not expose those operations
 * (PRD section 6). Debug-only reset tooling lives in the debug source set, not here.
 */
interface BlocklistRepository {

    fun observeAll(): Flow<List<LockedDomain>>

    fun observeCount(): Flow<Int>

    /** Snapshot used by the DNS filter; never called per packet from the UI. */
    suspend fun snapshot(): List<LockedDomain>

    /** Transactional insert. A duplicate hostname is reported, not silently merged. */
    suspend fun add(hostname: String, includeSubdomains: Boolean): AddOutcome
}

enum class AddOutcome {
    ADDED,
    DUPLICATE,
    FAILURE,
}
