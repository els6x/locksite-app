package com.locksite.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.locksite.app.data.dao.LockedDomainDao
import com.locksite.app.data.entity.LockedDomainEntity

/**
 * Local-only database. The blocklist never leaves the device (PRD section 11.1).
 *
 * `exportSchema = true` writes the schema to app/schemas so migrations can be diffed
 * and tested before shipping.
 */
@Database(
    entities = [LockedDomainEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class LockSiteDatabase : RoomDatabase() {

    abstract fun lockedDomainDao(): LockedDomainDao

    companion object {
        private const val DATABASE_NAME = "locksite.db"

        fun build(context: Context): LockSiteDatabase =
            Room.databaseBuilder(context.applicationContext, LockSiteDatabase::class.java, DATABASE_NAME)
                // No destructive fallback: losing the blocklist must never look like "safe".
                .build()
    }
}
