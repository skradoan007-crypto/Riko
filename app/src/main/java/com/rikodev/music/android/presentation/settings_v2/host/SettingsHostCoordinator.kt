/*
 * RIKO EXPERIENCE OS — PRODUCTION-GRADE ROADMAP v2.0
 * Milestone 7 — Settings Experience
 *
 * File 3 / 8 — SettingsHostCoordinator.kt + ViewModelBridge + ScreenWiring
 * File 4 / 8 — SettingsRendering.kt
 * File 5 / 8 — SettingsPreviewKit.kt
 * File 6 / 8 — SettingsTestKit.kt + Production Gate
 * File 7 / 8 — SettingsSecuritySpec.kt (dev mode gating)
 * File 8 / 8 — SettingsI18nSpec.kt
 * --------------------------------------------------------------------
 * Combined. Premium control room feel.
 * © RIKO Experience OS — Settings Tier
 */

package com.rikodev.music.android.presentation.settings_v2.host

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rikodev.music.android.presentation.settings_v2.contract.*
import com.rikodev.music.android.presentation.settings_v2.rendering.SettingsScreen
import com.rikodev.music.android.service.MusicPlaybackService
import com.rikodev.music.android.presentation.settings_v2.runtime.SettingsMapper
import com.rikodev.music.android.presentation.settings_v2.runtime.SettingsReducer
import com.rikodev.music.android.presentation.settings_v2.runtime.SettingsScreenCoordinator
import com.rikodev.music.android.presentation.settings_v2.runtime.SettingsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface SettingsHostDependencies {
    /**
     * Load persisted settings and re-apply their side effects (e.g. the
     * global RikoMotion.motionScale). Returns defaults on any failure —
     * the real impl logs; UI must never hard-fail on a preferences read.
     */
    suspend fun loadSettings(): SettingsState
    suspend fun applyTheme(theme: ThemePreference)
    suspend fun applyMotionIntensity(intensity: MotionIntensity)
    suspend fun applyGlassIntensity(intensity: GlassIntensity)
    suspend fun applyColorBehavior(behavior: ColorBehavior)
    suspend fun applyMiniPlayerBehavior(behavior: MiniPlayerBehavior)
    suspend fun saveSettings(state: SettingsState)
    suspend fun clearAllData()
    fun performHaptic()
    suspend fun showSnackbar(message: String)
    fun logEvent(event: String, params: Map<String, Any?>)
}

class SettingsHostCoordinator(
    private val dependencies: SettingsHostDependencies,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _events = MutableSharedFlow<SettingsEvent>(extraBufferCapacity = 64)

    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    fun execute(requests: List<SettingsCoordinatorRequest>) {
        requests.forEach { executeOne(it) }
    }

    private fun executeOne(request: SettingsCoordinatorRequest) {
        when (request) {
            is SettingsCoordinatorRequest.ApplyTheme -> scope.launch {
                try { withContext(Dispatchers.IO) { dependencies.applyTheme(request.theme) } }
                catch (e: Exception) { dependencies.logEvent("settings_theme_error", mapOf("error" to e::class.simpleName)) }
            }
            is SettingsCoordinatorRequest.ApplyMotionIntensity -> scope.launch {
                try { withContext(Dispatchers.IO) { dependencies.applyMotionIntensity(request.intensity) } }
                catch (_: Exception) {}
            }
            is SettingsCoordinatorRequest.ApplyGlassIntensity -> scope.launch {
                try { withContext(Dispatchers.IO) { dependencies.applyGlassIntensity(request.intensity) } }
                catch (_: Exception) {}
            }
            is SettingsCoordinatorRequest.ApplyColorBehavior -> scope.launch {
                try { withContext(Dispatchers.IO) { dependencies.applyColorBehavior(request.behavior) } }
                catch (_: Exception) {}
            }
            is SettingsCoordinatorRequest.ApplyMiniPlayerBehavior -> scope.launch {
                try { withContext(Dispatchers.IO) { dependencies.applyMiniPlayerBehavior(request.behavior) } }
                catch (_: Exception) {}
            }
            is SettingsCoordinatorRequest.SaveSettings -> scope.launch {
                try { withContext(Dispatchers.IO) { dependencies.saveSettings(request.state) } }
                catch (e: Exception) { dependencies.logEvent("settings_save_error", mapOf("error" to e::class.simpleName)) }
            }
            is SettingsCoordinatorRequest.ClearAllData -> scope.launch {
                try {
                    withContext(Dispatchers.IO) { dependencies.clearAllData() }
                    dependencies.logEvent("all_data_cleared", emptyMap())
                } catch (e: Exception) {
                    dependencies.logEvent("clear_data_error", mapOf("error" to e::class.simpleName))
                }
            }
            is SettingsCoordinatorRequest.EmitHaptic -> { try { dependencies.performHaptic() } catch (_: Exception) {} }
            is SettingsCoordinatorRequest.ShowSnackbar -> scope.launch {
                try { dependencies.showSnackbar(request.message) } catch (_: Exception) {}
            }
            is SettingsCoordinatorRequest.LogEvent -> { try { dependencies.logEvent(request.event, request.params) } catch (_: Exception) {} }
        }
    }

    fun release() { scope.cancel() }

    /** Load persisted settings (with side effects applied). Bridge + startup. */
    suspend fun load(): SettingsState = dependencies.loadSettings()
}

// ═══════════════════════════════════════════════════════════════════════════
// VIEW MODEL BRIDGE
// ═══════════════════════════════════════════════════════════════════════════

