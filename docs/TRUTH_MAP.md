# RIKO — TRUTH MAP

> **Master-plan requirement:** §97 Phase 1 — *TRUTH + ARCHITECTURE*.
> Establish: source of truth · component map · state ownership · dependency map ·
> reference mapping · risk register · canonical design tokens.
> *"No unnecessary visual redesign yet."*
>
> **Living document.** Every entry is verified against the code, not from memory.
> Tags: `[V]` verified in source · `[A]` agreed convention · `[N]` not yet established.

---

## 0. §7 First-Task Inspection Register

Every item master plan §7 names must be inspected **before** changing code. Status:

**App Architecture** — `MainActivity` ✓ · `RikoNavGraph` ✓ · `RikoDestination` ✓ ·
`VisualOsCoordinator` ✓ · `VisualOsNavBridge` ✓ · `PlayerViewModel` ✓ · `PlayerManager` ✓ ·
`MusicPlaybackService` ✓ · notification / MediaSession path ✓

**Now Playing** — `PlayerSheetHost` ✓ · `PlayerSheetFull` ✓ · `SheetMiniBar` ✓ ·
`MiniPlayRing` ✓ · `NowPlayingBackground` ✓ · playback controls ✓ · seek / waveform ✓ ·
pager ✓ · queue entry ✓ · menu ✓

**Visualizer** — `PcmAudioProcessor` ✓ · `PcmFrames` ✓ · `SpectrumEngine` ✓ · `Fft` ✓ ·
`SpectrumBands` ✓ · `SpectrumFrame` ✓ · `SpectrumController` ✓ · `SpectrumLevelSource` ✓ ·
`AuroraSpectrum` ✓ · visualizer settings ✓ · visualizer persistence ✓

**Design System** — `RikoTypography` ✓ · `RikoMotion` ✓ · `GlassTokens` ✓ · color system ✓ ·
artwork system ✓ · buttons ✓ · sheets ✓ · dialogs ✓ · navigation ✓ · row components ✓

**Product Screens** — Home ✓ · Library / My Music ✓ · Favorites ✓ · Playlists ✓ ·
Playlist Detail ✓ · Search ✓ · Settings ✓ · Queue ✓ · Now Playing ✓ · Visualizer Lab ✓ ·
EQ ✓ · Lyrics ✓ · Info ✓ · Sleep Timer ✓

No feature is considered understood from *"component exists"*. Each is judged as a complete
user experience: visually coherent, discoverable, responsive, accessible, performant,
consistent with RIKO, intentional, state-complete, real-world robust (§3).

---

## 1. Source of Truth (§8)

Rule: *No duplicate source of truth. Canonical owners must remain clear.*

| Domain | Canonical owner | File | Notes |
|---|---|---|---|
| Playback engine | `PlayerManager` / Media3 | `service/PlayerManager.kt` | single `MutableStateFlow<PlayerState>` |
| Current track / queue | `Media3` → `PlayerState` | `service/PlayerManager.kt` | no second copy anywhere |
| Audio session / EQ | `EqualizerEngine` | `audiofx/EqualizerEngine.kt` | wired from `PlayerManager` `[V]` |
| PCM / spectrum | `PcmAudioProcessor` | `audiofx/PcmAudioProcessor.kt` | owns `spectrum`, `bands`, `amplitude`, `diagnostics` `[V]` |
| FFT / bands | `SpectrumEngine` | `audiofx/SpectrumEngine.kt` | uses `core.experience.audio.Fft` `[V]` |
| Waveform state | `WaveEngine` | `core/experience/wave/` | exposed via `PlayerViewModel.waveState` `[V]` |
| Color extraction | `ColorIntelligenceEngine` | `core/experience/ColorIntelligenceEngine.kt` | album-art → `ColorState` `[V]` |
| Settings / prefs | `SettingsRepository` | `data/repository/SettingsRepository.kt` | owns `AppSettings`, `VisualizerPrefs` `[V]` |
| Sheet anchor / expansion | `PlayerSheetState` | `presentation/sheet/PlayerSheetState.kt` | `[V]` |
| Visual OS shell | `VisualOsCoordinator` | `presentation/visualos/host/VisualOsCoordinator.kt` | owns `VisualOsState` `[V]` |
| UI aggregation | `PlayerViewModel` | `presentation/nowplaying/PlayerViewModel.kt` | **aggregator only** — never owns audio truth `[V]` |
| Design tokens | `RikoTheme` / `RikoMotion` | `core/designsystem/riko/theme/` | see §7 |

