package com.rikodev.music.android.presentation.miniplayer

import com.rikodev.music.android.core.experience.ColorState
import com.rikodev.music.android.core.experience.wave.WaveState
import com.rikodev.music.android.domain.model.PlayerState
import com.rikodev.music.android.presentation.minicontact.MiniPlayerMorphSpec
import androidx.compose.ui.graphics.Path
import androidx.compose.animation.core.Animatable

// ═══════════════════════════════════════════════════════════════════════════
// RIKO MUSIC — MINI PLAYER 2.0: EXPERIENCE BRIDGE — MAPPER
//
// Build-2, Part A: MiniPlayerMapper
//
// Pure mapping functions: Experience OS → MiniPlayerState snapshots.
// NO UI. NO Compose. NO side effects. NO business logic.
// NO mutation of MiniPlayerState beyond what Mapper owns.
//
// Architecture:
//
//   PlayerState ──────────────┐
//   ColorState ──────────────┤
//   WaveState  ──────────────┼──→ MiniPlayerMapper ──→ MiniPlayerState
//                             │        (pure)
//   isFavorite (repo query) ─┘
//
// ⭐ NEW RULE (Build-2) ⭐
//   "No UI reads raw Experience OS."
//   MiniPlayerAlbum → MiniPlayerState (NOT → ColorState)
//   This Mapper is the ONLY code that touches raw Experience OS.
//
// ⭐ SINGLE RESPONSIBILITY ⭐
//   Mapper only maps. It does NOT decide visibility.
//   It does NOT handle gestures. It does NOT create initial state.
//   Factory creates initial state. Reducer handles events.
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Pure mapper from Experience OS to Mini Player snapshots.
 *
 * Every mapping function is:
 *   - Pure: same input → same output, no side effects
 *   - Single-direction: Experience OS → MiniPlayer snapshot (never reverse)
 *   - Lossy: only maps fields Mini Player needs (lightweight snapshots)
 *   - Deterministic: no randomness, no time dependency, no external state
 *
 * Usage: Called by ViewModel to merge Experience OS data into MiniPlayerState.
 * Also called by StateFactory for initial state creation.
 */
object MiniPlayerMapper {

    // ═════════════════════════════════════════════════════════════════
    // SNAPSHOT MAPPERS — One per Experience OS state
    // ═════════════════════════════════════════════════════════════════

    /**
     * Map [PlayerState] → playback fields needed by Mini Player.
     *
     * Extracts ONLY the fields Mini Player renders:
     *   - Song metadata (title, artist for display)
     *   - Playback status (playing, position, progress)
     *   - Loading/error states
     *
     * Does NOT extract: queue, repeatMode, shuffleMode, bufferedPosition,
     * playbackSpeed — these belong to Now Playing screen, not Mini Player.
     *
     * @param playerState Raw playback state from PlayerManager/Experience Hub.
     * @param isFavorite Favorite status from repository query.
     * @param albumArtUri Album art content URI string (from artwork loader).
     * @return Mapped playback fields as a flat data holder.
     */
    fun mapPlayback(
        playerState: PlayerState,
        isFavorite: Boolean = false,
        albumArtUri: String? = null
    ): PlaybackSnapshot {
        return PlaybackSnapshot(
            currentSong = playerState.currentSong,
            isPlaying = playerState.isPlaying,
            progress = playerState.progress,
            position = playerState.position,
            duration = playerState.duration,
            isLoading = playerState.isLoading,
            playbackError = playerState.error,
            isFavorite = isFavorite,
            albumArtUri = albumArtUri
        )
    }

    /**
     * Map [ColorState] → [MiniPlayerColorSnapshot].
     *
     * Extracts ONLY the fields Mini Player renders. This is a lossy
     * transformation — ColorState has 24+ fields, Mini Player needs 13.
     *
     * Dropped fields (and why):
     *   - backgroundGradient → Mini Player uses glass, not solid backgrounds
     *   - isMonochrome → Handled by Color Brain, Mini Player sees result
     *   - temperature → Used by Color Brain for modulation, not directly rendered
     *   - contrastLevel → Used by Color Brain, not directly rendered
     *   - dominantHue → Used by Color Brain, not directly rendered
     *   - dominantSaturation → Used by Color Brain, not directly rendered
     *   - tintStrength → Mapped into glassTint by Color Brain
     *   - isFromArtwork → Implementation detail, Mini Player doesn't care
     *
     * @param colorState Raw color state from ColorIntelligenceEngine.
     * @return Lightweight color snapshot for Mini Player rendering.
     */
    fun mapColors(colorState: ColorState): MiniPlayerColorSnapshot {
        return MiniPlayerColorSnapshot(
            primary = colorState.primary,
            secondary = colorState.secondary,
            accent = colorState.accent,
            waveStart = colorState.waveStart,
            waveEnd = colorState.waveEnd,
            glassTint = colorState.glassTint,
            ambientGlow = colorState.ambientGlow,
            shadowColor = colorState.shadowColor,
            dominantLuminance = colorState.dominantLuminance,
            isDarkArtwork = colorState.isDarkArtwork,
            energyIntensity = colorState.energyIntensity,
            pulseAmount = colorState.pulseAmount,
            isIdle = colorState.isIdle
        )
    }

