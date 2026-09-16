package `in`.heyvinay.swipepix.ui.gallery

import android.net.FakeUri
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
class GalleryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeTestMediaRepository
    private lateinit var viewModel: GalleryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeTestMediaRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadInitial_emitsAuthoritativeCountsAndPhotos() = runTest {
        val photo1 = createMediaItem(1L, "photo1.jpg", MediaType.PHOTO, isFav = false)
        val video1 = createMediaItem(2L, "video1.mp4", MediaType.VIDEO, isFav = true)
        fakeRepository.items = listOf(photo1, video1)
        fakeRepository.authoritativeCounts = AlbumMediaCount(
            totalCount = 2000,
            photoCount = 1800,
            videoCount = 200,
            favoriteCount = 50,
        )

        viewModel = GalleryViewModel(fakeRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Content state, got $state", state is GalleryUiState.Content)
        val content = state as GalleryUiState.Content
        assertEquals(2000, content.totalCount)
        assertEquals(1800, content.photoCount)
        assertEquals(200, content.videoCount)
        assertEquals(GalleryFilterCategory.ALL, content.selectedFilter)
        assertEquals(2, content.dateGroups.flatMap { it.photos }.size)
    }

    @Test
    fun setFilter_toVideos_reQueriesAndAppliesAuthoritativeVideoCount() = runTest {
        val video = createMediaItem(2L, "video1.mp4", MediaType.VIDEO, isFav = false)
        fakeRepository.items = listOf(video)
        fakeRepository.authoritativeCounts = AlbumMediaCount(
            totalCount = 2000,
            photoCount = 1800,
            videoCount = 200,
            favoriteCount = 50,
        )

        viewModel = GalleryViewModel(fakeRepository)
        advanceUntilIdle()

        viewModel.setFilter(GalleryFilterCategory.VIDEOS)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Content state, got $state", state is GalleryUiState.Content)
        val content = state as GalleryUiState.Content
        assertEquals(GalleryFilterCategory.VIDEOS, content.selectedFilter)
        assertEquals(200, content.totalCount)
        assertEquals(200, content.videoCount)
        assertEquals(0, content.photoCount)
        assertEquals(GalleryFilterCategory.VIDEOS, fakeRepository.lastRequestedFilter)
    }

    @Test
    fun setFilter_toFavorites_reQueriesAndAppliesAuthoritativeFavoriteCount() = runTest {
        val fav = createMediaItem(3L, "fav.jpg", MediaType.PHOTO, isFav = true)
        fakeRepository.items = listOf(fav)
        fakeRepository.authoritativeCounts = AlbumMediaCount(
            totalCount = 2000,
            photoCount = 1800,
            videoCount = 200,
            favoriteCount = 42,
        )

        viewModel = GalleryViewModel(fakeRepository)
        advanceUntilIdle()

        viewModel.setFilter(GalleryFilterCategory.FAVORITES)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Content state, got $state", state is GalleryUiState.Content)
        val content = state as GalleryUiState.Content
        assertEquals(GalleryFilterCategory.FAVORITES, content.selectedFilter)
        assertEquals(42, content.totalCount)
        assertEquals(GalleryFilterCategory.FAVORITES, fakeRepository.lastRequestedFilter)
    }

    @Test
    fun refresh_whileContentPresent_preservesPhotosAndDoesNotEmitLoading() = runTest {
        val photo1 = createMediaItem(1L, "photo1.jpg", MediaType.PHOTO, isFav = false)
        fakeRepository.items = listOf(photo1)
        fakeRepository.authoritativeCounts = AlbumMediaCount(100, 100, 0, 0)

        viewModel = GalleryViewModel(fakeRepository)
        advanceUntilIdle()

        val initialContent = viewModel.uiState.value
        assertTrue("Initial state must be Content", initialContent is GalleryUiState.Content)
        assertEquals(1, (initialContent as GalleryUiState.Content).dateGroups.flatMap { it.photos }.size)

        // Add a second photo and refresh
        val photo2 = createMediaItem(2L, "photo2.jpg", MediaType.PHOTO, isFav = false)
        fakeRepository.items = listOf(photo1, photo2)
        fakeRepository.authoritativeCounts = AlbumMediaCount(101, 101, 0, 0)

        // Calling refresh must keep state as Content (with isRefreshing = true during query)
        viewModel.refresh()
        // Prior to advancing idle, state must remain Content (never replaced with Loading!)
        val stateDuringRefresh = viewModel.uiState.value
        assertTrue("State during refresh must remain Content, got $stateDuringRefresh", stateDuringRefresh is GalleryUiState.Content)
        assertEquals(true, (stateDuringRefresh as GalleryUiState.Content).isRefreshing)
        assertEquals(1, stateDuringRefresh.dateGroups.flatMap { it.photos }.size)

        // When query finishes, content is atomically updated with new items and isRefreshing = false
        advanceUntilIdle()
        val stateAfterRefresh = viewModel.uiState.value
        assertTrue("State after refresh must be Content", stateAfterRefresh is GalleryUiState.Content)
        val finalContent = stateAfterRefresh as GalleryUiState.Content
        assertEquals(false, finalContent.isRefreshing)
        assertEquals(2, finalContent.dateGroups.flatMap { it.photos }.size)
        assertEquals(101, finalContent.totalCount)
    }

    @Test
    fun refresh_whenMediaStoreFails_retainsExistingContent() = runTest {
        val photo1 = createMediaItem(1L, "photo1.jpg", MediaType.PHOTO, isFav = false)
        fakeRepository.items = listOf(photo1)
        fakeRepository.authoritativeCounts = AlbumMediaCount(50, 50, 0, 0)

        viewModel = GalleryViewModel(fakeRepository)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is GalleryUiState.Content)

        // Make repository fail on subsequent queries
        fakeRepository.shouldFailPhotos = true
        viewModel.refresh()
        advanceUntilIdle()

        // Content must still be visible, NOT replaced with Error or Empty!
        val state = viewModel.uiState.value
        assertTrue("Expected Content state retained on refresh failure, got $state", state is GalleryUiState.Content)
        val content = state as GalleryUiState.Content
        assertEquals(false, content.isRefreshing)
        assertEquals(1, content.dateGroups.flatMap { it.photos }.size)
        assertEquals(50, content.totalCount)
    }

    private fun createMediaItem(id: Long, name: String, type: MediaType, isFav: Boolean): MediaItem {
        return MediaItem(
            id = id,
            contentUri = FakeUri("content://fake/$id"),
            displayName = name,
            mimeType = if (type == MediaType.VIDEO) "video/mp4" else "image/jpeg",
            dateAdded = 1000L,
            dateTaken = 1000000L,
            dateModified = 1000L,
            size = 1024L,
            width = 100,
            height = 100,
            bucketId = "bucket_1",
            bucketDisplayName = "Camera",
            mediaType = type,
            isFavorite = isFav,
        )
    }

    private class FakeTestMediaRepository : MediaRepository {
        var items = listOf<MediaItem>()
        var authoritativeCounts: AlbumMediaCount? = null
        var lastRequestedFilter: GalleryFilterCategory? = null
        var shouldFailPhotos: Boolean = false

        override suspend fun getPhotos(
            bucketId: String?,
            limit: Int,
            offset: Int,
            filter: GalleryFilterCategory,
        ): Result<List<MediaItem>> {
            lastRequestedFilter = filter
            if (shouldFailPhotos) {
                return Result.failure(RuntimeException("MediaStore query failure"))
            }
            return Result.success(items)
        }

        override suspend fun getAlbums(): Result<List<Album>> = Result.success(emptyList())

        override suspend fun getMediaCount(bucketId: String?): Result<AlbumMediaCount> {
            return Result.success(authoritativeCounts ?: AlbumMediaCount(items.size, items.size, 0, 0))
        }

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
