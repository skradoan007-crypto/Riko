package com.rikodev.music.android.di.stubs

import com.rikodev.music.android.presentation.settings_v2.host.SettingsHostDependencies
import com.rikodev.music.android.presentation.settings_v2.contract.SettingsState
import com.rikodev.music.android.presentation.settings_v2.contract.ThemePreference
import com.rikodev.music.android.presentation.settings_v2.contract.MotionIntensity
import com.rikodev.music.android.presentation.settings_v2.contract.GlassIntensity
import com.rikodev.music.android.presentation.settings_v2.contract.ColorBehavior
import com.rikodev.music.android.presentation.settings_v2.contract.MiniPlayerBehavior
import javax.inject.Singleton

@Singleton
class SettingsHostDependenciesStub @javax.inject.Inject constructor() : SettingsHostDependencies {
    override suspend fun loadSettings(): SettingsState = SettingsState.INITIAL
    override suspend fun applyTheme(theme: ThemePreference) {}
    override suspend fun applyMotionIntensity(intensity: MotionIntensity) {}
    override suspend fun applyGlassIntensity(intensity: GlassIntensity) {}
    override suspend fun applyColorBehavior(behavior: ColorBehavior) {}
    override suspend fun applyMiniPlayerBehavior(behavior: MiniPlayerBehavior) {}
    override suspend fun saveSettings(state: SettingsState) {}
    override suspend fun clearAllData() {}
    override fun performHaptic() {}
    override suspend fun showSnackbar(message: String) {}
    override fun logEvent(event: String, params: Map<String, Any?>) {}
}
