# RIKO — MASTER PLAN RECONCILED / EXECUTION PLAN

> Companion to the Master Implementation Prompt (`full plan.txt`, §0–§135).
> The Master Prompt remains the **contract**. This file is the **executable
> mapping**: every §-requirement reconciled against verified codebase reality,
> plus the concrete work order.
>
> **Status: PHASES 0—4 COMPLETE (CI green) — next: PHASE 5.**
> Reconciled: 2026-10-02 @ `6be4c3f7` · Updated: 2026-10-03, branch
> `riko-perf-fixes` @ Phase 4 head (this commit) after Phase 4 edits; updated per-commit
> whole-codebase sweep. Honesty rules (§126) apply to every claim in this file.
> **No line of the master plan may be dropped; this file is the only phase plan
> (§8 — one source of truth).**

---

## ★ EXECUTION LOG — Phases 3-11 agentic run (2026-10-05, all user-gated CI green)

| Phase | HEAD (green) | Key outcomes |
|---|---|---|
| 3 Component System | `cc5c2602` | RikoArtwork §76 (18 sites), RikoTrackRow canonical, V2TopBar→RikoTopBar, shapes collapse, G-14/D-16, §89 tests; 3 red→fixed (§126) |
| 4 Nav+Interaction | `c33c4ffe` | predictive back + root-swallow fix (G-3), `VisualOsCoordinatorWiring` single owner (G-4), VisualOsHost retired documented (G-18/D-19), Lab back + `RikoBackChainTest` |
| 5 Step 0 | `08d3b037` | G-1 `RikoMaterialTypography` (Type.kt deleted, slot test), G-11 AppLanguage removed, G-9 MaterialYou disproven |
| 5 Home | `b0691418` | persistent greeting bar, taps wired, chips live, D-9: 6 dormant files deleted |
| 5 My Music | `a144ae6b` | New tab sort, Downloads typed-empty, Playlists QueueMusic entry (D-12), dead cascade; fixes: parse → folders+remember |
| 5 Favorites | `1c680aa5` | real sort menu, error+retry, A14 undo (shell snackbar slot born), dead heart gone |
| 5 Playlists | `66dc2dd6` | real sort menu, skeleton+error, A83 delete confirm (detail sheet reuse), favorites menu hidden, create validation (dup/empty); APK naming `089dca0d` (artifact **riko music**, version 1.0.0) |
| 5 Search | `f6e62746` | Albums/Artists sections → detail routes (was unreachable!), error+retry, IME dismiss, shell metrics, dead SearchTopBar removed |
| 5 Settings | `ac26e5b4` | **placebo defect fixed**: load+restore on start (MainActivity), `RikoMotion.motionScale` live (MotionIntensity → 40 tween sites); settings_v2 "dormant" claim disproven |
| 5 Queue | `7eb52c87` | remove+UNDO (`PlayerManager.insertInQueue`, index-clamped), typed empty; queue-v2 = dormant disproven-for-live |
| 5 Sheets (G-2/G-8) | `6ada98b4` | Lyrics menu entry + `onShow`/position bridge wiring (was NEVER fired), honest no-lyrics state confirmed; Visualizer item verified LIVE |
| 5 Onboarding/Splash | `13986aa1` | Get Started label, social row dead removed, Settings 3 dead rows (G-7 backfill), Splash reduceMotion + 4s watchdog |
| 5 tail D-9 | `5be27489`,`35f1347e`,`6a8b6247`,`c50ff427` | deleted: search-v2, queue-v2, personalization (+bindings+stubs); VisualOsHost cascade (9 files incl. renderers/engine); V2 trio migrated (`RikoPlaylistCard` new, SectionHeader→canonical, buttons→Riko) + release tree; TopSheet 28→`RikoShapes.TopSheet`; shell mini slot retired |
| 6 Now Playing | `ad74023f` | repeat control (OFF/ALL/ONE), prev/next disabled at ends, error banner+retry, LIVE label (unknown duration), TimeTabular, AddToPlaylist picker wired (was no-op) |
| 7 Spectrum | `974cb2ff` | PcmFramesTest extended: seek/track-change/pause lifecycle (runs in CI) |
| 8 Motion | `9d7c02d2` | §80 press normalize → `RikoStateTokens.PressScale` (GlassButton/BubbleIcon×2/Transport×2/Hero 0.95/0.87/0.90/0.86/0.85→0.96); tier-mapping (200/220/400→normal/screen) |
| 9 Perf | `eb82780e` | G-19: `PcmAudioProcessor` duplicate ×2→×1 per chunk (shared view, absolute reads) |
| 10 A11y | `115edf3e` | G-8: EQ curve `CustomAccessibilityAction` per-band raise/lower (drag-only → TalkBack path), slider contentDescription; **audio-v2 deleted** (D-13 sequence done) |

### §126 disproofs from this run (stale claims → [R])

- `mBandsTableS10` (D-2): never existed — 0 refs. · D-3 resample tests: ALREADY on prod path (`SpectrumConfig.resampleToDisplay`, in `src/test`). · `windowAnchor` §32: already `PlasmaTokens.HumpEndOfProgress` → `uWindow`.
- G-19 `SessionViewModel` / `ReorderableLazyList`: classes do not exist; the perf family was executed as RC-01..RC-13 pass (inventory in code comments). · `V2QualityCard`: does not exist. · `settings_v2` "dormant": LIVE (NavGraph route). · G-7 "QueueSheet no empty": bare empty existed (upgraded). · "onboarding Terms/Privacy": params never rendered (removed; legal component `RikoOnboardingLegalText` kept for when a real policy exists — release checklist item).

### Deferred / blocked (with reasons, never dropped)

- **Legacy `GlassSurface` consumers** — frozen `presentation/library/**` imports it; D-7 freeze beats D-18. Unfreezes with the library (post-Phase 11).
- **debug tree / WaveLaboratory hidden entry** — D-14/Phase 7F decision gate. · **Focus-ring infra (§86)** + 48dp/contrast sweep + A87 2x-font clipping + A104 remainder = Phase 10 spot items verified on-device **[V]** (tokens `FocusRingColor/Width` exist).
- **Row-press holds**: QueueSheet 0.975 / AddSongsSheet 0.98 = deliberate subtle row tier (not button depth). Shimmer 700 / splash 450-900 / ambient 14000 = named holds.
- **[V] device gate (Phase 11, USER)**: install artifact **`riko music`**, back-chain, EQ TalkBack actions, reduced-motion, §115/§121 numbers, screenshots (D-10: no screenshots in repo).


## 0. GROUND TRUTH — VERIFIED FACTS

All items below were measured/verified by direct inspection or CI logs, not
inferred. Tag: **[M]** measured, **[R]** verified in repo, **[I]** inferred,
**[A]** assumption, **[V]** requires device verification, **[N]** not tested.

### 0.1 Build state — **RESOLVED** ✅ **[M]**

