# RIKO Performance Baseline (Wave 0)

> **Status:** instrumentation landed `[M] code` · device numbers **PENDING** `[N]`
> **Plan:** `docs/superpowers/plans/2026-10-05-riko-professional-campaign.md` §5 (Wave 0 — Evidence Before Optimization)
> **Rule:** every performance claim in this campaign is tagged `[M]/[R]/[V]/[N]`. "100% smooth" is the product goal — never an unverified claim.

## Purpose

Reproducible baseline BEFORE any performance-critical code changes.
Wave 10 must re-run these exact scenarios and write `PERF_BEFORE_AFTER.md` — never replace a poor baseline with an easier test.

## How to run

```powershell
# 1. Device: unlock screen, disable battery saver, keep cool (no thermal throttle)
# 2. Seed the device library with a real test corpus (see "Device preparation")
# 3. Run:
./gradlew :benchmark:connectedBenchmarkAndroidTest
# 4. Collect (writes this file's numbers from the benchmark JSON outputs):
powershell -File tools/collect-perf-results.ps1
```

Results JSON: `benchmark/build/outputs/connected_android_test_additional_output/**/*.json`
Each metric reports **P50 / P90 / P95 / P99 / min / max / median** — never averages only.

## Device preparation

1. Install benchmark APK (`:app:assembleBenchmark` + `:benchmark:connectedBenchmarkAndroidTest` does both).
2. Grant permissions (the harness also tries `pm grant` automatically):
   `READ_MEDIA_AUDIO`, `POST_NOTIFICATIONS`, `VIBRATE`.
3. Library corpus (minimum): **≥ 23 songs** (matches director's device), ideally **1000+** for list-stress honesty. Include art-tagged albums.
4. Complete onboarding once (harness taps "Get Started" if shown).
5. Playback: at least one song must be playable offline (benchmark taps the first row).

## Scenarios → instrumentation map

| # | Scenario (plan §5) | Benchmark class | Metrics | Status |
|---|---|---|---|---|
| 1 | My Music list fling | `ListFlingBenchmark.myMusicFling` | FrameTimingMetric | `[N]` |
| 2 | Favorites fling | `ListFlingBenchmark.favoritesFling` | FrameTimingMetric | `[N]` |
| 3 | Home scroll | `ListFlingBenchmark.homeFling` | FrameTimingMetric | `[N]` |
| 4 | Queue scroll | `PlayerInteractionBenchmark.queueScroll` | FrameTimingMetric | `[N]` |
| 5 | Queue reorder | manual (see below) + Wave 2.2 fix | frame feel | `[N]` |
| 6 | Now Playing open/close (morph) | `PlayerInteractionBenchmark.nowPlayingExpandCollapse` | FrameTimingMetric | `[N]` |
| 7 | Now Playing scrub | `PlayerInteractionBenchmark.nowPlayingScrub` | FrameTimingMetric | `[N]` |
| 8 | EQ interaction (band drag) | `PlayerInteractionBenchmark.eqBandDrag` | FrameTimingMetric | `[N]` |
| 9 | Visualizer animation | `PlayerInteractionBenchmark.visualizerAnimation` | FrameTimingMetric | `[N]` |
| 10 | Home startup (cold/warm/hot) | `StartupBenchmark` (3 tests) | StartupTimingMetric (TTID/TTFD) + FrameTimingMetric | `[N]` |
| 11 | Search result fling (J7) | `JourneyGapBenchmark.searchFling` **(Phase 19.1)** | FrameTimingMetric | `[N]` |
| 12 | Mini→full morph drag (J14) | `JourneyGapBenchmark.miniToFullMorphDrag` **(Phase 19.1)** | FrameTimingMetric | `[N]` |
| 13 | Background→resume (J15 UI) | `JourneyGapBenchmark.backgroundResume` **(Phase 19.1)** | FrameTimingMetric | `[N]` |
| 14 | Background→resume soak (J15 manual) | manual + adb meminfo/logcat | memory, ANR, playback resume | `[N]` |

Extra observations (manual, recorded in the tables below): memory during long scroll, CPU during Now Playing animation, 30-minute playback soak, thermal/battery.

## Measurement tables (fill from `tools/collect-perf-results.ps1` output)

### Startup — `StartupBenchmark` (ms)

| Metric | Cold P50 | Cold P90 | Cold P95 | Cold P99 | Warm P50 | Warm P95 | Hot P50 | Hot P95 |
|---|---|---|---|---|---|---|---|---|
| TTID | — | — | — | — | — | — | — | — |
| TTFD | — | — | — | — | — | — | — | — |
| frameDurationCpuMs P95 (launch anim) | — | — | — | — | — | — | — | — |

### List fling — `ListFlingBenchmark` (frameDurationCpuMs / frameOverrunMs)

| Scenario | P50 | P90 | P95 | P99 | Janky frames >16ms | Janky frames >33ms |
|---|---|---|---|---|---|---|
| MyMusic fling | — | — | — | — | — | — |
| Favorites fling | — | — | — | — | — | — |
| Home fling | — | — | — | — | — | — |

### Player interactions — `PlayerInteractionBenchmark`

| Scenario | P50 | P90 | P95 | P99 | Notes |
|---|---|---|---|---|---|
| NP expand/collapse morph | — | — | — | — | flagship motion — must stay smooth |
| NP scrub | — | — | — | — | |
| Queue scroll | — | — | — | — | |
| EQ band drag | — | — | — | — | baseline includes the known stale-closure bug (Wave 2.1 fixes) |
| Visualizer animation | — | — | — | — | |

### Manual observations

| Item | Value | Tag |
|---|---|---|
| Queue reorder — 5 consecutive swaps | — | `[N]` |
| Memory during 60s list scroll (dumpsys meminfo) | — | `[N]` |
| CPU during NP animation (top -m 1 / Perfetto) | — | `[N]` |
| 30-min playback soak (leaks/crashes/thermal) | — | `[N]` |
| Device A (API 35+, high-refresh) model | — | `[N]` |
| Device B (API 33+) model | — | `[N]` |
| Device C (API 26–29) model | — | `[N]` |

## Known baseline context (do not "fix" before measuring)

- `ListFlingBenchmark` numbers INCLUDE known jank sources: 250ms monolithic `playerState` collection (Queue/Search/Playlist), per-row ImageRequest + global crossfade, Interactive-tier glass rows. That is the point — Wave 1 must lower these numbers.
- `eqBandDrag` includes the stale-closure drag bug (finger tracking broken). Wave 2.1 fixes behavior; re-measure after.
- TTFD is `[N]` until Wave 3.4 adds `reportFullyDrawn` (StartupTimingMetric reports TTFD only when the app calls it).
- Release-like `benchmark` build = R8 + shrink + debug signing + `<profileable>`; debug impressions do NOT count as evidence.

## Honesty ledger

| Claim | Tag | Evidence |
|---|---|---|
| Instrumentation compiles & scenarios runnable | `[M]` | CI green + `:benchmark:connectedBenchmarkAndroidTest` runs on device |
| Any "smooth"/"faster" claim | `[N]` | **forbidden until this table is filled** |
