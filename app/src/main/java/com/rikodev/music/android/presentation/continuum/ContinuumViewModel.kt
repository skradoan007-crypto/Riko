package com.rikodev.music.android.presentation.continuum

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rikodev.music.android.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// RIKO CONTINUUM + LIQUID NEXUS — VIEW MODEL (P1, P4, P7, P9)
//
// Owns:
//   - Persisted VisualQualityTier (DataStore via SettingsRepository, default ULTRA)
//   - CONTINUUM smart navigation context & Timefold history state
//   - LiquidNexusStateMachine presentation state & one-shot effects
// Survives configuration changes cleanly and normalizes transient states.
// ─────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class ContinuumViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val visualQualityTier: StateFlow<VisualQualityTier> = settingsRepository.visualQualityTier
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = VisualQualityTier.DEFAULT
        )

    private val _navState = MutableStateFlow(ContinuumNavHistoryState())
    val navState: StateFlow<ContinuumNavHistoryState> = _navState.asStateFlow()

    private val _nexusState = MutableStateFlow(LiquidNexusState())
    val nexusState: StateFlow<LiquidNexusState> = _nexusState.asStateFlow()

    private val _effects = MutableSharedFlow<LiquidNexusEffect>(extraBufferCapacity = 16)
    val effects: SharedFlow<LiquidNexusEffect> = _effects.asSharedFlow()

    fun setVisualQualityTier(tier: VisualQualityTier) {
        viewModelScope.launch {
            settingsRepository.setVisualQualityTier(tier)
        }
    }

    fun onRouteVisited(route: String) {
        _navState.value = ContinuumNavHistoryReducer.onRouteVisited(_navState.value, route)
    }

    fun onSectionSelected(section: ContinuumSection): String {
        val (nextState, targetRoute) = ContinuumNavHistoryReducer.onSectionSelected(_navState.value, section)
        _navState.value = nextState
        return targetRoute
    }

    fun onLibrarySubTabSelected(subTab: Int) {
        _navState.value = ContinuumNavHistoryReducer.onLibrarySubTabSelected(_navState.value, subTab)
    }

    fun dispatchNexus(event: LiquidNexusEvent): List<LiquidNexusEffect> {
        val (next, emitted) = LiquidNexusStateMachine.reduce(_nexusState.value, event)
        _nexusState.value = next
        emitted.forEach { _effects.tryEmit(it) }
        return emitted
    }
}
