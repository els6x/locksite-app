# ADR 0001 — Platform and stack

- Status: Accepted
- Date: 2026-10-28
- Milestone: M0

## Context

LockSite must block user-chosen domains on a single, personally owned Android device.
Blocking has to work across browsers and apps, persist across restarts, and keep the
blocklist entirely on-device. The only mechanism an ordinary app can use for device-wide
name resolution is `VpnService` with a local TUN interface.

## Decision

1. **Native Android, Kotlin, Jetpack Compose.** No cross-platform framework: the core of
   the product is `VpnService` and packet handling, which are platform-specific anyway.
   A cross-platform UI layer would add weight without removing the hard part.
2. **Single Gradle module (`:app`)** for M0–M3, with package-level boundaries
   (`presentation`, `domain`, `data`, `vpn`, `dns`, `diagnostics`). Multi-module
   splitting is deferred until a boundary actually needs enforcing at build time.
3. **Toolchain:** AGP 9.4.0, Gradle 9.6.0, JDK 17+ (25 in use), Kotlin 2.2.10 (built-in
   Kotlin from AGP), Compose BOM 2026.09.00, `compileSdk`/`targetSdk` 37, `minSdk` 26.
4. **No dependency injection framework** for MVP. Manual construction in the application
   class keeps the graph readable for a small app.
5. **No `INTERNET` permission** in the M0 baseline. It is added only if a milestone
   proves it is required, together with a privacy review.
6. **Package id `com.locksite.app`.** The app is for personal use, not published to
   Google Play, so Play's `targetSdk` submission rule is not a constraint; the value is
   still kept current for platform behaviour.

## Consequences

- `minSdk` 26 covers Android 8.0 and later; the actual test device must be confirmed
  before M2 because VPN behaviour differs across OEM builds.
- Built-in Kotlin means the project must not apply `org.jetbrains.kotlin.android`.
  Adding a KMP module later would require the Android KMP library plugin instead.
- Compose compiler version is pinned to the Kotlin version bundled by AGP (2.2.10).
  Upgrading AGP may require bumping this in `gradle/libs.versions.toml`.
- No DI framework means ViewModels are constructed explicitly; acceptable at this size,
  revisit if the graph grows past roughly a dozen objects.

## Alternatives considered

| Option | Why rejected |
|---|---|
| Flutter / React Native | Cannot implement `VpnService` packet handling without native code; adds a bridge for no benefit |
| Web/PWA | No device-wide network interception possible |
| Accessibility-service based blocker | Fragile, invasive permissions, not device-wide at the DNS layer |
| Multi-module from day one | Build complexity before any boundary is proven necessary |
