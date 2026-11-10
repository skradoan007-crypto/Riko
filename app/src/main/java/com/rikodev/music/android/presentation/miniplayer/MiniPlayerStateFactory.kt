package com.rikodev.music.android.presentation.miniplayer

import com.rikodev.music.android.core.experience.ColorState
import com.rikodev.music.android.core.experience.wave.WaveState
import com.rikodev.music.android.domain.model.PlayerState
import com.rikodev.music.android.domain.model.Song
import androidx.compose.ui.platform.LocalDensity

// ═══════════════════════════════════════════════════════════════════════════
// RIKO MUSIC — MINI PLAYER 2.0: EXPERIENCE BRIDGE — STATE FACTORY
//
// Build-2, Part B: MiniPlayerStateFactory
//
// Creates initial MiniPlayerState for every lifecycle scenario.
// NO UI. NO Compose. NO side effects. Pure state creation.
//
// Scenarios:
//   1. Cold Start      — App just launched, nothing playing
//   2. Track Change    — New track started (same session)
//   3. Playback Resume — User unpaused (same track)
//   4. Notification    — User tapped notification to return
//   5. App Restore     — User returned from recent apps / deep link
//
// ⭐ SINGLE RESPONSIBILITY ⭐
//   Factory only CREATES initial state.
//   It does NOT update state (that's the reducer's job).
//   It does NOT map Experience OS (that's the mapper's job).
//   It uses Mapper for the mapping step, then composes the full state.
//
// ⭐ KEY DESIGN DECISION ⭐
//   Every factory method returns a COMPLETE MiniPlayerState.
//   No partial state, no "then call mergeExperience later".
//   If you get a state from Factory, it's ready to render.
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Factory for creating initial [MiniPlayerState] instances.
 *
 * Each factory method corresponds to a specific app lifecycle scenario.
 * The factory uses [MiniPlayerMapper] for Experience OS mapping, then
 * composes the complete state including morph, gesture, interaction,
 * and layout sub-states.
 *
 * All methods are pure functions — deterministic, no side effects,
 * no Android framework dependencies.
 */
object MiniPlayerStateFactory {

    // ═════════════════════════════════════════════════════════════════
    // SCENARIO 1: COLD START
    // ═════════════════════════════════════════════════════════════════

    /**
     * Cold start — app just launched, nothing playing.
     *
     * State: Invisible, collapsed, idle. Everything at defaults.
     * This is the safest initial state — mini player simply won't render.
     *
     * When does this happen?
     *   - Fresh install, first launch
     *   - App killed and relaunched with no media session
     *   - After "Clear Data"
     *
     * What happens next?
     *   - When user plays a song: ViewModel calls [forTrackChange]
     *   - If a media session exists: ViewModel calls [forNotificationResume]
     */
    fun coldStart(): MiniPlayerState {
        return MiniPlayerState.IDLE
    }

    // ═════════════════════════════════════════════════════════════════
    // SCENARIO 2: TRACK CHANGE
    // ═════════════════════════════════════════════════════════════════

    /**
     * Track change — a new song started playing.
     *
     * State: Visible, collapsed, fresh morph/gesture/interaction.
     * Color and wave start from whatever the current Experience OS provides.
     *
     * When does this happen?
     *   - User tapped a song in the library
     *   - Auto-advance to next track in queue
     *   - User skipped forward/backward
     *   - Queue reshuffled
     *
     * Key behavior:
     *   - Morph is ALWAYS reset to IDLE on track change
     *   - If user was mid-morph (dragging to expand), it collapses
     *   - This is a deliberate UX decision: track change = new context
     *
     * @param song The new song. Null if no song (shouldn't happen here).
     * @param albumArtUri Album art content URI. Null if no artwork.
     */
    fun forTrackChange(
        song: Song?,
        albumArtUri: String?
    ): MiniPlayerState {
        if (song == null) return coldStart()

        return MiniPlayerState(
            isVisible = true,
            isExpanded = false,
            currentSong = song,
            isPlaying = true,
            progress = 0f,
            position = 0L,
            duration = song.duration,
            albumArtUri = albumArtUri,
            isFavorite = song.isFavorite,
            isLoading = false,
            playbackError = null,
            color = MiniPlayerColorSnapshot.DEFAULT,
            wave = MiniPlayerWaveSnapshot.IDLE_BREATHING,
            morph = MiniPlayerMorphState.IDLE,
            gesture = MiniPlayerGestureState.IDLE,
            interaction = MiniPlayerInteractionState.IDLE,
            layout = MiniPlayerMapper.mapLayout(0f)
        )
    }

