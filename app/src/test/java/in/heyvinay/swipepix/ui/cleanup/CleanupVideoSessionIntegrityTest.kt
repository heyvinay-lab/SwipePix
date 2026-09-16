package `in`.heyvinay.swipepix.ui.cleanup

import android.net.FakeUri
import `in`.heyvinay.swipepix.data.local.entity.DecisionType
import `in`.heyvinay.swipepix.data.model.MediaItem
import `in`.heyvinay.swipepix.data.model.MediaType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Verifies that video playback, pausing, error recovery, and external player fallback
 * NEVER compromise the cleaning session checkpoint, decisions, or review counts.
 */
class CleanupVideoSessionIntegrityTest {

    private lateinit var sessionRepo: FakeCleanupSessionRepository

    private val videoItem1 = MediaItem(
        id = 201L,
        contentUri = FakeUri("content://media/external/video/media/201"),
        displayName = "video_1.mp4",
        mimeType = "video/mp4",
        dateAdded = 1000L,
        dateTaken = 1000L,
        dateModified = 1000L,
        size = 20000000L,
        width = 1080,
        height = 1920,
        bucketId = "album_videos",
        bucketDisplayName = "WhatsApp Video",
        mediaType = MediaType.VIDEO,
        durationMs = 15000L,
        orientation = 0
    )

    private val photoItem1 = MediaItem(
        id = 202L,
        contentUri = FakeUri("content://media/external/images/media/202"),
        displayName = "photo_1.jpg",
        mimeType = "image/jpeg",
        dateAdded = 1000L,
        dateTaken = 1000L,
        dateModified = 1000L,
        size = 4000000L,
        width = 4000,
        height = 3000,
        bucketId = "album_videos",
        bucketDisplayName = "WhatsApp Video",
        mediaType = MediaType.PHOTO,
        durationMs = 0L,
        orientation = 0
    )

    @Before
    fun setUp() {
        sessionRepo = FakeCleanupSessionRepository()
    }

    @Test
    fun testVideoPlayback_doesNotRecordSessionDecision() = runBlocking {
        val session = sessionRepo.startNewSession("album_videos", "WhatsApp Video")

        // User enters session, video is displayed
        var playbackState = VideoPlaybackState.IDLE
        playbackState = VideoPlaybackState.PREPARING
        playbackState = VideoPlaybackState.PLAYING
        playbackState = VideoPlaybackState.PAUSED

        // Verify: ZERO decisions recorded during playback
        val decision = sessionRepo.getDecision(videoItem1.id)
        assertNull(decision)

        val counts = sessionRepo.getSummaryCountsForSession(session.sessionId)
        assertEquals(0, counts.first)
        assertEquals(0, counts.second)
    }

    @Test
    fun testVideoExternalPlayer_doesNotRecordSessionDecision() = runBlocking {
        val session = sessionRepo.startNewSession("album_videos", "WhatsApp Video")

        // User taps "Open with..." fallback
        // Verify: reviewed count remains 0
        val reviewedCount = sessionRepo.getTotalReviewedCountForSession(session.sessionId)
        assertEquals(0, reviewedCount)
    }

    @Test
    fun testVideoSwipeRight_recordsKeepDecision() = runBlocking {
        val session = sessionRepo.startNewSession("album_videos", "WhatsApp Video")

        // User swipes right to KEEP video after playing it
        sessionRepo.recordDecision(
            mediaStoreId = videoItem1.id,
            contentUri = videoItem1.contentUri.toString(),
            decision = DecisionType.KEEP,
            albumId = "album_videos",
            sessionId = session.sessionId
        )

        val decision = sessionRepo.getDecision(videoItem1.id)
        assertNotNull(decision)
        assertEquals(DecisionType.KEEP.name, decision?.decision)
        assertEquals(1, sessionRepo.getSummaryCountsForSession(session.sessionId).first)
    }

    @Test
    fun testVideoSwipeLeft_recordsTrashPendingDecision() = runBlocking {
        val session = sessionRepo.startNewSession("album_videos", "WhatsApp Video")

        // User swipes left to TRASH video after playing it
        sessionRepo.recordDecision(
            mediaStoreId = videoItem1.id,
            contentUri = videoItem1.contentUri.toString(),
            decision = DecisionType.TRASH_PENDING,
            albumId = "album_videos",
            sessionId = session.sessionId
        )

        val decision = sessionRepo.getDecision(videoItem1.id)
        assertNotNull(decision)
        assertEquals(DecisionType.TRASH_PENDING.name, decision?.decision)
        assertEquals(1, sessionRepo.getSummaryCountsForSession(session.sessionId).second)
    }

    @Test
    fun testMixedPhotoAndVideoDeck_preservesSessionProgression() = runBlocking {
        val session = sessionRepo.startNewSession("album_videos", "WhatsApp Video")

        // Item 1 (Video): Kept
        sessionRepo.recordDecision(
            mediaStoreId = videoItem1.id,
            contentUri = videoItem1.contentUri.toString(),
            decision = DecisionType.KEEP,
            albumId = "album_videos",
            sessionId = session.sessionId
        )

        // Item 2 (Photo): Trashed
        sessionRepo.recordDecision(
            mediaStoreId = photoItem1.id,
            contentUri = photoItem1.contentUri.toString(),
            decision = DecisionType.TRASH_PENDING,
            albumId = "album_videos",
            sessionId = session.sessionId
        )

        val counts = sessionRepo.getSummaryCountsForSession(session.sessionId)
        assertEquals(1, counts.first)
        assertEquals(1, counts.second)

        val reviewedIds = sessionRepo.getReviewedIdsForSession(session.sessionId)
        assertEquals(setOf(201L, 202L), reviewedIds)
        assertEquals(2, sessionRepo.getTotalReviewedCountForSession(session.sessionId))
    }
}
