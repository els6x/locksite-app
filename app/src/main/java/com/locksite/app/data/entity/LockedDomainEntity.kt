package com.locksite.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Row of the locked blocklist (PRD section 10.2).
 *
 * The unique index on [hostnameNormalized] is the real duplicate guard: validation happens
 * before insert, and the database enforces it even if a caller forgets.
 */
@Entity(
    tableName = "locked_domains",
    indices = [Index(value = ["hostname_normalized"], unique = true)],
)
data class LockedDomainEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "hostname_normalized") val hostnameNormalized: String,
    @ColumnInfo(name = "include_subdomains") val includeSubdomains: Boolean,
    @ColumnInfo(name = "created_at") val createdAtEpochMillis: Long,
    @ColumnInfo(name = "state") val state: String,
)
