package `in`.heyvinay.swipepix.ui.cleanup

import android.net.FakeUri
import `in`.heyvinay.swipepix.data.local.entity.DecisionType
import `in`.heyvinay.swipepix.data.model.Album
import `in`.heyvinay.swipepix.data.model.MediaItem
import `in`.heyvinay.swipepix.data.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CleanupSourceSelectionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mediaRepository: TestableMediaRepository
    private lateinit var sessionRepository: FakeCleanupSessionRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mediaRepository = TestableMediaRepository()
        sessionRepository = FakeCleanupSessionRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testContentState_calculatesTotalCountsAndSortsCameraFirst() = runTest {
        val cameraAlbum = Album(
            id = "camera_id",
            displayName = "Camera",
            mediaCount = 500,
            photoCount = 450,
            videoCount = 50,
            coverUri = FakeUri("content://media/camera/cover"),
        )
        val screenshotsAlbum = Album(
            id = "screenshots_id",
            displayName = "Screenshots",
            mediaCount = 1000, // Higher count than Camera, but Camera must be sorted first!
            photoCount = 980,
            videoCount = 20,
            coverUri = FakeUri("content://media/screenshots/cover"),
        )
        val downloadsAlbum = Album(
            id = "downloads_id",
            displayName = "Downloads",
            mediaCount = 200,
            photoCount = 190,
            videoCount = 10,
            coverUri = FakeUri("content://media/downloads/cover"),
        )

        mediaRepository.albumsResult = Result.success(listOf(screenshotsAlbum, downloadsAlbum, cameraAlbum))

        // Pre-populate some reviewed decisions
        // 10 reviewed in Camera
        sessionRepository.recordDecision(1L, "uri1", DecisionType.KEEP, "camera_id", "s1")
        sessionRepository.recordDecision(2L, "uri2", DecisionType.TRASH_PENDING, "camera_id", "s1")

        // 5 reviewed in All Photos (albumId = null)
        sessionRepository.recordDecision(10L, "uri10", DecisionType.KEEP, null, "s1")

        val viewModel = CleanupSourceSelectionViewModel(mediaRepository, sessionRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Content state, got $state", state is SourceSelectionUiState.Content)
        val content = state as SourceSelectionUiState.Content

        // Check All Photos aggregation
        assertEquals(450 + 980 + 190, content.allPhotos.photoCount)
        assertEquals(50 + 20 + 10, content.allPhotos.videoCount)
        assertEquals(1, content.allPhotos.reviewedCount) // 1 reviewed with albumId = null

        // Check album sorting: Camera first (priority name), then Screenshots (1000), then Downloads (200)
        assertEquals(3, content.albums.size)
        assertEquals("Camera", content.albums[0].album.displayName)
        assertEquals(2, content.albums[0].reviewedCount) // 1 keep + 1 trash = 2 reviewed

        assertEquals("Screenshots", content.albums[1].album.displayName)
        assertEquals(0, content.albums[1].reviewedCount)

        assertEquals("Downloads", content.albums[2].album.displayName)
        assertEquals(0, content.albums[2].reviewedCount)
    }

    @Test
    fun testEmptyState_whenNoAlbumsReturned() = runTest {
        mediaRepository.albumsResult = Result.success(emptyList())

        val viewModel = CleanupSourceSelectionViewModel(mediaRepository, sessionRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Empty state, got $state", state is SourceSelectionUiState.Empty)
    }

    @Test
    fun testErrorState_whenRepositoryFails() = runTest {
        mediaRepository.albumsResult = Result.failure(RuntimeException("Disk query failed"))

        val viewModel = CleanupSourceSelectionViewModel(mediaRepository, sessionRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Error state, got $state", state is SourceSelectionUiState.Error)
        assertEquals("Disk query failed", (state as SourceSelectionUiState.Error).message)
    }

    @Test
    fun testResumableState_falseWhenAllPhotosReviewed() = runTest {
        val smallAlbum = Album(
            id = "small_album",
            displayName = "Small",
            mediaCount = 2,
            photoCount = 2,
            videoCount = 0,
            coverUri = null,
        )
        mediaRepository.albumsResult = Result.success(listOf(smallAlbum))

        // Create a session and review both items
        val session = sessionRepository.startNewSession("small_album", "Small")
        sessionRepository.recordDecision(1L, "uri1", DecisionType.KEEP, "small_album", session.sessionId)
        sessionRepository.recordDecision(2L, "uri2", DecisionType.KEEP, "small_album", session.sessionId)

        val viewModel = CleanupSourceSelectionViewModel(mediaRepository, sessionRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is SourceSelectionUiState.Content)
        val content = state as SourceSelectionUiState.Content

        val item = content.albums.first { it.album.id == "small_album" }
        assertEquals(2, item.reviewedCount)
        // Since all 2 items were reviewed, hasResumableSession must be FALSE
        org.junit.Assert.assertFalse(item.hasResumableSession)
    }

    private class TestableMediaRepository : MediaRepository {
        var albumsResult: Result<List<Album>> = Result.success(emptyList())

        override suspend fun getAlbums(): Result<List<Album>> = albumsResult

        override suspend fun getMediaCount(bucketId: String?): Result<`in`.heyvinay.swipepix.data.model.AlbumMediaCount> {
            val albums = albumsResult.getOrNull() ?: emptyList()
            val pCount = albums.sumOf { it.photoCount }
            val vCount = albums.sumOf { it.videoCount }
            return Result.success(`in`.heyvinay.swipepix.data.model.AlbumMediaCount(totalCount = pCount + vCount, photoCount = pCount, videoCount = vCount))
        }

        override suspend fun getPhotos(
            bucketId: String?,
            limit: Int,
            offset: Int,
            filter: `in`.heyvinay.swipepix.data.model.GalleryFilterCategory,
        ): Result<List<MediaItem>> = Result.success(emptyList())

        override suspend fun getPhotoById(id: Long): Result<MediaItem?> = Result.success(null)

        override fun observeChanges(): Flow<Unit> = flowOf()

        override suspend fun trashMediaItem(mediaItem: MediaItem): android.content.IntentSender? = null

        override suspend fun trashMediaItems(mediaItems: List<MediaItem>): android.content.IntentSender? = null

        override suspend fun getTrashedMedia(): Result<List<MediaItem>> = Result.success(emptyList())

        override suspend fun restoreMediaItems(mediaItems: List<MediaItem>): android.content.IntentSender? = null

        override suspend fun deleteMediaItemsPermanently(mediaItems: List<MediaItem>): android.content.IntentSender? = null

        override suspend fun setFavorite(mediaItems: List<MediaItem>, isFavorite: Boolean): android.content.IntentSender? = null
    }
}
