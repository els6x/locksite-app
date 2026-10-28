package com.locksite.app.dns

import com.locksite.app.domain.model.LockedDomain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DnsQueryHandlerTest {

    private fun rule(hostname: String, includeSubdomains: Boolean = true) = LockedDomain(
        id = 1,
        hostname = hostname,
        includeSubdomains = includeSubdomains,
        createdAtEpochMillis = 0,
    )

    private fun handler(vararg rules: LockedDomain) = DnsQueryHandler { rules.toList() }

    private fun decisionFor(handler: DnsQueryHandler, name: String): DnsQueryHandler.Decision =
        handler.handle(TestWire.query(name))

    @Test
    fun `blocks an exact match with nxdomain`() {
        val decision = decisionFor(handler(rule("instagram.com")), "instagram.com")

        assertTrue(decision is DnsQueryHandler.Decision.Block)
        val reply = (decision as DnsQueryHandler.Decision.Block).response
        assertEquals(DnsMessages.RCODE_NAME_ERROR, TestWire.u16(reply, 2) and 0x0F)
    }

    @Test
    fun `blocks a subdomain when the rule includes subdomains`() {
        val decision = decisionFor(handler(rule("instagram.com")), "cdn.instagram.com")

        assertTrue(decision is DnsQueryHandler.Decision.Block)
    }

    @Test
    fun `does not block a lookalike domain`() {
        val decision = decisionFor(handler(rule("example.com")), "notexample.com")

        assertTrue(decision is DnsQueryHandler.Decision.Forward)
    }

    @Test
    fun `does not block a suffix glued onto another label`() {
        val decision = decisionFor(handler(rule("example.com")), "example.com.attacker.test")

        assertTrue(decision is DnsQueryHandler.Decision.Forward)
    }

    @Test
    fun `does not block a subdomain when the rule is exact only`() {
        val decision = decisionFor(handler(rule("example.com", includeSubdomains = false)), "a.example.com")

        assertTrue(decision is DnsQueryHandler.Decision.Forward)
    }

    @Test
    fun `matches case-insensitively`() {
        val decision = decisionFor(handler(rule("Instagram.com")), "WWW.INSTAGRAM.COM")

        assertTrue(decision is DnsQueryHandler.Decision.Block)
    }

    @Test
    fun `forwards everything when no rule exists`() {
        val decision = decisionFor(handler(), "example.com")

        assertTrue(decision is DnsQueryHandler.Decision.Forward)
    }

    @Test
    fun `forwards when only one of several rules matches nothing`() {
        val decision = decisionFor(
            handler(rule("a.com"), rule("b.com"), rule("c.com")),
            "d.com",
        )

        assertTrue(decision is DnsQueryHandler.Decision.Forward)
    }

    @Test
    fun `drops a message that is already a response`() {
        val response = TestWire.query("example.com", flags = 0x8180)

        assertEquals(DnsQueryHandler.Decision.Drop, handler(rule("example.com")).handle(response))
    }

    @Test
    fun `drops a runt packet`() {
        assertEquals(DnsQueryHandler.Decision.Drop, handler().handle(ByteArray(6)))
    }

    @Test
    fun `answers a malformed question with a format error`() {
        val wire = TestWire.query("example.com")
        wire[DnsMessages.HEADER_SIZE] = 60.toByte()   // label length beyond the buffer

        val decision = handler(rule("example.com")).handle(wire)

        assertTrue(decision is DnsQueryHandler.Decision.Reject)
        val reply = (decision as DnsQueryHandler.Decision.Reject).response
        assertEquals(DnsMessages.RCODE_FORMAT_ERROR, TestWire.u16(reply, 2) and 0x0F)
    }

    @Test
    fun `forwards a non-standard opcode instead of guessing`() {
        // Opcode 2 (STATUS) in the flags field.
        val wire = TestWire.query("instagram.com", flags = 0x1100)

        val decision = handler(rule("instagram.com")).handle(wire)

        assertTrue(decision is DnsQueryHandler.Decision.Forward)
    }

    @Test
    fun `a locked name is answered locally without touching the network`() {
        // The decision carries bytes, not a forward request: nothing left the process.
        val decision = decisionFor(handler(rule("tiktok.com")), "www.tiktok.com")

        assertTrue(decision is DnsQueryHandler.Decision.Block)
    }
}
