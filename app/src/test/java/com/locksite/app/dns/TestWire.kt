package com.locksite.app.dns

/** Hand-rolled wire fixtures. Built byte by byte so the tests do not depend on the codec. */
internal object TestWire {

    fun u16(data: ByteArray, offset: Int): Int =
        ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)

    fun putU16(data: ByteArray, offset: Int, value: Int) {
        data[offset] = ((value ushr 8) and 0xFF).toByte()
        data[offset + 1] = (value and 0xFF).toByte()
    }

    /** A standard one-question A/IN query. */
    fun query(name: String, id: Int = 0x1234, flags: Int = 0x0100): ByteArray {
        val labels = name.split('.').filter { it.isNotEmpty() }
        val questionSize = labels.sumOf { it.length + 1 } + 1 + 4
        val buffer = ByteArray(DnsMessages.HEADER_SIZE + questionSize)
        putU16(buffer, 0, id)
        putU16(buffer, 2, flags)
        putU16(buffer, 4, 1)

        var offset = DnsMessages.HEADER_SIZE
        for (label in labels) {
            buffer[offset] = label.length.toByte()
            label.toByteArray(Charsets.US_ASCII).copyInto(buffer, offset + 1)
            offset += label.length + 1
        }
        buffer[offset] = 0
        offset += 1
        putU16(buffer, offset, 1)          // QTYPE A
        putU16(buffer, offset + 2, 1)      // QCLASS IN
        return buffer
    }

    fun ipv4(a: Int, b: Int, c: Int, d: Int): ByteArray =
        byteArrayOf(a.toByte(), b.toByte(), c.toByte(), d.toByte())

    fun ipv6(vararg groups: Int): ByteArray {
        val bytes = ByteArray(16)
        groups.forEachIndexed { index, group ->
            bytes[index * 2] = ((group ushr 8) and 0xFF).toByte()
            bytes[index * 2 + 1] = (group and 0xFF).toByte()
        }
        return bytes
    }

    /** Independent one's complement sum, used to verify the production checksums. */
    fun checksumSum(vararg chunks: ByteArray): Int {
        var sum = 0L
        for (chunk in chunks) {
            var index = 0
            while (index + 1 < chunk.size) {
                sum += (((chunk[index].toInt() and 0xFF) shl 8) or (chunk[index + 1].toInt() and 0xFF)).toLong()
                index += 2
            }
            if (index < chunk.size) {
                sum += ((chunk[index].toInt() and 0xFF) shl 8).toLong()
            }
        }
        while ((sum shr 16) != 0L) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        return sum.toInt() and 0xFFFF
    }
}
