package `in`.heyvinay.swipepix.data.datasource

import android.content.ContentResolver
import android.content.ContentUris
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import `in`.heyvinay.swipepix.data.model.Album
import `in`.heyvinay.swipepix.data.model.GalleryFilterCategory
import `in`.heyvinay.swipepix.data.model.MediaItem
import `in`.heyvinay.swipepix.data.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Low-level data source executing MediaStore queries strictly off the main thread.
 */
@Singleton
class MediaStoreDataSource @Inject constructor(
    private val contentResolver: ContentResolver,
) {
    private val filesBaseUri: Uri = MediaStore.Files.getContentUri("external")
    private val imageBaseUri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    private val videoBaseUri: Uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

    private val mediaProjection = buildList {
        add(MediaStore.MediaColumns._ID)
        add(MediaStore.MediaColumns.DISPLAY_NAME)
        add(MediaStore.MediaColumns.MIME_TYPE)
        add(MediaStore.MediaColumns.DATE_ADDED)
        add(MediaStore.MediaColumns.DATE_TAKEN)
        add(MediaStore.MediaColumns.DATE_MODIFIED)
        add(MediaStore.MediaColumns.SIZE)
        add(MediaStore.MediaColumns.WIDTH)
        add(MediaStore.MediaColumns.HEIGHT)
        add(MediaStore.MediaColumns.BUCKET_ID)
        add(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
        add(MediaStore.Files.FileColumns.MEDIA_TYPE)
        add(MediaStore.MediaColumns.ORIENTATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            add(MediaStore.MediaColumns.DURATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            add(MediaStore.MediaColumns.IS_FAVORITE)
        }
    }.toTypedArray()

    /**
     * Queries photos and videos in date-descending order with pagination support.
     */
    suspend fun queryPhotos(
        bucketId: String? = null,
        limit: Int = 100,
        offset: Int = 0,
        filter: GalleryFilterCategory = GalleryFilterCategory.ALL,
    ): List<MediaItem> = withContext(Dispatchers.IO) {
        val mediaList = mutableListOf<MediaItem>()
        val seenUris = mutableSetOf<Uri>()

        val baseSelection = when (filter) {
            GalleryFilterCategory.ALL -> {
                "(${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE} OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO}) AND ${MediaStore.MediaColumns.SIZE} > 0"
            }
            GalleryFilterCategory.VIDEOS -> {
                "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO} AND ${MediaStore.MediaColumns.SIZE} > 0"
            }
            GalleryFilterCategory.FAVORITES -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    "(${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE} OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO}) AND ${MediaStore.MediaColumns.SIZE} > 0 AND ${MediaStore.MediaColumns.IS_FAVORITE} = 1"
                } else {
                    "(${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE} OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO}) AND ${MediaStore.MediaColumns.SIZE} > 0"
                }
            }
            GalleryFilterCategory.SCREENSHOTS -> {
                "(${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE} OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO}) AND ${MediaStore.MediaColumns.SIZE} > 0 AND (${MediaStore.MediaColumns.BUCKET_DISPLAY_NAME} LIKE '%Screenshot%' OR ${MediaStore.MediaColumns.DISPLAY_NAME} LIKE '%Screenshot%')"
            }
        }
        val selection = if (!bucketId.isNullOrBlank()) {
            "$baseSelection AND ${MediaStore.MediaColumns.BUCKET_ID} = ?"
        } else {
            baseSelection
        }
        val selectionArgs = if (!bucketId.isNullOrBlank()) arrayOf(bucketId) else null

        val queryArgs = Bundle().apply {
            putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
            putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
            putString(
                ContentResolver.QUERY_ARG_SQL_SORT_ORDER,
                "${MediaStore.MediaColumns.DATE_TAKEN} DESC, ${MediaStore.MediaColumns.DATE_MODIFIED} DESC, ${MediaStore.MediaColumns.DATE_ADDED} DESC, ${MediaStore.MediaColumns._ID} DESC",
            )
            putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
            if (selectionArgs != null) {
                putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
            }
        }

        try {
            contentResolver.query(filesBaseUri, mediaProjection, queryArgs, null)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                val addedCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                val takenCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
                val modifiedCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)
                val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID)
                val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
                val mediaTypeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE)
                val orientationCol = cursor.getColumnIndex(MediaStore.MediaColumns.ORIENTATION)
                val durationCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.MediaColumns.DURATION)
                } else -1
                val favoriteCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    cursor.getColumnIndex(MediaStore.MediaColumns.IS_FAVORITE)
                } else -1

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val mime = cursor.getString(mimeCol) ?: "image/jpeg"
                    val fileType = if (mediaTypeCol != -1) cursor.getInt(mediaTypeCol) else MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
                    val isVideo = fileType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO || mime.startsWith("video/")
                    val type = if (isVideo) MediaType.VIDEO else MediaType.PHOTO
                    val contentUri = if (isVideo) {
                        ContentUris.withAppendedId(videoBaseUri, id)
                    } else {
                        ContentUris.withAppendedId(imageBaseUri, id)
                    }

                    if (!seenUris.add(contentUri)) continue

                    val isFav = if (favoriteCol != -1) cursor.getInt(favoriteCol) == 1 else false
                    val duration = if (durationCol != -1 && isVideo) cursor.getLong(durationCol) else 0L
                    val orientation = if (orientationCol != -1) cursor.getInt(orientationCol) else 0

                    mediaList.add(
                        MediaItem(
                            id = id,
                            contentUri = contentUri,
                            displayName = cursor.getString(nameCol) ?: "IMG_$id",
                            mimeType = mime,
                            dateAdded = cursor.getLong(addedCol),
                            dateTaken = cursor.getLong(takenCol),
                            dateModified = cursor.getLong(modifiedCol),
                            size = cursor.getLong(sizeCol),
                            width = cursor.getInt(widthCol),
                            height = cursor.getInt(heightCol),
                            bucketId = cursor.getString(bucketIdCol) ?: "",
                            bucketDisplayName = cursor.getString(bucketNameCol) ?: "Default",
                            mediaType = type,
                            isFavorite = isFav,
                            durationMs = duration,
                            orientation = orientation,
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        mediaList
    }

    /**
     * Discovers all photo albums/buckets on the device.
     */
    suspend fun queryAlbums(): List<Album> = withContext(Dispatchers.IO) {
        val albumMap = mutableMapOf<String, AlbumAccumulator>()

        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.BUCKET_ID,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
        )

        val queryArgs = Bundle().apply {
            putString(
                ContentResolver.QUERY_ARG_SQL_SELECTION,
                "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE} OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO}",
            )
            putStringArray(
                ContentResolver.QUERY_ARG_SORT_COLUMNS,
                arrayOf(MediaStore.MediaColumns.DATE_ADDED),
            )
            putInt(
                ContentResolver.QUERY_ARG_SORT_DIRECTION,
                ContentResolver.QUERY_SORT_DIRECTION_DESCENDING,
            )
        }

        try {
            contentResolver.query(filesBaseUri, projection, queryArgs, null)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID)
                val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
                val mediaTypeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE)

                while (cursor.moveToNext()) {
                    val bucketId = cursor.getString(bucketIdCol) ?: continue
                    val bucketName = cursor.getString(bucketNameCol) ?: "Album"
                    val id = cursor.getLong(idCol)
                    val fileType = if (mediaTypeCol != -1) cursor.getInt(mediaTypeCol) else MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
                    val uri = if (fileType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
                        ContentUris.withAppendedId(videoBaseUri, id)
                    } else {
                        ContentUris.withAppendedId(imageBaseUri, id)
                    }

                    val existing = albumMap[bucketId]
                    if (existing == null) {
                        val isVideo = fileType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                        albumMap[bucketId] = AlbumAccumulator(
                            id = bucketId,
                            displayName = bucketName,
                            coverUri = uri,
                            count = 1,
                            photoCount = if (isVideo) 0 else 1,
                            videoCount = if (isVideo) 1 else 0,
                        )
                    } else {
                        existing.count++
                        if (fileType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
                            existing.videoCount++
                        } else {
                            existing.photoCount++
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        albumMap.values.map {
            Album(
                id = it.id,
                displayName = it.displayName,
                coverUri = it.coverUri,
                mediaCount = it.count,
                photoCount = it.photoCount,
                videoCount = it.videoCount,
            )
        }.sortedByDescending { it.mediaCount }
    }

    /**
     * Authoritative count of photos, videos, and total media from MediaStore.
     * If bucketId is null, returns library-wide counts.
     */
    suspend fun queryMediaCount(bucketId: String? = null): `in`.heyvinay.swipepix.data.model.AlbumMediaCount = withContext(Dispatchers.IO) {
        val photoSelection = buildString {
            append("(${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE})")
            append(" AND ${MediaStore.MediaColumns.SIZE} > 0")
            if (!bucketId.isNullOrBlank()) {
                append(" AND ${MediaStore.MediaColumns.BUCKET_ID} = ?")
            }
        }
        val photoArgs = if (!bucketId.isNullOrBlank()) arrayOf(bucketId) else null

        val photoCount = try {
            val queryArgs = Bundle().apply {
                putString(ContentResolver.QUERY_ARG_SQL_SELECTION, photoSelection)
                if (photoArgs != null) {
                    putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, photoArgs)
                }
            }
            contentResolver.query(filesBaseUri, arrayOf(MediaStore.MediaColumns._ID), queryArgs, null)?.use {
                it.count
            } ?: 0
        } catch (e: Exception) {
            0
        }

        val videoSelection = buildString {
            append("(${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO})")
            append(" AND ${MediaStore.MediaColumns.SIZE} > 0")
            if (!bucketId.isNullOrBlank()) {
                append(" AND ${MediaStore.MediaColumns.BUCKET_ID} = ?")
            }
        }
        val videoArgs = if (!bucketId.isNullOrBlank()) arrayOf(bucketId) else null

        val videoCount = try {
            val queryArgs = Bundle().apply {
                putString(ContentResolver.QUERY_ARG_SQL_SELECTION, videoSelection)
                if (videoArgs != null) {
                    putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, videoArgs)
                }
            }
            contentResolver.query(filesBaseUri, arrayOf(MediaStore.MediaColumns._ID), queryArgs, null)?.use {
                it.count
            } ?: 0
        } catch (e: Exception) {
            0
        }

        val favoriteSelection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            buildString {
                append("(${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE} OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO})")
                append(" AND ${MediaStore.MediaColumns.SIZE} > 0")
                append(" AND ${MediaStore.MediaColumns.IS_FAVORITE} = 1")
                if (!bucketId.isNullOrBlank()) {
                    append(" AND ${MediaStore.MediaColumns.BUCKET_ID} = ?")
                }
            }
        } else null

        val favoriteCount = if (favoriteSelection != null) {
            try {
                val queryArgs = Bundle().apply {
                    putString(ContentResolver.QUERY_ARG_SQL_SELECTION, favoriteSelection)
                    if (photoArgs != null) {
                        putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, photoArgs)
                    }
                }
                contentResolver.query(filesBaseUri, arrayOf(MediaStore.MediaColumns._ID), queryArgs, null)?.use {
                    it.count
                } ?: 0
            } catch (e: Exception) {
                0
            }
        } else 0

        `in`.heyvinay.swipepix.data.model.AlbumMediaCount(
            totalCount = photoCount + videoCount,
            photoCount = photoCount,
            videoCount = videoCount,
            favoriteCount = favoriteCount,
        )
    }

    /**
     * Queries a single MediaItem by its MediaStore ID.
     */
    suspend fun queryMediaItemById(id: Long): MediaItem? = withContext(Dispatchers.IO) {
        val queryArgs = Bundle().apply {
            putString(
                ContentResolver.QUERY_ARG_SQL_SELECTION,
                "${MediaStore.MediaColumns._ID} = ?",
            )
            putStringArray(
                ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
                arrayOf(id.toString()),
            )
            putInt(ContentResolver.QUERY_ARG_LIMIT, 1)
        }

        try {
            contentResolver.query(filesBaseUri, mediaProjection, queryArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                    val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                    val addedCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                    val takenCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
                    val modifiedCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                    val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                    val widthCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)
                    val heightCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)
                    val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID)
                    val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
                    val mediaTypeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE)
                    val orientationCol = cursor.getColumnIndex(MediaStore.MediaColumns.ORIENTATION)
                    val durationCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        cursor.getColumnIndex(MediaStore.MediaColumns.DURATION)
                    } else -1
                    val favoriteCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        cursor.getColumnIndex(MediaStore.MediaColumns.IS_FAVORITE)
                    } else -1

                    val mediaId = cursor.getLong(idCol)
                    val mime = cursor.getString(mimeCol) ?: "image/jpeg"
                    val fileType = if (mediaTypeCol != -1) cursor.getInt(mediaTypeCol) else MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
                    val isVideo = fileType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO || mime.startsWith("video/")
                    val type = if (isVideo) MediaType.VIDEO else MediaType.PHOTO
                    val contentUri = if (isVideo) {
                        ContentUris.withAppendedId(videoBaseUri, mediaId)
                    } else {
                        ContentUris.withAppendedId(imageBaseUri, mediaId)
                    }

                    val isFav = if (favoriteCol != -1) cursor.getInt(favoriteCol) == 1 else false
                    val duration = if (durationCol != -1 && isVideo) cursor.getLong(durationCol) else 0L
                    val orientation = if (orientationCol != -1) cursor.getInt(orientationCol) else 0

                    return@withContext MediaItem(
                        id = mediaId,
                        contentUri = contentUri,
                        displayName = cursor.getString(nameCol) ?: "IMG_$mediaId",
                        mimeType = mime,
                        dateAdded = cursor.getLong(addedCol),
                        dateTaken = cursor.getLong(takenCol),
                        dateModified = cursor.getLong(modifiedCol),
                        size = cursor.getLong(sizeCol),
                        width = cursor.getInt(widthCol),
                        height = cursor.getInt(heightCol),
                        bucketId = cursor.getString(bucketIdCol) ?: "",
                        bucketDisplayName = cursor.getString(bucketNameCol) ?: "Default",
                        mediaType = type,
                        isFavorite = isFav,
                        durationMs = duration,
                        orientation = orientation,
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    /**
     * Observes MediaStore changes in real-time, emitting whenever media is added or deleted.
     */
    fun observeMediaStoreChanges(): Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                trySend(Unit)
            }
        }

        contentResolver.registerContentObserver(filesBaseUri, true, observer)

        awaitClose {
            contentResolver.unregisterContentObserver(observer)
        }
    }

    /**
     * Queries photos and videos currently located in Android's MediaStore Trash (API 30+).
     */
    suspend fun queryTrashedMedia(): List<MediaItem> = withContext(Dispatchers.IO) {
        val mediaList = mutableListOf<MediaItem>()
        val seenUris = mutableSetOf<Uri>()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return@withContext mediaList
        }

        val queryArgs = Bundle().apply {
            putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
            putStringArray(
                ContentResolver.QUERY_ARG_SORT_COLUMNS,
                arrayOf(MediaStore.MediaColumns.DATE_MODIFIED),
            )
            putInt(
                ContentResolver.QUERY_ARG_SORT_DIRECTION,
                ContentResolver.QUERY_SORT_DIRECTION_DESCENDING,
            )
        }

        try {
            contentResolver.query(filesBaseUri, mediaProjection, queryArgs, null)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                val addedCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                val takenCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
                val modifiedCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)
                val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID)
                val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
                val mediaTypeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE)
                val orientationCol = cursor.getColumnIndex(MediaStore.MediaColumns.ORIENTATION)
                val durationCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.MediaColumns.DURATION)
                } else -1
                val favoriteCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    cursor.getColumnIndex(MediaStore.MediaColumns.IS_FAVORITE)
                } else -1

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val mime = cursor.getString(mimeCol) ?: "image/jpeg"
                    val fileType = if (mediaTypeCol != -1) cursor.getInt(mediaTypeCol) else MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
                    val isVideo = fileType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO || mime.startsWith("video/")
                    val type = if (isVideo) MediaType.VIDEO else MediaType.PHOTO
                    val contentUri = if (isVideo) {
                        ContentUris.withAppendedId(videoBaseUri, id)
                    } else {
                        ContentUris.withAppendedId(imageBaseUri, id)
                    }

                    if (!seenUris.add(contentUri)) continue

                    val isFav = if (favoriteCol != -1) cursor.getInt(favoriteCol) == 1 else false
                    val duration = if (durationCol != -1 && isVideo) cursor.getLong(durationCol) else 0L
                    val orientation = if (orientationCol != -1) cursor.getInt(orientationCol) else 0

                    mediaList.add(
                        MediaItem(
                            id = id,
                            contentUri = contentUri,
                            displayName = cursor.getString(nameCol) ?: "IMG_$id",
                            mimeType = mime,
                            dateAdded = cursor.getLong(addedCol),
                            dateTaken = cursor.getLong(takenCol),
                            dateModified = cursor.getLong(modifiedCol),
                            size = cursor.getLong(sizeCol),
                            width = cursor.getInt(widthCol),
                            height = cursor.getInt(heightCol),
                            bucketId = cursor.getString(bucketIdCol) ?: "",
                            bucketDisplayName = cursor.getString(bucketNameCol) ?: "Default",
                            mediaType = type,
                            isFavorite = isFav,
                            durationMs = duration,
                            orientation = orientation,
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        mediaList
    }

    private data class AlbumAccumulator(
        val id: String,
        val displayName: String,
        val coverUri: Uri?,
        var count: Int,
        var photoCount: Int = 0,
        var videoCount: Int = 0,
    )
}
