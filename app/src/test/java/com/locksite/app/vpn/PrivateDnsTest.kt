package com.locksite.app.vpn

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivateDnsTest {

    @Test
    fun `strict hostname is detected`() {
        assertTrue(PrivateDns.isStrict("hostname"))
    }

    @Test
    fun `opportunistic is not strict`() {
        assertFalse(PrivateDns.isStrict("opportunistic"))
    }

    @Test
    fun `off is not strict`() {
        assertFalse(PrivateDns.isStrict("off"))
    }

    @Test
    fun `unknown or missing mode is not treated as strict`() {
        assertFalse(PrivateDns.isStrict(null))
        assertFalse(PrivateDns.isStrict("something-else"))
    }
}
