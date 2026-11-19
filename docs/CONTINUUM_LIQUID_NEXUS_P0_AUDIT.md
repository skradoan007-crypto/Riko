# RIKO CONTINUUM + LIQUID NEXUS — P0 Architecture Audit & Integration Plan

## 1. Existing Architecture Verified
- **Package & SDK**: `com.rikodev.music.android`, `minSdk = 26`, `compileSdk = 35`, `targetSdk = 35`, Kotlin + Jetpack Compose + Material 3 + Media3/ExoPlayer + Room + Hilt + DataStore + Coil.
- **Frozen Boundary (Untouched)**: `presentation/library/**` (121 files, D-7) — zero modifications.
- **Playback Single Source of Truth**: `PlayerManager` / `MusicPlaybackService` / `PlayerViewModel` (`currentSong`, `isPlaying`, `playbackPosition`, `duration`, `queue`, `shuffleMode`, `repeatMode`, `isCurrentSongFavorite`, `audioState`, `waveState`, `colorState`, `dynamicColors`).
- **MDNA / Audio Analysis**: `ExperienceDataHub` → `AudioState` (`energy`, `bassEnergy`, `midEnergy`, `trebleEnergy`, `transient`, `beatConfidence`, `silenceLevel`) + `MdnaSyncHub` (`liveSpectrumAvailable`, `offlineTimelineAvailable`).
- **Navigation & Shell**: `RikoNavGraph.kt` + `RikoAppShell.kt` + `PlayerSheetHost.kt` (Mini ↔ Full Now Playing morph sheet).
- **Settings Persistence**: `SettingsRepository` (`riko_settings` DataStore) + `settings_v2` (`SettingsContract`, `SettingsReducer`, `SettingsHostCoordinator`, `SettingsRendering`).

## 2. Phased Integration Plan (P0 → P10)
- **P0**: Architecture audit, git baseline, and non-breaking integration contract.
- **P1**: `ContinuumSection` (`Listen`, `Explore`, `Library`), `ContinuumNavHistory` (per-section context & Timefold stack), and `ContinuumLibraryHubScreen` (unifying Playlists + Favorites + Saved Collections).
- **P2**: `ContinuumWaveNavBar` — organic cubic-Bézier wave navigation surface with interruptible spring horizontal wave travel, custom geometric Headphones / Orbital Planet / Books icons, and Ultra/Balanced/Lite rendering.
- **P3**: `LiquidDockGeometry` + Compact Living Music Dock floating above the right side of the navbar with organic breathing contour, single Play/Pause inside artwork, and dedicated Expand button.
- **P4**: `LiquidNexusStateMachine` (`Compact`, `Expanding`, `Expanded`, `Scrubbing`, `ModeSelection`, `Collapsing`, `FullPlayer`) + coordinated right-to-center artwork morph and horizontal liquid chamber expansion.
- **P5**: Expanded Liquid Nexus layout — `(Name + Artist) | (Album Art + Play/Pause) | (Controls + Smart Mode Cluster)` + lower integrated Liquid Timeline + contextual secondary menu.
- **P6**: Scrubbing state — background content blur (API 31+ `RenderEffect` / Lite dim scrim), sharp CONTINUUM navbar & Liquid Nexus, elastic liquid time bubble (`m:ss / m:ss`), commit-on-release seeking without auto pause/play.
- **P7**: `VisualQualityEngine` (`VisualQualityTier.ULTRA` default, `BALANCED`, `LITE`) persisted in `SettingsRepository` DataStore + `Visual Experience` section in `settings_v2` + device capability separation (never overwrites user preference).
- **P8**: Real MDNA / `AudioState` contour reactivity bridge + smooth Full Now Playing (`PlayerSheetHost`) handoff & reverse collapse.
- **P9**: Smart CONTINUUM per-section context preservation, Timefold navigation history, and section-aware Contextual Action Pocket (`Listen`, `Explore`, `Library`).
- **P10**: Unit tests (`LiquidNexusStateMachineTest`, `VisualQualityEngineTest`, `ContinuumNavHistoryTest`), performance pass (`drawWithCache`, `graphicsLayer`, lifecycle pause), and final verification.