| Fact | Evidence |
|------|----------|
| HEAD **compiles; every phase commit ends green** | Phase 0 `37006478998`, Phase 1 `37020089897`, Phase 2 `37028654635`; Phase 3 green: `37122134156` (deletions+track row), `37123640385` (artwork), `37124202086` (top bar), `37125350194` (shapes), `37126657068` (state tokens/typography/tests) — all `success`. Phase 3 also had 3 red-then-fixed runs (§126): `37120462277` (missed live helpers → fix `66401acf`), `37121726265` (em-dash corruption → fix `a53254d6`), `37125028096` (`RikoMaterialShapes` lost in dedup → fix `ac8cfd3b`). |
| Historical breakage (now fixed) | 4 failures 2026-09-30→10-02; root cause: `RikoMotion.kt:211` nested `object Easing` shadowing Compose `Easing` (11 errors) + `MiniPlayerRenderingSpec.kt:2705` |
| Fix | Phase 0 commit `15df62bb` removed the dead nested object (4 lines; 0 references verified before removal, §95) |
| Last pre-breakage green | `36590602174` @ `a4238bb0` (2026-09-29) |

### 0.2 Build & verification loop **[R]**

- **No Android SDK on this machine.** `local.properties` has a foreign Linux
  path; no `ANDROID_HOME`. → **Local `gradlew` build is impossible.**
- **GitHub Actions is the sole compiler/test gate.**
  Workflow: `.github/workflows/build-debug-apk.yml`
  - Triggers: push to `main` **or `riko-perf-fixes`** (+ manual dispatch)
  - Steps: `:app:testDebugUnitTest` → `assembleDebug` → upload APK
    (`riko-music-debug-apk`, 30-day retention — the installable build [V])
  - Green run ≈ 7–8 min; compile fast-fail ≈ 2.5 min **[M]**
- `gh` CLI v2.101.0 available → I watch runs myself (§4); you may also check.
- Consequence: **every phase ends in a push — then the agent STOPS; the user reports CI result (green/red) and orders the next phase.** Run-watching by the agent is optional (user preference, 2026-10-03).
  done.**

### 0.3 Source-of-truth register (§8) — **[R]**, current state

| Concern | Canonical owner | Path | Verdict |
|---|---|---|---|
| Playback / queue / position | `PlayerManager` (@Singleton ExoPlayer) | `service/PlayerManager.kt` | OK |
| MediaSession / notification | `MusicPlaybackService` | `service/MusicPlaybackService.kt` | OK (`addSession` wired) |
| Now Playing surface | `PlayerSheetHost` + `PlayerSheetFull` | `presentation/sheet/` | OK — only surface wired |
| Sheet anchor | `PlayerSheetState` (anchored-drag, Full=0 / Mini=screenH) | `presentation/sheet/PlayerSheetState.kt` | OK |
| Pager | 3-item window, **follower of Media3** | `PlayerSheetFull.kt:159–223` | OK (§8 satisfied) |
| Mini player | `SheetMiniBar` + `MiniPlayRing` (private in host) | `PlayerSheetHost.kt:268, :338` | OK — `RikoMiniPlayer`/`V2MiniPlayer`/`MiniPlayer.kt`/preview-state deleted Phase 3; remainder re-homed (§0.4, D-18) |
| Spectrum analysis | `PcmAudioProcessor` owns the one `SpectrumEngine` | `audiofx/PcmAudioProcessor.kt:96` | OK — single owner |
| DSP primitive | `Fft` (radix-2) | `core/experience/audio/Fft.kt` | OK |
| Spectrum → UI | `PlayerViewModel.spectrumState` → `AuroraSpectrum` → `SpectrumLevelSource` → `SpectrumRenderer` | `presentation/nowplaying/visualization/` | OK |
| PCM tap | `DefaultRenderersFactory.buildAudioSink` → `setAudioProcessors` | `di/PlayerModule.kt:50–62` | OK — **primary path, no mic** |
| Visualizer prefs | DataStore `"riko_settings"`, `setVisualizerPrefs()` | `data/repository/SettingsRepository.kt:141` | OK in shape — **race defect G-10** |
| Artwork | `RikoArtwork` **canonical, adopted** | `riko/components/RikoArtwork.kt` | **OK (§76, Phase 3)** — 18 raw sites migrated; 2 remain in `V2Cards.kt` (dies Phase 5, D-9); wrapper keeps its own call; 1 painter site outside §76 wording (G-17) |
| Motion | single `object RikoMotion` | `riko/theme/RikoMotion.kt` | consolidated ✓ **and compiling ✓ (Phase 0)** |
| Track row | `RikoTrackRow` canonical | `riko/components/RikoTrackRow.kt` | **OK (§78, Phase 3)** — losers deleted, consumers migrated |
| Sheet types | `RikoSheetType` enum | `presentation/visualos/host/VisualOsSceneMap.kt` | **ownership smell** — sheet contract owned by Visual OS (Phase 4, G-18) |
| Theme entry | `RikoMusicTheme` (production) | `core/designsystem/theme/Theme.kt:66` | **not routed to canonical tokens (G-1)** |

### 0.4 Duplicate / dormant inventory — **[R]** (drives §79, §80, §95)

| Area | Competing implementations | Live one | Dormant size |
|---|---|---|---|
| Track row | `RikoTrackRow` (former `V2TrackRow`/`RikoSongRow`/`CollectionTrackRow` — **deleted Phase 3**) | `RikoTrackRow` | none — consumers migrated (§78) |
| Mini player | `SheetMiniBar` \| `presentation/miniplayer/` (20 files) \| `MiniPlayer.kt` \| `RikoMiniPlayer` \| `V2MiniPlayer` \| `VisualOsDockRenderer` | `SheetMiniBar` | preview-state fns + `RikoMiniPlayer`/`V2MiniPlayer`/`MiniPlayer.kt` **deleted Phase 3**; full retirement re-homed (Phase 5 tail: live `PlayerViewModel` + frozen `MiniPlayerDockIntegration` block deletion) |
| Mini contact helpers | `presentation/minicontact/` (4) | none | 202 lines |
| Now Playing | `PlayerSheetFull` \| `PlasmaMediaCard`+`PlasmaWaveformCanvas`+`PlaybackControls`+`GradientSlider`+`AlbumArtBackground`+`PlasmaTokens` \| `TransportControlsV2` | `PlayerSheetFull` | careful split (D-4/R-3) |
| V2 design system | `V2AppBar`/`V2Sheet`/`V2NpCards`/`V2BottomNav`/`V2MiniPlayer` **deleted Phase 3**; live: `V2Buttons`, `V2Cards`, `V2SectionHeader` (+`V2TestTags` = production) | live V2 trio until Phase 5 | 3 files — Phase 5 tail (D-9) |
| Bottom nav | `RikoBottomNavigation` (live, `RikoNavGraph.kt:257`); `V2BottomNav` **deleted Phase 3** | `RikoBottomNavigation` | none |
| **Dormant feature stacks** | `search-v2` (23 f / ~7k) · `queue-v2` (12 f / ~2.5k) · `audio-v2` (~2k, incl. accessible EQ screen) · `personalization` (~1.2k) · `release/{audit,beta,engineering}` (~1k) · `debug` (14 f / ~2.7k, incl. unreachable WaveLaboratory) · `settings_v2` · `allsongs` VM · lyrics stack (contract/host/wiring — unreachable, G-2) | live simple screens | **~15k lines** (library 52.8k is separately FROZEN) |
| EQ screens | live `EqualizerSheet` (10 sliders, 0 semantics) \| dead `AudioScreen` (2k, best a11y) · duplicate `EqualizerPresets` models | `EqualizerSheet` | G-8 |
| Typography systems | `theme/Type.kt` (legacy, `FontFamily.Default`) \| `riko/theme/RikoTypography.kt` (canonical) \| `RikoThemeV2.type` | mixed at runtime | **G-1** |
| Theme entries | `RikoMusicTheme` (prod) \| `RikoTheme` (onboarding/preview, holds the slot mapping `RikoTheme.kt:27–42`) \| `RikoThemeV2` | `RikoMusicTheme` | **G-1** |
| Shape objects | **collapsed Phase 3**: canonical `RikoShape` + `RikoCornerRadius`; legacy `RikoShapes` kept for `TopSheet` (28dp) only | `RikoShape` | 1 file deferred — TopSheet value reconcile → Phase 5 (§80) |
| Artwork→color | `DynamicColors` (MaterialYou, `RikoMusicTheme(dynamicColors=…)`) \| `ColorIntelligenceEngine` (+ `AlbumPalette` dead) | both partly live | **G-9** |

