# ADR 0002 — DNS filtering approach

- **Status:** Accepted (M2)
- **Date:** 2026-10-28
- **Supersedes:** nothing. Implements the open decisions in PRD section 19.

## Context

The PRD requires a local DNS filter built on `VpnService`, and explicitly forbids the
common shortcut of capturing all traffic, processing only DNS, and dropping the rest
(PRD 4.3). Section 4.4 also requires a decision between a hand-written packet parser and
a third-party TUN/DNS library, with licence, maintenance and security reviewed.

Four things had to be settled before any code: the route scope, the parser, the response
for a blocked name, and the upstream resolver.

## Decision 1 — Intercept only the DNS addresses, never a default route

The tunnel is built with:

| Entry | Value |
|---|---|
| Interface address (v4) | `10.111.222.1/32` |
| DNS server (v4) | `10.111.222.2`, route `10.111.222.2/32` |
| Interface address (v6) | `fd00:1:fd00:1:fd00:1:fd00:1/128` |
| DNS server (v6) | `fd00:1:fd00:1:fd00:1:fd00:2`, route `…::2/128` |

There is no `0.0.0.0/0` and no `::/0`. Only packets addressed to the two virtual DNS
servers ever enter the tunnel.

**Why:** the PRD's hard rule is that no wide route may exist while forwarding is
unproven. With a /32 route, a bug in the packet code cannot break general connectivity —
the failure mode is "DNS stops working", which is loud and recoverable, instead of
"connections silently die", which is not. It also means unsupported traffic (TCP, ICMP,
fragments, extension headers) is never *dropped by us*; it simply never arrives.

**Cost:** DNS-over-HTTPS, DNS-over-TLS to a third-party resolver, and apps with a
hardcoded resolver bypass the filter. That limitation already exists for any DNS filter
and is stated in the onboarding and settings screens.

## Decision 2 — Own parser, no third-party TUN/DNS library

Written from scratch: IPv4/IPv6 + UDP framing, DNS header and question parsing, response
construction, checksums.

**Why:**
- The surface actually needed is tiny. This filter never parses answers, authority or
  additional records — it decides on the QNAME and relays the rest untouched. A general
  DNS library would bring a large parser (and its CVE history) for code paths we would
  never call.
- Every line is unit-testable on the JVM without a device (see `dns/*Test.kt`).
- No new dependency means no licence, maintenance or supply-chain review, and nothing
  that can silently change behaviour between builds.

**Cost:** we own the correctness. Mitigated by rejecting anything malformed instead of
guessing, and by a strict rule that the parser never allocates from attacker-controlled
lengths.

## Decision 3 — Blocked names get NXDOMAIN (RCODE 3)

**Why:** PRD FR-021 asks for a documented RCODE and warns against fake IPs without
justification. NXDOMAIN is the standard "this name does not exist" answer: the client
stops immediately and renders its normal failure state.

Alternatives rejected:
- `0.0.0.0` / `::` (the Pi-hole "null" style): the client opens a connection to an
  address that goes nowhere, so the page hangs until it times out. Slower and no clearer.
- A fake A record pointing at a block page: requires serving HTTP, and leaks that the
  user is being filtered to anything that inspects the answer.

No SOA record is attached, so the negative answer is not cached. The blocklist is
mutable and a cached NXDOMAIN would outlive an edit.

## Decision 4 — Upstream resolver: Cloudflare 1.1.1.1, fallback 1.0.0.1

**Why:** allowed queries must still resolve, which requires a resolver the app is allowed
to use without configuration. Cloudflare publishes a privacy policy that excludes selling
or profiling DNS data, and the addresses are anycast so latency is low from Indonesia.

**Cost:** allowed queries leave the device to a third party. That is disclosed in the
settings screen. The app makes no other network request: no telemetry, no HTTP, no
backend. `INTERNET` permission exists solely for this forwarding socket.

Rejected: reading the underlying network's DNS servers. Once a VPN is up, the link
properties describe the VPN, so this needs continuous tracking of the non-VPN network —
more moving parts, and it silently changes behaviour when the user roams.

## Decision 5 — MTU 4096

The tunnel carries only DNS. A UDP answer larger than one tunnel packet cannot be written
back (this filter does not fragment in userspace), so the MTU is raised above the 1500 of
a normal link. 4096 covers every EDNS0 response a phone can request, which keeps the
truncation fallback a corner case.

When an answer still does not fit, the client receives a truncated reply (TC=1) rather
than silence, so it fails fast instead of hanging.

## Consequences

**Accepted limitations (recorded, not hidden):**

- DNS over TCP is not implemented. A response too large for UDP is truncated; the client's
  TCP retry gets no answer. Remains a documented limitation after M3 (D-08).
- DNS-over-TLS / DNS-over-HTTPS configured by the user or an app bypasses the filter.
- Private DNS set to "Automatic" on the device may attempt DoT to the virtual resolver.
  It fails and falls back to plaintext, but it is worth checking on the test device.
- Direct IP access is not filtered (no DNS query is involved).
- The service does not call `startForeground`. The system VPN notification keeps the
  process alive; this must be verified across reboot and process death in M4.
- `CONFLICT_WITH_OTHER_VPN` is detected since M3: consent denied while another VPN is
  active, or `establish()` fails with a VPN transport present. Detection needs the
  read-only `ACCESS_NETWORK_STATE` permission and opens no socket. The failed-`establish()`
  case is correlation, not proof of cause: a VPN that has just started may not yet be the
  active network, in which case the failure is reported as ERROR instead.

**Verified in M2:** build, unit tests, lint. **Not verified:** real network behaviour on a
physical device. No claim of working blocking is made until that test is run.
