package com.locksite.app.vpn

/**
 * Everything that defines the shape of the tunnel.
 *
 * The key property of this configuration: the interface owns one address and routes
 * **only the two DNS server addresses**. No `0.0.0.0/0`, no `::/0`. Non-DNS traffic is
 * therefore never handed to the app at all, so a bug in the packet code cannot break
 * general connectivity — the worst case is that DNS stops working, which is visible and
 * recoverable instead of silent data loss.
 */
object VpnConfig {

    /**
     * Larger than a normal 1500-byte link MTU on purpose.
     *
     * The tunnel only ever carries DNS, and an answer that does not fit in one write is a
     * packet this filter cannot deliver (it does not fragment in userspace). 4096 covers
     * every EDNS0 response a phone can ask for, so the truncation fallback stays a corner
     * case instead of a common one.
     */
    const val MTU = 4096

    /** Largest DNS message that still fits in one IP packet on this tunnel. */
    const val MAX_DNS_PAYLOAD = MTU - 64

    /** Address assigned to the TUN interface itself. */
    const val IPV4_INTERFACE = "10.111.222.1"
    const val IPV4_INTERFACE_PREFIX = 32

    /** Virtual DNS server the system is told to use; packets to it land in our TUN. */
    const val IPV4_DNS = "10.111.222.2"
    const val IPV4_DNS_PREFIX = 32

    const val IPV6_INTERFACE = "fd00:1:fd00:1:fd00:1:fd00:1"
    const val IPV6_INTERFACE_PREFIX = 128

    const val IPV6_DNS = "fd00:1:fd00:1:fd00:1:fd00:2"
    const val IPV6_DNS_PREFIX = 128

    const val DNS_PORT = 53

    /**
     * Upstream resolvers for allowed queries. Cloudflare's public resolver is used
     * because it publishes a privacy policy that excludes selling or profiling DNS data.
     * The second entry is only tried when the first one times out.
     */
    val UPSTREAM_RESOLVERS = listOf("1.1.1.1", "1.0.0.1")

    const val UPSTREAM_TIMEOUT_MS = 3000

    /**
     * Apps excluded from the tunnel because a DNS-only VPN makes them report "offline".
     *
     * Android validates a VPN network by probing it. This tunnel has no internet route, so
     * the probe fails and `NET_CAPABILITY_VALIDATED` is absent. Apps that gate on it — GitHub
     * Mobile is the known case — show "offline" even though the underlying internet works.
     * `setUnderlyingNetworks` does not change that probe on every device, so these packages
     * are excluded entirely and use the physical network instead.
     *
     * Consequence: their DNS is not filtered. That is acceptable because they are not block
     * targets; a domain blocked in the browser is still blocked for every other app.
     */
    val COMPATIBILITY_BYPASS_PACKAGES = listOf("com.github.android")

    /** Bounds how many lookups can be in flight before the read loop applies backpressure. */
    const val MAX_CONCURRENT_QUERIES = 8
}
