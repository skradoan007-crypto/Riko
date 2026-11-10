/*
 * RIKO EXPERIENCE OS — PRODUCTION-GRADE ROADMAP v2.0
 * Milestone 7 — Settings Experience
 *
 * File 1 / 8 — SettingsContract.kt
 * --------------------------------------------------------------------
 * Settings should not feel like a boring preference list.
 * It should feel like a premium control room.
 *
 * SCOPE
 * -----
 *   - Theme preference (system / light / dark / AMOLED)
 *   - Motion intensity (off / reduced / normal / expressive)
 *   - Glass intensity (off / light / normal / heavy)
 *   - Color behavior preferences
 *   - Mini player behavior preferences
 *   - Accessibility preferences
 *   - Diagnostics toggle
 *   - Developer options (gated — 7 taps pattern)
 *   - Profile presets
 *
 * RULES ENFORCED
 * --------------
 * v2.0 Security pillar — dev mode gated, no security risk in release
 *
 * © RIKO Experience OS — Settings Tier
 */

package com.rikodev.music.android.presentation.settings_v2.contract

// ═══════════════════════════════════════════════════════════════════════════
// THEME
// ═══════════════════════════════════════════════════════════════════════════

enum class ThemePreference(val displayName: String) {
    SYSTEM("Follow System"),
    LIGHT("Light"),
    DARK("Dark"),
    AMOLED("AMOLED Black"),
}

enum class MotionIntensity(val displayName: String, val scale: Float) {
    OFF("Off", 0.0f),
    REDUCED("Reduced", 0.4f),
    NORMAL("Normal", 1.0f),
    EXPRESSIVE("Expressive", 1.3f),
}

enum class GlassIntensity(val displayName: String, val blurRadius: Float) {
    OFF("Off", 0.0f),
    LIGHT("Light", 8.0f),
    NORMAL("Normal", 16.0f),
    HEAVY("Heavy", 24.0f),
}

enum class ColorBehavior(val displayName: String) {
    FOLLOW_ALBUM_ART("Follow Album Art"),
    ACCENT_ONLY("Accent Only"),
    STATIC("Static Theme"),
}

enum class MiniPlayerBehavior(val displayName: String) {
    ALWAYS_VISIBLE("Always Visible"),
    HIDE_ON_SCROLL("Hide on Scroll"),
    HIDE_IN_NOW_PLAYING("Hide in Now Playing"),
}

// ═══════════════════════════════════════════════════════════════════════════
// ACCESSIBILITY
// ═══════════════════════════════════════════════════════════════════════════

