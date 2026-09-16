package `in`.heyvinay.swipepix.ui.util

import android.net.FakeUri
import `in`.heyvinay.swipepix.data.model.MediaItem
import `in`.heyvinay.swipepix.data.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class DateUtilsTest {

    private val zoneId = ZoneId.of("UTC")
    private val fixedToday = LocalDate.of(2026, 9, 16)

    private fun createItem(
        id: Long,
        dateTaken: Long = 0L,
        dateModified: Long = 0L,
        dateAdded: Long = 0L,
        mediaType: MediaType = MediaType.PHOTO,
    ): MediaItem {
        return MediaItem(
            id = id,
            contentUri = FakeUri("content://media/test/$id"),
            displayName = "item_$id.jpg",
            mimeType = if (mediaType == MediaType.VIDEO) "video/mp4" else "image/jpeg",
            dateAdded = dateAdded,
            dateTaken = dateTaken,
            dateModified = dateModified,
            size = 1024L,
            width = 1920,
            height = 1080,
            bucketId = "test_bucket",
            bucketDisplayName = "Camera",
            mediaType = mediaType,
        )
    }

    @Test
    fun effectiveTimestamp_prefersDateTaken() {
        val item = createItem(id = 1, dateTaken = 1726488000000L, dateModified = 1726000000L, dateAdded = 1725000000L)
        assertEquals(1726488000000L, item.effectiveTimestamp)
    }

    @Test
    fun effectiveTimestamp_fallsBackToDateModified() {
        val item = createItem(id = 2, dateTaken = 0L, dateModified = 1726000000L, dateAdded = 1725000000L)
        assertEquals(1726000000000L, item.effectiveTimestamp)
    }

    @Test
    fun effectiveTimestamp_fallsBackToDateAdded() {
        val item = createItem(id = 3, dateTaken = 0L, dateModified = 0L, dateAdded = 1725000000L)
        assertEquals(1725000000000L, item.effectiveTimestamp)
    }

    @Test
    fun effectiveTimestamp_normalizesSecondsToMilliseconds() {
        // dateTaken provided in seconds (10 digits)
        val item = createItem(id = 4, dateTaken = 1726488000L, dateModified = 0L, dateAdded = 0L)
        assertEquals(1726488000000L, item.effectiveTimestamp)
    }

    @Test
    fun formatLocalDate_todayYesterdaySameYearDifferentYear() {
        assertEquals("Today", DateUtils.formatLocalDate(fixedToday, fixedToday))
        assertEquals("Yesterday", DateUtils.formatLocalDate(fixedToday.minusDays(1), fixedToday))
        assertEquals("September 14", DateUtils.formatLocalDate(fixedToday.minusDays(2), fixedToday))
        assertEquals("December 25, 2024", DateUtils.formatLocalDate(LocalDate.of(2024, 12, 25), fixedToday))
    }

    @Test
    fun formatMediaSubtitle_singularAndPlural() {
        assertEquals("1 photo", DateUtils.formatMediaSubtitle(photoCount = 1, videoCount = 0))
        assertEquals("5 photos", DateUtils.formatMediaSubtitle(photoCount = 5, videoCount = 0))
        assertEquals("1 video", DateUtils.formatMediaSubtitle(photoCount = 0, videoCount = 1))
        assertEquals("3 videos", DateUtils.formatMediaSubtitle(photoCount = 0, videoCount = 3))
        assertEquals("1 photo • 1 video", DateUtils.formatMediaSubtitle(photoCount = 1, videoCount = 1))
        assertEquals("4 photos • 2 videos", DateUtils.formatMediaSubtitle(photoCount = 4, videoCount = 2))
    }

    @Test
    fun groupPhotosByDate_maintainsStrictDescendingOrder() {
        // Today is 2026-09-16
        // Today: epoch ms for 2026-09-16 12:00:00 UTC = 1789560000000L
        val todayMs = fixedToday.atStartOfDay(zoneId).toInstant().toEpochMilli() + 3600000L
        val yesterdayMs = fixedToday.minusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() + 3600000L
        val sept14Ms = fixedToday.minusDays(2).atStartOfDay(zoneId).toInstant().toEpochMilli() + 3600000L

        // Provide items out of order (Sept 14 first, then Today, then Yesterday)
        val items = listOf(
            createItem(id = 1, dateTaken = sept14Ms),
            createItem(id = 2, dateTaken = todayMs),
            createItem(id = 3, dateTaken = yesterdayMs),
            createItem(id = 4, dateTaken = sept14Ms + 1000L),
            createItem(id = 5, dateTaken = 0L), // Earlier
        )

        val grouped = DateUtils.groupPhotosByDate(items, zoneId = zoneId, today = fixedToday)

        val headers = grouped.keys.toList()
        assertEquals(listOf("Today", "Yesterday", "September 14", "Earlier"), headers)

        // Today has item 2
        assertEquals(1, grouped["Today"]?.size)
        assertEquals(2L, grouped["Today"]?.first()?.id)

        // Yesterday has item 3
        assertEquals(1, grouped["Yesterday"]?.size)
        assertEquals(3L, grouped["Yesterday"]?.first()?.id)

        // September 14 has items 4 and 1 (sorted descending by timestamp)
        val sept14Items = grouped["September 14"]!!
        assertEquals(2, sept14Items.size)
        assertEquals(4L, sept14Items[0].id)
        assertEquals(1L, sept14Items[1].id)

        // Earlier has item 5
        assertEquals(1, grouped["Earlier"]?.size)
        assertEquals(5L, grouped["Earlier"]?.first()?.id)
    }

    @Test
    fun groupPhotosByDate_subtitleFormattingMatchesActualMedia() {
        val todayMs = fixedToday.atStartOfDay(zoneId).toInstant().toEpochMilli() + 3600000L
        val items = listOf(
            createItem(id = 1, dateTaken = todayMs, mediaType = MediaType.PHOTO),
            createItem(id = 2, dateTaken = todayMs + 10L, mediaType = MediaType.VIDEO),
        )

        val grouped = DateUtils.groupPhotosByDate(items, zoneId = zoneId, today = fixedToday)
        val todayItems = grouped["Today"]!!
        val subtitle = DateUtils.formatMediaSubtitle(todayItems)

        assertEquals("1 photo • 1 video", subtitle)
    }

    @Test
    fun groupPhotosByDate_withDuplicateUris_deduplicatesGracefully() {
        val todayMs = fixedToday.atStartOfDay(zoneId).toInstant().toEpochMilli() + 3600000L
        val item1 = createItem(id = 1000138102L, dateTaken = todayMs)
        // itemDuplicate has same URI
        val itemDuplicate = createItem(id = 1000138102L, dateTaken = todayMs)

        val items = listOf(item1, itemDuplicate)
        val grouped = DateUtils.groupPhotosByDate(items, zoneId = zoneId, today = fixedToday)
        val todayItems = grouped["Today"]!!

        assertEquals("Should deduplicate items with identical URI", 1, todayItems.size)
        assertEquals(1000138102L, todayItems[0].id)
    }
}
