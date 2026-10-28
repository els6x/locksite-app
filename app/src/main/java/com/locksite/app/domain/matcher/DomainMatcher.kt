package com.locksite.app.domain.matcher

/**
 * Domain matching by label boundary (PRD FR-020).
 *
 * Never use `query.contains(rule)` — that would make the rule `example.com` match
 * `notexample.com` and `example.com.attacker.test`.
 */
object DomainMatcher {

    /** Normalises a hostname for comparison: lowercase, no trailing dot. */
    fun normalise(hostname: String): String = hostname.trim().lowercase().removeSuffix(".")

    /**
     * @param query the hostname being resolved
     * @param rule the locked hostname
     * @param includeSubdomains whether subdomains of [rule] are also blocked
     */
    fun matches(query: String, rule: String, includeSubdomains: Boolean): Boolean {
        val q = normalise(query)
        val r = normalise(rule)
        if (q.isEmpty() || r.isEmpty()) return false
        if (q == r) return true
        return includeSubdomains && q.endsWith(".$r")
    }

    /** True when any rule in [rules] blocks [query]. */
    fun matchesAny(
        query: String,
        rules: List<Pair<String, Boolean>>,
    ): Boolean = rules.any { (hostname, includeSubdomains) ->
        matches(query, hostname, includeSubdomains)
    }
}
