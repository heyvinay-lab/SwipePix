package `in`.heyvinay.swipepix.data.model

import android.net.FakeUri
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaItemTest {

    @Test
    fun mediaItem_creationAndProperties() {
        val fakeUri = FakeUri("content://fake/test")
        val item = MediaItem(
            id = 123L,
            contentUri = fakeUri,
            displayName = "test.jpg",
            mimeType = "image/jpeg",
            dateAdded = 1000L,
            dateTaken = 1000L,
            dateModified = 1000L,
            size = 2048L,
            width = 1920,
            height = 1080,
            bucketId = "camera_bucket",
            bucketDisplayName = "Camera",
            mediaType = MediaType.PHOTO,
        )

        assertEquals(123L, item.id)
        assertEquals("test.jpg", item.displayName)
        assertEquals(MediaType.PHOTO, item.mediaType)
        assertEquals(1920, item.width)
        assertEquals(1080, item.height)
        assertEquals("content://fake/test", item.contentUri.toString())
    }

    @Test
    fun album_creationAndProperties() {
        val fakeUri = FakeUri("content://fake/test")
        val album = Album(
            id = "bucket_1",
            displayName = "Screenshots",
            coverUri = fakeUri,
            mediaCount = 42,
        )

        assertEquals("bucket_1", album.id)
        assertEquals("Screenshots", album.displayName)
        assertEquals(42, album.mediaCount)
        assertEquals("content://fake/test", album.coverUri.toString())
    }

    @Test
    fun albumMediaCount_creationAndDefaults() {
        val count = AlbumMediaCount(
            totalCount = 300,
            photoCount = 200,
            videoCount = 100,
            favoriteCount = 25,
        )

        assertEquals(300, count.totalCount)
        assertEquals(200, count.photoCount)
        assertEquals(100, count.videoCount)
        assertEquals(25, count.favoriteCount)
    }

    @Test
    fun galleryFilterCategory_labels() {
        assertEquals("All", GalleryFilterCategory.ALL.label)
        assertEquals("Favorites", GalleryFilterCategory.FAVORITES.label)
        assertEquals("Videos", GalleryFilterCategory.VIDEOS.label)
        assertEquals("Screenshots", GalleryFilterCategory.SCREENSHOTS.label)
    }
}
