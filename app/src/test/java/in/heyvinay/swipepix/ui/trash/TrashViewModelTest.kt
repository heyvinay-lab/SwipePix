package `in`.heyvinay.swipepix.ui.trash

import android.net.FakeUri
import `in`.heyvinay.swipepix.data.model.MediaItem
import `in`.heyvinay.swipepix.data.model.MediaType
import `in`.heyvinay.swipepix.ui.cleanup.FakeMediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class TrashViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeMediaRepo: FakeMediaRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeMediaRepo = FakeMediaRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testLoadTrash_withMixedContent_calculatesCountsAndDisplaysAll() = runTest {
        val photo1 = createItem(1L, MediaType.PHOTO)
        val photo2 = createItem(2L, MediaType.PHOTO)
        val video1 = createItem(3L, MediaType.VIDEO)
        fakeMediaRepo.trashed = listOf(photo1, photo2, video1)

        val viewModel = TrashViewModel(fakeMediaRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is TrashUiState.Ready)
        val ready = state as TrashUiState.Ready

        assertEquals(3, ready.totalCount)
        assertEquals(2, ready.photoCount)
        assertEquals(1, ready.videoCount)
        assertEquals(3, ready.displayedItems.size)
    }

    @Test
    fun testFiltering_photosAndVideos() = runTest {
        val photo = createItem(10L, MediaType.PHOTO)
        val video = createItem(20L, MediaType.VIDEO)
        fakeMediaRepo.trashed = listOf(photo, video)

        val viewModel = TrashViewModel(fakeMediaRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setFilter(TrashFilter.PHOTOS)
        var ready = viewModel.uiState.value as TrashUiState.Ready
        assertEquals(1, ready.displayedItems.size)
        assertEquals(10L, ready.displayedItems[0].id)

        viewModel.setFilter(TrashFilter.VIDEOS)
        ready = viewModel.uiState.value as TrashUiState.Ready
        assertEquals(1, ready.displayedItems.size)
        assertEquals(20L, ready.displayedItems[0].id)
    }

    @Test
    fun testSelection_toggleAndSelectAll() = runTest {
        val photo1 = createItem(1L, MediaType.PHOTO)
        val photo2 = createItem(2L, MediaType.PHOTO)
        fakeMediaRepo.trashed = listOf(photo1, photo2)

        val viewModel = TrashViewModel(fakeMediaRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleSelection(1L)
        var ready = viewModel.uiState.value as TrashUiState.Ready
        assertEquals(setOf(1L), ready.selectedIds)

        viewModel.toggleSelectAll()
        ready = viewModel.uiState.value as TrashUiState.Ready
        assertEquals(setOf(1L, 2L), ready.selectedIds)

        viewModel.toggleSelectAll()
        ready = viewModel.uiState.value as TrashUiState.Ready
        assertTrue(ready.selectedIds.isEmpty())
    }

    private fun createItem(id: Long, type: MediaType): MediaItem {
        return MediaItem(
            id = id,
            contentUri = FakeUri("content://trash/$id"),
            displayName = "item_$id",
            mimeType = if (type == MediaType.VIDEO) "video/mp4" else "image/jpeg",
            dateAdded = 1000L,
            dateTaken = 1000L,
            dateModified = 1000L,
            size = 5000L,
            width = 1920,
            height = 1080,
            bucketId = "trash",
            bucketDisplayName = "Trash",
            mediaType = type
        )
    }
}
