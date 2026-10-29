# Changelog

All notable changes to this project are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Added — M6 (managed-device evaluation) and UI polish

- **M6 evaluated, not enabled.** `docs/ADR/0003-managed-device-mode.md` records why Device
  Owner / managed-device hardening is not implemented: it would restrict uninstall and VPN
  settings (forbidden by the project rules), needs factory-reset provisioning, and cannot be
  tested here. No `DevicePolicyManager` code or device-admin receiver was added.
- **UI polish.** The two dashboard stat cards now share one height, the locked-count number is
  smaller, card radii are unified (16 controls / 24 cards / full pills; `Radius.large`
  removed), and every clickable clips its ripple to its own shape, so a tap no longer paints a
  grey rectangle.
- **Light motion.** Bottom-bar selection fades between colours, screens fade with a small
  slide, and the locked count animates when it changes. No palette or layout change.
- **Docs trimmed for publication.** `docs/DEVICE_NOTES.md` (contained an ADB serial) and
  `docs/BETA_CHECKLIST.md` were removed; all references were updated.
- **Hide locked domain names.** A Settings switch masks every locked name on the Home and
  Locked-list screens (`instagram.com` → `•••••••••.•••`). The stored hostname, the filter and
  search keep using the real name; only the display changes. The preference is local and
  persists across launches. 7 new unit tests (`DomainMaskerTest`).
- **MIT licence.** Added `LICENSE` (MIT) and a licence section in the README.
- **Strict Private DNS guard.** Found on a real device: with Private DNS set to a hostname
  (`dns.adguard.com`), the filter claimed to be active while every DNS lookup failed, because
  the resolver refuses to fall back from DNS-over-TLS. The service now reads
  `private_dns_mode` and refuses to start when it is `hostname`, publishing an explanation
  instead of breaking the phone's DNS. 4 new unit tests (`PrivateDnsTest`).
- **Live blocking verified on a device (D-01…D-04).** With Private DNS set to Automatic, every
  locked name and a subdomain returned NXDOMAIN while `example.com`, `cloudflare.com` and a
  look-alike resolved. This is the first end-to-end blocking result; reboot, network changes,
  force-stop, another VPN and a corrupted database remain untested.

### Changed — M6

- `versionName` 0.8.0, `versionCode` 9.
- History rewritten to remove `docs/DEVICE_NOTES.md` from every commit (it contained a device
  serial). A pre-rewrite bundle was kept outside the repository.

### Added — M5 (private beta)

- **Recovery is shown before protection is enabled.** While protection is not running, the
  dashboard displays a *Cara menghentikan perlindungan* card: stop it from Android VPN
  settings or by force-stopping the app, stopping does not delete locked domains, and it does
  not start by itself after a reboot. This closes the M5 exit criterion.
- **Beta documentation:** `docs/RELEASE_NOTES.md` and `docs/KNOWN_LIMITATIONS.md`.
- No change to the DNS filter, blocklist or tunnel behaviour.

### Changed — M5

- `versionName` 0.6.0, `versionCode` 7.
- Verified on a real device for the first time: the APK installs and launches, and GitHub
  Mobile reports **online** while protection is active (compatibility bypass). Blocking end to
  end on a live network is still **not verified** — see `docs/TEST_PLAN.md`.

### Known limitations after M5

- Blocking a domain on a live network, reboot, Wi-Fi ↔ cellular, force-stop, another VPN and a
  corrupted database remain untested (D-01…D-10, I-01…I-09).
- The beta APK is debug-signed and internal-only.
- DNS over TCP is not implemented; DoH/DoT, other VPNs, proxies and direct IP access bypass
  the filter; compatibility-bypassed apps are not filtered.
- The bypass package list is hardcoded, not user-configurable.

### Fixed — M4.1 (compatibility bypass)

- **Apps that report "offline" while protection is active.** Android validates a VPN network
  by probing it; a DNS-only tunnel has no route to answer that probe, so
  `NET_CAPABILITY_VALIDATED` is absent and apps such as GitHub Mobile show offline.
  `setUnderlyingNetworks` does not change that probe on every device. GitHub Mobile
  (`com.github.android`) is now excluded from the tunnel via
  `VpnService.Builder.addDisallowedApplication`, so it uses the physical network and reports
  online. Its DNS is no longer filtered; every other app is unaffected.
- `versionName` 0.5.1, `versionCode` 6.

