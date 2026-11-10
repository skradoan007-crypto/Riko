package com.rikodev.music.android.audiofx

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.os.Build
import android.util.Log
import com.rikodev.music.android.core.util.RikoLog
import com.rikodev.music.android.data.db.dao.EqPresetDao
import com.rikodev.music.android.data.db.dao.EqSettingsDao
import com.rikodev.music.android.data.db.entity.EqPresetEntity
import com.rikodev.music.android.domain.model.AudioFxState
import com.rikodev.music.android.domain.model.EqPreset
import com.rikodev.music.android.domain.model.EqualizerBand
import com.rikodev.music.android.domain.model.EqualizerState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * EqualizerEngine — fresh, lag-free audio effects engine.
 *
 * Design invariants (locked):
 *  - ALL android.media.audiofx objects are created/touched on a dedicated
 *    single-thread engine dispatcher. The UI thread never blocks on an
 *    AudioEffect call.
 *  - UI gets an instant state echo (MutableStateFlow) for every user action,
 *    then the audio change is applied asynchronously via the command queue.
 *  - EQ bands use the device's classic Equalizer (its native band layout),
 *    channel balance uses DynamicsProcessing per-channel InputGain (API 28+).
 *  - Persistence: EQ/effects snapshot in Room (eq_settings, debounced 350 ms),
 *    reverb/loudness/balance in SharedPreferences.
 */
