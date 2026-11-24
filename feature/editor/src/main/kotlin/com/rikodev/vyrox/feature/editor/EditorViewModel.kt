package com.rikodev.vyrox.feature.editor

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rikodev.vyrox.core.model.ColorGradeState
import com.rikodev.vyrox.core.model.ColorPreset
import com.rikodev.vyrox.core.model.ProjectDocument
import com.rikodev.vyrox.core.model.VideoClip
import com.rikodev.vyrox.domain.ColorPresetRepository
import com.rikodev.vyrox.domain.ProjectRepository
import com.rikodev.vyrox.domain.UndoRedoStack
import com.rikodev.vyrox.media.engine.PreviewStatus
import com.rikodev.vyrox.media.engine.VyRoxPreviewController
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class EditorToolTab { TRIM, COLOR, SPEED }

data class EditorUiState(
    val project: ProjectDocument? = null,
    val primaryClip: VideoClip? = null,
    val sourceUri: String? = null,
    val previewStatus: PreviewStatus = PreviewStatus.Idle,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val activeTool: EditorToolTab = EditorToolTab.TRIM,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val presets: List<ColorPreset> = emptyList(),
    val statusToast: String? = null
)

@HiltViewModel
class EditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext context: Context,
    private val projectRepository: ProjectRepository,
    private val colorPresetRepository: ColorPresetRepository
) : ViewModel() {

    private val projectId: String = checkNotNull(savedStateHandle["projectId"])
    val previewController = VyRoxPreviewController(context)

    private val undoStack = UndoRedoStack<ProjectDocument>()
    private val projectFlow = MutableStateFlow<ProjectDocument?>(null)
    private val sourceUriFlow = MutableStateFlow<String?>(null)
    private val playbackFlow = MutableStateFlow(Triple(false, 0L, 0L))
    private val toolFlow = MutableStateFlow(EditorToolTab.TRIM)
    private val toastFlow = MutableStateFlow<String?>(null)

    val uiState: StateFlow<EditorUiState> = combine(
        combine(projectFlow, sourceUriFlow, previewController.status) { d, u, s -> Triple(d, u, s) },
        combine(playbackFlow, toolFlow, colorPresetRepository.observePresets(), toastFlow) { p, t, pr, m ->
            listOf(p, t, pr, m)
        }
    ) { first, second ->
        val doc = first.first
        val uri = first.second
        val pStatus = first.third
        @Suppress("UNCHECKED_CAST")
        val playState = second[0] as Triple<Boolean, Long, Long>
        val tool = second[1] as EditorToolTab
        @Suppress("UNCHECKED_CAST")
        val presets = second[2] as List<ColorPreset>
        val toast = second[3] as String?

        val clip = doc?.clips?.firstOrNull()
        val safeDur = playState.third.takeIf { it > 0L } ?: (clip?.durationMs ?: 1_000L)

        EditorUiState(
            project = doc,
            primaryClip = clip,
            sourceUri = uri,
            previewStatus = pStatus,
            isPlaying = playState.first,
            positionMs = playState.second.coerceIn(0L, safeDur),
            durationMs = safeDur,
            activeTool = tool,
            canUndo = undoStack.canUndo,
            canRedo = undoStack.canRedo,
            presets = presets,
            statusToast = toast
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EditorUiState())

    init {
        viewModelScope.launch {
            val loaded = projectRepository.loadProject(projectId)
            if (loaded == null) {
                toastFlow.value = "Project file not found."
                return@launch
            }
            undoStack.initialize(loaded)
            projectFlow.value = loaded
            val clip = loaded.clips.firstOrNull()
            if (clip != null) {
                val uri = projectRepository.resolveAssetUri(clip.mediaAssetId)
                sourceUriFlow.value = uri
                if (uri != null) {
                    previewController.loadClip(uri, clip, autoPlay = true)
                } else {
                    toastFlow.value = "Original video URI missing. Please re-import."
                }
            }
        }

        viewModelScope.launch {
            while (isActive) {
                val p = previewController.player
                val rawDur = p.duration
                val safeDur = if (rawDur > 0L) rawDur else 0L
                val safePos = p.currentPosition.coerceAtLeast(0L)
                playbackFlow.value = Triple(p.isPlaying, safePos, safeDur)
                delay(100L)
            }
        }
    }

    fun onScreenResumed() {
        val clip = projectFlow.value?.clips?.firstOrNull()
        val uri = sourceUriFlow.value
        if (clip != null && uri != null) {
            previewController.loadClip(uri, clip, autoPlay = false)
        }
    }

    fun onScreenPaused() {
        previewController.pauseAndFreeDecoder()
    }

    fun selectTool(tool: EditorToolTab) {
        toolFlow.value = tool
    }

    fun togglePlayPause() = previewController.togglePlayPause()

    fun seekTo(positionMs: Long) = previewController.seekTo(positionMs)

    fun previewColorGrade(grade: ColorGradeState) {
        previewController.updateColorGrade(grade)
        val doc = projectFlow.value ?: return
        val clip = doc.clips.firstOrNull() ?: return
        projectFlow.value = doc.copy(clips = listOf(clip.copy(colorGrade = grade)) + doc.clips.drop(1))
    }

    fun commitColorGrade(grade: ColorGradeState) {
        updatePrimaryClip { it.copy(colorGrade = grade) }
    }

    fun commitTrim(startMs: Long, endMs: Long) {
        val clip = projectFlow.value?.clips?.firstOrNull() ?: return
        val maxSrc = clip.effectiveSourceDurationMs
        val safeStart = startMs.coerceIn(0L, (maxSrc - 500L).coerceAtLeast(0L))
        val safeEnd = endMs.coerceIn(safeStart + 500L, maxSrc)
        updatePrimaryClip { it.copy(sourceTrimStartMs = safeStart, sourceTrimEndMs = safeEnd) }
    }

    fun commitSpeed(speed: Float) {
        updatePrimaryClip { it.copy(speed = speed.coerceIn(0.25f, 3.0f)) }
    }

    fun toggleMute() {
        updatePrimaryClip { it.copy(muted = !it.muted) }
    }

    fun saveCustomPreset(name: String) {
        val grade = projectFlow.value?.clips?.firstOrNull()?.colorGrade ?: return
        viewModelScope.launch {
            val saved = colorPresetRepository.saveCustomPreset(name, grade)
            toastFlow.value = "Saved preset '${saved.name}'"
        }
    }

    fun dismissToast() {
        toastFlow.value = null
    }

    fun undo() {
        val restored = undoStack.undo() ?: return
        applyAndPersist(restored, pushUndo = false)
    }

    fun redo() {
        val restored = undoStack.redo() ?: return
        applyAndPersist(restored, pushUndo = false)
    }

    private fun updatePrimaryClip(transform: (VideoClip) -> VideoClip) {
        val doc = projectFlow.value ?: return
        val first = doc.clips.firstOrNull() ?: return
        val updatedClip = transform(first)
        val updatedDoc = doc.copy(clips = listOf(updatedClip) + doc.clips.drop(1))
        applyAndPersist(updatedDoc, pushUndo = true)
    }

    private fun applyAndPersist(doc: ProjectDocument, pushUndo: Boolean) {
        if (pushUndo) undoStack.push(doc)
        projectFlow.value = doc
        val clip = doc.clips.firstOrNull()
        val uri = sourceUriFlow.value
        if (clip != null && uri != null) {
            previewController.loadClip(uri, clip, autoPlay = false)
        }
        viewModelScope.launch {
            projectRepository.saveProject(doc)
        }
    }

    override fun onCleared() {
        previewController.release()
        super.onCleared()
    }
}
