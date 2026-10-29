# Dependency audit — LockSite

Status: M6. Scope: every direct dependency declared in `gradle/libs.versions.toml` and
`app/build.gradle.kts`, plus the build plugins. Transitive AndroidX/Compose artefacts are
resolved by the Compose BOM and are not listed individually.

## 1. Runtime dependencies

| Dependency | Version | Licence | Maintainer | Why it is here |
|---|---|---|---|---|
| `androidx.core:core-ktx` | 1.19.1 | Apache 2.0 | Google / AndroidX | Kotlin extensions used by the platform layer |
| `androidx.lifecycle:lifecycle-runtime-ktx` | 2.11.0 | Apache 2.0 | Google / AndroidX | Coroutine-aware lifecycle |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | 2.11.0 | Apache 2.0 | Google / AndroidX | `viewModel()` in Compose |
| `androidx.lifecycle:lifecycle-runtime-compose` | 2.11.0 | Apache 2.0 | Google / AndroidX | `collectAsStateWithLifecycle` |
| `androidx.activity:activity-compose` | 1.13.0 | Apache 2.0 | Google / AndroidX | `setContent`, activity result launchers |
| `androidx.navigation:navigation-compose` | 2.10.2 | Apache 2.0 | Google / AndroidX | Screen graph |
| `androidx.compose:compose-bom` | 2026.09.00 | Apache 2.0 | Google / AndroidX | Aligns all Compose versions |
| `androidx.compose.ui:ui` (+ graphics, tooling-preview) | BOM | Apache 2.0 | Google / AndroidX | Compose UI |
| `androidx.compose.ui:ui-tooling` (`debugImplementation`) | BOM | Apache 2.0 | Google / AndroidX | Preview tooling, debug builds only |
| `androidx.compose.material3:material3` | BOM | Apache 2.0 | Google / AndroidX | Material 3 components |
| `androidx.compose.material:material-icons-core` | BOM | Apache 2.0 | Google / AndroidX | Icons (not pulled in by material3) |
| `androidx.room:room-runtime` / `room-ktx` | 2.8.5 | Apache 2.0 | Google / AndroidX | Local blocklist persistence |
| `junit:junit` (test only) | 4.13.2 | EPL 1.0 | JUnit team | JVM unit tests |

## 2. Build-time only

| Tool | Version | Licence | Note |
|---|---|---|---|
| Android Gradle Plugin | 9.4.0 | Apache 2.0 | Builds the app; not shipped |
| Kotlin (built-in, via AGP) | 2.2.10 | Apache 2.0 | Compiled into the app |
| Compose compiler plugin | 2.2.10 | Apache 2.0 | Must match the Kotlin version |
| KSP | 2.2.10-2.0.2 | Apache 2.0 | Room code generation |
| Room compiler | 2.8.5 | Apache 2.0 | Annotation processor, build only |

## 3. Deliberately absent

- **No third-party TUN/DNS library.** The packet and DNS layer is hand-written and unit
  tested (ADR 0002). This removes a large parser and its CVE history from the attack surface.
- **No HTTP client, analytics, ads or crash-reporting SDK.** The only socket the app opens is
  the protected UDP socket that relays allowed DNS queries.

## 4. Maintenance and security review

- All runtime dependencies are AndroidX/Compose or JUnit, actively maintained by Google or
  the JUnit team. No abandoned or unmaintained dependency is present.
- Versions were current at the time of the M6 audit (2026-10-29). `lint` reports
  "newer version available" warnings for some AndroidX artefacts; those are informational
  and are not pinned by this project's own policy.
- No dependency is added without a recorded reason. A future addition must document licence,
  maintenance activity and open security issues here and in an ADR (per `AGENTS.md`).

## 5. How to re-run the audit

```bash
./gradlew :app:dependencies --configuration debugRuntimeClasspath
```

Review the output against this file; update the version table when a dependency changes.
