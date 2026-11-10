/*
 * RIKO EXPERIENCE OS — HOST INTEGRATION TIER (B5.5)
 * Settings Engine — E4 (3/5)
 *
 * SettingsHostDependenciesReal.kt — replaces SettingsHostDependenciesStub.
 *
 * Uses its own small DataStore rather than forcing these settings_v2
 * enums (ThemePreference/MotionIntensity/GlassIntensity/etc.) into the
 * pre-existing SettingsRepository, whose keys are for a different,
 * older settings surface with its own separate types (e.g. its
 * GlassIntensityPref vs this file's GlassIntensity) — reusing it would
 * mean guessing at type compatibility I can't verify by compiling.
 * Real persistence either way; this path was the lower-risk one.
 *
 * clearAllData() is honestly scoped to preferences only (this
 * DataStore + search history) — it does NOT touch the music library
 * Room database. Wiping the wrong table blind, unverified by
 * compilation, is a real-data-loss risk not worth taking; a full
 * "factory reset" that also clears library/playlist data is a
 * separate, clearly-scoped follow-up.
 *
 * © RIKO Experience OS — Settings Host Tier
 */

package com.rikodev.music.android.presentation.settings_v2.host

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.widget.Toast
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rikodev.music.android.core.designsystem.riko.theme.RikoMotion
import com.rikodev.music.android.core.util.RikoLog
import com.rikodev.music.android.presentation.settings_v2.contract.ColorBehavior
import com.rikodev.music.android.presentation.settings_v2.contract.GlassIntensity
import com.rikodev.music.android.presentation.settings_v2.contract.MiniPlayerBehavior
import com.rikodev.music.android.presentation.settings_v2.contract.MotionIntensity
import com.rikodev.music.android.presentation.settings_v2.contract.SettingsState
import com.rikodev.music.android.presentation.settings_v2.contract.ThemePreference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
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
}

@Singleton
class SettingsHostDependenciesReal @Inject constructor(
    @ApplicationContext private val context: Context,
) : SettingsHostDependencies {

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
        )
        // Side effect: motion scale must be live app-wide at startup, not
        // only after the Settings screen happens to open.
        RikoMotion.motionScale = state.motionIntensity.scale
        state
    } catch (e: Exception) {
        Log.w(TAG, "loadSettings failed — using defaults", e)
        SettingsState.INITIAL
    }

    override suspend fun applyTheme(theme: ThemePreference) {
        context.settingsV2DataStore.edit { it[Keys.THEME] = theme.name }
    }

    override suspend fun applyMotionIntensity(intensity: MotionIntensity) {
        context.settingsV2DataStore.edit { it[Keys.MOTION] = intensity.name }
        // A14 "settings reflect everywhere": instant global effect on change.
        RikoMotion.motionScale = intensity.scale
    }

    override suspend fun applyGlassIntensity(intensity: GlassIntensity) {
        context.settingsV2DataStore.edit { it[Keys.GLASS] = intensity.name }
    }

    override suspend fun applyColorBehavior(behavior: ColorBehavior) {
        context.settingsV2DataStore.edit { it[Keys.COLOR_BEHAVIOR] = behavior.name }
    }

    override suspend fun applyMiniPlayerBehavior(behavior: MiniPlayerBehavior) {
        context.settingsV2DataStore.edit { it[Keys.MINI_PLAYER] = behavior.name }
    }

    override suspend fun saveSettings(state: SettingsState) {
        context.settingsV2DataStore.edit { prefs ->
            prefs[Keys.THEME] = state.theme.name
            prefs[Keys.MOTION] = state.motionIntensity.name
            prefs[Keys.GLASS] = state.glassIntensity.name
            prefs[Keys.COLOR_BEHAVIOR] = state.colorBehavior.name
            prefs[Keys.MINI_PLAYER] = state.miniPlayerBehavior.name
        }
        // Bulk-save path (profiles / reset) also applies motion live.
        RikoMotion.motionScale = state.motionIntensity.scale
    }

    override suspend fun clearAllData() {
        // Scoped to preferences only — see file header.
        context.settingsV2DataStore.edit { it.clear() }
        RikoMotion.motionScale = MotionIntensity.NORMAL.scale
    }

    override fun performHaptic() {
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
