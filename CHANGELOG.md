# Changelog

All notable changes to this project are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Added — M2 (VPN/DNS technical spike)

- `LockSiteVpnService`: local `VpnService` that establishes a TUN interface and answers
  DNS queries. Routes **only** the two virtual DNS addresses (`/32` and `/128`) — no
  default route, so non-DNS traffic never enters the tunnel and cannot be dropped.
- `dns/` package, pure Kotlin and unit tested without a device:
  `IpPackets` (IPv4/IPv6 + UDP parse/build with checksums), `DnsMessages` (header and
  question parsing, compression pointers accepted backwards only), `DnsResponses`
  (NXDOMAIN, SERVFAIL, FORMERR, TC), `DnsQueryHandler` (block/forward/reject/drop
  decision), `UdpDnsForwarder` (protected UDP socket, validates sender, transaction ID
  and QR bit).
- Blocked names are answered with **NXDOMAIN**, never a fake address. No SOA is attached,
  so nothing is negative-cached.
- Allowed names are relayed to `1.1.1.1` (fallback `1.0.0.1`).
- `VpnController`: live `StateFlow<ProtectionStatus>` pushed only by the service. No
  status flag is persisted anywhere.
- Dashboard: real status, and the yellow primary action becomes **Aktifkan perlindungan**
  while protection is not running (add-domain drops to a ghost button, keeping one yellow
  action per screen). System VPN consent is launched from the app shell.
- `docs/ADR/0002-dns-filtering-approach.md` records the route scope, the hand-written
  parser, the NXDOMAIN choice, the upstream resolver and the MTU.
- 54 new unit tests (82 total): packet round-trips, checksum verification, malformed
  input, matcher integration, and a real loopback UDP exchange.

### Changed

- `versionName` 0.3.0, `versionCode` 3.
- **`INTERNET` permission added.** It is required to relay allowed DNS queries. This is a
  deliberate weakening of the M0 invariant and is documented in `docs/SECURITY.md`.
- MTU is 4096 rather than 1500: the tunnel carries only DNS, and an answer larger than one
  packet cannot be written back. Oversized answers are returned as TC=1 instead of being
  dropped.
- `DashboardViewModel` now combines the blocklist flow with the live service status.

### Known limitations after M2

- **Blocking has not been verified on a real device or network.** Only the build, unit
  tests and lint are green. See the manual runbook in `docs/TEST_PLAN.md`.
- DNS over TCP is not implemented: a truncated answer makes the client retry over TCP and
  that retry gets no answer.
- DNS-over-TLS/HTTPS, other VPNs, proxies and direct IP access bypass the filter.
- `CONFLICT_WITH_OTHER_VPN` is defined but never produced.
- The service does not call `startForeground`; survival across reboot and process death is
  an M4 item.
- Light theme only; no dark variant.
- Lint: 9 warnings, 0 errors.

### Added — M1 (UI and local blocklist)

- Onboarding explaining the DNS filter, the VPN permission and the limits of the product.
- Dashboard with a truthful status field (always "not configured" until M2), locked-domain
  count and the last locked hostname.
- Add-domain flow: input → validation → permanent-lock confirmation dialog → persist.
- Locked-domain list with local search, "locked" badge and creation date. **No delete,
  edit or unblock action exists anywhere in the UI.**
- Settings: version, privacy, recovery procedure, protection limits.
- Design system matching the supplied reference: light grey canvas, white flat cards,
  pastel teal/green/yellow fields, **no shadows**, radius 16/24/28, spacing scale 4–48.
  Documented in `docs/DESIGN_SYSTEM.md`.
- Domain layer (pure Kotlin, unit tested): `DomainValidator`, `DomainMatcher`,
  `AddDomainUseCase`, `BlocklistRepository` port.
- Room persistence: `locked_domains` with a unique index on the normalised hostname,
  schema exported to `app/schemas/`. DAO has no delete/update statement.
- 28 unit tests: validation, matching (including `notexample.com` and suffix-injection
  false positives), and use-case behaviour with a fake repository.
- Adaptive launcher icon (teal background, ink padlock).

### Changed

- `versionName` 0.2.0, `versionCode` 2.
- Build: KSP enabled for Room, `buildConfig` enabled, `material-icons-core` added.
  `android.disallowKotlinSourceSets=false` is required because KSP still registers
  generated sources through the `kotlin.sourceSets` DSL that AGP 9 forbids.

### Known limitations after M1

- Light theme only; no dark variant.
- Lint: 10 warnings, 0 errors (unused resources, newer dependency versions available).

### Added — M0 (foundation)

- Android project scaffold: Kotlin + Jetpack Compose, Gradle Kotlin DSL, single `:app`
  module. Build configuration only — no product behaviour.
- Toolchain pinned: AGP 9.4.0, Gradle 9.6.0, Kotlin 2.2.10 (built-in Kotlin),
  Compose BOM 2026.09.00, `compileSdk`/`targetSdk` 37, `minSdk` 26.
- Package skeleton matching the PRD architecture: `presentation`, `domain`, `data`,
  `vpn`, `dns`, `diagnostics`, `ui/theme`.
- Documentation baseline: `AGENTS.md`, `docs/TECHNICAL_DESIGN.md`, `docs/SECURITY.md`,
  `docs/TEST_PLAN.md`, `docs/ADR/0001-platform-and-stack.md`.
- Privacy defaults in the manifest: `allowBackup=false`, cloud backup and device
  transfer excluded, **no `INTERNET` permission**.
- `PRD.md` renamed from `LockSite_PRD.md` to match the references inside the document.

### Notes

- VPN/DNS filtering was **not** implemented in M0; it arrived in M2.
- ADR 0002 (DNS filtering approach) was intentionally left open until the M2 spike; it is
  now written.
