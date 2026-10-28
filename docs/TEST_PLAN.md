# Test Plan — LockSite

Status: M2. The pure DNS/packet layer is covered by JVM unit tests. Everything that
requires a real network or a real device is still open and is marked as such.

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
| U-11 | Service state → UI status mapping (all six states) | M3 | open |
| U-12 | DNS header/question parsing, including compression pointers | M2 | pass |
| U-13 | Malformed DNS input rejected (truncated, reserved label, over-long name) | M2 | pass |
| U-14 | NXDOMAIN reply echoes the question and carries no answer record | M2 | pass |
| U-15 | IPv4 and IPv6 UDP round-trip preserves every field | M2 | pass |
| U-16 | IPv4 header checksum and UDP checksums verify independently | M2 | pass |
| U-17 | Blocked name answered locally; allowed name forwarded | M2 | pass |
| U-18 | Forwarder validates transaction ID, QR bit and sender | M2 | pass |
| U-19 | Forwarder times out instead of hanging | M2 | pass |

## 2. DNS tests (device / instrumentation)

| ID | Case | Milestone | Status |
|---|---|---|---|
| D-01 | Blocked domain returns the documented denial response | M2 | **not run** |
| D-02 | Allowed domain still resolves | M2 | **not run** |
| D-03 | Subdomain blocked according to policy | M2 | **not run** |
| D-04 | Look-alike domain is not blocked | M2 | **not run** |
| D-05 | Malformed query does not crash the service | M2 | **not run** |
| D-06 | Resolver timeout yields SERVFAIL, not a hang | M3 | not run |
| D-07 | Invalid/spoofed response is rejected | M3 | not run |
| D-08 | DNS over TCP handled or documented as unsupported | M3 | open (documented unsupported) |
| D-09 | Network change (Wi-Fi ↔ mobile data) keeps filtering or reports failure | M4 | not run |

Use domains you control or a test domain reserved for this purpose. Never use third-party
sites as block targets.

### Manual runbook for D-01 … D-05 (requires a phone)

1. Install the debug APK, open the app, add a domain you control (for example
   `blocked.example.com`).
2. Tap **Aktifkan perlindungan**, accept the system VPN dialog. The dashboard must show
   **Perlindungan aktif** and the Android VPN key icon must appear.
3. `adb shell nslookup blocked.example.com` → must return NXDOMAIN.
4. `adb shell nslookup example.com` → must return a real address.
5. `adb shell nslookup www.blocked.example.com` → NXDOMAIN (subdomain policy).
6. `adb shell nslookup notblocked.example.com` → resolves.
7. Open a browser and load a normal site → must still work (no route was widened).
8. Turn the VPN off in Android settings → dashboard must show **Dihentikan**.

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
