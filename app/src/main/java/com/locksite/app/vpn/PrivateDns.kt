package com.locksite.app.vpn

/**
 * Android's Private DNS (DNS-over-TLS) mode, as far as this app needs to know it.
 *
 * A DNS-only VPN answers plaintext DNS. When the device is set to a strict hostname, the
 * resolver insists on TLS, cannot fall back, and every lookup fails — including names that
 * are not blocked. In that state the filter must not pretend to work, because claiming
 * "active" while the phone has no DNS is worse than refusing to start.
 *
 * `opportunistic` and `off` are fine: opportunistic falls back to plaintext.
 */
object PrivateDns {

    const val MODE_HOSTNAME = "hostname"

    fun isStrict(mode: String?): Boolean = mode == MODE_HOSTNAME
}
