package `in`.heyvinay.swipepix.ui.util

import `in`.heyvinay.swipepix.data.model.MediaItem
import `in`.heyvinay.swipepix.data.model.MediaType
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtils {

    private val sameYearFormatter = DateTimeFormatter.ofPattern("MMMM d", Locale.getDefault())
    private val differentYearFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.getDefault())

    fun formatLocalDate(
        date: LocalDate,
        today: LocalDate = LocalDate.now(ZoneId.systemDefault()),
    ): String {
        val yesterday = today.minusDays(1)
        return when {
            date.isEqual(today) -> "Today"
            date.isEqual(yesterday) -> "Yesterday"
            date.year == today.year -> date.format(sameYearFormatter)
            else -> date.format(differentYearFormatter)
        }
    }

    fun formatDateHeader(
        timestamp: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
        today: LocalDate = LocalDate.now(zoneId),
    ): String {
        if (timestamp <= 0) return "Earlier"

        // MediaStore dateAdded/dateModified is in seconds; dateTaken is in milliseconds
        val epochMillis = if (timestamp < 100_000_000_000L) timestamp * 1000 else timestamp
        val date = Instant.ofEpochMilli(epochMillis)
            .atZone(zoneId)
            .toLocalDate()

        return formatLocalDate(date, today)
    }

    /**
     * Groups photos by date in strictly descending chronological order
     * (Today -> Yesterday -> older dates -> Earlier), and sorts photos within
     * each group descending by effective timestamp.
     */
    fun groupPhotosByDate(
        photos: List<MediaItem>,
        zoneId: ZoneId = ZoneId.systemDefault(),
        today: LocalDate = LocalDate.now(zoneId),
    ): Map<String, List<MediaItem>> {
        val uniquePhotos = photos.distinctBy { it.contentUri.toString() }
        val dateMap = mutableMapOf<LocalDate?, MutableList<MediaItem>>()

        for (photo in uniquePhotos) {
            val ts = photo.effectiveTimestamp
            val date: LocalDate? = if (ts > 0) {
                Instant.ofEpochMilli(ts).atZone(zoneId).toLocalDate()
            } else {
                null
            }
            dateMap.getOrPut(date) { mutableListOf() }.add(photo)
        }

        // Sort non-null dates in reverse chronological order, null ("Earlier") goes last
        val sortedKeys = dateMap.keys.sortedWith(
            Comparator { d1, d2 ->
                when {
                    d1 == null && d2 == null -> 0
                    d1 == null -> 1
                    d2 == null -> -1
                    else -> d2.compareTo(d1) // descending
                }
            }
        )

        val result = LinkedHashMap<String, List<MediaItem>>()
        for (key in sortedKeys) {
            val header = if (key == null) "Earlier" else formatLocalDate(key, today)
            val items = dateMap[key]!!.distinctBy { it.contentUri.toString() }.sortedWith(
                compareByDescending<MediaItem> { it.effectiveTimestamp }
                    .thenByDescending { it.id }
            )
            val existing = result[header]
            if (existing != null) {
                result[header] = (existing + items).distinctBy { it.contentUri.toString() }.sortedWith(
                    compareByDescending<MediaItem> { it.effectiveTimestamp }
                        .thenByDescending { it.id }
                )
            } else {
                result[header] = items
            }
        }

        return result
    }

    /**
     * Formats media counts with proper singular and plural grammar:
     * - "1 photo"
     * - "5 photos"
     * - "1 video"
     * - "3 videos"
     * - "4 photos • 2 videos"
     */
    fun formatMediaSubtitle(photoCount: Int, videoCount: Int): String {
        val numberFormat = NumberFormat.getNumberInstance()
        return when {
            photoCount > 0 && videoCount > 0 -> {
                val p = "${numberFormat.format(photoCount)} ${if (photoCount == 1) "photo" else "photos"}"
                val v = "${numberFormat.format(videoCount)} ${if (videoCount == 1) "video" else "videos"}"
                "$p • $v"
            }
            videoCount > 0 -> {
                "${numberFormat.format(videoCount)} ${if (videoCount == 1) "video" else "videos"}"
            }
            else -> {
                "${numberFormat.format(photoCount)} ${if (photoCount == 1) "photo" else "photos"}"
            }
        }
    }

    /**
     * Convenience method to format subtitle from a collection of MediaItems.
     */
    fun formatMediaSubtitle(items: List<MediaItem>): String {
        val photoCount = items.count { it.mediaType == MediaType.PHOTO }
        val videoCount = items.count { it.mediaType == MediaType.VIDEO }
        return formatMediaSubtitle(photoCount, videoCount)
    }

    /**
     * Formats video duration in milliseconds to m:ss or h:mm:ss.
     */
    fun formatDuration(durationMs: Long): String {
        val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
        }
    }
}
