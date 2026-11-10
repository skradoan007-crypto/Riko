# RIKO Performance — Suspected Bottleneck Register (H1–H13)

> **Phase 18 (A) artifact** · honesty tags: `[M]` · `[R]` · `[V]` · `[N]`
> **Rule:** every hypothesis is **UNVERIFIED** until Phase B device baselines confirm or reject it. Static evidence below is `[R]` (code-read) only.
> **Status column:** `UNVERIFIED` → `CONFIRMED` / `REJECTED` / `PARTIAL` — filled after Phase B/C with metric deltas.

---

## 1. Hypothesis table

| ID | Hypothesis | Static evidence (Phase A) | Status | Wave | Priority |
|---|---|---|---|---|---|
| **H1** | Wide `collectAsState()` roots recompose large subtrees on every player tick | **88 `collectAsState()` call sites, 0 `collectAsStateWithLifecycle` in `presentation\`** `[R]` (grep). `AuroraSpectrum.kt:160-171` collects **11 flows** at one root `[R]`. `PlayerSheetHost.kt:282-284` reads `isPlaying`+`playbackPosition`+`duration` at host root `[R]`. `PlayerSheetFull.kt:154-161` collects 8 flows at root `[R]`. Note: `lifecycle-runtime-compose` is already a dependency (`libs.versions.toml:33`) — swap is pure call-site work `[R]`. | UNVERIFIED (static strongly implicates) | C1 | P0 |
| **H2** | Position/duration flows emit too hot and are read at high composition scope → recomposition churn during playback | `PlayerSheetHost.kt:283-284` (`playbackPosition`, `duration`) read at host root `[R]`. `PlayerSheetFull.kt:927-928` also collects position/duration `[R]`. `PlayerViewModel` exports exist (Wave 1 LAG KILL) `[R]` — need to confirm they are sliced `.map{}.distinctUntilChanged()`. | UNVERIFIED | C1 | P0 |
| **H3** | Visualization state pushed to UI at frame rate without throttle → CPU + recomposition during NP/visualizer | `PlayerViewModel.kt:123-143,230` exports `spectrumState`/`waveState`/`audioState`/`colorState` `[R]`. Backed by `PcmAudioProcessor` / `ExperienceDataHub` `System.nanoTime()` pipeline `[R]`. `WavePhaseClock.kt` `withFrameNanos` loop `[R]`. | UNVERIFIED | C2/C5 | P0 |
| **H4** | Plasma canvas AGSL `RuntimeShader` costs GPU/CPU per frame under load | `PlasmaWaveformCanvas.kt:51-126` uses `RuntimeShader` `[R]`. Uniform updates may re-trigger draw more than needed — needs trace. | UNVERIFIED | C5 | P1 |
| **H5** | Room/`combine` latency + `filterNotNull()` suspend-forever → songs-not-loading / skeleton hang | **PARTIAL FIX ALREADY LANDED** `[R]`: `MusicRepository.kt:85-89` "FIX: always set cache, even on failure" via `getOrDefault(emptyList())`. **Residual risk:** `songsCache = MutableStateFlow(null)` (`:74`) + `getAllSongs` still uses `songsCache.filterNotNull()` (`:122`) — until first emit, downstream waits. Init path: `refreshSignal.debounce(300L)` (`:84`) = **minimum 300 ms latency** before first cache set, plus query time. `combine` of 2 flows (songs+favorites) at `:121-124` `[R]`. HomeViewModel combines 5 flows `[R]`. | PARTIAL (hang fixed; latency chain remains) | C2 | P0 |
| **H6** | Lazy* config gaps (missing `key`/`contentType`) in list wrappers cause fling jank | **BETTER THAN EXPECTED** `[R]`: grep shows `key` + `contentType` present in FavoritesScreen, PlaylistDetailScreen, CollectionDetailScreens, SearchScreen, HomeScreen, LibraryContents, PlaylistsScreen, AddSongsSheet, LyricsRendering, HomePlan11Components. **Gaps found:** `QueueSheet.kt:153` has `key` but **no `contentType`**; `AddToPlaylistSheet.kt:67` has `key` but **no `contentType`**; `EqualizerSheet.kt:217,235` has `key` but **no `contentType`**. | PARTIAL (mostly good; 3 gaps) | C3 | P1 |
| **H7** | Artwork decode not size-constrained → layout shift + decode jank | Wave 1.2 artwork cache "done" per Campaign v3 `[N]`. Coil 2.7.0 in use `[R]`. `PlayerSheetHost.kt:134,143` already moves image decode to `Dispatchers.IO` `[R]`. Needs re-measure of thumb/full-bleed sizing. | UNVERIFIED | C4 | P1 |
| **H8** | Queue reorder drag path jank on long queues | `QueueSheet.kt:105-111` local drag state `[R]`. Wave 2.2 "done" but **no device numbers** `[N]`. | UNVERIFIED | C5 | P1 |
| **H9** | Startup TTFD gate correctness unproven on device | `reportFullyDrawn` wired at `MainActivity.kt:285` inside `runCatching` `[R]`. `StartupReadiness.kt` uses `Dispatchers.IO` scope `[R]`. **TTFD metric table still empty** `[N]` — gate ordering unproven on hardware. | UNVERIFIED | C7 | P0 measure |
| **H10** | Release log/analyzer noise costs startup+scroll | Not yet audited for Timber/log volume `[N]`. CI runs unit tests + assembleDebug only `[R]`. | UNVERIFIED | C8 | P2 |
| **H11** | Unstable models across composition boundaries → systemic recomposition tax | **PARTIAL** `[R]`: `domain\model\Song.kt` has 10 `@Immutable` annotations (all models) `[R]`. `AuroraMotion.kt:19` `@Immutable`, `PlayerSheetState.kt:27` `@Stable`, `MiniPlayerPerformanceGate.kt:190,389` `@Immutable` `[R]`. **Gap:** most presentation UI-state holders / DTOs crossing composable boundaries lack `@Immutable`; almost no `@Stable` usage. | PARTIAL | C1→C9 | P1 |
| **H12** | **Non-perf product debt** (not a perf hypothesis) | From `.mimocode\plans\1791612818890-swift-comet.md` `[R]`: NP title cut-off / 3-tier transport buttons (`PlayerSheetFull.kt`), 80+ hardcoded token violations, 3 parallel type systems, Visualizer Lab polish. **Export button: CONFIRMED still a no-op** — `VisualizerExport` defined but never called; `AuroraSpectrum.kt:323` is a comment-only onClick `[R]`. Tracked in `PERF_NONPERF_BACKLOG.md` — **never mixed into perf commits**. | N/A (product) | separate | P1 product |
| **H13** | `LazyVerticalGrid`-in-scroll crash | **ALREADY FIXED** `[R]`: `AuroraSpectrum.kt:597` comment "FIX: Row-based grid instead of LazyVerticalGrid (crashes inside verticalScroll)". `:485` still uses `verticalScroll` with the Row-based grid inside — correct pattern. **Do NOT re-fix.** Verify once in audit only. | **FIXED** (verify-only) | A | — |

---

## 2. Static audit notes by module (A-1 … A-11)

### A-1 `service\` — playback core `[R]`
- `PlayerManager.kt:61` owns a single `CoroutineScope(SupervisorJob() + Dispatchers.Main)` — intentional (comment at `:195`: Dispatchers.Default crashes on play with IllegalStateException).
- Single ExoPlayer ownership preserved (canonical constraint).
- **Still to verify in C2/C7:** listener add/remove symmetry, notification rebuild frequency, work done on Main inside player callbacks.

### A-2 `audiofx\` + visualizer pipeline `[R]`
- `ExperienceDataHub.kt:101`, `VisualizerManager.kt:78`, `EqualizerEngine.kt:79` use `Dispatchers.Default` scopes — correct for DSP/CPU work.
- `AudioBands.kt:78-79` documents Default as a real thread pool (not Main) — bridge coroutines run there.
- Canonical pipeline `PcmFrames → SpectrumEngine → SpectrumBands` intact.
- **Still to verify:** per-buffer allocations (GC pressure), StateFlow emission rate to UI, `System.nanoTime()` bookkeeping cost.

### A-3 `data\` — repositories `[R]`
- **Good:** every repository method uses `Dispatchers.IO` (`MusicRepository`, `PlaylistRepository`, `SettingsRepository`) or `flowOn(Dispatchers.IO)` `[R]`. No `runBlocking` found.
- `SettingsRepository.kt:154` comment: "PERF P2 (RC-12): proto/deser off Main" — already done.
- **H5 residual:** 300 ms `debounce` + `filterNotNull()` before first cache emit (see table).

### A-4 `presentation\nowplaying\` `[R]`
- `PlayerViewModel` already exports granular StateFlows (Wave 1 LAG KILL artifact) `[R]` — C1 **deepens**, does not redo.
- `PlayerViewModel.kt:313` has a `combine` for `playerState`; `:341` `combine(queue, currentSong)`; `:520` another `combine` — each needs slice-audit in C1.
- `PlasmaWaveformCanvas.kt` already honors `isReducedMotion` / `isBatterySaver` `[R]`; `bandsScratch` FloatArray reuse pattern at `:114` `[R]` — good.

### A-5 `presentation\sheet\` `[R]`
- **H1 hotspot.** `PlayerSheetHost.kt:107-109,282-284` — 6 `collectAsState()` including hot position/duration at root.
- `PlayerSheetFull.kt:154-161,902-931` — 8+ `collectAsState()`.
- `AuroraSpectrum.kt:160-171` — **11 flows collected at one root** (worst case).
- Morph (mini↔full) per-frame composition cost — C5 target.

### A-6 `presentation\miniplayer\` + `minicontact\` `[R]`
- `MiniPlayerIntegration.kt:112` `collectAsState()` on `state`.
- `MiniPlayerPerformanceGate.kt` has `@Immutable` models (`:190,389`) `[R]` — some perf work already done.
- Thumb pipeline + drag morph per-frame work — C4/C5 targets.

### A-7 list wrappers (NOT `library\`) `[R]`
- See H6 — `key`/`contentType` largely present; 3 gaps (QueueSheet, AddToPlaylistSheet, EqualizerSheet).
- **`presentation\library\**` FROZEN (D-7)** — read-only in audit, never modify.

### A-8 `core\` `[R]`
- `StartupReadiness.kt:54` uses `Dispatchers.IO` scope `[R]`.
- `ColorExtractor.kt:36` extracts on `Dispatchers.Default` `[R]` — good.
- `PlayerSheetHost.kt:134,143` already moved image decode to `Dispatchers.IO` `[R]`.
- RikoTouch/RikoGesture hook cost — C5 target.

### A-9 `benchmark\` `[R]`
- Module complete enough for J1–J6, J8, J10–J13 (see plan §1.5).
- **Coverage gaps:** J7 search fling, J14 mini→full morph drag, J15 background/resume — add as **new** methods only.
- `minSdk 28` vs app 26 → API 26–27 devices need manual Perfetto.

### A-10 Gradle / compose compiler `[R]`
- No `strongSkipping`, no `stabilityConfigurationFile` today.
- `benchmark` buildType idempotent guard present in both `app\build.gradle.kts:64` and `benchmark\build.gradle.kts:23`.
- Baseline Profile wired: `baselineProfile(project(":benchmark"))` + `profileinstaller` `[R]`.

### A-11 CI `[R]`
- `.github\workflows\build-debug-apk.yml`: token-drift gate → `:app:testDebugUnitTest` → `assembleDebug`. Uploads APK artifact.
- **Does not** compile/run androidTest or macrobenchmark (expected — no device farm).
- CI is the sole compile gate (no local JDK/SDK).

---

## 3. Benchmark coverage gaps (Phase B1 work) — **IMPLEMENTED** `Phase 19.1`

New file only: `benchmark\src\main\java\com\rikodev\music\android\benchmark\JourneyGapBenchmark.kt` `[R]`.
Existing scenario semantics in `ListFlingBenchmark` / `PlayerInteractionBenchmark` / `StartupBenchmark` untouched (baseline-continuity rule).

| Journey | Gap | Implementation |
|---|---|---|
| J7 search result fling | no benchmark class | `JourneyGapBenchmark.searchFling` — open Home → Search icon → type "a" → fling results |
| J14 mini→full morph drag | only expand/collapse click existed | `JourneyGapBenchmark.miniToFullMorphDrag` — 25-step swipe up (mini→full) + down (full→mini) |
| J15 background→resume | manual only | `JourneyGapBenchmark.backgroundResume` — UI-thread resume frames. **Manual meminfo/logcat soak still required** (plan J15 full scope). |

---

## 4. Status flip log (fill after Phase B / each wave)

| ID | After Phase B | After wave | Delta (p95 etc.) | Notes |
|---|---|---|---|---|
| H1 | static CONFIRMED [R] | **MITIGATED (Phase 20)** | device numbers `[N]` | 88 collectAsState→WithLifecycle; AuroraSpectrum hot reads in LabSpectrumCanvas draw scope; MiniPlayRing isolates 4Hz ticker |
| H2 | static CONFIRMED [R] | **MITIGATED (Phase 20)** | device numbers `[N]` | position/duration no longer at host root; PlayerViewModel slices verified already `.map{}.distinctUntilChanged()` |
| H3 | UNVERIFIED | **PARTIAL (Phase 20/24)** | device numbers `[N]` | hot reads in draw scope (C1); WavePhaseClock gated (C5). Emission rate throttle not changed (would alter viz latency feel — needs device proof first) |
| H4 | UNVERIFIED | open | — | PlasmaWaveformCanvas currently unused (PlasmaTimeline not mounted); WaveformTimeline is the live timeline and already gated |
| H5 | PARTIAL | **FIXED (Phase 21)** | — | `songsCache` starts `emptyList()`; `filterNotNull()` emits immediately. Residual 300ms debounce is acceptable (coalescing). |
| H6 | PARTIAL | **FIXED (Phase 20)** | — | contentType added to QueueSheet, AddToPlaylistSheet, EqualizerSheet |
| H7 | UNVERIFIED | **MITIGATED (Phase 23)** | device numbers `[N]` | inline ImageRequests remembered; mini crossfade(false). Global crossfade(false) + ArtworkRequestFactory already in place from Plan 1.2. |
| H8 | UNVERIFIED | open | — | queue reorder drag — needs device J9 feel + metrics |
| H9 | UNVERIFIED | **AUDITED (Phase 26)** | device numbers `[N]` | TTFD already gated on criticalReady+onboarding+withFrameNanos; StartupReadiness has timeouts. Numbers still owed. |
| H10 | UNVERIFIED | **FIXED (Phase 27)** | — | all Log.d/i behind RikoLog (release no-op); Log.w/e preserved |
| H11 | PARTIAL | **PARTIAL (Phase 20)** | — | domain models already @Immutable; presentation hot reads scoped. Remaining @Immutable gaps are low-priority (C9 if measured). |
| H13 | FIXED `[R]` (verify-only) | — | — | do not re-fix |
