package `in`.heyvinay.swipepix.ui.video

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.heyvinay.swipepix.ui.theme.SwipePixMotion
import kotlin.math.roundToInt

/**
 * High-performance, touch-friendly seek slider and scrubber for [SwipePixVideoPlayer].
 *
 * Key Architectural Highlights:
 * 1. Strict Pointer Consumption: All down/drag/up pointer changes are explicitly consumed,
 *    preventing parent gesture detectors (such as horizontal card swipes in Cleaning mode
 *    or pager swipes in PhotoViewer) from intercepting seek interactions.
 * 2. Visual Scrubbing Feedback: Thumb dynamically enlarges and illuminates during active drag.
 * 3. Formatted Elapsed and Duration Labels: Supports `mm:ss` (< 1h) and `hh:mm:ss` (>= 1h).
 * 4. Micro-Scrubbing Accuracy: Clamps scrub progress between 0f and 1f with real-time feedback.
 */
@Composable
fun SwipePixVideoSeekSlider(
    currentPositionMs: Long,
    durationMs: Long,
    onScrub: (positionMs: Long) -> Unit,
    onScrubFinished: (positionMs: Long) -> Unit,
    modifier: Modifier = Modifier,
    activeTrackColor: Color = MaterialTheme.colorScheme.primary,
    inactiveTrackColor: Color = Color.White.copy(alpha = 0.24f),
    thumbColor: Color = Color.White,
    showTimeLabels: Boolean = true,
    trackHeight: Dp = 4.dp,
) {
    val currentOnScrub by rememberUpdatedState(onScrub)
    val currentOnScrubFinished by rememberUpdatedState(onScrubFinished)

    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val normalFraction = if (durationMs > 0) {
        (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    val displayFraction = if (isDragging) dragFraction else normalFraction
    val displayPositionMs = if (isDragging) {
        (dragFraction * durationMs).toLong()
    } else {
        currentPositionMs
    }

    val thumbSize by animateDpAsState(
        targetValue = if (isDragging) 16.dp else 10.dp,
        animationSpec = tween(SwipePixMotion.DURATION_FAST),
        label = "thumbSize"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (showTimeLabels) {
            // Elapsed Time
            Text(
                text = SwipePixPlaybackState.formatTime(displayPositionMs, durationMs),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.width(if (durationMs >= 3600000L) 56.dp else 38.dp),
            )
        }

        // Draggable Scrubber Bar Container
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .height(36.dp) // Generous touch target area (36dp height)
                .pointerInput(durationMs) {
                    // Tap-to-seek
                    detectTapGestures(
                        onPress = { offset ->
                            if (durationMs <= 0L) return@detectTapGestures
                            val widthPx = size.width.toFloat()
                            if (widthPx > 0) {
                                val fraction = (offset.x / widthPx).coerceIn(0f, 1f)
                                val targetMs = (fraction * durationMs).toLong()
                                currentOnScrub(targetMs)
                                currentOnScrubFinished(targetMs)
                            }
                        }
                    )
                }
                .pointerInput(durationMs) {
                    // Drag-to-scrub
                    detectDragGestures(
                        onDragStart = { offset ->
                            if (durationMs <= 0L) return@detectDragGestures
                            isDragging = true
                            val widthPx = size.width.toFloat()
                            val initialFraction = if (widthPx > 0) (offset.x / widthPx).coerceIn(0f, 1f) else 0f
                            dragFraction = initialFraction
                            currentOnScrub((initialFraction * durationMs).toLong())
                        },
                        onDrag = { change, _ ->
                            change.consume() // STRICT CONSUMPTION: Prevents card swipe / pager swipe
                            if (durationMs <= 0L) return@detectDragGestures
                            val widthPx = size.width.toFloat()
                            if (widthPx > 0) {
                                val newFraction = (change.position.x / widthPx).coerceIn(0f, 1f)
                                dragFraction = newFraction
                                currentOnScrub((newFraction * durationMs).toLong())
                            }
                        },
                        onDragEnd = {
                            if (durationMs > 0L) {
                                currentOnScrubFinished((dragFraction * durationMs).toLong())
                            }
                            isDragging = false
                        },
                        onDragCancel = {
                            if (durationMs > 0L) {
                                currentOnScrubFinished((dragFraction * durationMs).toLong())
                            }
                            isDragging = false
                        }
                    )
                },
            contentAlignment = Alignment.CenterStart
        ) {
            val totalWidth = maxWidth

            // 1. Inactive Background Track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(trackHeight)
                    .clip(RoundedCornerShape(trackHeight / 2))
                    .background(inactiveTrackColor)
            )

            // 2. Active Filled Track
            Box(
                modifier = Modifier
                    .width(totalWidth * displayFraction)
                    .height(trackHeight)
                    .clip(RoundedCornerShape(trackHeight / 2))
                    .background(activeTrackColor)
            )

            // 3. Draggable Glowing Thumb
            val density = LocalDensity.current
            val thumbSizePx = with(density) { thumbSize.toPx() }
            val thumbOffsetXPx = (constraints.maxWidth * displayFraction) - (thumbSizePx / 2f)

            Box(
                modifier = Modifier
                    .offset { IntOffset(thumbOffsetXPx.roundToInt().coerceAtLeast(0), 0) }
                    .size(thumbSize)
                    .shadow(elevation = if (isDragging) 6.dp else 2.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(thumbColor)
            )
        }

        if (showTimeLabels) {
            // Total Duration
            Text(
                text = SwipePixPlaybackState.formatTime(durationMs, durationMs),
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.width(if (durationMs >= 3600000L) 56.dp else 38.dp),
            )
        }
    }
}
