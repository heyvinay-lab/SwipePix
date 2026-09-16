package `in`.heyvinay.swipepix.ui.cleanup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import `in`.heyvinay.swipepix.data.model.MediaItem
import `in`.heyvinay.swipepix.data.model.MediaType
import `in`.heyvinay.swipepix.ui.theme.LocalDarkTheme
import `in`.heyvinay.swipepix.ui.util.DateUtils
import `in`.heyvinay.swipepix.ui.util.VideoIntentUtils
import `in`.heyvinay.swipepix.ui.video.SwipePixVideoPlayer
import `in`.heyvinay.swipepix.ui.video.VideoPlayerMode
import kotlin.math.abs

/**
 * Standard corner shape for the adaptive media container.
 */
val AdaptiveMediaShape = RoundedCornerShape(24.dp)

/**
 * Calculates the exact display dimensions (in Dp) for a media surface
 * so that it strictly fits within [maxStageWidth] and [maxStageHeight]
 * while matching [aspectRatio] (width / height).
 */
fun calculateAdaptiveMediaSize(
    aspectRatio: Float,
    maxStageWidth: Dp,
    maxStageHeight: Dp,
): DpSize {
    val clampedAspect = if (aspectRatio > 0f) aspectRatio.coerceIn(0.35f, 2.8f) else 0.75f
    val stageAspect = if (maxStageHeight.value > 0f) {
        maxStageWidth.value / maxStageHeight.value
    } else {
        1f
    }

    return if (clampedAspect > stageAspect) {
        // Width-constrained: media is wider than stage (landscape, square, panorama)
        val targetWidth = maxStageWidth
        val targetHeight = (maxStageWidth.value / clampedAspect).dp
        DpSize(targetWidth, targetHeight.coerceIn(120.dp, maxStageHeight))
    } else {
        // Height-constrained: media is taller than stage (portrait, vertical)
        val targetHeight = maxStageHeight
        val targetWidth = (maxStageHeight.value * clampedAspect).dp
        DpSize(targetWidth.coerceIn(120.dp, maxStageWidth), targetHeight)
    }
}

/**
 * Convenience / compatibility overload for raw pixel dimensions.
 */
fun calculateAdaptiveSize(
    mediaWidth: Int,
    mediaHeight: Int,
    maxStageWidth: Dp,
    maxStageHeight: Dp,
): DpSize {
    val aspect = if (mediaWidth > 0 && mediaHeight > 0) {
        mediaWidth.toFloat() / mediaHeight.toFloat()
    } else {
        0.75f
    }
    return calculateAdaptiveMediaSize(aspect, maxStageWidth, maxStageHeight)
}

/**
 * Adaptive Visual Media Surface for the Swipe Cleaning Flow.
 *
 * Architecture:
 * - FRAME SIZE == IMAGE SURFACE SIZE: The visible rounded container exactly matches
 *   the displayed media bounds with zero empty card padding, zero pillarboxing, and zero letterboxing.
 * - Single shared 24dp rounded corner shape ([AdaptiveMediaShape]) applied directly to the media surface.
 * - 1dp subtle border hugging the edge of the media.
 * - Handles EXIF orientation via [MediaItem.displayedAspectRatio] from the initial composition frame.
 * - Dynamic fallback reconciliation via [AsyncImagePainter.State.Success] intrinsicSize if MediaStore
 *   dimensions were missing or unpopulated.
 * - In-deck video playback via [InDeckVideoPlayer] (Hardware-accelerated TextureView + MediaPlayer)
 *   when [isInteractive] is true.
 * - External player fallback ("Open with...") via [VideoIntentUtils].
 */
@Composable
fun AdaptiveMediaSurface(
    mediaItem: MediaItem,
    maxStageWidth: Dp,
    maxStageHeight: Dp,
    modifier: Modifier = Modifier,
    isInteractive: Boolean = true,
    content: @Composable (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val isDark = LocalDarkTheme.current

    var intrinsicAspectRatio by remember(mediaItem.contentUri) { mutableFloatStateOf(0f) }

    val effectiveAspectRatio = if (intrinsicAspectRatio > 0f) {
        intrinsicAspectRatio
    } else {
        mediaItem.displayedAspectRatio
    }

    val surfaceSize = remember(effectiveAspectRatio, maxStageWidth, maxStageHeight) {
        calculateAdaptiveMediaSize(
            aspectRatio = effectiveAspectRatio,
            maxStageWidth = maxStageWidth,
            maxStageHeight = maxStageHeight,
        )
    }

    val thumbnailKey = remember(mediaItem.contentUri) { "${mediaItem.contentUri}_256" }
    val cardKey = remember(mediaItem.contentUri) { "${mediaItem.contentUri}_card" }

    val imageRequest = remember(mediaItem.contentUri) {
        ImageRequest.Builder(context)
            .data(mediaItem.contentUri)
            .placeholderMemoryCacheKey(thumbnailKey)
            .memoryCacheKey(cardKey)
            .crossfade(false)
            .build()
    }

    Box(
        modifier = modifier
            .size(surfaceSize.width, surfaceSize.height)
            .clip(AdaptiveMediaShape)
            .background(if (isDark) Color(0xFF0F1219) else Color(0xFF1E2430))
            .border(
                width = 1.dp,
                color = if (isDark) Color(0x2BFFFFFF) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = AdaptiveMediaShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (mediaItem.mediaType == MediaType.VIDEO) {
            SwipePixVideoPlayer(
                mediaItem = mediaItem,
                mode = VideoPlayerMode.CLEANING_DECK,
                isInteractive = isInteractive,
                shape = AdaptiveMediaShape,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            // Base Media Layer (Photos)
            AsyncImage(
                model = imageRequest,
                contentDescription = mediaItem.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onState = { state ->
                    if (state is AsyncImagePainter.State.Success) {
                        val size = state.painter.intrinsicSize
                        if (size.width > 0 && size.height > 0 &&
                            !size.width.isNaN() && !size.height.isNaN() &&
                            !size.width.isInfinite() && !size.height.isInfinite()
                        ) {
                            val ratio = size.width / size.height
                            if (abs(ratio - intrinsicAspectRatio) > 0.01f) {
                                intrinsicAspectRatio = ratio
                            }
                        }
                    }
                },
            )
        }

        // Overlay content (e.g. SwipeFeedbackOverlay)
        content?.invoke()
    }
}
