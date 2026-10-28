package com.locksite.app

import android.content.Context
import com.locksite.app.data.local.LockSiteDatabase
import com.locksite.app.data.local.OnboardingStore
import com.locksite.app.data.repository.RoomBlocklistRepository
import com.locksite.app.domain.repository.BlocklistRepository
import com.locksite.app.domain.usecase.AddDomainUseCase

/**
 * Manual dependency graph. No DI framework: at this size an explicit container is
 * easier to read than generated wiring (see ADR 0001).
 */
class AppContainer(context: Context) {

    private val applicationContext = context.applicationContext

    private val database by lazy { LockSiteDatabase.build(applicationContext) }

    val blocklistRepository: BlocklistRepository by lazy {
        RoomBlocklistRepository(database.lockedDomainDao())
    }

    val onboardingStore: OnboardingStore by lazy { OnboardingStore(applicationContext) }

    val addDomainUseCase: AddDomainUseCase by lazy { AddDomainUseCase(blocklistRepository) }
}
