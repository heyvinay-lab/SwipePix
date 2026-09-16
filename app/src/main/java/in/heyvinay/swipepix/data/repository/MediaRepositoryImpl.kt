package `in`.heyvinay.swipepix.data.repository

import `in`.heyvinay.swipepix.data.datasource.MediaStoreDataSource
import `in`.heyvinay.swipepix.data.model.Album
import `in`.heyvinay.swipepix.data.model.MediaItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepositoryImpl @Inject constructor(
    private val dataSource: MediaStoreDataSource,
    private val contentResolver: android.content.ContentResolver,
) : MediaRepository {

    override suspend fun getPhotos(
        bucketId: String?,
        limit: Int,
        offset: Int,
        filter: `in`.heyvinay.swipepix.data.model.GalleryFilterCategory,
    ): Result<List<MediaItem>> = runCatching {
        dataSource.queryPhotos(bucketId = bucketId, limit = limit, offset = offset, filter = filter)
    }

    override suspend fun getAlbums(): Result<List<Album>> = runCatching {
        dataSource.queryAlbums()
    }

    override suspend fun getMediaCount(bucketId: String?): Result<`in`.heyvinay.swipepix.data.model.AlbumMediaCount> = runCatching {
        dataSource.queryMediaCount(bucketId)
    }

    override suspend fun getPhotoById(id: Long): Result<MediaItem?> = runCatching {
        dataSource.queryMediaItemById(id)
    }

    override fun observeChanges(): Flow<Unit> {
        return dataSource.observeMediaStoreChanges()
    }

    override suspend fun trashMediaItem(mediaItem: MediaItem): android.content.IntentSender? {
        return trashMediaItems(listOf(mediaItem))
    }

    override suspend fun trashMediaItems(mediaItems: List<MediaItem>): android.content.IntentSender? {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R && mediaItems.isNotEmpty()) {
            try {
                android.provider.MediaStore.createTrashRequest(
                    contentResolver,
                    mediaItems.map { it.contentUri },
                    true
                ).intentSender
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else {
            null
        }
    }

    override suspend fun getTrashedMedia(): Result<List<MediaItem>> = runCatching {
        dataSource.queryTrashedMedia()
    }

    override suspend fun restoreMediaItems(mediaItems: List<MediaItem>): android.content.IntentSender? {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R && mediaItems.isNotEmpty()) {
            try {
                android.provider.MediaStore.createTrashRequest(
                    contentResolver,
                    mediaItems.map { it.contentUri },
                    false // false indicates untrash/restore
                ).intentSender
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else {
            null
        }
    }

    override suspend fun deleteMediaItemsPermanently(mediaItems: List<MediaItem>): android.content.IntentSender? {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R && mediaItems.isNotEmpty()) {
            try {
                android.provider.MediaStore.createDeleteRequest(
                    contentResolver,
                    mediaItems.map { it.contentUri }
                ).intentSender
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else {
            null
        }
    }

    override suspend fun setFavorite(mediaItems: List<MediaItem>, isFavorite: Boolean): android.content.IntentSender? {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R && mediaItems.isNotEmpty()) {
            try {
                android.provider.MediaStore.createFavoriteRequest(
                    contentResolver,
                    mediaItems.map { it.contentUri },
                    isFavorite
                ).intentSender
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else {
            null
        }
    }
}

