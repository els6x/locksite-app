# Security & Privacy — LockSite

Scope: M0. This document describes the intended security posture; nothing here is a
claim that protection currently works — the filter is implemented in M2/M3.

## 1. Privacy invariants

| Invariant | Enforcement |
|---|---|
| Blocklist never leaves the device | No `INTERNET` permission in M0; no account, no sync, no backend |
| No browsing history collected | Only aggregate block counters, never QNAME history |
| No DNS payloads in logs | Log technical status only (state changes, error codes) |
| No analytics/ads SDK | Prohibited in `AGENTS.md` |
| No cloud backup of the blocklist | `allowBackup=false`, `data_extraction_rules.xml` excludes all domains |

If a later milestone needs network access, it must be justified in an ADR and reviewed
here first.

## 2. Threat model (summary)

| Threat | Mitigation | Residual risk |
|---|---|---|
| Wrong domain locked by mistake | Validation, scope explanation, explicit confirmation before locking | User can still lock a too-broad domain |
| Database corruption | Read failures surface as an error state, never as "empty = safe" | Data loss possible if storage is destroyed |
| Service stopped by system | UI shows "not active"; no fake "protected" state | Background limits can stop the service |
| App uninstalled / data cleared | Documented as out of scope for an ordinary app | Bypass possible by device owner |
| DNS-over-HTTPS / other VPN / direct IP | Documented limitation in onboarding and diagnostics | Not mitigated by DNS filtering |
| Malformed or hostile packets | Length validation, fuzzing, bounded parsing, reject invalid responses | Parser bugs could still crash the service |
| Spoofed DNS response | Validate transaction ID, flags, source address, length | Off-path spoofing remains possible |

**Positioning:** the product must never claim that blocking cannot be bypassed. The
device owner always retains system-level control (VPN settings, force-stop, uninstall,
factory reset).

## 3. Network security rules

- Traffic is not routed to any remote server. The VPN is local.
- Route only what the filter can actually handle; unknown traffic is forwarded, not
  dropped.
- Upstream resolver choice is documented, including its privacy consequences.
- Third-party TUN/DNS libraries: licence, maintenance activity and open security issues
  reviewed before adoption; version pinned.

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
