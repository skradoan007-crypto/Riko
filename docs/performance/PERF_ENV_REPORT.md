# RIKO Performance — Environment & Device Report

> **Phase 18 (A) artifact** · honesty tags: `[M]` measured · `[R]` read/verified in code · `[V]` visual/user-verified · `[N]` assumed / needs verification
> **Plan:** `.mimocode\plans\1792178586648-silent-pixel.md` §1.4

## 1. Build environment (this machine)

| Item | Value | Tag |
|---|---|---|
| Local JDK | **none** — compile gate is GitHub Actions CI only | `[R]` |
| Local Android SDK | **none** (foreign `local.properties` path present, unusable) | `[R]` |
| OS | Windows (PowerShell) | `[R]` |
| Branch | `riko-perf-fixes` | `[R]` |
| CI workflow | `.github/workflows/build-debug-apk.yml` | `[R]` |
| CI triggers | push to `main`, `riko-perf-fixes` + `workflow_dispatch` | `[R]` |
| CI gates | token-drift (`tools/check-token-drift.ps1`) → `:app:testDebugUnitTest` → `assembleDebug` | `[R]` |
| CI does NOT gate | androidTest / instrumented tests (never compile there) | `[R]` AGENTS.md |

## 2. Build variants

| Variant | minify | shrinkResources | debuggable | signing | role |
|---|---|---|---|---|---|
| `debug` | yes | yes | yes | debug | everyday dev/CI APK (renamed "riko music.apk") |
| `release` | yes | yes | no | release | ship |
| `benchmark` | yes (initWith release) | yes | **no** | **debug** (for install) | macrobenchmark target — release-like R8+shrink, `<profileable>` via baselineprofile plugin |

Source: `app\build.gradle.kts:31-70` `[R]`

## 3. Toolchain versions (locked — NO bumps mid-campaign)

| Tool | Version | Source |
|---|---|---|
| Kotlin | 2.0.21 | `gradle\libs.versions.toml` `[R]` |
| AGP | 8.5.2 | same |
| Compose BOM | 2024.09.03 | same |
| Hilt | 2.52 | same |
| KSP | 2.0.21-1.0.25 | same |
| Room | 2.6.1 | same |
| Media3 | 1.4.1 | same |
| Coroutines | 1.9.0 | same |
| Coil | 2.7.0 | same |
| Navigation Compose | 2.8.2 | same |
| kotlinx-serialization | 1.7.3 | same |
| Baseline Profile / profileinstaller | 1.4.1 | same |
| JVM target | 17 | `app\build.gradle.kts:79` `[R]` |

Full table → `PERF_DEP_COMPAT.md`.

## 4. Device matrix (user fills)

| Slot | Target | Model | API | Display Hz | Thermal state | Notes | Tag |
|---|---|---|---|---|---|---|---|
| **A** | high-refresh, API 35+ | — | — | — | — | primary acceptance device | `[N]` |
| **B** | mid-range, API 33+ | — | — | — | — | secondary | `[N]` |
| **C** | low-end, API 26–29 | — | — | — | — | **manual Perfetto only** — macrobenchmark minSdk 28 `[R]` | `[N]` |

## 5. Benchmark harness constraints `[R]`

- Module: `benchmark\build.gradle.kts` — `com.android.test` + `androidx.baselineprofile`, `targetProjectPath = ":app"`, self-instrumenting.
- **`benchmark` module `minSdk = 28`** vs app `minSdk = 26` → API 26–27 devices cannot run macrobenchmark; use manual Perfetto / `dumpsys` there.
- Deps: `androidx.benchmark:benchmark-macro-junit4:1.4.1`, uiautomator 2.3.0, espresso 3.6.1.
- Run: `./gradlew :benchmark:connectedBenchmarkAndroidTest`
- Collect: `powershell -File tools\collect-perf-results.ps1` → `docs\performance\PERF_RESULTS_RAW.md`

## 6. Frame-budget reference (acceptance targets)

| Display | Per-frame budget | Notes |
|---|---|---|
| 60 Hz | 16.67 ms | primary acceptance target |
| 90 Hz | 11.11 ms | |
| 120 Hz | 8.33 ms | |

Acceptance (60 Hz, Plan.txt rule): **≥95% of critical-journey frames inside budget**; p95/p99 inspected; never averages alone.

## 7. App facts relevant to perf

| Fact | Value | Tag |
|---|---|---|
| applicationId | `com.rikodev.music.android` | `[R]` |
| minSdk / target / compile | 26 / 35 / 35 | `[R]` |
| Offline-first | no `INTERNET` permission | `[R]` |
| FGS media playback | yes (`MusicPlaybackService`) | `[R]` |
| Source size | ≈ 438 `.kt` files, ~98,500 lines | `[R]` |
| Frozen tree | `presentation\library\**` — 121 files, ~52,825 lines (D-7) | `[R]` |
| Room migrations | `MIGRATION_3_4` present | `[R]` |
