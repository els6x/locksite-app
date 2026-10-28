package com.locksite.app.dns

/** The question section of a DNS message. */
data class DnsQuestion(
    /** Presentation-format name without a trailing dot, exactly as decoded from the wire. */
    val name: String,
    val type: Int,
    val dnsClass: Int,
    /** Offset just past QCLASS: everything before it is echoed verbatim in a reply. */
    val endOffset: Int,
)

/**
 * A parsed DNS query.
 *
 * [raw] is the exact message that arrived, reused byte-for-byte when forwarding so the
 * upstream resolver sees an unmodified question (including any EDNS0 OPT record).
 */
class DnsQuery(
    val id: Int,
    val flags: Int,
    val question: DnsQuestion,
    val raw: ByteArray,
) {
    val opcode: Int get() = (flags shr 11) and 0x0F
    val recursionDesired: Boolean get() = (flags and DnsMessages.FLAG_RD) != 0
    val isResponse: Boolean get() = (flags and DnsMessages.FLAG_QR) != 0
}

/**
 * Header + question parsing only.
 *
 * Answer, authority and additional records are never interpreted: the filter decides on
 * the QNAME and forwards the rest untouched. That keeps the parser small and removes the
 * largest source of DNS parsing bugs.
 */
object DnsMessages {

    const val HEADER_SIZE = 12

    const val FLAG_QR = 0x8000
    const val FLAG_RD = 0x0100
    const val FLAG_RA = 0x0080

    const val OPCODE_QUERY = 0

    const val RCODE_FORMAT_ERROR = 1
    const val RCODE_SERVER_FAILURE = 2
    const val RCODE_NAME_ERROR = 3

    private const val MAX_NAME_LENGTH = 255
    private const val MAX_POINTER_JUMPS = 16
    private const val LABEL_MASK = 0xC0

    /** Returns `null` when [data] is not a well-formed single-question query. */
    fun parseQuery(data: ByteArray, length: Int = data.size): DnsQuery? {
        if (length < HEADER_SIZE) return null

        val flags = readU16(data, 2)
        if ((flags and FLAG_QR) != 0) return null          // a response, not a query
        if (readU16(data, 4) != 1) return null             // exactly one question expected

        val (name, nameEnd) = readName(data, HEADER_SIZE, length) ?: return null
        if (nameEnd + 4 > length) return null

        return DnsQuery(
            id = readU16(data, 0),
            flags = flags,
            question = DnsQuestion(
                name = name,
                type = readU16(data, nameEnd),
                dnsClass = readU16(data, nameEnd + 2),
                endOffset = nameEnd + 4,
            ),
            raw = data.copyOfRange(0, length),
        )
    }

    /**
     * Decodes a (possibly compressed) name.
     *
     * Compression pointers are only accepted when they point backwards, which makes a
     * pointer loop impossible and bounds the work for hostile input.
     *
     * @return the decoded name and the offset just past the name in the *original* stream
     */
    internal fun readName(data: ByteArray, start: Int, limit: Int): Pair<String, Int>? {
        val builder = StringBuilder()
        var offset = start
        var endOffset = -1
        var jumps = 0
        var wireLength = 0

        while (true) {
            if (offset >= limit) return null
            val lengthByte = data[offset].toInt() and 0xFF

            if (lengthByte == 0) {
                if (endOffset < 0) endOffset = offset + 1
                break
            }

            if ((lengthByte and LABEL_MASK) == LABEL_MASK) {
                if (offset + 1 >= limit) return null
                val pointer = ((lengthByte and 0x3F) shl 8) or (data[offset + 1].toInt() and 0xFF)
                if (endOffset < 0) endOffset = offset + 2
                if (++jumps > MAX_POINTER_JUMPS) return null
                // Only backwards pointers are legal, and a name never starts inside the
                // fixed header, so a pointer into the header is always malformed.
                if (pointer >= offset || pointer < HEADER_SIZE) return null
                offset = pointer
                continue
            }

            if ((lengthByte and LABEL_MASK) != 0) return null  // 0x40 / 0x80 are reserved

            if (offset + 1 + lengthByte > limit) return null
            wireLength += lengthByte + 1
            if (wireLength > MAX_NAME_LENGTH) return null

            if (builder.isNotEmpty()) builder.append('.')
            // ISO-8859-1 maps every byte 1:1, so malformed non-ASCII bytes can never be
            // silently folded into a different hostname by a lossy UTF-8 decode.
            builder.append(String(data, offset + 1, lengthByte, Charsets.ISO_8859_1))
            offset += lengthByte + 1
        }

        return builder.toString() to endOffset
    }
}
