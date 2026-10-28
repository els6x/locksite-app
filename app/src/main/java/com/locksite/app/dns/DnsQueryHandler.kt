package com.locksite.app.dns

import com.locksite.app.domain.matcher.DomainMatcher
import com.locksite.app.domain.model.LockedDomain

/**
 * Decides what to do with one DNS message.
 *
 * Pure logic: no sockets, no Android, no I/O. The service supplies the current rule
 * snapshot and performs the forwarding, which keeps this class fully unit-testable.
 */
class DnsQueryHandler(
    private val rules: () -> List<LockedDomain>,
) {

    sealed interface Decision {
        /** The QNAME is locked: answer locally, never ask upstream. */
        data class Block(val response: ByteArray) : Decision

        /** The QNAME is allowed: ask the upstream resolver and relay its answer. */
        data class Forward(val query: DnsQuery) : Decision

        /** The message cannot be used, but the sender still deserves an answer. */
        data class Reject(val response: ByteArray) : Decision

        /** Not something this filter handles. Silence is correct. */
        data object Drop : Decision
    }

    fun handle(packet: ByteArray, length: Int = packet.size): Decision {
        if (length < DnsMessages.HEADER_SIZE) return Decision.Drop

        val flags = readU16(packet, 2)
        // Never answer a message that is already a response: that is how loops start.
        if ((flags and DnsMessages.FLAG_QR) != 0) return Decision.Drop

        val query = DnsMessages.parseQuery(packet, length)
            ?: return DnsResponses.formatError(packet, length)
                ?.let { Decision.Reject(it) }
                ?: Decision.Drop

        // Only standard queries are matched against the blocklist. Anything else is
        // forwarded untouched rather than guessed at.
        if (query.opcode != DnsMessages.OPCODE_QUERY) return Decision.Forward(query)

        return if (isLocked(query.question.name)) {
            Decision.Block(DnsResponses.nameError(query))
        } else {
            Decision.Forward(query)
        }
    }

    private fun isLocked(name: String): Boolean {
        if (name.isEmpty()) return false
        return rules().any { rule ->
            DomainMatcher.matches(name, rule.hostname, rule.includeSubdomains)
        }
    }
}
