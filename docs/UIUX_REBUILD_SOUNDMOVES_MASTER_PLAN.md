# RIKO × SOUNDMOVES — UI REBUILD MASTER PLAN

> **Status:** 🟢 ACTIVE — Product Director approved (SoundMoves spec direction)
> **Supersedes:** `UIUX_REBUILD_V3.md` (obsolete light plan) · absorbs Plan 11 remaining work
> **Source of truth:** `docs/Plans/SoundMoves_UI_Enterprise_Workflow_v2.txt` (SM-UI-SPEC-001 v2.0)
> + companion deck (19 slides). If this plan and the spec ever disagree, **the spec wins**.
> **Scope:** Rendering/Compose layer only. Frozen layers (Rule #70): Engine / Reducer /
> Runtime / Mapper / audiofx / service / data — UNTOUCHED. "হাড় থাকবে, চামড়া নতুন।"

---

## 0. OPERATING MODEL (যেভাবে আমরা কাজ করবো)

### 0.1 Build loop (PAT workflow)
```
AI codes phase → local `gradlew assembleDebug` verify → commit → PUSH (PAT)
→ GitHub Actions builds debug APK (existing CI: build-debug-apk.yml, artifact 30d)
→ Product Director downloads APK from Actions artifacts → install → run
→ eyeball vs REF-1/2/3 reference images → PASS / fix-list → next phase
```
PAT handling rules: fine-grained PAT, **Contents: Read+Write only**, stored in git
credential store / env — NEVER committed, never echoed in logs/commands.

### 0.2 Visual QA protocol (honest limitation)
এই AI model **image দেখতে পারে না** (no image input)। তাই visual QA দুই স্তরে:
- **AI-side (code-level):** token governance greps (0 hardcoded colors/sizes in live
  code), spec Section 8 checklist mapped to code, component geometry review,
  build green in CI.
- **Director-side (visual):** তুমি reference mockup (REF-1/2/3, `images/UI target/`)
  এর পাশে app রেখে মিলিয়ে দেখবে — per-screen PASS/FAIL + fix-list দেবে।
  Pixel-diff ≤1% target per spec C4; real-data vs sample-string differences fall
  in the WARN band (1–2.5%) with QA note — spec-anticipated.
- পরে চাইলে Paparazzi numeric pixel-diff CI gate যোগ করা যাবে (optional, SM-8)।

### 0.3 Cadence (PROJECT_STATUS §10 policy)
Phase = subsystem-sized build → `assembleDebug` verify → PUSH → Director review →
freeze → next। কোনো ১৫-খানা micro-build না। প্রতি phase-এর শেষে এই file আর
`PROJECT_STATUS.md` changelog আপডেট হবে।

### 0.4 Skill gates
- Phase-end: `awesome-code-review` (diff review before push)
- Legacy retirement: `awesome-code-cleanup` (dead code sweep)
- Bug fixing: `systematic-debugging`; testable logic: `test-driven-development`
- Planning/risk: `thinking-model-router` + `thinking-pre-mortem` at SM-3 kickoff

---

## 1. CURRENT STATE (audit summary — 2026-09-08)

**Done:** SM Phase 0 (tokens: canvas gradient, aurora, white glass, type scale) ও
Phase 1 partial (aurora buttons/chips, 56dp primary, toggle-aurora) — commits
`b6b4d9d`, `84ebf24` + fixes। riko components are token-clean (0 hardcodes)।
Shell + list screens (Home/MyMusic/Playlists/Favorites/collection) are on riko DS।

**Top gaps (from full audit):**

| # | Gap | Spec ref |
|---|-----|----------|
| 1 | 5 tabs vs spec 4 tabs; index-0 mismatch; "Favorites" US spelling | 2.6, Sec 4 |
| 2 | Onboarding screen nonexistent | 3.1 |
| 3 | 14 of 22 components missing (dual pill, TrackListRow enum, ArtistRow, MoodGridCard, PromoBannerCard, SettingsRowCard, WaveformVisualizer, TransportControlBar, SortDropdownChip, ToggleSwitch, OnboardingLegalText…) | Sec 2 |
| 4 | Now Playing: old DS, square hero (not 280dp circle), 2-row transport violating D5, plasma waveform (not 46 bars), debug HUD forced on | 3.7, D5 |
| 5 | Settings: fully old DS, no PromoBannerCard / SettingsRowCard pattern | 3.9 |
| 6 | Bottom nav: 72dp (spec 64), flat-violet pill (spec 52dp aurora circle), double padding | 2.6 |
| 7 | Mini-player: no progress hairline | D13 |
| 8 | Baloo 2 not wired (FontFamily.Default; no res/font, no dependency) | 1.3, D1 |
| 9 | RikoTopBar exists but 0 screens use it — headers hand-rolled | 2.2–2.5 |
| 10 | No Moods grid anywhere | 3.3, D6 |
| 11 | VisualOsHost overlay still renders above riko shell (dual-stack) | — |
| 12 | Repo strays tracked in git (Project RIKO.zip 1.99MB, _p7_*.txt), broken 0-byte artifact file, 13 uncommitted screenshot deletions | hygiene |
| 13 | ~150 dead files shipping (old library/, miniplayer/, allsongs/ — 0 call sites) | cleanup |
| 14 | 193 hardcoded Color + 114 inline fontSize in presentation (mostly dead/dev files, but Settings/NowPlaying live files affected) | tokens |

---

## 2. PHASE R0 — HYGIENE & BASELINE (pre-work, ~half day)

Goal: clean repo + green baseline build + PAT loop live। কোনো visual change নেই।

1. Delete `presentation/navigation/RikoNavGraph.kt System.Object[]` (0-byte PS artifact)
2. `git rm` tracked strays: `_p7_nav.txt`, `_p7_np_screen.txt`, `_p7_np_screen2.txt`,
   `_p7_vm.txt`, `Project RIKO.zip` · delete on-disk `gitfix.log`, `p7_*.log`
3. `.gitignore` += `_p7_*.txt`, `*.zip` (root), `*.log` (already there)
4. Commit pending: 13 screenshot deletions + 6 new screenshots (reorganize into
   `images/builds/` optional)
5. Baseline: `./gradlew assembleDebug` — MUST pass before any UI work
6. PAT wiring: remote credential set; test push+CI run
7. Sync `PROJECT_STATUS.md` changelog row

**Exit gate:** clean `git status`, green CI, PAT push verified।

---

## 3. PHASE SM-0 — TOKEN COMPLETION (finish spec Section 1)

Phase 0 commit করা আছে, কিন্তু spec 1.3/1.7-এর কিছু অংশ বাকি:

1. **Baloo 2 font** (D1): bundle OFL TTFs (400/500/600/700) in `res/font/`
   (offline-first — no INTERNET permission, no downloadable-fonts certs needed)
   → `RikoTypography.RikoFontFamily` swap (one place, line 22)। Fredoka →
   Poppins fallback chain comment রাখবো।
2. **Size tokens (spec 1.7):** `RikoDimensions`-এ যাচাই/সংযোজন —
   BottomNavHeight 72→**64dp**, BottomNavActiveIndicator **52dp** (exists, unused),
   row height 76dp, hero 280dp, transport center 64dp, waveform 48dp, kebab 44dp hit-slop
3. **Radius audit:** SortDropdownChip = 8dp (radius-xs) exception ছাড়া সব pill full।
4. Motion tokens (5.1) verify in RikoMotion: 100/200/300/400ms + spring-snappy(300/26)

**Exit gate (spec A2):** Baloo renders on device; zero raw hex/dp outside token
files in riko package; size tokens all named।

---

## 4. PHASE SM-1 — ATOMIC COMPONENTS (finish spec 2.6–2.14 → 2.23)

| Component | Status | Work |
|---|---|---|
| IconButton (2.10) | exists | verify 44dp + states + preview |
| PrimaryButton (2.7) | exists | verify pressed darken 8% overlay |
| SecondaryButton (2.8) | exists | verify states |
| **ToggleSwitch (2.16)** | missing | NEW `RikoToggleSwitch.kt` — 44×24, aurora ON, 300ms thumb spring |
| **SortDropdownChip (2.14)** | missing | NEW — 28–32dp, **radius-xs 8dp** (the one exception), chevron-down |
| **FilterTabPill (2.13)** | duplicated | extract from HomeCategoryChips + LibraryModeSelector → shared `RikoFilterTabPill.kt` |
| OnboardingLegalText (2.23) | missing | NEW — inline tappable links (Terms/Privacy) |

Every atomic gets `@Preview` with all states (Phase 1 exit gate)।

---

## 5. PHASE SM-2 — COMPOSITE COMPONENTS (14 builds, spec 2.9–2.22)

| Component | File | Spec notes |
|---|---|---|
| **TrackListRow** (2.11) | refactor `RikoSongRow` → `RikoTrackListRow` | add `trailing` enum: PLAY_ONLY / MENU_ONLY / PLAY_AND_MENU; 56×56 r12 thumb, 76dp row |
| **SegmentedDualActionPill** (2.9) | NEW | outer glass 56dp pill; left aurora segment ~52% w/ icons; D3 icons always ON |
| **ArtistRow** (2.22) | NEW | 56×56 r16 photo, followers caption, Follow secondary button |
| **MoodGridCard** (2.19) | NEW | 2-col ~169×180 r20, photo + bottom scrim, Gold Record badge exception |
| **PromoBannerCard** (2.17) | NEW (absorb HomeFeaturedBanner) | 168–180dp r20, blue scrim #1B4FBF left 65%, compact primary button |
| **SettingsRowCard** (2.15) | NEW | 72dp r16–20, 40dp icon badge, chevron OR toggle trailing |
| **CircularHeroAlbumArt** (2.18) | adopt `RikoArtwork(Hero)` | 280dp circle, ≤72% width |
| **WaveformVisualizer** (2.20) | NEW `RikoWaveformVisualizer.kt` | ~46 discrete bars, 2dp w/3dp gap, played white 90% / unplayed 45%, scrub = slider, optional live bounce near playhead |
| **TransportControlBar** (2.21) | NEW `RikoTransportControlBar.kt` | **D5 exact order: Shuffle · SkipNext · PlayPause(64dp glass circle) · SkipPrevious · Repeat** — do NOT "fix" |
| **BottomNavBar fix** (2.6) | modify `RikoBottomNavigation` | 64dp, 52dp aurora active circle (spring-slide, not opacity swap), fix double padding (shell 16dp + inner 24dp → one 20dp margin), 4 items |
| **AppBar A/B/C/D** (2.2–2.5) | refit `RikoTopBar` → 4 variants | A title+search+heart · B back+centered title+heart · C avatar+search+heart · D title+bell; adopt in ALL screens (currently 0 users) |
| **MiniPlayer hairline** (D13) | modify `RikoMiniPlayer` | thin progress line top edge, aurora fill, from playerState.progress |
| SectionHeader (2.12) | exists | verify |
| StatusBar (2.1) | activity/theme | force light-content white icons app-wide |

**Exit gate (spec A2):** all 22 Section-2 components exist with previews, built
only from tokens + atomics; Director deck-slide-7 cross-check।

---

## 6. PHASE SM-3 — SCREENS × 9 + NAVIGATION (spec Sec 3 + Sec 4)

স্ক্রিনগুলো real data-র সাথে বানানো হবে (data layer already frozen ও live) —
greenfield "static mock assembly" step skip; C4 WARN band covers real-data diff।

| Screen | Work |
|---|---|
| **Onboarding (3.1)** NEW | `presentation/onboarding/` + route + first-run gate (post-splash); full-bleed hero photo → gradient dissolve; Get Started primary + Continue-with-Google (D2: real "G" logo) + legal text। Asset pending → placeholder artwork collage তত্ত্বাবধানে। |
| **Home (3.2+3.3, D6)** | AppBar C · FilterTabPills · For You carousel (large PromoBannerCard + Play Now) · Recently Played (TrackListRow PLAY_ONLY) · **Moods 2-col grid scroll-continuation (NEW)** · Artists (ArtistRow)। Mood data: derived (see D-NEW-2)। |
| **Favourites (3.4)** | AppBar A · SegmentedDualActionPill · "N favourites" + SortDropdownChip row · TrackListRow PLAY_ONLY |
| **Playlist (3.5, D12)** | demote tab → sub-destination under Library · AppBar A · count + sort row · Add New Playlist capsule (52dp aurora + circle) · TrackListRow MENU_ONLY · subtitle = curator (D8) |
| **My Music (3.6)** | AppBar B (Library root tab) · FilterTabPills (see D-NEW-1) · Recently Played · Artists w/ Follow |
| **Now Playing (3.7)** | FULL rebuild on riko DS: AppBar B · 280dp circular hero (RikoArtwork.Hero) · title + artist (D10) · WaveformVisualizer + time row · TransportControlBar (D5) · ambient glow tinted from artwork (recommended in spec) · **debug HUD off** · PlayerViewModel untouched (frozen) |
| **My Favourite Songs (3.8)** | align existing `RikoCollectionDetail` to spec: titleless AppBar B · 200–220dp hero · SegmentedDualActionPill · PLAY_AND_MENU rows |
| **Setting (3.9)** | rebuild on riko: AppBar D · PromoBannerCard (see D-NEW-3) · SettingsRowCard list · Dark Mode toggle ON (D11 Exit row coral `RikoError`) · সব real settings SettingsRowCard pattern-এ (see D-NEW-4) |
| **Search** | keep current VM/behavior; restyle field + rows on riko tokens (spec silent on search — pre-existing) |

**Navigation restructure (spec Sec 4):**
- **4 tabs: Home · Favourites · Library(My Music) · Settings** — "Favourites" spelling
- Playlists + Now Playing = pushed destinations (no tab); Visualizer tab removed
- Start destination = Home; Search reachable from headers (as now)
- Transitions (5.2): tab crossfade 200ms; push slide-in-right 400ms + prev dim 70%
- Shared-element row-thumbnail → hero: stretch goal via nav-compose 2.8
  SharedTransitionScope; fallback = fade+scale. Highest-impact motion (spec priority 1)
- **Visual OS off-ramp:** remove `VisualOsHost` render from RikoNavGraph
  (overlay z-100 conflict) — files stay dormant for M2.9 evaluation
- Clean `RikoDestinations.bottomNavItems` dead list; register or delete
  PlaylistEditor/AddToPlaylist routes

**Change control (spec A4/A6) — Director decisions needed (D-NEW):**
- **D-NEW-1** My Music filter labels: spec "Suggested/Folders/Downloads/New" vs our
  real modes "Songs/Albums/Artists/Folders" → **recommend: keep real modes,
  spec FilterTabPill styling** (Downloads has no data source — TD-4 already flagged)
- **D-NEW-2** Mood grid data: no mood-entity exists → **recommend: fixed mood
  presets (Chill, Energy, Heartbeat, Gold…) mapped to genre/keyword queries over
  real library; cover = palette-derived gradient from member songs**
- **D-NEW-3** Promo banner: no premium product exists → **recommend: render spec
  banner, tap → "Coming soon" toast** (visual fidelity, no fake upsell)
- **D-NEW-4** Settings scope: spec shows 6 rows; our control room has ~10 real
  sections → **recommend: keep ALL real functionality, restyled as
  SettingsRowCard groups (spec pattern, Rule #71)** — deleting working features = regression
- **D-NEW-5** Onboarding hero asset → **recommend: temporary artwork-mosaic w/ blur +
  re-asset later** (spec asset checklist has it as commissioned work)

**Exit gate:** all 9 screens on riko DS, nav per spec, build green, Director
visual round per screen।

---

## 7. PHASE SM-4 — MOTION POLISH (spec Sec 5, order per spec)

5.2 transitions (in SM-3) → 5.3 nav circle spring-slide + icon pulse →
5.4 press scale 0.96 + 8% darken → 5.5 heart spring 1.0→1.3→1.0 (+particles nice-to-have) →
5.6 toggle slide 300ms → 5.7 pill morph + autoscroll → 5.8 waveform progress 1:1 +
live bounce → 5.9 list entrance stagger (30–40ms, ≤6 items, degrade gracefully) →
5.10 skeleton shimmer 1200ms।

60fps target; frame drops on low-end = skip stagger keep fade (spec C6)।

---

## 8. PHASE SM-5 — PLATFORM POLISH + A11Y (spec Phase 7 + C5)

- Status bar light-content app-wide ✓ (from SM-2, re-verify)
- **44dp min hit-slop** — kebab 18dp visible but 44dp touch target
- Content descriptions: every icon-only button (state-dependent favourite labels)
- Contrast: tertiary 38% white = large/bold/decorative ONLY (C5 finding #2);
  Button-Label never below 14sp
- Font-scale 130%: 76dp rows fit 2 lines, no clip
- D14 states: empty/loading/offline/error on every screen — skeleton shimmer,
  glass empty-states from Section 1 tokens
- RTL: pin D5 transport order (spec C7)

---

## 9. PHASE SM-6 — QA & SIGN-OFF (spec Sec 8 + C4/C9)

1. Token governance grep = **0** live-code violations (193→0 in active files;
   dev/debug panels exempt-but-flagged)
2. D1–D14 + D-NEW all verified implemented (no silent reversion)
3. Director visual pass: 9 screens vs REF-1/2/3 → PASS or fix-loop
4. Screenshot set committed to `images/builds/<date>/`
5. Optional: Paparazzi baselines + CI numeric diff (≤1% PASS / ≤2.5% WARN)
6. **Acceptance certificate (spec C9):** filled + signed (Director = PO/DL/AX)

---

## 10. PHASE SM-7 — LEGACY RETIREMENT (post-sign-off, behavior-preserving)

1. Delete verified-dead packages (0 call sites confirmed at deletion time):
   `presentation/library/` (~100 files), `presentation/miniplayer/` (22),
   `presentation/allsongs/`, `components/MiniPlayer.kt`
   — WaveLaboratory/debug refs আগে check করে
2. Old DS imports retire from live screens (nowplaying sheets, splash,
   search remnants, NavGraph) — old `theme/glass/components` packages তারপর delete
3. `RikoMusicTheme` → `RikoTheme` swap in MainActivity (final unwrap)
4. TD-3/TD-4 close; PROJECT_STATUS.md full sync

---

## 11. RISK REGISTER (update per phase)

| ID | Risk | Prob | Impact | Mitigation |
|---|---|---|---|---|
| SR-1 | Shared-element transition API instability (nav 2.8 experimental) | Med | Med | fallback fade+scale; ship motion polish separately |
| SR-2 | Mood grid empty on small libraries | Med | Low | empty-state D14 + min-card-count guard |
| SR-3 | Baloo 2 metrics shift layouts vs Default | Med | Low | preview + device pass; Fredoka fallback ready |
| SR-4 | Nav restructure breaks hiltViewModel wiring | Low | High | keep route IDs stable; build verify per screen |
| SR-5 | Settings restyle hides real toggles | Low | Med | D-NEW-4 review at build; keep all rows functional |
| SR-6 | Real-data screens never hit ≤1% vs mockup | Certain | Low | C4 WARN band + Director sign-off note (spec-sanctioned) |
| SR-7 | Visual OS removal regresses back-press/gesture chain | Low | Med | back-press chain audit in SM-3; VisualOsBackPressChain retained until verified |

---

## 12. COMMIT PLAN (phase = one subsystem push)

```
R0    chore: repo hygiene + baseline build + PAT loop
SM-0  Phase SM-0: Baloo 2 font bundle + size/radius/motion tokens complete
SM-1  Phase SM-1: atomics complete (ToggleSwitch, SortDropdownChip, FilterTabPill, legal text)
SM-2  Phase SM-2: 14 composite components per spec Section 2 (+nav/miniplayer fixes)
SM-3  Phase SM-3: 9 screens + 4-tab nav + Visual OS off-ramp (per-screen sub-commits allowed)
SM-4  Phase SM-4: motion polish per spec Section 5
SM-5  Phase SM-5: platform polish + accessibility (C5)
SM-6  Phase SM-6: QA fixes + acceptance certificate
SM-7  chore(SM-7): legacy design system retirement + dead package removal
```

Each push → CI assembleDebug → Director APK review → freeze → next।

---

## 13. NEXT ACTION

1. Director answers D-NEW-1…5 (Section 6) — quick calls, recommendations ready
2. Director provides PAT (Contents RW scope)
3. Execute **R0** → baseline green → **SM-0** begins

---

**END OF MASTER PLAN — build against the spec, exactly as written.**
