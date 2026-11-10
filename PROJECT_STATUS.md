# RIKO EXPERIENCE OS — PROJECT STATUS

> Single source of truth। এই file-এ থাকে শুধু project map —
> milestones, frozen architectures, rules registry, completed এবং
> pending subsystems, technical debt, known risks, future roadmap।
> কোনো code না, শুধু map। যত বড় project হবে, এই file ততবার বাঁচাবে।
>
> **Maintained by:** Product Director (Riko ভাই) + AI Engineering
> **Living document:** প্রতিটা milestone lock হলে এই file আপডেট হবে।
> **Last updated:** 2026-10-05 — **Phases 0-10 ALL CI green** (agentic run: screen groups + tail + NowPlaying + Spectrum + Motion + Perf + A11y). Phase 11 = docs done + **device [V] gate (USER: artifact `riko music`)**. Full execution log: `docs/MASTER_PLAN_RECONCILED.md` → `★ EXECUTION LOG`.
> M2.9 deferred per Product Director redirect to UI integration.

## PERF ENGINEERING CAMPAIGN v1.0 — 2026-10-13 (CODE WAVES COMPLETE)

> **Plan:** `.mimocode\plans\1792178586648-silent-pixel.md` (approved). Method = Plan.txt adapted to Kotlin/Compose. **No framework change.**
> **Branch:** `riko-perf-fixes` · one Phase per commit (`Phase N editing completed`) · honesty tags `[M]/[R]/[V]/[N]`

### DONE — all pushed, invariants verified

| Commit | Phase | Wave | Summary |
|---|---|---|---|
| `4a23d969` | 18 | A | Audit docs: PERF_ENV_REPORT + PERF_DEP_COMPAT + PERF_HYPOTHESES (H1-H13) + PERF_NONPERF_BACKLOG |
| `8e3de30c` | 19.1 | B1 | JourneyGapBenchmark - J7 searchFling, J14 miniToFullMorphDrag, J15 backgroundResume |
| `e23fd68b` | 20 | C1+C3 | 88 collectAsState to collectAsStateWithLifecycle; hot reads to leaves (LabSpectrumCanvas draw scope, MiniPlayRing); list contentType gaps closed |
| `cfdfeb6a` | 27 | C8 | RikoLog (BuildConfig.DEBUG gate) - all Log.d/i behind it; Log.w/e preserved |
| `77da78ef` | 21 | C2 | H5 fix: songsCache starts emptyList() - filterNotNull hang killed |
| `a9a8d591` | 23 | C4 | Coil: remembered requests (MiniPlayerAlbum, PlaylistsScreen, RikoCollectionHero); mini crossfade(false) |
| `b58c7b9a` | 24 | C5 | WavePhaseClock frame loop visibility-gated (WaveformTimeline idiom); wired through AuroraFlow/NowPlayingBackground/PlayerSheetFull |
| followup batch | 28-33 | +H12+tests+docs | LyricsScreenWiring sliced combine; export button wired; transport glow; unit tests; this checkpoint |

