package com.locksite.app.dns

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DnsResponsesTest {

    private fun parse(name: String, id: Int = 0x0BAD, flags: Int = 0x0100) =
        requireNotNull(DnsMessages.parseQuery(TestWire.query(name, id, flags)))

    @Test
    fun `name error echoes id and question`() {
        val query = parse("instagram.com")
        val reply = DnsResponses.nameError(query)

        assertEquals(0x0BAD, TestWire.u16(reply, 0))
        assertEquals(1, TestWire.u16(reply, 4))              // QDCOUNT
        assertEquals(0, TestWire.u16(reply, 6))              // ANCOUNT
        assertEquals(0, TestWire.u16(reply, 8))              // NSCOUNT
        assertEquals(0, TestWire.u16(reply, 10))             // ARCOUNT

        val question = reply.copyOfRange(DnsMessages.HEADER_SIZE, reply.size)
        val original = query.raw.copyOfRange(DnsMessages.HEADER_SIZE, query.question.endOffset)
        assertTrue(original.contentEquals(question))
    }

    @Test
    fun `name error sets response flags and rcode 3`() {
        val reply = DnsResponses.nameError(parse("instagram.com"))

        val flags = TestWire.u16(reply, 2)
        assertEquals(DnsMessages.FLAG_QR, flags and DnsMessages.FLAG_QR)
        assertEquals(DnsMessages.FLAG_RD, flags and DnsMessages.FLAG_RD)
        assertEquals(DnsMessages.FLAG_RA, flags and DnsMessages.FLAG_RA)
        assertEquals(DnsMessages.RCODE_NAME_ERROR, flags and 0x0F)
        assertEquals(0, flags and 0x7800)                    // opcode mirrored as QUERY
    }

    @Test
    fun `server failure sets rcode 2`() {
        val reply = DnsResponses.serverFailure(parse("example.com"))

        assertEquals(DnsMessages.RCODE_SERVER_FAILURE, TestWire.u16(reply, 2) and 0x0F)
        assertEquals(DnsMessages.FLAG_QR, TestWire.u16(reply, 2) and DnsMessages.FLAG_QR)
    }

    @Test
    fun `truncated reply sets the tc bit and carries no answer`() {
        val reply = DnsResponses.truncated(parse("example.com"))

        val flags = TestWire.u16(reply, 2)
        assertEquals(DnsMessages.FLAG_QR, flags and DnsMessages.FLAG_QR)
        assertEquals(0x0200, flags and 0x0200)               // TC
        assertEquals(0, flags and 0x0F)                      // NOERROR, not an error code
        assertEquals(0, TestWire.u16(reply, 6))              // ANCOUNT
    }

    @Test
    fun `format error echoes only the header`() {
        val request = TestWire.query("example.com")
        val reply = DnsResponses.formatError(request)

        assertNotNull(reply)
        requireNotNull(reply)
        assertEquals(DnsMessages.HEADER_SIZE, reply.size)
        assertEquals(0x1234, TestWire.u16(reply, 0))
        assertEquals(DnsMessages.RCODE_FORMAT_ERROR, TestWire.u16(reply, 2) and 0x0F)
        assertEquals(0, TestWire.u16(reply, 4))
    }

    @Test
    fun `format error refuses a runt packet`() {
        assertNull(DnsResponses.formatError(ByteArray(5)))
    }

    @Test
    fun `replies do not carry a fake address record`() {
        val reply = DnsResponses.nameError(parse("instagram.com"))

        // Header + echoed question only: no answer section can be smuggled in.
        // "instagram.com" is 15 wire bytes (10 + 4 + root), then QTYPE + QCLASS.
        assertEquals(DnsMessages.HEADER_SIZE + 15 + 4, reply.size)
    }
}
