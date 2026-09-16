package `in`.heyvinay.swipepix.ui.trash

import android.content.IntentSender
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.heyvinay.swipepix.data.model.MediaItem
import `in`.heyvinay.swipepix.data.model.MediaType
import `in`.heyvinay.swipepix.data.repository.MediaRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TrashFilter {
    ALL,
    PHOTOS,
    VIDEOS
}

sealed interface TrashUiState {
    data object Loading : TrashUiState
    data object Empty : TrashUiState
    data class Error(val message: String) : TrashUiState
    data class Ready(
        val allItems: List<MediaItem>,
        val displayedItems: List<MediaItem>,
        val selectedIds: Set<Long>,
        val activeFilter: TrashFilter,
        val totalCount: Int,
        val photoCount: Int,
        val videoCount: Int,
    ) : TrashUiState
}

@HiltViewModel
class TrashViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<TrashUiState>(TrashUiState.Loading)
    val uiState: StateFlow<TrashUiState> = _uiState.asStateFlow()

    private var cachedItems = listOf<MediaItem>()
    private val selectedIds = mutableSetOf<Long>()
    private var currentFilter = TrashFilter.ALL
    private var loadJob: Job? = null

    init {
        loadTrashedMedia()
    }

    fun loadTrashedMedia() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = TrashUiState.Loading

            mediaRepository.getTrashedMedia()
                .onSuccess { items ->
                    val unique = items.distinctBy { it.contentUri.toString() }
                    cachedItems = unique
                    selectedIds.clear()

                    if (unique.isEmpty()) {
                        _uiState.value = TrashUiState.Empty
                    } else {
                        emitReadyState()
                    }
                }
                .onFailure { error ->
                    _uiState.value = TrashUiState.Error(error.message ?: "Failed to load trash")
                }
        }
    }

    fun setFilter(filter: TrashFilter) {
        currentFilter = filter
        emitReadyState()
    }

    fun toggleSelection(id: Long) {
        if (selectedIds.contains(id)) {
            selectedIds.remove(id)
        } else {
            selectedIds.add(id)
        }
        emitReadyState()
    }

    fun toggleSelectAll() {
        val displayed = getFilteredItems()
        if (selectedIds.size == displayed.size) {
            selectedIds.clear()
        } else {
            selectedIds.clear()
            selectedIds.addAll(displayed.map { it.id })
        }
        emitReadyState()
    }

    suspend fun createRestoreRequest(): IntentSender? {
        val targets = getTargetItems()
        if (targets.isEmpty()) return null
        return mediaRepository.restoreMediaItems(targets)
    }

    suspend fun createPermanentDeleteRequest(): IntentSender? {
        val targets = getTargetItems()
        if (targets.isEmpty()) return null
        return mediaRepository.deleteMediaItemsPermanently(targets)
    }

    private fun getTargetItems(): List<MediaItem> {
        val displayed = getFilteredItems()
        return if (selectedIds.isNotEmpty()) {
            displayed.filter { it.id in selectedIds }
        } else {
            displayed
        }
    }

    private fun getFilteredItems(): List<MediaItem> {
        return when (currentFilter) {
            TrashFilter.ALL -> cachedItems
            TrashFilter.PHOTOS -> cachedItems.filter { it.mediaType == MediaType.PHOTO }
            TrashFilter.VIDEOS -> cachedItems.filter { it.mediaType == MediaType.VIDEO }
        }
    }

    private fun emitReadyState() {
        val displayed = getFilteredItems()
        val photos = cachedItems.count { it.mediaType == MediaType.PHOTO }
        val videos = cachedItems.count { it.mediaType == MediaType.VIDEO }

        _uiState.value = TrashUiState.Ready(
            allItems = cachedItems,
            displayedItems = displayed,
            selectedIds = selectedIds.toSet(),
            activeFilter = currentFilter,
            totalCount = cachedItems.size,
            photoCount = photos,
            videoCount = videos
        )
    }
}
