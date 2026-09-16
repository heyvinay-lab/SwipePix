package `in`.heyvinay.swipepix.data.model

import android.net.Uri

/**
 * Represents a single media item from the device's MediaStore.
 *
 * This is an immutable snapshot of MediaStore data — it does NOT hold
 * references to bitmaps, cursors, or file handles.
 */
data class MediaItem(
    val id: Long,
    val contentUri: Uri,
    val displayName: String,
    val mimeType: String,
    val dateAdded: Long,
    val dateTaken: Long,
    val dateModified: Long,
    val size: Long,
    val width: Int,
    val height: Int,
    val bucketId: String,
    val bucketDisplayName: String,
    val mediaType: MediaType,
    val isFavorite: Boolean = false,
    val durationMs: Long = 0L,
    val orientation: Int = 0,
) {
    /**
     * Effective width taking EXIF orientation into account.
     * When rotated 90 or 270 degrees, visual width corresponds to native height.
     */
    val displayedWidth: Int
        get() = if (orientation == 90 || orientation == 270) height else width

    /**
     * Effective height taking EXIF orientation into account.
     * When rotated 90 or 270 degrees, visual height corresponds to native width.
     */
    val displayedHeight: Int
        get() = if (orientation == 90 || orientation == 270) width else height

    /**
     * Display aspect ratio (width / height) representing the media as actually viewed by the user.
     * Falls back to 0.75f (standard 3:4 portrait) if dimensions are missing or invalid.
     */
    val displayedAspectRatio: Float
        get() {
            val w = displayedWidth
            val h = displayedHeight
            return if (w > 0 && h > 0) w.toFloat() / h.toFloat() else 0.75f
        }

    /**
     * Authoritative millisecond timestamp for chronological ordering and date grouping.
     * Prefers `dateTaken` (already in ms), falling back to `dateModified` (in sec -> ms),
     * `dateAdded` (in sec -> ms), or 0L.
     */
    val effectiveTimestamp: Long
        get() {
            val taken = if (dateTaken in 1L until 100_000_000_000L) dateTaken * 1000L else dateTaken
            return when {
                taken > 0L -> taken
                dateModified > 0L -> dateModified * 1000L
                dateAdded > 0L -> dateAdded * 1000L
                else -> 0L
            }
        }
}
