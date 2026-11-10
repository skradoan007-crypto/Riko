package com.rikodev.music.android.dna.sync

import android.content.Context
import android.content.SharedPreferences
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
    @ApplicationContext private val context: Context
) : RawSpectrumTap, LatencyProvider {

    val ring = SpectrumHistoryRing(SpectrumBands.COUNT)

    /** The production [BandSource]: history ring + latency compensation. */
    val source: BandSource = LatencyCompensatedSource(ring, this)

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

    val outputLabel: String get() = if (bluetooth) "Bluetooth" else "Speaker / wired"

    override fun latencyNanos(): Long = (baseLatencyMs + offsetMs) * 1_000_000L

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
        if (attached.getAndIncrement() == 0) pcm.rawTap = this
    }

    fun detach() {
        val remaining = attached.updateAndGet { if (it > 0) it - 1 else 0 }
        if (remaining == 0 && pcm.rawTap === this) pcm.rawTap = null
    }

    // ── RawSpectrumTap (AUDIO THREAD — allocation-free, never throws) ──

    override fun onFrame(levels: FloatArray, timestampNanos: Long) {
        ring.publish(levels, timestampNanos)
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
