package com.locksite.app.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the resolver chain used for allowed queries.
 *
 * The AdGuard setting must never leave the phone without a working resolver: with AdGuard on,
 * Cloudflare has to remain in the chain as a fallback.
 */
class VpnConfigTest {

    @Test
    fun `cloudflare is the whole chain when AdGuard is off`() {
        assertEquals(VpnConfig.UPSTREAM_CLOUDFLARE, VpnConfig.upstreams(adGuardEnabled = false))
    }

    @Test
    fun `AdGuard leads the chain when enabled`() {
        val chain = VpnConfig.upstreams(adGuardEnabled = true)
        assertEquals(VpnConfig.UPSTREAM_ADGUARD, chain.take(VpnConfig.UPSTREAM_ADGUARD.size))
    }

    @Test
    fun `AdGuard keeps Cloudflare as a fallback`() {
        val chain = VpnConfig.upstreams(adGuardEnabled = true)
        assertTrue(chain.containsAll(VpnConfig.UPSTREAM_CLOUDFLARE))
    }

    @Test
    fun `AdGuard addresses never leak into the disabled chain`() {
        val chain = VpnConfig.upstreams(adGuardEnabled = false)
        assertFalse(chain.any { it in VpnConfig.UPSTREAM_ADGUARD })
    }

    @Test
    fun `the enabled chain has no duplicate resolver`() {
        val chain = VpnConfig.upstreams(adGuardEnabled = true)
        assertEquals(chain.size, chain.toSet().size)
    }
}
