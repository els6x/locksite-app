# Known limitations — LockSite 0.7.0 (beta)

This list is deliberately blunt. If a limitation is not here, it does not mean it is solved —
it may simply not have been examined yet.

## 1. The filter is not a guarantee

- Blocking works at the **DNS** layer only. It stops a name from resolving; it does not
  inspect or block traffic that reaches an IP address by another route.
- The following bypass the filter entirely:
  - DNS-over-HTTPS (DoH) and DNS-over-TLS (DoT) used by browsers or apps;
  - another VPN running on the device;
  - HTTP/SOCKS proxies, including web proxies such as CroxyProxy — the phone only resolves
    the proxy host, so the blocked domain never reaches the filter;
  - direct access by IP address;
  - a DNS answer already cached before protection was enabled;
  - apps that ship their own resolver and ignore the system DNS server.
- **Do not describe LockSite as "unbypassable".** It is a focus aid, not a security boundary.
- **Strict Private DNS breaks the filter.** If Android's Private DNS is set to a hostname
  (DNS-over-TLS strict), the resolver will not fall back to plaintext, so every lookup fails —
  including names that are not blocked. LockSite detects this and **refuses to activate**
  instead of breaking DNS while claiming to be active. Set Private DNS to Off or Automatic to
  use the app.

## 2. DNS over TCP is not implemented

- The filter answers UDP DNS. If a response is too large, it is returned with the truncation
  bit set (TC=1), and the client retries over TCP. That TCP retry is **not** handled, so the
  lookup fails instead of being filtered or forwarded.
- Consequence: some large answers (and some blocked names that trigger a TCP retry) behave
  inconsistently.

## 3. Some apps are deliberately not filtered

- `com.github.android` and `com.spotify.music` are excluded from the tunnel
  (`VpnConfig.COMPATIBILITY_BYPASS_PACKAGES`) because a DNS-only VPN made them report "offline".
  A DNS-only tunnel never becomes `NET_CAPABILITY_VALIDATED`, and apps that gate on that
  capability show "offline". Excluded apps use the physical network, so **their DNS is not
  filtered**.
- The list is hardcoded in this build. There is no setting to add or remove packages. If
  another app misreports connectivity, it has to be added in code and rebuilt.
- A domain blocked in the browser is still blocked for every other app; only the bypassed
  app's own lookups are unfiltered.
- A package is only excluded if it was installed when the tunnel was established. Installing a
  listed app later requires toggling protection off and on again.

## 4. Protection does not survive a restart or process death

- The service is `START_NOT_STICKY` and does not call `startForeground`.
- After a reboot, a force-stop, or the process being killed, protection is **off**. The
  dashboard says **Belum aktif** — it never claims protection it does not have.
- There is no auto-restart. The user must tap **Aktifkan perlindungan** again.
- Always-on VPN is an OS setting; if the user enables it, Android may start the service. That
  is not something LockSite turns on.

## 5. "Permanent" is a UI property, not a device lock

- A locked domain has no delete, edit or unblock action **inside the app**.
- The device owner can still: turn the VPN off, force-stop the app, clear app data, or
  uninstall the app. None of these is prevented, and none should be.
- Clearing data or uninstalling removes the blocklist.

## 6. Conflict detection is best-effort

- Android allows one VPN at a time. LockSite reports **Bentrok dengan VPN lain** when another
  VPN owns the tunnel, and clears the report when the app returns to the foreground.
- Detection depends on `ConnectivityManager` reporting a VPN transport. A VPN that is starting
  or tearing down can be misreported for a moment.

## 7. Not tested beyond one device

- Verified on **one** phone running Android 13 (API 33), arm64-v8a.
- Android 8–12 and 14+ are untested. OEM background-management behaviour is untested.
- There are **no instrumentation tests**. Unit tests cover the pure DNS/packet/matcher layers
  (103 tests); anything needing a real network or a real VPN is manual.
- Blocking was verified **manually on one device** (D-01…D-04): locked names and a subdomain
  returned NXDOMAIN while allowed names and a look-alike resolved. There is still no automated
  instrumentation test for it.

## 8. Packaging and UI

- The beta APK is **debug-signed** (`applicationId` `com.locksite.app.debug`). It is for
  internal distribution only, cannot go to a store, and cannot be upgraded in place by a
  differently signed build.
- Light theme only. There is no dark variant.
- UI text is Indonesian only.

## 9. Privacy boundary

- No account, no cloud, no analytics, no browsing history is recorded or transmitted.
- While protection is active, DNS queries for **allowed** names are forwarded to a public
  resolver. With the AdGuard switch on (the default) the chain is AdGuard DNS
  (`94.140.14.14`, `94.140.15.15`) and then Cloudflare (`1.1.1.1`, `1.0.0.1`) as a fallback;
  with it off, Cloudflare only. Either way the resolver sees those names. Blocked names are
  answered locally and never leave the device.

## 10. Managed-device hardening is not implemented

- LockSite is an ordinary app. It does not use Device Owner / Profile Owner policies, so the
  device owner can always turn the VPN off, force-stop the app, clear its data, or uninstall
  it.
- Restricting uninstall or VPN settings would require managed provisioning (a factory reset
  plus an EMM/QR flow) and would conflict with the project's rule not to weaken Android's
  recovery paths. See `docs/ADR/0003-managed-device-mode.md`.
- If a managed fleet ever needs those controls, it must be a separate deliverable, not this
  build.
