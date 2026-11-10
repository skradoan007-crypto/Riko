package com.rikodev.music.android.presentation.library

import com.rikodev.music.android.core.experience.ColorState
import com.rikodev.music.android.core.experience.wave.WaveState
import com.rikodev.music.android.domain.model.Song

// ═══════════════════════════════════════════════════════════════════════════
// RIKO MUSIC — MILESTONE 2: LIBRARY RUNTIME — EXPERIENCE BRIDGE — MAPPER
//
// Phase A, File 6: LibraryMapper.kt
//
// Pure mapping functions: Repository / Experience OS → Library Snapshots.
// NO UI. NO Compose. NO side effects. NO business logic.
// NO mutation of LibraryState beyond what Mapper owns.
//
// Architecture:
//
//   Song (domain model) ──────┐
//   ColorState (Experience OS)─┤
//   WaveState (Experience OS) ─┼──→ LibraryMapper ──→ LibraryState
//                              │        (pure)
//   Repository (Room) ─────────┘
//
// ⭐ RULE #31: Runtime Owns Truth ⭐
//   Repository → Mapper → Reducer → State → UI.
//   No shortcut. Every path goes through Mapper.
//
// ⭐ RULE #33: Cards Never Fetch ⭐
//   Mapper creates snapshots. Cards read snapshots.
//   Cards NEVER touch Repository, DAO, or ViewModel.
//
// ⭐ SINGLE RESPONSIBILITY ⭐
//   Mapper only maps. It does NOT decide visibility.
//   It does NOT handle events. It does NOT sort content.
//   It does NOT create initial state.
//   Factory creates initial state. Reducer handles events.
//   Collection Engine owns sorting.
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Pure mapper from Repository / Experience OS to Library snapshots.
 *
 * Every mapping function is:
 *   - Pure: same input → same output, no side effects
 *   - Single-direction: Source → Library snapshot (never reverse)
 *   - Lossy: only maps fields Library needs (lightweight snapshots)
 *   - Deterministic: no randomness, no time dependency, no external state
 *
 * Usage: Called by ViewModel and StateFactory.
 * This is the ONLY code that touches raw domain models for Library.
 */
object LibraryMapper {

    // ═════════════════════════════════════════════════════════════════
    // SNAPSHOT MAPPERS — One per domain model
    // ═════════════════════════════════════════════════════════════════

    /**
     * Map [Song] domain model → [SongSnapshot].
     *
     * Extracts ONLY the fields Library renders.
     * Adds formatted duration and display properties.
     *
     * Dropped fields (and why):
     *   - path, uri → File system details, Library doesn't render them
     *   - size → Shown in FolderSnapshot, not individual songs
     *   - dateModified → Not displayed in Library
     *   - trackNumber → Shown in album detail, not Library grid
     *   - year → Shown in album detail, not Library grid
     *   - bitrate, sampleRate → Shown in song info, not Library card
     *   - mimeType → Technical detail, not rendered in Library
     *
     * @param song Raw domain model from Repository.
     * @return Lightweight snapshot for Library card rendering.
     */
    fun mapSong(song: Song): SongSnapshot {
        return SongSnapshot(
            id = song.id,
            title = song.title,
            artist = song.artist,
            album = song.album,
            albumId = song.albumId,
            duration = song.duration,
            durationFormatted = song.durationFormatted,
            artUri = null, // Art URI resolved by ViewModel/artwork loader
            isFavorite = song.isFavorite,
            dateAdded = song.dateAdded
        )
    }

    /**
     * Map list of [Song] domain models → list of [SongSnapshot].
     *
     * Batch mapping for efficiency. Used by Collection Engine
     * when preparing section content.
     */
    fun mapSongs(songs: List<Song>): List<SongSnapshot> {
        return songs.map { mapSong(it) }
    }

    // ═════════════════════════════════════════════════════════════════
    // COLOR MAPPER — ColorState → LibraryColorSnapshot
    // ═════════════════════════════════════════════════════════════════

    /**
     * Map [ColorState] → [LibraryColorSnapshot].
     *
     * Extracts ONLY the fields Library renders. This is a lossy
     * transformation — ColorState has 24+ fields, Library needs 10.
     *
     * Library color usage differs from Mini Player:
     *   - More ambient (backgroundTint, cardGlass)
     *   - Less reactive (no pulse, no beat)
     *   - Section accents for headers
     *   - Dock tint for Mini Player dock
     *
     * @param colorState Raw color state from ColorIntelligenceEngine.
     * @return Lightweight color snapshot for Library rendering.
     */
    fun mapColors(colorState: ColorState): LibraryColorSnapshot {
        return LibraryColorSnapshot(
            primary = colorState.primary,
            secondary = colorState.secondary,
            accent = colorState.accent,
            backgroundTint = colorState.glassTint,
            cardGlass = colorState.glassTint,
            sectionAccent = colorState.waveStart,
            dockTint = colorState.glassTint,
            dominantLuminance = colorState.dominantLuminance,
            isDark = colorState.isDarkArtwork
        )
    }