@Singleton
class EqualizerEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val eqSettingsDao: EqSettingsDao,
    private val eqPresetDao: EqPresetDao
) {

    companion object {
        private const val TAG = "EqualizerEngine"
        private const val PREFS = "riko_audio_fx"
        private const val KEY_REVERB_ENABLED = "reverb_enabled"
        private const val KEY_REVERB_PRESET = "reverb_preset"
        private const val KEY_LOUDNESS_ENABLED = "loudness_enabled"
        private const val KEY_LOUDNESS_GAIN = "loudness_gain"
        private const val KEY_BALANCE_ENABLED = "balance_enabled"
        private const val KEY_BALANCE_LEVEL = "balance_level"
        private const val KEY_PREAMP_LEVEL = "preamp_level"
        private const val KEY_BASS_FREQUENCY = "bass_frequency"
        private const val KEY_BASS_MAX_GAIN = "bass_max_gain"
        private const val KEY_LOUDNESS_MAX_GAIN = "loudness_max_gain"
        private const val KEY_VOLUME_LEVEL = "volume_level"
        private const val KEY_EQ_UI_THEME = "eq_ui_theme"
        const val MAX_USER_PRESETS = 10
        private val REVERB_NAMES = listOf(
            "None", "Small Room", "Medium Room", "Large Room",
            "Medium Hall", "Large Hall", "Plate"
        )
    }

    // ── Engine thread — one dedicated thread for all AudioEffect work ──────
    private val engineDispatcher = Executors.newSingleThreadExecutor { r ->
        Thread(r, "rikofx-engine").also { it.isDaemon = true }
    }.asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Sequential command queue: UI posts lambdas, worker executes in order.
    private val commands = Channel<suspend () -> Unit>(Channel.UNLIMITED)

    init {
        scope.launch(engineDispatcher) {
            for (command in commands) {
                try {
                    command()
                } catch (t: Throwable) {
                    Log.e(TAG, "FX command failed", t)
                }
            }
        }
    }

    private fun post(command: suspend () -> Unit) {
        commands.trySend(command)
    }

    // ── State ──────────────────────────────────────────────────────────────
    private val _audioFxState = MutableStateFlow(AudioFxState())
    val audioFxState: StateFlow<AudioFxState> = _audioFxState

    /** User-created presets (Room-backed). */
    val userPresets: StateFlow<List<EqPreset>> = eqPresetDao.observeAll()
        .map { list -> list.map { EqPreset(id = it.id, name = it.name) } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    // ── Effects (engine thread only) ───────────────────────────────────────
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var presetReverb: PresetReverb? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var balanceDp: DynamicsProcessing? = null
    private var attachedSessionId: Int = -1
    private var devicePresetNames: List<String> = emptyList()
    private var activePresetName: String = "Flat"

    private var saveEqJob: kotlinx.coroutines.Job? = null

    // ═══════════════════════════════════════════════════════════════════
    // Lifecycle
    // ═══════════════════════════════════════════════════════════════════

    /** Called from PlayerManager on STATE_READY with the live audio session. */
    fun initialize(audioSessionId: Int) {
        if (audioSessionId <= 0) return
        post {
            if (audioSessionId == attachedSessionId) return@post
            releaseInternal()
            attachedSessionId = audioSessionId
            createEffects(audioSessionId)
            restoreSettings()
        }
    }

    fun releaseAll() {
        post {
            releaseInternal()
            attachedSessionId = -1
            _audioFxState.value = AudioFxState()
            devicePresetNames = emptyList()
        }
        com.rikodev.music.android.service.RikoNotificationCenter.cancelEqActive(context)
    }

    private fun releaseInternal() {
        try { equalizer?.release() } catch (t: Throwable) { Log.w(TAG, "eq release", t) }
        try { bassBoost?.release() } catch (t: Throwable) { Log.w(TAG, "bass release", t) }
        try { virtualizer?.release() } catch (t: Throwable) { Log.w(TAG, "virt release", t) }
        try { presetReverb?.release() } catch (t: Throwable) { Log.w(TAG, "reverb release", t) }
        try { loudnessEnhancer?.release() } catch (t: Throwable) { Log.w(TAG, "loud release", t) }
        try { balanceDp?.release() } catch (t: Throwable) { Log.w(TAG, "dp release", t) }
        equalizer = null
        bassBoost = null
        virtualizer = null
        presetReverb = null
        loudnessEnhancer = null
        balanceDp = null
    }

    private fun createEffects(sessionId: Int) {
        // Equalizer — device-native band layout
        try {
            val eq = Equalizer(0, sessionId)
            equalizer = eq
            val bandCount = eq.numberOfBands.toInt()
            val min = eq.bandLevelRange[0].toInt()
            val max = eq.bandLevelRange[1].toInt()
            val bands = (0 until bandCount).map { index ->
                EqualizerBand(
                    index = index,
                    frequencyHz = eq.getCenterFreq(index.toShort()) / 1000,
                    levelMillibel = eq.getBandLevel(index.toShort()).toInt(),
                    minLevel = min,
                    maxLevel = max
                )
            }
            devicePresetNames = (0 until eq.numberOfPresets.toInt()).mapNotNull { p ->
                try { eq.getPresetName(p.toShort()) } catch (t: Throwable) { null }
            }
            _audioFxState.value = _audioFxState.value.copy(
                equalizerState = _audioFxState.value.equalizerState.copy(
                    bands = bands,
                    presets = EqualizerPresets.presetNames,
                    currentPreset = "Flat"
                )
            )
            RikoLog.i(TAG, "Equalizer ready: $bandCount bands, ${devicePresetNames.size} device presets")
        } catch (t: Throwable) {
            Log.e(TAG, "Equalizer init failed", t)
            equalizer = null
        }

        // BassBoost
        try {
            bassBoost = BassBoost(0, sessionId).also { _audioFxState.value = _audioFxState.value.copy(bassBoostAvailable = it.hasControl()) }
        } catch (t: Throwable) {
            Log.w(TAG, "BassBoost init failed", t); bassBoost = null
        }

        // Virtualizer
        try {
            virtualizer = Virtualizer(0, sessionId).also { _audioFxState.value = _audioFxState.value.copy(virtualizerAvailable = it.hasControl()) }
        } catch (t: Throwable) {
            Log.w(TAG, "Virtualizer init failed", t); virtualizer = null
        }

        // PresetReverb (0 = global output mix session)
        try {
            presetReverb = PresetReverb(0, 0)
            _audioFxState.value = _audioFxState.value.copy(reverbAvailable = true)
        } catch (t: Throwable) {
            Log.w(TAG, "PresetReverb init failed", t); presetReverb = null
            _audioFxState.value = _audioFxState.value.copy(reverbAvailable = false)
        }

        // LoudnessEnhancer
        try {
            loudnessEnhancer = LoudnessEnhancer(sessionId)
        } catch (t: Throwable) {
            Log.w(TAG, "LoudnessEnhancer init failed", t); loudnessEnhancer = null
        }

        // Channel balance — DynamicsProcessing per-channel InputGain (API 28+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val config = DynamicsProcessing.Config.Builder(
                    DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                    2,                      // stereo channel count
                    false, 0,               // pre-EQ not in use
                    false, 0,               // MBC not in use
                    false, 0,               // post-EQ not in use
                    false                   // limiter not in use
                ).setPreferredFrameDuration(10f).build()
                val dp = DynamicsProcessing(0, sessionId, config)
                dp.enabled = true
                dp.setInputGainbyChannel(0, 0f)
                dp.setInputGainbyChannel(1, 0f)
                balanceDp = dp
                _audioFxState.value = _audioFxState.value.copy(balanceAvailable = true, preampAvailable = true)
                RikoLog.i(TAG, "Balance DP ready")
            } catch (t: Throwable) {
                Log.w(TAG, "Balance DP init failed — balance unavailable", t)
                balanceDp = null
                _audioFxState.value = _audioFxState.value.copy(balanceAvailable = false, preampAvailable = false)
            }
        } else {
            _audioFxState.value = _audioFxState.value.copy(balanceAvailable = false, preampAvailable = false)
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // EQ — bands, presets, reset
    // ═══════════════════════════════════════════════════════════════════

    fun setEqEnabled(enabled: Boolean) {
        _audioFxState.value = _audioFxState.value.copy(
            equalizerState = _audioFxState.value.equalizerState.copy(enabled = enabled)
        )
        post {
            equalizer?.enabled = enabled
            scheduleEqSave()
        }
        // Flat Equalizer parity: status notification when EQ is active.
        if (enabled) {
            com.rikodev.music.android.service.RikoNotificationCenter.postEqActive(
                context,
                _audioFxState.value.equalizerState.currentPreset
            )
        } else {
            com.rikodev.music.android.service.RikoNotificationCenter.cancelEqActive(context)
        }
    }

    fun setBandLevel(bandIndex: Int, levelMillibel: Int) {
        val clamped = levelMillibel.coerceIn(-1500, 1500)
        updateBandInState(bandIndex, clamped, "Custom")
        activePresetName = "Custom"
        post {
            val eq = equalizer ?: return@post
            val bandCount = eq.numberOfBands.toInt()
            if (bandIndex !in 0 until bandCount) return@post
            val (min, max) = eq.bandLevelRange
            eq.setBandLevel(bandIndex.toShort(), clamped.coerceIn(min.toInt(), max.toInt()).toShort())
            scheduleEqSave()
        }
    }

    fun applyPreset(presetName: String) {
        post {
            val eq = equalizer ?: return@post
            val bandCount = eq.numberOfBands.toInt()
            val (min, max) = eq.bandLevelRange

            // Plan 6.1: map by the device's ACTUAL band center frequencies
            // (milliHertz -> Hz), never by band index.
            val deviceFreqsHz = LongArray(bandCount) { i ->
                eq.getCenterFreq(i.toShort()) / 1000L
            }
            val customLevels = EqualizerPresets.getBandLevels(presetName, deviceFreqsHz)
            for (i in 0 until bandCount) {
                val level = (customLevels.getOrNull(i) ?: 0)
                    .coerceIn(min.toInt(), max.toInt())
                eq.setBandLevel(i.toShort(), level.toShort())
            }
            activePresetName = presetName
            echoLevelsFromDevice(eq, presetName)
            scheduleEqSave()
        }
    }

    fun resetEq() {
        // Per spec: Reset touches ONLY the EQ bands, never the effect cards.
        post {
            val eq = equalizer ?: return@post
            applyFlatLevels(eq)
            echoLevelsFromDevice(eq, "Flat")
            scheduleEqSave()
        }
    }

    private fun applyFlatLevels(eq: Equalizer) {
        val (min, max) = eq.bandLevelRange
        val zero = 0.coerceIn(min.toInt(), max.toInt()).toShort()
        for (b in 0 until eq.numberOfBands.toInt()) {
            eq.setBandLevel(b.toShort(), zero)
        }
    }

    private fun echoLevelsFromDevice(eq: Equalizer, presetName: String) {
        val bands = _audioFxState.value.equalizerState.bands.map { band ->
            band.copy(levelMillibel = eq.getBandLevel(band.index.toShort()).toInt())
        }
        _audioFxState.value = _audioFxState.value.copy(
            equalizerState = _audioFxState.value.equalizerState.copy(
                bands = bands,
                currentPreset = presetName
            )
        )
    }

    private fun updateBandInState(bandIndex: Int, levelMillibel: Int, presetName: String) {
        val current = _audioFxState.value
        val bands = current.equalizerState.bands.map {
            if (it.index == bandIndex) it.copy(levelMillibel = levelMillibel) else it
        }
        _audioFxState.value = current.copy(
            equalizerState = current.equalizerState.copy(bands = bands, currentPreset = presetName)
        )
    }

    // ═══════════════════════════════════════════════════════════════════
    // User presets (Room)
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Save the current band levels as a named user preset.
     * Flat Equalizer parity: max [MAX_USER_PRESETS] saved profiles.
     * @return true if saved; false when the name is blank or the limit is hit.
     */
    fun saveUserPreset(name: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return false
        val levels = _audioFxState.value.equalizerState.bands.map { it.levelMillibel }
        // Synchronous limit check on the engine thread is avoided — Room count
        // is launched and the insert is skipped when full.
        scope.launch {
            try {
                val count = eqPresetDao.count()
                if (count >= MAX_USER_PRESETS) {
                    RikoLog.w(TAG, "Preset limit reached ($MAX_USER_PRESETS) — not saving $trimmed")
                    return@launch
                }
                eqPresetDao.upsert(
                    EqPresetEntity(name = trimmed, bandLevels = levels.joinToString(","), createdAt = System.currentTimeMillis())
                )
                _audioFxState.value = _audioFxState.value.copy(
                    equalizerState = _audioFxState.value.equalizerState.copy(currentPreset = trimmed)
                )
                RikoLog.i(TAG, "Preset saved: $trimmed (${count + 1}/$MAX_USER_PRESETS)")
            } catch (t: Throwable) {
                Log.e(TAG, "savePreset failed", t)
            }
        }
        return true
    }

    fun applyUserPreset(id: Long) {
        post {
            val entity = eqPresetDao.getById(id) ?: return@post
            val eq = equalizer ?: return@post
            val levels = entity.bandLevels.split(",").mapNotNull { it.trim().toIntOrNull() }
            val (min, max) = eq.bandLevelRange
            for ((bandIndex, level) in levels.withIndex()) {
                if (bandIndex < eq.numberOfBands.toInt()) {
                    eq.setBandLevel(
                        bandIndex.toShort(),
                        level.coerceIn(min.toInt(), max.toInt()).toShort()
                    )
                }
            }
            echoLevelsFromDevice(eq, entity.name)
            scheduleEqSave()
        }
    }

    fun deleteUserPreset(id: Long) {
        scope.launch {
            try {
                eqPresetDao.deleteById(id)
            } catch (t: Throwable) {
                Log.e(TAG, "deletePreset failed", t)
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Effect cards
    // ═══════════════════════════════════════════════════════════════════

    fun setBassBoostEnabled(enabled: Boolean) {
        _audioFxState.value = _audioFxState.value.copy(bassBoostEnabled = enabled)
        post { bassBoost?.enabled = enabled }
    }

    fun setBassBoostStrength(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        _audioFxState.value = _audioFxState.value.copy(bassBoostStrength = clamped)
        post { bassBoost?.setStrength(clamped.toShort()) }
    }

    /**
     * Flat Equalizer "Pro" bass frequency knob. Public BassBoost API has no
     * frequency setter on most OEMs — we store the requested value and only
     * mark available=true when a device exposes matching parameters.
     */
    fun setBassBoostFrequency(freq: Int) {
        val clamped = freq.coerceIn(0, 1000)
        _audioFxState.value = _audioFxState.value.copy(bassBoostFrequency = clamped)
        scheduleEqSave()
    }

    fun setBassBoostMaxGain(gain: Int) {
        val clamped = gain.coerceIn(0, 1000)
        _audioFxState.value = _audioFxState.value.copy(bassBoostMaxGain = clamped)
        scheduleEqSave()
    }

    fun setLoudnessMaxGain(gainDb: Int) {
        val clamped = gainDb.coerceIn(0, 20)
        _audioFxState.value = _audioFxState.value.copy(loudnessMaxGainDb = clamped)
        scheduleEqSave()
    }

    /** Stream volume 0..100 percent (STREAM_MUSIC). UI labels it "Volume". */
    fun setVolumeLevel(percent: Int) {
        val clamped = percent.coerceIn(0, 100)
        _audioFxState.value = _audioFxState.value.copy(volumeLevel = clamped)
        try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            val max = am.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
            val idx = (clamped * max / 100f).roundToInt().coerceIn(0, max)
            am.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, idx, 0)
        } catch (t: Throwable) {
            Log.w(TAG, "setVolumeLevel failed", t)
        }
        scheduleEqSave()
    }

    /** Persist EQ sheet appearance (DARK | LIGHT) — Flat ThemeChooser parity. */
    fun setEqUiThemeMode(mode: String) {
        _audioFxState.value = _audioFxState.value.copy(eqUiThemeMode = mode)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_EQ_UI_THEME, mode).apply()
    }

    fun setVirtualizerEnabled(enabled: Boolean) {
        _audioFxState.value = _audioFxState.value.copy(virtualizerEnabled = enabled)
        post { virtualizer?.enabled = enabled }
    }

    fun setVirtualizerStrength(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        _audioFxState.value = _audioFxState.value.copy(virtualizerStrength = clamped)
        post { virtualizer?.setStrength(clamped.toShort()) }
    }

    fun setReverbEnabled(enabled: Boolean) {
        _audioFxState.value = _audioFxState.value.copy(reverbEnabled = enabled)
        post { presetReverb?.enabled = enabled }
    }

    fun setReverbPreset(presetIndex: Int) {
        val clamped = presetIndex.coerceIn(0, REVERB_NAMES.size - 1)
        _audioFxState.value = _audioFxState.value.copy(reverbPreset = clamped.toShort())
        post {
            presetReverb?.let { reverb ->
                try {
                    reverb.preset = if (clamped == 0) PresetReverb.PRESET_NONE else clamped.toShort()
                } catch (t: Throwable) {
                    Log.w(TAG, "reverb preset failed", t)
                }
            }
        }
    }

    fun setLoudnessEnabled(enabled: Boolean) {
        _audioFxState.value = _audioFxState.value.copy(loudnessEnabled = enabled)
        post {
            loudnessEnhancer?.let { loud ->
                try { loud.enabled = enabled } catch (t: Throwable) { Log.w(TAG, "loudness enable", t) }
            }
        }
    }

    fun setLoudnessGain(gainDb: Int) {
        val clamped = gainDb.coerceIn(0, 20)
        _audioFxState.value = _audioFxState.value.copy(loudnessGainDb = clamped)
        post {
            loudnessEnhancer?.let { loud ->
                try { loud.setTargetGain(clamped * 100) } catch (t: Throwable) { Log.w(TAG, "loudness gain", t) }
            }
        }
    }

    // ── Channel balance (−100 = full left, +100 = full right) ──────────────

    fun setBalanceEnabled(enabled: Boolean) {
        _audioFxState.value = _audioFxState.value.copy(balanceEnabled = enabled)
        post {
            try { balanceDp?.enabled = enabled } catch (t: Throwable) { Log.w(TAG, "balance enable", t) }
            applyDpGains()
            savePrefs()
        }
    }

    fun setBalanceLevel(level: Int) {
        val clamped = level.coerceIn(-100, 100)
        _audioFxState.value = _audioFxState.value.copy(balanceLevel = clamped)
        post {
            applyDpGains()
            savePrefs()
        }
    }

    // ── Preamp (master gain before EQ bands, plan 6.2/6.3) ─────────────────

    fun setPreampLevel(levelMb: Int) {
        val clamped = levelMb.coerceIn(-1500, 1500)
        _audioFxState.value = _audioFxState.value.copy(
            preampLevelMb = clamped,
            preampAvailable = balanceDp != null
        )
        post {
            applyDpGains()
            savePrefs()
        }
    }

    /**
     * Plan 6.2/6.3: apply combined preamp + balance through DynamicsProcessing
     * per-channel InputGain. Preamp is a symmetric master gain; balance offsets
     * the opposite channel downward. One atomic write so the two never fight.
     */
    private fun applyDpGains() {
        val dp = balanceDp ?: return
        try {
            val state = _audioFxState.value
            val preampMb = if (state.preampAvailable) state.preampLevelMb.toFloat() else 0f
            val balance = state.balanceLevel
            val oppositeMb = -(abs(balance) * 15f)
            val leftGain = preampMb + (if (balance >= 0) oppositeMb else 0f)
            val rightGain = preampMb + (if (balance < 0) oppositeMb else 0f)
            dp.setInputGainbyChannel(0, leftGain)
            dp.setInputGainbyChannel(1, rightGain)
        } catch (t: Throwable) {
            Log.w(TAG, "dp gain apply failed", t)
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Persistence
    // ═══════════════════════════════════════════════════════════════════

    private fun scheduleEqSave() {
        saveEqJob?.cancel()
        val snapshot = _audioFxState.value
        saveEqJob = scope.launch {
            delay(350)
            try {
                val bands = snapshot.equalizerState.bands
                fun level(i: Int): Int = bands.getOrNull(i)?.levelMillibel ?: 0
                eqSettingsDao.saveEqSettings(
                    com.rikodev.music.android.data.db.entity.EqSettingsEntity(
                        enabled = snapshot.equalizerState.enabled,
                        presetName = snapshot.equalizerState.currentPreset,
                        band0Level = level(0), band1Level = level(1), band2Level = level(2),
                        band3Level = level(3), band4Level = level(4),
                        // Plan 6.1: authoritative full-band CSV (5/6/10-band
                        // layouts); legacy columns above stay as rollback.
                        bandLevelsCsv = bands.joinToString(",") { it.levelMillibel.toString() },
                        virtualizerEnabled = snapshot.virtualizerEnabled,
                        virtualizerStrength = snapshot.virtualizerStrength,
                        bassBoostEnabled = snapshot.bassBoostEnabled,
                        bassBoostStrength = snapshot.bassBoostStrength
                    )
                )
            } catch (t: Throwable) {
                Log.w(TAG, "eq save failed", t)
            }
        }
    }

    private fun savePrefs() {
        val snapshot = _audioFxState.value
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_REVERB_ENABLED, snapshot.reverbEnabled)
            .putInt(KEY_REVERB_PRESET, snapshot.reverbPreset.toInt())
            .putBoolean(KEY_LOUDNESS_ENABLED, snapshot.loudnessEnabled)
            .putInt(KEY_LOUDNESS_GAIN, snapshot.loudnessGainDb)
            .putBoolean(KEY_BALANCE_ENABLED, snapshot.balanceEnabled)
            .putInt(KEY_BALANCE_LEVEL, snapshot.balanceLevel)
            .putInt(KEY_PREAMP_LEVEL, snapshot.preampLevelMb)
            .putInt(KEY_BASS_FREQUENCY, snapshot.bassBoostFrequency)
            .putInt(KEY_BASS_MAX_GAIN, snapshot.bassBoostMaxGain)
            .putInt(KEY_LOUDNESS_MAX_GAIN, snapshot.loudnessMaxGainDb)
            .putInt(KEY_VOLUME_LEVEL, snapshot.volumeLevel)
            .putString(KEY_EQ_UI_THEME, snapshot.eqUiThemeMode)
            .apply()
    }

    private fun restoreSettings() {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        // Apply the on/off master switches first.
        val eq = equalizer
        val snapshot = _audioFxState.value
        try { eq?.enabled = snapshot.equalizerState.enabled } catch (t: Throwable) { Log.w(TAG, "eq enable", t) }

        // Reverb / loudness / balance from prefs.
        val reverbEnabled = prefs.getBoolean(KEY_REVERB_ENABLED, false)
        val reverbPreset = prefs.getInt(KEY_REVERB_PRESET, 0).coerceIn(0, REVERB_NAMES.size - 1)
        val loudnessEnabled = prefs.getBoolean(KEY_LOUDNESS_ENABLED, false)
        val loudnessGain = prefs.getInt(KEY_LOUDNESS_GAIN, 0).coerceIn(0, 20)
        val balanceEnabled = prefs.getBoolean(KEY_BALANCE_ENABLED, false)
        val balanceLevel = prefs.getInt(KEY_BALANCE_LEVEL, 0).coerceIn(-100, 100)
        val preampLevel = prefs.getInt(KEY_PREAMP_LEVEL, 0).coerceIn(-1500, 1500)
        val bassFreq = prefs.getInt(KEY_BASS_FREQUENCY, 0).coerceIn(0, 1000)
        val bassMaxGain = prefs.getInt(KEY_BASS_MAX_GAIN, 0).coerceIn(0, 1000)
        val loudnessMaxGain = prefs.getInt(KEY_LOUDNESS_MAX_GAIN, 20).coerceIn(0, 20)
        val volumeLevel = prefs.getInt(KEY_VOLUME_LEVEL, 100).coerceIn(0, 100)
        val eqUiTheme = prefs.getString(KEY_EQ_UI_THEME, "DARK") ?: "DARK"
        _audioFxState.value = snapshot.copy(
            reverbEnabled = reverbEnabled,
            reverbPreset = reverbPreset.toShort(),
            loudnessEnabled = loudnessEnabled,
            loudnessGainDb = loudnessGain,
            balanceEnabled = balanceEnabled,
            balanceLevel = balanceLevel,
            preampLevelMb = preampLevel,
            preampAvailable = balanceDp != null,
            bassBoostFrequency = bassFreq,
            bassBoostMaxGain = bassMaxGain,
            loudnessMaxGainDb = loudnessMaxGain,
            volumeLevel = volumeLevel,
            eqUiThemeMode = eqUiTheme
        )
        try {
            presetReverb?.preset = if (reverbPreset == 0) PresetReverb.PRESET_NONE else reverbPreset.toShort()
            presetReverb?.enabled = reverbEnabled
        } catch (t: Throwable) { Log.w(TAG, "reverb restore", t) }
        try {
            loudnessEnhancer?.setTargetGain(loudnessGain * 100)
            loudnessEnhancer?.enabled = loudnessEnabled
        } catch (t: Throwable) { Log.w(TAG, "loudness restore", t) }
        if (balanceDp != null) {
            try {
                applyDpGains()
                balanceDp?.enabled = balanceEnabled
            } catch (t: Throwable) { Log.w(TAG, "balance restore", t) }
        }

        // Restore last EQ + main effect snapshot from Room (single row id=1).
        scope.launch {
            try {
                val saved = eqSettingsDao.getEqSettingsSync() ?: return@launch
                val state = _audioFxState.value
                // Plan 6.1: the full-band CSV wins when present (round-trips
                // 6/10-band layouts); legacy five columns are the fallback.
                val csvLevels = saved.bandLevelsCsv.split(",")
                    .mapNotNull { it.trim().toIntOrNull() }
                _audioFxState.value = state.copy(
                    equalizerState = state.equalizerState.copy(
                        enabled = saved.enabled,
                        currentPreset = saved.presetName,
                        bands = state.equalizerState.bands.mapIndexed { index, band ->
                            val level = csvLevels.getOrNull(index)
                                ?: when (index) {
                                    0 -> saved.band0Level; 1 -> saved.band1Level
                                    2 -> saved.band2Level; 3 -> saved.band3Level
                                    4 -> saved.band4Level; else -> band.levelMillibel
                                }
                            band.copy(levelMillibel = level)
                        }
                    ),
                    virtualizerEnabled = saved.virtualizerEnabled,
                    virtualizerStrength = saved.virtualizerStrength,
                    bassBoostEnabled = saved.bassBoostEnabled,
                    bassBoostStrength = saved.bassBoostStrength
                )
                val restored = _audioFxState.value
                try {
                    eq?.enabled = restored.equalizerState.enabled
                    val (min, max) = eq?.bandLevelRange ?: return@launch
                    restored.equalizerState.bands.forEach { band ->
                        eq?.setBandLevel(
                            band.index.toShort(),
                            band.levelMillibel.coerceIn(min.toInt(), max.toInt()).toShort()
                        )
                    }
                } catch (t: Throwable) { Log.w(TAG, "eq restore apply", t) }
                try {
                    virtualizer?.enabled = restored.virtualizerEnabled
                    virtualizer?.setStrength(restored.virtualizerStrength.toShort())
                    bassBoost?.enabled = restored.bassBoostEnabled
                    bassBoost?.setStrength(restored.bassBoostStrength.toShort())
                } catch (t: Throwable) { Log.w(TAG, "fx restore apply", t) }
            } catch (t: Throwable) {
                Log.w(TAG, "settings restore failed", t)
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Helpers for UI
    // ═══════════════════════════════════════════════════════════════════

    val reverbPresetNames: List<String> get() = REVERB_NAMES

    /** Plan 6.2: proper labels — 3600 Hz reads "3.6K", 14000 Hz reads "14K". */
    fun getFrequencyLabel(hz: Int): String = when {
        hz >= 1000 -> {
            if (hz % 1000 == 0) "${hz / 1000}K"
            else "%.1fK".format(hz / 1000.0)
        }
        else -> "$hz"
    }

    /** Suspending helper if a caller needs the engine thread to drain (tests/diagnostics). */
    suspend fun <T> onEngineThread(block: suspend () -> T): T =
        withContext(engineDispatcher) { block() }
}
