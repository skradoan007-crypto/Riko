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
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val activeTool: EditorToolTab = EditorToolTab.TRIM,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val presets: List<ColorPreset> = emptyList(),
    val errorMessage: String? = null
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
    private val errorFlow = MutableStateFlow<String?>(null)

    val uiState: StateFlow<EditorUiState> = combine(
        projectFlow,
        sourceUriFlow,
        playbackFlow,
        toolFlow,
        colorPresetRepository.observePresets()
    ) { doc, uri, playState, tool, presets ->
        val clip = doc?.clips?.firstOrNull()
        EditorUiState(
            project = doc,
            primaryClip = clip,
            sourceUri = uri,
            isPlaying = playState.first,
            positionMs = playState.second,
            durationMs = playState.third.coerceAtLeast(clip?.durationMs ?: 0L),
            activeTool = tool,
            canUndo = undoStack.canUndo,
            canRedo = undoStack.canRedo,
            presets = presets,
            errorMessage = errorFlow.value
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EditorUiState())

    init {
        viewModelScope.launch {
            val loaded = projectRepository.loadProject(projectId)
            if (loaded == null) {
                errorFlow.value = "Project file not found."
                return@launch
            }
            undoStack.initialize(loaded)
            projectFlow.value = loaded
            val clip = loaded.clips.firstOrNull()
            if (clip != null) {
                val uri = projectRepository.resolveAssetUri(clip.mediaAssetId)
                sourceUriFlow.value = uri
                if (uri != null) {
                    previewController.loadClip(uri, clip)
                }
            }
        }

        // Poll player clock for timeline playhead
        viewModelScope.launch {
            while (isActive) {
                val p = previewController.player
                playbackFlow.value = Triple(
                    p.isPlaying,
                    p.currentPosition.coerceAtLeast(0L),
                    p.duration.coerceAtLeast(0L)
                )
                delay(80L)
            }
        }
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

    fun updateTrim(startMs: Long, endMs: Long) {
        val safeStart = startMs.coerceAtLeast(0L)
        val safeEnd = endMs.coerceAtLeast(safeStart + 500L)
        updatePrimaryClip { it.copy(sourceTrimStartMs = safeStart, sourceTrimEndMs = safeEnd) }
    }

    fun updateSpeed(speed: Float) {
        updatePrimaryClip { it.copy(speed = speed.coerceIn(0.25f, 4f)) }
    }

    fun toggleMute() {
        updatePrimaryClip { it.copy(muted = !it.muted) }
    }

    fun saveCustomPreset(name: String) {
        val grade = projectFlow.value?.clips?.firstOrNull()?.colorGrade ?: return
        viewModelScope.launch {
            colorPresetRepository.saveCustomPreset(name, grade)
        }
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
            previewController.loadClip(uri, clip)
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
