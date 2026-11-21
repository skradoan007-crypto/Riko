package com.rikodev.music.android.presentation.settings_v2.host

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.widget.Toast
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rikodev.music.android.core.designsystem.riko.theme.RikoMotion
import com.rikodev.music.android.core.util.RikoLog
import com.rikodev.music.android.presentation.settings_v2.contract.AccessibilityPreferences
import com.rikodev.music.android.presentation.settings_v2.contract.ColorBehavior
import com.rikodev.music.android.presentation.settings_v2.contract.DeveloperOptions
import com.rikodev.music.android.presentation.settings_v2.contract.DiagnosticsToggle
import com.rikodev.music.android.presentation.settings_v2.contract.GlassIntensity
import com.rikodev.music.android.presentation.settings_v2.contract.LogLevel
import com.rikodev.music.android.presentation.settings_v2.contract.MiniPlayerBehavior
import com.rikodev.music.android.presentation.settings_v2.contract.MotionIntensity
import com.rikodev.music.android.presentation.settings_v2.contract.SettingsState
import com.rikodev.music.android.presentation.settings_v2.contract.ThemePreference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsV2DataStore by preferencesDataStore(name = "riko_settings_v2")
private const val TAG = "RikoSettingsHost"

private object Keys {
    val THEME = stringPreferencesKey("theme")
    val MOTION = stringPreferencesKey("motion_intensity")
    val GLASS = stringPreferencesKey("glass_intensity")
    val COLOR_BEHAVIOR = stringPreferencesKey("color_behavior")
    val MINI_PLAYER = stringPreferencesKey("mini_player_behavior")

    // Accessibility
    val HIGH_CONTRAST = booleanPreferencesKey("acc_high_contrast")
    val LARGE_TEXT = booleanPreferencesKey("acc_large_text")
    val REDUCE_MOTION = booleanPreferencesKey("acc_reduce_motion")
    val DISABLE_HAPTICS = booleanPreferencesKey("acc_disable_haptics")
    val TALKBACK_VERBOSE = booleanPreferencesKey("acc_talkback_verbose")

    // Diagnostics
    val SHOW_FRAME_RATE = booleanPreferencesKey("diag_frame_rate")
    val SHOW_RECOMP_COUNT = booleanPreferencesKey("diag_recomp_count")
    val SHOW_MEMORY_USAGE = booleanPreferencesKey("diag_memory_usage")
    val SHOW_WAVE_INSPECTOR = booleanPreferencesKey("diag_wave_inspector")
    val ENABLE_VALIDATION_GATES = booleanPreferencesKey("diag_validation_gates")
    val LOG_LEVEL = stringPreferencesKey("diag_log_level")

    // Developer options
    val DEV_ENABLED = booleanPreferencesKey("dev_enabled")
    val DEV_HIDDEN_SCREENS = booleanPreferencesKey("dev_hidden_screens")
    val DEV_PERF_OVERLAY = booleanPreferencesKey("dev_perf_overlay")
    val DEV_FORCE_REDUCED_MOTION = booleanPreferencesKey("dev_force_reduced_motion")
    val DEV_FORCE_LOW_END = booleanPreferencesKey("dev_force_low_end")
    val DEV_SKIP_SPLASH = booleanPreferencesKey("dev_skip_splash")
    val DEV_AUTO_GATES = booleanPreferencesKey("dev_auto_gates")
}