**Junk files:** 12 tracked 0-byte `*.kt System.Object[]` artefacts — **removed in
Phase 1** ✓. `Project RIKO.zip` untracked + `*.zip` gitignored ✓.

### 0.5 Tests — **[M]** (drives §88, §89)

- 19 test files → **18 after Phase 3** (8 unit, 10 instrumented; net −1) **[M]**
- **Resolved (Phase 3, §89):** instrumented suite rewritten against production — `V2TrackRowTest`→`RikoTrackRowTest`, `V2ChromeTest`→`RikoChromeTest` (canonical top bar / nav / hero play / sheet), `V2NowPlayingTest` deleted (tested dead `V2QualityCard`/`V2NextUpCard`); 8 remaining tests target live components.   **[N]** androidTest never compiles in CI (workflow = unit tests + assembleDebug only) — signatures verified against source; device run required in Phase 11.
- **Parity violation (§89):** `SpectrumConfig.resampleToDisplay()` tested but
  unused; production `SpectrumLevelSource.resample()` used but untested (D-3,
  Phase 7 B).

### 0.6 Doc integrity — **[R]**

- Mojibake in `PROJECT_STATUS.md` + `ARCHITECTURE_FREEZE.md` → **re-encoded in
  Phase 1** ✓.
- Milestone file-count claims verified real (differences were subdir counting)
  — **NOT fake** ✓.
- Phase 1 item "risk findings added to PROJECT_STATUS Technical Debt table" —
  **[N]** re-verify during Phase 11 docs refresh.

### 0.7 Frozen / locked surfaces (must not be rewritten)

`presentation/library/**` = **121 files, 52,825 lines LOCKED**
(`docs/ARCHITECTURE_FREEZE.md` §1). Per Plan §128 **and** the freeze:
bugfix/docs/tests/behaviour-preserving perf only. Library requirements are met
through the outside/host layer or a new build layer (D-7). Note: library *host*
files outside `presentation/library/**` (e.g. `presentation/collection/*`,
`LibraryTabs` where it lives) are **not** frozen — verify path per file before
touching.

### 0.8 PHASE STATUS BOARD **[M]** (source: `git log` + `gh run list`)

| Phase | Plan §97 area | Status | Evidence |
|---|---|---|---|
| 0 | Build restore | ✅ DONE | `15df62bb` → CI `37006478998` success |
| 1 | Truth + architecture | ✅ DONE | `9348641d` + `f50b9ab7` → CI `37020089897` success (TRUTH_MAP + this doc + junk/zip removal + UTF-8 repair + `.gitignore`) |
| 2 | Design system | ✅ DONE (scope note below) | `29062c96` → CI `37028654635` success (RikoDimensions/RikoMotion/RikoStateTokens/RikoTypography + `DESIGN_TOKEN_MAPPING.md`) |
| 3 | Component system |  ✅ DONE (scope note below)  | `e245956e`…`4aa6e0a9` (10 commits) + CI runs §0.1 — deletions, §78 track row, §76 artwork (18 sites), V2TopBar→RikoTopBar, RikoShapes collapse, G-14 entry + D-16 GlassButton, §89 test rewrite |
| 4 | Navigation + interaction |  ✅ DONE   | manifest flag, conditional BackHandler, `VisualOsCoordinatorWiring`+test, `RikoSheetType→contract`, Home saveable, Lab `onBack`+`RikoBackChainTest` + CI `37132159799` success (user-reported) |
| 5 | Screen-by-screen UX | ⬜ | — |
| 6 | Now Playing | ⬜ | — |
| 7 | Spectrum / plasma | ⬜ | — |
| 8 | Motion / microinteraction | ⬜ | — |
| 9 | Performance | ⬜ | — |
| 10 | Accessibility + edge states | ⬜ | — |
| 11 | Final validation | ⬜ | — |

**Phase 2 scope note (§126):** delivered tokens + the §97 mapping table, as its
own doc declares (`DESIGN_TOKEN_MAPPING.md` §Scope): *call-site migration was
deliberately scheduled* to Phase 3 (components) and Phase 5 (screens) so every
value change is looked at visually. Two Phase-2 plan items are **not yet done**
and are re-homed below: theme-entry unification (G-1 → Phase 5 Step 0) and
`V2ThemeTest` extension (→ Phase 5 Step 0). Not skipped — scheduled.

---

## 1. RECONCILIATION — PLAN § → VERIFIED REALITY

### 1.1 Already satisfied (do **not** rebuild — §94)

| Plan § | Requirement | Verified location |
|---|---|---|
| §29 Layout A | `≈360dp × 151dp`, aspect `2.38:1`, header/title/baseline/time/controls Y | `PlasmaTokens.kt`: `RefCardWidthDp=360`, `CardAspect=2.38`, `HeaderY .16`, `TitleY .33`, `BaselineY .505`, `TimeY .61`, `ControlsY .795` **[R]** |
| §30 inactive track | `≈3.25dp`, `≈42% white` | `PlasmaWaveformCanvas.kt:330`; `PlasmaTokens.InactiveAlpha=0.42` **[R]** |
| §30 ridges | max `≈12.5dp`, one-sided, filled | `:329` `maxHump = 12.5 * uPx`; `:358` 5-ridge loop **[R]** |
| §30 thumb | `≈16.5dp` outer, `≈2.5dp` ring, accent + white ring | `:419–420` **[R]** |
| §30 glow | thumb-only, no global blur | `:428` `exp(-d²/σ²)`, comment `:40` **[R]** |
| §32 ridge window | `0.07 / 0.28 / 0.85 × progress`, calm gap before thumb | `PlasmaTokens.HumpStartU/HumpPeakU/HumpEndOfProgress`; shader `:346–348` **[R]** |
| §33 Layout B | `uPlasma = 0` classic / `1` plasma | `:394` baseline switch **[R]** |
| §35/§36 AGSL | constant source, typed uniforms, premultiplied | `PLASMA_AGSL :281–440`; binder `:154–206`; `layout(color) half4` **[R]** |
| §41 phase clock | smooth speed, monotonic, no `uTime*speed` | `WavePhaseClock.kt` **[M]** |
| §45 ridge colors | `#2B59E2 / #771FFD / #F942E8` + accent, hot core | `AlbumPalette.kt:33–48` **[M]** — but **AlbumPalette unreachable until Phase 7 E** (G-10) |
| §44 color intelligence | artwork → palette → accent, gray fallback | `AlbumPalette.kt:42` `FALLBACK_ACCENT` **[R]** |
| §47/§48 Tier 1 audio | Media3 PCM, no mic on primary path | `di/PlayerModule.kt:50–62` **[R]** |
| §49 pipeline | PCM→mono→normalize→Hann→FFT→dB→log bands→smoothing→peak→frame | `SpectrumEngine.kt` + `SpectrumBands` **[R]** |
| §55 DSP vs render | DSP ≠ display rate | `EMIT_INTERVAL_NS = 33ms` ≈ 30 Hz **[M]** |
| §56 Lab presets | SIGNATURE/CALM/PULSE/STUDIO; AURORA/CLASSIC; 32/40/48 | `VisualizerLab.kt:28–56,:93–101`; `SpectrumStyle.kt:16` **[R]** |
| §56 persistence | atomic `setVisualizerPrefs()` | `SettingsRepository:141–150` **[R]** (race → G-10) |
| §106 Reset to Signature | one meaningful reset | exists (`eb253b81`) **[R]** |
| §59–§66 state tokens | Playing/Paused/Scrub/Buffer/ReducedMotion/BatterySaver amp+speed | `PlasmaTokens` full set **[M]** — live-renderer wiring incomplete (G-10) |
| §58 honest states | LIVE/PREVIEW/BUFFERING/OFF/ERROR | `SpectrumController.isTapLive()` + `SpectrumLevelSource` fallback **[R]** |
| §69 playback safety | passthrough byte-exact, no blocking on callback | `PcmAudioProcessor` duplicate-buffer analysis `:151–204` **[R]** |
| §100 debug surface | calibration panels exist | `presentation/debug/` 14 files — **but unreachable (G-6/D-14)** |
| §105/§106 Lab persistence + reset | — | ✓ (see above) |
| §112 renderer modes | AURORA/CLASSIC only (both dead renderers deleted) | ✓ (`b709a4f2` era) **[R]** |
| §24 motion consolidation | one `RikoMotion`, six §24 classes | ✓ Phase 0+2 (`travel()`/`settle()` aliases added, dead `Duration` ladder removed) **[R]** |

