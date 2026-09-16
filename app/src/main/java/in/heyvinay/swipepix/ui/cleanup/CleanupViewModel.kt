package `in`.heyvinay.swipepix.ui.cleanup

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.ImageLoader
import coil3.request.ImageRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import `in`.heyvinay.swipepix.data.local.entity.CleaningSessionEntity
import `in`.heyvinay.swipepix.data.local.entity.DecisionType
import `in`.heyvinay.swipepix.data.model.MediaItem
import `in`.heyvinay.swipepix.data.model.MediaType
import `in`.heyvinay.swipepix.data.preferences.UserPreferencesRepository
import `in`.heyvinay.swipepix.data.repository.CleanupSessionRepository
import `in`.heyvinay.swipepix.data.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed interface CleanupUiState {
    data object Loading : CleanupUiState
    data class Error(val message: String) : CleanupUiState
    data object Empty : CleanupUiState

    data class Active(
        val currentPhoto: MediaItem?,
        val nextPhoto: MediaItem?,
        val nextNextPhoto: MediaItem?,
        val keptCount: Int,
        val trashedCount: Int,
        val totalCount: Int,
        val reviewedCount: Int,
        val canUndo: Boolean,
        val showUndo: Boolean = true,
        val lastRestoredPhoto: MediaItem? = null,
    ) : CleanupUiState

    data class Summary(
        val keptCount: Int,
        val trashedCount: Int,
        val totalReviewedCount: Int,
        val trashedPhotos: List<MediaItem>,
    ) : CleanupUiState

    data class Success(
        val trashedCount: Int,
    ) : CleanupUiState
}

data class SwipeAction(
    val mediaStoreId: Long,
    val mediaItem: MediaItem,
    val decision: DecisionType,
    val direction: SwipeDirection,
    val timestamp: Long = System.currentTimeMillis()
)

