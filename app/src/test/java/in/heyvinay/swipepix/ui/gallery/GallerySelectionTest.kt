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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GallerySelectionTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeSelectionMediaRepository
    private lateinit var viewModel: GalleryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeSelectionMediaRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun enterSelection_addsItemAndEnablesSelectionMode() = runTest {
        val photo1 = createMediaItem(1L, "photo1.jpg")
        val photo2 = createMediaItem(2L, "photo2.jpg")
        fakeRepository.items = listOf(photo1, photo2)

        viewModel = GalleryViewModel(fakeRepository)
        advanceUntilIdle()

        viewModel.enterSelection(photo1)
        val content = viewModel.uiState.value as GalleryUiState.Content
        assertTrue(content.isSelectionMode)
        assertEquals(setOf(1L), content.selectedIds)
    }

    @Test
    fun toggleSelection_addsAndRemovesItems() = runTest {
        val photo1 = createMediaItem(1L, "photo1.jpg")
        val photo2 = createMediaItem(2L, "photo2.jpg")
        fakeRepository.items = listOf(photo1, photo2)

        viewModel = GalleryViewModel(fakeRepository)
        advanceUntilIdle()

        viewModel.toggleSelection(photo1)
        var content = viewModel.uiState.value as GalleryUiState.Content
        assertTrue(content.isSelectionMode)
        assertEquals(setOf(1L), content.selectedIds)

        viewModel.toggleSelection(photo2)
        content = viewModel.uiState.value as GalleryUiState.Content
        assertEquals(setOf(1L, 2L), content.selectedIds)

        viewModel.toggleSelection(photo1)
        content = viewModel.uiState.value as GalleryUiState.Content
        assertEquals(setOf(2L), content.selectedIds)
        assertTrue(content.isSelectionMode)

        viewModel.toggleSelection(photo2)
        content = viewModel.uiState.value as GalleryUiState.Content
        assertEquals(emptySet<Long>(), content.selectedIds)
        assertFalse(content.isSelectionMode)
    }

    @Test
    fun toggleSelectAll_selectsAllThenClearsAll() = runTest {
        val photo1 = createMediaItem(1L, "photo1.jpg")
        val photo2 = createMediaItem(2L, "photo2.jpg")
        val photo3 = createMediaItem(3L, "photo3.jpg")
        fakeRepository.items = listOf(photo1, photo2, photo3)

        viewModel = GalleryViewModel(fakeRepository)
        advanceUntilIdle()

        viewModel.toggleSelectAll()
        var content = viewModel.uiState.value as GalleryUiState.Content
        assertTrue(content.isSelectionMode)
        assertEquals(setOf(1L, 2L, 3L), content.selectedIds)

        // Toggling again when all are selected should deselect all
        viewModel.toggleSelectAll()
        content = viewModel.uiState.value as GalleryUiState.Content
        assertFalse(content.isSelectionMode)
        assertEquals(emptySet<Long>(), content.selectedIds)
    }

    @Test
    fun clearSelection_resetsSelectionMode() = runTest {
        val photo1 = createMediaItem(1L, "photo1.jpg")
        fakeRepository.items = listOf(photo1)

        viewModel = GalleryViewModel(fakeRepository)
        advanceUntilIdle()

        viewModel.enterSelection(photo1)
        assertTrue((viewModel.uiState.value as GalleryUiState.Content).isSelectionMode)

        viewModel.clearSelection()
        val content = viewModel.uiState.value as GalleryUiState.Content
        assertFalse(content.isSelectionMode)
        assertEquals(emptySet<Long>(), content.selectedIds)
    }

    @Test
    fun batchTrash_removesItemsAndSupportsUndo() = runTest {
        val photo1 = createMediaItem(1L, "photo1.jpg")
        val photo2 = createMediaItem(2L, "photo2.jpg")
        fakeRepository.items = listOf(photo1, photo2)

        viewModel = GalleryViewModel(fakeRepository)
        advanceUntilIdle()

        viewModel.enterSelection(photo1)
        viewModel.createTrashRequestForSelected()
        assertEquals(listOf(photo1), fakeRepository.lastTrashedItems)

        // Simulate successful system trash
        fakeRepository.items = listOf(photo2)
        viewModel.onTrashOperationSuccess()
        advanceUntilIdle()

        val contentAfterTrash = viewModel.uiState.value as GalleryUiState.Content
        assertFalse(contentAfterTrash.isSelectionMode)
        assertEquals(emptySet<Long>(), contentAfterTrash.selectedIds)
        assertEquals(1, contentAfterTrash.dateGroups.flatMap { it.photos }.size)

        // Undo trash
        viewModel.createUndoTrashRequest()
        assertEquals(listOf(photo1), fakeRepository.lastRestoredItems)

        fakeRepository.items = listOf(photo1, photo2)
        viewModel.onUndoOperationSuccess()
        advanceUntilIdle()

        val contentAfterUndo = viewModel.uiState.value as GalleryUiState.Content
        assertEquals(2, contentAfterUndo.dateGroups.flatMap { it.photos }.size)
    }

    @Test
    fun setFilter_clearsSelectionMode() = runTest {
        val photo1 = createMediaItem(1L, "photo1.jpg")
        fakeRepository.items = listOf(photo1)

        viewModel = GalleryViewModel(fakeRepository)
        advanceUntilIdle()

        viewModel.enterSelection(photo1)
        assertTrue((viewModel.uiState.value as GalleryUiState.Content).isSelectionMode)

        viewModel.setFilter(GalleryFilterCategory.VIDEOS)
        advanceUntilIdle()

        val content = viewModel.uiState.value as GalleryUiState.Content
        assertFalse(content.isSelectionMode)
        assertEquals(emptySet<Long>(), content.selectedIds)
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

    private class FakeSelectionMediaRepository : MediaRepository {
        var items = listOf<MediaItem>()
        var lastTrashedItems: List<MediaItem>? = null
        var lastRestoredItems: List<MediaItem>? = null

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

        override suspend fun trashMediaItem(mediaItem: MediaItem): android.content.IntentSender? = null

        override suspend fun trashMediaItems(mediaItems: List<MediaItem>): android.content.IntentSender? {
            lastTrashedItems = mediaItems
            return null
        }

        override suspend fun getTrashedMedia(): Result<List<MediaItem>> = Result.success(emptyList())

        override suspend fun restoreMediaItems(mediaItems: List<MediaItem>): android.content.IntentSender? {
            lastRestoredItems = mediaItems
            return null
        }

        override suspend fun deleteMediaItemsPermanently(mediaItems: List<MediaItem>): android.content.IntentSender? = null

        override suspend fun setFavorite(mediaItems: List<MediaItem>, isFavorite: Boolean): android.content.IntentSender? = null
    }
}