### Added — M4 (resilience and recovery)

- **Connectivity-validation fix.** The tunnel now declares the physical network underneath
  it (`VpnService.Builder.setUnderlyingNetworks`) and refreshes it through a
  `NetworkCallback` when the connection changes. Apps that gate on
  `NET_CAPABILITY_VALIDATED` — GitHub Mobile is the concrete case — are addressed in code;
  the outcome is **unverified on a device**. The tunnel still routes only DNS, so the PRD
  route rule is preserved.
- **Pure underlying-network selector** (`vpn/UnderlyingNetworks.kt`): never chooses a VPN
  transport, requires internet capability, prefers a validated network, deterministic ties.
  8 new unit tests (103 total).
- **`docs/RECOVERY.md`** — restart, process death, force-stop, uninstall and always-on VPN,
  with what the app will and will not do.
- **`docs/DEPENDENCIES.md`** — dependency audit with versions, licences and maintenance.

### Changed

- `versionName` 0.5.0, `versionCode` 5.
- Privacy/logging review: no `Log.*` call exists in `app/src/main`; the logging rules in
  `docs/SECURITY.md` hold by construction.
- `docs/TECHNICAL_DESIGN.md` §5 documents the underlying-network behaviour and the
  network-change path.

### Known limitations after M4

- **Device and network behaviour is still unverified.** Restart, Wi-Fi ↔ cellular, process
  death and the connectivity fix all need a physical phone; the runbook is in
  `docs/TEST_PLAN.md` (D-01…D-10, I-01…I-09).
- DNS over TCP is not implemented: a truncated answer makes the client retry over TCP and
  that retry gets no answer.
- DNS-over-TLS/HTTPS, other VPNs, proxies and direct IP access bypass the filter.
- The service does not call `startForeground`. The system VPN notification keeps it alive;
  if the process is killed, the status resets truthfully to "not active" and the user must
  re-activate (`docs/RECOVERY.md`).
- Light theme only; no dark variant.

### Added — M3 (filter integration and diagnostics)

- **Blocklist → matcher wiring is exercised, not assumed.** The service takes a snapshot at
  start and follows `observeAll()` afterwards; `DnsQueryHandler` matches every QNAME against
  the live rules by label boundary.
- **`CONFLICT_WITH_OTHER_VPN` is now produced.** Denying consent while another VPN runs, or
  a failed `establish()` with a VPN transport present, reports a conflict instead of a
  generic tunnel error. `VpnController.refreshConflictState()` clears a stale conflict once
  the other VPN is gone.
- **Diagnostics screen (FR-031).** Real service state, VPN consent, whether another VPN
  owns the tunnel, the locked-domain count (unknown — never 0 — when the read fails), a
  self-test result and the last check time. No browsing history is shown.
- **`DiagnosticsSelfTest`** — pure and in-process: builds a wire query, runs it through the
  real handler with the live rules, and checks that a locked name is blocked while a
  reserved `.invalid` probe is forwarded. It opens no socket.
- **`ProtectionStatusText`** — one exhaustive status → string mapping, unit tested, so the
  six states cannot drift between screens.
- 13 new unit tests (95 total).

### Changed

- `versionName` 0.4.0, `versionCode` 4.
- **`ACCESS_NETWORK_STATE` permission added** for conflict detection. Read-only; documented
  in `docs/SECURITY.md`.
- Settings links to the new Diagnostics screen; the shared `InfoCard` moved to
  `ui/components`.

### Known limitations after M3

- **Blocking is still unverified on a real device or network.** Only build, unit tests and
  lint are green.
- DNS over TCP is not implemented: a truncated answer makes the client retry over TCP and
  that retry gets no answer.
- DNS-over-TLS/HTTPS, other VPNs, proxies and direct IP access bypass the filter. A web
  proxy such as CroxyProxy is a concrete example: the phone only resolves the proxy host,
  so the blocked domain never reaches the filter.
- The service does not call `startForeground`; survival across reboot and process death is
  an M4 item.
- **Some apps report "offline" while the VPN is active even though internet works.** Android
  validates the VPN network (no internet route), so `NET_CAPABILITY_VALIDATED` is absent and
  apps that gate on it (e.g. GitHub Mobile) show offline. Fix planned for M4 via
  `VpnService.Builder.setUnderlyingNetworks()` — without capturing all traffic.
- Light theme only; no dark variant.

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
