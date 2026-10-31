package com.locksite.app.data.local

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Local-only display and resolver preferences.
 *
 * Nothing here leaves the device. The mask flag only changes how locked names are drawn; it
 * does not change the stored blocklist or the filter, and it is never reported to a server.
 * The AdGuard flag only chooses which resolver chain allowed queries use.
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

    /**
     * Whether allowed queries are also filtered by AdGuard DNS.
     *
     * Default on: AdGuard is the feature the user asked for, and Cloudflare stays at the end
     * of the chain, so an unreachable AdGuard degrades to plain resolution instead of a
     * dead resolver.
     */
    private val _adGuardEnabled = MutableStateFlow(prefs.getBoolean(KEY_ADGUARD, true))
    val adGuardEnabled: StateFlow<Boolean> = _adGuardEnabled.asStateFlow()

    fun setAdGuardEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_ADGUARD, enabled) }
        _adGuardEnabled.value = enabled
    }

    private companion object {
        const val PREFS_NAME = "locksite_privacy"
        const val KEY_MASK_DOMAINS = "mask_domains"
        const val KEY_ADGUARD = "adguard_enabled"
    }
}
