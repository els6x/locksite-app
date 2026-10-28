package com.locksite.app.dns

/**
 * Builders for the replies this filter synthesises.
 *
 * **Blocked names are answered with NXDOMAIN (RCODE 3).** It is the standard negative
 * answer: the client stops immediately and shows its normal "site not found" state,
 * instead of connecting to a placeholder address that would then hang or leak. No fake
 * A/AAAA record is invented, so nothing downstream can treat the block as a real host.
 *
 * No SOA record is attached, which means resolvers will not negative-cache the answer.
 * That is intentional: the blocklist can change at any time and a stale cached NXDOMAIN
 * would outlive an edit.
 */
object DnsResponses {

    private const val FLAG_TC = 0x0200

    /** "This name does not exist." Used for every blocked QNAME. */
    fun nameError(query: DnsQuery): ByteArray = replyWithQuestion(query, DnsMessages.RCODE_NAME_ERROR)

    /** "The resolver could not answer." Used when the upstream lookup fails. */
    fun serverFailure(query: DnsQuery): ByteArray =
        replyWithQuestion(query, DnsMessages.RCODE_SERVER_FAILURE)

    /**
     * "The answer is too big for this transport."
     *
     * A UDP answer larger than the tunnel MTU cannot be written back as one packet. Rather
     * than dropping it (which stalls the client) or fragmenting it in userspace (which this
     * filter does not do), the client is told to retry over TCP. TCP DNS is not implemented
     * yet, so the practical effect is a fast, honest failure instead of a hang.
     */
    fun truncated(query: DnsQuery): ByteArray {
        val reply = replyWithQuestion(query, 0)
        writeU16(reply, 2, readU16(reply, 2) or FLAG_TC)
        return reply
    }

    /**
     * "I could not parse your question." Only the fixed header is echoed, because the
     * question section is exactly what could not be trusted.
     */
    fun formatError(request: ByteArray, length: Int = request.size): ByteArray? {
        if (length < DnsMessages.HEADER_SIZE) return null
        val reply = ByteArray(DnsMessages.HEADER_SIZE)
        writeU16(reply, 0, readU16(request, 0))
        writeU16(reply, 2, responseFlags(readU16(request, 2), DnsMessages.RCODE_FORMAT_ERROR))
        return reply
    }

    private fun replyWithQuestion(query: DnsQuery, rcode: Int): ByteArray {
        val questionBytes = query.raw.copyOfRange(DnsMessages.HEADER_SIZE, query.question.endOffset)
        val reply = ByteArray(DnsMessages.HEADER_SIZE + questionBytes.size)
        writeU16(reply, 0, query.id)
        writeU16(reply, 2, responseFlags(query.flags, rcode))
        writeU16(reply, 4, 1)                  // QDCOUNT: the question is echoed
        questionBytes.copyInto(reply, DnsMessages.HEADER_SIZE)
        return reply
    }

    private fun responseFlags(queryFlags: Int, rcode: Int): Int =
        DnsMessages.FLAG_QR or
            (queryFlags and 0x7800) or          // opcode is mirrored
            (queryFlags and DnsMessages.FLAG_RD) or
            DnsMessages.FLAG_RA or
            (rcode and 0x0F)
}
