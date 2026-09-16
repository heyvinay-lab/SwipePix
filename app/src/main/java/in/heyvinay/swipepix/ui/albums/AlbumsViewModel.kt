package `in`.heyvinay.swipepix.ui.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.heyvinay.swipepix.data.model.Album
import `in`.heyvinay.swipepix.data.repository.MediaRepository
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
 * UI state for the albums screen.
 */
sealed interface AlbumsUiState {
    data object Loading : AlbumsUiState
    data object Empty : AlbumsUiState
    data class Error(val message: String) : AlbumsUiState
    data class Content(
        val albums: List<Album>,
        val isRefreshing: Boolean = false,
    ) : AlbumsUiState
}

@HiltViewModel
class AlbumsViewModel @Inject constructor(
    private val repository: MediaRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AlbumsUiState>(AlbumsUiState.Loading)
    val uiState: StateFlow<AlbumsUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadAlbums(isRefresh = false)
        observeMediaChanges()
    }

    private fun loadAlbums(isRefresh: Boolean = false) {
        val currentContent = _uiState.value as? AlbumsUiState.Content
        if (isRefresh && currentContent != null) {
            _uiState.value = currentContent.copy(isRefreshing = true)
        } else if (_uiState.value !is AlbumsUiState.Content) {
            _uiState.value = AlbumsUiState.Loading
        }

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            repository.getAlbums()
                .onSuccess { albums ->
                    val unique = albums.distinctBy { it.id }
                    _uiState.value = if (unique.isEmpty()) {
                        AlbumsUiState.Empty
                    } else {
                        AlbumsUiState.Content(unique, isRefreshing = false)
                    }
                }
                .onFailure { error ->
                    if (_uiState.value is AlbumsUiState.Content) {
                        _uiState.value = (_uiState.value as AlbumsUiState.Content).copy(isRefreshing = false)
                    } else {
                        _uiState.value = AlbumsUiState.Error(
                            error.message ?: "Failed to load albums",
                        )
                    }
                }
        }
    }

    fun refresh() {
        loadAlbums(isRefresh = true)
    }

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    private fun observeMediaChanges() {
        repository.observeChanges()
            .debounce(500L)
            .onEach { refresh() }
            .launchIn(viewModelScope)
    }
}
