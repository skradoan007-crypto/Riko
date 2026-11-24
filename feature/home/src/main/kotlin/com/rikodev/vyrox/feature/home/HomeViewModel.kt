package com.rikodev.vyrox.feature.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rikodev.vyrox.core.common.DeviceCapabilityProbe
import com.rikodev.vyrox.core.common.DeviceProfile
import com.rikodev.vyrox.core.model.ProjectSummary
import com.rikodev.vyrox.domain.ProjectRepository
import com.rikodev.vyrox.media.engine.MediaInspector
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val recentProjects: List<ProjectSummary> = emptyList(),
    val deviceProfile: DeviceProfile? = null,
    val selectedTab: Int = 0,
    val isImporting: Boolean = false,
    val statusBanner: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val projectRepository: ProjectRepository,
    private val mediaInspector: MediaInspector,
    deviceProbe: DeviceCapabilityProbe
) : ViewModel() {

    private val selectedTab = MutableStateFlow(0)
    private val importing = MutableStateFlow(false)
    private val statusBanner = MutableStateFlow<String?>(null)
    private val profile = deviceProbe.probe()

    val uiState: StateFlow<HomeUiState> = combine(
        projectRepository.observeRecentProjects(),
        selectedTab,
        importing,
        statusBanner
    ) { projects, tab, isImp, banner ->
        HomeUiState(
            recentProjects = projects,
            deviceProfile = profile,
            selectedTab = tab,
            isImporting = isImp,
            statusBanner = banner
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState(deviceProfile = profile))

    fun onTabSelected(index: Int) {
        selectedTab.value = index
    }

    fun importVideoAndOpen(uri: Uri, onProjectReady: (String) -> Unit) {
        viewModelScope.launch {
            importing.value = true
            runCatching {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                val meta = mediaInspector.inspectVideo(uri)
                projectRepository.createProjectFromVideo(meta)
            }.onSuccess { doc ->
                importing.value = false
                onProjectReady(doc.projectId)
            }.onFailure { err ->
                importing.value = false
                statusBanner.value = "Import failed: ${err.localizedMessage ?: "Unsupported video"}"
            }
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            projectRepository.deleteProject(projectId)
        }
    }

    fun dismissBanner() {
        statusBanner.value = null
    }
}
