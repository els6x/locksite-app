# Test Plan — LockSite

Status: M6. The pure DNS/packet layer, the filter wiring and the underlying-network choice
are covered by JVM unit tests. Everything that requires a real network or a real device is
still open and is marked as such.

## 1. Unit tests (JVM, no device)

| ID | Case | Milestone | Status |
|---|---|---|---|
| U-01 | Valid hostnames accepted (`example.com`, `a.b.co.id`) | M1 | pass |
| U-02 | Invalid rejected: empty, spaces, `https://`, path, query, port | M1 | pass |
| U-03 | Normalisation: uppercase → lowercase, trailing dot removed | M1 | pass |
| U-04 | Duplicate domain rejected with "already locked" | M1 | pass |
| U-05 | Exact match: `example.com` matches `example.com` | M1 | pass |
| U-06 | Subdomain match: `a.example.com` matches rule `example.com` | M1 | pass |
| U-07 | False positive guard: `notexample.com` does **not** match | M1 | pass |
| U-08 | Suffix guard: `example.com.attacker.test` does **not** match | M1 | pass |
| U-09 | IDN policy behaves as documented | M1 | pass |
| U-10 | Repository insert is transactional; failure leaves no partial row | M1 | pass |
| U-11 | Service state → UI status mapping (all six states) | M3 | pass |
| U-12 | DNS header/question parsing, including compression pointers | M2 | pass |
| U-13 | Malformed DNS input rejected (truncated, reserved label, over-long name) | M2 | pass |
| U-14 | NXDOMAIN reply echoes the question and carries no answer record | M2 | pass |
| U-15 | IPv4 and IPv6 UDP round-trip preserves every field | M2 | pass |
| U-16 | IPv4 header checksum and UDP checksums verify independently | M2 | pass |
| U-17 | Blocked name answered locally; allowed name forwarded | M2 | pass |
| U-18 | Forwarder validates transaction ID, QR bit and sender | M2 | pass |
| U-19 | Forwarder times out instead of hanging | M2 | pass |
| U-20 | Diagnostics self-test blocks a locked probe and forwards an allowed probe | M3 | pass |
| U-21 | Diagnostics self-test reports failure when every candidate probe is locked | M3 | pass |
| U-22 | Every `ProtectionStatus` maps to a distinct label and a distinct note | M3 | pass |
| U-23 | Foreign-VPN resolution: own tunnel never foreign; unknown transport stays unknown | M3 | pass |
| U-24 | Underlying-network choice: never a VPN transport, prefers validated, null when none | M4 | pass |
| U-25 | Locked-name masking hides every letter/digit and keeps the dot structure | M6 | pass |
| U-26 | Strict Private DNS (`hostname`) is detected; opportunistic/off are not | M6 | pass |

## 2. DNS tests (device / instrumentation)

| ID | Case | Milestone | Status |
|---|---|---|---|
| D-01 | Blocked domain returns the documented denial response | M2 | **pass** — manual, Android 13 |
| D-02 | Allowed domain still resolves | M2 | **pass** — manual, Android 13 |
| D-03 | Subdomain blocked according to policy | M2 | **pass** — manual, Android 13 |
| D-04 | Look-alike domain is not blocked | M2 | **pass** — manual, Android 13 |
| D-05 | Malformed query does not crash the service | M2 | **not run** |
| D-06 | Resolver timeout yields SERVFAIL, not a hang | M3 | not run |
| D-07 | Invalid/spoofed response is rejected | M3 | not run |
| D-08 | DNS over TCP handled or documented as unsupported | M3 | open (documented unsupported) |
| D-09 | Network change (Wi-Fi ↔ mobile data) keeps filtering or reports failure | M4 | not run |
| D-10 | App relying on `NET_CAPABILITY_VALIDATED` (e.g. GitHub Mobile) shows online while VPN active after the `setUnderlyingNetworks` fix | M4 | not run |
| D-11 | GitHub Mobile stays online while protection is active (compatibility bypass) | M4.1 | **pass** — manual, Android 13 |
| D-12 | Strict Private DNS → app refuses to activate and DNS keeps working | M6 | **pass** — manual, Android 13 |

Use domains you control or a test domain reserved for this purpose. Never use third-party
sites as block targets. D-01…D-04 were verified with Private DNS set to **Automatic**; strict
Private DNS makes any DNS-only filter unusable (D-12).

### Manual runbook for D-01 … D-05 (requires a phone)

`nslookup` is **not** present in the Android shell (verified on Android 13 / API 33). Use
`ping`, which goes through the system resolver: a name that cannot be resolved prints
`ping: unknown host <name>`, a name that resolves prints `PING <name> (<ip>)`.

1. Pick a domain **you control that resolves normally**. This matters: a made-up name such as
   `blocked.example.com` prints `unknown host` even with the filter off, so it proves nothing.
2. With protection **off**, `adb shell ping -c 1 -W 2 <domain>` must print `PING <domain> (<ip>)`.
   If it does not, the domain is not a valid test target — choose another.
3. Install the debug APK, open the app, add that domain.
4. Tap **Aktifkan perlindungan**, accept the system VPN dialog. The dashboard must show
   **Perlindungan aktif** and the Android VPN key icon must appear.
