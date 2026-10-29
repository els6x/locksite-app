# Release notes — LockSite

## 0.6.0 — private beta (M5)

- **Build:** debug APK (`app-debug.apk`), `applicationId` `com.locksite.app.debug`,
  `versionName` `0.6.0-debug`, `versionCode` 7.
- **Distribution:** internal only. The APK is signed with the debug keystore, so it must not
  be published to a store and cannot be upgraded in place by a differently signed build.
- **Platform:** Android 8.0 (API 26) and newer. Verified on one device only — see
  `docs/DEVICE_NOTES.md`.

### What this build adds

- **Recovery is shown before protection is enabled.** While protection is not running, the
  dashboard displays how to stop it (Android VPN settings or force-stop), that stopping does
  not delete locked domains, and that protection does not start by itself after a reboot.
  This closes the M5 exit criterion "the user understands recovery before enabling
  protection".
- **Beta documentation:** a manual test checklist (`docs/BETA_CHECKLIST.md`), device and
  Android-version notes (`docs/DEVICE_NOTES.md`), and an explicit limitations list
  (`docs/KNOWN_LIMITATIONS.md`).
- No behaviour change to the DNS filter, the blocklist or the VPN tunnel in this release.

### What is verified

| Item | Evidence |
|---|---|
| Build, unit tests, lint | `assembleDebug` successful; 103 unit tests, 0 failures; lint 0 errors |
| App installs and launches on a real phone | OPPO CPH2217, Android 13 (SDK 33), arm64-v8a |
| GitHub Mobile no longer reports "offline" while protection is active | Manual observation by the device owner on the same phone after the compatibility bypass (`com.github.android` excluded from the tunnel) |
| Rules persist across process restart | Room persistence, covered by unit tests |

### What is **not** verified

- That a blocked domain actually returns the denial response on a network, and that allowed
  domains still resolve. These need the manual runbook (D-01…D-07).
- Behaviour across reboot, Wi-Fi ↔ cellular change, force-stop, another VPN, and a corrupted
  database (I-05…I-09).
- Any device other than the one listed in `docs/DEVICE_NOTES.md`.

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
compatibility-bypassed apps are not filtered.
