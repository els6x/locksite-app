package com.locksite.app.dns

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DnsMessagesTest {

    @Test
    fun `parses a simple query`() {
        val query = DnsMessages.parseQuery(TestWire.query("example.com", id = 0x4321))

        assertNotNull(query)
        requireNotNull(query)
        assertEquals(0x4321, query.id)
        assertEquals("example.com", query.question.name)
        assertEquals(1, query.question.type)
        assertEquals(1, query.question.dnsClass)
        assertTrue(query.recursionDesired)
        assertEquals(DnsMessages.OPCODE_QUERY, query.opcode)
    }

    @Test
    fun `keeps the raw message intact for forwarding`() {
        val wire = TestWire.query("a.example.com")
        val query = requireNotNull(DnsMessages.parseQuery(wire))

        assertTrue(wire.contentEquals(query.raw))
    }

    @Test
    fun `question end offset points at the end of qclass`() {
        val wire = TestWire.query("abc.io")
        val query = requireNotNull(DnsMessages.parseQuery(wire))

        assertEquals(wire.size, query.question.endOffset)
    }

    @Test
    fun `rejects a response`() {
        val wire = TestWire.query("example.com", flags = 0x8180)

        assertNull(DnsMessages.parseQuery(wire))
    }

    @Test
    fun `rejects zero questions`() {
        val wire = TestWire.query("example.com")
        TestWire.putU16(wire, 4, 0)

        assertNull(DnsMessages.parseQuery(wire))
    }

    @Test
    fun `rejects a truncated header`() {
        assertNull(DnsMessages.parseQuery(ByteArray(11)))
    }

    @Test
    fun `rejects a truncated question`() {
        val wire = TestWire.query("example.com").copyOf(16)

        assertNull(DnsMessages.parseQuery(wire))
    }

    @Test
    fun `rejects a label that runs past the buffer`() {
        val wire = TestWire.query("example.com")
        wire[DnsMessages.HEADER_SIZE] = 60.toByte()   // claims a 60-byte label

        assertNull(DnsMessages.parseQuery(wire))
    }

    @Test
    fun `rejects a reserved label type`() {
        val wire = TestWire.query("example.com")
        wire[DnsMessages.HEADER_SIZE] = 0x40.toByte()

        assertNull(DnsMessages.parseQuery(wire))
    }

    @Test
    fun `decodes a backward compression pointer`() {
        // "example.com" written at offset 12, then a pointer to it at offset 25.
        val name = TestWire.query("example.com")
        val pointerOffset = DnsMessages.HEADER_SIZE + 13
        val buffer = ByteArray(pointerOffset + 2)
        name.copyInto(buffer, 0, 0, pointerOffset)
        buffer[pointerOffset] = 0xC0.toByte()         // pointer to offset 12
        buffer[pointerOffset + 1] = 0x0C

        val decoded = DnsMessages.readName(buffer, pointerOffset, buffer.size)

        assertNotNull(decoded)
        requireNotNull(decoded)
        assertEquals("example.com", decoded.first)
        assertEquals(pointerOffset + 2, decoded.second)
    }

    @Test
    fun `rejects a forward compression pointer`() {
        val buffer = ByteArray(20)
        buffer[12] = 0xC0.toByte()
        buffer[13] = 0x10                            // points to offset 16, i.e. forwards

        assertNull(DnsMessages.readName(buffer, 12, buffer.size))
    }

    @Test
    fun `rejects a self-referential pointer`() {
        val buffer = ByteArray(20)
        buffer[12] = 0xC0.toByte()
        buffer[13] = 0x0C                            // points at itself

        assertNull(DnsMessages.readName(buffer, 12, buffer.size))
    }

    @Test
    fun `rejects a pointer into the fixed header`() {
        val buffer = ByteArray(20)
        buffer[12] = 0xC0.toByte()
        buffer[13] = 0x00                            // offset 0 is header, never a name

        assertNull(DnsMessages.readName(buffer, 12, buffer.size))
    }

    @Test
    fun `rejects a name longer than 255 bytes`() {
        val label = "a".repeat(63)
        val longName = listOf(label, label, label, label).joinToString(".")
        val buffer = ByteArray(DnsMessages.HEADER_SIZE + 260)
        var offset = DnsMessages.HEADER_SIZE
        repeat(4) {
            buffer[offset] = 63
            label.toByteArray(Charsets.US_ASCII).copyInto(buffer, offset + 1)
            offset += 64
        }

        assertNull(DnsMessages.readName(buffer, DnsMessages.HEADER_SIZE, buffer.size))
        // Sanity: the fixture itself is a legal-but-too-long name, not a broken buffer.
        assertEquals(255, longName.length)
    }
}