5. `adb shell ping -c 1 -W 2 <domain>` → must now print `ping: unknown host <domain>`.
6. `adb shell ping -c 1 -W 2 www.<domain>` → `ping: unknown host` (subdomain policy).
7. `adb shell ping -c 1 -W 2 example.com` → must print `PING example.com (<ip>)` (allowed name
   still resolves).
8. Open a browser and load a normal site → must still work (no route was widened).
9. Turn the VPN off in Android settings → dashboard must show **Dihentikan**.

## 3. Android integration tests

| ID | Case | Milestone | Status |
|---|---|---|---|
| I-01 | VPN permission granted → service starts, status Active | M2 | **not run** |
| I-02 | VPN permission denied → status stays NotConfigured, no crash | M2 | **not run** |
| I-03 | Service stop → status Stopped | M2 | **not run** |
| I-04 | App closed and reopened → rules persist, status re-checked | M1/M3 | partially (rules persist, verified by Room) |
| I-05 | Device reboot → rules persist | M4 | not run |
| I-06 | Another VPN active → conflict reported | M4 | open |
| I-07 | Force-stop by user → status not shown as active | M4 | not run |
| I-08 | Corrupted database / failed migration → error state, not "empty = safe" | M4 | not run |
| I-09 | Recovery via Android VPN settings works | M4 | not run |

## 4. Acceptance criteria (MVP)

1. A valid domain can be locked.
2. No delete/unblock action exists for locked domains in the release build.
3. Rules survive process restart and device reboot.
4. DNS for a blocked test domain receives a denial response.
5. An allowed test domain still resolves through the supported path.
6. Status is truthful when the VPN is not running.
7. Recovery procedure is tested and documented.
8. No browsing data leaves the device.
9. All executed tests and their results are recorded.

## 5. Recording results

For each milestone, record: commands executed, pass/fail counts, device model and Android
version, and anything not tested. Untested items are reported as untested — never implied
as working.

### M2 result

| Item | Result |
|---|---|
| `./gradlew testDebugUnitTest` | 82 tests, 0 failures, 0 errors |
| `./gradlew assembleDebug` | BUILD SUCCESSFUL |
| `./gradlew lintDebug` | 0 errors, 9 warnings (all pre-existing or version-currency) |
| Real device / network | **not executed** — no device available, emulator offline |

### M3 result

| Item | Result |
|---|---|
| `./gradlew testDebugUnitTest` | 95 tests, 0 failures, 0 errors |
| `./gradlew assembleDebug` | BUILD SUCCESSFUL |
| `./gradlew lintDebug` | 0 errors, 9 warnings (version-currency, unused strings, KTX hint; no new M3 finding) |
| Real device / network | **not executed** — no device available, emulator offline |
| D-06 / D-07 | **not run** — need a device |
| D-08 (DNS over TCP) | documented as unsupported; not exercised |

### M4 result

| Item | Result |
|---|---|
| `./gradlew testDebugUnitTest` | 103 tests, 0 failures, 0 errors |
| `./gradlew assembleDebug` | BUILD SUCCESSFUL |
| `./gradlew lintDebug` | 0 errors, 9 warnings (all pre-existing) |
| Real device / network | **not executed** — no device available, emulator offline |
| D-09 / D-10 (network change, connectivity validation) | **not run** — need a device |
| I-05…I-09 (reboot, conflict, force-stop, corrupt DB, recovery) | **not run** — need a device |

### M5 result

| Item | Result |
|---|---|
| `./gradlew testDebugUnitTest` | 103 tests, 0 failures, 0 errors |
| `./gradlew assembleDebug` | BUILD SUCCESSFUL |
| `./gradlew lintDebug` | 0 errors, 9 warnings (all pre-existing; no new M5 finding) |
| Install and launch on a real device | **pass** — Android 13 (API 33), arm64-v8a |
| D-11 (GitHub Mobile online with protection active) | **pass** — manual observation on the same device |
| D-01…D-10, I-01…I-09 (live blocking, reboot, network change, conflict, recovery) | **not run** |
| Instrumentation tests | none exist |

The only device result recorded here is the one above. It is a manual observation, not an
automated instrumentation test, and it does not establish that blocking works end to end.

### M6 result

| Item | Result |
|---|---|
| `./gradlew testDebugUnitTest` | 114 tests, 0 failures, 0 errors |
| `./gradlew assembleDebug` | BUILD SUCCESSFUL |
| `./gradlew lintDebug` | 0 errors, 9 warnings (pre-existing) |
| M6 managed-device evaluation | documented in `docs/ADR/0003`; no code added, no device test (needs managed provisioning) |
| UI polish (card heights, radii, ripple clipping, light motion) | verified by screenshot on the device; not an automated test |
| Locked-name masking | covered by `DomainMaskerTest` (7 tests); toggle verified on the device by screenshot |
| Strict Private DNS guard | verified on the device: activation refused, status explained, DNS kept working |
| D-01…D-04 (live blocking) | **pass** — manual, Android 13: every locked name and a subdomain returned NXDOMAIN; allowed names and a look-alike resolved |
| D-05…D-10, I-01…I-09 (malformed query, network change, reboot, force-stop, conflict, corrupt DB) | **not run** |