### Verified invariants (hard check)
- presentation/library/** (D-7, 121 files) - ZERO diff
- Plain collectAsState() outside frozen tree - 0
- Ungated Log.d/i - 0
- PcmFrames to SpectrumEngine to SpectrumBands - intact
- Single PlayerManager/ExoPlayer - intact

### Blocked on USER (device measurement - cannot be done from this box)
1. Phase 19 (B) baseline numbers: `./gradlew :benchmark:connectedBenchmarkAndroidTest` + `powershell -File tools/collect-perf-results.ps1` on Device A/B(/C) then fill PERF_BASELINE.md, flip H1-H13 statuses.
2. W4 feel gate: Campaign section 22 checklist 1-18 with [V] notes.
3. CI report for the latest batch.

### Re-entry (after measurement)
Campaign v3 feature phases resume at Phase 31+ (6.2 Premium EQ UI, 7 visual unification, 8 playback residual, 9 a11y). Measure each feature frame cost on the J-suite before calling it done.

## ?. CAMPAIGN v3 SESSION CHECKPOINT � 2026-10-07 (Phases 0-17 GREEN � Waves 0-5 complete)

- **Chain contract:** plan `docs/superpowers/plans/2026-10-05-riko-professional-campaign.md` (v3) �24 execution order, one Phase per commit (`Phase N editing completed`) + PAT push + CI poll (green=next / red=fix+rebuild). TODO always updated.
- **DONE Phases 0-17:** Wave 0 evidence harness (benchmark buildType, macrobenchmarks, PERF_BASELINE) � Wave 1 LAG KILL (granular PlayerState, artwork factory+cache keys, row hygiene, root invalidations, baseline profile wiring) � Wave 2 GESTURE (EQ drag root-fix, queue reorder by Song-ID keys, RikoTouch/RikoGesture canonical system, app-wide sweep) � Wave 3 STARTUP (LibraryRepository shared cache+ContentObserver, StartupReadiness, platform splash+TTFD) � Wave 4 NP AURORA (AuroraMotion musical reactivity, AuroraFlow AGSL+canvas waves, plasma timeline in seekbar) � Wave 5 STUDIO (source inventory doc, SPECTROGRAM+SHAPE styles, RikoRotaryKnob, versioned user presets) � 6.1 EQ correctness (log-freq preset mapping, band_levels_csv schema+MIGRATION_3_4, round-trip tests).
- **Next = Phase 18 (6.2 premium EQ UI):** all bands + glow curve + user preset UI + live spectrum overlay + preamp; then Phase 19 (Wave 7 visual unification), 20 (Wave 8 playback reliability), 21 (Wave 9 a11y+text), 22 (Wave 10 measurement), 23 (final human feel gate).

---

## ?. CAMPAIGN EXECUTION STATE � 2026-10-06 (RIKO Professional Campaign v3 � Wave 0 DONE)

> **New campaign:** plan `docs/superpowers/plans/2026-10-05-riko-professional-campaign.md` (v3, director-upgraded) supersedes the old phase plan for quality work. Old history below stays valid.
> **Next:** say `CONTINUE-W` ? resume at **Wave 1 (LAG KILL)** per plan �24 order, slice 1 = Task 1.1 (kill monolithic PlayerState recomposition).

- **Wave 0 (Evidence Before Optimization) = DONE this session:** app `benchmark` build type (release-like R8+shrink, debug-signed) + `benchmark` variant in benchmark module; `<profileable android:shell=true>` in manifest; macrobenchmark harness `BenchmarkSupport.kt` + `StartupBenchmark.kt` (cold/warm/hot TTID/TTFD) + `ListFlingBenchmark.kt` (MyMusic/Favorites/Home fling) + `PlayerInteractionBenchmark.kt` (NP morph/scrub/queue/EQ drag/visualizer); `docs/performance/PERF_BASELINE.md` + `tools/collect-perf-results.ps1` (tables = `[N]` until device run).
- **Local compile impossible on this box** (no JDK/Android SDK; `local.properties` = Linux SDK path) ? compile gate = CI, per AGENTS.md workflow.
- **USER ACTION after CI green:** `./gradlew :benchmark:connectedBenchmarkAndroidTest` on device A ? `powershell -File tools/collect-perf-results.ps1` ? paste numbers into PERF_BASELINE.md. Those are the campaign truth numbers.
- **Rules in force:** v3 plan �2 non-negotiables (re-locate symbols before edit; symptom?root-cause?evidence?fix?verify; never mass-replace; wire-or-delete with proof; gate fail = no next wave; `[M]/[R]/[V]/[N]` honesty tags). Branch `riko-perf-fixes`.


## ★ EXECUTION STATE — 2026-10-05 (Phases 6-11 ADDENDUM · all green)

- **Phase 5 = DONE** (all 9 screen groups + tail D-9: ~70 dormant files deleted incl. search-v2/queue-v2/personalization/audio-v2 + VisualOsHost cascade + V2 trio migrated).
- **Phase 6 ✅ `ad74023f`**: repeat control, prev/next disabled at ends, NP error banner+retry, LIVE label, TimeTabular, Add-to-playlist picker wired.
- **Phase 7 ✅ `974cb2ff`**: PcmFrames seek/track-change/pause tests (run in CI); D-2/D-3/windowAnchor = disproven-or-done [R].
- **Phase 8 ✅ `9d7c02d2`**: §80 press depths normalized → `RikoStateTokens.PressScale`; tier mapping (200/220/400→normal/screen); shimmer/splash/ambient = named holds.
- **Phase 9 ✅ `eb82780e`**: G-19 duplicate×2→×1 per chunk; SessionViewModel/ReorderableLazyList claims stale (RC-01..13 pass had covered the family) [R].
- **Phase 10 ✅ `115edf3e`**: EQ per-band TalkBack actions + slider identity; audio-v2 deleted after a11y absorption (D-13).
- **Phase 11**: docs updated (this + plan `★ EXECUTION LOG`); **remaining = USER device [V]**: install `riko music` artifact, back-chain, EQ TalkBack, reduced-motion, §115/§121 numbers, screenshots (D-10).
- **Deferred (reasoned)**: legacy GlassSurface (frozen D-7), debug tree/WaveLab (D-14/Phase 7F), focus-ring/48dp/contrast/A87 → device spot-check [V]; row-press 0.975/0.98 = deliberate subtle tier.


## ★ EXECUTION STATE — 2026-10-03 (STOPPED HERE · say `CONTINUE-W` to resume)

> এটাই আজকের (২০২৬-১০-০৩) live execution state। **কাল `CONTINUE-W` বললে ঠিক এই section থেকে, এই context + rules + full attention নিয়ে resume হবে** — নতুন করে assumption নয়, অপ্রয়োজনীয়ভাবে restart নয়।
> Phase plan (single source of truth): `docs/MASTER_PLAN_RECONCILED.md` · workflow + memory: `AGENTS.md` · token map: `docs/DESIGN_TOKEN_MAPPING.md`

### আজ যা যা হলো (session order)

| ধাপ | কাজ | State / evidence |
|---|---|---|
| **Phase 3 — Component System** | §76 RikoArtwork (18 raw AsyncImage sites + fill-mode wrapper), §78 track row (V2TrackRow/RikoSongRow shims deleted), V2TopBar→RikoTopBar (V2AppBar deleted), RikoShapes collapse (riko plural deleted, legacy = TopSheet-only), G-14 state-token entry + D-16 GlassButton→RikoTypography.Button, §89 test rewrite (RikoTrackRowTest/RikoChromeTest, V2NowPlayingTest deleted) | ✅ CI `37127370271` green @ `cc5c2602`; §125 report দেওয়া হয়েছে; 3টা red→fixed (live helpers missing / em-dash corruption / RikoMaterialShapes lost) |
| **Phase 4 — Nav + Interaction** | G-3 `enableOnBackInvokedCallback=true` + conditional BackHandler (root-এ back সব গ্রাস করত — fixed), G-4 callback single owner = `VisualOsCoordinatorWiring` + `VisualOsCoordinatorWiringTest`, `RikoSheetType`→contract, G-18 VisualOsHost **RETIRED (documented)** + D-19, §107 Lab `onBack`+BackHandler+`RikoBackChainTest`, Home `selectedCategory`→rememberSaveable, §23 gesture audit (sheet drag/queue reorder/waveform seek তিনটেই live) | ✅ CI `37132159799` green @ `c33c4ffe` |
| **Phase 5 Step 0 — design gates** | G-1/D-16: `RikoMaterialTypography` single source (দুই theme entry), legacy `theme/Type.kt` deleted, `TypographySlotMappingTest` (১৩ slot + Baloo family); G-11/D-11: AppLanguage selector+contract removed (৬ file, zero leftover); G-9/D-12: MaterialYou disproven (0 `dynamicColorScheme` — আসলে ছিল না; artwork `DynamicColors` = vision identity, রাখা হলো) | ✅ user-reported green @ `08d3b037` |
| **Phase 5 — Home group** | G-7 চারটাই: top bar persistent (loading/error/empty-তেও) + "Prince Lovers" placeholder → time-greeting; card taps live; See-all Top Picks→MyMusic / Recently Played hidden (`V2SectionHeader` nullable); fake banner বাদ → `HomeFeaturedBanner`; Plan-11 chips live (`selectedCategory` → Top Picks source switch); D-9: ৬টা dormant file deleted (HomeHeader/HomeQuickActions/MoodsSection/HomeSections/HomeCollectionSections/HomeHeroCard; `artworkModelFor`→ArtworkModel.kt, `HomeHeroData`→HomeUiState) | ✅ red `37136066065` (sectionLabel String?) → fix `37136625166` green @ `b0691418` |
| **Phase 5 — My Music group (PUSHED, RED ❌ = STOPPING POINT)** | G-7: New tab = `dateAdded` desc + আলাদা `newListState` (আগে Suggested-এর duplicate + shared scroll); Downloads = typed `CollectionEmptyState` (ভুল scan CTA বাদ) + toolbar hidden; **D-12 Playlists entry**: QueueMusic icon (আগে `onPlaylistsClick` call-ই হতো না → Playlists unreachable!); dead cascade: `AlbumLibraryContent`/`ArtistLibraryContent`/`RikoAlbumCard`/`RikoArtistCard`/grid states/dead params+NavGraph args/দুই VM-এর albums-artists compute/`ArtistUi` (`AlbumUi` রাখা — CollectionDetail live) | ❌ commit `28b630c5`, CI `37138709201` FAILURE — error নিচে |

### 🔴 বাকি কাজ + কোথায় কী

1. **IMMEDIATE (আজকে থেমেছে):** `presentation/mymusic/MyMusicScreen.kt:150-163` — QueueMusic icon block ভেঙে `Icon(`-এর ভেতরে ঢুকে গেছে → `MyMusicScreen.kt:158` "Expecting an element" (CI `37138709201`)। **Broken state:**
   ```kotlin
   actions = {
       Icon(
           Icon(
               Icons.Filled.QueueMusic, ... );   // inserted block এখানে আটকেছে (152-157)
       Icons.Filled.Favorite, ...                 // 158: dangling args → parse error
   ```
   **Fix:** actions block-কে দুটো পূর্ণ আলাদা `Icon(...)` call হিসেবে নতুনভাবে লিখতে হবে (QueueMusic আগে, Favorite পরে; দুটোই 48dp + clickable + tint) → recheck → commit `fix(phase 5): ...` → PAT push → **STOP, user-এর CI report অপেক্ষা**।
2. **Phase 5 screen groups (plan order):** Favorites → Playlists → Search → Settings → Queue → supporting sheets (এখানেই **G-2/D-15 Lyrics NP entry** + **G-8/D-13 EqualizerSheet**) → Onboarding/Splash। প্রতিটার gate = green CI + report।
3. **Deferred (পরের গ্রুপেই আসবে):** Album/Artist detail routes এখন unreachable (entry ছিল শুধু deleted dead params) → **Search group-এ search-এর ALBUMS/ARTISTS sections এই detail routes-এ wire করব**; androidTest + full back-chain pass = device [V] (Phase 11)।
4. **Phase 5 tail (D-9):** dormant stacks delete (search-v2, queue-v2, personalization, G-15 release/debug trees), VisualOsHost physical deletion (D-19), V2 trio (V2Buttons/V2Cards/V2SectionHeader) migrate+delete, legacy `GlassSurface` consumers → canonical (D-18)।
5. **পরবর্তী phases:** 6 Now Playing → 7 Spectrum/Plasma → 8 Motion → 9 Perf → 10 A11y → 11 Final Validation।

### Rules / conventions / architecture (মেনে চলতে হবে)

- **Workflow chain:** phase পড়া → file select → (দরকার হলে research) → edit → প্রতিটি edit check → শেষে recheck (error/plan-drift) → commit `Phase [N] editing completed` (red-fix এ `fix(phase N): ...`) → push → **STOP; CI result user নিজে দেয়** (agent-এর দ্রুত run-check ঐচ্ছিক)।
- **D-9 live-first / delete-when-green:** reference-search আগে, তারপর delete; **frozen `presentation/library/**` ছোঁয়া নিষিদ্ধ** (D-7); value changes Phase-5 slices/§80-এ; §126 honesty — প্রতিটি দাবির tag `[M]/[R]/[V]/[N]`, disproven claim স্পষ্ট লিখব।
- **Canonical systems:** `RikoArtwork` (একমাত্র image loader, §76), `RikoTrackRow`, `RikoTopBar`, `RikoShape`+`RikoCornerRadius` (একটাই shape source), `RikoTypography`+`RikoMaterialTypography` (single slot source), `RikoMotion`, `RikoStateTokens`, PlayerSheetHost morph sheet (mini/full), `PcmFrames → SpectrumEngine → SpectrumBands`, single `PlayerManager`।
- **Communication:** সবসময় বাংলায় + emoji; ভুল/risk/limitation খুলে বলবি; closest friend + coding partner হিসেবে propose ও challenge করবি।
- **PowerShell pitfalls (এই session-এ শেখা):** `@(...)` array-এ `+` concat দিলে পুরোটা space-join হয়ে এক line হয়ে যায় → temp-file splice বা plain-string array; function `return`-এ List enumerate হয়ে fixed array হয় → inline List ব্যবহার; `git push` PAT `http.extraheader` দিয়ে (plain push hang করে)।

### Current stopping point → `CONTINUE-W` দিলে exact steps

1. `MyMusicScreen.kt:150-163`-এর actions block শুধু করা (উপরের broken state দেখে)।
2. Recheck: leftover grep + brace balance + `git status`।
3. `fix(phase 5): rebuild MyMusic top-bar actions (QueueMusic icon split Icon call)` commit → PAT push → **STOP** (user সবুজ/লাল জানাবে)।
4. Green হলে → **Favorites group** (plan: explore → G-7 re-verify → edits → recheck → `Phase 5 editing completed` → push → stop)।


## 0. IDENTITY VERDICT

RIKO Experience OS এখন আর "offline music player" না। এটা একটা **Experience
Operating System** — একটা personal audio universe যেখানে প্রতিটা pixel,
প্রতিটা gesture, প্রতিটা waveform user-এর শোনার অভিজ্ঞতাকে shape করে।
Identity চারটা pillar-এ দাঁড়িয়ে আছে:

1. **Glass** — depth, blur, material honesty. Background কখনো flat না।
2. **Wave** — music যখন বাজে, screen যখন বাঁচে। Wave Engine হলো heartbeat।
3. **Color Intelligence** — album art থেকে extracted palette পুরো screen-কে
   saturate করে। User কখনো "static theme" দেখে না।
4. **Living Motion** — motion decorative না, semantic। প্রতিটা animation
   একটা information বহন করে (Rule #73)।

এই চার pillar-এর মধ্যে কোনো feature না, শুধু feel — আর feel-ই হলো
RIKO-র পরিচয় (Rule #72)। Identity Test: logo ঢেকে দিলেও ৫ সেকেন্ডের মধ্যে
"এটা RIKO" বলে চেনা যাবে — এটাই M2.9-এর সবচেয়ে important gate।

---

## 1. CURRENT STATUS OVERVIEW

```
███████████████████████████░░  ≈ 93–95%
```

| Subsystem                | Status        | Score      | Build  | Lines   |
|--------------------------|---------------|------------|--------|---------|
| Foundation (DI, Theme)   | ✅ Complete    | —          | M0     | ~3000   |
| Experience Hub           | ✅ Complete    | —          | M0.5   | ~2500   |
| Color Intelligence       | ✅ Complete    | 10/10      | M1     | ~4200   |
| Wave Engine              | ✅ Complete    | 10/10      | M1     | ~5500   |
| Rendering Runtime        | ✅ Complete    | 10/10      | B4     | ~3900   |
| Now Playing              | ✅ Complete    | —          | M1.5   | ~6000   |
| Mini Player              | ✅ Complete    | —          | M1.6   | ~4500   |
| Library Runtime (B1-B2)  | ✅ Complete    | 10/10      | B1-B2  | ~8500   |
| Collection Engine (B3)   | ✅ Complete    | 9.998/10   | B3     | ~3500   |
| Rendering UI (B5.3)      | ✅ Complete    | 10/10      | B5.3   | ~14453  |
| Library Constitution (B5.1)| ✅ Complete  | 10/10      | B5.1   | ~4914   |
| Screen Runtime (B5.2)    | ✅ Complete    | 9.99/10    | B5.2   | ~3720   |
| Interaction Runtime (B5.4)| ✅ Complete   | —          | B5.4   | ~10937  |
| Host Integration (B5.5)  | 🟡 Reported   | —          | B5.5   | ~3901   |
| Visual OS Integration (12f)| 🟡 Reported   | —          | M2.9→ | ~3,500  |

**Total locked codebase:** ~98,500+ lines of pure Kotlin + Compose across
~155+ files.

**Critical note:** Host Integration (B5.5) এবং Visual OS Integration status "🟡 Reported" —
code লেখা হয়েছে কিন্তু compile, run বা device validation হয়নি।
"LOCK" verdict শুধু M2.9 Experience Validation শেষ হলেই দেওয়া যাবে।

**Visual OS Integration (12 files):**
- `VisualOsSceneMap.kt` — Route → SceneType mapping (~306L)
- `VisualOsNavBridge.kt` — NavController ↔ Coordinator bridge (~270L)
- `VisualOsHost.kt` — Render orchestrator composable (~130L)
- `VisualOsBackgroundRenderer.kt` — Living background rendering (~256L)
- `VisualOsDockRenderer.kt` — Mini player dock rendering (~391L)
- `VisualOsOverlayRenderer.kt` — Overlay rendering (~357L)
- `VisualOsHandoffRenderer.kt` — Dock ↔ NowPlaying handoff (~361L)
- `VisualOsBackPressChain.kt` — Back press priority chain (~68L)
- `VisualOsDeviceTierProvider.kt` — Device tier + system state (~141L)
- `VisualOsAccessibilityBridge.kt` — Focus order (~130L)
- `RikoNavGraph.kt` — Rewritten with Visual OS wrap (~270L)
- `MainActivity.kt` — Rewritten with ProvideVisualOs (~220L)

---

## 2. MILESTONE ROADMAP

Project শুরু থেকে আজ পর্যন্ত এবং এখন থেকে v1.0 পর্যন্ত সব milestone এক
জায়গায়:

### Completed Milestones

| Milestone | Title                      | Status      | Lock Date    |
|-----------|----------------------------|-------------|--------------|
| M0        | Foundation                 | 🔒 LOCKED   | Early phase  |
| M0.5      | Experience Hub             | 🔒 LOCKED   | Early phase  |
| M1        | Color + Wave Engine        | 🔒 LOCKED   | Mid phase    |
| M1.5      | Now Playing                | 🔒 LOCKED   | Mid phase    |
| M1.6      | Mini Player                | 🔒 LOCKED   | Mid phase    |
| M2 (build)| Library (B1–B5.5)          | 🟡 Reported | Current      |

### Active ও Upcoming Milestones

| Milestone | Title                          | Status      | Goal                                                    |
|-----------|--------------------------------|-------------|---------------------------------------------------------|
| **M2.9**  | **Experience Validation**      | ⏳ Next     | No new code. Validate identity, memory, frame, stress। Identity Test ⭐ |
| M3        | Search Experience              | Pending     | Search build using frozen architecture (Rule #70)      |
| M4        | Queue + Lyrics                 | Pending     | Queue management, lyrics sync, both via same pipeline  |
| M5        | Audio (Equalizer, FX)          | Pending     | Audio post-processing, equalizer UI                     |
| M6        | Settings                       | Pending     | Settings screen using Collection card pattern           |
| M7        | Device Validation              | Pending     | Real-device fleet testing, OEM-specific quirks          |
| M8        | Release Candidate              | Pending     | Feature freeze, final polish, store-readiness           |
| v1.0      | Production Release             | Pending     | Public launch                                           |

### M2.9 — Experience Validation (Detail)

> এটাই এখন সবচেয়ে important milestone। নতুন feature লেখা নিষেধ।
> শুধু validate।

**6 Validation Gates:**

1. **Library Reality Test** — 10K / 20K / 50K / 100K songs লোড করে
   memory, scroll, render পরীক্ষা। প্রতিটা scale-এ আলাদা।
2. **Memory Audit** — Leak / Bitmap / Coroutine / Flow / Snapshot।
   প্রতিটা leak source আলাদাভাবে trace করা হবে।
3. **Frame Timing** — 30Hz / 60Hz / 90Hz / 120Hz / 144Hz। প্রতিটা
   refresh rate-এ frame budget (16ms / 11ms / 8ms / 6.9ms) verify।
4. **Animation Continuity** — Mini Player → Library → Now Playing →
   Back → Search → Again, ১০০ বার। Morph continuity বারবার ভাঙে কিনা।
5. **Stress Test** — Scroll + Play + Queue + Favorite + Search + Rotate
   + Background + Foreground + Bluetooth + Headset + Notification,
   সব একসাথে এবং interleaved।
6. **Identity Test ⭐** — Logo ঢেকে দিয়ে app খুললে ৫ সেকেন্ডের মধ্যে
   "এটা RIKO" চেনা যায় কিনা। যদি উত্তর হ্যাঁ — identity pass।

M2.9 শেষ না হওয়া পর্যন্ত M2-কে "LOCKED" বলা honest না।

---

## 3. FROZEN ARCHITECTURE

### Signature Pipeline (Rule #70 — Architecture Freeze)

নতুন milestone-এ নতুন architecture invent করা যাবে না। শুধু
existing pattern reuse হবে। RIKO-র signature pipeline এখন frozen:

```
Contract → Runtime → Reducer → Mapper → Rendering Runtime → Compose → Integration
```

প্রতিটা নতুন screen / subsystem এই pipeline follow করবে। এটা ভাঙলে
product-এর পরিচয় নষ্ট হয়।

### Layered Architecture (Locked Layers Table)

নিচের সব layer আনুষ্ঠানিকভাবে LOCKED 🔒। এগুলোতে আর নতুন feature যোগ
করা যাবে না — শুধু bug fix এবং documentation যাবে।

| #  | Layer                                  | Path                                          | Files | Lines  | Build | Score     | Status        |
|----|----------------------------------------|-----------------------------------------------|-------|--------|-------|-----------|---------------|
| 1  | Collection Constitution                | `presentation/library/contract/`              | 15    | ~6000  | B1 v2 | 10/10     | 🔒 LOCKED     |
| 2  | Collection Runtime                     | `presentation/library/runtime/`               | 5     | ~2500  | B2    | 10/10     | 🔒 LOCKED     |
| 3  | Collection Engine                      | `presentation/library/engine/`                | 13    | ~3500  | B3    | 9.998/10  | 🔒 LOCKED     |
| 4  | Rendering Runtime                      | `presentation/library/rendering/`             | 14    | ~3900  | B4    | 10/10     | 🔒 LOCKED     |
| 5  | Library Screen Constitution            | `presentation/library/screen/contract/`       | 15    | ~4914  | B5.1  | 10/10     | 🔒 LOCKED     |
| 6  | Library Screen Runtime                 | `presentation/library/screen/runtime/`        | 8     | ~3720  | B5.2  | 9.99/10   | 🔒 LOCKED     |
| 7  | Library Background Language            | `presentation/library/screen/background/`     | 5     | ~2125  | B5.3-A| 10/10     | 🔒 LOCKED     |
| 8  | Library Identity Layer                 | `presentation/library/screen/identity/`       | 9     | ~4823  | B5.3-B| 9.995/10  | 🔒 LOCKED     |
| 9  | Library Living Motion                  | `presentation/library/screen/motion/`         | 6     | ~3636  | B5.3-C| 10/10     | 🔒 LOCKED     |
| 10 | Library Composition                    | `presentation/library/screen/composition/`    | 4     | ~3869  | B5.3-D| 10/10     | 🔒 LOCKED     |
| 11 | Library Interaction Runtime            | `presentation/library/interaction/`           | 15    | ~10937 | B5.4  | (pending) | 🔒 LOCKED     |
| 12 | Library Host Integration               | `presentation/library/host/`                   | 7     | ~3901  | B5.5  | (pending) | 🟡 REPORTED   |

**Total locked codebase:** ~94 files, ~38,800 lines (Phase A–D pure
rendering, zero business logic)। B5.4 + B5.5 যোগ করলে মোট ~50,000+ lines
Library subsystem-এ একা।

### Architecture Invariants (10 — Locked)

এই 10টি invariant এখন থেকে ভঙ্গ করা যাবে না:

1. Reducer is pure: `(State, Event) → Pair<State, List<Action>>`
2. Reducer NEVER calls Engine — Coordinator does (Rule #47)
3. Coordinator is pure Kotlin — returns `CoordinatorRequest` objects
4. Host (B5.5) executes `CoordinatorRequest` async (Coroutine/Flow)
5. Engine results arrive as new Events — Reducer handles in separate pass
6. State is immutable — Reducer returns new state, never mutates
7. Actions describe intent — never execution (Rule #48)
8. Diagnostics is observation-only — never affects behavior (Rule #53)
9. Mapper is the only place composition happens (Rule #55)
10. At most one Hero per screen (Rule #57)

### Post-Freeze Protocol

✅ **Allowed on locked layers:**
- Bug fix (incorrect behavior)
- Documentation update
- Test addition
- Performance optimization (behavior unchanged)

❌ **Forbidden on locked layers:**
- নতুন field যোগ কোনো locked contract-এ
- নতুন event / action / request যোগ কোনো locked runtime-এ
- নতুন sealed variant যোগ কোনো locked sealed class-এ
- API signature change কোনো locked file-এ
- Package move / rename

যদি কোনো feature সত্যিই দরকার হয় — সেটা নতুন build-এ যাবে (B6+),
নতুন contract তৈরি করে, locked contracts কে extend করে।

---

## 4. RULES REGISTRY (#1 – #83)

> Rule freeze directive (Product Director): নতুন rule numbering
> এখন আর হবে না যদি না সত্যিই কোনো cross-project principle থাকে।
> Project maturity-তে discipline এখন internalized — নিয়ম না বাড়িয়ে
> enforcement-এ ফোকাস।

### B1 — Collection Constitution (#1 – ~#20)

Foundation rules যা Collection Contract তৈরি করেছে — Single Source of
Identity, Identity Never Recomputed, Snapshot Is Truth At A Point In
Time, ইত্যাদি। এই rules আজও enforce হয় এবং প্রতিটা নতুন contract
তৈরির সময় এগুলো মানতে হয়।

| #    | Rule                                       | Status        |
|------|--------------------------------------------|---------------|
| #1   | Single Source of Identity                  | 🔒 Enforced   |
| #2   | Identity Never Recomputed                  | 🔒 Enforced   |
| #3   | Snapshot Is Truth At A Point In Time       | 🔒 Enforced   |
| #4–#43 | (Foundation discipline — abbreviated)    | 🔒 Enforced   |
| #44  | Stable Key Always                          | 🔒 Enforced   |

### B2 / B5.2 — Collection Runtime + Screen Runtime

| #    | Rule                                       | Status        |
|------|--------------------------------------------|---------------|
| #45  | Events Are Verbs (Past Tense)              | 🔒 Enforced   |
| #46  | No Random / No UUID In Domain              | 🔒 Enforced   |
| #47  | Reducer Never Calls Engine                 | 🔒 Enforced   |
| #48  | Actions Are Nouns                          | 🔒 Enforced   |
| #54  | Screen Tells a Story                       | 🔒 Enforced   |
| #55  | Screen Owns Composition                    | 🔒 Enforced   |
| #56  | (reserved — implicit)                      | —             |
| #57  | Every Screen Has One Hero                  | 🔒 Enforced   |

### B3 — Collection Engine

| #    | Rule                                       | Status        |
|------|--------------------------------------------|---------------|
| #49  | Strategy Never Scores                      | 🔒 Enforced   |
| #50  | Scorer Never Ranks                         | 🔒 Enforced   |
| #51  | Ranking Never Filters Identity             | 🔒 Enforced   |
| #52  | Cache Never Changes Data                   | 🔒 Enforced   |
| #53  | Diagnostics Never Affects Engine           | 🔒 Enforced   |

### B5.3 — Library Rendering UI (Phases A–D)

| #    | Rule                                       | Status        |
|------|--------------------------------------------|---------------|
| #58  | Runtime Never Knows Rendering              | 🔒 Enforced   |
| #59  | Mapper Is The Last Translator              | 🔒 Enforced   |
| #60  | Screen State Is Already Renderable         | 🔒 Enforced   |
| #61  | Coordinator Never Mutates State            | 🔒 Enforced   |
| #62  | Never Rebuild Existing Systems             | 🔒 Enforced   |
| #63  | Background Never Demands Attention         | 🔒 Enforced   |
| #64  | Motion Must Be Discoverable, Not Obvious   | 🔒 Enforced   |
| #65  | Hero Never Competes                        | 🔒 Enforced   |
| #66  | Collections Feel Discoverable              | 🔒 Enforced   |
| #67  | Dock Never Interrupts                      | 🔒 Enforced   |
| #68  | Hero Explains Library                      | 🔒 Enforced   |
| #69  | Sections Breathe Independently             | 🔒 Enforced   |
| #70  | Architecture Freeze                        | 🔒 Enforced   |
| #71  | New Screen Must Feel Familiar              | 🔒 Enforced   |
| #72  | Identity Before Feature                    | 🔒 Enforced   |
| #73  | Motion Must Have Meaning                   | 🔒 Enforced   |
| #74  | Silence Is Also UX                         | 🔒 Enforced   |
| #75  | Composition Is Invisible                   | 🔒 Enforced   |
| #76  | Performance Guards Never Optimize          | 🔒 Enforced   |
| #77  | Preview Is Documentation                   | 🔒 Enforced   |

### B5.4 — Library Interaction Runtime

| #    | Rule                                       | Status        |
|------|--------------------------------------------|---------------|
| #78  | Interaction Creates Intent, Never Action   | 🔒 Enforced   |
| #79  | Selection Is Independent                   | 🔒 Enforced   |
| #80  | Gesture Never Knows UI                     | 🔒 Enforced   |
| #81  | Every Interaction Is Replayable            | 🔒 Enforced   |
| #82  | Every Gesture Has Explanation              | 🔒 Enforced   |

### B5.5 — Library Host Integration

| #    | Rule                                       | Status        |
|------|--------------------------------------------|---------------|
| #83  | Coordinator Executes. Reducer Never Does.  | 🔒 Enforced   |

### Rule Freeze Policy

এখন থেকে নতুন rule numbering শুধু তখনই হবে যদি কোনো principle
সত্যিই cross-project — মানে Search, Queue, Settings, Lyrics সবাই
মানবে। Subsystem-specific discipline এখন আর নতুন rule হিসেবে registry
না, সেটা সেই subsystem-এর contract-এ documented থাকবে। এর ফলে
registry ছোট থাকবে, কিন্তু enforcement discipline বাড়বে।

---

## 5. COMPLETED SUBSYSTEMS (DETAIL)

### Foundation (M0) ✅
- **DI**: Hilt module — `PlayerModule`, `ExperienceModule`, `DatabaseModule`
- **Theme**: `Color.kt`, `Dimensions.kt`, `Motion.kt`, `RikoMotion.kt`, `RikoShapes.kt`, `GlassTokens.kt`, `Type.kt`, `Theme.kt`
- **Glass Engine V2**: `GlassSurface`, `GlassButton`, `GlassToggle`, `GlassDialog`, `GlassLoadingIndicator`, `BubbleIcon`
- **Animation**: `SpringMotion`, `RikoLiveClock`
- **Util**: `ColorExtractor`, `DeviceTilt`

### Experience Hub (M0.5) ✅
- `ExperienceInspector`, `ExperienceInspectorV2`, `ExperienceProfile`, `ExperienceProfiles`, `WaveCalibration`, `ExperiencePreset`
- Calibration framework — 8 profiles (DEFAULT, AMOLED, CINEMATIC, COMPACT, LOW_END, HIGH_END, REDUCED_MOTION, HIGH_CONTRAST)

### Color Intelligence (M1) ✅
- `ColorIntelligenceEngine`, `ColorState`, `PaletteAnalyzer`
- Album art → palette → screen saturation pipeline

### Wave Engine (M1) ✅
- `WaveEngine`, `WaveState`, `WaveFrame`, `WaveEnergy`, `WaveQuality`, `MotionSolver`, `HighlightSolver`, `EnergySolver`, `RibbonPhysicsSolver`, `OrganicPathGenerator`, `BezierControl`, `MotionState`, `RibbonState`, `RibbonPoint`, `RibbonMode`, `HighlightState`
- Render pipeline: `WaveCanvas`, `RibbonRenderer`, `GlassRibbonRenderer`, `GlowRenderer`, `HighlightRenderer`, `WaveRibbonHost`, `WaveQualityController`, `RenderPerformance`, `InspectorOverlay`, `WavePreviewScreen`
- Audio FX integration: `AudioFxManager`, `VisualizerManager`, `AudioState`, `AudioBands`, `WaveformRepository`, `PcmAudioProcessor`, `ExperienceDataHub`, `AudioFileAnalyzer`, `AudioFrame`, `AudioNormalizer`, `AudioSmoother`

### Now Playing (M1.5) ✅
- `NowPlayingScreen`, `NowPlayingLayout`, `NowPlayingBackground`, `NowPlayingEffects`, `NowPlayingPerformance`, `HeroAlbum`, `MetadataSection`, `OrganicTimeline`, `TransportControlsV2`, `PlaybackGestures`, `WaveHost`, `QueueHost`, `LyricsHost`
- Sheets: `SongInfoSheet`, `QueueSheet`, `EqualizerSheet`, `SleepTimerSheet`

### Mini Player (M1.6) ✅
- `MiniPlayerSurface`, `MiniPlayerLayout`, `MiniPlayerBackground`, `MiniPlayerAlbum`, `MiniPlayerWave`, `MiniPlayerControls`, `MiniPlayerProgress`, `MiniPlayerGestureLayer`, `MiniPlayerShadow`, `MiniPlayerActions`, `MiniPlayerEvents`, `MiniPlayerContract`, `MiniPlayerMapper`, `MiniPlayerReducer`, `MiniPlayerStateFactory`, `MiniPlayerState`, `MiniPlayerTokens`, `MiniPlayerRenderingSpec`, `MiniPlayerPerformanceGate`, `MiniPlayerPreviewState`, `MiniPlayerMetadata`, `MiniPlayerPreviewValidation`, `MiniPlayerIntegration`

### Library (M2 — Build Complete, Awaiting Validation)
- **B1 Collection Constitution** (15 files, ~6000 lines, 10/10)
- **B2 Collection Runtime** (5 files, ~2500 lines, 10/10)
- **B3 Collection Engine** (13 files, ~3500 lines, 9.998/10)
- **B4 Rendering Runtime** (14 files, ~3900 lines, 10/10)
- **B5.1 Library Screen Constitution** (15 files, ~4914 lines, 10/10)
- **B5.2 Library Screen Runtime** (8 files, ~3720 lines, 9.99/10)
- **B5.3 Library Rendering UI** (24 files, ~14453 lines, Phases A–D all 10/10 or 9.995/10)
- **B5.4 Library Interaction Runtime** (15 files, ~10937 lines, ✅ locked)
- **B5.5 Library Host Integration** (7 files, ~3901 lines, 🟡 reported)

### Data Layer (Locked)
- `MusicRepository`, `PlaylistRepository`, `SettingsRepository`
- `RikoDatabase`, `Entities`, `Daos`
- `Song` domain model

### Service Layer (Locked)
- `PlayerManager`, `MusicPlaybackService`, `HeadsetReceiver`

### Debug / Wave Laboratory (Locked)
- `WaveLaboratoryScreen`, `WaveLaboratoryViewModel`
- `HiddenDevPanel`, `InspectorToggle`, `SnapshotBar`
- Panels: `PerformancePanel`, `RenderPanel`, `PhysicsPanel`, `MotionPanel`, `AudioPanel`
- `SliderCard`, `ProfileSelector`, `RibbonViewport`, `ValueChip`

### Navigation (Locked)
- `RikoDestinations`, `RikoNavGraph`, `RikoBottomNav`
- `GlassBottomSheetContainer`

---

## 6. PENDING SUBSYSTEMS

নিচের subsystems এখনো বানানো হয়নি। প্রতিটা Rule #70 (Architecture Freeze)
মেনে নতুন contract তৈরি করবে, কিন্তু frozen pipeline follow করবে।

### M2.9 — Experience Validation (NEXT)
- 6 validation gates (Library Reality, Memory, Frame, Animation, Stress, Identity)
- কোনো নতুন feature না, শুধু audit + report + fix
- Validation tools: memory profiler, frame timer, gesture replay (already
  exists via `InteractionDiagnostics`), identity blind-test protocol

### M3 — Search Experience
- SearchContract → SearchRuntime → SearchReducer → SearchMapper →
  SearchRendering → SearchIntegration
- Reuse: Glass, Wave ambient (subtle), Color palette, CollectionCard
  pattern for results
- Search scope: songs, albums, artists, folders, playlists, lyrics
  (future), recently played

### M4 — Queue + Lyrics
- QueueContract → QueueRuntime → QueueReducer → QueueMapper →
  QueueRendering → QueueIntegration
- Lyrics: sync engine (LRC parser), scrolling lyrics, karaoke mode
- Both follow same pipeline as Library

### M5 — Audio (Equalizer, FX)
- Equalizer sheet already exists (UI shell) — needs full audio pipeline
- Bass boost, 3D surround, reverb, audio normalizer integration
- Reuse existing `AudioFxManager`, `AudioNormalizer`

### M6 — Settings
- SettingsContract → SettingsRuntime → SettingsReducer → SettingsMapper →
  SettingsRendering → SettingsIntegration
- Sections: Appearance, Audio, Playback, Library, About
- Use CollectionCard pattern for settings groups (Rule #71)

### M7 — Device Validation
- Real-device fleet testing across OEMs (Samsung, Pixel, Xiaomi, OnePlus,
  Vivo, Oppo, Realme, Motorola)
- OEM-specific audio routing, notification handling, background limits
- Foldable / tablet form factor validation

### M8 — Release Candidate
- Feature freeze, final polish
- Performance regression suite (automated)
- Store listing assets (screenshots, feature graphic, description)
- Beta program (optional)

### v1.0 — Production Release
- Public launch on Play Store
- Crash reporting, analytics (privacy-preserving)
- OTA update mechanism (Play Store managed)

---

## 7. TECHNICAL DEBT

এই section-এ থাকবে known technical debt — কোনো assumption, কোনো shortcut,
কোনো "later করব" টাইপের জিনিস। প্রতিটা debt-এর সাথে owner এবং
target milestone থাকবে।

### Current Debt (as of B5.5 reported complete)

| ID   | Description                                                              | Severity | Owner  | Target      |
|------|--------------------------------------------------------------------------|----------|--------|-------------|
| TD-1 | B5.5 host integration compile verification pending — code লেখা হয়েছে কিন্তু `./gradlew assembleDebug` এখনো হয়নি | High     | Eng    | M2.9        |
| TD-2 | Real-device test এখনো হয়নি — শুধু Compose Preview-তে verify করা হয়েছে   | High     | Eng    | M2.9 + M7   |
| TD-3 | `AllSongsScreen.kt.bak` file project-এ আছে — cleanup দরকার              | Low      | Eng    | M2.9        |
| TD-4 | Library এবং AllSongs screen এর মধ্যে overlap — AllSongs কি deprecated নাকি still active, স্পষ্ট না | Medium   | Product| M2.9        |
| TD-5 | Search screen (existing `SearchScreen.kt` + `SearchViewModel.kt`) M3-এ rebuild হবে নাকি extend হবে — decision pending | Medium | Product | M3 kick-off |
| TD-6 | Wave Engine-এর audio license / patent check হয়নি (commercial release-এর আগে দরকার) | Low       | Legal  | M8          |
| TD-7 | Lyrics engine এখনো scaffolded না — `LyricsHost.kt` shell আছে কিন্তু sync engine নাই | Medium    | Eng    | M4          |
| TD-8 | Notification channel + MediaSession integration — service আছে কিন্তু system notification UI এখনো final না | Medium | Eng    | M7          |
| TD-9 | Backup rules (`backup_rules.xml`, `data_extraction_rules.xml`) auto-generated — manual review দরকার | Low       | Eng    | M8          |
| TD-10 | Splash screen (`SplashScreen.kt`) — generic implementation, RIKO identity (logo morph, wave reveal) এখনো নেই | Medium  | Eng    | M2.9 Identity Test |

> নতুন debt চোখে পড়লে এই table-এ append করা হবে। প্রতিটা milestone
> freeze-এর সময় debt review বাধ্যতামূলক।

---

## 8. KNOWN RISKS

Risk = এমন কিছু যা ভুল হতে পারে এবং যার ফলে project timeline বা quality
impact খাবে। প্রতিটা risk-এর সাথে probability, impact, mitigation থাকবে।

| ID  | Risk                                                                       | Prob | Impact | Mitigation                                                        |
|-----|----------------------------------------------------------------------------|------|--------|-------------------------------------------------------------------|
| R-1 | B5.5 host integration compile না হলে পুরো M2 rework দরকার হতে পারে         | Med  | High   | M2.9-এ প্রথমেই `./gradlew assembleDebug` চালানো হবে              |
| R-2 | 50K+ songs এ scroll frame drop — Collection Engine ranking O(n log n) কিন্তু real data তে verify হয়নি | Med | High | M2.9 Library Reality Test-এ 50K + 100K পরীক্ষা                 |
| R-3 | Memory leak: Bitmap recycling album art-এ — ColorExtractor এখনো leak-prone | Med  | High   | M2.9 Memory Audit + LeakCanary integration                       |
| R-4 | Wave Engine frame budget 120Hz/144Hz-এ miss হতে পারে — high refresh ডিভাইসে | Med  | Med    | M2.9 Frame Timing gate, `WaveQualityController` adaptive degrade (Rule #76 মেনে — report only, manual fix) |
| R-5 | Mini Player ↔ Library morph continuity ১০০ বারে ভাঙতে পারে                 | Med  | Med    | M2.9 Animation Continuity gate (১০০ iteration automated test)    |
| R-6 | Identity Test fail — logo ঢাকলে RIKO-র পরিচয় ৫ সেকেন্ডে চেনা না যায়       | Low  | High   | M2.9 Identity Test gate, যদি fail হয় splash + hero redesign     |
| R-7 | OEM-specific audio routing (Samsung, Xiaomi) ভাঙতে পারে                    | High | Med    | M7 Device Validation, OEM test matrix                            |
| R-8 | Compose Compiler version upgrade-এ breaking change আসতে পারে              | Low  | Med    | Version pin in `libs.versions.toml`, upgrade শুধু M8-এর আগে      |
| R-9 | Backup/Restore এ user data corruption (playlist, play counts)              | Low  | High   | M8-এ backup rules review + manual test                           |
| R-10 | v1.0-এর আগে privacy review বাকি (analytics, crash reporting)               | Med  | High   | M8-এ privacy audit, opt-in consent flow                          |

---

## 9. FUTURE ROADMAP (POST-v1.0)

v1.0-এর পরের পরিকল্পনা — এগুলো এখনো speculative, শুধু direction হিসেবে
রাখা হলো:

### v1.x — Refinement
- Performance tuning (real-user telemetry থেকে data-driven)
- Bug fixes from production crash reports
- UI polish based on user feedback
- Lyrics sync engine v2 (Karaoke mode, multi-language)
- Sleep timer enhancements (fade-out, recurring)

### v2.x — Expansion
- Cloud sync (optional, opt-in) — playlist, play counts, favorites
- Podcast support (extending audio engine)
- Audiobook support (chapter navigation, bookmark)
- Cross-device playback handoff (Cast protocol)
- Companion Wear OS app
- Android Auto integration

### v3.x — Intelligence
- Personalized recommendations (on-device ML)
- Listening habits dashboard (Wrapped-style yearly report)
- Mood-based auto-playlist generation
- Smart volume leveling across album
- Audio scene detection (driving, workout, sleep)

### v4.x — Platform
- Desktop (Kotlin Multiplatform — same pipeline, different rendering)
- Web companion (player controls + library management)
- Tablet / foldable UX refinement
- Cross-platform sync protocol

> এগুলো এখন শুধু direction। প্রতিটা version-এর শুরুতে আলাদা milestone
> planning হবে। কোনোটা বাদ যেতে পারে, নতুন কিছু যোগ হতে পারে।

---

## 10. BUILD CADENCE POLICY

> Product Director directive: এখন থেকে আর ১৫টা micro build না।
> Subsystem → Review → Freeze → Next।

### Cadence Definition

প্রতিটা build cycle এই 4টা stage মানবে:

1. **Subsystem Build** — একটা complete subsystem একসাথে বানানো হবে
   (যেমন B5.4 = 15 files একসাথে, B5.5 = 7 files একসাথে)। Micro-step-এ
   ভাঙা নয়। File size 500–2500 lines পর্যন্ত স্বাভাবিক, 5000+ lines
   ও 11000+ lines ও acceptable যদি subsystem সেটা demand করে।

2. **Review** — Product Director সম্পূর্ণ subsystem review করবেন।
   Score দেবেন (e.g. 9.99/10, 10/10)। Feedback এবং verdict।

3. **Freeze** — Score satisfactory হলে subsystem freeze হবে। Locked
   layer হিসেবে registry তে যাবে। এর পরে শুধু bug fix।

4. **Next** — পরবর্তী subsystem শুরু। কোনো locked layer-কে extend
   না, নতুন contract তৈরি করা হবে।

### Why This Cadence?

- **Maintainability** — ছোট ছোট step-এ code review কঠিন হয়ে যায়,
  context switch বাড়ে। Subsystem-level review-এ পুরো জিনিস একবারে
  দেখা যায়।
- **Cohesion** — একটা subsystem-এর সব file একসাথে লেখা হলে, সেগুলোর
  মধ্যে consistency বেশি থাকে। Contract ↔ Runtime ↔ Reducer ↔ Mapper
  সব একই mental model-এ লেখা।
- **Reviewability** — Product Director পুরো subsystem দেখেন, শুধু একটা
  ফাইল না। Decision বেশি informed হয়।
- **Discipline** — Freeze একটা meaningful event, একটা tick না।

### File Size Observation (Historical)

শুরুতে files ছোট ছিল:
- Phase A: 200–300 lines প্রতিটা file
- Phase B: 400–600 lines
- Phase C: 500–700 lines

এখন subsystem-level engineering-এ চলে গেছে:
- B5.4 Interaction: 468–1228 lines per file
- B5.5 Host: 344–759 lines per file
- Single file 2500+ lines, 5000+ lines, এমনকি 11000+ lines ও acceptable
  যদি subsystem সেটা demand করে।

এটা maturity চিহ্ন — discipline এখন আর file size-এ না, subsystem
cohesion-এ।

---

## 11. CHANGELOG

Project history এক জায়গায়। শুধু milestone-level events, প্রতিটা
code change না।

| Date       | Event                                                                  | Verdict              |
|------------|------------------------------------------------------------------------|----------------------|

| 2026-10-03 | Agent session: Phase 3 ✅ + Phase 4 ✅ + Phase 5 Step 0 ✅ + Home group ✅ (red→fix→green); My Music group pushed RED (`28b630c5`) — stopped | in progress |
| (early)    | M0 Foundation LOCKED                                                  | ✅                   |
| (early)    | M0.5 Experience Hub LOCKED                                            | ✅                   |
| (early)    | M1 Color + Wave Engine LOCKED                                         | ✅ 10/10             |
| (early)    | M1.5 Now Playing LOCKED                                               | ✅                   |
| (early)    | M1.6 Mini Player LOCKED                                               | ✅                   |
| (mid)      | B1 v2 Collection Constitution LOCKED (15 files, ~6000L, 10/10)        | ✅ 10/10             |
| (mid)      | B2 Collection Runtime LOCKED (5 files, ~2500L, 10/10)                 | ✅ 10/10             |
| (mid)      | B3 Collection Engine LOCKED (13 files, ~3500L, 9.998/10)              | ✅ 9.998/10          |
| (mid)      | B4 Rendering Runtime LOCKED (14 files, ~3900L, 10/10)                 | ✅ 10/10             |
| (mid)      | B5.1 Library Screen Constitution LOCKED (15 files, ~4914L, 10/10)     | ✅ 10/10             |
| (mid)      | B5.2 Library Screen Runtime LOCKED (8 files, ~3720L, 9.99/10)         | ✅ 9.99/10           |
| (mid)      | B5.3-A Background Language LOCKED (5 files, ~2125L, 10/10)            | ✅ 10/10             |
| (mid)      | B5.3-B Identity Layer LOCKED (9 files, ~4823L, 9.995/10)              | ✅ 9.995/10          |
| (mid)      | B5.3-C Living Motion LOCKED (6 files, ~3636L, 10/10)                  | ✅ 10/10             |
| (mid)      | B5.3-D Composition LOCKED (4 files, ~3869L, 10/10)                    | ✅ 10/10             |
| (mid)      | Architecture Freeze declared (Rule #70)                                | 🔒 Frozen            |
| (recent)   | B5.4 Interaction Runtime LOCKED (15 files, ~10937L)                   | ✅ Locked            |
| (recent)   | B5.5 Host Integration reported complete (7 files, ~3901L)             | 🟡 Reported          |
| (current)  | PROJECT_STATUS.md created — single source of truth                    | 📋 Living document   |
| (pending)  | M2.9 Experience Validation — next milestone                            | ⏳ Next              |
| (current)  | ~~UI/UX REBUILD V3 (light-first)~~ → **OBSOLETE** (direction pivoted to dark-first premium) | ⛔ Obsolete |
| (current)  | **PLAN 1: Premium Design System Foundation** — 18 new files in `riko/` (6 theme + 11 components + preview lab), production untouched | 🟢 Code complete — build verify pending |
| (current)  | Preview entry: `MainActivity.DESIGN_PREVIEW_MODE = true` (temporary, revert Plan 2) | 🟡 Temporary |
| (next)     | Build verification (`assembleDebug`) + Visual QA A–E → **PALETTE LOCK** | ⏳ Next |
| (current)  | **PLAN 2: Global App Shell Migration** — RikoAppShell + ambient bg + TopBar(5 variants) + shell mini player adapter + floating nav (5 destinations preserved); VisualOsHost renderDock gate; production screens untouched | 🟢 Code complete — build verify pending |
| (next)     | Plan 2 build + on-device QA matrix → Plan 3 (Home visual migration) | ⏳ Next |
| (current)  | **PLAN 3: Home/Music Full Visual Migration** — presentation/home/ (9 files: VM+state+7 components), route preserved, real history via PlayHistoryDao (READ-only), albums/artists client-derived, hero + sections + states, All Songs list preserved at bottom | 🟢 Code complete — build verify pending |
| (next)     | Plan 3 build + visual QA → Plan 4 (next screen migration) | ⏳ Next |
| (next)     | Plan 3 build + visual QA → Plan 4 (next screen migration) | ⏳ Next |
| (current)  | **PLAN 4: My Music Library Migration** — presentation/mymusic/ (8 files: mode enum + VM + screen + 5 components), Songs/Albums/Artists/Folders modes, adaptive grids, mode-local sort, hoisted per-mode scroll, Downloads omitted (no real data source), new `my_music` route from Home "All Songs" chip | 🟢 Code complete — build verify pending |
| (current)  | **PLAN 5: Collection Detail System** — presentation/collection/ shared framework (CollectionIdentity + RikoCollectionDetail container + hero/actions/track-row/states components + Album/Artist/Folder ViewModels + 3 detail screens); PlaylistDetailScreen & FavoritesScreen adapted onto the framework (edit/add-songs logic preserved); new routes album/{id}·artist/{id}·folder/{path} wired in NavGraph; Home & My Music album/artist/folder taps navigate to detail | 🟢 Code complete — build verify pending (Plans 1–5 combined `assembleDebug` not yet run) |
| (current)  | **PLAN 6: Favorites + Playlist Experience Migration** — Playlists index migrated to Riko (header + New Playlist CTA + single-column cards w/ real coverUri or deterministic fallback); create as compact Riko bottom sheet; PlaylistDetail gains More → Rename/Delete sheets + up/down reorder in edit mode (existing repos); Favorites get real count+duration metadata, Browse Music empty CTA, subtle favorite-heart pop; all via existing repositories/routes — no new DB/storage | 🟢 Code complete — build verify pending (Plans 1–6 combined) |
| (current)  | **PLAN 7: Now Playing Experience Migration** — `presentation/nowplaying/NowPlayingScreen.kt` (932 lines): visualizer mode toggle (artwork ↔ full-bleed waveform), loading/error/no-track states, breathing album art with ambient glow, real-energy WaveformTimeline, hero play button, transport pills (shuffle/prev/next/repeat), quality strip (real bitrate/sampleRate), lyrics/eq/timer/more sheets wired, accessibility content descriptions on all icons | 🟢 Code complete — build verify pending (Plans 1–7 combined) |
| (current)  | **PLAN 8: Search + Universal Music Discovery** — `presentation/search/SearchScreen.kt` (389 lines): glass search field with focus state, RikoSongRow results, empty/no-results/loading states, keyboard handling, back navigation, now-playing highlight, Riko skeleton loading; existing SearchViewModel + MusicRepository.searchSongs() preserved (songs-only search, 280ms debounce, flatMapLatest cancellation) | 🟢 Code complete — build verify pending (Plans 1–8 combined) |
| (current)  | **PLAN 9: Settings + Utility Experience** — `presentation/settings_v2/` (6 files, ~1,479 lines): premium control room with GlassSurface cards, IconBadge icons, PillSegmentedControl option selection, PillToggle switches, SettingsRow reusable pattern; all settings categories (Appearance, Motion, Glass, Color, MiniPlayer, Accessibility, Diagnostics, Language, Profile, Developer); already complete from Milestone 7 — verified, no migration needed | 🟢 Code complete — build verify pending (Plans 1–9 combined) |
| (current)  | **PLAN 10: Final Convergence + Polish + Production QA** — Full-app audit: visual consistency, design token leaks, navigation/back-stack verification, player consistency, performance audit, accessibility audit, legacy cleanup, build verification; no new features, only hardening and coherence | 🔵 In progress |
| (next)     | Build verification (`assembleDebug`, Plans 1–9 combined) + report errors/screenshots | ⏳ Next |
| (next)     | docs/UIUX_PLAN5_COLLECTION_DETAIL.md (pending) → final Plan 5 doc | ⏳ Next |
| (current)  | **PLAN 4: My Music / Library System** — presentation/mymusic/ (8 files: 4 modes Songs/Albums/Artists/Folders, mode selector + sort sheet + cards), new `my_music` route from Home's All Songs chip, Downloads excluded (no real data), library runtime untouched | 🟢 Code complete — build verify pending |
| (next)     | Plan 4 build + visual QA → Plan 5 | ⏳ Next |

---

## 12. HOW TO USE THIS FILE

### For Product Director (Riko ভাই)
- প্রতিটা review-এর আগে এই file-এর "Current Status Overview" আর
  "Pending Subsystems" দেখে verdict দিন।
- নতুন milestone plan করার সময় "Future Roadmap" reference করুন।
- Debt বা Risk চোখে পড়লে "Technical Debt" / "Known Risks" table-এ
  append করুন।
- "LOCK" verdict শুধু তখনই দিন যখন M2.9 validation gate pass হয়।

### For Engineering (AI / future devs)
- নতুন feature শুরুর আগে "Frozen Architecture" আর "Rules Registry"
  পড়ুন। Rule #70 violate করবেন না।
- Locked layer extend করবেন না। নতুন contract তৈরি করুন।
- কোনো debt বা risk চোখে পড়লে এই file-এ append করুন — silent রাখবেন না।
- প্রতিটা PR-এ এই file-এর "Changelog" update করুন।

### For Future Self (6 মাস পরে যখন context হারিয়ে যাবে)
- এই file-টা পড়লেই পুরো project এক পলকে বোঝা যাবে।
- কোথায় আছি, কোথায় যেতে হবে, কী কী risk — সব এক জায়গায়।
- নতুন কোনো feature শুরু করার আগে এই file পড়া বাধ্যতামূলক।

---

## 13. NEXT ACTION

> ⚠️ **2026-10-03:** নিচের steps = legacy milestone view। LIVE next action দেখো উপরের `## ★ EXECUTION STATE` section — `CONTINUE-W` সেখান থেকেই resume করবে।

এখন project-এ একমাত্র priority:

**M2.9 — Experience Validation শুরু করো।**

কোনো নতুন feature না। শুধু validate।

### Step 1 (Immediate)
- `./gradlew assembleDebug` চালাও — compile হয় কিনা verify করো।
- যদি compile না হয় — B5.5 host integration-এ bug fix করো। এটাই
  TD-1 technical debt resolution।

### Step 2
- Emulator বা real device-এ app install করো।
- Library screen খুলে identity check করো — দেখো RIKO-র পরিচয় আছে কিনা।

### Step 3
- 6 validation gates একে একে চালাও। প্রতিটা gate-এর রিপোর্ট এই
  file-এর নতুন section-এ যোগ করো।

### Step 4
- সব gate pass হলে M2 official LOCK। তারপর M3 Search Experience শুরু।

---

## 14. v2.0 ROADMAP UPGRADE — PRODUCTION-GRADE

> Product Director directive (v2.0): original workflow এর সাথে একটা ৬ষ্ঠ
> layer যোগ হচ্ছে প্রতিটা milestone-এ — **Production Gate**।
> "Feature-complete" আর "production-grade" দুইটা আলাদা জিনিস।
> Spotify-level app আর আমাদের app-এর পার্থক্য এই invisible layer-এ।

### 14.1 Production Pillars (প্রতিটা milestone-এ apply হবে)

একবার define করা হলো, প্রতি milestone-এ শুধু reference হবে — repeat না।

| Pillar | মানে | Non-negotiable bar |
|--------|------|---------------------|
| 🧪 Test Runtime | Unit (Reducer/ViewModel), Integration (Engine↔Repo), UI/Instrumented, Screenshot regression | নতুন Engine/Reducer merge হবে না test ছাড়া |
| 📊 Performance Budget | Frame <16ms (60fps) / <11ms (90fps ready), cold start <800ms, recomposition logged | Layout Inspector audit প্রতি rendering phase-এ |
| 🔒 Security & Privacy | Encrypted local storage (search history, lyrics cache), no PII in logs, R8 + shrinking, network security config | Search/listening history device-এ encrypted |
| ♿ Accessibility | TalkBack labels, 48dp min touch, font-scale 200% না ভাঙা, contrast ≥4.5:1 | Accessibility Scanner pass প্রতি screen-এ |
| 📡 Observability | Structured logging, crash reporter (breadcrumbs, non-fatal), perf traces, privacy-safe analytics (hashed) | Crash-free ≥99.5% target pre-release block |
| 🛡 Resilience | Empty/error/loading states, offline/retry, SavedStateHandle process-death restore | Engine fail হলে graceful fallback, crash না |
| 🌍 i18n Ready | Strings externalized (BN + EN অন্তত), RTL না ভাঙা, locale-aware date/number | Zero hardcoded string Composable-এ |
| 🧩 Modularity | Gradle module boundary (feature vs core), single Hilt DI graph, no circular dependency | Milestone isolated in own module |

### 14.2 v2.0 Milestone Renumbering

| v1.0 | v2.0 | Title | Status |
|------|------|-------|--------|
| M2.9 | M2.9 | Experience Validation | ⏳ Next |
| M3 | M3 | Search Experience | Pending |
| M4 | M4 | Queue Experience (separate) | Pending |
| — | M5 | Lyrics Experience (separate, copyright-safe) | Pending |
| M5 | M6 | Audio Experience | Pending |
| M6 | M7 | Settings Experience | Pending |
| — | M8 | Personalization (theme engine, encrypted backup) | Pending |
| — | M9 | Background Experience (foreground service, doze, audio focus) | 🆕 New |
| — | M10 | System Integration (Android Auto, SAF, foldable) | 🆕 New |
| — | M10.5 | Beta & Staged Rollout (feature flags, crash thresholds) | 🆕 New |
| M8 | M11 | Release Engineering (was Release Candidate) | Pending |
| v1.0 | v1.0 | RIKO EXPERIENCE OS — Public Release | Pending |

### 14.3 v2.0 Total Estimate

| Milestone | Title | Files (est.) | Lines (est.) |
|-----------|-------|--------------|--------------|
| M2.9 | Experience Validation | ~10 | ~3,500 |
| M3 | Search Experience | ~28 | ~10,000 |
| M4 | Queue Experience | ~13 | ~4,500 |
| M5 | Lyrics Experience | ~12 | ~4,000 |
| M6 | Audio Experience | ~24 | ~9,000 |
| M7 | Settings Experience | ~18 | ~7,000 |
| M8 | Personalization | ~18 | ~6,500 |
| M9 | Background Experience | ~14 | ~5,500 |
| M10 | System Integration | ~16 | ~6,000 |
| M10.5 | Beta & Staged Rollout | ~10 | ~3,500 |
| M11 | Release Engineering | ~12 | ~4,000 |
| **TOTAL v2.0** | | **~175** | **~63,500** |

Existing locked codebase: ~95,000 lines। v2.0 শেষে মোট: **~158,500 lines**।

### 14.4 Per-Milestone Production Gate Checklist

প্রতিটা milestone-এর শেষে এই ৮ pillar verify হবে (code-level — কোনো run না):

- [ ] 🧪 Test: Reducer/Engine unit tests, integration tests for cross-module
- [ ] 📊 Performance: frame/recomposition/cold-start budgets declared in code
- [ ] 🔒 Security: encrypted storage paths defined, no PII log statements
- [ ] ♿ Accessibility: contentDescription, touch targets, semantics in code
- [ ] 📡 Observability: structured log events, crash breadcrumb hooks
- [ ] 🛡 Resilience: empty/error/loading fallback states in code
- [ ] 🌍 i18n: zero hardcoded strings (use string resources)
- [ ] 🧩 Modularity: clean package boundary, no circular import

### 14.5 Phase 2 (Analysis) — 8 Measurable Audit Gates

যখন ভাই analysis mode-এ যাবেন, সেই audit objective থাকবে এই ৮টা
measurable gate দিয়ে:

| Audit Item | Measurable Bar |
|------------|----------------|
| Test coverage | Core Engine/Reducer layer minimum coverage target tracked |
| Crash-free rate | ≥99.5% (from beta data) |
| ANR rate | <0.47% |
| Cold start | Baseline Profile সহ target time measured |
| APK/AAB size | Per-release delta tracked, unexplained bloat flagged |
| Accessibility scan | Android Accessibility Scanner pass প্রতি screen-এ |
| Dependency audit | `./gradlew dependencyUpdates` + vulnerability scan |
| Memory leak | LeakCanary leak-free প্রতি major navigation cycle-এ |

এই ৮টা gate না মিললে "best app" claim subjective থাকে। মিললে data-backed।

### 14.6 Execution Discipline (v2.0)

- কোনো build/run না — শুধু code, পুরো শেষ হলে ভাই analysis mode-এ যাবেন
- প্রতিটা milestone একটানা, subsystem-level, Rule #70 মেনে
- Production Gate প্রতিটা milestone-এর শেষে enforce (code-level)
- কোনো new rule numbering না (Rule freeze policy মেনে, #83 freeze)
- File size: subsystem demand অনুযায়ী, 500–5000+ lines acceptable

### 14.7 v2.0 Build Order

```
M2.9 (Validation Infra)        ✅ COMPLETE (10 files, 5933L)
  ↓
M3 (Search)                    ✅ COMPLETE (19 files, 7395L)
  ↓
M4 (Queue)                     ✅ COMPLETE (10 files, 2387L)
→ M5 (Lyrics)                  ✅ COMPLETE (9 files, 2056L)
  ↓
M6 (Audio)                     ✅ COMPLETE (6 files, 1945L)
  ↓
M7 (Settings)                  ✅ COMPLETE (5 files, 1350L)
→ M8 (Personalization)         ✅ COMPLETE (4 files, 1170L)
  ↓
M9 (Background)                ✅ COMPLETE (2 files, 636L)
→ M10 (System Integration)     ✅ COMPLETE (1 file, 470L)
  ↓
M10.5 (Beta & Staged Rollout)  ✅ COMPLETE (1 file, 528L)
  ↓
M11 (Release Engineering)      ✅ COMPLETE (1 file, 387L)
  ↓
Phase 2 — Whole-Project Analysis (8 measurable gates) ✅ INFRA READY (1 file, 528L)
  ↓
RIKO EXPERIENCE OS v1.0        ⏳ AWAITING ANALYSIS (Riko ভাই)
```

### 14.8 v2.0 Build Statistics

| Milestone | Files | Lines | Status |
|-----------|-------|-------|--------|
| M2.9 Validation | 10 | 5,933 | ✅ |
| M3 Search | 19 | 7,395 | ✅ |
| M4 Queue | 10 | 2,387 | ✅ |
| M5 Lyrics | 9 | 2,056 | ✅ |
| M6 Audio | 6 | 1,945 | ✅ |
| M7 Settings | 5 | 1,350 | ✅ |
| M8 Personalization | 4 | 1,170 | ✅ |
| M9 Background | 2 | 636 | ✅ |
| M10 System | 1 | 470 | ✅ |
| M10.5 Beta | 1 | 528 | ✅ |
| M11 Release | 1 | 387 | ✅ |
| Final Audit | 1 | 528 | ✅ |
| **v2.0 TOTAL** | **~69** | **~25,785** | ✅ |

Existing locked codebase: ~95,000 lines.
v2.0 added: ~25,785 lines.
**PROJECT TOTAL: ~120,785 lines of pure Kotlin + Compose across ~210 files.**

### 14.9 Next Step — Analysis Mode

ভাই, v2.0 build সম্পূর্ণ। এখন আপনার analysis mode-এ entry point:

1. **Build verification**: `./gradlew assembleRelease` চালান
2. **Test execution**: `./gradlew test` ও `./gradlew connectedAndroidTest`
3. **Real device test**: install করে সব screen navigate করুন
4. **Beta**: closed beta চালান ২০-৫০ users-এ ১ সপ্তাহ
5. **Audit**: `AuditRunner.run(AuditInput(...))` দিয়ে ৮ gates verify করুন
6. **Verdict**: `READY_FOR_RELEASE` হলে staged rollout শুরু করুন

Entry file: `presentation/release/audit/FinalAuditGates.kt`
Handoff checklist: `AnalysisHandoffChecklist.printFullChecklist()`

---

**END OF PROJECT_STATUS.md (v2.0 synced)**

> এই file নতুন feature-এর চেয়ে কম exciting, কিন্তু দীর্ঘমেয়াদে
> সবচেয়ে মূল্যবান জিনিসগুলোর একটি হবে। Project যত বড় হবে, এই file
> ততবার বাঁচাবে।

© RIKO Experience OS — Project Status Registry v2.0