    /**
     * Map [WaveState] → [MiniPlayerWaveSnapshot].
     *
     * Extracts ONLY the fields Mini Player renders. WaveState has
     * complex nested structures (WaveEnergy, RibbonState, OrganicPath,
     * HighlightState) — Mini Player only needs the flat summary.
     *
     * Dropped fields (and why):
     *   - energy.bassPressure, midTexture, trebleSpark, etc. → Mini Player
     *     uses overallEnergy, not individual frequency bands
     *   - ribbon.elasticity, viscosity, pressure, turbulence → Physics internals
     *   - path (OrganicPath) → Full geometry, Mini Player renders simplified
     *   - highlight (HighlightState) → Glass/reflection data, not for Mini
     *   - mode (RibbonMode) → Internal behavior, Mini Player sees result
     *   - isVisible → Always visible if wave snapshot exists
     *   - timestamp → Frame timing, Mini Player uses animation clocks
     *
     * @param waveState Raw wave state from WaveEngine.
     * @return Lightweight wave snapshot for Mini Player rendering.
     */
    fun mapWave(waveState: WaveState): MiniPlayerWaveSnapshot {
        return MiniPlayerWaveSnapshot(
            isActive = waveState.isActive,
            hasBeat = waveState.hasBeat,
            overallEnergy = waveState.overallEnergy,
            thickness = waveState.thickness,
            opacity = waveState.opacity,
            isExpanding = waveState.isExpanding,
            isSettling = waveState.isSettling,
            energy = waveState.overallEnergy
        )
    }

    // ═════════════════════════════════════════════════════════════════
    // LAYOUT MAPPER — Morph progress → Layout snapshot
    // ═════════════════════════════════════════════════════════════════

    /**
     * Map [morphProgress] → [MiniPlayerLayoutSnapshot].
     *
     * Uses [MiniPlayerMorphSpec] interpolation functions from the contract.
     * This is the ONLY place where morph progress becomes layout values.
     * Components read these values directly — zero computation in composables.
     *
     * The layout snapshot contains ALL visual properties that change during
     * morph: height, width, corner radius, album art size, opacity values,
     * shadow elevation, glow radius, reflection alpha, wave thickness.
     *
     * @param morphProgress Current morph progress 0f..1f.
     * @return Pre-computed layout values for all components.
     */
    fun mapLayout(morphProgress: Float): MiniPlayerLayoutSnapshot {
        val p = morphProgress.coerceIn(0f, 1f)

        return MiniPlayerLayoutSnapshot(
            // ── Primary dimensions (from MorphSpec) ────────────────
            height = MiniPlayerMorphSpec.heightAt(p, 0),
            widthFraction = MiniPlayerMorphSpec.widthFractionAt(p),
            cornerRadius = MiniPlayerMorphSpec.cornerRadiusAt(p),
            albumArtSize = MiniPlayerMorphSpec.albumArtSizeAt(p),

            // ── Content opacity (from MorphSpec) ────────────────────
            controlsOpacity = MiniPlayerMorphSpec.controlsOpacityAt(p),
            waveVisibility = MiniPlayerMorphSpec.waveVisibilityAt(p),

            // ── Glass (inverse of morph progress) ───────────────────
            glassTintAlpha = 0.10f * (1f - p),
            glassShadowAlpha = 0.20f * (1f - p),

            // ── Shadow elevation (8dp → 24dp) ────────────────────
            shadowElevation = 8f + 16f * p,

            // ── Ambient glow (appears after Phase 4) ──────────────
            glowRadius = if (p > 0.50f) 40f * ((p - 0.50f) / 0.50f) else 0f,
            glowIntensity = if (p > 0.50f) 1f * ((p - 0.50f) / 0.50f) else 0f,

            // ── Reflection (appears after Phase 5) ─────────────────
            reflectionAlpha = if (p > 0.70f) 0.25f * ((p - 0.70f) / 0.30f) else 0f,

            // ── Wave thickness multiplier (0.5 → 1.0) ────────────
            waveThickness = 0.5f + 0.5f * p,

            // ── Crossfade (Phase 6: HANDOFF 0.85 → 1.00) ─────────
            miniPlayerOpacity = if (p > 0.85f) 1f - ((p - 0.85f) / 0.15f) else 1f,
            nowPlayingOpacity = if (p > 0.85f) (p - 0.85f) / 0.15f else 0f
        )
    }

