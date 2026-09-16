package `in`.heyvinay.swipepix.ui.cleanup

import android.net.FakeUri
import androidx.compose.ui.unit.dp
import `in`.heyvinay.swipepix.data.model.MediaItem
import `in`.heyvinay.swipepix.data.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveMediaSurfaceTest {

    private val stageWidth = 360.dp
    private val stageHeight = 480.dp

    @Test
    fun testLandscape16x9_fitsWidthAndScalesHeight() {
        val size = calculateAdaptiveMediaSize(
            aspectRatio = 16f / 9f,
            maxStageWidth = stageWidth,
            maxStageHeight = stageHeight
        )
        assertEquals(stageWidth, size.width)
        assertTrue(size.height < stageHeight)
        assertTrue(size.height.value in 200f..205f)
    }

    @Test
    fun testPortrait3x4_matchesStageAspectExactly() {
        // Stage is 360 / 480 = 0.75
        val size = calculateAdaptiveMediaSize(
            aspectRatio = 3f / 4f,
            maxStageWidth = stageWidth,
            maxStageHeight = stageHeight
        )
        assertEquals(stageWidth, size.width)
        assertEquals(stageHeight, size.height)
    }

    @Test
    fun testTallPortrait9x16_fitsHeightAndScalesWidth() {
        val size = calculateAdaptiveMediaSize(
            aspectRatio = 9f / 16f,
            maxStageWidth = stageWidth,
            maxStageHeight = stageHeight
        )
        assertEquals(stageHeight, size.height)
        assertTrue(size.width < stageWidth)
        assertTrue(size.width.value in 268f..272f)
    }

    @Test
    fun testSquare1x1_fitsWidthAndMatchesWidthHeight() {
        val size = calculateAdaptiveMediaSize(
            aspectRatio = 1.0f,
            maxStageWidth = stageWidth,
            maxStageHeight = stageHeight
        )
        assertEquals(stageWidth, size.width)
        assertEquals(stageWidth, size.height)
    }

    @Test
    fun testExtremeWideClamping_clampedTo2_8() {
        val size = calculateAdaptiveMediaSize(
            aspectRatio = 5.0f,
            maxStageWidth = stageWidth,
            maxStageHeight = stageHeight
        )
        assertEquals(stageWidth, size.width)
        val expectedHeight = (stageWidth.value / 2.8f).dp
        assertEquals(expectedHeight.value, size.height.value, 0.1f)
    }

    @Test
    fun testExtremeTallClamping_clampedTo0_35() {
        val size = calculateAdaptiveMediaSize(
            aspectRatio = 0.1f,
            maxStageWidth = stageWidth,
            maxStageHeight = stageHeight
        )
        assertEquals(stageHeight, size.height)
        val expectedWidth = (stageHeight.value * 0.35f).dp
        assertEquals(expectedWidth.value, size.width.value, 0.1f)
    }

    @Test
    fun testInvalidAspect_fallsBackTo0_75() {
        val size = calculateAdaptiveMediaSize(
            aspectRatio = 0f,
            maxStageWidth = stageWidth,
            maxStageHeight = stageHeight
        )
        assertEquals(stageWidth, size.width)
        assertEquals(stageHeight, size.height)
    }

    @Test
    fun testMediaItem_orientationSwapping_preventsLandscapeCardForPortraitSensor() {
        // Sensor captures raw 4000x3000 with EXIF orientation 90
        val rawPhoto = MediaItem(
            id = 1L,
            contentUri = FakeUri("content://media/external/images/media/1"),
            displayName = "portrait_shot.jpg",
            mimeType = "image/jpeg",
            dateAdded = 1000L,
            dateTaken = 1000L,
            dateModified = 1000L,
            size = 5000000L,
            width = 4000,
            height = 3000,
            bucketId = "1",
            bucketDisplayName = "Camera",
            mediaType = MediaType.PHOTO,
            orientation = 90
        )

        // Visual dimensions must be swapped to 3000x4000 (aspect 0.75)
        assertEquals(3000, rawPhoto.displayedWidth)
        assertEquals(4000, rawPhoto.displayedHeight)
        assertEquals(0.75f, rawPhoto.displayedAspectRatio, 0.001f)

        // Sizing with displayedAspectRatio produces portrait card matching stage exactly
        val cardSize = calculateAdaptiveMediaSize(
            aspectRatio = rawPhoto.displayedAspectRatio,
            maxStageWidth = stageWidth,
            maxStageHeight = stageHeight
        )
        assertEquals(stageWidth, cardSize.width)
        assertEquals(stageHeight, cardSize.height)
    }

    @Test
    fun testMediaItem_orientation270_swapsWidthHeight() {
        val rawPhoto = MediaItem(
            id = 2L,
            contentUri = FakeUri("content://media/external/images/media/2"),
            displayName = "portrait_270.jpg",
            mimeType = "image/jpeg",
            dateAdded = 1000L,
            dateTaken = 1000L,
            dateModified = 1000L,
            size = 5000000L,
            width = 3840,
            height = 2160,
            bucketId = "1",
            bucketDisplayName = "Camera",
            mediaType = MediaType.PHOTO,
            orientation = 270
        )

        assertEquals(2160, rawPhoto.displayedWidth)
        assertEquals(3840, rawPhoto.displayedHeight)
        assertEquals(2160f / 3840f, rawPhoto.displayedAspectRatio, 0.001f)
    }

    @Test
    fun testMediaItem_orientation0and180_preservesDimensions() {
        val photo0 = MediaItem(
            id = 3L,
            contentUri = FakeUri("content://media/external/images/media/3"),
            displayName = "landscape_0.jpg",
            mimeType = "image/jpeg",
            dateAdded = 1000L,
            dateTaken = 1000L,
            dateModified = 1000L,
            size = 5000000L,
            width = 1920,
            height = 1080,
            bucketId = "1",
            bucketDisplayName = "Camera",
            mediaType = MediaType.PHOTO,
            orientation = 0
        )
        assertEquals(1920, photo0.displayedWidth)
        assertEquals(1080, photo0.displayedHeight)

        val photo180 = photo0.copy(orientation = 180)
        assertEquals(1920, photo180.displayedWidth)
        assertEquals(1080, photo180.displayedHeight)
    }
}
