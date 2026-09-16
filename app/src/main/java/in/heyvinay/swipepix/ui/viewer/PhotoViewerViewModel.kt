package `in`.heyvinay.swipepix.ui.viewer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.heyvinay.swipepix.data.model.MediaItem
import `in`.heyvinay.swipepix.data.repository.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ViewerUiState {
    data object Loading : ViewerUiState
    data class Error(val message: String) : ViewerUiState
    data class Content(
        val photos: List<MediaItem>,
        val initialIndex: Int,
    ) : ViewerUiState
}

@HiltViewModel
class PhotoViewerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MediaRepository,
) : ViewModel() {

    private val mediaId: Long = checkNotNull(savedStateHandle["mediaId"])
    private val albumId: String? = savedStateHandle["albumId"]

    private val _uiState = MutableStateFlow<ViewerUiState>(ViewerUiState.Loading)
    val uiState: StateFlow<ViewerUiState> = _uiState.asStateFlow()

    init {
        loadPhotos()
    }

    private fun loadPhotos() {
        viewModelScope.launch {
            _uiState.value = ViewerUiState.Loading

            repository.getPhotos(bucketId = albumId, limit = Int.MAX_VALUE, offset = 0)
                .onSuccess { rawPhotos ->
                    val allPhotos = rawPhotos.distinctBy { it.contentUri.toString() }
                    val index = allPhotos.indexOfFirst { it.id == mediaId }.coerceAtLeast(0)
                    _uiState.value = ViewerUiState.Content(
                        photos = allPhotos,
                        initialIndex = index,
                    )
                }
                .onFailure { error ->
                    _uiState.value = ViewerUiState.Error(
                        error.message ?: "Failed to load photos",
                    )
                }
        }
    }

    private var lastTrashedItem: MediaItem? = null

    suspend fun createTrashRequest(mediaItem: MediaItem): android.content.IntentSender? {
        lastTrashedItem = mediaItem
        return repository.trashMediaItem(mediaItem)
    }

    fun onItemTrashed(mediaItem: MediaItem) {
        // Remove from the current list without destroying the viewer state
        val currentState = _uiState.value as? ViewerUiState.Content ?: return
        val newList = currentState.photos.filterNot { it.id == mediaItem.id }
        _uiState.value = currentState.copy(photos = newList)
    }

    suspend fun createUndoTrashRequest(): android.content.IntentSender? {
        val item = lastTrashedItem ?: return null
        return repository.restoreMediaItems(listOf(item))
    }

    fun onUndoOperationSuccess() {
        lastTrashedItem = null
        loadPhotos()
    }

    suspend fun createFavoriteRequest(mediaItem: MediaItem): android.content.IntentSender? {
        return repository.setFavorite(listOf(mediaItem), !mediaItem.isFavorite)
    }

    fun onFavoriteOperationSuccess(mediaId: Long, newFavoriteState: Boolean) {
        val currentState = _uiState.value as? ViewerUiState.Content ?: return
        val updated = currentState.photos.map {
            if (it.id == mediaId) it.copy(isFavorite = newFavoriteState) else it
        }
        _uiState.value = currentState.copy(photos = updated)
    }
}

