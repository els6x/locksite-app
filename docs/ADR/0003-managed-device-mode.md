# ADR 0003 — Managed-device (Device Owner) hardening

- **Status:** Accepted (M6) — **evaluation only, not implemented**
- **Date:** 2026-10-29
- **Supersedes:** nothing. Addresses PRD section 14, M6.

## Context

PRD M6 is explicitly optional and only after the MVP is stable. It asks four things:

1. evaluate Android Device Owner / managed-device provisioning;
2. review Android policy and the reset/provisioning consequences;
3. test whether app removal and VPN changes can be restricted on a managed device;
4. document that this configuration is not the normal mode for all users.

The product's promise is that a locked domain cannot be removed **from the app's UI**. It has
never promised that the device owner cannot remove the app or the VPN — and `AGENTS.md`
forbids weakening Android's recovery paths.

## What Device Owner would add

A Device Owner (DO) or Profile Owner app, provisioned through a managed deployment, can call
`DevicePolicyManager` to:

| Policy | Effect |
|---|---|
| `setUninstallBlocked(packageName, true)` | The user cannot uninstall the app |
| `addPersistentPreferredActivity` | Keeps a chosen launcher/app in front |
| `setLockTaskPackages` / lock task mode | Kiosk-style pinning |
| `DISALLOW_CONFIG_VPN` | The user cannot change VPN settings |
| `DISALLOW_APPS_CONTROL` | The user cannot clear app data or force-stop |
| `setPackagesSuspended` | Suspends chosen apps |

Provisioning requires either a factory-reset device plus QR/NFC/ADB provisioning, or an EMM
(Enterprise Mobility Management) console. It cannot be enabled by an ordinary installed app,
and it is not something the user can switch on from LockSite's settings.

## Decision — do not implement it in this app

LockSite stays an ordinary, unmanaged app. No `DevicePolicyManager` code is added, no
`device_admin` receiver is declared, and no policy is applied.

**Why:**

- **It contradicts a hard rule.** `AGENTS.md` says: do not weaken Android's system recovery
  paths (VPN settings, force-stop, uninstall). `DISALLOW_APPS_CONTROL` and
  `setUninstallBlocked` do exactly that. Shipping them would make the app harder to remove
  than the product ever claimed, and would turn a focus aid into a device lock.
- **It requires a factory reset.** Provisioning is not reversible without another reset. A
  normal user cannot opt in, and an app update cannot turn it on.
- **It cannot be tested here.** Verification needs a dedicated managed device or an EMM
  tenant. Neither exists in this project; claiming it works would be an unverified claim,
  which the project forbids.
- **Distribution policy.** Managed-device capabilities on a consumer build invite store and
  policy problems, and they are not needed for the stated product goal.
- **The recovery guarantee is a feature.** A user must always be able to turn the VPN off and
  remove the app. M6 must not erode that.

## If a managed deployment is ever required

Do it as a **separate deliverable**, not by changing this app:

1. A separate build flavour (or a separate app id) for managed fleets, clearly labelled.
2. Provisioning through an EMM/QR flow, with the organisation — not the end user — as the
   administrator.
3. A written policy stating which controls are applied and how an administrator removes them.
4. Keep an out-of-band recovery path (the EMM console) that does not depend on the app.
5. Test on a dedicated managed device before any claim is made.
6. Never publish that build as the normal consumer app.

## Consequences

- M6 is complete **as an evaluation**. No product behaviour changes.
- LockSite remains unmanaged: the device owner can always disable the VPN or uninstall the
  app. This is documented in `docs/RECOVERY.md` and `docs/KNOWN_LIMITATIONS.md`.
- If the product direction changes to managed fleets, this ADR must be superseded by one that
  records the provisioning model, the policies, the administrator recovery path, and the
  device test results.
