package `in`.heyvinay.swipepix.ui.albums

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.heyvinay.swipepix.data.model.Album
import `in`.heyvinay.swipepix.data.repository.CleanupSessionRepository
import `in`.heyvinay.swipepix.data.repository.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the album detail landing screen.
 */
sealed interface AlbumDetailUiState {
    data object Loading : AlbumDetailUiState
    data class Error(val message: String) : AlbumDetailUiState
    data class Content(
        val albumName: String,
        val coverUri: Uri?,
        val photoCount: Int,
        val videoCount: Int,
        val totalCount: Int,
        val reviewedCount: Int,
        val hasResumableSession: Boolean = false,
    ) : AlbumDetailUiState
}

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val mediaRepository: MediaRepository,
    private val sessionRepository: CleanupSessionRepository,
) : ViewModel() {

    val albumId: String = checkNotNull(savedStateHandle["albumId"])
    val albumName: String = checkNotNull(savedStateHandle["albumName"])

    private val _uiState = MutableStateFlow<AlbumDetailUiState>(AlbumDetailUiState.Loading)
    val uiState: StateFlow<AlbumDetailUiState> = _uiState.asStateFlow()

    init {
        loadAlbumDetail()
        observeMediaChanges()
    }

    private fun loadAlbumDetail() {
        viewModelScope.launch {
            _uiState.value = AlbumDetailUiState.Loading

            val countResult = mediaRepository.getMediaCount(albumId)
            val firstPhotoResult = mediaRepository.getPhotos(bucketId = albumId, limit = 1, offset = 0)

            countResult.onSuccess { counts ->
                val coverUri = firstPhotoResult.getOrNull()?.firstOrNull()?.contentUri
                val resumableSession = sessionRepository.getResumableSession(albumId)
                val reviewedCount = if (resumableSession != null) {
                    sessionRepository.getTotalReviewedCountForSession(resumableSession.sessionId)
                } else {
                    sessionRepository.getTotalReviewedCount(albumId)
                }
                val hasResumable = resumableSession != null && reviewedCount > 0 && (counts.totalCount <= 0 || reviewedCount < counts.totalCount)

                _uiState.value = AlbumDetailUiState.Content(
                    albumName = albumName,
                    coverUri = coverUri,
                    photoCount = counts.photoCount,
                    videoCount = counts.videoCount,
                    totalCount = counts.totalCount,
                    reviewedCount = reviewedCount,
                    hasResumableSession = hasResumable,
                )
            }.onFailure { error ->
                _uiState.value = AlbumDetailUiState.Error(
                    error.message ?: "Failed to load album",
                )
            }
        }
    }

    fun refresh() {
        loadAlbumDetail()
    }

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    private fun observeMediaChanges() {
        mediaRepository.observeChanges()
            .debounce(500L)
            .onEach { loadAlbumDetail() }
            .launchIn(viewModelScope)
    }
}
