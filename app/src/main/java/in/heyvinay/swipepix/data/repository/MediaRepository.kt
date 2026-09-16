package `in`.heyvinay.swipepix.data.repository

import `in`.heyvinay.swipepix.data.model.Album
import `in`.heyvinay.swipepix.data.model.MediaItem
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for interacting with device media.
 */
interface MediaRepository {
    /**
     * Retrieves a paginated list of photos, optionally filtered by album/bucket.
     */
    suspend fun getPhotos(
        bucketId: String? = null,
        limit: Int = 100,
        offset: Int = 0,
        filter: `in`.heyvinay.swipepix.data.model.GalleryFilterCategory = `in`.heyvinay.swipepix.data.model.GalleryFilterCategory.ALL,
    ): Result<List<MediaItem>>

    /**
     * Retrieves all discovered albums on the device with photo counts.
     */
    suspend fun getAlbums(): Result<List<Album>>

    /**
     * Retrieves authoritative photo, video, and total count for a scope or library-wide.
     */
    suspend fun getMediaCount(bucketId: String? = null): Result<`in`.heyvinay.swipepix.data.model.AlbumMediaCount>

    /**
     * Retrieves a single photo by its ID.
     */
    suspend fun getPhotoById(id: Long): Result<MediaItem?>

    /**
     * Observes real-time changes to the device photo library.
     */
    fun observeChanges(): Flow<Unit>

    /**
     * Creates a MediaStore trash request for a specific media item.
     * Returns an IntentSender that should be launched to prompt the user.
     */
    suspend fun trashMediaItem(mediaItem: MediaItem): android.content.IntentSender?

    /**
     * Creates a batched MediaStore trash request for multiple media items.
     * Returns an IntentSender that should be launched to prompt the user once for all items.
     */
    suspend fun trashMediaItems(mediaItems: List<MediaItem>): android.content.IntentSender?

    /**
     * Retrieves media items currently in Android's MediaStore Trash.
     */
    suspend fun getTrashedMedia(): Result<List<MediaItem>>

    /**
     * Creates a MediaStore request to restore (untrash) media items from the trash.
     */
    suspend fun restoreMediaItems(mediaItems: List<MediaItem>): android.content.IntentSender?

    /**
     * Creates a MediaStore request to permanently delete media items.
     */
    suspend fun deleteMediaItemsPermanently(mediaItems: List<MediaItem>): android.content.IntentSender?

    /**
     * Creates a MediaStore request to toggle favorite status on Android 11+ (API 30+).
     */
    suspend fun setFavorite(mediaItems: List<MediaItem>, isFavorite: Boolean): android.content.IntentSender?
}

