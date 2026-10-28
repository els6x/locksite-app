package com.locksite.app.dns

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IpPacketsTest {

    private val clientV4 = TestWire.ipv4(10, 111, 222, 1)
    private val serverV4 = TestWire.ipv4(10, 111, 222, 2)
    private val clientV6 = TestWire.ipv6(0xfd00, 0x0001, 0xfd00, 0x0001, 0xfd00, 0x0001, 0xfd00, 0x0001)
    private val serverV6 = TestWire.ipv6(0xfd00, 0x0001, 0xfd00, 0x0001, 0xfd00, 0x0001, 0xfd00, 0x0002)

    private fun datagram(
        source: ByteArray,
        destination: ByteArray,
        payload: ByteArray = TestWire.query("example.com"),
    ) = UdpDatagram(
        sourceAddress = source,
        destinationAddress = destination,
        sourcePort = 40_000,
        destinationPort = 53,
        payload = payload,
    )

    @Test
    fun `ipv4 round trip preserves every field`() {
        val original = datagram(clientV4, serverV4)

        val parsed = IpPackets.parseUdp(IpPackets.buildUdp(original))

        assertEquals(original, parsed)
    }

    @Test
    fun `ipv6 round trip preserves every field`() {
        val original = datagram(clientV6, serverV6)

        val parsed = IpPackets.parseUdp(IpPackets.buildUdp(original))

        assertEquals(original, parsed)
    }

    @Test
    fun `ipv4 header checksum verifies`() {
        val packet = IpPackets.buildUdp(datagram(clientV4, serverV4))
        val headerLength = (packet[0].toInt() and 0x0F) * 4

        assertEquals(0xFFFF, TestWire.checksumSum(packet.copyOfRange(0, headerLength)))
    }

    @Test
    fun `ipv4 udp checksum verifies`() {
        val packet = IpPackets.buildUdp(datagram(clientV4, serverV4))
        val udpLength = TestWire.u16(packet, 2) - 20

        assertEquals(0xFFFF, TestWire.checksumSum(pseudoHeader(clientV4, serverV4, udpLength, ipv6 = false), packet.copyOfRange(20, 20 + udpLength)))
    }

    @Test
    fun `ipv6 udp checksum verifies`() {
        val packet = IpPackets.buildUdp(datagram(clientV6, serverV6))
        val udpLength = TestWire.u16(packet, 4)

        assertEquals(0xFFFF, TestWire.checksumSum(pseudoHeader(clientV6, serverV6, udpLength, ipv6 = true), packet.copyOfRange(40, 40 + udpLength)))
    }

    @Test
    fun `odd payload length still produces a valid checksum`() {
        val payload = byteArrayOf(1, 2, 3, 4, 5)
        val packet = IpPackets.buildUdp(datagram(clientV4, serverV4, payload))
        val udpLength = TestWire.u16(packet, 2) - 20

        assertEquals(0xFFFF, TestWire.checksumSum(pseudoHeader(clientV4, serverV4, udpLength, ipv6 = false), packet.copyOfRange(20, 20 + udpLength)))
    }

    @Test
    fun `declares the correct ipv4 length and protocol`() {
        val packet = IpPackets.buildUdp(datagram(clientV4, serverV4))

        assertEquals(4, (packet[0].toInt() ushr 4) and 0x0F)
        assertEquals(5, packet[0].toInt() and 0x0F)
        assertEquals(IpPackets.PROTOCOL_UDP, packet[9].toInt() and 0xFF)
        assertEquals(packet.size, TestWire.u16(packet, 2))
    }

    @Test
    fun `declares the correct ipv6 payload length`() {
        val packet = IpPackets.buildUdp(datagram(clientV6, serverV6))

        assertEquals(6, (packet[0].toInt() ushr 4) and 0x0F)
        assertEquals(packet.size - 40, TestWire.u16(packet, 4))
        assertEquals(IpPackets.PROTOCOL_UDP, packet[6].toInt() and 0xFF)
    }

    @Test
    fun `ignores tcp packets`() {
        val packet = IpPackets.buildUdp(datagram(clientV4, serverV4))
        packet[9] = 6   // TCP

        assertNull(IpPackets.parseUdp(packet))
    }

    @Test
    fun `ignores fragmented packets instead of guessing`() {
        val packet = IpPackets.buildUdp(datagram(clientV4, serverV4))
        TestWire.putU16(packet, 6, 0x2000)   // MF set, more fragments follow

        assertNull(IpPackets.parseUdp(packet))
    }

    @Test
    fun `ignores a packet whose length lies`() {
        val packet = IpPackets.buildUdp(datagram(clientV4, serverV4))
        TestWire.putU16(packet, 2, packet.size + 100)

        assertNull(IpPackets.parseUdp(packet))
    }

    @Test
    fun `ignores a truncated udp header`() {
        val packet = IpPackets.buildUdp(datagram(clientV4, serverV4)).copyOf(24)

        assertNull(IpPackets.parseUdp(packet))
    }

    @Test
    fun `ignores garbage`() {
        assertNull(IpPackets.parseUdp(ByteArray(3)))
        assertNull(IpPackets.parseUdp(ByteArray(40)))
    }

    @Test
    fun `parses a hand built ipv4 packet`() {
        // Independent fixture: proves the parser is not just the inverse of the builder.
        val payload = TestWire.query("example.com")
        val udpLength = 8 + payload.size
        val packet = ByteArray(20 + udpLength)
        packet[0] = 0x45
        TestWire.putU16(packet, 2, packet.size)
        packet[9] = IpPackets.PROTOCOL_UDP.toByte()
        clientV4.copyInto(packet, 12)
        serverV4.copyInto(packet, 16)
        TestWire.putU16(packet, 20, 33_333)
        TestWire.putU16(packet, 22, 53)
        TestWire.putU16(packet, 24, udpLength)
        payload.copyInto(packet, 28)

        val parsed = IpPackets.parseUdp(packet)

        assertNotNull(parsed)
        requireNotNull(parsed)
        assertEquals(33_333, parsed.sourcePort)
        assertEquals(53, parsed.destinationPort)
        assertTrue(serverV4.contentEquals(parsed.destinationAddress))
        assertTrue(payload.contentEquals(parsed.payload))
    }

    private fun pseudoHeader(
        source: ByteArray,
        destination: ByteArray,
        udpLength: Int,
        ipv6: Boolean,
    ): ByteArray = if (ipv6) {
        ByteArray(40).also { header ->
            source.copyInto(header, 0)
            destination.copyInto(header, 16)
            header[34] = ((udpLength ushr 8) and 0xFF).toByte()
            header[35] = (udpLength and 0xFF).toByte()
            header[39] = IpPackets.PROTOCOL_UDP.toByte()
        }
    } else {
        ByteArray(12).also { header ->
            source.copyInto(header, 0)
            destination.copyInto(header, 4)
            header[9] = IpPackets.PROTOCOL_UDP.toByte()
            header[10] = ((udpLength ushr 8) and 0xFF).toByte()
            header[11] = (udpLength and 0xFF).toByte()
        }
    }
}
