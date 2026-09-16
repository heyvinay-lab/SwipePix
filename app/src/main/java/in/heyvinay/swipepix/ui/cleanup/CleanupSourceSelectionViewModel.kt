package `in`.heyvinay.swipepix.ui.cleanup

import android.net.Uri
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

data class AllPhotosSource(
    val photoCount: Int,
    val videoCount: Int,
    val reviewedCount: Int = 0,
    val coverUri: Uri? = null,
    val hasResumableSession: Boolean = false,
)

data class AlbumSourceItem(
    val album: Album,
    val reviewedCount: Int = 0,
    val hasResumableSession: Boolean = false,
)

sealed interface SourceSelectionUiState {
    data object Loading : SourceSelectionUiState
    data object Empty : SourceSelectionUiState
    data class Error(val message: String) : SourceSelectionUiState
    data class Content(
        val allPhotos: AllPhotosSource,
        val albums: List<AlbumSourceItem>,
    ) : SourceSelectionUiState
}

@HiltViewModel
class CleanupSourceSelectionViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val sessionRepository: CleanupSessionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SourceSelectionUiState>(SourceSelectionUiState.Loading)
    val uiState: StateFlow<SourceSelectionUiState> = _uiState.asStateFlow()

    private val priorityNames = setOf("camera", "dcim", "recents")

    init {
        loadSources()
        observeMediaChanges()
    }

    fun loadSources() {
        viewModelScope.launch {
            _uiState.value = SourceSelectionUiState.Loading

            mediaRepository.getAlbums()
                .onSuccess { albums ->
                    if (albums.isEmpty()) {
                        _uiState.value = SourceSelectionUiState.Empty
                        return@onSuccess
                    }

                    // Sort deterministically: Camera/DCIM first, then by media count descending
                    val sortedAlbums = albums.sortedWith(
                        compareByDescending<Album> { it.displayName.lowercase().trim() in priorityNames }
                            .thenByDescending { it.mediaCount }
                    )

                    val libraryCounts = mediaRepository.getMediaCount(null).getOrNull()
                    val totalPhotos = libraryCounts?.photoCount ?: albums.sumOf { it.photoCount }
                    val totalVideos = libraryCounts?.videoCount ?: albums.sumOf { it.videoCount }

                    val totalMedia = libraryCounts?.totalCount ?: (totalPhotos + totalVideos)

                    // Retrieve resumable session for all-photos if exists
                    val resumableAll = sessionRepository.getResumableSession(null)
                    val allPhotosReviewed = if (resumableAll != null) {
                        sessionRepository.getTotalReviewedCountForSession(resumableAll.sessionId)
                    } else {
                        sessionRepository.getTotalReviewedCount(null)
                    }
                    val hasResumableAll = resumableAll != null && allPhotosReviewed > 0 && (totalMedia <= 0 || allPhotosReviewed < totalMedia)

                    // Cover URI for All Photos: top album's cover or first available
                    val allPhotosCover = sortedAlbums.firstOrNull { it.coverUri != null }?.coverUri

                    val allPhotosSource = AllPhotosSource(
                        photoCount = totalPhotos,
                        videoCount = totalVideos,
                        reviewedCount = allPhotosReviewed,
                        coverUri = allPhotosCover,
                        hasResumableSession = hasResumableAll,
                    )

                    // Retrieve reviewed counts and resumable state for each album
                    val albumSourceItems = sortedAlbums.map { album ->
                        val resumable = sessionRepository.getResumableSession(album.id)
                        val reviewed = if (resumable != null) {
                            sessionRepository.getTotalReviewedCountForSession(resumable.sessionId)
                        } else {
                            sessionRepository.getTotalReviewedCount(album.id)
                        }
                        AlbumSourceItem(
                            album = album,
                            reviewedCount = reviewed,
                            hasResumableSession = resumable != null && reviewed > 0 && (album.mediaCount <= 0 || reviewed < album.mediaCount),
                        )
                    }

                    _uiState.value = SourceSelectionUiState.Content(
                        allPhotos = allPhotosSource,
                        albums = albumSourceItems,
                    )
                }
                .onFailure { error ->
                    _uiState.value = SourceSelectionUiState.Error(
                        error.message ?: "Failed to load albums",
                    )
                }
        }
    }

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    private fun observeMediaChanges() {
        mediaRepository.observeChanges()
            .debounce(500L)
            .onEach { loadSources() }
            .launchIn(viewModelScope)
    }
}
