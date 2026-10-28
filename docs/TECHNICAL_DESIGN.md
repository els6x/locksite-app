# Technical Design — LockSite

Status: M0 baseline. No VPN/DNS filtering exists yet.

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

## 5. Network design (planned, M2)

MVP uses a local TUN interface with DNS interception:

1. `VpnService.Builder` creates the TUN device and adds only the DNS server address(es)
   that the filter answers, plus a route limited to those addresses.
2. Packets are parsed for IPv4/IPv6 + UDP/TCP DNS only.
3. Blocked QNAME → synthesised DNS response (documented RCODE; no fake IPs without a
   written reason).
4. Allowed QNAME → forwarded to the configured upstream resolver, response validated
   (transaction ID, header flags, source, length) before returning it.
5. Anything the filter does not understand must still be forwarded, never dropped.

**Do not ship** a configuration that captures all traffic before forwarding is proven.

### Open decisions (blocking M2, see PRD section 19)

- Upstream resolver and its privacy consequences.
- Whether IPv6 and DNS-over-TCP are in scope for MVP.
- Own parser vs audited third-party TUN/DNS library (licence + maintenance review).
- Physical test device and its Android version.

## 6. Status model

UI status is derived from the real service state, not from a stored flag:

`NotConfigured → PermissionPending → Starting → Active | Error`, plus `Reconnecting`
and `Stopped`. Failure to read the blocklist is an **error**, never "empty = safe".

## 7. Build commands

```bash
./gradlew assembleDebug        # build debug APK
./gradlew testDebugUnitTest    # unit tests
./gradlew lint                 # Android lint
./gradlew installDebug         # install on a connected device
```

`local.properties` holds `sdk.dir` and is never committed.
