package com.locksite.app.data.repository

import android.database.sqlite.SQLiteConstraintException
import com.locksite.app.data.dao.LockedDomainDao
import com.locksite.app.data.entity.LockedDomainEntity
import com.locksite.app.domain.model.LockState
import com.locksite.app.domain.model.LockedDomain
import com.locksite.app.domain.repository.AddOutcome
import com.locksite.app.domain.repository.BlocklistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Room-backed blocklist.
 *
 * Errors are surfaced, never swallowed: a failed read throws so the UI can show an error
 * state instead of pretending the list is empty and therefore "safe" (PRD FR-040).
 */
class RoomBlocklistRepository(
    private val dao: LockedDomainDao,
) : BlocklistRepository {

    override fun observeAll(): Flow<List<LockedDomain>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeCount(): Flow<Int> = dao.observeCount()

    override suspend fun snapshot(): List<LockedDomain> = dao.snapshot().map { it.toDomain() }

    override suspend fun add(hostname: String, includeSubdomains: Boolean): AddOutcome = try {
        dao.insert(
            LockedDomainEntity(
                hostnameNormalized = hostname,
                includeSubdomains = includeSubdomains,
                createdAtEpochMillis = System.currentTimeMillis(),
                state = LockState.LOCKED.name,
            ),
        )
        AddOutcome.ADDED
    } catch (duplicate: SQLiteConstraintException) {
        AddOutcome.DUPLICATE
    } catch (other: Exception) {
        AddOutcome.FAILURE
    }
}

private fun LockedDomainEntity.toDomain(): LockedDomain = LockedDomain(
    id = id,
    hostname = hostnameNormalized,
    includeSubdomains = includeSubdomains,
    createdAtEpochMillis = createdAtEpochMillis,
    state = LockState.LOCKED,
)
