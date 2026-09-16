package `in`.heyvinay.swipepix.ui.viewer

import android.net.FakeUri
import androidx.lifecycle.SavedStateHandle
import `in`.heyvinay.swipepix.data.model.Album
import `in`.heyvinay.swipepix.data.model.AlbumMediaCount
import `in`.heyvinay.swipepix.data.model.GalleryFilterCategory
import `in`.heyvinay.swipepix.data.model.MediaItem
import `in`.heyvinay.swipepix.data.model.MediaType
import `in`.heyvinay.swipepix.data.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PhotoViewerDeleteTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeViewerMediaRepository
    private lateinit var viewModel: PhotoViewerViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeViewerMediaRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadPhotos_initializesCorrectInitialIndex() = runTest {
        val p1 = createMediaItem(10L, "p1.jpg")
        val p2 = createMediaItem(20L, "p2.jpg")
        val p3 = createMediaItem(30L, "p3.jpg")
        fakeRepository.items = listOf(p1, p2, p3)

        val handle = SavedStateHandle(mapOf("mediaId" to 20L))
        viewModel = PhotoViewerViewModel(handle, fakeRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ViewerUiState.Content)
        val content = state as ViewerUiState.Content
        assertEquals(3, content.photos.size)
        assertEquals(1, content.initialIndex)
        assertEquals(20L, content.photos[content.initialIndex].id)
    }

    @Test
    fun onItemTrashed_removesItemSmoothly() = runTest {
        val p1 = createMediaItem(10L, "p1.jpg")
        val p2 = createMediaItem(20L, "p2.jpg")
        val p3 = createMediaItem(30L, "p3.jpg")
        fakeRepository.items = listOf(p1, p2, p3)

        val handle = SavedStateHandle(mapOf("mediaId" to 20L))
        viewModel = PhotoViewerViewModel(handle, fakeRepository)
        advanceUntilIdle()

        viewModel.createTrashRequest(p2)
        assertEquals(p2, fakeRepository.lastTrashedItem)

        viewModel.onItemTrashed(p2)
        val content = viewModel.uiState.value as ViewerUiState.Content
        assertEquals(2, content.photos.size)
        assertEquals(listOf(10L, 30L), content.photos.map { it.id })
    }

    @Test
    fun onItemTrashed_whenLastItemDeleted_emptiesListForAutoExit() = runTest {
        val p1 = createMediaItem(10L, "p1.jpg")
        fakeRepository.items = listOf(p1)

        val handle = SavedStateHandle(mapOf("mediaId" to 10L))
        viewModel = PhotoViewerViewModel(handle, fakeRepository)
        advanceUntilIdle()

        viewModel.createTrashRequest(p1)
        viewModel.onItemTrashed(p1)

        val content = viewModel.uiState.value as ViewerUiState.Content
        assertTrue("Photos list must be empty to trigger auto back navigation", content.photos.isEmpty())
    }

    @Test
    fun undoTrash_restoresItemAndReloadsPhotos() = runTest {
        val p1 = createMediaItem(10L, "p1.jpg")
        fakeRepository.items = listOf(p1)

        val handle = SavedStateHandle(mapOf("mediaId" to 10L))
        viewModel = PhotoViewerViewModel(handle, fakeRepository)
        advanceUntilIdle()

        viewModel.createTrashRequest(p1)
        viewModel.onItemTrashed(p1)

        viewModel.createUndoTrashRequest()
        assertEquals(listOf(p1), fakeRepository.lastRestoredItems)

        viewModel.onUndoOperationSuccess()
        advanceUntilIdle()

        val content = viewModel.uiState.value as ViewerUiState.Content
        assertEquals(1, content.photos.size)
    }

    @Test
    fun setFavorite_togglesFavoriteStateSuccessfully() = runTest {
        val p1 = createMediaItem(10L, "p1.jpg")
        fakeRepository.items = listOf(p1)

        val handle = SavedStateHandle(mapOf("mediaId" to 10L))
        viewModel = PhotoViewerViewModel(handle, fakeRepository)
        advanceUntilIdle()

        viewModel.createFavoriteRequest(p1)
        assertEquals(listOf(p1), fakeRepository.lastFavoriteItems)
        assertEquals(true, fakeRepository.lastFavoriteState)

        viewModel.onFavoriteOperationSuccess(10L, true)
        val content = viewModel.uiState.value as ViewerUiState.Content
        assertTrue(content.photos.first().isFavorite)
    }

    private fun createMediaItem(id: Long, name: String): MediaItem {
        return MediaItem(
            id = id,
            contentUri = FakeUri("content://fake/$id"),
            displayName = name,
            mimeType = "image/jpeg",
            dateAdded = 1000L,
            dateTaken = 1000000L,
            dateModified = 1000L,
            size = 1024L,
            width = 100,
            height = 100,
            bucketId = "bucket_1",
            bucketDisplayName = "Camera",
            mediaType = MediaType.PHOTO,
            isFavorite = false,
        )
    }

    private class FakeViewerMediaRepository : MediaRepository {
        var items = listOf<MediaItem>()
        var lastTrashedItem: MediaItem? = null
        var lastRestoredItems: List<MediaItem>? = null
        var lastFavoriteItems: List<MediaItem>? = null
        var lastFavoriteState: Boolean? = null

        override suspend fun getPhotos(
            bucketId: String?,
            limit: Int,
            offset: Int,
            filter: GalleryFilterCategory,
        ): Result<List<MediaItem>> = Result.success(items)

        override suspend fun getAlbums(): Result<List<Album>> = Result.success(emptyList())

        override suspend fun getMediaCount(bucketId: String?): Result<AlbumMediaCount> =
            Result.success(AlbumMediaCount(items.size, items.size, 0, 0))

        override suspend fun getPhotoById(id: Long): Result<MediaItem?> = Result.success(null)

        override fun observeChanges(): Flow<Unit> = flowOf()

        override suspend fun trashMediaItem(mediaItem: MediaItem): android.content.IntentSender? {
            lastTrashedItem = mediaItem
            return null
        }

        override suspend fun trashMediaItems(mediaItems: List<MediaItem>): android.content.IntentSender? = null

        override suspend fun getTrashedMedia(): Result<List<MediaItem>> = Result.success(emptyList())

        override suspend fun restoreMediaItems(mediaItems: List<MediaItem>): android.content.IntentSender? {
            lastRestoredItems = mediaItems
            return null
        }

        override suspend fun deleteMediaItemsPermanently(mediaItems: List<MediaItem>): android.content.IntentSender? = null

        override suspend fun setFavorite(mediaItems: List<MediaItem>, isFavorite: Boolean): android.content.IntentSender? {
            lastFavoriteItems = mediaItems
            lastFavoriteState = isFavorite
            return null
        }
    }
}