---

## 2. Component Map (§97 Phase 3 seed)

### 2.1 Track row — **4 competing families** `[V]`

| Component | File | Status |
|---|---|---|
| `RikoTrackRow` | `core/designsystem/riko/components/RikoTrackRow.kt` | **candidate canonical** (commit `6be4c3f7`) `[V]` |
| `RikoSongRow` | `core/designsystem/riko/components/RikoSongRow.kt` | duplicate `[V]` |
| `V2TrackRow` | `core/designsystem/riko/components/V2TrackRow.kt` | duplicate, V2 family `[V]` |
| `RikoCollectionTrackRow` | `presentation/collection/components/CollectionTrackRow.kt` | collection-specific `[V]` |
| `QueueRow` | `presentation/nowplaying/QueueSheet.kt:182` (private) | queue-only, acceptable `[V]` |
| `PickableSongRow` | `presentation/playlist/AddSongsSheet.kt:185` (private) | picker-only, acceptable `[V]` |

`presentation/library/**` row rendering is **LOCKED** — excluded from consolidation (`docs/ARCHITECTURE_FREEZE.md`).

### 2.2 Mini player — **3 competing top-level + 1 dormant system** `[V]`

| Component | File | Status |
|---|---|---|
| `RikoMiniPlayer` | `core/designsystem/riko/components/RikoMiniPlayer.kt` | candidate `[V]` |
| `V2MiniPlayer` | `core/designsystem/riko/components/V2MiniPlayer.kt` | duplicate, V2 family `[V]` |
| `MiniPlayer` | `presentation/components/MiniPlayer.kt` | duplicate `[V]` |
| `SheetMiniBar` | `presentation/sheet/PlayerSheetHost.kt:268` (private) | in-sheet variant `[V]` |
| `presentation/miniplayer/**` | `MiniPlayerRoot`, `MiniPlayerSurface`, `MiniPlayerWave`, … | dormant full system, `MiniPlayerTokens.kt` present `[V]` |

### 2.3 The "V2" parallel family `[V]`

A second component namespace exists alongside the canonical one:

`V2TrackRow` · `V2MiniPlayer` · `V2BottomNav` · `V2TopBar` (`V2AppBar.kt`) · `V2Cards` · `V2NpCards`

→ Each is a second source of truth for the same job. Phase 3 must pick one family per component.

### 2.4 Navigation / bars `[V]`

| Concern | Canonical | Competing |
|---|---|---|
| Bottom navigation | `RikoBottomNavigation` (`RikoBottomNavigation.kt:71`) | `V2BottomNav` (`V2BottomNav.kt:31`) |
| Top bar | `RikoTopBar` (`RikoTopBar.kt:67`) | `V2TopBar` (`V2AppBar.kt:21`), `SearchTopBar` (screen-local) |

### 2.5 Artwork `[V]`

- Canonical: **`RikoArtwork`** (`core/designsystem/riko/components/RikoArtwork.kt:49`).
- **23 source files call raw `AsyncImage` directly**, bypassing the canonical wrapper
  (`HeroAlbum.kt` ×2, `PlayerSheetFull.kt` ×2, `V2Cards.kt` ×2, `AlbumArtBackground.kt`,
  `MiniPlayerAlbum.kt`, `NowPlayingBackground.kt`, `PlaylistsScreen.kt`, `QueueSheet.kt`,
  `PlayerSheetHost.kt`, `RikoTopBar.kt`, `RikoArtistRow.kt`, `MiniPlayer.kt`, `SongListItem.kt`, …).
- Phase 3: migrate each to `RikoArtwork` (loading, placeholder, error, hero/plasma behaviour).

### 2.6 Screens present `[V]`

Home · My Music · Favorites · Playlists · Playlist Detail · Search · Settings · Queue ·
Now Playing · Visualizer Lab · Equalizer (`EqualizerSheet.kt`) · Lyrics · Info
(`SongInfoSheet.kt`) · Sleep Timer (`SleepTimerSheet.kt`).

---

