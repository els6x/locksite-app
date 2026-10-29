# Recovery — LockSite

Status: M6. This document describes how a user gets out of every state the app can put the
device in. It is the user-facing counterpart to `docs/SECURITY.md` §4.

The app must never make these paths harder to reach. They are Android system controls, not
features of LockSite.

## 1. Normal: turn protection off

1. Android Settings → **Network & internet → VPN** (the path varies by OEM).
2. Select **LockSite**.
3. Turn it off, or tap **Disconnect**.

The dashboard then shows **Dihentikan**. No domain is unlocked by this: the rules stay in
the database, they simply stop being enforced while the tunnel is down.

## 2. The service looks stuck

1. Try §1 first. A VPN that is being torn down can take a moment to disappear from the list.
2. If the entry is gone but the dashboard still says active, the status is stale. Force-stop
   the app: Android Settings → Apps → LockSite → **Force stop**. This kills the process; the
   status resets on the next launch, because nothing about protection is persisted.
3. **Settings → Diagnostik → Periksa ulang** re-reads the VPN consent and another-VPN state,
   but it does not re-derive an `ACTIVE` status; it is not a substitute for step 2.

## 3. After a device reboot

- Rules persist (they live in Room).
- **Protection does not start by itself.** The service is `START_NOT_STICKY` and is only
  started by an explicit tap, so the app never claims protection it did not establish.
- The dashboard therefore shows **Belum aktif** until **Aktifkan perlindungan** is tapped
  again.
- If the device uses **Always-on VPN** (Android Settings → VPN → gear icon), Android may
  start the service itself. That is an OS setting, not something LockSite enables.

## 4. After the process is killed

Same as §3: the tunnel dies with the process, the in-memory status is gone, and the next
launch reports the truth (**Belum aktif**). Re-activate from the dashboard.

## 5. Uninstall or clear app data

- **Clear data** removes the blocklist. This is a destructive, out-of-app action and is
  intentionally not offered inside LockSite.
- **Uninstall** removes the app and its rules. Android also removes its VPN configuration.

## 6. What LockSite will never do

- It will not add an in-app **Unblock**, **Delete** or **Edit** action for a locked domain.
- It will not hide or disable the Android VPN settings entry, force-stop, or uninstall.
- It will not auto-restart into a state it cannot prove: no stored "active" flag exists.

## 7. If internet stops working while protection is on

This is the symptom of a tunnel problem, not of a blocked domain. Follow §1 (turn the VPN
off). If normal browsing returns, the filter was the cause; re-activate it and check
**Settings → Diagnostik**. If browsing does not return with the VPN off, the problem is the
underlying connection, not LockSite.

**If every site fails while protection is on**, check Android's **Private DNS** setting. A
strict Private DNS hostname (DNS-over-TLS) cannot work with a DNS-only filter, because the
resolver never falls back to plaintext. LockSite refuses to activate in that state and tells
you why; set Private DNS to **Off** or **Automatic**, then activate again.
