package `in`.heyvinay.swipepix.ui.albums

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.heyvinay.swipepix.data.model.MediaItem
import `in`.heyvinay.swipepix.data.repository.MediaRepository
import `in`.heyvinay.swipepix.ui.gallery.DateGroup
import `in`.heyvinay.swipepix.ui.util.DateUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the album photo browser screen.
 */
sealed interface ViewPhotosUiState {
    data object Loading : ViewPhotosUiState
    data object Empty : ViewPhotosUiState
    data class Error(val message: String) : ViewPhotosUiState
    data class Content(
        val albumName: String,
        val dateGroups: List<DateGroup>,
        val totalLoaded: Int,
        val totalCount: Int,
        val photoCount: Int,
        val videoCount: Int,
        val isLoadingMore: Boolean,
        val hasMore: Boolean,
        val selectedIds: Set<Long> = emptySet(),
        val isSelectionMode: Boolean = false,
    ) : ViewPhotosUiState
}

@HiltViewModel
class ViewPhotosViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MediaRepository,
) : ViewModel() {

    val albumId: String = checkNotNull(savedStateHandle["albumId"])
    val albumName: String = checkNotNull(savedStateHandle["albumName"])

    private val _uiState = MutableStateFlow<ViewPhotosUiState>(ViewPhotosUiState.Loading)
    val uiState: StateFlow<ViewPhotosUiState> = _uiState.asStateFlow()

    private val allPhotos = mutableListOf<MediaItem>()
    private val selectedIds = mutableSetOf<Long>()
    private var lastTrashedItems = listOf<MediaItem>()
    private var authoritativeCounts: `in`.heyvinay.swipepix.data.model.AlbumMediaCount? = null
    private var currentOffset = 0
    private var hasMore = true
    private var isLoadingMore = false

    private var loadJob: Job? = null
    private var loadMoreJob: Job? = null

    companion object {
        private const val PAGE_SIZE = 200
    }

    init {
        loadInitial()
        observeMediaChanges()
    }

    private fun loadInitial() {
        loadMoreJob?.cancel()
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = ViewPhotosUiState.Loading
            currentOffset = 0
            hasMore = true

            // Query authoritative counts in parallel
            authoritativeCounts = repository.getMediaCount(albumId).getOrNull()

            repository.getPhotos(bucketId = albumId, limit = PAGE_SIZE, offset = 0)
                .onSuccess { photos ->
                    val unique = photos.distinctBy { it.contentUri.toString() }
                    allPhotos.clear()
                    allPhotos.addAll(unique)
                    val existingIds = allPhotos.map { it.id }.toSet()
                    selectedIds.retainAll(existingIds)
                    allPhotos.sortWith(
                        compareByDescending<MediaItem> { it.effectiveTimestamp }
                            .thenByDescending { it.id }
                    )
                    currentOffset = photos.size
                    hasMore = photos.size == PAGE_SIZE
                    emitContent()
                }
                .onFailure { error ->
                    _uiState.value = ViewPhotosUiState.Error(
                        error.message ?: "Failed to load photos",
                    )
                }
        }
    }

    fun loadMore() {
        if (isLoadingMore || !hasMore || loadJob?.isActive == true || loadMoreJob?.isActive == true) return

        loadMoreJob = viewModelScope.launch {
            isLoadingMore = true
            emitContent()

            repository.getPhotos(bucketId = albumId, limit = PAGE_SIZE, offset = currentOffset)
                .onSuccess { photos ->
                    val existingUris = allPhotos.map { it.contentUri.toString() }.toHashSet()
                    val newUniquePhotos = photos.filter { existingUris.add(it.contentUri.toString()) }
                    allPhotos.addAll(newUniquePhotos)
                    allPhotos.sortWith(
                        compareByDescending<MediaItem> { it.effectiveTimestamp }
                            .thenByDescending { it.id }
                    )
                    currentOffset += photos.size
                    hasMore = photos.size == PAGE_SIZE && newUniquePhotos.isNotEmpty()
                }
                .onFailure {
                    // Silently fail on pagination — user still sees existing content
                }

            isLoadingMore = false
            emitContent()
        }
    }

    fun refresh() {
        loadInitial()
    }

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    private fun observeMediaChanges() {
        repository.observeChanges()
            .debounce(500L)
            .onEach { loadInitial() }
            .launchIn(viewModelScope)
    }

    fun enterSelection(item: MediaItem) {
        selectedIds.add(item.id)
        emitContent()
    }

    fun toggleSelection(item: MediaItem) {
        if (selectedIds.contains(item.id)) {
            selectedIds.remove(item.id)
        } else {
            selectedIds.add(item.id)
        }
        emitContent()
    }

    fun toggleSelectAll() {
        if (selectedIds.size == allPhotos.size && allPhotos.isNotEmpty()) {
            selectedIds.clear()
        } else {
            selectedIds.clear()
            selectedIds.addAll(allPhotos.map { it.id })
        }
        emitContent()
    }

    fun clearSelection() {
        selectedIds.clear()
        emitContent()
    }

    fun getSelectedItems(): List<MediaItem> {
        return allPhotos.filter { it.id in selectedIds }
    }

    suspend fun createTrashRequestForSelected(): android.content.IntentSender? {
        val targets = getSelectedItems()
        if (targets.isEmpty()) return null
        lastTrashedItems = targets
        return repository.trashMediaItems(targets)
    }

    fun onTrashOperationSuccess() {
        val trashedIds = lastTrashedItems.map { it.id }.toSet()
        allPhotos.removeAll { it.id in trashedIds }
        selectedIds.clear()
        emitContent()
        refresh()
    }

    suspend fun createUndoTrashRequest(): android.content.IntentSender? {
        if (lastTrashedItems.isEmpty()) return null
        return repository.restoreMediaItems(lastTrashedItems)
    }

    fun onUndoOperationSuccess() {
        lastTrashedItems = emptyList()
        refresh()
    }

    private fun emitContent() {
        if (allPhotos.isEmpty()) {
            _uiState.value = ViewPhotosUiState.Empty
            return
        }

        val uniquePhotos = allPhotos.distinctBy { it.contentUri.toString() }
        val grouped = DateUtils.groupPhotosByDate(uniquePhotos)
        val dateGroups = grouped.map { (header, photos) ->
            DateGroup(header = header, photos = photos)
        }

        val total = authoritativeCounts?.totalCount ?: allPhotos.size
        val photos = authoritativeCounts?.photoCount ?: allPhotos.size
        val videos = authoritativeCounts?.videoCount ?: 0

        _uiState.value = ViewPhotosUiState.Content(
            albumName = albumName,
            dateGroups = dateGroups,
            totalLoaded = allPhotos.size,
            totalCount = total,
            photoCount = photos,
            videoCount = videos,
            isLoadingMore = isLoadingMore,
            hasMore = hasMore,
            selectedIds = selectedIds.toSet(),
            isSelectionMode = selectedIds.isNotEmpty(),
        )
    }
}