## 3. State Ownership `[V]`

| State | Owner | Flow |
|---|---|---|
| `PlayerState` (track, position, queue, playing) | `PlayerManager` | `playerState: StateFlow<PlayerState>` |
| `SpectrumFrame` | `PcmAudioProcessor` | `spectrum` |
| `AudioBands` | `PcmAudioProcessor` | `bands` |
| amplitude / `PcmDiagnostics` | `PcmAudioProcessor` | `amplitude`, `diagnostics` |
| `WaveState` / `WaveDiagnostics` | `WaveEngine` | via `PlayerViewModel` |
| `AudioState` | `ExperienceDataHub` | via `PlayerViewModel.audioState` |
| `ColorState` | `ColorIntelligenceEngine` | via `PlayerViewModel.colorState` |
| `VisualizerPrefs` / `AppSettings` | `SettingsRepository` | `visualizerPrefs` |
| `VisualOsState` | `VisualOsCoordinator` | `state` |
| Sheet expansion | `PlayerSheetState` | — |

**Rule:** `PlayerViewModel` only *re-exposes* flows. It must never become a second owner.

---

## 4. Dependency Map `[V]`

Import direction counted across `app/src/main/java` (arrows = "imports"):

```text
presentation ──> core (465)  domain (58)  data (45)  audiofx (11)  service (10)
di          ──> presentation (60)  core (9)  data (7)  audiofx (4)
core        ──> audiofx (9)  domain (1)
service     ──> domain (5)  audiofx (2)
data        ──> domain (3)
audiofx     ──> domain (6)  data (5)  service (1)  core (1)
domain      ──> (leaf)
```

### Two cycles found `[V]`

**Cycle A — `core ↔ audiofx`**
- `core/experience/wave/{WaveEngine,WaveFrame,EnergySolver}.kt` → `audiofx.AudioState`
- `core/experience/{ColorIntelligenceEngine,ExperienceInspector,ExperienceInspectorV2}.kt` → `audiofx.{AudioState,ExperienceDataHub}`
- `audiofx/SpectrumEngine.kt:5` → `core.experience.audio.Fft`

**Cycle B — `service ↔ audiofx`**
- `service/PlayerManager.kt:10-11` → `audiofx.{EqualizerEngine,VisualizerManager}`
- `audiofx/ExperienceDataHub.kt:5` → `service.PlayerManager`

These are Phase 1 findings, **not changed here** (§97 Phase 1 = establish, not redesign).

---

## 5. Reference Mapping `[V]`

### 5.1 Reference images → screens (written-number specs)

| Reference | Target screen / component |
|---|---|
| `01_home.png` … `04_now_playing.png` | Home / Library / Queue / Now Playing |
| `05_settings_home.png` | Settings landing |
| `06_settings_color.png` | Color & theme settings |
| `07_settings_wave.png` | Waveform settings |
| `08_settings_mini_player.png` | Mini player settings |
| `09_settings_visual.png` | Visualizer settings |
| `10_settings_colour_more.png` | Advanced color |
| `11_settings_colour_more_2.png` | Advanced color (2) |
| `12_settings_scrolling.png` | Scroll / marquee settings |
| `13_settings_sound_effects.png` | Sound effects / EQ |
| `14_settings_sky.png` | Background atmosphere |
| `15_settings_visual_effects.png` | Visual effects |
| `16_full_lyrics.png` | Lyrics |
| `17_visualizer.png` | Visualizer |
| `Plasma Waveform _ Album & Artist Page — One UI 7-8.jpg` | Waveform anatomy: baseline · ridges · ridge order · window 0.07/0.28/0.85 · aspect 2.38:1 `[M]` |

> Geometry not measurable from the reference **by an agent without image input** is tagged
> `[A]` and must live as a named token, never as a magic number.

### 5.2 Reference source repositories → future phases `[V]`

Location: `extra rare files to use/` — **248.1 MB · 34,060 files · untracked · not referenced by any app source.**

| Folder | Contents | Used in |
|---|---|---|
| `equalizer_ref/` | decompiled equalizer app (`java/`, `kotlin/`, `res/`, `AndroidManifest.xml`) | **EQ phase — to be rebuilt to plan** `[A]` |
| `visualization templates and sources/` | `android-visualizer-master`, `Audio-Visualizer-main`, `audio-visualizer-master`, `RealTimeSpectrogramFFT-master` | Visualizer phase |
| `JTransforms-master`, `JTransforms-3.2-sources` | FFT library sources | Spectrum phase |
| `projectm-master` | ProjectM (MilkDrop-style) visualizer | Visualizer phase |

