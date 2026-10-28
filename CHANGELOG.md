# Changelog

All notable changes to this project are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

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
