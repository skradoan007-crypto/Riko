package com.rikodev.music.android.domain.model

import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Immutable

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — DOMAIN MODELS
// Pure Kotlin data classes. No Android framework dependencies here.
// These flow through the entire app via StateFlow.
//
// PERF PASS (Priority 2): every model is @Immutable — all properties are
// final vals and the model objects are never mutated after construction.
// Without the annotation, Compose sees `Song` as unstable (it contains
// android.net.Uri) and every class that receives a Song (PlayerState, list
// rows, mini player) as unstable too — so LazyColumn rows and the mini
// player recomposed on EVERY parent recomposition even when the data was
// unchanged. The annotation is a truthful promise (val-only data classes,
// replaced by copy() on change) and lets those composables be skipped.
// ─────────────────────────────────────────────────────────────────────────────

@Immutable
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val duration: Long,           // milliseconds
    val size: Long,               // bytes
    val path: String,             // file path
    val uri: Uri,                 // content URI
    val dateAdded: Long,          // epoch ms
    val dateModified: Long,
    val trackNumber: Int,
    val year: Int,
    val bitrate: Int,             // kbps
    val sampleRate: Int,          // Hz
    val mimeType: String,
    val isFavorite: Boolean = false
) {
    // Computed ONCE per immutable Song (plan 1.3): the old `get()` formatted
    // per access — every list row recomposition allocated a Formatter.
    val durationFormatted: String = run {
        val totalSeconds = duration / 1000
        "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
    }

    val sizeFormatted: String = "%.1f MB".format(size / (1024.0 * 1024.0))

    val bitrateFormatted: String = "$bitrate kbps"
}

@Immutable
data class Playlist(
    val id: Long,
    val name: String,
    val description: String = "",
    val coverUri: Uri? = null,    // custom thumbnail picked from storage
    val songIds: List<Long> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val isFavorites: Boolean = false   // The built-in "Favorites" playlist
)

// ── Player State ──────────────────────────────────────────────────────────────

enum class RepeatMode { OFF, REPEAT_ALL, REPEAT_ONE }
enum class ShuffleMode { OFF, ON }

@Immutable
data class QueueItem(
    val queueIndex: Int,         // position in queue (1, 2, 3, ...)
    val song: Song
)

@Immutable
data class PlayerState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val position: Long = 0L,            // current position ms
    val duration: Long = 0L,            // total duration ms
    val bufferedPosition: Long = 0L,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val shuffleMode: ShuffleMode = ShuffleMode.OFF,
    val queue: List<QueueItem> = emptyList(),
    val currentQueueIndex: Int = 0,
    val playbackSpeed: Float = 1.0f,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val progress: Float
        get() = if (duration > 0) position.toFloat() / duration.toFloat() else 0f

    val progressFormatted: String
        get() {
            val totalSeconds = position / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }

    val hasNext: Boolean
        get() = currentQueueIndex < queue.size - 1 || repeatMode != RepeatMode.OFF

    val hasPrevious: Boolean
        get() = currentQueueIndex > 0 || position > 3000
}

// ── EQ State ──────────────────────────────────────────────────────────────────

@Immutable
data class EqualizerBand(
    val index: Int,
    val frequencyHz: Int,       // center frequency
    val levelMillibel: Int,     // current level (typically -1500 to +1500)
    val minLevel: Int,          // min millibel
    val maxLevel: Int           // max millibel
) {
    val levelDb: Float get() = levelMillibel / 100f    // convert to dB for display
}

@Immutable
data class EqualizerState(
    val enabled: Boolean = true,
    val bands: List<EqualizerBand> = emptyList(),
    val currentPreset: String = "Custom",
    val presets: List<String> = emptyList()
)

/** A user-created equalizer preset (Room-backed). */
@Immutable
data class EqPreset(
    val id: Long,
    val name: String
)

@Immutable
data class AudioFxState(
    val equalizerState: EqualizerState = EqualizerState(),
    val virtualizerStrength: Int = 0,      // 0..1000
    val virtualizerEnabled: Boolean = false,
    val virtualizerAvailable: Boolean = false,
    val bassBoostStrength: Int = 0,        // 0..1000
    val bassBoostEnabled: Boolean = false,
    val bassBoostAvailable: Boolean = false,
    val reverbPreset: Short = 0,           // PresetReverb preset index (0 = none)
    val reverbEnabled: Boolean = false,
    // Plan 6.2: honest unsupported state — PresetReverb may be absent.
    val reverbAvailable: Boolean = false,
    val loudnessEnabled: Boolean = false,
    val loudnessGainDb: Int = 0,           // LoudnessEnhancer gain, 0..20 dB
    val balanceEnabled: Boolean = false,   // Channel balance (L..R)
    val balanceLevel: Int = 0,             // -100 (full left) .. +100 (full right)
    val balanceAvailable: Boolean = false, // requires DynamicsProcessing (API 28+)
    // Plan 6.2/6.3: preamp — master gain before EQ bands (headroom management).
    val preampLevelMb: Int = 0,            // -1500..+1500 mB (−15..+15 dB), 0 = unity
    val preampAvailable: Boolean = false,  // requires DynamicsProcessing (API 28+)
    // Flat Equalizer parity: bass-pro knobs + loudness max gain + volume + theme.
    val bassBoostFrequency: Int = 0,       // 0..1000 arbitrary scale (OEM param)
    val bassBoostMaxGain: Int = 0,         // 0..1000 arbitrary scale (OEM param)
    val bassBoostFreqAvailable: Boolean = false,
    val bassBoostMaxGainAvailable: Boolean = false,
    val loudnessMaxGainDb: Int = 20,       // 0..20 dB cap for LoudnessEnhancer
    val volumeLevel: Int = 100,            // 0..100 stream volume percent
    val volumeAvailable: Boolean = true,
    val eqUiThemeMode: String = "DARK"     // EqUiThemeMode.name — DARK | LIGHT
)

// ── Dynamic Color State ───────────────────────────────────────────────────────

@Immutable
data class AlbumArtColors(
    val dominant: Int = 0xFF0A0A1A.toInt(),
    val vibrant: Int = 0xFF00D4FF.toInt(),
    val darkVibrant: Int = 0xFF0A0A1A.toInt(),
    val lightVibrant: Int = 0xFFCCF0FF.toInt(),
    val muted: Int = 0xFF2A2A4A.toInt(),
    val darkMuted: Int = 0xFF0F0F20.toInt()
)