**Rule:** reference gives direction; RIKO gives identity. No copyrighted assets, logos or
source-app icons may be copied into the product (§4).

---

## 6. Risk Register `[V]`

| # | Risk | Sev | Evidence | Phase |
|---|---|---|---|---|
| R1 | Two component families (`Riko*` vs `V2*`) → divergent look/behaviour | High | §2.3 | 3 |
| R2 | 4 track-row implementations | High | §2.1 | 3 |
| R3 | 3 mini-player implementations + dormant `presentation/miniplayer/**` | High | §2.2 | 3 |
| R4 | 23 files bypass `RikoArtwork` with raw `AsyncImage` | Med | §2.5 | 3 |
| R5 | `core ↔ audiofx` dependency cycle | Med | §4 Cycle A | 6 |
| R6 | `service ↔ audiofx` dependency cycle | Med | §4 Cycle B | 6 |
| R7 | Dead FFT tier config: `SpectrumConfig.fftSizeFor` never called; `PcmAudioProcessor.kt` hardcodes `1024` | Med | verified | 6 |
| R8 | Test/production resample mismatch | Med | verified | 7 |
| R9 | Repeat control has no live button | Med | verified | 5 |
| R10 | `presentation/library/**` locked (121 files / ~52.8k lines) — cannot be restructured | Low | `docs/ARCHITECTURE_FREEZE.md` | — |
| R11 | No Android SDK on this machine — CI is the only build gate | High | `local.properties` points to Linux path | — |
| R12 | Reference material (248 MB) untracked and could be lost | Low | §5.2 | — |

---

## 7. Canonical Design Tokens `[V]`

Existing token surfaces (Phase 2 consolidates; Phase 1 only *establishes*):

| Area | Token file | Role |
|---|---|---|
| Motion (easing/durations) | `core/designsystem/riko/theme/RikoMotion.kt` | **canonical motion** |
| Theme shell | `core/designsystem/riko/theme/RikoTheme.kt` + `RikoThemeV2.kt` | two theme entry points → resolve in Phase 2 |
| Typography | `core/designsystem/riko/theme/RikoTypography.kt` | canonical type scale |
| Glass / scrim | `core/designsystem/theme/GlassTokens.kt` | canonical glass |
| Legacy theme | `core/designsystem/theme/Theme.kt` | legacy path |
| Plasma waveform | `presentation/nowplaying/PlasmaTokens.kt` | waveform geometry tokens |
| Album color | `presentation/nowplaying/color/AlbumPalette.kt` | ridge colors `#2B59E2` `#771FFD` `#F942E8` |
| Mini player | `presentation/miniplayer/MiniPlayerTokens.kt` | mini-player tokens |
| Visual OS | `presentation/visualos/contract/VisualOsTokens.kt` | shell tokens |
| Wave motion | `core/experience/wave/{MotionSolver,MotionState}.kt` | wave-specific motion |
| Library | `presentation/library/**` tokens | **LOCKED** — excluded |

`RikoMotion.kt` previously nested a dead `object Easing` that shadowed the imported
`androidx.compose.animation.core.Easing` and broke compilation (Phase 0, commit `15df62bb`).

---

## 8. Phase 1 Cleanup Performed `[V]`

- Deleted 12 zero-byte `*.kt System.Object[]` junk files — every one proved to have a clean
  sibling (or, for `NowPlayingScreen`, **no declaration and no reference anywhere**).
- Untracked `Project RIKO.zip` (7.6 MB); added `*.zip` / `*.rar` / `*.7z` to `.gitignore`.
- Ignored `extra rare files to use/` (248 MB reference material, never to be committed).
- Re-encoded `PROJECT_STATUS.md` to valid UTF-8 (validated: 0 replacement chars, 826 lines,
  3,091 Bengali chars, 98 emoji, original em-dashes and `↔` preserved).
- `docs/ARCHITECTURE_FREEZE.md` checked — **already valid UTF-8, untouched**.
