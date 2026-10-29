# Beta test checklist — LockSite 0.6.0

Manual acceptance checklist for the private beta. Every item has an **expected result** and a
**record** column. Leave an item blank only if it was not run; write "not run" rather than
implying a pass.

- **Build under test:** `app-debug.apk`, `versionName` `0.6.0-debug`, `versionCode` 7.
- **Device:** OPPO CPH2217, Android 13 (SDK 33), arm64-v8a — see `docs/DEVICE_NOTES.md`.
- **Test domains:** use a domain you control (e.g. `blocked.example.com`) and a normal site
  you do not mind loading. Never use a third-party site as a block target.

## A. Install and launch

| # | Step | Expected | Record |
|---|---|---|---|
| A-1 | `adb install -r app-debug.apk` | `Success` | |
| A-2 | Launch from the launcher | Onboarding opens on a **clean** install; no crash | |
| A-3 | Re-launch after finishing onboarding | Dashboard opens directly | |

`adb install -r` keeps existing app data. To see onboarding again, uninstall first or clear
app data (see section K).

## B. Onboarding

| # | Step | Expected | Record |
|---|---|---|---|
| B-1 | Read the three points | Explains the DNS filter, the VPN permission, and that it is not absolute | |
| B-2 | Check the footnote | States that the connection can be restored from Android VPN settings | |

## C. Add a domain

| # | Step | Expected | Record |
|---|---|---|---|
| C-1 | Add `blocked.example.com` | Confirmation dialog warns the lock is permanent | |
| C-2 | Confirm | Domain appears in **Domain terkunci**; count on dashboard increments | |
| C-3 | Try to add the same domain again | Rejected as "already locked" | |
| C-4 | Look for delete/edit/unblock | **No such action exists anywhere** | |

## D. Activate protection

| # | Step | Expected | Record |
|---|---|---|---|
| D-1 | Tap **Aktifkan perlindungan** | Android VPN consent dialog appears | |
| D-2 | Accept | Dashboard shows **Perlindungan aktif**; Android VPN key icon appears | |
| D-3 | Check the dashboard while inactive (before D-1) | A **Cara menghentikan perlindungan** card is visible | |
| D-4 | On a fresh install, tap **Aktifkan perlindungan** and deny the system dialog | Status stays **Belum aktif**, no crash | |

## E. Blocking (DNS)

`nslookup` does **not** exist in the Android shell (verified on Android 13 / API 33). Use
`ping`, which resolves through the system resolver: `ping: unknown host <name>` means
resolution failed, `PING <name> (<ip>)` means it resolved.

**Precondition (E-0):** the domain you block must be a real name that resolves when protection
is off. A made-up name such as `blocked.example.com` prints `unknown host` even without the
filter, so it proves nothing.

| # | Step | Expected | Record |
|---|---|---|---|
| E-0 | Protection **off**: `adb shell ping -c 1 -W 2 <your-domain>` | `PING <your-domain> (<ip>)` — the domain is a valid target | |
| E-1 | Protection **on**: `adb shell ping -c 1 -W 2 <your-domain>` | `ping: unknown host <your-domain>` | |
| E-2 | `adb shell ping -c 1 -W 2 www.<your-domain>` | `ping: unknown host` (subdomain policy) | |
| E-3 | `adb shell ping -c 1 -W 2 example.com` | `PING example.com (<ip>)` — allowed name still resolves | |
| E-4 | Only if you control a look-alike name: `adb shell ping -c 1 -W 2 <look-alike>` | Resolves — a look-alike is not matched (skip and write "not run" otherwise) | |
| E-5 | Load a normal site in a browser | Still works — no route was widened | |
| E-6 | Optional: send a malformed DNS query | Service does not crash (needs a custom DNS client) | |

## F. Connectivity compatibility

| # | Step | Expected | Record |
|---|---|---|---|
| F-1 | Open GitHub Mobile while protection is active | Shows **online**, not offline | |
| F-2 | Open any other app that checks connectivity | Behaves normally | |

## G. Diagnostics

| # | Step | Expected | Record |
|---|---|---|---|
| G-1 | Open **Pengaturan → Buka diagnostik** | Service status, VPN permission, other-VPN fact, locked count are shown | |
| G-2 | Tap **Jalankan tes filter** | A locked probe is rejected; an allowed probe is forwarded | |
| G-3 | With an empty blocklist | Result says **Tidak diuji**, never a false **Lulus** | |
| G-4 | Double-tap the test button | Only one run starts | |

## H. Conflict with another VPN

| # | Step | Expected | Record |
|---|---|---|---|
| H-1 | Start another VPN, then activate LockSite | Status reports **Bentrok dengan VPN lain** | |
| H-2 | Turn the other VPN off, return to LockSite | Conflict clears on resume | |

## I. Recovery

| # | Step | Expected | Record |
|---|---|---|---|
| I-1 | Turn LockSite off in Android VPN settings | Dashboard shows **Dihentikan** | |
| I-2 | Confirm the blocklist is unchanged | Domains still listed | |
| I-3 | Force-stop the app, reopen | Status shows **Belum aktif**, not a stale "active" | |
| I-4 | Re-activate | Protection returns | |

## J. Restart and network change

| # | Step | Expected | Record |
|---|---|---|---|
| J-1 | Reboot the phone, reopen the app | Rules persist; status **Belum aktif** (no auto-start) | |
| J-2 | With protection active, switch Wi-Fi ↔ mobile data | Filtering continues, or the app reports a failure — it must not silently claim success | |

## K. Uninstall / clear data

| # | Step | Expected | Record |
|---|---|---|---|
| K-1 | Clear app data | Blocklist is gone; app returns to onboarding | |
| K-2 | Uninstall | App and its VPN configuration are removed | |

## Sign-off

- Tested by: ______________________
- Date: ______________________
- Device / Android: ______________________
- Items run / passed: ______ / ______
- Items not run (list): ______________________