    /**
     * Track change with full Experience OS data.
     *
     * Called by ViewModel when it has all three Experience OS states
     * available (PlayerState, ColorState, WaveState) at track start.
     *
     * Uses [MiniPlayerMapper] for all Experience OS mapping.
     * Color and wave may already have data from the new artwork.
     */
    fun forTrackChange(
        song: Song?,
        albumArtUri: String?,
        playerState: PlayerState,
        colorState: ColorState,
        waveState: WaveState,
        isFavorite: Boolean
    ): MiniPlayerState {
        if (song == null) return coldStart()

        val colorSnapshot = MiniPlayerMapper.mapColors(colorState)
        val waveSnapshot = MiniPlayerMapper.mapWave(waveState)

        return MiniPlayerState(
            isVisible = true,
            isExpanded = false,
            currentSong = song,
            isPlaying = playerState.isPlaying,
            progress = playerState.progress,
            position = playerState.position,
            duration = playerState.duration,
            albumArtUri = albumArtUri,
            isFavorite = isFavorite,
            isLoading = playerState.isLoading,
            playbackError = playerState.error,
            color = colorSnapshot,
            wave = waveSnapshot,
            morph = MiniPlayerMorphState.IDLE,
            gesture = MiniPlayerGestureState.IDLE,
            interaction = MiniPlayerInteractionState.IDLE,
            layout = MiniPlayerMapper.mapLayout(0f)
        )
    }

    // ═════════════════════════════════════════════════════════════════
    // SCENARIO 3: PLAYBACK RESUME
    // ═════════════════════════════════════════════════════════════════

    /**
     * Playback resume — user unpaused the current track.
     *
     * State: Visible, collapsed, isPlaying = true. Preserves current
     * song, progress, color, wave. Only changes isPlaying.
     *
     * When does this happen?
     *   - User tapped play/pause
     *   - Bluetooth headset play command
     *   - Notification play button
     *   - WaveEngine resumes (idle → active)
     *
     * Key behavior:
     *   - Morph is NOT reset (user might be mid-morph while pausing/resuming)
     *   - Wave transitions from IDLE_BREATHING → active (handled by WaveEngine)
     *   - Color energyIntensity resumes (handled by ColorBrain)
     *
     * @param currentState The state before pause (preserves morph, gesture, etc.).
     * @param playerState Updated PlayerState (isPlaying = true, updated position).
     */
    fun forPlaybackResume(
        currentState: MiniPlayerState,
        playerState: PlayerState
    ): MiniPlayerState {
        return MiniPlayerMapper.mergeExperience(
            playback = MiniPlayerMapper.mapPlayback(playerState, currentState.isFavorite, currentState.albumArtUri),
            color = currentState.color,
            wave = currentState.wave,
            currentState = currentState
        )
    }

    // ═════════════════════════════════════════════════════════════════
    // SCENARIO 4: NOTIFICATION RESUME
    // ═════════════════════════════════════════════════════════════════

    /**
     * Notification resume — user tapped the media notification.
     *
     * State: Visible, collapsed (always), with current playback data.
     * This is similar to app restore but specifically from notification tap.
     *
     * When does this happen?
     *   - User tapped "Play" on media notification
     *   - User tapped notification body to open app
     *   - Notification action triggered (next, previous)
     *
     * Key behavior:
     *   - ALWAYS collapsed (morph resets). User is coming from outside.
     *   - No mid-morph preservation. Fresh collapsed state.
     *   - Color/Wave may need warm-up time (first render might use defaults)
     *
     * @param playerState Current playback state from PlayerManager.
     * @param colorState Current color state (may be IDLE if app was killed).
     * @param waveState Current wave state (may be IDLE if app was killed).
     * @param isFavorite Whether current song is in favorites.
     * @param albumArtUri Album art URI (may need reload if app was killed).
     */
    fun forNotificationResume(
        playerState: PlayerState,
        colorState: ColorState,
        waveState: WaveState,
        isFavorite: Boolean,
        albumArtUri: String?
    ): MiniPlayerState {
        if (playerState.currentSong == null) return coldStart()

        return MiniPlayerState(
            isVisible = true,
            isExpanded = false,
            currentSong = playerState.currentSong,
            isPlaying = playerState.isPlaying,
            progress = playerState.progress,
            position = playerState.position,
            duration = playerState.duration,
            albumArtUri = albumArtUri,
            isFavorite = isFavorite,
            isLoading = playerState.isLoading,
            playbackError = playerState.error,
            color = MiniPlayerMapper.mapColors(colorState),
            wave = MiniPlayerMapper.mapWave(waveState),
            morph = MiniPlayerMorphState.IDLE,
            gesture = MiniPlayerGestureState.IDLE,
            interaction = MiniPlayerInteractionState.IDLE,
            layout = MiniPlayerMapper.mapLayout(0f)
        )
    }

