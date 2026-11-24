package com.rikodev.vyrox.feature.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rikodev.vyrox.core.common.DeviceCapabilityProbe
import com.rikodev.vyrox.core.common.DeviceProfile
import com.rikodev.vyrox.core.model.ColorPreset
import com.rikodev.vyrox.core.model.ProjectSummary
import com.rikodev.vyrox.core.storage.VyRoxStorageManager
import com.rikodev.vyrox.domain.ColorPresetRepository
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
    val presets: List<ColorPreset> = emptyList(),
    val deviceProfile: DeviceProfile? = null,
    val selectedTab: Int = 0,
    val isImporting: Boolean = false,
    val cacheBytes: Long = 0L,
    val lastCrashLog: String? = null,
    val statusBanner: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val projectRepository: ProjectRepository,
    private val colorPresetRepository: ColorPresetRepository,
    private val mediaInspector: MediaInspector,
    private val storage: VyRoxStorageManager,
    deviceProbe: DeviceCapabilityProbe
) : ViewModel() {

    private val selectedTab = MutableStateFlow(0)
    private val importing = MutableStateFlow(false)
    private val statusBanner = MutableStateFlow<String?>(null)
    private val cacheBytes = MutableStateFlow(0L)
    private val crashLog = MutableStateFlow<String?>(null)
    private val profile = deviceProbe.probe()

    init {
        refreshDiagnostics()
    }

    val uiState: StateFlow<HomeUiState> = combine(
        combine(projectRepository.observeRecentProjects(), colorPresetRepository.observePresets(), selectedTab) { p, pr, t ->
            Triple(p, pr, t)
        },
        combine(importing, statusBanner, cacheBytes, crashLog) { imp, ban, cb, cl ->
            listOf(imp, ban, cb, cl)
        }
    ) { first, second ->
        HomeUiState(
            recentProjects = first.first,
            presets = first.second,
            deviceProfile = profile,
            selectedTab = first.third,
            isImporting = second[0] as Boolean,
            statusBanner = second[1] as String?,
            cacheBytes = second[2] as Long,
            lastCrashLog = second[3] as String?
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState(deviceProfile = profile))

    fun onTabSelected(index: Int) {
        selectedTab.value = index
        if (index == 2 || index == 3) refreshDiagnostics()
    }

    fun refreshDiagnostics() {
        viewModelScope.launch {
            cacheBytes.value = storage.computeCacheBytes()
            crashLog.value = storage.readLastCrashLog()
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            val freed = storage.clearAllCache()
            cacheBytes.value = storage.computeCacheBytes()
            statusBanner.value = "Cleared ${freed / 1024L} KB temporary cache."
        }
    }

    fun clearCrashLog() {
        storage.clearCrashLog()
        crashLog.value = null
    }

    fun deleteCustomPreset(presetId: String) {
        viewModelScope.launch { colorPresetRepository.deleteCustomPreset(presetId) }
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
                statusBanner.value = "Import failed: ${err.localizedMessage ?: "Unsupported video format"}"
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