### 1.2 Verified gaps (must be done) — original set

| Plan § | Gap | Evidence |
|---|---|---|
| **§78** | 4 competing track rows | §0.4 |
| **§79** | 6 competing mini players, ~10k dormant lines | §0.4 |
| **§76** | raw `AsyncImage` bypasses `RikoArtwork` (count: 27 refs / 26 real sites per mapping §6; "30 files" per earlier grep — **re-measure in Phase 3**, G-17) | §0.3, mapping §6 |
| **§50/§67** | tier → FFT size dead config: `SpectrumConfig.fftSizeFor()` never called; DSP hardcoded `FFT_SIZE = 1024` | `PcmAudioProcessor.kt:46` **[M]** |
| **§51/§67** | `AuroraSpectrum.kt:168` hardcodes `SpectrumTier.BALANCED`; tier + density parallel truths | **[M]** |
| **§32** | `windowAnchor` not a named token | `PlasmaWaveformCanvas.kt:347–348` **[M]** |
| **§89** | two band resamplers, tested-unused vs used-untested | **[M]** (D-3) |
| **§19** | no live repeat control (`cycleRepeatMode()` exists) | `PlayerViewModel.kt:487` **[M]** |
| **§88** | 11/19 tests target dead `V2*` | §0.5 **[M]** |
| **§4** | reference images unreadable by model → written numbers only | D-0/D-10 |

### 1.3 NEW GAPS — whole-codebase sweep after Phase 2 **[M]**

Found while re-reading the plan word-by-word against the tree; each is assigned
to a phase below. (Gaps ≠ scope creep: every one maps to an existing §.)

| ID | Gap | Evidence | Phase |
|---|---|---|---|
| **G-1** | Production typography not routed: 3 type systems; `RikoMusicTheme` → legacy `theme/Type.kt` (`FontFamily.Default`); the slot→canonical mapping (`RikoTheme.kt:27–42`) exists **only** in `RikoTheme` (onboarding/preview). `DESIGN_TOKEN_MAPPING` §1's "slots already routed" is true only inside `RikoTheme`. | `Theme.kt:91`, `Type.kt:18/20`, `RikoTheme.kt:27–42/:126`, `MainActivity.kt:227` | **Step 0 DONE**: `RikoMaterialTypography` single source (both entries), legacy `theme/Type.kt` retired, `TypographySlotMappingTest` pins 13 slots; color/entry collapse stays with slices ||
| **G-2** | Lyrics unreachable: NP dropdown has 5 items (EQ/Visualizer/Info/Sleep/AddToPlaylist) — **no Lyrics**; lyrics stack (contract/host/wiring/renderers) + `V2QualityCard` (2.2k, best a11y) all dead | `PlayerSheetFull.kt:630–667` **[R]** | 5 (wire entry + sheet) · 6 (NP integration) |
| **G-3** | Predictive back not wired: `android:enableOnBackInvokedCallback` **absent** from manifest | `AndroidManifest.xml` grep = 0 **[R]** | 4 |
| **G-4** | Coordinator callback double-wire: `MainActivity.kt:161–190` **and** `VisualOsNavBridge.kt:132–161` both assign `onNavigateToNowPlaying/onTogglePlayPause/onBackPress/onExtractColors`; NavBridge dispose nulls them; MainActivity alone sets `onSkipToNext/Prev` → inconsistent dock behavior | grep both files **[R]** | 4 |
| **G-5** | NP missing states: buffering, error, prev/next-disabled, no repeat button, `onOpenAddToPlaylist` nav no-op (`RikoNavGraph.kt:523 /* post-M2 */`), unknown-duration/LIVE handling | reconciled §1.2 + sweep | 6 |
| **G-6** | Dormant stacks ~15k LOC (§0.4) incl. unreachable WaveLaboratory → §100 only half-satisfied | §0.4 | 3 (V2/miniplayer) · 5 tail (stacks, D-9) · 7 F (Lab entry, D-14) |
| **G-7** | Per-screen state/dead-control inventory: Home top bar vanishes in loading/error/empty; See-all & playlist-card taps no-op; "Prince Lovers" placeholder shipping; onboarding Terms/Privacy/social dead; MyMusic Downloads CTA wrong + New-mode duplicate; Playlists sort chip TODO + no index skeleton; Favorites sort undiscoverable + no error; QueueSheet no empty + remove-no-undo; **no error states** on Favorites/Playlists/Search/Settings/Queue; Splash no timeout/reduced-motion path | sweep | 5 (per-screen, re-verify fresh) |
| **G-8** | Two EQ screens: live `EqualizerSheet` (10 sliders, zero semantics, `LinearGradient` non-linear press target) vs dead `AudioScreen` (2k, best a11y); duplicate `EqualizerPresets` models | sweep | 5 (live path wins, D-13) · 10 (semantics) |
| **G-9** | Color unreconciled: 3 palettes, dual artwork→color pipelines, 373 raw `Color(0x)`/176 distinct (mapping §4), MaterialYou branch + `// REVERT in Plan 2` comment | mapping §4, `MainActivity.kt:222–227` | Step 0 verified **no MaterialYou anywhere** (`dynamicColorScheme` = 0 refs — claim disproven [R]); artwork `DynamicColors` bridge = identity, unified with `colorState` in per-screen slices + D-12 satisfied ||
| **G-10** | Spectrum runtime defects: `updateVisualizerPrefs` read-modify-write race; `onFlush` missing `lastEmitNanos` reset; 3 `DeviceTier` impls (= D-2); battery/reduced-motion tokens not wired into live `AuroraSpectrum`; 8-way waveform registry duplication; legacy mic capture concurrent with primary path | sweep D-series | 7 |
| **G-11** | `AppLanguage` selector non-functional: 0 `stringResource`, 4-entry `strings.xml`, no locales | grep | **resolved Phase 5 Step 0** [R] — selector UI + contract/state/action/event/request/coordinator/deps removed (6 files) ||
| **G-12** | Plan reference screenshots (`01_home.png…17_visualizer.png`, plasma jpg) absent from repo | filesystem search | **ratified D-10**: proceed on written numbers + `images/UI target/` |
| **Phase 3 ✓ (shapes collapsed; TopSheet 28dp + GlassSurface→canonical deferred, D-18)** → 5 (blur adoption) |
| **Phase 3 ✓ (entry-point adoption + GlassButton D-16)**; raw scales→§80, FocusRing→§10, TimeTabular→5/6 (D-18) |
| **G-15** | `presentation/release/{audit,beta,engineering}` + `presentation/debug` compile into production src (no guard) | sweep | 5 tail (D-9) / 7 F |
| **G-16** | Test parity extras beyond §0.5: theme test not extended (Phase 2 gate item) | Phase 2 diff | **resolved Step 0** — `TypographySlotMappingTest` (5 slot tests + brand-family) extends theme coverage alongside `V2ThemeTest` ||
| **re-measured Phase 3 [M] ✓** — 20 raw at start → 18 migrated, 2 in V2Cards (Phase 5); see §0.3/§0.4 |
| **G-18** | Visual OS: `VisualOsHost` not mounted while comments claim live; `RikoSheetType` owned by `VisualOsSceneMap` | sweep | 4 |
| **G-19** | Perf defects: `SessionViewModel` re-reads whole player screen + 3 buffers; full-NP state re-read per emission; `PcmAudioProcessor` `duplicate()` per chunk (§70); `ReorderableLazyList` state duplication | sweep | 9 (registry → 7) |
| **G-20** | Library/Collection host surfaces: verify which files sit outside the freeze; §14 requirements only via host layer | freeze §0.7 | 5 (per-file path check) |

