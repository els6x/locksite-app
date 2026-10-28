# Technical Design — LockSite

Status: M3 complete. The DNS filter is wired to the blocklist, the status is reported from
the real service, and a diagnostics screen exposes the facts the app can observe.
**Real-network verification on a physical device has not been performed yet**, so no claim
is made that blocking works end to end.

## 1. Toolchain (verified on the development machine)

| Component | Version | Note |
|---|---|---|
| Android Gradle Plugin | 9.4.0 | Max supported API level: 37 |
| Gradle | 9.6.0 | Minimum required by AGP 9.4 |
| JDK | 17 minimum, 25 used | Android Studio JBR 25.0.3 |
| Kotlin | 2.2.10 | Bundled by AGP 9.4 built-in Kotlin |
| Compose compiler plugin | 2.2.10 | Must match the Kotlin version |
| SDK Build Tools | 36.0.0 | Installed with the SDK |
| compileSdk / targetSdk | 37 | Android 17 |
| minSdk | 26 | Android 8.0; revisit if the test device differs |
| Package / namespace | `com.locksite.app` | Personal build, not published to Google Play |

### AGP 9 specifics that shape the build files

- **Built-in Kotlin is enabled by default.** `org.jetbrains.kotlin.android` is *not*
  applied; AGP compiles Kotlin sources itself and brings KGP 2.2.10.
- **The new public DSL is mandatory.** Legacy variant APIs (`applicationVariants`,
  `variantFilter`, `BaseExtension`) are gone. Use `androidComponents.onVariants`.
- **`targetSdk` defaults to `compileSdk`.** It is set explicitly anyway.
- `org.jetbrains.kotlin.plugin.compose` is still required for Compose.

## 2. Module boundaries

Single Gradle module (`:app`) for M0–M3. Package boundaries carry the architecture:

```
com.locksite.app
├── presentation/   Compose screens + ViewModels (StateFlow)
├── domain/         pure Kotlin: model, validation, matcher, use cases
├── data/           Room entities, DAO, repository
├── vpn/            VpnService, service controller, status
├── dns/            packet/DNS parsing, response building, forwarder
├── diagnostics/    status snapshot + internal test probes
└── ui/theme/       Material 3 theme
```

Rules:

- `domain` must not import `android.*` (matcher and validator are plain JVM code, unit
  testable without a device).
- `presentation` never touches the database or a socket directly.
- `vpn`/`dns` never write to the database directly; they read a snapshot of rules.

## 3. Data model (implemented in M1)

Room database `locksite.db`, version 1, schema exported to `app/schemas/`.

Table `locked_domains`: `id` (PK, autoincrement), `hostname_normalized` (unique index),
`include_subdomains`, `created_at`, `state` (only `LOCKED`).

Decisions:

- **Room, not DataStore.** The blocklist needs a unique constraint, a transactional insert
  and a diffable schema. A preferences file would have to reimplement all three in memory.
- **No `@Delete` / `@Update` in the DAO.** The only write path is `@Insert`. Removing a row
  is not an operation the production code can express.
- **Duplicate detection is the database's job**, not the UI's: the unique index aborts the
  insert and the repository maps that to `AddOutcome.DUPLICATE`.
- **No destructive fallback.** `fallbackToDestructiveMigration` is deliberately absent: a
  failed migration must surface as an error, never as an empty (and therefore "safe") list.

### KSP + built-in Kotlin caveat

AGP 9 forbids the `kotlin.sourceSets` DSL that KSP uses to register generated sources.
`android.disallowKotlinSourceSets=false` in `gradle.properties` is the documented escape
hatch. Revisit when KSP supports built-in Kotlin natively; remove the flag then.

### Repository port

`domain/repository/BlocklistRepository.kt` is the interface (pure Kotlin);
`data/repository/RoomBlocklistRepository.kt` is the implementation. This adds a
`domain/repository` package that the PRD folder list did not enumerate — the alternative
was letting the domain depend on the data layer.

## 4. Navigation (implemented in M1)

`navigation-compose` with five destinations: `onboarding`, `home`, `add-domain`,
`locked-list`, `settings`. The bottom bar exposes only the three top-level destinations and
is hidden on onboarding and on add-domain, where a single task should not compete with
navigation.

## 5. Network design (implemented in M2)

Decision record: `docs/ADR/0002-dns-filtering-approach.md`.

MVP uses a local TUN interface with DNS interception:

1. `VpnService.Builder` creates the TUN device, adds one interface address per family and
   routes **only** the two virtual DNS addresses (`/32` and `/128`). No default route.
2. Packets are parsed for IPv4/IPv6 + UDP DNS only. TCP, fragments and extension headers
   are ignored — they are never dropped, because they are never routed into the tunnel.
