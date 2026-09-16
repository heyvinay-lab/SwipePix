package `in`.heyvinay.swipepix.ui.viewer

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import `in`.heyvinay.swipepix.data.model.MediaItem
import `in`.heyvinay.swipepix.data.model.MediaType
import `in`.heyvinay.swipepix.ui.theme.SwipePixMotion
import `in`.heyvinay.swipepix.ui.util.DateUtils
import `in`.heyvinay.swipepix.ui.video.SwipePixVideoPlayer
import `in`.heyvinay.swipepix.ui.video.VideoPlayerMode
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * Adaptive Visual Media Surface for SwipePix Photo Viewer.
 *
 * Architecture:
 * - Dynamically derives aspect-ratio frame from native width & height.
 * - At resting state (scale <= 1.05f):
 *   - Clamped within available stage bounds between top bar and bottom info panel
 *   - 16dp rounded corners with subtle translucent border
 *   - Soft depth elevation shadow
 *   - Real video badge with duration and centered play action
 *   - Floating zoom level pill (e.g. "1.0×") and expand button (OpenInFull)
 * - When zoomed (scale > 1.05f):
 *   - Corner radius animates to 0dp and clipping disabled
 *   - Unconstrained pinch-to-zoom (up to 5x) & panning across entire canvas
 *   - Double-tap or expand button resets smoothly to 1.0x
 */
@Composable
fun ZoomableImage(
    mediaItem: MediaItem,
    modifier: Modifier = Modifier,
    onTap: () -> Unit = {},
    showChrome: Boolean = true,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val toggleZoom: () -> Unit = {
        scope.launch {
            val targetScale = if (scale > 1.05f) 1f else 2.5f
            val startScale = scale
            val startOffset = offset
            animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = tween(SwipePixMotion.DURATION_FAST, easing = FastOutSlowInEasing)
            ) { progress, _ ->
                scale = startScale + (targetScale - startScale) * progress
                offset = Offset(
                    x = startOffset.x * (1f - progress),
                    y = startOffset.y * (1f - progress)
                )
            }
        }
    }

    val cornerRadius by animateDpAsState(
        targetValue = if (scale > 1.05f) 0.dp else 16.dp,
        animationSpec = tween(SwipePixMotion.DURATION_FAST),
        label = "viewerCornerRadius"
    )

    val topReserved by animateDpAsState(
        targetValue = if (showChrome) 68.dp else 0.dp,
        animationSpec = tween(SwipePixMotion.DURATION_FAST),
        label = "viewerTopReserved"
    )

    val bottomReserved by animateDpAsState(
        targetValue = if (showChrome) 180.dp else 0.dp,
        animationSpec = tween(SwipePixMotion.DURATION_FAST),
        label = "viewerBottomReserved"
    )

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val stageWidth = maxWidth
        val availableHeight = (maxHeight - topReserved - bottomReserved).coerceAtLeast(100.dp)
        val restingOffsetY = (topReserved - bottomReserved) / 2
        val stageOffsetY by animateDpAsState(
            targetValue = if (scale > 1.05f) 0.dp else restingOffsetY,
            animationSpec = tween(SwipePixMotion.DURATION_FAST),
            label = "viewerStageOffsetY"
        )

        // Full gesture surface for pan, pinch-zoom, and canvas taps
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onTap() },
                        onDoubleTap = { toggleZoom() }
                    )
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        do {
                            val event = awaitPointerEvent()
                            val pointerCount = event.changes.count { it.pressed }
                            if (pointerCount >= 2) {
                                val zoomChange = event.calculateZoom()
                                val panChange = event.calculatePan()
                                scale = (scale * zoomChange).coerceIn(1f, 5f)
                                if (scale > 1.05f) {
                                    offset += panChange
                                } else {
                                    offset = Offset.Zero
                                }
                                event.changes.forEach { it.consume() }
                            } else if (pointerCount == 1 && scale > 1.05f) {
                                val panChange = event.calculatePan()
                                offset += panChange
                                event.changes.forEach { it.consume() }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
        )

        val hasValidDimensions = mediaItem.displayedWidth > 0 && mediaItem.displayedHeight > 0
        val (targetWidth, targetHeight) = if (hasValidDimensions) {
            val mediaRatio = mediaItem.displayedAspectRatio
            val stageRatio = stageWidth.value / availableHeight.value
            if (mediaRatio > stageRatio) {
                stageWidth to (stageWidth / mediaRatio)
            } else {
                (availableHeight * mediaRatio) to availableHeight
            }
        } else {
            stageWidth to availableHeight
        }

        val thumbnailKey = mediaItem.contentUri.toString() + "_256"
        val fullKey = mediaItem.contentUri.toString() + "_full"

        // 1. Scalable & Pannable Media Box
        Box(
            modifier = Modifier
                .size(targetWidth, targetHeight)
                .offset(y = stageOffsetY)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                    shape = RoundedCornerShape(cornerRadius)
                    clip = scale <= 1.05f
                    shadowElevation = if (scale <= 1.05f) 12f else 0f
                }
                .then(
                    if (scale <= 1.05f) {
                        Modifier.border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(cornerRadius)
                        )
                    } else Modifier
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (mediaItem.mediaType == MediaType.VIDEO) {
                SwipePixVideoPlayer(
                    mediaItem = mediaItem,
                    mode = VideoPlayerMode.VIEWER,
                    isInteractive = scale <= 1.05f,
                    shape = RoundedCornerShape(cornerRadius),
                    externalControlsVisible = showChrome,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(mediaItem.contentUri)
                        .placeholderMemoryCacheKey(thumbnailKey)
                        .memoryCacheKey(fullKey)
                        .diskCacheKey(fullKey)
                        .crossfade(false)
                        .build(),
                    contentDescription = mediaItem.displayName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        // 2. Overlay Zoom Controls (Anchored to the image frame bounds when resting, docked at bottom-end when zoomed)
        Box(
            modifier = if (scale <= 1.05f) {
                Modifier
                    .size(targetWidth, targetHeight)
                    .offset(y = stageOffsetY)
            } else {
                Modifier
                    .fillMaxSize()
                    .padding(
                        end = 16.dp,
                        bottom = if (showChrome) bottomReserved + 16.dp else 40.dp
                    )
            },
            contentAlignment = Alignment.BottomEnd,
        ) {
            AnimatedVisibility(
                visible = (showChrome || scale > 1.05f) && mediaItem.mediaType != MediaType.VIDEO,
                enter = fadeIn(tween(SwipePixMotion.DURATION_FAST)),
                exit = fadeOut(tween(SwipePixMotion.DURATION_FAST)),
                modifier = if (scale <= 1.05f) Modifier.padding(12.dp) else Modifier,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // Zoom indicator pill (clickable to toggle zoom)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.70f))
                            .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(12.dp))
                            .clickable { toggleZoom() }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.1f×", scale),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }

                    // Expand / Reset button (clickable to toggle zoom)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.70f))
                            .border(1.dp, Color.White.copy(alpha = 0.20f), CircleShape)
                            .clickable { toggleZoom() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (scale > 1.05f) Icons.Default.CloseFullscreen else Icons.Default.OpenInFull,
                            contentDescription = if (scale > 1.05f) "Reset Zoom" else "Zoom 2.5x",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

