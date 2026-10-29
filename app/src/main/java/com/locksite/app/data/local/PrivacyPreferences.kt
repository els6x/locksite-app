package com.locksite.app.data.local

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Local-only display preferences.
 *
 * Nothing here leaves the device. The mask flag only changes how locked names are drawn; it
 * does not change the stored blocklist or the filter, and it is never reported to a server.
 */
class PrivacyPreferences(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _maskDomains = MutableStateFlow(prefs.getBoolean(KEY_MASK_DOMAINS, false))
    val maskDomains: StateFlow<Boolean> = _maskDomains.asStateFlow()

    fun setMaskDomains(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_MASK_DOMAINS, enabled) }
        _maskDomains.value = enabled
    }

    private companion object {
        const val PREFS_NAME = "locksite_privacy"
        const val KEY_MASK_DOMAINS = "mask_domains"
    }
}