---

## 2. DESIGN / ARCHITECTURE DECISIONS

*(Delegation: gaps resolved by judgment; requirements stay verbatim. Each
decision states its plan anchor. **R = ratified by user 2026-10-03**.)*

| ID | Plan anchor | Decision | Why |
|---|---|---|---|
| D-0 | §4, §99 | Reference images unreadable by me → all §29–32 numbers come from the Master Prompt's written values `[M]`; any other value = named `[A]` token, never hard-coded | §99 |
| D-1 | §6, §103 | Keep AGSL opt-in API 33+; `LegacyGradientProgressBar` fallback must be *real*, not blank; minSdk stays 26 | §6 |
| D-2 | §50, §67 | Tier selection becomes real: `fftSizeFor(tier)` wired; one band-count truth; `SpectrumTier`/`VisualizerDensity` tables collapse to one | §67, §8 |
| D-3 | §89 | Delete `resampleToDisplay`, move its tests onto `SpectrumLevelSource.resample` | §89 |
| D-4 | §79, §95 | Delete dormant code **only after** the replacing path is green; reference-search first; never batch-delete first | §95 |
| D-5 | §94 | Visualizer stack is verify/wire/tune/measure — **not** a rewrite (geometry/tokens/clock/Lab already match §1.1) | §94 |
| D-6 | §80 | Motion consolidation value-preserving (Phase 0 did exactly this) | §80 |
| D-7 | §128, freeze | `presentation/library/**` frozen; requirements via host/new layers | freeze |
| D-8 | §126 | Every claim carries `[M]/[R]/[V]/[N]` | §126 |
| **D-9 (R)** | §95, §3, D-4 | **Live-first, delete-when-green:** Phases 3–5 invest only in live screens; dormant stacks deleted at the tail of the phase whose live path replaced them (search→after Search screen, queue→after Queue, audio-v2→after EQ decision, etc.) | plan sequencing |
| **D-10 (R)** | §4, §99 | **Proceed without missing screenshots**: written numbers + `images/UI target/` + build screenshots; when real refs arrive, fine-tune then | D-0 |
| **D-11 (R)** | §82, §17 | **Remove the dead `AppLanguage` selector**; no i18n scope (plan has none) | §82 |
| **D-12 (R)** | §11, §130 | **Remove MaterialYou `dynamicColors`; one RIKO palette** (code's own `// REVERT in Plan 2` agrees) | §11 |
| D-13 | §74, §86 | EQ: invest in live `EqualizerSheet`; port a11y patterns from dead `AudioScreen` in Phase 10; then delete audio-v2 per D-9 | §86 |
| D-14 | §100 | WaveLaboratory becomes reachable via **hidden entry** (long-press Settings version row) in Phase 7 F — never a visible route | §100 + §94 |
| D-15 | §7, §19 | Lyrics: add the missing NP menu entry → route existing lyrics stack; honest empty state when no lyrics; polish in Phase 5/6 | §7 screens list |
| D-16 | §11, §8 | Phase 3 components read **explicit** `RikoTypography.*` roles (no Material slots); single theme entry + slot routing lands in Phase 5 Step 0 (keeps visual change reviewable) | §8, §96 |
| D-17 | workflow | Phase-complete commits are exactly `Phase N editing completed`; intermediate red-fix commits use `fix(phase N): …` | user contract |
| **D-18** | §80, §86, §95, D-9 | **Phase 3 deferral set**: value-changing reconciliations (TopSheet 28dp, raw press scales), focus-ring rollout (§86), legacy `GlassSurface` consumers, V2 trio + full mini retirement (visual/freeze-sensitive) — re-homed with reasons, never dropped | §80 "value changes not in Phase 3", D-4 |
| **D-19** | G-18, D-9, §23 | **VisualOsHost tree = documented-RETIRED** (never mounted; live = Plan 2 shell + SheetMiniBar); physical deletion re-homed to Phase 5 tail; full back-chain pass stays [V] | G-18 wording, D-4 |

---

## 3. EXECUTION PHASES (file-level work order)

> Rule for **every** phase (§96): inspect → reference-search → smallest change
> → tests → push → **green CI** → §125 report → next. Unrelated changes never
> share a commit. Frozen tree (§0.7) untouchable.

### PHASE 0 — BUILD RESTORE ✅ DONE (`15df62bb`, CI `37006478998`)

Removed dead nested `object Easing` from `RikoMotion.kt`. Gate met.

### PHASE 1 — TRUTH + ARCHITECTURE ✅ DONE (`9348641d`, `f50b9ab7`, CI `37020089897`)

Delivered: `docs/TRUTH_MAP.md` (§8 register + risks), this file, 12 junk files
removed, `Project RIKO.zip` untracked + `*.zip` gitignored, UTF-8 re-encode of
`PROJECT_STATUS.md` + `ARCHITECTURE_FREEZE.md`, `.gitignore` hardening.
*Item 2 (debt table) re-verified in Phase 11 (§0.6 `[N]`)*.

### PHASE 2 — DESIGN SYSTEM ✅ DONE (`29062c96`, CI `37028654635`)

Delivered: `RikoSpacing`/`RikoComponentSize`/`RikoIconSize` (RikoDimensions),
`RikoShape` intent set, `RikoStateTokens` (new), `RikoTypography` + `TimeTabular`,
motion aliases `travel()`/`settle()` + dead `Duration` ladder removed, and the
§97 mapping table `docs/DESIGN_TOKEN_MAPPING.md` (8 areas + migration plan §9).
Scope note + re-homed items: see §0.8.

### PHASE 3 — COMPONENT SYSTEM (§76–§79, §95, §8) ⬜ NEXT

1. **G-17 re-measure** raw `AsyncImage` / row / mini counts `[M]`; update §0.3/§1.2 tables.
2. **`RikoArtwork` canonicalization (§76)** — replace raw `AsyncImage` sites in
   the unlocked tree, order: `PlayerSheetFull` → `PlayerSheetHost` → `HeroAlbum`
   → `NowPlayingBackground` → Home rows → collection rows → rest.
3. **Track row (§78)** — `RikoTrackRow` canonical (scaffolded `6be4c3f7`);
   migrate `V2TrackRow` / `RikoSongRow` / `CollectionTrackRow` consumers, then
   delete losers (D-4). Consistent semantics: artwork, title, subtitle,
   duration, playing state, trailing menu.
4. **Mini player (§79, §21)** — `SheetMiniBar`/`MiniPlayRing` canonical; delete
   `presentation/miniplayer/` (22f), `minicontact/` (4f), `RikoMiniPlayer`,
   `V2MiniPlayer`, `MiniPlayer.kt`, `MiniPlayerDockIntegration` after reference
   search; verify §21 (≈44dp artwork, title, artist, progress hairline, play/pause).
5. **V2 design system** — delete `V2AppBar/V2Cards/V2Sheet/V2Buttons/V2NpCards/
   V2BottomNav`; **rewrite 11 `V2*Test` against production components (§89)**.
6. **Playback controls** — remove dead `GradientSlider`, `PlaybackControls`,
   `TransportControlsV2` row (live `PlayPauseHeroButton` + `NpProgressBar` stay).
7. **Sheets/dialogs/shape/glass dups (G-13)** — one `GlassSheet`, one
   `DeletePlaylistDialog`, `AddSongsSheet` dead states, `RikoArtworkSheet`
   fate; collapse duplicate `RikoShapes`; `GlassSurface` → canonical import.
8. **G-14**: adopt `RikoStateTokens` (press scale, focus ring) across
   interactive components. **G-1/D-16**: component text moves to explicit
   `RikoTypography.*` roles.

*Gate:* green CI; test parity restored (§89); counts updated (G-17); §125 report.
*Not here:* library (frozen), screen visuals (5), value changes (§80).


**Phase 3 scope note (§126):**

- **G-17 re-measured [M]:** 20 raw `AsyncImage` call sites at phase start (docs said 26/27 — those counted references); 18 migrated, 2 remain in `V2Cards.kt` (file dies Phase 5, D-9); wrapper keeps its own call; 1 `rememberAsyncImagePainter` site (`VisualOsDockRenderer`) sits outside §76's `AsyncImage` wording — documented, not migrated.
- `RikoArtwork` extended with fill-mode (`size = null` + `fallback`) so sites keep perf-prescaled `ImageRequest`s (RC-06) and custom placeholders (heart/letter/gradient) — no perf or visual regressions, no value changes.
- **Claim disproofs (§126):** "two GlassSheet", `DeletePlaylistDialog` ×2, `RikoArtworkSheet`, AddSongsSheet "dead states" — none exist; AddSongsSheet has one live `filterText` state.
- **Deferral set D-18:** legacy `RikoShapes.TopSheet` (28dp) reconcile → Phase 5 (§80); raw press scales (GlassButton 0.95 / BubbleIcon 0.87 / TransportControls 0.90 / hero 0.86) → §80 (only 0.96-token sites adopted `RikoStateTokens`); focus-ring rollout → Phase 10/§86 (tokens exist, adoption infra absent); legacy `GlassSurface`→`RikoGlassSurface` consumer migration → Phase 5 (frozen library imports it; changes visuals); V2 cards/buttons/SectionHeader → Phase 5 tail (D-9); full mini-player retirement → Phase 5 tail (freeze-compatible sequencing); `RikoThemeV2` kept (live; G-9/G-1 stays Phase 5).
- D-16 component text: `GlassButton` moved to `RikoTypography.Button`; screen-level `MaterialTheme.typography` (18 files) stays for Phase 5 slices.

### PHASE 4 — NAVIGATION + INTERACTION (§22, §23, §107, §120) ⬜

1. **G-3**: add `android:enableOnBackInvokedCallback="true"`; verify
   `PredictiveBackHandler` (`PlayerSheetHost:191`) + `BackHandler`
   (`RikoNavGraph`/`MainActivity`) chain end-to-end [V].
2. **G-4**: single owner for coordinator callbacks (MainActivity **or**
   NavBridge, not both); preserve `onSkipToNext/Prev`; unit test; [V] dock pass.
3. Back hierarchy exactly §22: visualizer overlay → NP full → NP mini → route →
   system. `/now_playing` stub (`RikoNavGraph.kt:453–469`) — no dead-end/dup.
4. Tab behaviour + state restoration (§22, §120: return to exact scroll/tab/queue
   position; SavedStateHandle).
5. §23 gesture table per gesture: affordance + feedback + result —
   `PlayerSheetFull` drag/expand/collapse, queue reordering, waveform seek.
6. **G-18**: `VisualOsHost` mount decision (mount deliberately or document
   retired); move `RikoSheetType` out of `VisualOsSceneMap` to the sheet
   contract owner.
7. §107 Lab → Now Playing back target.

*Gate:* green CI + navigation instrumented test + [V] back-chain report.


**Phase 4 scope note (§126):**

- **G-3:** `enableOnBackInvokedCallback=true` added; NavGraph `BackHandler` is now conditional (chain-consumes OR stack can pop) so back at the root reaches the system instead of being swallowed; chain code-read as §22: overlay → NP full → NP mini → route → system [V device].
- **G-4:** single owner = `VisualOsCoordinatorWiring` (MainActivity-only, unit-tested `VisualOsCoordinatorWiringTest`: assign/delegate/pipeline/dispose). NavBridge stripped of 4 never-called params + its callback block — its `onTogglePlayPause`/`onExtractColors` were EMPTY stubs that shadowed the real wiring (last-writer bug); onSkipToNext/Prev preserved via MainActivity wiring.
- `/now_playing` stub: reviewed — canonical handoff (expand sheet + self-pop + AllSongs fallback), no dead-end, unchanged.
- **§120:** tab nav already `restoreState`/`saveState`; MyMusic/Favorites already `rememberSaveable`; Home `selectedCategory` was plain `remember` → now `rememberSaveable`. Remaining plain-remember states audited = transient overlays (sheets, dialogs, drag, animations) — deliberately not saved.
- **§23 gestures [V]:** all three exist — sheet drag (`anchoredDraggable` + progress morph + tap-expand), queue reorder (drag handle + `contentDescription` + real `playerManager.reorderQueue`), waveform seek (`detectTapGestures`/`detectDragGestures` + glowing thumb).
- **G-18:** decision = **RETIRED, documented** in both VisualOsHost files (0 call sites [R]); `RikoSheetType` moved to `visualos/contract/`; physical deletion → Phase 5 tail (D-19).
- **§107:** `WaveLaboratoryScreen` gained `onBack` + `BackHandler` + header back arrow (target = Settings when D-14 entry lands, Phase 7 F); instrumented `RikoBackChainTest` covers header+system back; NP back target = existing sheet handlers.
- *Gate:* CI green (user-reported) + `RikoBackChainTest` on device + [V] back-chain report.

### PHASE 5 — SCREEN-BY-SCREEN UX (§10–§18, §81–§87, §91, §114, §127) ⬜

**Step 0 — design-system gates (before any screen):**
- **G-1/D-16**: unify theme entry — `RikoMusicTheme` maps every Material slot
  to canonical `RikoTypography.*` (mirror `RikoTheme.kt:27–42`); retire legacy
  `theme/Type.kt` scale; one entry point (§8). Extend the theme test (G-16).
- **G-9/D-12**: remove `dynamicColors`; single RIKO palette; then color work is
  per-screen slices (373 refs → tokens, `[N]` normalisations confirmed by eye).
- **G-11/D-11**: remove dead `AppLanguage` selector.

> **Step 0 DONE** (Phase 5 head): typography slots unified (`RikoMaterialTypography`, both theme entries; `theme/Type.kt` deleted; G-16 test added) + `AppLanguage` selector removed (D-11) + G-9 MaterialYou claim disproven (never existed; artwork accent bridge kept).

**Screens, in order** — Home → My Music/Collection → Favorites → Playlists →
Search → Settings → Queue → supporting sheets → Onboarding/Splash.
Per screen (§127 — obvious requirements are mine):
- §13–18 explicit requirements (hierarchy, no dead navigation, lazy/stable keys,
  no glass-per-row, favorites play/shuffle/count/undo, playlist validation +
  destructive confirmation + error recovery, search debounce + IME + result
  count, settings reflect everywhere).
- States: loading skeleton · empty (typed, §85) · error · permission ·
  disabled · selection · destructive confirmation (§83).
- **G-7 seed list** re-verified per screen: wire or delete every dead control.
- Token migration slice per screen: typography slots → roles, shapes →
  `RikoShape`, spacing → `RikoSpacing`, raw tweens → `RikoMotion`, colors →
  tokens (DESIGN_TOKEN_MAPPING tables).
- **G-2/D-15**: Lyrics NP menu entry + sheet contract; honest no-lyrics state.
- **G-8/D-13**: EQ = live `EqualizerSheet` gets the work.
- **G-20**: library/collection files — path-checked against freeze before edits.
- §91 SEE/FEEL/DO notes + §125 report per screen group.

**Tail — dormant retirement (D-9):** when a live path is green, reference-search
then delete its dormant stack (search-v2 → after Search; queue-v2 → after
Queue; personalization → after Settings; **G-15** release/debug trees → after
Step 0 / Phase 7 F decision).

*Gate per screen group:* green CI + report; final gate: full §0.4 table shows
live-only for retired areas.

### PHASE 6 — NOW PLAYING (§19–§21, §43–§44, §104, §110) ⬜

1. Deep polish `PlayerSheetFull` (954) + `PlayerSheetHost` (373).
2. §20 continuity: artwork morph, background crossfade, palette blend — mini→full
   is one spatial transformation; no teleport/gray flash/jump.
3. §21 mini: exactly one; ≈44dp artwork; **one playback-position truth** (ring
   4 Hz vs `NpProgressBar` — confirm single source).
4. **G-5/§104 state matrix**: buffering, error + retry, prev/next disabled at
   ends, **repeat control** (wire `cycleRepeatMode()`), unknown duration → `LIVE`
   + seek disabled with explanation, **wire `onOpenAddToPlaylist`** (currently
   no-op → `AddSongsSheet`), scrub feedback (§110 thumb, §43 interpolation).
5. **G-14**: time/progress read-outs → `RikoTypography.TimeTabular` (§11).
6. **G-9**: single artwork→color pipeline for NP background (DynamicColors gone
   per D-12; `ColorIntelligenceEngine` vs `AlbumPalette` resolved — §44/§45).
7. `Plasma*` NP suite split per D-4/R-3: tokens + canvas stay (Phase 7 E needs
   them), unused card wrapper goes — careful, reference-searched.

*Gate:* green CI + §104 matrix filled `[M]/[V]` + 100-cycle mini↔full check [V].

### PHASE 7 — SPECTRUM / PLASMA WAVEFORM (§6, §27–§75, §98–§106, §113) ⬜

**Re-scoped per D-5: verify → wire → tune → measure. Not a rewrite.**

- **A — Audio tap** [R]: extend `PcmFramesTest` to seek/track-change/pause.
- **B — DSP** [R]: `Fft`→`SpectrumEngine`→`SpectrumBands`; **D-3** move
  resample tests to production path; single band truth (D-2) — kill legacy
  `mBandsTableS10` table in `SpectrumEngine`.
- **C — Renderer** [V]: dummy data → tune against §29–32 written numbers (D-10);
  **§32 `windowAnchor` becomes a named `PlasmaTokens` token** (default `THUMB`).
- **D — Real audio** [V]: real `SpectrumFrame`; no stale data on
  pause/seek/track-change; **§50/§67 wire `fftSizeFor(tier)`** (benchmark before
  2048 — "do not automatically select the largest FFT").
- **E — Now Playing integration** [V]: waveform live in NP (§113); **§45/§46
  AlbumPalette ridge colors + color modes enter the live path**; §34 window
  behaviour; battery/reduced-motion (§64–§66) wired into live renderer (G-10).
- **F — Lab** [R→V]: presets/persistence/reset exist; **fix G-10 defects**
  (`updateVisualizerPrefs` race, `onFlush` `lastEmitNanos`); **D-14 hidden
  entry** so §100 panel is actually reachable; honest ERROR/OFF states; **G-19**
  8-way waveform registry collapses to one.
- **G — Performance** [V]: record frame time (target ≤8.3 ms @120 Hz —
  measure, don't invent §72); allocation profile (§70 `duplicate()` per chunk);
  §73 memory loop (10 track changes / 100 play-pause / 50 expand / Lab cycles);
  battery. §68 PCM-offload → honest PREVIEW; §69 playback sacred + tests.
- §40/§75: confirm phase/progress/levels never reach composition (draw-phase
  reads); §111 waveform touch target ≥48dp; §102 quality levels honest.

*Gate:* green CI + DSP unit tests + §98 measurable numbers recorded `[V]`.

### PHASE 8 — MOTION / MICROINTERACTION (§24, §80, §109, §115, §116) ⬜

1. Verify Phase 3/5 migrations left ~0 raw `tween(N)`/`spring()` call sites
   outside named holds (DESIGN_TOKEN_MAPPING §7 `[R]/[N]` tables → 0).
2. §115 microinteraction matrix walked component-by-component (press, hover,
   toggle, favourite, reorder, drag, swipe, slider, tab, sheet) against
   `RikoMotion` tiers; press states via `RikoStateTokens` (G-14).
3. §109 play/pause motion; §24 reduced motion (decorative stops, feedback
   stays); §116 no timer choreography audit.
4. No motion-value changes hidden in other phases (§80).

*Gate:* green CI + §115 matrix table filled + reduced-motion spot check [V].

### PHASE 9 — PERFORMANCE (§40, §71–§75, §92, §121) ⬜

Baseline → measure → smallest useful change → re-measure → compare.
- **G-19**: `SessionViewModel` single-read-per-emission (kill 3 buffers +
  full-screen re-read); `ReorderableLazyList` duplication; NP state re-read.
- Recomposition audit §74; start-up, scroll, sheet, NP, spectrum, artwork,
  search paths; budgets §71 (frame/GPU/CPU/memory/reliability).
- Fixed list must show real before/after numbers `[M]`.

*Gate:* green CI + §121 numbers table + report.

### PHASE 10 — ACCESSIBILITY + EDGE STATES (§25, §26, §86, §87, §104, §108) ⬜

- TalkBack sweep incl. **`EqualizerSheet` sliders** (G-8: role/label/value/
  increment semantics) ; semantic role/label/state/selected everywhere;
  §104 semanticLabel list (buffering/error/no-lyrics/artwork/track position).
- 48dp targets, contrast, focus rings (tokens exist → verify live), keyboard/DPAD.
- §87: 1.0–2.0× font, no clipping (esp. queue rows, settings, sheets).
- §108 Lab a11y; §26 edge matrix fully covered (zero/one song, huge library,
  missing artwork, broken metadata, long titles, unknown duration, loading,
  error, buffering, rotation, process death, low RAM, battery saver, thermal,
  visualizer unavailable).

*Gate:* green CI + device a11y pass `[V]` + §26 matrix table.

### PHASE 11 — FINAL VALIDATION (§88, §90, §117–§124, §131, §132) ⬜

1. Full test suite + parity proof (§89): every green test maps to shipped code.
2. §90 screenshot validation against available refs (D-10) [V].
3. §117 device matrix · §120 lifecycle (rotation/process death/background) ·
   §121 perf numbers final · §118 a11y final · §119 motion final.
4. §122 product audit as one product (every "Does X feel…?" question answered
   with evidence) → §131 formula → §132 experience statement.
5. Docs refresh: TRUTH_MAP counts, PROJECT_STATUS debt table (§0.6 `[N]`),
   this file's status board → all ✅. Then **§124 "done" gate**.

*Gate:* green CI + full §125 final report + §124 checklist.

---

## 4. VERIFICATION LOOP (operational)

```
edit → commit → push → GitHub Actions
        ├─ :app:testDebugUnitTest
        └─ assembleDebug   → green = phase closes (§125 report)
red CI → fix before any further phase work
```

- Phase-complete commit message (exact): `Phase N editing completed`
  (N = 3, 4, … 11). Intermediate red-fix commits: `fix(phase N): …` (D-17).
- My loop (I run these and report to you):
  - `git push origin riko-perf-fixes` (PAT via remote auth — **never** committed; **rotate the PAT when the project closes**)
  - `gh run list --repo skradoan007-crypto/puuuuuush --branch riko-perf-fixes --limit 5`
  - `gh run watch <run-id> --repo skradoan007-crypto/puuuuuush --exit-status`
- Green ≈ 7–8 min; fail-fast ≈ 2.5 min. You may also just tell me green/red —
  your call always wins (red → I fix → rebuild → report).
- APK artifact `riko-music-debug-apk` (30-day retention) is the installable
  build for [V] checks.
- Nothing declared complete without a green run (§124).

---

## 5. RISK REGISTER

| ID | Risk | Prob | Impact | Mitigation |
|---|---|---|---|---|
| R-0 | HEAD unbuildable | ~~High~~ **closed** (Phase 0) | — | every phase re-checks CI first |
| R-1 | No local compiler → slow loop | High | Med | small single-purpose commits; compile-risk review before push |
| R-2 | Dormant deletion breaks hidden reference | Med | High | §95/D-4 protocol: reference search → delete → CI |
| R-3 | `Plasma*` tokens live while wrappers dead | Med | High | careful split (Phase 6 item 7) |
| R-4 | Reference images absent → geometry drift | Med | Med | D-10: written `[M]` numbers, `[A]` tokens, re-check on arrival |
| R-5 | Theme-entry merge → visual regression | Med | Med | Phase 5 Step 0 as its **own** commit; screen-by-screen visual review |
| R-6 | Tier/FFT touches audio-safe path | Med | **High** | §69: passthrough untouched; unit tests; playback sacred |
| R-7 | Frozen library edited accidentally | Med | High | D-7; every diff touching `presentation/library/` reviewed |
| R-8 | Green CI proves dead code (§89) | High | Med | Phase 3 rewrites `V2*Test` against production |
| R-9 | Feature-complete ≠ complete (§3) | Med | High | §91 per screen + §122 final audit |
| R-10 | Doc drift/mojibake | Low | Low | Phase 1 fixed; Phase 11 refresh |
| R-11 | Phase 5 is huge → scope creep | Med | High | strict per-screen commits + gates; Step 0 separate |
| R-12 | Stack deletion breaks string-routed nav | Med | Med | reference search includes `RikoDestination` routes/tests |
| R-13 | Removing MaterialYou surprises user | Low | Low | ratified D-12; single revertable commit |
| R-14 | Lyrics entry exposes unfinished lyrics UX | Med | Med | D-15: wire + honest empty state first; polish Phase 5/6 |
| R-15 | Callback fix changes dock behavior | Med | Med | single owner + unit test + [V] dock pass |
| R-16 | Multi-doc drift (this file vs TRUTH_MAP vs DESIGN_TOKEN_MAPPING) | Med | Med | this file = phase plan; others = registers; Phase 11 reconciles counts |

---

## 6. REPORT FORMAT (verbatim, §125) + HONESTY (§126)

Every phase report contains exactly these headings, no vague "everything is
improved":

**Files Changed · Symbols Changed · User-Visible Changes · UI/UX Changes ·
Reference Integration · Spectrum Changes · Architecture Changes · Performance
Changes · Tests · Tests Actually Run · Build · Device Verification · Remaining
Gaps**

Tags on every claim: `[M]` measured · `[R]` verified in repo · `[V]` requires
device · `[N]` not tested. Assumptions never become facts.

---

## 7. SCOPE CONTROL REMINDERS

- §127 — obvious professional requirements (back, loading, empty, error,
  confirmation, disabled, selection, a11y, focus, persistence, recovery) are
  **mine to identify**; "the plan did not mention it" is not an excuse.
- §128 — existing code is not automatically correct; improve conflicts with
  product intent, never rewrite for its own sake.
- §129 — reference is not automatically correct; adapt where it harms RIKO.
- §130 — RIKO must not become Samsung/Spotify/Apple/generic-Material clone.
  *Target: the quality of the reference, expressed through RIKO.*
- §123 — professional = coherent, intentional, restrained, discoverable,
  responsive, accessible, reliable, measurable, maintainable, performant —
  **not** more gradients/blur/animation/screens.
- §93 — third-party code: research only unless named in plan; AGSL/blur/deps
  already approved in-plan.
- §134 — *"Now Playing হলো product; বাকি সব তার orbit."* / *"গান চালাও →
  screen বেঁচে ওঠে।"*
- Ratified scope guards (2026-10-03): D-9 live-first deletion · D-10 written
  numbers · D-11 no i18n · D-12 one palette.

---

**END OF EXECUTION PLAN — ready; next action: PHASE 3.**
