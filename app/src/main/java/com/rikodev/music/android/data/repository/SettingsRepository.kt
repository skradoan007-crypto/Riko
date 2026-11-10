package com.rikodev.music.android.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import androidx.compose.material.icons.filled.Pause

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — SETTINGS REPOSITORY
//
// Powers the "Advanced Customisation" Settings screen.
// Every toggle/slider here is a real, functional preference that other
// parts of the app (theme, glass system, playback) read from reactively.
// ─────────────────────────────────────────────────────────────────────────────

private val Context.dataStore by preferencesDataStore(name = "riko_settings")

enum class GlassIntensityPref { SUBTLE, BALANCED, BOLD }
enum class ThemeAccentSource { ALBUM_ART, FIXED_CYAN, FIXED_VIOLET }

/**
 * Visualizer Lab colour theme. ALBUM_ACCENT follows the current track's
 * artwork palette (one song = one environment); the fixed themes are
 * user-chosen identities that stay stable across tracks.
 */
enum class VisualizerTheme { ALBUM_ACCENT, NEON_CYAN, AURORA_VIOLET, MONO }

/**
 * Visualizer Lab configuration — persisted so the user's setup survives
 * restarts (DataStore, same store as [AppSettings]).
 *
 * [styleName] is a SpectrumStyle name (AURORA / CLASSIC) kept as a String
 * so the data layer never imports presentation types.
 * [bandDensity] = display band count: 32 sparse · 40 balanced · 48 rich.
 */
data class VisualizerPrefs(
    val enabled: Boolean = true,
    val styleName: String = "AURORA",
    val theme: VisualizerTheme = VisualizerTheme.ALBUM_ACCENT,
    val sensitivity: Float = 1.0f,
    val glowEnabled: Boolean = true,
    val bandDensity: Int = 40,
    // Plan 5.5 schema: shape / motionSpeed / reactivityCurve. Missing
    // DataStore keys fall back to these defaults = forward-compatible
    // migration for every previously saved setup.
    val shapeName: String = "RIBBON",
    val motionSpeed: Float = 1.0f,
    val reactivityCurve: Float = 1.0f,
    /** Continuous glow intensity 0..1 (the on/off switch stays separate). */
    val glowAmount: Float = 1.0f,
    // Phase 2: Visualizer Studio extended parameters
    /** Bar width multiplier (0.5–2.0) for bar-based styles. */
    val barWidth: Float = 1.0f,
    /** Bar spacing multiplier (0.0–1.0) for bar-based styles. */
    val barSpacing: Float = 0.3f,
    /** Smoothing factor (0.0–1.0) — reduces visual jitter. */
    val smoothing: Float = 0.5f,
    /** Peak decay rate (0.1–0.9) — how fast peak caps fall. */
    val peakDecay: Float = 0.5f,
    /** Reflection effect (AURORA, LINE). */
    val reflectionEnabled: Boolean = true,
    /** Mirror effect (CLASSIC, LINE). */
    val mirrorEnabled: Boolean = false
)

