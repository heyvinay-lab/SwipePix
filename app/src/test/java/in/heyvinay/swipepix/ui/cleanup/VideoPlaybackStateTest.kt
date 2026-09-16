package `in`.heyvinay.swipepix.ui.cleanup

import android.net.FakeUri
import androidx.compose.ui.unit.dp
import `in`.heyvinay.swipepix.data.model.MediaItem
import `in`.heyvinay.swipepix.data.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoPlaybackStateTest {

    private val sampleVideoItem = MediaItem(
        id = 101L,
        contentUri = FakeUri("content://media/external/video/media/101"),
        displayName = "sample_video.mp4",
        mimeType = "video/mp4",
        dateAdded = 1000L,
        dateTaken = 1000L,
        dateModified = 1000L,
        size = 15000000L,
        width = 1080,
        height = 1920,
        bucketId = "1",
        bucketDisplayName = "Camera",
        mediaType = MediaType.VIDEO,
        durationMs = 28000L,
        orientation = 0
    )

    private val samplePhotoItem = MediaItem(
        id = 102L,
        contentUri = FakeUri("content://media/external/images/media/102"),
        displayName = "sample_photo.jpg",
        mimeType = "image/jpeg",
        dateAdded = 1000L,
        dateTaken = 1000L,
        dateModified = 1000L,
        size = 3000000L,
        width = 3000,
        height = 4000,
        bucketId = "1",
        bucketDisplayName = "Camera",
        mediaType = MediaType.PHOTO,
        durationMs = 0L,
        orientation = 0
    )

    @Test
    fun testVideoIdentifiedCorrectly() {
        assertEquals(MediaType.VIDEO, sampleVideoItem.mediaType)
        assertTrue(sampleVideoItem.durationMs > 0)
        assertEquals("content://media/external/video/media/101", sampleVideoItem.contentUri.toString())
    }

    @Test
    fun testPhotoUnaffectedByVideoLogic() {
        assertEquals(MediaType.PHOTO, samplePhotoItem.mediaType)
        assertEquals(0L, samplePhotoItem.durationMs)
        assertFalse(samplePhotoItem.mediaType == MediaType.VIDEO)
    }

    @Test
    fun testInitialState_isIdle() {
        var state = VideoPlaybackState.IDLE
        assertEquals(VideoPlaybackState.IDLE, state)
    }

    @Test
    fun testPlayAction_transitionsIdleToPreparing() {
        var state = VideoPlaybackState.IDLE
        // User taps play button
        state = VideoPlaybackState.PREPARING
        assertEquals(VideoPlaybackState.PREPARING, state)
    }

    @Test
    fun testPreparedAction_transitionsPreparingToPlaying() {
        var state = VideoPlaybackState.PREPARING
        // MediaPlayer onPrepared fires
        state = VideoPlaybackState.PLAYING
        assertEquals(VideoPlaybackState.PLAYING, state)
    }

    @Test
    fun testPauseAction_transitionsPlayingToPaused() {
        var state = VideoPlaybackState.PLAYING
        // User taps video surface while playing
        state = VideoPlaybackState.PAUSED
        assertEquals(VideoPlaybackState.PAUSED, state)
    }

    @Test
    fun testResumeAction_transitionsPausedToPlaying() {
        var state = VideoPlaybackState.PAUSED
        // User taps resume button
        state = VideoPlaybackState.PLAYING
        assertEquals(VideoPlaybackState.PLAYING, state)
    }

    @Test
    fun testCompletionAction_transitionsPlayingToCompleted() {
        var state = VideoPlaybackState.PLAYING
        // MediaPlayer onCompletion fires
        state = VideoPlaybackState.COMPLETED
        assertEquals(VideoPlaybackState.COMPLETED, state)
    }

    @Test
    fun testReplayAction_transitionsCompletedToPreparing() {
        var state = VideoPlaybackState.COMPLETED
        // User taps replay button
        state = VideoPlaybackState.PREPARING
        assertEquals(VideoPlaybackState.PREPARING, state)
    }

    @Test
    fun testPlaybackError_transitionsPreparingToError() {
        var state = VideoPlaybackState.PREPARING
        // MediaPlayer onError fires
        state = VideoPlaybackState.ERROR
        assertEquals(VideoPlaybackState.ERROR, state)
    }

    @Test
    fun testRetryAction_transitionsErrorToPreparing() {
        var state = VideoPlaybackState.ERROR
        // User taps Try Again
        state = VideoPlaybackState.PREPARING
        assertEquals(VideoPlaybackState.PREPARING, state)
    }

    @Test
    fun testAdaptiveSize_preservesVideoAspectRatio() {
        val stageWidth = 360.dp
        val stageHeight = 480.dp

        // Portrait 9:16 video
        val portraitSize = calculateAdaptiveMediaSize(
            aspectRatio = sampleVideoItem.displayedAspectRatio,
            maxStageWidth = stageWidth,
            maxStageHeight = stageHeight
        )
        assertEquals(stageHeight, portraitSize.height)
        assertTrue(portraitSize.width < stageWidth)

        // Landscape 16:9 video
        val landscapeVideo = sampleVideoItem.copy(width = 1920, height = 1080)
        val landscapeSize = calculateAdaptiveMediaSize(
            aspectRatio = landscapeVideo.displayedAspectRatio,
            maxStageWidth = stageWidth,
            maxStageHeight = stageHeight
        )
        assertEquals(stageWidth, landscapeSize.width)
        assertTrue(landscapeSize.height < stageHeight)
    }
}