    // ═════════════════════════════════════════════════════════════════
    // EXPERIENCE MERGE — Full Experience OS → MiniPlayerState merge
    // ═════════════════════════════════════════════════════════════════

    /**
     * Merge a full Experience OS snapshot into [MiniPlayerState].
     *
     * This is the PRIMARY function called by ViewModel on every Experience
     * OS change. It maps all three states (Player, Color, Wave) into
     * MiniPlayerState snapshots, while PRESERVING local state:
     *   - morph (gesture + animation state)
     *   - gesture (accumulated drag, direction lock)
     *   - interaction (press, haptic, breathing)
     *
     * Why preserve local state?
     *   Experience OS updates come per-frame (60-144Hz for wave).
     * If we reset morph/gesture on every color change, all gestures
     * would break instantly. Local state must survive Experience OS updates.
     *
     * @param playback Playback snapshot from [mapPlayback].
     * @param color Color snapshot from [mapColors].
     * @param wave Wave snapshot from [mapWave].
     * @param currentState Current MiniPlayerState (preserves morph/gesture/interaction).
     * @return New MiniPlayerState with updated Experience OS data, preserved local state.
     */
    fun mergeExperience(
        playback: PlaybackSnapshot,
        color: MiniPlayerColorSnapshot,
        wave: MiniPlayerWaveSnapshot,
        currentState: MiniPlayerState
    ): MiniPlayerState {
        // Detect track change — triggers morph reset if expanded
        val songChanged = playback.currentSong?.id != currentState.currentSong?.id
        val shouldResetMorph = songChanged && currentState.morph.morphProgress > 0f

        return currentState.copy(
            // ── Playback (from Experience OS) ────────────────────
            currentSong = playback.currentSong,
            isPlaying = playback.isPlaying,
            progress = playback.progress,
            position = playback.position,
            duration = playback.duration,
            isLoading = playback.isLoading,
            playbackError = playback.playbackError,
            isFavorite = playback.isFavorite,
            albumArtUri = playback.albumArtUri,
            isVisible = playback.currentSong != null,

            // ── Color (from Experience OS) ───────────────────────
            color = color,

            // ── Wave (from Experience OS) ────────────────────────
            wave = wave
        ).let { state ->
            if (shouldResetMorph) {
                // Track changed while morphing — reset to collapsed
                state.copy(
                    morph = MiniPlayerMorphState.IDLE,
                    gesture = MiniPlayerGestureState.IDLE,
                    interaction = MiniPlayerInteractionState.IDLE,
                    isExpanded = false
                )
            } else {
                state
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // CONVENIENCE: Combined snapshot for batch updates
    // ═════════════════════════════════════════════════════════════════

    /**
     * A combined snapshot of all Experience OS data needed by Mini Player.
     *
     * Used by ViewModel's combine{} block to batch all Experience OS
     * updates into a single merge operation. This avoids triple-recomposition
     * when PlayerState, ColorState, and WaveState all change in the same frame.
     */
    data class ExperienceSnapshot(
        val playerState: PlayerState = PlayerState(),
        val colorState: ColorState = ColorState.IDLE,
        val waveState: WaveState = WaveState.IDLE,
        val isFavorite: Boolean = false,
        val albumArtUri: String? = null
    ) {
        /**
         * Map all Experience OS states to their Mini Player snapshots.
         * Returns individual snapshots for selective update or full merge.
         */
        fun toSnapshots(): Triple<PlaybackSnapshot, MiniPlayerColorSnapshot, MiniPlayerWaveSnapshot> {
            return Triple(
                mapPlayback(playerState, isFavorite, albumArtUri),
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
        playerState: PlayerState,
        colorState: ColorState,
        waveState: WaveState,
        isFavorite: Boolean = false,
        albumArtUri: String? = null
    ): ExperienceSnapshot {
        return ExperienceSnapshot(
            playerState = playerState,
            colorState = colorState,
            waveState = waveState,
            isFavorite = isFavorite,
            albumArtUri = albumArtUri
        )
    }

    // ═════════════════════════════════════════════════════════════════
    // PLAYBACK SNAPSHOT — Mapped playback fields
    // ═════════════════════════════════════════════════════════════════

    /**
     * Flat data holder for mapped playback fields.
     *
     * Why a separate class instead of returning MiniPlayerState directly?
     *   1. Mapper is a stateless pure function — it shouldn't create
     *      full MiniPlayerState instances (that's the reducer's job).
     *   2. Factory needs individual snapshots to compose initial state.
     *   3. Tests can verify mapping in isolation.
     */
    data class PlaybackSnapshot(
        val currentSong: com.rikodev.music.android.domain.model.Song? = null,
        val isPlaying: Boolean = false,
        val progress: Float = 0f,
        val position: Long = 0L,
        val duration: Long = 0L,
        val isLoading: Boolean = false,
        val playbackError: String? = null,
        val isFavorite: Boolean = false,
        val albumArtUri: String? = null
    )
}
