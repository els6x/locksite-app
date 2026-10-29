package com.locksite.app.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UnderlyingNetworksTest {

    private fun candidate(
        id: Int,
        isVpn: Boolean = false,
        hasInternet: Boolean = true,
        isValidated: Boolean = false,
        isActive: Boolean = false,
    ) = UnderlyingCandidate(
        id = id,
        isVpn = isVpn,
        hasInternet = hasInternet,
        isValidated = isValidated,
        isActive = isActive,
    )

    @Test
    fun `prefers the active physical network over another validated one`() {
        val chosen = selectUnderlying(
            listOf(
                candidate(id = 1, isValidated = true),
                candidate(id = 2, isValidated = false, isActive = true),
            ),
        )

        assertEquals(2, chosen?.id)
    }

    @Test
    fun `prefers a validated network when none is active`() {
        val chosen = selectUnderlying(
            listOf(
                candidate(id = 1, isValidated = false),
                candidate(id = 2, isValidated = true),
            ),
        )

        assertEquals(2, chosen?.id)
    }

    @Test
    fun `never picks a VPN transport, even when it is active and validated`() {
        val chosen = selectUnderlying(
            listOf(
                candidate(id = 1, isVpn = true, isValidated = true, isActive = true),
                candidate(id = 2, isValidated = false),
            ),
        )

        assertEquals(2, chosen?.id)
    }

    @Test
    fun `returns null when only a VPN transport is available`() {
        val chosen = selectUnderlying(
            listOf(candidate(id = 1, isVpn = true, isValidated = true, isActive = true)),
        )

        assertNull(chosen)
    }

    @Test
    fun `ignores a network without internet capability, even when it is active and validated`() {
        val chosen = selectUnderlying(
            listOf(candidate(id = 1, hasInternet = false, isValidated = true, isActive = true)),
        )

        assertNull(chosen)
    }

    @Test
    fun `prefers an internet-capable network over a validated one without internet`() {
        val chosen = selectUnderlying(
            listOf(
                candidate(id = 1, hasInternet = false, isValidated = true, isActive = true),
                candidate(id = 2, hasInternet = true, isValidated = false),
            ),
        )

        assertEquals(2, chosen?.id)
    }

    @Test
    fun `returns null for an empty candidate list`() {
        assertNull(selectUnderlying(emptyList()))
    }

    @Test
    fun `is deterministic when candidates tie on every rank`() {
        val chosen = selectUnderlying(
            listOf(
                candidate(id = 7, isValidated = true),
                candidate(id = 3, isValidated = true),
            ),
        )

        assertEquals(3, chosen?.id)
    }
}
