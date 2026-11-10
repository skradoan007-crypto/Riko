package com.rikodev.music.android.dna.sync

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioDeviceCallback
import android.os.Handler
import android.os.Looper
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import com.rikodev.music.android.audiofx.PcmAudioProcessor
import com.rikodev.music.android.audiofx.RawSpectrumTap
import com.rikodev.music.android.audiofx.SpectrumBands
import com.rikodev.music.android.audiofx.visualizer.SpectrumConfig
import com.rikodev.music.android.dna.core.BandSource
import com.rikodev.music.android.dna.core.LatencyCompensatedSource
import com.rikodev.music.android.dna.core.LatencyProvider
import com.rikodev.music.android.dna.core.SpectrumHistoryRing
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

// ─────────────────────────────────────────────────────────────────────────────
// RIKO DNA (MDNA) — SYNC HUB (P1)
//
// Owns the audio-thread → render-thread pipeline:
//
//   PcmAudioProcessor (audio thread, ~60Hz, UNdelayed)
//        │  RawSpectrumTap.onFrame
//        ▼
//   SpectrumHistoryRing  ──►  LatencyCompensatedSource (render thread, every vsync)
//                                   asks for the spectrum at  now − latency
//
// LATENCY MODEL (honest version): the tap runs ahead of the speaker by the
// output latency. Media3 exposes NO continuous output-latency reading, so
// latency = a base estimate (the project's existing AV_SYNC_DELAY_MS, so MDNA
// lines up with the other visualizers) + a USER OFFSET that is remembered
// separately for built-in/wired vs Bluetooth output (Bluetooth lag is large
// and differs per headset — only the listener can calibrate it).
//
// The tap is attached ONLY while an MDNA surface is on screen (ref-counted), so
// every other screen pays nothing.
// ─────────────────────────────────────────────────────────────────────────────
@Singleton
class MdnaSyncHub @Inject constructor(
    private val pcm: PcmAudioProcessor,
    private val player: ExoPlayer,
    @ApplicationContext private val context: Context
) : RawSpectrumTap, LatencyProvider {

    val ring = SpectrumHistoryRing(SpectrumBands.COUNT)

    /** The production [BandSource]: history ring + latency compensation. */
    private val compensated = LatencyCompensatedSource(ring, this)
    val source: BandSource = object : BandSource {
        override val bandCount = SpectrumBands.COUNT
        override fun sample(nowNanos: Long, out: FloatArray): Boolean {
            if (!playing) { out.fill(0f); return false }
            return compensated.sample(nowNanos, out)
        }
    }
    @Volatile var playing = false
        private set
    @Volatile var positionMs = 0L
        private set
    @Volatile var playbackSpeed = 1f
        private set
    @Volatile var lastFrameNanos = 0L
        private set
    @Volatile var framesReceived = 0L
        private set
    val sampleRate: Int get() = pcm.diagnostics.value.sampleRate
    private val handler = Handler(Looper.getMainLooper())
    private val audioManager get() = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val clockUpdate = object : Runnable {
        override fun run() {
            positionMs = player.currentPosition
            playbackSpeed = player.playbackParameters.speed
            if (attached.get() > 0) handler.postDelayed(this, 100L)
        }
    }
    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.contains(Player.EVENT_POSITION_DISCONTINUITY) ||
                events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION) ||
                events.contains(Player.EVENT_PLAYBACK_PARAMETERS_CHANGED) ||
                events.contains(Player.EVENT_IS_PLAYING_CHANGED)) {
                onReset()
            }
            playing = player.isPlaying
            positionMs = player.currentPosition
            playbackSpeed = player.playbackParameters.speed
        }
    }
    private val routeListener = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) = routeChanged()
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) = routeChanged()
    }
    private fun routeChanged() {
        bluetooth = isBluetoothOutput()
        onReset()
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    private val attached = AtomicInteger(0)

    @Volatile private var loaded = false
    @Volatile private var bluetooth = false
    @Volatile private var offsetBuiltinMs = 0
    @Volatile private var offsetBluetoothMs = 0

    /** Base estimate shared with the other visualizers (ms). */
    val baseLatencyMs: Int get() = SpectrumConfig.AV_SYNC_DELAY_MS.toInt()

    /** The user's correction for the CURRENT output kind (ms, may be negative). */
    val offsetMs: Int
        get() {
            ensureLoaded()
            return if (bluetooth) offsetBluetoothMs else offsetBuiltinMs
        }

    val outputLabel: String get() = if (bluetooth) "Bluetooth connected (estimate)" else "Speaker / wired (estimate)"

    override fun latencyNanos(): Long = (baseLatencyMs + offsetMs).coerceAtLeast(0) * 1_000_000L

    fun setOffsetMs(ms: Int) {
        ensureLoaded()
        val v = ms.coerceIn(MIN_OFFSET_MS, MAX_OFFSET_MS)
        if (bluetooth) {
            offsetBluetoothMs = v
            prefs.edit().putInt(KEY_BLUETOOTH, v).apply()
        } else {
            offsetBuiltinMs = v
            prefs.edit().putInt(KEY_BUILTIN, v).apply()
        }
    }

    /** Ref-counted: every on-screen MDNA surface calls attach() / detach() once. */
    fun attach() {
        ensureLoaded()
        bluetooth = isBluetoothOutput()
        check(Looper.myLooper() == Looper.getMainLooper())
        if (attached.getAndIncrement() == 0) {
            onReset()
            playing = player.isPlaying
            player.addListener(playerListener)
            audioManager.registerAudioDeviceCallback(routeListener, handler)
            handler.post(clockUpdate)
            pcm.rawTap = this
        }
    }

    fun detach() {
        val remaining = attached.updateAndGet { if (it > 0) it - 1 else 0 }
        if (remaining == 0) {
            if (pcm.rawTap === this) pcm.rawTap = null
            playing = false
            player.removeListener(playerListener)
            audioManager.unregisterAudioDeviceCallback(routeListener)
            handler.removeCallbacks(clockUpdate)
            onReset()
        }
    }

    // ── RawSpectrumTap (AUDIO THREAD — allocation-free, never throws) ──

    override fun onFrame(levels: FloatArray, timestampNanos: Long) {
        ring.publish(levels, timestampNanos)
        lastFrameNanos = timestampNanos
        framesReceived++
    }

    override fun onReset() {
        ring.markReset(System.nanoTime())
    }

    // ── internals ──

    private fun ensureLoaded() {
        if (loaded) return
        offsetBuiltinMs = prefs.getInt(KEY_BUILTIN, 0)
        offsetBluetoothMs = prefs.getInt(KEY_BLUETOOTH, 0)
        loaded = true
    }

    private fun isBluetoothOutput(): Boolean {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        return am.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any { d ->
            d.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    (d.type == AudioDeviceInfo.TYPE_BLE_HEADSET || d.type == AudioDeviceInfo.TYPE_BLE_SPEAKER))
        }
    }

    companion object {
        const val MIN_OFFSET_MS = -150
        const val MAX_OFFSET_MS = 400
        private const val PREFS_NAME = "mdna_sync"
        private const val KEY_BUILTIN = "offset_builtin_ms"
        private const val KEY_BLUETOOTH = "offset_bluetooth_ms"
    }
}
