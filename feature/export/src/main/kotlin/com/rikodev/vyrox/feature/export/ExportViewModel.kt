package com.rikodev.vyrox.feature.export

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rikodev.vyrox.core.model.ExportConfig
import com.rikodev.vyrox.core.model.ExportResolution
import com.rikodev.vyrox.core.model.ExportVideoCodec
import com.rikodev.vyrox.domain.ProjectRepository
import com.rikodev.vyrox.media.engine.ExportState
import com.rikodev.vyrox.media.engine.VyRoxExportEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExportUiState(
    val projectTitle: String = "VyRox Export",
    val config: ExportConfig = ExportConfig(),
    val exportState: ExportState = ExportState.Idle
)

@HiltViewModel
class ExportViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val projectRepository: ProjectRepository,
    private val exportEngine: VyRoxExportEngine
) : ViewModel() {

    private val projectId: String = checkNotNull(savedStateHandle["projectId"])
    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    private var exportJob: Job? = null

    init {
        viewModelScope.launch {
            projectRepository.loadProject(projectId)?.let { doc ->
                _uiState.value = _uiState.value.copy(
                    projectTitle = doc.title,
                    config = _uiState.value.config.copy(outputName = doc.title)
                )
            }
        }
    }

    fun selectResolution(res: ExportResolution) {
        _uiState.value = _uiState.value.copy(config = _uiState.value.config.copy(resolution = res))
    }

    fun selectCodec(codec: ExportVideoCodec) {
        _uiState.value = _uiState.value.copy(config = _uiState.value.config.copy(codec = codec))
    }

    fun startExport() {
        exportJob?.cancel()
        exportJob = viewModelScope.launch {
            val doc = projectRepository.loadProject(projectId)
            val clip = doc?.clips?.firstOrNull()
            val uri = clip?.let { projectRepository.resolveAssetUri(it.mediaAssetId) }
            if (clip == null || uri == null) {
                _uiState.value = _uiState.value.copy(
                    exportState = ExportState.Failed("No valid video clip found in this project.")
                )
                return@launch
            }
            exportEngine.exportClip(uri, clip, _uiState.value.config).collect { state ->
                _uiState.value = _uiState.value.copy(exportState = state)
            }
        }
    }

    fun cancelExport() {
        exportJob?.cancel()
        _uiState.value = _uiState.value.copy(exportState = ExportState.Idle)
    }
}
