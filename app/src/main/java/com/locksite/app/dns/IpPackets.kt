package com.locksite.app.dns

/**
 * A UDP datagram lifted out of an IP packet, or about to be wrapped in one.
 *
 * Addresses keep their raw width: 4 bytes for IPv4, 16 bytes for IPv6. That is the only
 * thing the builders need to tell the two families apart.
 */
class UdpDatagram(
    val sourceAddress: ByteArray,
    val destinationAddress: ByteArray,
    val sourcePort: Int,
    val destinationPort: Int,
    val payload: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UdpDatagram) return false
        return sourcePort == other.sourcePort &&
            destinationPort == other.destinationPort &&
            sourceAddress.contentEquals(other.sourceAddress) &&
            destinationAddress.contentEquals(other.destinationAddress) &&
            payload.contentEquals(other.payload)
    }

    override fun hashCode(): Int {
        var result = sourceAddress.contentHashCode()
        result = 31 * result + destinationAddress.contentHashCode()
        result = 31 * result + sourcePort
        result = 31 * result + destinationPort
        result = 31 * result + payload.contentHashCode()
        return result
    }
}

/**
 * Minimal IPv4/IPv6 + UDP codec.
 *
 * Scope is deliberately narrow: this app only ever sees packets addressed to its own
 * virtual DNS server, so extension headers, fragmentation, ICMP and TCP are out of scope.
 * Anything outside that scope is reported as `null` (unparseable) and therefore ignored
 * rather than answered with a malformed packet.
 */
object IpPackets {

    const val PROTOCOL_UDP = 17

    private const val IPV4_MIN_HEADER = 20
    private const val IPV6_HEADER = 40
    private const val UDP_HEADER = 8

    private const val FLAG_MORE_FRAGMENTS = 0x2000
    private const val FRAGMENT_OFFSET_MASK = 0x1FFF
    private const val FLAG_DONT_FRAGMENT = 0x4000

    private const val DEFAULT_TTL = 64

    /** Returns the UDP datagram carried by [packet], or `null` when it is not plain UDP. */
    fun parseUdp(packet: ByteArray, length: Int = packet.size): UdpDatagram? {
        if (length < IPV4_MIN_HEADER) return null
        return when ((packet[0].toInt() ushr 4) and 0x0F) {
            4 -> parseIpv4(packet, length)
            6 -> parseIpv6(packet, length)
            else -> null
        }
    }

    /** Wraps [datagram] in a fresh IP packet with correct lengths and checksums. */
    fun buildUdp(datagram: UdpDatagram): ByteArray = when (datagram.sourceAddress.size) {
        4 -> buildIpv4(datagram)
        16 -> buildIpv6(datagram)
        else -> throw IllegalArgumentException("Unsupported address width: ${datagram.sourceAddress.size}")
    }

    private fun parseIpv4(packet: ByteArray, length: Int): UdpDatagram? {
        val headerLength = (packet[0].toInt() and 0x0F) * 4
        if (headerLength < IPV4_MIN_HEADER || length < headerLength) return null

        val totalLength = readU16(packet, 2)
        if (totalLength < headerLength + UDP_HEADER || totalLength > length) return null
        if ((packet[9].toInt() and 0xFF) != PROTOCOL_UDP) return null

        // Reassembly is out of scope: only unfragmented datagrams are handled.
        val flagsAndOffset = readU16(packet, 6)
        if ((flagsAndOffset and FLAG_MORE_FRAGMENTS) != 0) return null
        if ((flagsAndOffset and FRAGMENT_OFFSET_MASK) != 0) return null

        return readUdp(
            packet = packet,
            udpOffset = headerLength,
            ipEnd = totalLength,
            source = packet.copyOfRange(12, 16),
            destination = packet.copyOfRange(16, 20),
        )
    }

    private fun parseIpv6(packet: ByteArray, length: Int): UdpDatagram? {
        if (length < IPV6_HEADER) return null

        val payloadLength = readU16(packet, 4)
        val totalLength = IPV6_HEADER + payloadLength
        if (payloadLength < UDP_HEADER || totalLength > length) return null
        // Extension header chains are not walked; a non-UDP next header is simply skipped.
        if ((packet[6].toInt() and 0xFF) != PROTOCOL_UDP) return null

        return readUdp(
            packet = packet,
            udpOffset = IPV6_HEADER,
            ipEnd = totalLength,
            source = packet.copyOfRange(8, 24),
            destination = packet.copyOfRange(24, 40),
        )
    }