3. Blocked QNAME → local NXDOMAIN reply (no fake IPs, no SOA so nothing is cached).
4. Allowed QNAME → forwarded to `1.1.1.1` (fallback `1.0.0.1`) over a protected UDP
   socket, response validated (sender, transaction ID, QR bit) before returning it.
5. A malformed question gets FORMERR; a query that is already a response is dropped.

### Package layout

```
dns/
├── Bytes.kt            16-bit big-endian helpers
├── Checksums.kt        one's complement arithmetic (IPv4 header + UDP)
├── IpPackets.kt        IPv4/IPv6 + UDP parse and build
├── DnsMessages.kt      header/question parsing, compression pointers
├── DnsResponses.kt     NXDOMAIN, SERVFAIL, FORMERR, TC
├── DnsQueryHandler.kt  pure decision: block / forward / reject / drop
└── UdpDnsForwarder.kt  protected UDP socket, per-query, validated replies

vpn/
├── VpnConfig.kt            addresses, routes, MTU, upstreams
├── VpnController.kt        live status + consent intent (owned by AppContainer)
└── LockSiteVpnService.kt   lifecycle, TUN read loop, wiring
```

### Threading

One blocking read loop on `Dispatchers.IO`. A permit is taken from a `Semaphore(8)`
**before** each packet is dispatched to its own coroutine, so up to eight lookups run
concurrently and a slow resolver applies backpressure instead of spawning unbounded
coroutines. Writes to the tunnel are serialised by a `Mutex`; a failed write and a
malformed packet are both contained to that packet, so neither can terminate the loop.

If the loop itself ends while the service still believes it is running — EOF, I/O error —
the service publishes `ERROR` and stops itself rather than leaving the dashboard claiming
"active" over a dead filter. `onRevoke` suppresses that, because a read failure right
after the system took the VPN away is expected, not a fault.

### Sizing

MTU is 4096 (not 1500) because the tunnel carries only DNS and an answer that exceeds one
packet cannot be written back. Answers that still do not fit are returned as TC=1 so the
client fails fast instead of hanging.

### Deliberately not built yet

- DNS over TCP (the client's retry after TC=1 gets no answer).
- IPv6 extension header chains, IP fragmentation reassembly, ICMP.
- Foreground service / restart-after-boot behaviour (M4).

## 6. Status model

UI status is derived from the real service state, not from a stored flag.

`VpnController` (in `AppContainer`) holds a `StateFlow<ProtectionStatus>` that only
`LockSiteVpnService` may push to:

| Transition | Trigger |
|---|---|
| `NOT_CONFIGURED → PERMISSION_PENDING` | user taps activate, service requested |
| `PERMISSION_PENDING → ACTIVE` | `establish()` returned a descriptor and the loop started |
| `PERMISSION_PENDING → ERROR` | `establish()` failed, or the blocklist could not be read |
| `ACTIVE → STOPPED` | `onRevoke()` (user or system turned the VPN off) or `onDestroy()` |

Nothing is persisted. If the process dies, the status resets to `NOT_CONFIGURED` — which
is the truth, because the tunnel died with it. Failure to read the blocklist is an
**error**, never "empty = safe".

`CONFLICT_WITH_OTHER_VPN` is produced when the filter cannot run because another VPN owns
the tunnel: consent is denied while a VPN transport is active, or `establish()` returns
`null` with a VPN transport present. Detection uses `ConnectivityManager` and the read-only
`ACCESS_NETWORK_STATE` permission. `VpnController.refreshConflictState()` clears a stale
conflict once the other VPN is gone, and never touches a running or starting service.

## 7. Diagnostics (implemented in M3)

`presentation/diagnostics/` renders PRD FR-031 without inventing data:

| Field | Source |
|---|---|
| Service state | `VpnController.status`, pushed by the service |
| VPN consent | `VpnService.prepare(context) == null` |
| Another VPN | `ConnectivityManager` VPN transport, only while this app is not active |
| Locked count | `BlocklistRepository.observeCount()`; a failed read shows "unknown", never 0 |
| Self-test | `DiagnosticsSelfTest` — pure, in-process, no socket |

The self-test builds a wire query, runs it through the real `DnsQueryHandler` with the live
rule snapshot, and checks that a locked name is blocked while a reserved `.invalid` probe is
forwarded. It proves the wiring, not the TUN path: that still requires D-01…D-05 on a
device. `presentation/common/ProtectionStatusText` is the single status→string mapping, so
every state is covered and testable without Compose.

## 8. Build commands

```bash
./gradlew assembleDebug        # build debug APK
./gradlew testDebugUnitTest    # unit tests
./gradlew lint                 # Android lint
./gradlew installDebug         # install on a connected device
```

`local.properties` holds `sdk.dir` and is never committed.
