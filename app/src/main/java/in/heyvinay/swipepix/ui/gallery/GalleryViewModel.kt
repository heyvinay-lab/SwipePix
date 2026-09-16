package `in`.heyvinay.swipepix.ui.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.heyvinay.swipepix.data.model.MediaItem
import `in`.heyvinay.swipepix.data.repository.MediaRepository
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

typealias GalleryFilterCategory = `in`.heyvinay.swipepix.data.model.GalleryFilterCategory

/**
 * A date-grouped section for display in the photo grid.
 */
data class DateGroup(
    val header: String,
    val photos: List<MediaItem>,
    val dateKey: String = header,
)

/**
 * UI state for the gallery screen.
 */
sealed interface GalleryUiState {
    data object Loading : GalleryUiState
    data class Empty(val selectedFilter: GalleryFilterCategory = GalleryFilterCategory.ALL) : GalleryUiState
    data class Error(val message: String) : GalleryUiState
    data class Content(
        val dateGroups: List<DateGroup>,
        val totalCount: Int,
        val totalLoaded: Int = 0,
        val photoCount: Int = totalCount,
        val videoCount: Int = 0,
        val selectedFilter: GalleryFilterCategory = GalleryFilterCategory.ALL,
        val isLoadingMore: Boolean = false,
        val hasMore: Boolean = true,
        val isRefreshing: Boolean = false,
        val selectedIds: Set<Long> = emptySet(),
        val isSelectionMode: Boolean = false,
    ) : GalleryUiState
}

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val repository: MediaRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<GalleryUiState>(GalleryUiState.Loading)
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    private val allPhotos = mutableListOf<MediaItem>()
    private val selectedIds = mutableSetOf<Long>()
    private var lastTrashedItems = listOf<MediaItem>()
    private var authoritativeCounts: `in`.heyvinay.swipepix.data.model.AlbumMediaCount? = null
    private var selectedFilter = GalleryFilterCategory.ALL
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
        loadData(isRefresh = false)
    }

    /**
     * Loads or refreshes data.
     * When isRefresh = true and content already exists, existing photos and counts remain
     * visible with isRefreshing = true, preventing white flashes or '0 photos' flicker.
     */
    private fun loadData(isRefresh: Boolean) {
        val currentContent = _uiState.value as? GalleryUiState.Content
        if (isRefresh && currentContent != null) {
            _uiState.value = currentContent.copy(isRefreshing = true)
        } else if (_uiState.value !is GalleryUiState.Content) {
            _uiState.value = GalleryUiState.Loading
        }

        loadMoreJob?.cancel()
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            currentOffset = 0
            hasMore = true

            // Fetch authoritative total, photo, and video counts
            val newCounts = repository.getMediaCount(null).getOrNull()
            if (newCounts != null) {
                authoritativeCounts = newCounts
            }

            repository.getPhotos(limit = PAGE_SIZE, offset = 0, filter = selectedFilter)
                .onSuccess { photos ->
                    android.util.Log.d(
                        "SwipePixGallery",
                        "MediaStore query returned ${photos.size} items (filter=$selectedFilter, isRefresh=$isRefresh). Authoritative library total=${authoritativeCounts?.totalCount}, videos=${authoritativeCounts?.videoCount}, favs=${authoritativeCounts?.favoriteCount}"
                    )

                    val unique = photos.distinctBy { it.contentUri.toString() }
                    if (unique.size < photos.size) {
                        android.util.Log.w(
                            "SwipePixGallery",
                            "Deduplicated ${photos.size - unique.size} duplicate URIs from raw MediaStore query!"
                        )
                    }

                    // Atomic data replacement: update backing list and emit content in one pass
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
                    emitContent(isRefreshing = false)
                }
                .onFailure { error ->
                    if (_uiState.value is GalleryUiState.Content) {
                        // Background refresh failed: preserve existing gallery content
                        _uiState.value = (_uiState.value as GalleryUiState.Content).copy(isRefreshing = false)
                    } else {
                        _uiState.value = GalleryUiState.Error(
                            error.message ?: "Failed to load photos",
                        )
                    }
                }
        }
    }

    /**
     * Sets the active filter category and updates the displayed items.
     */
    fun setFilter(category: GalleryFilterCategory) {
        if (selectedFilter == category) return
        selectedFilter = category
        selectedIds.clear()
        // If content already exists, refresh with the new filter without blanking the screen
        loadData(isRefresh = _uiState.value is GalleryUiState.Content)
    }

    /**
     * Loads the next page of photos. Called when the user scrolls near the end.
     */
    fun loadMore() {
        if (isLoadingMore || !hasMore || loadJob?.isActive == true || loadMoreJob?.isActive == true) return

        loadMoreJob = viewModelScope.launch {
            isLoadingMore = true
            emitContent(isRefreshing = (_uiState.value as? GalleryUiState.Content)?.isRefreshing ?: false)

            repository.getPhotos(limit = PAGE_SIZE, offset = currentOffset, filter = selectedFilter)
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
            emitContent(isRefreshing = (_uiState.value as? GalleryUiState.Content)?.isRefreshing ?: false)
        }
    }

    /**
     * Refreshes the photo library in the background while keeping current content visible.
     */
    fun refresh() {
        loadData(isRefresh = true)
    }

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    private fun observeMediaChanges() {
        repository.observeChanges()
            .debounce(500L)
            .onEach { refresh() }
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

    private fun emitContent(isRefreshing: Boolean = false) {
        if (allPhotos.isEmpty()) {
            _uiState.value = GalleryUiState.Empty(selectedFilter = selectedFilter)
            return
        }

        val uniqueFiltered = allPhotos.distinctBy { it.contentUri.toString() }
        val grouped = DateUtils.groupPhotosByDate(uniqueFiltered)
        val dateGroups = grouped.map { (header, photos) ->
            DateGroup(header = header, photos = photos)
        }

        val (total, photos, videos) = when (selectedFilter) {
            GalleryFilterCategory.ALL -> {
                val total = authoritativeCounts?.totalCount ?: allPhotos.size
                val p = authoritativeCounts?.photoCount ?: allPhotos.count { it.mediaType == `in`.heyvinay.swipepix.data.model.MediaType.PHOTO }
                val v = authoritativeCounts?.videoCount ?: allPhotos.count { it.mediaType == `in`.heyvinay.swipepix.data.model.MediaType.VIDEO }
                Triple(total, p, v)
            }
            GalleryFilterCategory.VIDEOS -> {
                val videoCount = authoritativeCounts?.videoCount ?: allPhotos.size
                Triple(videoCount, 0, videoCount)
            }
            GalleryFilterCategory.FAVORITES -> {
                val favCount = authoritativeCounts?.favoriteCount ?: allPhotos.size
                val p = allPhotos.count { it.mediaType == `in`.heyvinay.swipepix.data.model.MediaType.PHOTO }
                val v = allPhotos.count { it.mediaType == `in`.heyvinay.swipepix.data.model.MediaType.VIDEO }
                Triple(favCount, p, v)
            }
            GalleryFilterCategory.SCREENSHOTS -> {
                val p = allPhotos.count { it.mediaType == `in`.heyvinay.swipepix.data.model.MediaType.PHOTO }
                val v = allPhotos.count { it.mediaType == `in`.heyvinay.swipepix.data.model.MediaType.VIDEO }
                Triple(allPhotos.size, p, v)
            }
        }

        _uiState.value = GalleryUiState.Content(
            dateGroups = dateGroups,
            totalCount = total,
            totalLoaded = allPhotos.size,
            photoCount = photos,
            videoCount = videos,
            selectedFilter = selectedFilter,
            isLoadingMore = isLoadingMore,
            hasMore = hasMore,
            isRefreshing = isRefreshing,
            selectedIds = selectedIds.toSet(),
            isSelectionMode = selectedIds.isNotEmpty(),
        )
    }
}
