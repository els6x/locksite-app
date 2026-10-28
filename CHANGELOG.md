# Changelog

All notable changes to this project are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

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

- Protection status is hard-coded to "not configured": there is no VPN service yet (M2).
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

- VPN/DNS filtering is **not** implemented. The placeholder screen states this explicitly.
- ADR 0002 (DNS filtering approach) is intentionally not written yet; it depends on the
  M2 technical spike.
