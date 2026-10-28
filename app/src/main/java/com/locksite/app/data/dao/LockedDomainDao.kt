package com.locksite.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.locksite.app.data.entity.LockedDomainEntity
import kotlinx.coroutines.flow.Flow

/**
 * Deliberately read + insert only. There is no @Delete or @Update statement in the
 * production source set (PRD section 6).
 */
@Dao
interface LockedDomainDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: LockedDomainEntity): Long

    @Query("SELECT * FROM locked_domains ORDER BY created_at DESC")
    fun observeAll(): Flow<List<LockedDomainEntity>>

    @Query("SELECT COUNT(*) FROM locked_domains")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM locked_domains ORDER BY created_at DESC")
    suspend fun snapshot(): List<LockedDomainEntity>
}