    private fun readUdp(
        packet: ByteArray,
        udpOffset: Int,
        ipEnd: Int,
        source: ByteArray,
        destination: ByteArray,
    ): UdpDatagram? {
        if (ipEnd < udpOffset + UDP_HEADER) return null
        val sourcePort = readU16(packet, udpOffset)
        val destinationPort = readU16(packet, udpOffset + 2)
        val udpLength = readU16(packet, udpOffset + 4)
        if (udpLength < UDP_HEADER) return null
        val udpEnd = udpOffset + udpLength
        if (udpEnd > ipEnd) return null
        return UdpDatagram(
            sourceAddress = source,
            destinationAddress = destination,
            sourcePort = sourcePort,
            destinationPort = destinationPort,
            payload = packet.copyOfRange(udpOffset + UDP_HEADER, udpEnd),
        )
    }

    private fun buildIpv4(datagram: UdpDatagram): ByteArray {
        require(datagram.destinationAddress.size == 4) { "IPv4 needs 4-byte addresses" }
        val udpLength = UDP_HEADER + datagram.payload.size
        val totalLength = IPV4_MIN_HEADER + udpLength
        val packet = ByteArray(totalLength)

        packet[0] = 0x45                      // version 4, 5-word header
        packet[1] = 0                         // DSCP / ECN
        writeU16(packet, 2, totalLength)
        writeU16(packet, 4, 0)                // identification, irrelevant without fragmentation
        writeU16(packet, 6, FLAG_DONT_FRAGMENT)
        packet[8] = DEFAULT_TTL.toByte()
        packet[9] = PROTOCOL_UDP.toByte()
        datagram.sourceAddress.copyInto(packet, 12)
        datagram.destinationAddress.copyInto(packet, 16)
        writeU16(packet, 10, Checksums.complete(Checksums.sum(packet, 0, IPV4_MIN_HEADER)))

        writeUdp(packet, IPV4_MIN_HEADER, datagram)
        return packet
    }

    private fun buildIpv6(datagram: UdpDatagram): ByteArray {
        require(datagram.destinationAddress.size == 16) { "IPv6 needs 16-byte addresses" }
        val udpLength = UDP_HEADER + datagram.payload.size
        val packet = ByteArray(IPV6_HEADER + udpLength)

        packet[0] = 0x60                      // version 6, traffic class 0, flow label 0
        writeU16(packet, 4, udpLength)        // payload length excludes the fixed header
        packet[6] = PROTOCOL_UDP.toByte()
        packet[7] = DEFAULT_TTL.toByte()
        datagram.sourceAddress.copyInto(packet, 8)
        datagram.destinationAddress.copyInto(packet, 24)

        writeUdp(packet, IPV6_HEADER, datagram)
        return packet
    }

    private fun writeUdp(packet: ByteArray, offset: Int, datagram: UdpDatagram) {
        val udpLength = UDP_HEADER + datagram.payload.size
        writeU16(packet, offset, datagram.sourcePort)
        writeU16(packet, offset + 2, datagram.destinationPort)
        writeU16(packet, offset + 4, udpLength)
        writeU16(packet, offset + 6, 0)       // checksum field must be zero while summing
        datagram.payload.copyInto(packet, offset + UDP_HEADER)

        var total = Checksums.sum(datagram.sourceAddress, 0, datagram.sourceAddress.size)
        total = Checksums.sum(datagram.destinationAddress, 0, datagram.destinationAddress.size, total)
        total += PROTOCOL_UDP.toLong()
        total += udpLength.toLong()
        total = Checksums.sum(packet, offset, udpLength, total)

        var checksum = Checksums.complete(total)
        // A computed checksum of zero must be transmitted as 0xFFFF (RFC 768).
        if (checksum == 0) checksum = 0xFFFF
        writeU16(packet, offset + 6, checksum)
    }
}
