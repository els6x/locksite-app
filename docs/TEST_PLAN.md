# Test Plan — LockSite

Status: M0. Only the build itself is verified. Test cases below are the target set; each
row states the milestone that introduces it.

## 1. Unit tests (JVM, no device)

| ID | Case | Milestone |
|---|---|---|
| U-01 | Valid hostnames accepted (`example.com`, `a.b.co.id`) | M1 |
| U-02 | Invalid rejected: empty, spaces, `https://`, path, query, port | M1 |
| U-03 | Normalisation: uppercase → lowercase, trailing dot removed | M1 |
| U-04 | Duplicate domain rejected with "already locked" | M1 |
| U-05 | Exact match: `example.com` matches `example.com` | M1 |
| U-06 | Subdomain match: `a.example.com` matches rule `example.com` | M1 |
| U-07 | False positive guard: `notexample.com` does **not** match | M1 |
| U-08 | Suffix guard: `example.com.attacker.test` does **not** match | M1 |
| U-09 | IDN policy behaves as documented | M1 |
| U-10 | Repository insert is transactional; failure leaves no partial row | M1 |
| U-11 | Service state → UI status mapping (all six states) | M3 |

## 2. DNS tests (device / instrumentation)

| ID | Case | Milestone |
|---|---|---|
| D-01 | Blocked domain returns the documented denial response | M2 |
| D-02 | Allowed domain still resolves | M2 |
| D-03 | Subdomain blocked according to policy | M2 |
| D-04 | Look-alike domain is not blocked | M2 |
| D-05 | Malformed query does not crash the service | M2 |
| D-06 | Resolver timeout yields a handled error, not a hang | M3 |
| D-07 | Invalid/spoofed response is rejected | M3 |
| D-08 | DNS over TCP handled or documented as unsupported | M3 |
| D-09 | Network change (Wi-Fi ↔ mobile data) keeps filtering or reports failure | M4 |

Use domains you control or a test domain reserved for this purpose. Never use third-party
sites as block targets.

## 3. Android integration tests

| ID | Case | Milestone |
|---|---|---|
| I-01 | VPN permission granted → service starts, status Active | M2 |
| I-02 | VPN permission denied → status stays NotConfigured, no crash | M2 |
| I-03 | Service stop → status Stopped | M2 |
| I-04 | App closed and reopened → rules persist, status re-checked | M1/M3 |
| I-05 | Device reboot → rules persist | M4 |
| I-06 | Another VPN active → conflict reported | M4 |
| I-07 | Force-stop by user → status not shown as active | M4 |
| I-08 | Corrupted database / failed migration → error state, not "empty = safe" | M4 |
| I-09 | Recovery via Android VPN settings works | M4 |

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