@Singleton
class SettingsHostDependenciesReal @Inject constructor(
    @ApplicationContext private val context: Context,
) : SettingsHostDependencies {

    private val _settingsState = MutableStateFlow(SettingsState.INITIAL)
    override val settingsState: StateFlow<SettingsState> = _settingsState.asStateFlow()

    private fun syncRuntimeEffects(state: SettingsState) {
        _settingsState.value = state
        RikoMotion.motionScale = when {
            state.accessibility.reduceMotion || state.developer.forceReducedMotion -> 0.0f
            else -> state.motionIntensity.scale
        }
    }

    override suspend fun loadSettings(): SettingsState = try {
        val prefs = context.settingsV2DataStore.data.first()
        fun <T : Enum<T>> pick(raw: String?, vararg options: T, fallback: T): T =
            if (raw == null) fallback else options.firstOrNull { it.name == raw } ?: fallback

        val state = SettingsState.INITIAL.copy(
            theme = pick(prefs[Keys.THEME], *ThemePreference.values(), fallback = SettingsState.INITIAL.theme),
            motionIntensity = pick(prefs[Keys.MOTION], *MotionIntensity.values(), fallback = SettingsState.INITIAL.motionIntensity),
            glassIntensity = pick(prefs[Keys.GLASS], *GlassIntensity.values(), fallback = SettingsState.INITIAL.glassIntensity),
            colorBehavior = pick(prefs[Keys.COLOR_BEHAVIOR], *ColorBehavior.values(), fallback = SettingsState.INITIAL.colorBehavior),
            miniPlayerBehavior = pick(prefs[Keys.MINI_PLAYER], *MiniPlayerBehavior.values(), fallback = SettingsState.INITIAL.miniPlayerBehavior),
            accessibility = AccessibilityPreferences(
                highContrast = prefs[Keys.HIGH_CONTRAST] ?: false,
                largeText = prefs[Keys.LARGE_TEXT] ?: false,
                reduceMotion = prefs[Keys.REDUCE_MOTION] ?: false,
                disableHaptics = prefs[Keys.DISABLE_HAPTICS] ?: false,
                talkBackVerbose = prefs[Keys.TALKBACK_VERBOSE] ?: false,
            ),
            diagnostics = DiagnosticsToggle(
                showFrameRate = prefs[Keys.SHOW_FRAME_RATE] ?: false,
                showRecompositionCount = prefs[Keys.SHOW_RECOMP_COUNT] ?: false,
                showMemoryUsage = prefs[Keys.SHOW_MEMORY_USAGE] ?: false,
                showWaveInspector = prefs[Keys.SHOW_WAVE_INSPECTOR] ?: false,
                enableValidationGates = prefs[Keys.ENABLE_VALIDATION_GATES] ?: false,
                logLevel = pick(prefs[Keys.LOG_LEVEL], *LogLevel.values(), fallback = LogLevel.WARNING),
            ),
            developer = DeveloperOptions(
                enabled = prefs[Keys.DEV_ENABLED] ?: false,
                showHiddenScreens = prefs[Keys.DEV_HIDDEN_SCREENS] ?: false,
                enablePerformanceOverlay = prefs[Keys.DEV_PERF_OVERLAY] ?: false,
                forceReducedMotion = prefs[Keys.DEV_FORCE_REDUCED_MOTION] ?: false,
                forceLowEndProfile = prefs[Keys.DEV_FORCE_LOW_END] ?: false,
                skipSplashScreen = prefs[Keys.DEV_SKIP_SPLASH] ?: false,
                enableValidationGatesAuto = prefs[Keys.DEV_AUTO_GATES] ?: false,
                unlockCount = if (prefs[Keys.DEV_ENABLED] == true) 7 else 0,
            )
        )
        syncRuntimeEffects(state)
        state
    } catch (e: Exception) {
        Log.w(TAG, "loadSettings failed — using defaults", e)
        syncRuntimeEffects(SettingsState.INITIAL)
        SettingsState.INITIAL
    }

    override suspend fun applyTheme(theme: ThemePreference) {
        syncRuntimeEffects(_settingsState.value.copy(theme = theme))
        context.settingsV2DataStore.edit { it[Keys.THEME] = theme.name }
    }

    override suspend fun applyMotionIntensity(intensity: MotionIntensity) {
        syncRuntimeEffects(_settingsState.value.copy(motionIntensity = intensity))
        context.settingsV2DataStore.edit { it[Keys.MOTION] = intensity.name }
    }

    override suspend fun applyGlassIntensity(intensity: GlassIntensity) {
        syncRuntimeEffects(_settingsState.value.copy(glassIntensity = intensity))
        context.settingsV2DataStore.edit { it[Keys.GLASS] = intensity.name }
    }

    override suspend fun applyColorBehavior(behavior: ColorBehavior) {
        syncRuntimeEffects(_settingsState.value.copy(colorBehavior = behavior))
        context.settingsV2DataStore.edit { it[Keys.COLOR_BEHAVIOR] = behavior.name }
    }

    override suspend fun applyMiniPlayerBehavior(behavior: MiniPlayerBehavior) {
        syncRuntimeEffects(_settingsState.value.copy(miniPlayerBehavior = behavior))
        context.settingsV2DataStore.edit { it[Keys.MINI_PLAYER] = behavior.name }
    }

    override suspend fun saveSettings(state: SettingsState) {
        syncRuntimeEffects(state)
        context.settingsV2DataStore.edit { prefs ->
            prefs[Keys.THEME] = state.theme.name
            prefs[Keys.MOTION] = state.motionIntensity.name
            prefs[Keys.GLASS] = state.glassIntensity.name
            prefs[Keys.COLOR_BEHAVIOR] = state.colorBehavior.name
            prefs[Keys.MINI_PLAYER] = state.miniPlayerBehavior.name

            prefs[Keys.HIGH_CONTRAST] = state.accessibility.highContrast
            prefs[Keys.LARGE_TEXT] = state.accessibility.largeText
            prefs[Keys.REDUCE_MOTION] = state.accessibility.reduceMotion
            prefs[Keys.DISABLE_HAPTICS] = state.accessibility.disableHaptics
            prefs[Keys.TALKBACK_VERBOSE] = state.accessibility.talkBackVerbose

            prefs[Keys.SHOW_FRAME_RATE] = state.diagnostics.showFrameRate
            prefs[Keys.SHOW_RECOMP_COUNT] = state.diagnostics.showRecompositionCount
            prefs[Keys.SHOW_MEMORY_USAGE] = state.diagnostics.showMemoryUsage
            prefs[Keys.SHOW_WAVE_INSPECTOR] = state.diagnostics.showWaveInspector
            prefs[Keys.ENABLE_VALIDATION_GATES] = state.diagnostics.enableValidationGates
            prefs[Keys.LOG_LEVEL] = state.diagnostics.logLevel.name

            prefs[Keys.DEV_ENABLED] = state.developer.enabled
            prefs[Keys.DEV_HIDDEN_SCREENS] = state.developer.showHiddenScreens
            prefs[Keys.DEV_PERF_OVERLAY] = state.developer.enablePerformanceOverlay
            prefs[Keys.DEV_FORCE_REDUCED_MOTION] = state.developer.forceReducedMotion
            prefs[Keys.DEV_FORCE_LOW_END] = state.developer.forceLowEndProfile
            prefs[Keys.DEV_SKIP_SPLASH] = state.developer.skipSplashScreen
            prefs[Keys.DEV_AUTO_GATES] = state.developer.enableValidationGatesAuto
        }
    }

    override suspend fun clearAllData() {
        syncRuntimeEffects(SettingsState.INITIAL)
        context.settingsV2DataStore.edit { it.clear() }
    }

    override fun performHaptic() {
        if (_settingsState.value.accessibility.disableHaptics) return
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
                ?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        if (vibrator?.hasVibrator() == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(20)
            }
        }
    }

    override suspend fun showSnackbar(message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun logEvent(event: String, params: Map<String, Any?>) {
        RikoLog.d(TAG) {
            buildString {
                append(event)
                if (params.isNotEmpty()) {
                    append(" ")
                    append(params.entries.joinToString(", ") { (k, v) -> "$k=$v" })
                }
            }
        }
    }
}
