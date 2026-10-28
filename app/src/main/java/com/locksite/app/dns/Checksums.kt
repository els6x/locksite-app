package com.locksite.app.dns

/**
 * One's complement arithmetic used by the IPv4 header checksum and the UDP checksum
 * (RFC 1071). Kept in a single place so both packet builders share the same code path.
 */
internal object Checksums {

    /** Adds the 16-bit words of [length] bytes starting at [offset] to [initial]. */
    fun sum(data: ByteArray, offset: Int, length: Int, initial: Long = 0L): Long {
        var total = initial
        var index = offset
        val end = offset + length
        while (index + 1 < end) {
            total += (((data[index].toInt() and 0xFF) shl 8) or (data[index + 1].toInt() and 0xFF)).toLong()
            index += 2
        }
        if (index < end) {
            // Odd trailing byte is padded with a zero byte on the right.
            total += ((data[index].toInt() and 0xFF) shl 8).toLong()
        }
        return total
    }

    /** Folds carries back into the low 16 bits. */
    fun fold(total: Long): Int {
        var value = total
        while ((value shr 16) != 0L) {
            value = (value and 0xFFFF) + (value shr 16)
        }
        return value.toInt() and 0xFFFF
    }

    /** The value written into a checksum field: folded sum, inverted. */
    fun complete(total: Long): Int = fold(total).inv() and 0xFFFF
}
