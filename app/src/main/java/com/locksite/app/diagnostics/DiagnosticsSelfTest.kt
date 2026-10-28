package com.locksite.app.diagnostics

import com.locksite.app.dns.DnsMessages
import com.locksite.app.dns.DnsQueryHandler
import com.locksite.app.dns.writeU16
import com.locksite.app.domain.matcher.DomainMatcher
import com.locksite.app.domain.model.LockedDomain

/**
 * In-process self-test for the filter wiring (PRD FR-031).
 *
 * It exercises the same path a real query takes *inside* the process: build a wire query,
 * run it through [DnsQueryHandler] with the current rule snapshot, and inspect the
 * decision. No socket is opened, so the test works whether or not the VPN is running and
 * it can never send a query for a name the user locked.
 *
 * What it proves: the blocklist is readable, the matcher is wired to the handler, a locked
 * name is answered locally, and an unrelated name is forwarded.
 * What it does **not** prove: that the TUN device receives traffic. That needs a device
 * test (see `docs/TEST_PLAN.md`, D-01 … D-05).
 */
object DiagnosticsSelfTest {

    /** Reserved TLD (RFC 2606): never resolves, so a probe cannot hit a real site. */
    private const val RESERVED_TLD = "invalid"

    private const val PROBE_CANDIDATES = 32

    data class Result(
        /** Hostname of the rule that was probed, or `null` when the blocklist is empty. */
        val lockedProbe: String?,
        /** Whether the locked probe was blocked; `null` when there was nothing to probe. */
        val blockedAsExpected: Boolean?,
        val allowedProbe: String,
        val allowedAsExpected: Boolean,
        val ruleCount: Int,
    ) {
        /** True only when both the block check and the forward check actually passed. */
        val passed: Boolean
            get() = blockedAsExpected == true && allowedAsExpected

        /** True when there was no locked domain to probe, so blocking was not verified. */
        val blockCheckSkipped: Boolean
            get() = blockedAsExpected == null
    }

    fun run(rules: List<LockedDomain>): Result {
        val handler = DnsQueryHandler { rules }

        val allowedProbe = chooseAllowedProbe(rules)
        val allowedAsExpected = handler.handle(query(allowedProbe)) is DnsQueryHandler.Decision.Forward

        // Probe the oldest and the newest rule. Checking only one of them would hide a
        // hostname that fails to round-trip whenever it is not the one that was picked.
        val probes = listOfNotNull(rules.firstOrNull(), rules.lastOrNull())
            .distinctBy { it.hostname }
        val blockedAsExpected = if (probes.isEmpty()) {
            null
        } else {
            probes.all { handler.handle(query(it.hostname)) is DnsQueryHandler.Decision.Block }
        }

        return Result(
            lockedProbe = probes.firstOrNull()?.hostname,
            blockedAsExpected = blockedAsExpected,
            allowedProbe = allowedProbe,
            allowedAsExpected = allowedAsExpected,
            ruleCount = rules.size,
        )
    }

    /**
     * Picks a name that no rule matches.
     *
     * A rule always has at least two labels (the validator rejects single-label input), so
     * a fresh label under the reserved `.invalid` TLD cannot be covered by a suffix rule.
     * The fixed candidate set keeps the result deterministic. If a user really locked every
     * candidate, the last one is reused and the self-test honestly reports the block
     * instead of hiding it.
     */
    private fun chooseAllowedProbe(rules: List<LockedDomain>): String {
        val candidates = (0 until PROBE_CANDIDATES).map { "probe-$it.$RESERVED_TLD" }
        return candidates.firstOrNull { name -> !isLocked(name, rules) }
            ?: "probe-${PROBE_CANDIDATES - 1}.$RESERVED_TLD"
    }

    private fun isLocked(name: String, rules: List<LockedDomain>): Boolean =
        rules.any { DomainMatcher.matches(name, it.hostname, it.includeSubdomains) }

    private fun query(name: String, id: Int = 0x4C53): ByteArray {
        val labels = name.split('.').filter { it.isNotEmpty() }
        val size = DnsMessages.HEADER_SIZE + labels.sumOf { it.length + 1 } + 1 + 4
        val buffer = ByteArray(size)

        writeU16(buffer, 0, id)
        writeU16(buffer, 2, DnsMessages.FLAG_RD) // standard query, recursion desired
        writeU16(buffer, 4, 1)                   // QDCOUNT

        var offset = DnsMessages.HEADER_SIZE
        for (label in labels) {
            buffer[offset] = label.length.toByte()
            label.toByteArray(Charsets.US_ASCII).copyInto(buffer, offset + 1)
            offset += label.length + 1
        }
        buffer[offset] = 0
        offset += 1
        writeU16(buffer, offset, 1)     // QTYPE A
        writeU16(buffer, offset + 2, 1) // QCLASS IN
        return buffer
    }
}
