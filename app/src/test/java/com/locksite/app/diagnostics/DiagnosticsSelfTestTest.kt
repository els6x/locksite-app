package com.locksite.app.diagnostics

import com.locksite.app.domain.model.LockedDomain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsSelfTestTest {

    private fun rule(hostname: String, includeSubdomains: Boolean = true) = LockedDomain(
        id = 1,
        hostname = hostname,
        includeSubdomains = includeSubdomains,
        createdAtEpochMillis = 0,
    )

    @Test
    fun `blocks the probed locked domain and forwards the allowed probe`() {
        val result = DiagnosticsSelfTest.run(listOf(rule("instagram.com")))

        assertEquals("instagram.com", result.lockedProbe)
        assertEquals(true, result.blockedAsExpected)
        assertTrue(result.allowedAsExpected)
        assertTrue(result.passed)
        assertEquals(1, result.ruleCount)
    }

    @Test
    fun `honours an exact-only rule when probing the rule hostname`() {
        val result = DiagnosticsSelfTest.run(listOf(rule("example.com", includeSubdomains = false)))

        assertEquals(true, result.blockedAsExpected)
        assertTrue(result.passed)
    }

    @Test
    fun `an empty blocklist cannot verify blocking`() {
        val result = DiagnosticsSelfTest.run(emptyList())

        assertNull(result.lockedProbe)
        assertNull(result.blockedAsExpected)
        assertTrue(result.allowedAsExpected)
        // Forwarding works, but nothing was blocked-checked: this must not read as "passed".
        assertFalse(result.passed)
        assertTrue(result.blockCheckSkipped)
        assertEquals(0, result.ruleCount)
    }

    @Test
    fun `the allowed probe stays outside suffix rules`() {
        val rules = listOf(
            rule("instagram.com"),
            rule("tiktok.com"),
            rule("probe.invalid"),
        )

        val result = DiagnosticsSelfTest.run(rules)

        assertTrue(result.allowedAsExpected)
        assertTrue(result.passed)
    }

    @Test
    fun `reports a failure when every candidate probe is locked`() {
        // Worst case built directly: every name the self-test could choose is locked, so
        // the forwarding check must fail rather than quietly passing.
        val rules = (0 until 40).map { rule("probe-$it.invalid") }

        val result = DiagnosticsSelfTest.run(rules)

        assertFalse(result.allowedAsExpected)
        assertFalse(result.passed)
        assertEquals(true, result.blockedAsExpected)
    }
}
