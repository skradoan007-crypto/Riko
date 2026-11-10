# RIKO Performance — Dependency Compatibility Table

> **Phase 18 (A) artifact** · honesty tags: `[M]` · `[R]` · `[V]` · `[N]`
> **Rule:** NO version bumps during performance waves (Campaign §26). Dependency modernization is a **post-campaign checkpoint**, benchmarked before adoption.

## 1. Core stack (from `gradle\libs.versions.toml` + `app\build.gradle.kts`) `[R]`

| Library | Version | Role in perf story | Compat notes | Bump? |
|---|---|---|---|---|
| Kotlin | 2.0.21 | language + Compose compiler host | Compose compiler ships with Kotlin plugin (`kotlin.compose`) | ❌ no |
| AGP | 8.5.2 | build | benchmark module `com.android.test` needs AGP 8.x | ❌ no |
| Compose BOM | 2024.09.03 | entire UI layer | `compose = true`; no explicit `stabilityConfigurationFile` / `strongSkipping` flags today `[N]` | ❌ no (C9 only if evidence) |
| Hilt | 2.52 | DI | KSP 2.0.21-1.0.25 pairing is required | ❌ no |
| KSP | 2.0.21-1.0.25 | Room + Hilt codegen | must track Kotlin version | ❌ no |
| Room | 2.6.1 | persistence | KSP schema export to `app/schemas`; `MIGRATION_3_4` present | ❌ no |
| Media3 | 1.4.1 | ExoPlayer + session + ui + common | `@UnstableApi` opt-in already set in `kotlinOptions` | ❌ no |
| Coroutines | 1.9.0 | async | `ExperimentalCoroutinesApi` opt-in | ❌ no |
| DataStore | 1.1.1 | settings | `flowOn(Dispatchers.IO)` already applied in SettingsRepository | ❌ no |
| Palette | 1.0.0 | artwork color extraction | runs on `Dispatchers.Default` (`ColorExtractor.kt`) | ❌ no |
| **Coil** | **2.7.0** | image loading (artwork) | key C4 lever: `ImageRequest` size/cache/bitmapConfig | ❌ no (configure, don't replace) |
| Navigation Compose | 2.8.2 | `RikoDestination` graph | C6 lever | ❌ no |
| kotlinx-serialization | 1.7.3 | JSON settings/presets | keep off Main (C2) | ❌ no |
| Baseline Profile | 1.4.1 | startup path optimization | wired via `baselineProfile(project(":benchmark"))` + `profileinstaller` | ❌ no (refresh profile, not bump lib) |
| profileinstaller | 1.4.1 | ships the profile | | ❌ no |

## 2. Test / benchmark stack `[R]`

| Library | Version | Notes |
|---|---|---|
| androidx.benchmark:benchmark-macro-junit4 | 1.4.1 | macrobenchmark harness |
| androidx.test.ext:junit | 1.2.1 | |
| espresso-core | 3.6.1 | |
| uiautomator | 2.3.0 | drives `RikoBench` content-desc selectors |
| robolectric | 4.14.1 | unit tests (`:app:testDebugUnitTest`) |
| compose ui-test-junit4 | (BOM) | unit-level compose tests |

## 3. Plugins `[R]`

| Plugin | Version | Notes |
|---|---|---|
| com.android.application | 8.5.2 | |
| org.jetbrains.kotlin.android | 2.0.21 | |
| org.jetbrains.kotlin.plugin.compose | 2.0.21 | Compose compiler |
| com.google.dagger.hilt.android | 2.52 | |
| com.google.devtools.ksp | 2.0.21-1.0.25 | |
| org.jetbrains.kotlin.plugin.serialization | 2.0.21 | |
| androidx.baselineprofile | 1.4.1 | creates/uses `benchmark` buildType (idempotent guard in build files) |

## 4. Compose compiler / stability flags (current state) `[R]` `[N]`

| Flag / setting | Present? | Notes |
|---|---|---|
| `compose = true` (buildFeatures) | yes | `app\build.gradle.kts:93` |
| `strongSkipping` | **not configured** | potential C9 experiment — needs evidence first |
| `stabilityConfigurationFile` | **not configured** | potential C9 experiment |
| `@Immutable` on domain models | **yes** — `domain\model\Song.kt` (10 annotations) | good; presentation state objects mostly lack it (H11) |
| `@Stable` | `PlayerSheetState.kt:27` | limited use |
| freeCompilerArgs opt-ins | Material3/Foundation/Animation/Coroutines/Media3 UnstableApi | `app\build.gradle.kts:78-85` |

## 5. Explicit compatibility guardrails (perf campaign)

1. **No version bumps** during waves C1–C9. If a wave appears to need one → stop, log in `PERF_HYPOTHESES.md`, escalate to user.
2. **No speculative library swaps** (list, image, animation). Lazy* + Coil + Compose Animation stay.
3. `benchmark` buildType is guarded idempotently (`findByName("benchmark") ?: create("benchmark")`) — never clash with baselineprofile plugin.
4. R8 rules live in `app\proguard-rules.pro` — Baseline Profile regeneration (C8) must verify no runtime-needed paths are stripped; keep ProGuard mapping.
5. Compose BOM 2024.09.03 → newer is a **post-campaign** decision, benchmarked before adoption.

## 6. Known dependency-related perf levers (for later waves, not upgrades)

| Lever | Wave | Action |
|---|---|---|
| Coil `ImageRequest` size / cache / `bitmapConfig` | C4 | configure existing Coil 2.7.0 |
| Compose stability (`@Immutable`, stable collections) | C1 | annotate, don't bump compiler |
| `collectAsStateWithLifecycle` (from `lifecycle-runtime-compose` **already a dependency**) | C1 | swap call sites; lib already present |
| Baseline Profile content refresh | C8 | re-run `BaselineProfileGenerator` |
| Log stripping in release | C8 | `BuildConfig`/`isDebuggable` guards; keep Timber errors |