    // ═════════════════════════════════════════════════════════════════
    // WAVE MAPPER — WaveState → LibraryWaveSnapshot
    // ═════════════════════════════════════════════════════════════════

    /**
     * Map [WaveState] → [LibraryWaveSnapshot].
     *
     * Library wave is SUBTLE — alive but not dominant.
     * Rule #29: Motion Explains Hierarchy.
     * Library wave opacity is capped at 0.3, thickness at 0.5.
     *
     * Dropped fields (and why):
     *   - hasBeat → Library doesn't react to individual beats
     *   - isExpanding, isSettling → Internal physics, Library sees result
     *   - overallEnergy → Mapped to simplified energy field
     *   - ribbon, path, highlight → Full geometry, Library uses simplified
     *
     * @param waveState Raw wave state from WaveEngine.
     * @return Lightweight wave snapshot for Library rendering.
     */
    fun mapWave(waveState: WaveState): LibraryWaveSnapshot {
        return LibraryWaveSnapshot(
            isActive = waveState.isActive,
            energy = waveState.overallEnergy,
            opacity = if (waveState.isActive) 0.3f * waveState.opacity else 0f,
            thickness = if (waveState.isActive) 0.5f * waveState.thickness else 0.5f
        )
    }

    // ═════════════════════════════════════════════════════════════════
    // EXPERIENCE MERGE — Experience OS data → LibraryState merge
    // ═════════════════════════════════════════════════════════════════

    /**
     * Merge Experience OS color and wave data into [LibraryState].
     *
     * Preserves ALL local state (header, selection, scroll, search,
     * filter, sort, dock, runtime) — only updates color and wave.
     *
     * Why preserve local state?
     *   Experience OS updates come per-frame (60-144Hz for wave).
     * If we reset scroll/selection on every color change,
     * all user interaction would break instantly.
     *
     * @param color Color snapshot from [mapColors].
     * @param wave Wave snapshot from [mapWave].
     * @param currentState Current LibraryState (preserves all local state).
     * @return New LibraryState with updated Experience OS data.
     */
    fun mergeExperience(
        color: LibraryColorSnapshot,
        wave: LibraryWaveSnapshot,
        currentState: LibraryState
    ): LibraryState {
        return currentState.copy(
            color = color,
            wave = wave
        )
    }

    /**
     * Merge playback state into Library dock.
     *
     * Updates dock visibility based on whether a song is playing.
     * Preserves ALL other state.
     *
     * @param isPlaying Whether media is actively playing.
     * @param currentSongId Current song ID. Null if nothing playing.
     * @param currentState Current LibraryState.
     * @return New LibraryState with updated dock state.
     */
    fun mergePlayback(
        isPlaying: Boolean,
        currentSongId: Long?,
        currentState: LibraryState
    ): LibraryState {
        return currentState.copy(
            dock = currentState.dock.copy(
                isDocked = currentSongId != null
            )
        )
    }

    // ═════════════════════════════════════════════════════════════════
    // CONVENIENCE: Combined snapshot for batch updates
    // ═════════════════════════════════════════════════════════════════

    /**
     * A combined snapshot of all Experience OS data needed by Library.
     *
     * Used by ViewModel's combine{} block to batch all Experience OS
     * updates into a single merge operation.
     */
    data class ExperienceSnapshot(
        val colorState: ColorState = ColorState.IDLE,
        val waveState: WaveState = WaveState.IDLE,
        val isPlaying: Boolean = false,
        val currentSongId: Long? = null
    ) {
        /**
         * Map all Experience OS states to their Library snapshots.
         */
        fun toSnapshots(): Pair<LibraryColorSnapshot, LibraryWaveSnapshot> {
            return Pair(
                mapColors(colorState),
                mapWave(waveState)
            )
        }
    }

    /**
     * Convenience: Create [ExperienceSnapshot] from raw flows.
     * Called by ViewModel's combine{} block.
     */
    fun createSnapshot(
        colorState: ColorState,
        waveState: WaveState,
        isPlaying: Boolean = false,
        currentSongId: Long? = null
    ): ExperienceSnapshot {
        return ExperienceSnapshot(
            colorState = colorState,
            waveState = waveState,
            isPlaying = isPlaying,
            currentSongId = currentSongId
        )
    }
}
