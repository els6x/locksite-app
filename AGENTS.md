# AGENTS.md — Rules for AI coding agents

This repository implements **LockSite**, an Android app that blocks user-chosen domains
through a local DNS filter running on `VpnService`. Read `PRD.md` first; it is the source
of truth for product behaviour.

## Read before changing code

1. `PRD.md` — product requirements.
2. `docs/TECHNICAL_DESIGN.md` — stack, module boundaries, open decisions.
3. `docs/SECURITY.md` — privacy and threat model constraints.
4. `docs/TEST_PLAN.md` — what must be tested.
5. `docs/ADR/` — recorded decisions. Do not silently contradict an accepted ADR.

## Workflow

Work one milestone at a time (see `PRD.md` section 14). For every milestone:

1. Audit the repository and run the existing build/tests **before** editing.
2. Do not assume a file, class or function exists — verify it.
3. Write down the list of files you intend to change.
4. Implement only the current milestone.
5. Run `./gradlew assembleDebug`, `./gradlew testDebugUnitTest` and `./gradlew lint`
   (whatever exists) and report the **real** result.
6. Review `git diff`, then commit. Never `git reset --hard`, never rewrite history,
   never push without explicit permission.

## Hard prohibitions

- Do not implement a VPN that captures all traffic, processes only DNS and drops the
  rest. That breaks connectivity. Never enable wide routes before forwarding works.
- Do not claim a domain is blocked without a real network or instrumentation test.
- Do not add telemetry, analytics, accounts, ads, or any backend. No `INTERNET`
  permission unless a milestone explicitly requires it and documents why.
- Do not add a delete/edit/unblock action for locked domains in production code.
  Debug-only reset tooling must live in `debug` sources and be clearly labelled.
- Do not log DNS payloads, resolved hostnames, or browsing history.
- Do not weaken Android system recovery paths (VPN settings, force-stop, uninstall).
- Do not add a large dependency without documenting licence, maintenance status and
  trade-off in an ADR.
- Do not report "protection active" from a stored boolean; status must come from the
  actual service state.

## Language and style

- Source code, comments, commit messages, and repository documents: **English**.
- User-facing strings: Indonesian (target user), kept in `res/values/strings.xml`.
- Kotlin: official style, 4-space indent, no wildcard imports.

## Definition of done

Scope implemented, no placeholders presented as working features, build green, relevant
tests executed and reported, error and loading states handled, docs updated, unresolved
limitations recorded, diff reviewed, commit created.