@HiltViewModel
class SettingsViewModelBridge @Inject constructor(
    private val hostCoordinator: SettingsHostCoordinator,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState.INITIAL)
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    private val _uiState = MutableStateFlow(SettingsMapper.map(_state.value))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            hostCoordinator.events.collect { processEvent(it) }
        }
        viewModelScope.launch {
            _state.collect { _uiState.value = SettingsMapper.map(it) }
        }
        // A14: restore persisted settings — was always SettingsState.INITIAL,
        // so every session silently showed defaults regardless of saved prefs.
        viewModelScope.launch {
            runCatching { hostCoordinator.load() }
                .onSuccess { saved ->
                    _state.value = saved
                    _uiState.value = SettingsMapper.map(saved)
                }
                .onFailure { e ->
                    hostCoordinator.execute(
                        listOf(
                            SettingsCoordinatorRequest.LogEvent(
                                "settings_load_error",
                                mapOf("error" to (e::class.simpleName ?: "unknown"))
                            )
                        )
                    )
                }
        }
    }

    fun onThemeChange(theme: ThemePreference) = processEvent(SettingsEvent.ThemeChanged(theme))
    fun onMotionIntensityChange(intensity: MotionIntensity) = processEvent(SettingsEvent.MotionIntensityChanged(intensity))
    fun onGlassIntensityChange(intensity: GlassIntensity) = processEvent(SettingsEvent.GlassIntensityChanged(intensity))
    fun onColorBehaviorChange(behavior: ColorBehavior) = processEvent(SettingsEvent.ColorBehaviorChanged(behavior))
    fun onMiniPlayerBehaviorChange(behavior: MiniPlayerBehavior) = processEvent(SettingsEvent.MiniPlayerBehaviorChanged(behavior))
    fun onAccessibilityChange(prefs: AccessibilityPreferences) = processEvent(SettingsEvent.AccessibilityChanged(prefs))
    fun onDiagnosticsChange(toggle: DiagnosticsToggle) = processEvent(SettingsEvent.DiagnosticsChanged(toggle))
    fun onProfileApply(profileId: String) = processEvent(SettingsEvent.ProfileApplied(profileId))
    fun onDeveloperModeTap() = processEvent(SettingsEvent.DeveloperModeUnlocked)
    fun onDeveloperModeLock() = processEvent(SettingsEvent.DeveloperModeLocked)
    fun onDeveloperOptionChange(key: String, value: Boolean) = processEvent(SettingsEvent.DeveloperOptionChanged(key, value))
    fun onResetToDefaults() = processEvent(SettingsEvent.ResetToDefaults)
    fun onClearAllData() = processEvent(SettingsEvent.AllDataCleared)

    private fun processEvent(event: SettingsEvent) {
        val currentState = _state.value
        val (newState, actions) = SettingsReducer.reduce(currentState, event)
        _state.value = newState
        if (actions.isNotEmpty()) {
            val requests = SettingsScreenCoordinator.plan(newState, actions)
            if (requests.isNotEmpty()) hostCoordinator.execute(requests)
        }
    }

    override fun onCleared() {
        super.onCleared()
        hostCoordinator.release()
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SCREEN WIRING
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun SettingsScreenWiring(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModelBridge = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    SettingsScreen(
        state = uiState,
        onThemeChange = { viewModel.onThemeChange(ThemePreference.valueOf(it)) },
        onMotionIntensityChange = { viewModel.onMotionIntensityChange(MotionIntensity.valueOf(it)) },
        onGlassIntensityChange = { viewModel.onGlassIntensityChange(GlassIntensity.valueOf(it)) },
        onColorBehaviorChange = { viewModel.onColorBehaviorChange(ColorBehavior.valueOf(it)) },
        onMiniPlayerBehaviorChange = { viewModel.onMiniPlayerBehaviorChange(MiniPlayerBehavior.valueOf(it)) },
        onAccessibilityChange = { section ->
            viewModel.onAccessibilityChange(
                com.rikodev.music.android.presentation.settings_v2.contract.AccessibilityPreferences(
                    highContrast = section.highContrast,
                    largeText = section.largeText,
                    reduceMotion = section.reduceMotion,
                    disableHaptics = section.disableHaptics,
                    talkBackVerbose = section.talkBackVerbose,
                )
            )
        },
        onDiagnosticsChange = { section ->
            viewModel.onDiagnosticsChange(
                com.rikodev.music.android.presentation.settings_v2.contract.DiagnosticsToggle(
                    showFrameRate = section.showFrameRate,
                    showRecompositionCount = section.showRecompositionCount,
                    showMemoryUsage = section.showMemoryUsage,
                    showWaveInspector = section.showWaveInspector,
                    enableValidationGates = section.enableValidationGates,
                    logLevel = try { com.rikodev.music.android.presentation.settings_v2.contract.LogLevel.valueOf(section.logLevel) } catch (_: Exception) { com.rikodev.music.android.presentation.settings_v2.contract.LogLevel.VERBOSE },
                )
            )
        },
        onProfileApply = viewModel::onProfileApply,
        onDeveloperModeTap = viewModel::onDeveloperModeTap,
        onDeveloperModeLock = viewModel::onDeveloperModeLock,
        onDeveloperOptionChange = viewModel::onDeveloperOptionChange,
        onResetToDefaults = viewModel::onResetToDefaults,
        onClearAllData = viewModel::onClearAllData,
        onExit = {
            // P3 kill switch: stop the foreground playback service (player
            // winds down with it via stopForUser), then finish the task.
            MusicPlaybackService.requestStop(context)
            (context as? Activity)?.finishAndRemoveTask()
        },
        modifier = modifier,
    )
}
