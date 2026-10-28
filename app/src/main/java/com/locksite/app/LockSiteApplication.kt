package com.locksite.app

import android.app.Application

/**
 * Process-wide entry point and owner of the manual dependency graph.
 *
 * No analytics, no network initialisation: the app never phones home.
 */
class LockSiteApplication : Application() {

    val container: AppContainer by lazy { AppContainer(this) }
}