data class AppSettings(
    val glassIntensity: GlassIntensityPref = GlassIntensityPref.BALANCED,
    val bubbleAnimationsEnabled: Boolean = true,
    val accentSource: ThemeAccentSource = ThemeAccentSource.ALBUM_ART,
    val gaplessPlayback: Boolean = true,
    val crossfadeDurationMs: Int = 0,           // 0 = off
    val resumeOnHeadsetConnect: Boolean = false,
    val hapticFeedbackEnabled: Boolean = true,
    val showLyricsAutomatically: Boolean = false,
    val minSongDurationSec: Int = 30,           // filters out short clips/voice notes
    val gridColumnsAllSongs: Int = 1,           // 1 = list, 2/3 = grid
    val miniPlayerWaveformEnabled: Boolean = true,
    val highQualityAlbumArt: Boolean = true,
    val pauseOnBluetoothDisconnect: Boolean = true,
    val volumeNormalizationEnabled: Boolean = false
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val GLASS_INTENSITY = stringPreferencesKey("glass_intensity")
        val BUBBLE_ANIMATIONS = booleanPreferencesKey("bubble_animations_enabled")
        val ACCENT_SOURCE = stringPreferencesKey("accent_source")
        val GAPLESS_PLAYBACK = booleanPreferencesKey("gapless_playback")
        val CROSSFADE_DURATION = intPreferencesKey("crossfade_duration_ms")
        val RESUME_ON_HEADSET = booleanPreferencesKey("resume_on_headset_connect")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback_enabled")
        val SHOW_LYRICS_AUTO = booleanPreferencesKey("show_lyrics_automatically")
        val MIN_SONG_DURATION = intPreferencesKey("min_song_duration_sec")
        val GRID_COLUMNS = intPreferencesKey("grid_columns_all_songs")
        val MINI_PLAYER_WAVEFORM = booleanPreferencesKey("mini_player_waveform_enabled")
        val HIGH_QUALITY_ART = booleanPreferencesKey("high_quality_album_art")
        val PAUSE_ON_BT_DISCONNECT = booleanPreferencesKey("pause_on_bluetooth_disconnect")
        val VOLUME_NORMALIZATION = booleanPreferencesKey("volume_normalization_enabled")
        val VISUALIZER_ENABLED = booleanPreferencesKey("visualizer_enabled")
        val VISUALIZER_STYLE = stringPreferencesKey("visualizer_style")
        val VISUALIZER_THEME = stringPreferencesKey("visualizer_theme")
        val VISUALIZER_SENSITIVITY = floatPreferencesKey("visualizer_sensitivity")
        val VISUALIZER_GLOW = booleanPreferencesKey("visualizer_glow")
        val VISUALIZER_BANDS = intPreferencesKey("visualizer_band_density")
        val VISUALIZER_SHAPE = stringPreferencesKey("visualizer_shape")
        val VISUALIZER_MOTION_SPEED = floatPreferencesKey("visualizer_motion_speed")
        val VISUALIZER_REACTIVITY = floatPreferencesKey("visualizer_reactivity_curve")
        val VISUALIZER_GLOW_AMOUNT = floatPreferencesKey("visualizer_glow_amount")
        val VISUALIZER_USER_PRESETS = stringPreferencesKey("visualizer_user_presets")
        // Phase 2: Visualizer Studio extended parameters
        val VISUALIZER_BAR_WIDTH = floatPreferencesKey("visualizer_bar_width")
        val VISUALIZER_BAR_SPACING = floatPreferencesKey("visualizer_bar_spacing")
        val VISUALIZER_SMOOTHING = floatPreferencesKey("visualizer_smoothing")
        val VISUALIZER_PEAK_DECAY = floatPreferencesKey("visualizer_peak_decay")
        val VISUALIZER_REFLECTION = booleanPreferencesKey("visualizer_reflection")
        val VISUALIZER_MIRROR = booleanPreferencesKey("visualizer_mirror")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            glassIntensity = prefs[Keys.GLASS_INTENSITY]?.let {
                runCatching { GlassIntensityPref.valueOf(it) }.getOrNull()
            } ?: GlassIntensityPref.BALANCED,
            bubbleAnimationsEnabled = prefs[Keys.BUBBLE_ANIMATIONS] ?: true,
            accentSource = prefs[Keys.ACCENT_SOURCE]?.let {
                runCatching { ThemeAccentSource.valueOf(it) }.getOrNull()
            } ?: ThemeAccentSource.ALBUM_ART,
            gaplessPlayback = prefs[Keys.GAPLESS_PLAYBACK] ?: true,
            crossfadeDurationMs = prefs[Keys.CROSSFADE_DURATION] ?: 0,
            resumeOnHeadsetConnect = prefs[Keys.RESUME_ON_HEADSET] ?: false,
            hapticFeedbackEnabled = prefs[Keys.HAPTIC_FEEDBACK] ?: true,
            showLyricsAutomatically = prefs[Keys.SHOW_LYRICS_AUTO] ?: false,
            minSongDurationSec = prefs[Keys.MIN_SONG_DURATION] ?: 30,
            gridColumnsAllSongs = prefs[Keys.GRID_COLUMNS] ?: 1,
            miniPlayerWaveformEnabled = prefs[Keys.MINI_PLAYER_WAVEFORM] ?: true,
            highQualityAlbumArt = prefs[Keys.HIGH_QUALITY_ART] ?: true,
            pauseOnBluetoothDisconnect = prefs[Keys.PAUSE_ON_BT_DISCONNECT] ?: true,
            volumeNormalizationEnabled = prefs[Keys.VOLUME_NORMALIZATION] ?: false
        )
    }.flowOn(Dispatchers.IO) // PERF P2 (RC-12): proto/deser off Main

    /**
     * Visualizer Lab configuration — same DataStore, independent flow so the
     * lab screen never waits on unrelated settings emissions.
     */
    val visualizerPrefs: Flow<VisualizerPrefs> = context.dataStore.data.map { prefs ->
        VisualizerPrefs(
            enabled = prefs[Keys.VISUALIZER_ENABLED] ?: true,
            styleName = prefs[Keys.VISUALIZER_STYLE] ?: "AURORA",
            theme = prefs[Keys.VISUALIZER_THEME]?.let {
                runCatching { VisualizerTheme.valueOf(it) }.getOrNull()
            } ?: VisualizerTheme.ALBUM_ACCENT,
            sensitivity = (prefs[Keys.VISUALIZER_SENSITIVITY] ?: 1f).coerceIn(0.5f, 1.5f),
            glowEnabled = prefs[Keys.VISUALIZER_GLOW] ?: true,
            bandDensity = prefs[Keys.VISUALIZER_BANDS] ?: 40,
            shapeName = prefs[Keys.VISUALIZER_SHAPE] ?: "RIBBON",
            motionSpeed = (prefs[Keys.VISUALIZER_MOTION_SPEED] ?: 1f).coerceIn(0.25f, 2f),
            reactivityCurve = (prefs[Keys.VISUALIZER_REACTIVITY] ?: 1f).coerceIn(0.25f, 2f),
            glowAmount = (prefs[Keys.VISUALIZER_GLOW_AMOUNT] ?: 1f).coerceIn(0f, 1f),
            barWidth = (prefs[Keys.VISUALIZER_BAR_WIDTH] ?: 1f).coerceIn(0.5f, 2f),
            barSpacing = (prefs[Keys.VISUALIZER_BAR_SPACING] ?: 0.3f).coerceIn(0f, 1f),
            smoothing = (prefs[Keys.VISUALIZER_SMOOTHING] ?: 0.5f).coerceIn(0f, 1f),
            peakDecay = (prefs[Keys.VISUALIZER_PEAK_DECAY] ?: 0.5f).coerceIn(0.1f, 0.9f),
            reflectionEnabled = prefs[Keys.VISUALIZER_REFLECTION] ?: true,
            mirrorEnabled = prefs[Keys.VISUALIZER_MIRROR] ?: false
        )
    }.flowOn(Dispatchers.IO)

    /** Persist the full Lab configuration in ONE edit (atomic, no partial writes). */
    suspend fun setVisualizerPrefs(value: VisualizerPrefs) {
        context.dataStore.edit {
            it[Keys.VISUALIZER_ENABLED] = value.enabled
            it[Keys.VISUALIZER_STYLE] = value.styleName
            it[Keys.VISUALIZER_THEME] = value.theme.name
            it[Keys.VISUALIZER_SENSITIVITY] = value.sensitivity.coerceIn(0.5f, 1.5f)
            it[Keys.VISUALIZER_GLOW] = value.glowEnabled
            it[Keys.VISUALIZER_BANDS] = value.bandDensity
            it[Keys.VISUALIZER_SHAPE] = value.shapeName
            it[Keys.VISUALIZER_MOTION_SPEED] = value.motionSpeed.coerceIn(0.25f, 2f)
            it[Keys.VISUALIZER_REACTIVITY] = value.reactivityCurve.coerceIn(0.25f, 2f)
            it[Keys.VISUALIZER_GLOW_AMOUNT] = value.glowAmount.coerceIn(0f, 1f)
            it[Keys.VISUALIZER_BAR_WIDTH] = value.barWidth.coerceIn(0.5f, 2f)
            it[Keys.VISUALIZER_BAR_SPACING] = value.barSpacing.coerceIn(0f, 1f)
            it[Keys.VISUALIZER_SMOOTHING] = value.smoothing.coerceIn(0f, 1f)
            it[Keys.VISUALIZER_PEAK_DECAY] = value.peakDecay.coerceIn(0.1f, 0.9f)
            it[Keys.VISUALIZER_REFLECTION] = value.reflectionEnabled
            it[Keys.VISUALIZER_MIRROR] = value.mirrorEnabled
        }
    }

    // ── Plan 5.5: user studio presets (save / restore / rename / delete /
    // reset) — tolerant decode, renderer stays blind to persistence. ────────

    val userVisualizerPresets: Flow<List<UserVisualizerPreset>> =
        context.dataStore.data.map { prefs ->
            decodeUserPresets(prefs[Keys.VISUALIZER_USER_PRESETS])
        }.flowOn(Dispatchers.IO)

    /** Save (or overwrite by id) one preset — atomic single edit. */
    suspend fun saveUserVisualizerPreset(preset: UserVisualizerPreset) {
        context.dataStore.edit { prefs ->
            val current = decodeUserPresets(prefs[Keys.VISUALIZER_USER_PRESETS])
            val next = current.filterNot { it.id == preset.id } + preset
            prefs[Keys.VISUALIZER_USER_PRESETS] = encodeUserPresets(next)
        }
    }

    suspend fun renameUserVisualizerPreset(id: String, name: String) {
        context.dataStore.edit { prefs ->
            val current = decodeUserPresets(prefs[Keys.VISUALIZER_USER_PRESETS])
            val next = current.map { if (it.id == id) it.copy(name = name) else it }
            prefs[Keys.VISUALIZER_USER_PRESETS] = encodeUserPresets(next)
        }
    }

    suspend fun deleteUserVisualizerPreset(id: String) {
        context.dataStore.edit { prefs ->
            val current = decodeUserPresets(prefs[Keys.VISUALIZER_USER_PRESETS])
            prefs[Keys.VISUALIZER_USER_PRESETS] = encodeUserPresets(current.filterNot { it.id == id })
        }
    }

    /** Reset the Lab to factory defaults (plan 5.5 "reset"). */
    suspend fun resetVisualizerPrefs() {
        setVisualizerPrefs(VisualizerPrefs())
    }

    suspend fun setGlassIntensity(value: GlassIntensityPref) {
        context.dataStore.edit { it[Keys.GLASS_INTENSITY] = value.name }
    }

    suspend fun setBubbleAnimationsEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.BUBBLE_ANIMATIONS] = value }
    }

    suspend fun setAccentSource(value: ThemeAccentSource) {
        context.dataStore.edit { it[Keys.ACCENT_SOURCE] = value.name }
    }

    suspend fun setGaplessPlayback(value: Boolean) {
        context.dataStore.edit { it[Keys.GAPLESS_PLAYBACK] = value }
    }

    suspend fun setCrossfadeDuration(ms: Int) {
        context.dataStore.edit { it[Keys.CROSSFADE_DURATION] = ms }
    }

    suspend fun setResumeOnHeadsetConnect(value: Boolean) {
        context.dataStore.edit { it[Keys.RESUME_ON_HEADSET] = value }
    }

    suspend fun setHapticFeedbackEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.HAPTIC_FEEDBACK] = value }
    }

    suspend fun setShowLyricsAutomatically(value: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_LYRICS_AUTO] = value }
    }

    suspend fun setMinSongDuration(seconds: Int) {
        context.dataStore.edit { it[Keys.MIN_SONG_DURATION] = seconds }
    }

    suspend fun setGridColumns(columns: Int) {
        context.dataStore.edit { it[Keys.GRID_COLUMNS] = columns }
    }

    suspend fun setMiniPlayerWaveformEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.MINI_PLAYER_WAVEFORM] = value }
    }

    suspend fun setHighQualityAlbumArt(value: Boolean) {
        context.dataStore.edit { it[Keys.HIGH_QUALITY_ART] = value }
    }

    suspend fun setPauseOnBluetoothDisconnect(value: Boolean) {
        context.dataStore.edit { it[Keys.PAUSE_ON_BT_DISCONNECT] = value }
    }

    suspend fun setVolumeNormalizationEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.VOLUME_NORMALIZATION] = value }
    }
}
