package com.rikodev.ibd3d.android.ui

import androidx.lifecycle.*
import com.rikodev.ibd3d.core.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@HiltViewModel
class PanelViewModel
@Inject
constructor(private val repo: PanelRepository, private val saved: SavedStateHandle) : ViewModel() {
    val preferences =
        repo.preferences.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            UserPreferences(),
        )
    val catalog = MutableStateFlow<CatalogData?>(null)
    val error = MutableStateFlow<String?>(null)
    val query = saved.getStateFlow("query", "")
    val category = saved.getStateFlow("category", "All")
    val messages = MutableSharedFlow<String>(extraBufferCapacity = 8)

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            error.value = null
            try {
                catalog.value = repo.load()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error.value = "Could not load the bundled catalog. Please retry."
            }
        }
    }

    fun query(v: String) {
        saved["query"] = v
    }

    fun category(v: String) {
        saved["category"] = v
    }

    private fun persist(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                messages.emit("Could not save changes. Please try again.")
            }
        }
    }

    fun favorite(id: String) = persist { repo.favorite(id) }

    fun copied(id: String) = persist { repo.record(id) }

    fun feedback(v: Boolean) = persist { repo.feedback(v) }

    fun accent(v: String) = persist { repo.accent(v) }

    fun noticeSeen() = persist { repo.noticeSeen() }

    fun reminder(value: Boolean) = persist { repo.reminder(value) }

    fun clear() = persist { repo.clear() }
}
