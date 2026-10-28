package com.locksite.app

import android.app.Application

/**
 * Process-wide entry point.
 *
 * M0 keeps this class intentionally empty: no dependency-injection container, no
 * analytics, no network initialisation. Services and repositories are wired in M1/M3.
 */
class LockSiteApplication : Application()
