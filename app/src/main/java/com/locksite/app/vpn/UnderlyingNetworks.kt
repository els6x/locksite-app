package com.locksite.app.vpn

/**
 * A physical network candidate for the tunnel's underlying network.
 *
 * Deliberately plain data: [android.net.Network] and [android.net.NetworkCapabilities]
 * cannot be built in a JVM unit test, so the choice itself is expressed over this type and
 * tested without a device.
 */
internal data class UnderlyingCandidate(
    val id: Int,
    val isVpn: Boolean,
    val hasInternet: Boolean,
    val isValidated: Boolean,
    val isActive: Boolean,
)

/**
 * Picks the network the tunnel should declare as its underlying network.
 *
 * Android derives a VPN network's validated state from the network underneath it. A DNS-only
 * tunnel has no route to the internet, so without an explicit underlying network the VPN can
 * be reported as unvalidated and apps that gate on `NET_CAPABILITY_VALIDATED` — GitHub
 * Mobile is a concrete example — show "offline" even though internet works (ADR 0002).
 *
 * Rules, in priority order:
 * 1. A VPN transport is never chosen; pinning the tunnel to itself would be a loop.
 * 2. Only networks with internet capability qualify.
 * 3. The active network wins. At `establish()` time the active network is still the physical
 *    link, so this picks the link the device is actually using.
 * 4. A validated network wins over an unvalidated one.
 * 5. Ties are broken by [UnderlyingCandidate.id] so the result is deterministic.
 *
 * `null` means "keep the platform default", which is the only honest answer when no
 * physical network is available.
 */
internal fun selectUnderlying(candidates: List<UnderlyingCandidate>): UnderlyingCandidate? =
    candidates
        .asSequence()
        .filter { !it.isVpn && it.hasInternet }
        .sortedWith(
            compareByDescending<UnderlyingCandidate> { it.isActive }
                .thenByDescending { it.isValidated }
                .thenBy { it.id },
        )
        .firstOrNull()