data class AccessibilityPreferences(
    val highContrast: Boolean,
    val largeText: Boolean,
    val reduceMotion: Boolean,
    val disableHaptics: Boolean,
    val talkBackVerbose: Boolean,
) {
    companion object {
        val DEFAULT = AccessibilityPreferences(
            highContrast = false,
            largeText = false,
            reduceMotion = false,
            disableHaptics = false,
            talkBackVerbose = false,
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// DIAGNOSTICS
// ═══════════════════════════════════════════════════════════════════════════

data class DiagnosticsToggle(
    val showFrameRate: Boolean,
    val showRecompositionCount: Boolean,
    val showMemoryUsage: Boolean,
    val showWaveInspector: Boolean,
    val enableValidationGates: Boolean,
    val logLevel: LogLevel,
) {
    companion object {
        val DEFAULT = DiagnosticsToggle(
            showFrameRate = false,
            showRecompositionCount = false,
            showMemoryUsage = false,
            showWaveInspector = false,
            enableValidationGates = false,
            logLevel = LogLevel.WARNING,
        )
    }
}

enum class LogLevel(val displayName: String) {
    NONE("None"),
    ERROR("Error"),
    WARNING("Warning"),
    INFO("Info"),
    DEBUG("Debug"),
    VERBOSE("Verbose"),
}

// ═══════════════════════════════════════════════════════════════════════════
// DEVELOPER OPTIONS (gated)
// ═══════════════════════════════════════════════════════════════════════════

data class DeveloperOptions(
    val enabled: Boolean,
    val showHiddenScreens: Boolean,
    val enablePerformanceOverlay: Boolean,
    val forceReducedMotion: Boolean,
    val forceLowEndProfile: Boolean,
    val skipSplashScreen: Boolean,
    val enableValidationGatesAuto: Boolean,
    val unlockCount: Int,
) {
    companion object {
        val LOCKED = DeveloperOptions(
            enabled = false,
            showHiddenScreens = false,
            enablePerformanceOverlay = false,
            forceReducedMotion = false,
            forceLowEndProfile = false,
            skipSplashScreen = false,
            enableValidationGatesAuto = false,
            unlockCount = 0,
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// PROFILE PRESETS
// ═══════════════════════════════════════════════════════════════════════════

data class SettingsProfilePreset(
    val id: String,
    val displayName: String,
    val description: String,
    val theme: ThemePreference,
    val motionIntensity: MotionIntensity,
    val glassIntensity: GlassIntensity,
    val colorBehavior: ColorBehavior,
) {
    companion object {
        val DEFAULT = SettingsProfilePreset(
            id = "default",
            displayName = "RIKO Default",
            description = "Balanced experience for most users",
            theme = ThemePreference.SYSTEM,
            motionIntensity = MotionIntensity.NORMAL,
            glassIntensity = GlassIntensity.NORMAL,
            colorBehavior = ColorBehavior.FOLLOW_ALBUM_ART,
        )

        val CINEMATIC = SettingsProfilePreset(
            id = "cinematic",
            displayName = "Cinematic",
            description = "Expressive motion, heavy glass, dark theme",
            theme = ThemePreference.DARK,
            motionIntensity = MotionIntensity.EXPRESSIVE,
            glassIntensity = GlassIntensity.HEAVY,
            colorBehavior = ColorBehavior.FOLLOW_ALBUM_ART,
        )

        val BATTERY_SAVER = SettingsProfilePreset(
            id = "battery_saver",
            displayName = "Battery Saver",
            description = "Reduced motion, no glass, AMOLED theme",
            theme = ThemePreference.AMOLED,
            motionIntensity = MotionIntensity.REDUCED,
            glassIntensity = GlassIntensity.OFF,
            colorBehavior = ColorBehavior.STATIC,
        )

        val ACCESSIBILITY = SettingsProfilePreset(
            id = "accessibility",
            displayName = "Accessibility",
            description = "High contrast, no motion, large text",
            theme = ThemePreference.DARK,
            motionIntensity = MotionIntensity.OFF,
            glassIntensity = GlassIntensity.OFF,
            colorBehavior = ColorBehavior.STATIC,
        )

        val allBuiltIn: List<SettingsProfilePreset> = listOf(DEFAULT, CINEMATIC, BATTERY_SAVER, ACCESSIBILITY)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SETTINGS STATE
// ═══════════════════════════════════════════════════════════════════════════

data class SettingsState(
    val theme: ThemePreference,
    val motionIntensity: MotionIntensity,
    val glassIntensity: GlassIntensity,
    val colorBehavior: ColorBehavior,
    val miniPlayerBehavior: MiniPlayerBehavior,
    val accessibility: AccessibilityPreferences,
    val diagnostics: DiagnosticsToggle,
    val developer: DeveloperOptions,
    val activeProfileId: String?,
    val isProcessing: Boolean,
    val lastActionDescription: String?,
) {
    companion object {
        val INITIAL = SettingsState(
            theme = ThemePreference.SYSTEM,
            motionIntensity = MotionIntensity.NORMAL,
            glassIntensity = GlassIntensity.NORMAL,
            colorBehavior = ColorBehavior.FOLLOW_ALBUM_ART,
            miniPlayerBehavior = MiniPlayerBehavior.ALWAYS_VISIBLE,
            accessibility = AccessibilityPreferences.DEFAULT,
            diagnostics = DiagnosticsToggle.DEFAULT,
            developer = DeveloperOptions.LOCKED,
            activeProfileId = null,
            isProcessing = false,
            lastActionDescription = null,
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// ACTION / EVENT / REQUEST
// ═══════════════════════════════════════════════════════════════════════════

sealed class SettingsAction {
    data class SetTheme(val theme: ThemePreference) : SettingsAction()
    data class SetMotionIntensity(val intensity: MotionIntensity) : SettingsAction()
    data class SetGlassIntensity(val intensity: GlassIntensity) : SettingsAction()
    data class SetColorBehavior(val behavior: ColorBehavior) : SettingsAction()
    data class SetMiniPlayerBehavior(val behavior: MiniPlayerBehavior) : SettingsAction()
    data class SetAccessibility(val prefs: AccessibilityPreferences) : SettingsAction()
    data class SetDiagnostics(val toggle: DiagnosticsToggle) : SettingsAction()
    data class ApplyProfile(val profileId: String) : SettingsAction()
    data object UnlockDeveloperMode : SettingsAction()
    data object LockDeveloperMode : SettingsAction()
    data class SetDeveloperOption(val key: String, val value: Boolean) : SettingsAction()
    data object ResetToDefaults : SettingsAction()
    data object ClearAllData : SettingsAction()
}

sealed class SettingsEvent {
    data class ThemeChanged(val theme: ThemePreference) : SettingsEvent()
    data class MotionIntensityChanged(val intensity: MotionIntensity) : SettingsEvent()
    data class GlassIntensityChanged(val intensity: GlassIntensity) : SettingsEvent()
    data class ColorBehaviorChanged(val behavior: ColorBehavior) : SettingsEvent()
    data class MiniPlayerBehaviorChanged(val behavior: MiniPlayerBehavior) : SettingsEvent()
    data class AccessibilityChanged(val prefs: AccessibilityPreferences) : SettingsEvent()
    data class DiagnosticsChanged(val toggle: DiagnosticsToggle) : SettingsEvent()
    data class ProfileApplied(val profileId: String) : SettingsEvent()
    data object DeveloperModeUnlocked : SettingsEvent()
    data object DeveloperModeLocked : SettingsEvent()
    data class DeveloperOptionChanged(val key: String, val value: Boolean) : SettingsEvent()
    data object ResetToDefaults : SettingsEvent()
    data object AllDataCleared : SettingsEvent()
}

sealed class SettingsCoordinatorRequest {
    data class ApplyTheme(val theme: ThemePreference) : SettingsCoordinatorRequest()
    data class ApplyMotionIntensity(val intensity: MotionIntensity) : SettingsCoordinatorRequest()
    data class ApplyGlassIntensity(val intensity: GlassIntensity) : SettingsCoordinatorRequest()
    data class ApplyColorBehavior(val behavior: ColorBehavior) : SettingsCoordinatorRequest()
    data class ApplyMiniPlayerBehavior(val behavior: MiniPlayerBehavior) : SettingsCoordinatorRequest()
    data class SaveSettings(val state: SettingsState) : SettingsCoordinatorRequest()
    data object ClearAllData : SettingsCoordinatorRequest()
    data object EmitHaptic : SettingsCoordinatorRequest()
    data class ShowSnackbar(val message: String) : SettingsCoordinatorRequest()
    data class LogEvent(val event: String, val params: Map<String, Any?>) : SettingsCoordinatorRequest()
}
