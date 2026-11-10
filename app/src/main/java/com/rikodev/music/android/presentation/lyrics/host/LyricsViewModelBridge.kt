/*
 * RIKO EXPERIENCE OS — PRODUCTION-GRADE ROADMAP v2.0
 * Milestone 5 — Lyrics Experience
 *
 * File 6 / 12 — LyricsViewModelBridge.kt
 * --------------------------------------------------------------------
 * Android ViewModel bridge for Lyrics.
 * © RIKO Experience OS — Lyrics Host Tier
 */

package com.rikodev.music.android.presentation.lyrics.host

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rikodev.music.android.presentation.lyrics.contract.*
import com.rikodev.music.android.presentation.lyrics.runtime.LyricsMapper
import com.rikodev.music.android.presentation.lyrics.runtime.LyricsReducer
import com.rikodev.music.android.presentation.lyrics.runtime.LyricsScreenCoordinator
import com.rikodev.music.android.presentation.lyrics.runtime.LyricsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.animation.core.Animatable

@HiltViewModel
class LyricsViewModelBridge @Inject constructor(
    private val hostCoordinator: LyricsHostCoordinator,
) : ViewModel() {

    private val _state = MutableStateFlow(LyricsState.INITIAL)
    val state: StateFlow<LyricsState> = _state.asStateFlow()

    private val _uiState = MutableStateFlow(LyricsMapper.map(_state.value))
    val uiState: StateFlow<LyricsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            hostCoordinator.events.collect { event ->
                processEvent(event)
            }
        }
        viewModelScope.launch {
            _state.collect { newState ->
                _uiState.value = LyricsMapper.map(newState)
            }
        }
    }

    fun onShow(songId: Long) = processEvent(LyricsEvent.Shown(songId))
    fun onHide() = processEvent(LyricsEvent.Hidden)
    fun onPlaybackPositionChanged(positionMs: Long) = processEvent(LyricsEvent.PlaybackPositionUpdated(positionMs))
    fun onLineTap(lineIndex: Int) {
        val line = _state.value.lyrics.lines.getOrNull(lineIndex) ?: return
        if (line.isSynced && line.startTimeMs != null) {
            processEvent(LyricsEvent.LineSeeked(lineIndex, line.startTimeMs))
        } else {
            processEvent(LyricsEvent.ScrolledToLine(lineIndex))
        }
    }
    fun onReadingModeChange(mode: LyricsReadingMode) = processEvent(LyricsEvent.ReadingModeApplied(mode))
    fun onFontSizeScale(scale: Float) = processEvent(LyricsEvent.FontSizeApplied(scale))
    fun onToggleFollow() = processEvent(LyricsEvent.FollowToggled(!_state.value.isFollowingPlayback))
    fun onRefresh() = processEvent(LyricsEvent.Refreshed)
    fun onDismissError() = processEvent(LyricsEvent.ErrorDismissed)
    fun onAttributionTap() {
        val url = _state.value.lyrics.providerUrl ?: return
        processEvent(LyricsEvent.AttributionOpened(url))
    }
    fun onShare() = processEvent(LyricsEvent.LyricsShared)

    private fun processEvent(event: LyricsEvent) {
        val currentState = _state.value
        val (newState, actions) = LyricsReducer.reduce(currentState, event)
        _state.value = newState
        if (actions.isNotEmpty()) {
            val requests = LyricsScreenCoordinator.plan(newState, actions)
            if (requests.isNotEmpty()) {
                hostCoordinator.execute(requests)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        hostCoordinator.release()
    }
}
