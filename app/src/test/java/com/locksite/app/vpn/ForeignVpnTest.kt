package com.locksite.app.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The ownership decision behind [VpnController.hasForeignVpn].
 *
 * The important case is our own tunnel reporting ERROR while it is still filtering: that
 * must never be classified as "another VPN".
 */
class ForeignVpnTest {

    @Test
    fun `our own tunnel is never reported as foreign`() {
        assertEquals(false, resolveForeignVpn(ownTunnelUp = true, vpnTransport = true))
    }

    @Test
    fun `our own tunnel stays ours even when the transport cannot be read`() {
        assertEquals(false, resolveForeignVpn(ownTunnelUp = true, vpnTransport = null))
    }

    @Test
    fun `a transport without our tunnel is foreign`() {
        assertEquals(true, resolveForeignVpn(ownTunnelUp = false, vpnTransport = true))
    }

    @Test
    fun `no transport means no foreign vpn`() {
        assertEquals(false, resolveForeignVpn(ownTunnelUp = false, vpnTransport = false))
    }

    @Test
    fun `an unreadable transport is unknown, not absent`() {
        assertNull(resolveForeignVpn(ownTunnelUp = false, vpnTransport = null))
    }
}
