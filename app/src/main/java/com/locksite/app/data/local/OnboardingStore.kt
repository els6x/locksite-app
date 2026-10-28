package com.locksite.app.data.local

import android.content.Context

/**
 * Remembers that the onboarding explanation was read once.
 *
 * Plain SharedPreferences on purpose: one boolean does not justify a DataStore dependency.
 */
class OnboardingStore(context: Context) {

    private val preferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun isCompleted(): Boolean = preferences.getBoolean(KEY_COMPLETED, false)

    fun markCompleted() {
        preferences.edit().putBoolean(KEY_COMPLETED, true).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "locksite_prefs"
        const val KEY_COMPLETED = "onboarding_completed"
    }
}