    // ═════════════════════════════════════════════════════════════════
    // SCENARIO 5: APP RESTORE
    // ═════════════════════════════════════════════════════════════════

    /**
     * App restore — user returned from recent apps or deep link.
     *
     * State: Visible if song exists, collapsed (always), with current data.
     * Similar to notification resume but handles config changes better.
     *
     * When does this happen?
     *   - User switched away and came back (recent apps)
     *   - Deep link opened the app (now playing, specific track)
     *   - Activity recreation (config change, multi-window)
     *   - Process death recovery (saved state restored)
     *
     * Key behavior:
     *   - ALWAYS collapsed. No morph state survives process death.
     *   - Experience OS states are whatever PlayerManager/WaveEngine/ColorBrain
     *     have at restore time (may be IDLE if engines are still initializing).
     *   - If process was killed, color/wave will be defaults → natural fade-in
     *     as ColorBrain analyzes artwork and WaveEngine starts.
     *
     * @param playerState Current playback state from PlayerManager.
     * @param colorState Current color state from ColorIntelligenceEngine.
     * @param waveState Current wave state from WaveEngine.
     * @param isFavorite Whether current song is in favorites.
     * @param albumArtUri Album art URI from artwork cache/loader.
     */
    fun forAppRestore(
        playerState: PlayerState,
        colorState: ColorState,
        waveState: WaveState,
        isFavorite: Boolean,
        albumArtUri: String?
    ): MiniPlayerState {
        if (playerState.currentSong == null) return coldStart()

        return MiniPlayerState(
            isVisible = true,
            isExpanded = false,
            currentSong = playerState.currentSong,
            isPlaying = playerState.isPlaying,
            progress = playerState.progress,
            position = playerState.position,
            duration = playerState.duration,
            albumArtUri = albumArtUri,
            isFavorite = isFavorite,
            isLoading = playerState.isLoading,
            playbackError = playerState.error,
            color = MiniPlayerMapper.mapColors(colorState),
            wave = MiniPlayerMapper.mapWave(waveState),
            morph = MiniPlayerMorphState.IDLE,
            gesture = MiniPlayerGestureState.IDLE,
            interaction = MiniPlayerInteractionState.IDLE,
            layout = MiniPlayerMapper.mapLayout(0f)
        )
    }

    // ═════════════════════════════════════════════════════════════════
    // SCENARIO 6: CONFIGURATION CHANGE
    // ═════════════════════════════════════════════════════════════════

    /**
     * Configuration change — rotation, multi-window, density change.
     *
     * State: SAME as current state, only layout recalculated.
     * Morph, gesture, interaction are preserved.
     * This prevents the mini player from collapsing on rotation.
     *
     * When does this happen?
     *   - Screen rotation (portrait ↔ landscape)
     *   - Multi-window mode entered/exited
     *   - Display density changed (accessibility settings)
     *   - Font size changed
     *
     * Key behavior:
     *   - Morph progress is PRESERVED (contract Part K: PRESERVE policy)
     *   - Layout values are recalculated from morphProgress
     *   - This is the ONLY scenario where morph survives lifecycle event
     *
     * @param currentState The state before config change.
     * @return Same state with recalculated layout snapshot.
     */
    fun forConfigChange(currentState: MiniPlayerState): MiniPlayerState {
        return currentState.copy(
            layout = MiniPlayerMapper.mapLayout(currentState.morph.morphProgress)
        )
    }

    // ═════════════════════════════════════════════════════════════════
    // SCENARIO 7: SCREEN OFF / SCREEN ON
    // ═════════════════════════════════════════════════════════════════

    /**
     * Screen off — all animations stop, mini player becomes invisible.
     *
     * State: Invisible, all local state reset to IDLE.
     * Experience OS data (song, color, wave) preserved.
     * On screen on, [forScreenOn] restores visibility.
     *
     * Why reset local state?
     *   When screen is off, user can't interact. Any morph in progress
     *   would be stale when screen turns back on (spring animations have
     *   no frames to animate). Better to start clean.
     */
    fun forScreenOff(currentState: MiniPlayerState): MiniPlayerState {
        return currentState.copy(
            isVisible = false,
            morph = MiniPlayerMorphState.IDLE,
            gesture = MiniPlayerGestureState.IDLE,
            interaction = MiniPlayerInteractionState.IDLE,
            isExpanded = false
        )
    }

    /**
     * Screen on — restore visibility if a song is loaded.
     *
     * State: Visible if song exists, collapsed. Experience OS data preserved.
     * Local state stays at IDLE (set by [forScreenOff]).
     */
    fun forScreenOn(currentState: MiniPlayerState): MiniPlayerState {
        return currentState.copy(
            isVisible = currentState.currentSong != null,
            layout = MiniPlayerMapper.mapLayout(0f)
        )
    }
}
