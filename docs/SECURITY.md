# Security & Privacy — LockSite

Scope: M2. This document describes the intended security posture. The DNS filter is
implemented and unit-tested, but **has not been verified on a real network** — nothing
here is a claim that blocking currently works end to end.

## 1. Privacy invariants

| Invariant | Enforcement |
|---|---|
| Blocklist never leaves the device | No account, no sync, no backend; the only socket opened is the DNS forwarder |
| `INTERNET` permission is used for one thing only | Relaying allowed DNS queries to `1.1.1.1`; no HTTP client exists in the app |
| No browsing history collected | Only aggregate block counters, never QNAME history |
| No DNS payloads in logs | Log technical status only (state changes, error codes) |
| No analytics/ads SDK | Prohibited in `AGENTS.md` |
| No cloud backup of the blocklist | `allowBackup=false`, `data_extraction_rules.xml` excludes all domains |

### Change from M0: `INTERNET` permission added

M0 shipped with no `INTERNET` permission at all, which made "the app cannot phone home"
structurally true. M2 cannot keep that: forwarding an allowed DNS query requires a socket.
The permission is now present and the invariant is weaker as a result — it is now enforced
by code review rather than by the manifest.

What still holds: there is no HTTP client, no telemetry SDK, no analytics, no backend, and
no code path that sends anything to the developer. The only outbound packets are DNS
queries to the configured resolver. Any future network use must be justified in an ADR and
reviewed here first.

## 2. Threat model (summary)

| Threat | Mitigation | Residual risk |
|---|---|---|
| Wrong domain locked by mistake | Validation, scope explanation, explicit confirmation before locking | User can still lock a too-broad domain |
| Database corruption | Read failures surface as an error state, never as "empty = safe" | Data loss possible if storage is destroyed |
| Service stopped by system | UI shows "not active"; no fake "protected" state | Background limits can stop the service |
| App uninstalled / data cleared | Documented as out of scope for an ordinary app | Bypass possible by device owner |
| DNS-over-HTTPS / other VPN / direct IP | Documented limitation in onboarding and settings | Not mitigated by DNS filtering |
| Malformed or hostile packets | Length validation, bounded parsing, compression pointers only accepted backwards, per-packet exception containment | Parser bugs could still crash the process |
| Spoofed DNS response | Transaction ID, QR bit and sender address validated before relaying | Off-path spoofing remains possible |
| One bad packet kills the tunnel | Every packet is handled inside its own failure boundary; writes are per-packet | A bug in the read loop itself would still stop filtering |

**Positioning:** the product must never claim that blocking cannot be bypassed. The
device owner always retains system-level control (VPN settings, force-stop, uninstall,
factory reset).

## 3. Network security rules

- The tunnel is local: only the two virtual DNS addresses are routed into it. No default
  route, so unsupported traffic is never dropped by this app.
- Upstream resolver choice is documented (`docs/ADR/0002`), including its privacy
  consequences and the fact that allowed queries leave the device.
- The forwarder socket is excluded from the tunnel with `VpnService.protect`, so a query
  cannot be captured by the interface that is answering it.
- Third-party TUN/DNS libraries: none adopted in M2. If one is ever added, licence,
  maintenance activity and open security issues must be reviewed and the version pinned.

## 4. Recovery (must be documented for users)

1. Android Settings → Network → VPN → disable LockSite.
2. If the service is stuck: force-stop the app from system settings.
3. If that fails: clear app data or uninstall.
4. Uninstalling may remove local configuration — this is expected.

These are OS recovery paths, not a feature of the app. The app must never make them
harder to reach.

## 5. Logging rules

- Allowed: service state transitions, error codes, counts, timestamps.
- Forbidden: hostnames queried, DNS payloads, IP addresses of visited sites, device
  identifiers.
- Debug-only reset tooling must live in `debug` sources and never ship in release.


## 4. Recovery (must be documented for users)

1. Android Settings → Network → VPN → disable LockSite.
2. If the service is stuck: force-stop the app from system settings.
3. If that fails: clear app data or uninstall.
4. Uninstalling may remove local configuration — this is expected.

These are OS recovery paths, not a feature of the app. The app must never make them
harder to reach.

## 5. Logging rules

- Allowed: service state transitions, error codes, counts, timestamps.
- Forbidden: hostnames queried, DNS payloads, IP addresses of visited sites, device
  identifiers.
- Debug-only reset tooling must live in `debug` sources and never ship in release.
