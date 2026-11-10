/*
 * RIKO EXPERIENCE OS — PRODUCTION-GRADE ROADMAP v2.0
 * Milestone 7 — Settings Experience
 *
 * File 2 / 8 — SettingsReducer.kt + Mapper + Coordinator
 * File 3 / 8 — SettingsHostCoordinator.kt
 * File 4 / 8 — SettingsViewModelBridge.kt + ScreenWiring
 * File 5 / 8 — SettingsRendering.kt
 * File 6 / 8 — SettingsPreviewKit.kt
 * File 7 / 8 — SettingsTestKit.kt + Production Gate
 * File 8 / 8 — SettingsSecuritySpec.kt (Dev mode gating)
 * --------------------------------------------------------------------
 * Combined to maintain cadence. Premium control room feel.
 * © RIKO Experience OS — Settings Tier
 */

package com.rikodev.music.android.presentation.settings_v2.runtime

import com.rikodev.music.android.presentation.settings_v2.contract.*

// ═══════════════════════════════════════════════════════════════════════════
// REDUCER
// ═══════════════════════════════════════════════════════════════════════════

object SettingsReducer {

    fun reduce(state: SettingsState, event: SettingsEvent): Pair<SettingsState, List<SettingsAction>> {
        return when (event) {
            is SettingsEvent.ThemeChanged -> state.copy(
                theme = event.theme,
                activeProfileId = null,
                lastActionDescription = "Theme: ${event.theme.displayName}",
            ) to listOf(SettingsAction.SetTheme(event.theme))

            is SettingsEvent.MotionIntensityChanged -> state.copy(
                motionIntensity = event.intensity,
                activeProfileId = null,
                lastActionDescription = "Motion: ${event.intensity.displayName}",
            ) to listOf(SettingsAction.SetMotionIntensity(event.intensity))

            is SettingsEvent.GlassIntensityChanged -> state.copy(
                glassIntensity = event.intensity,
                activeProfileId = null,
                lastActionDescription = "Glass: ${event.intensity.displayName}",
            ) to listOf(SettingsAction.SetGlassIntensity(event.intensity))

            is SettingsEvent.ColorBehaviorChanged -> state.copy(
                colorBehavior = event.behavior,
                activeProfileId = null,
                lastActionDescription = "Color: ${event.behavior.displayName}",
            ) to listOf(SettingsAction.SetColorBehavior(event.behavior))

            is SettingsEvent.MiniPlayerBehaviorChanged -> state.copy(
                miniPlayerBehavior = event.behavior,
                lastActionDescription = "Mini Player: ${event.behavior.displayName}",
            ) to listOf(SettingsAction.SetMiniPlayerBehavior(event.behavior))

            is SettingsEvent.AccessibilityChanged -> state.copy(
                accessibility = event.prefs,
                lastActionDescription = "Accessibility updated",
            ) to listOf(SettingsAction.SetAccessibility(event.prefs))

            is SettingsEvent.DiagnosticsChanged -> state.copy(
                diagnostics = event.toggle,
                lastActionDescription = "Diagnostics updated",
            ) to listOf(SettingsAction.SetDiagnostics(event.toggle))

            is SettingsEvent.ProfileApplied -> {
                val profile = SettingsProfilePreset.allBuiltIn.find { it.id == event.profileId }
                if (profile == null) {
                    state to emptyList()
                } else {
                    state.copy(
                        theme = profile.theme,
                        motionIntensity = profile.motionIntensity,
                        glassIntensity = profile.glassIntensity,
                        colorBehavior = profile.colorBehavior,
                        activeProfileId = profile.id,
                        lastActionDescription = "Profile: ${profile.displayName}",
                    ) to listOf(SettingsAction.ApplyProfile(profile.id))
                }
            }

            is SettingsEvent.DeveloperModeUnlocked -> {
                val newUnlockCount = state.developer.unlockCount + 1
                val shouldUnlock = newUnlockCount >= 7
                state.copy(
                    developer = state.developer.copy(
                        enabled = shouldUnlock,
                        unlockCount = newUnlockCount,
                    ),
                    lastActionDescription = if (shouldUnlock) "Developer mode unlocked" else "Tap ${7 - newUnlockCount} more times",
                ) to listOf(SettingsAction.UnlockDeveloperMode)
            }

            is SettingsEvent.DeveloperModeLocked -> state.copy(
                developer = DeveloperOptions.LOCKED,
                lastActionDescription = "Developer mode locked",
            ) to listOf(SettingsAction.LockDeveloperMode)

            is SettingsEvent.DeveloperOptionChanged -> {
                if (!state.developer.enabled) {
                    state to emptyList()
                } else {
                    val newDev = when (event.key) {
                        "showHiddenScreens" -> state.developer.copy(showHiddenScreens = event.value)
                        "enablePerformanceOverlay" -> state.developer.copy(enablePerformanceOverlay = event.value)
                        "forceReducedMotion" -> state.developer.copy(forceReducedMotion = event.value)
                        "forceLowEndProfile" -> state.developer.copy(forceLowEndProfile = event.value)
                        "skipSplashScreen" -> state.developer.copy(skipSplashScreen = event.value)
                        "enableValidationGatesAuto" -> state.developer.copy(enableValidationGatesAuto = event.value)
                        else -> state.developer
                    }
                    state.copy(developer = newDev) to listOf(SettingsAction.SetDeveloperOption(event.key, event.value))
                }
            }

            is SettingsEvent.ResetToDefaults -> SettingsState.INITIAL.copy(
                developer = state.developer, // preserve dev unlock
                lastActionDescription = "Settings reset",
            ) to listOf(SettingsAction.ResetToDefaults)

            is SettingsEvent.AllDataCleared -> SettingsState.INITIAL.copy(
                lastActionDescription = "All data cleared",
            ) to listOf(SettingsAction.ClearAllData)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SCREEN COORDINATOR
// ═══════════════════════════════════════════════════════════════════════════

object SettingsScreenCoordinator {

    fun plan(state: SettingsState, actions: List<SettingsAction>): List<SettingsCoordinatorRequest> {
        val requests = mutableListOf<SettingsCoordinatorRequest>()
        actions.forEach { action ->
            when (action) {
                is SettingsAction.SetTheme -> {
                    requests += SettingsCoordinatorRequest.ApplyTheme(action.theme)
                    requests += SettingsCoordinatorRequest.SaveSettings(state)
                    requests += SettingsCoordinatorRequest.EmitHaptic
                }
                is SettingsAction.SetMotionIntensity -> {
                    requests += SettingsCoordinatorRequest.ApplyMotionIntensity(action.intensity)
                    requests += SettingsCoordinatorRequest.SaveSettings(state)
                }
                is SettingsAction.SetGlassIntensity -> {
                    requests += SettingsCoordinatorRequest.ApplyGlassIntensity(action.intensity)
                    requests += SettingsCoordinatorRequest.SaveSettings(state)
                }
                is SettingsAction.SetColorBehavior -> {
                    requests += SettingsCoordinatorRequest.ApplyColorBehavior(action.behavior)
                    requests += SettingsCoordinatorRequest.SaveSettings(state)
                }
                is SettingsAction.SetMiniPlayerBehavior -> {
                    requests += SettingsCoordinatorRequest.ApplyMiniPlayerBehavior(action.behavior)
                    requests += SettingsCoordinatorRequest.SaveSettings(state)
                }
                is SettingsAction.SetAccessibility -> {
                    requests += SettingsCoordinatorRequest.SaveSettings(state)
                }
                is SettingsAction.SetDiagnostics -> {
                    requests += SettingsCoordinatorRequest.SaveSettings(state)
                    requests += SettingsCoordinatorRequest.LogEvent("diagnostics_changed", mapOf(
                        "frame_rate" to action.toggle.showFrameRate,
                        "recomp_count" to action.toggle.showRecompositionCount,
                        "memory_usage" to action.toggle.showMemoryUsage,
                    ))
                }
                is SettingsAction.ApplyProfile -> {
                    val profile = SettingsProfilePreset.allBuiltIn.find { it.id == action.profileId }
                    if (profile != null) {
                        requests += SettingsCoordinatorRequest.ApplyTheme(profile.theme)
                        requests += SettingsCoordinatorRequest.ApplyMotionIntensity(profile.motionIntensity)
                        requests += SettingsCoordinatorRequest.ApplyGlassIntensity(profile.glassIntensity)
                        requests += SettingsCoordinatorRequest.ApplyColorBehavior(profile.colorBehavior)
                        requests += SettingsCoordinatorRequest.SaveSettings(state)
                        requests += SettingsCoordinatorRequest.EmitHaptic
                    }
                }
                is SettingsAction.UnlockDeveloperMode -> {
                    if (state.developer.enabled) {
                        requests += SettingsCoordinatorRequest.LogEvent("dev_mode_unlocked", emptyMap())
                        requests += SettingsCoordinatorRequest.ShowSnackbar("Developer mode unlocked")
                    }
                }
                is SettingsAction.LockDeveloperMode -> {
                    requests += SettingsCoordinatorRequest.SaveSettings(state)
                }
                is SettingsAction.SetDeveloperOption -> {
                    requests += SettingsCoordinatorRequest.SaveSettings(state)
                    requests += SettingsCoordinatorRequest.LogEvent("dev_option_changed", mapOf(
                        "key" to action.key,
                        "value" to action.value,
                    ))
                }
                is SettingsAction.ResetToDefaults -> {
                    requests += SettingsCoordinatorRequest.SaveSettings(state)
                    requests += SettingsCoordinatorRequest.ShowSnackbar("Settings reset")
                }
                is SettingsAction.ClearAllData -> {
                    requests += SettingsCoordinatorRequest.ClearAllData
                    requests += SettingsCoordinatorRequest.ShowSnackbar("All data cleared")
                }
            }
        }
        return requests
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// MAPPER
// ═══════════════════════════════════════════════════════════════════════════

data class SettingsUiState(
    val appearanceSection: AppearanceSectionUi,
    val motionSection: MotionSectionUi,
    val glassSection: GlassSectionUi,
    val colorSection: ColorSectionUi,
    val miniPlayerSection: MiniPlayerSectionUi,
    val accessibilitySection: AccessibilitySectionUi,
    val diagnosticsSection: DiagnosticsSectionUi,
    val profileSection: ProfileSectionUi,
    val developerSection: DeveloperSectionUi,
    val lastActionDescription: String?,
    val accessibilityLabel: String,
)

data class AppearanceSectionUi(val theme: String, val availableThemes: List<String>)
data class MotionSectionUi(val intensity: String, val availableIntensities: List<String>)
data class GlassSectionUi(val intensity: String, val availableIntensities: List<String>)
data class ColorSectionUi(val behavior: String, val availableBehaviors: List<String>)
data class MiniPlayerSectionUi(val behavior: String, val availableBehaviors: List<String>)
data class AccessibilitySectionUi(
    val highContrast: Boolean, val largeText: Boolean, val reduceMotion: Boolean,
    val disableHaptics: Boolean, val talkBackVerbose: Boolean,
)
data class DiagnosticsSectionUi(
    val showFrameRate: Boolean, val showRecompositionCount: Boolean,
    val showMemoryUsage: Boolean, val showWaveInspector: Boolean,
    val enableValidationGates: Boolean, val logLevel: String, val availableLogLevels: List<String>,
)
data class ProfileSectionUi(val activeProfileId: String?, val availableProfiles: List<ProfileOptionUi>)
data class ProfileOptionUi(val id: String, val displayName: String, val description: String)
data class DeveloperSectionUi(
    val enabled: Boolean, val unlockCount: Int, val tapsToUnlock: Int,
    val showHiddenScreens: Boolean, val enablePerformanceOverlay: Boolean,
    val forceReducedMotion: Boolean, val forceLowEndProfile: Boolean,
    val skipSplashScreen: Boolean, val enableValidationGatesAuto: Boolean,
)

object SettingsMapper {

    fun map(state: SettingsState): SettingsUiState {
        return SettingsUiState(
            appearanceSection = AppearanceSectionUi(
                theme = state.theme.displayName,
                availableThemes = ThemePreference.values().map { it.displayName },
            ),
            motionSection = MotionSectionUi(
                intensity = state.motionIntensity.displayName,
                availableIntensities = MotionIntensity.values().map { it.displayName },
            ),
            glassSection = GlassSectionUi(
                intensity = state.glassIntensity.displayName,
                availableIntensities = GlassIntensity.values().map { it.displayName },
            ),
            colorSection = ColorSectionUi(
                behavior = state.colorBehavior.displayName,
                availableBehaviors = ColorBehavior.values().map { it.displayName },
            ),
            miniPlayerSection = MiniPlayerSectionUi(
                behavior = state.miniPlayerBehavior.displayName,
                availableBehaviors = MiniPlayerBehavior.values().map { it.displayName },
            ),
            accessibilitySection = AccessibilitySectionUi(
                highContrast = state.accessibility.highContrast,
                largeText = state.accessibility.largeText,
                reduceMotion = state.accessibility.reduceMotion,
                disableHaptics = state.accessibility.disableHaptics,
                talkBackVerbose = state.accessibility.talkBackVerbose,
            ),
            diagnosticsSection = DiagnosticsSectionUi(
                showFrameRate = state.diagnostics.showFrameRate,
                showRecompositionCount = state.diagnostics.showRecompositionCount,
                showMemoryUsage = state.diagnostics.showMemoryUsage,
                showWaveInspector = state.diagnostics.showWaveInspector,
                enableValidationGates = state.diagnostics.enableValidationGates,
                logLevel = state.diagnostics.logLevel.displayName,
                availableLogLevels = LogLevel.values().map { it.displayName },
            ),
            profileSection = ProfileSectionUi(
                activeProfileId = state.activeProfileId,
                availableProfiles = SettingsProfilePreset.allBuiltIn.map {
                    ProfileOptionUi(it.id, it.displayName, it.description)
                },
            ),
            developerSection = DeveloperSectionUi(
                enabled = state.developer.enabled,
                unlockCount = state.developer.unlockCount,
                tapsToUnlock = (7 - state.developer.unlockCount).coerceAtLeast(0),
                showHiddenScreens = state.developer.showHiddenScreens,
                enablePerformanceOverlay = state.developer.enablePerformanceOverlay,
                forceReducedMotion = state.developer.forceReducedMotion,
                forceLowEndProfile = state.developer.forceLowEndProfile,
                skipSplashScreen = state.developer.skipSplashScreen,
                enableValidationGatesAuto = state.developer.enableValidationGatesAuto,
            ),
            lastActionDescription = state.lastActionDescription,
            accessibilityLabel = buildString {
                append("Settings")
                if (state.lastActionDescription != null) append(", ${state.lastActionDescription}")
            },
        )
    }
}
