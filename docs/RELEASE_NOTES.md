# Release notes — LockSite

## 0.8.0 — MVP feature-complete (beta)

- **Build:** debug APK (`app-debug.apk`), `applicationId` `com.locksite.app.debug`,
  `versionName` `0.8.0-debug`, `versionCode` 9.
- **Distribution:** internal only. The APK is signed with the debug keystore, so it must not
  be published to a store and cannot be upgraded in place by a differently signed build.
- **Platform:** Android 8.0 (API 26) and newer. Verified on one Android 13 device only.

### What this build adds

- **Hide locked domain names.** A switch in Settings masks every locked name on the Home and
  Locked-list screens (`instagram.com` → `•••••••••.•••`). The stored hostname, the filter and
  search still use the real name; only the drawing changes. The preference is local and
  persists across launches.
- **MIT licence.** The project is released under the MIT licence (`LICENSE`), so others may
  use, modify and redistribute it, including commercially, provided the copyright and licence
  notice are kept.
- **Private DNS guard.** If Android's Private DNS is set to a strict hostname
  (DNS-over-TLS), a DNS-only filter cannot work: the resolver never falls back to plaintext
  and every lookup fails. The app now detects this and **refuses to activate** with an
  explanation, instead of breaking DNS while claiming to be active. Set Private DNS to Off or
  Automatic to use the filter.
- **M6 — managed-device evaluation.** Device Owner / managed-device hardening was evaluated and
  is **deliberately not enabled**. Restricting uninstall or VPN settings would conflict with the
  project's rule not to weaken Android's recovery paths and would require factory-reset
  provisioning. See `docs/ADR/0003-managed-device-mode.md`. No `DevicePolicyManager` code was
  added.
- **UI polish.** The two dashboard stat cards now share one height; the locked-count number is
  smaller; radii are unified (16 controls / 24 cards / full pills); and every clickable clips
  its ripple to its own shape, so tapping no longer paints a grey rectangle.
- **Light motion.** Bottom-bar selection fades between colours, screens fade with a small
  slide, and the locked count animates when it changes. The palette and layout are unchanged.
- **Recovery before activation** (from 0.6.0). While protection is not running, the dashboard
  explains how to stop it, that stopping does not delete locked domains, and that it does not
  start by itself after a reboot.

### What is verified

| Item | Evidence |
|---|---|
| Build, unit tests, lint | `assembleDebug` successful; 114 unit tests, 0 failures; lint 0 errors |
| App installs, launches and renders on a real phone | Android 13 (SDK 33), arm64-v8a; screenshot checked |
| GitHub Mobile no longer reports "offline" while protection is active | Manual observation after the compatibility bypass (`com.github.android` excluded from the tunnel) |
| Live DNS blocking on a real network | **pass** — all 4 locked names and a subdomain returned NXDOMAIN; `example.com`, `cloudflare.com` and a look-alike resolved (Private DNS set to Automatic) |
| Strict Private DNS is refused, not ignored | **pass** — activation refused with an explanation; DNS kept working |
| Rules persist across process restart | Room persistence, covered by unit tests |

### What is **not** verified

- Behaviour across reboot, Wi-Fi ↔ cellular change, force-stop, another VPN, and a corrupted
  database.
- Malformed-query handling on a device (D-05).
- Any device other than the one Android 13 phone used for the beta.

Do not describe blocking as "working end to end" on the strength of this build alone.

### Install

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Stop protection / recover

1. Android Settings → Network & internet → VPN → **LockSite** → turn off or disconnect.
2. If the app looks stuck: Android Settings → Apps → LockSite → **Force stop**.
3. Locked domains are not removed by either step. Full procedures: `docs/RECOVERY.md`.

### Known limitations

See `docs/KNOWN_LIMITATIONS.md`. The short version: DNS filtering is not a guarantee; DNS over
TCP, DNS-over-HTTPS/TLS, other VPNs, proxies and direct-IP access can bypass it, and the
compatibility-bypassed apps are not filtered. Managed-device hardening is not implemented.