@HiltViewModel
class CleanupViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val mediaRepository: MediaRepository,
    private val sessionRepository: CleanupSessionRepository,
    private val preferencesRepository: UserPreferencesRepository,
    @param:ApplicationContext private val context: Context,
    private val imageLoader: ImageLoader,
) : ViewModel() {

    val albumId: String? = savedStateHandle["albumId"]
    val albumName: String? = savedStateHandle["albumName"]
    val isNewSession: Boolean = savedStateHandle.get<Boolean>("isNewSession") ?: false

    private val _uiState = MutableStateFlow<CleanupUiState>(CleanupUiState.Loading)
    val uiState: StateFlow<CleanupUiState> = _uiState.asStateFlow()

    private val _undoEvents = MutableSharedFlow<SwipeAction>()
    val undoEvents: SharedFlow<SwipeAction> = _undoEvents.asSharedFlow()

    private var showUndoOption = true
    val confirmBeforeApplying: StateFlow<Boolean> = preferencesRepository.confirmBeforeApplying
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private var activeSession: CleaningSessionEntity? = null
    private var initialTotalCount = 0
    private var pendingPhotos = mutableListOf<MediaItem>()
    private val undoStack = mutableListOf<SwipeAction>()

    private var keptCount = 0
    private var trashedCount = 0

    init {
        loadSession()
    }

    private fun loadSession() {
        viewModelScope.launch {
            _uiState.value = CleanupUiState.Loading

            try {
                val rememberProgress = preferencesRepository.rememberProgress.first()
                showUndoOption = preferencesRepository.showUndoOption.first()

                activeSession = if (isNewSession) {
                    sessionRepository.startNewSession(albumId, albumName)
                } else {
                    sessionRepository.getOrCreateActiveSession(albumId, albumName)
                }

                val session = activeSession ?: throw IllegalStateException("Failed to initialize cleaning session")
                val reviewedIds = if (rememberProgress) {
                    sessionRepository.getReviewedIdsForSession(session.sessionId)
                } else {
                    emptySet()
                }

                val counts = sessionRepository.getSummaryCountsForSession(session.sessionId)
                keptCount = counts.first
                trashedCount = counts.second

                mediaRepository.getPhotos(bucketId = albumId, limit = Int.MAX_VALUE, offset = 0)
                    .onSuccess { allPhotos ->
                        initialTotalCount = allPhotos.size

                        if (allPhotos.isEmpty()) {
                            _uiState.value = CleanupUiState.Empty
                            return@onSuccess
                        }

                        // Filter out already reviewed photos for this session
                        val unreviewed = if (rememberProgress) {
                            allPhotos.filter { it.id !in reviewedIds }
                        } else {
                            allPhotos
                        }

                        if (unreviewed.isEmpty()) {
                            // If there are pending trash decisions, show summary
                            val pendingTrash = sessionRepository.getPendingTrashDecisionsForSession(session.sessionId)
                            if (pendingTrash.isNotEmpty()) {
                                showSummary()
                            } else {
                                withContext(NonCancellable + Dispatchers.IO) {
                                    sessionRepository.completeSession(session.sessionId)
                                }
                                _uiState.value = CleanupUiState.Empty
                            }
                            return@onSuccess
                        }

                        val totalReviewedInSession = if (rememberProgress) {
                            sessionRepository.getTotalReviewedCountForSession(session.sessionId)
                        } else {
                            0
                        }
                        initialTotalCount = totalReviewedInSession + unreviewed.size

                        // Queue stores unreviewed photos in order
                        pendingPhotos = unreviewed.toMutableList()
                        updateActiveState()
                    }
                    .onFailure { error ->
                        _uiState.value = CleanupUiState.Error(error.message ?: "Failed to load photos")
                    }
            } catch (e: Exception) {
                _uiState.value = CleanupUiState.Error(e.message ?: "Failed to initialize session")
            }
        }
    }

    fun onSwipeRight() {
        if (pendingPhotos.isEmpty()) return
        val photo = pendingPhotos.removeAt(0)
        keptCount++

        val action = SwipeAction(
            mediaStoreId = photo.id,
            mediaItem = photo,
            decision = DecisionType.KEEP,
            direction = SwipeDirection.RIGHT
        )
        undoStack.add(action)

        viewModelScope.launch(Dispatchers.IO) {
            val sessionId = activeSession?.sessionId ?: ""
            sessionRepository.recordDecision(
                mediaStoreId = photo.id,
                contentUri = photo.contentUri.toString(),
                decision = DecisionType.KEEP,
                albumId = albumId,
                sessionId = sessionId
            )
        }

        checkCompletionOrUpdate()
    }

    fun onSwipeLeft() {
        if (pendingPhotos.isEmpty()) return
        val photo = pendingPhotos.removeAt(0)
        trashedCount++

        val action = SwipeAction(
            mediaStoreId = photo.id,
            mediaItem = photo,
            decision = DecisionType.TRASH_PENDING,
            direction = SwipeDirection.LEFT
        )
        undoStack.add(action)

        viewModelScope.launch(Dispatchers.IO) {
            val sessionId = activeSession?.sessionId ?: ""
            sessionRepository.recordDecision(
                mediaStoreId = photo.id,
                contentUri = photo.contentUri.toString(),
                decision = DecisionType.TRASH_PENDING,
                albumId = albumId,
                sessionId = sessionId
            )
        }

        checkCompletionOrUpdate()
    }

    fun onUndo() {
        if (undoStack.isEmpty()) return
        val lastAction = undoStack.removeAt(undoStack.size - 1)

        when (lastAction.decision) {
            DecisionType.KEEP -> keptCount = (keptCount - 1).coerceAtLeast(0)
            DecisionType.TRASH_PENDING -> trashedCount = (trashedCount - 1).coerceAtLeast(0)
            else -> {}
        }

        // Put photo back at the head of the queue
        pendingPhotos.add(0, lastAction.mediaItem)

        viewModelScope.launch(Dispatchers.IO) {
            sessionRepository.undoDecision(lastAction.mediaStoreId)
            _undoEvents.emit(lastAction)
        }

        updateActiveState(restoredPhoto = lastAction.mediaItem)
    }

    fun finishSessionEarly() {
        viewModelScope.launch {
            withContext(NonCancellable + Dispatchers.IO) {
                activeSession?.let { sessionRepository.pauseSession(it.sessionId) }
            }
            showSummary()
        }
    }

    fun saveAndExit(onExit: () -> Unit) {
        viewModelScope.launch {
            withContext(NonCancellable + Dispatchers.IO) {
                activeSession?.let { sessionRepository.pauseSession(it.sessionId) }
            }
            withContext(Dispatchers.Main) {
                onExit()
            }
        }
    }

    fun discardSession(onExit: () -> Unit) {
        viewModelScope.launch {
            withContext(NonCancellable + Dispatchers.IO) {
                val session = activeSession
                if (session != null) {
                    sessionRepository.discardSessionById(session.sessionId)
                } else {
                    sessionRepository.discardSession(albumId)
                }
            }
            withContext(Dispatchers.Main) {
                onExit()
            }
        }
    }

    fun completeSessionWithoutTrash(onComplete: () -> Unit) {
        viewModelScope.launch {
            withContext(NonCancellable + Dispatchers.IO) {
                if (pendingPhotos.isEmpty()) {
                    activeSession?.let { sessionRepository.completeSession(it.sessionId) }
                } else {
                    activeSession?.let { sessionRepository.pauseSession(it.sessionId) }
                }
            }
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    suspend fun createBatchTrashRequest(): android.content.IntentSender? {
        val sessionId = activeSession?.sessionId
        val pendingTrash = if (sessionId != null) {
            sessionRepository.getPendingTrashDecisionsForSession(sessionId)
        } else {
            sessionRepository.getPendingTrashDecisions(albumId)
        }
        if (pendingTrash.isEmpty()) return null

        val mediaItems = pendingTrash.map { entity ->
            MediaItem(
                id = entity.mediaStoreId,
                contentUri = Uri.parse(entity.contentUri),
                displayName = "IMG_${entity.mediaStoreId}",
                mimeType = "image/jpeg",
                dateAdded = entity.timestamp,
                dateTaken = entity.timestamp,
                dateModified = entity.timestamp,
                size = 0L,
                width = 0,
                height = 0,
                bucketId = entity.albumId ?: "",
                bucketDisplayName = "",
                mediaType = MediaType.PHOTO
            )
        }

        return mediaRepository.trashMediaItems(mediaItems)
    }

    fun onTrashOperationSuccess() {
        viewModelScope.launch {
            val sessionId = activeSession?.sessionId
            val pendingTrash = if (sessionId != null) {
                sessionRepository.getPendingTrashDecisionsForSession(sessionId)
            } else {
                sessionRepository.getPendingTrashDecisions(albumId)
            }
            val ids = pendingTrash.map { it.mediaStoreId }
            withContext(NonCancellable + Dispatchers.IO) {
                sessionRepository.markBatchAsTrashed(ids)
                if (pendingPhotos.isEmpty()) {
                    activeSession?.let { sessionRepository.completeSession(it.sessionId) }
                } else {
                    activeSession?.let { sessionRepository.pauseSession(it.sessionId) }
                }
            }

            _uiState.value = CleanupUiState.Success(trashedCount = ids.size)
        }
    }

    private suspend fun showSummary() {
        val sessionId = activeSession?.sessionId
        val pendingTrash = if (sessionId != null) {
            sessionRepository.getPendingTrashDecisionsForSession(sessionId)
        } else {
            sessionRepository.getPendingTrashDecisions(albumId)
        }
        val counts = if (sessionId != null) {
            sessionRepository.getSummaryCountsForSession(sessionId)
        } else {
            sessionRepository.getSummaryCounts(albumId)
        }

        val trashedItems = pendingTrash.map { entity ->
            MediaItem(
                id = entity.mediaStoreId,
                contentUri = Uri.parse(entity.contentUri),
                displayName = "IMG_${entity.mediaStoreId}",
                mimeType = "image/jpeg",
                dateAdded = entity.timestamp,
                dateTaken = entity.timestamp,
                dateModified = entity.timestamp,
                size = 0L,
                width = 0,
                height = 0,
                bucketId = entity.albumId ?: "",
                bucketDisplayName = "",
                mediaType = MediaType.PHOTO
            )
        }

        _uiState.value = CleanupUiState.Summary(
            keptCount = counts.first,
            trashedCount = counts.second,
            totalReviewedCount = counts.first + counts.second,
            trashedPhotos = trashedItems
        )
    }

    private fun checkCompletionOrUpdate() {
        if (pendingPhotos.isEmpty()) {
            viewModelScope.launch {
                showSummary()
            }
        } else {
            updateActiveState()
        }
    }

    private fun updateActiveState(restoredPhoto: MediaItem? = null) {
        val current = pendingPhotos.getOrNull(0)
        val next = pendingPhotos.getOrNull(1)
        val nextNext = pendingPhotos.getOrNull(2)

        val reviewedCount = initialTotalCount - pendingPhotos.size

        _uiState.value = CleanupUiState.Active(
            currentPhoto = current,
            nextPhoto = next,
            nextNextPhoto = nextNext,
            keptCount = keptCount,
            trashedCount = trashedCount,
            totalCount = initialTotalCount,
            reviewedCount = reviewedCount.coerceAtLeast(0),
            canUndo = undoStack.isNotEmpty() && showUndoOption,
            showUndo = showUndoOption,
            lastRestoredPhoto = restoredPhoto
        )

        preloadUpcomingPhotos()
    }

    private fun preloadUpcomingPhotos() {
        try {
            val nextToPreload = listOfNotNull(
                pendingPhotos.getOrNull(1),
                pendingPhotos.getOrNull(2)
            )

            for (photo in nextToPreload) {
                // Preload thumbnail for Frame 0 instant display
                val thumbRequest = ImageRequest.Builder(context)
                    .data(photo.contentUri)
                    .memoryCacheKey("${photo.contentUri}_256")
                    .size(256, 256)
                    .build()
                imageLoader.enqueue(thumbRequest)

                // Preload card-resolution image
                val cardRequest = ImageRequest.Builder(context)
                    .data(photo.contentUri)
                    .memoryCacheKey("${photo.contentUri}_card")
                    .size(1080, 1920)
                    .build()
                imageLoader.enqueue(cardRequest)
            }
        } catch (_: Exception) {
            // Preloading failure is non-fatal (e.g. headless unit tests)
        }
    }
}
