package `in`.heyvinay.swipepix.ui.cleanup

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class SwipeDirection {
    LEFT,
    RIGHT
}

@Stable
class SwipeCardState(
    val screenWidthPx: Float,
    val scope: CoroutineScope,
    private val onSwipeComplete: (SwipeDirection) -> Unit
) {
    val offsetX = Animatable(0f)
    val offsetY = Animatable(0f)
    val rotation = Animatable(0f)

    val threshold: Float = screenWidthPx * 0.35f
    val maximumOffscreen: Float = screenWidthPx * 1.5f

    val dragProgress: Float
        get() = (offsetX.value / threshold).coerceIn(-1f, 1f)

    val isSwipingLeft: Boolean
        get() = offsetX.value < -20f

    val isSwipingRight: Boolean
        get() = offsetX.value > 20f

    val feedbackAlpha: Float
        get() = abs(dragProgress).coerceIn(0f, 1f)

    suspend fun snap(x: Float, y: Float) {
        offsetX.snapTo(x)
        offsetY.snapTo(y * 0.15f)
        val targetRotation = ((x / screenWidthPx) * 16f).coerceIn(-16f, 16f)
        rotation.snapTo(targetRotation)
    }

    fun animateToCenter() {
        scope.launch {
            val springSpec = spring<Float>(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
            launch { offsetX.animateTo(0f, springSpec) }
            launch { offsetY.animateTo(0f, springSpec) }
            launch { rotation.animateTo(0f, springSpec) }
        }
    }

    fun swipe(direction: SwipeDirection, onFinished: () -> Unit = {}) {
        scope.launch {
            val targetX = if (direction == SwipeDirection.RIGHT) maximumOffscreen else -maximumOffscreen
            val targetRotation = if (direction == SwipeDirection.RIGHT) 18f else -18f
            val animSpec: AnimationSpec<Float> = tween(durationMillis = 240)

            launch { offsetY.animateTo(0f, animSpec) }
            launch { rotation.animateTo(targetRotation, animSpec) }
            offsetX.animateTo(targetX, animSpec)

            // Reset offsets BEFORE invoking onSwipeComplete so the incoming new card is never rendered at an offscreen position
            offsetX.snapTo(0f)
            offsetY.snapTo(0f)
            rotation.snapTo(0f)

            onSwipeComplete(direction)
            onFinished()
        }
    }

    fun animateUndoReturn(fromDirection: SwipeDirection, onSettled: () -> Unit = {}) {
        scope.launch {
            val startX = if (fromDirection == SwipeDirection.RIGHT) maximumOffscreen else -maximumOffscreen
            val startRotation = if (fromDirection == SwipeDirection.RIGHT) 16f else -16f
            offsetX.snapTo(startX)
            rotation.snapTo(startRotation)
            offsetY.snapTo(0f)

            val springSpec = spring<Float>(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMedium
            )
            launch { rotation.animateTo(0f, springSpec) }
            offsetX.animateTo(0f, springSpec)
            onSettled()
        }
    }
}

@Composable
fun rememberSwipeCardState(
    onSwipeComplete: (SwipeDirection) -> Unit
): SwipeCardState {
    val scope = rememberCoroutineScope()
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
    val screenWidthPx = with(LocalDensity.current) { screenWidthDp.toPx() }

    return remember(screenWidthPx) {
        SwipeCardState(
            screenWidthPx = screenWidthPx,
            scope = scope,
            onSwipeComplete = onSwipeComplete
        )
    }
}

fun Modifier.swipeCardGesture(
    state: SwipeCardState,
    enabled: Boolean = true
): Modifier = this
    .graphicsLayer {
        translationX = state.offsetX.value
        translationY = state.offsetY.value
        rotationZ = state.rotation.value
    }
    .pointerInput(enabled) {
        if (!enabled) return@pointerInput
        val velocityTracker = VelocityTracker()

        coroutineScope {
            var currentX = state.offsetX.value
            var currentY = state.offsetY.value

            detectDragGestures(
                onDragStart = {
                    velocityTracker.resetTracking()
                    currentX = state.offsetX.value
                    currentY = state.offsetY.value
                },
                onDragEnd = {
                    val velocity = velocityTracker.calculateVelocity()
                    val velX = velocity.x

                    // Directional integrity: A fling velocity is only valid in the direction of the drag
                    val isDraggedRight = state.offsetX.value > 0f
                    val isDraggedLeft = state.offsetX.value < 0f

                    val shouldSwipeRight = (state.offsetX.value > state.threshold) || (isDraggedRight && velX > 900f)
                    val shouldSwipeLeft = (state.offsetX.value < -state.threshold) || (isDraggedLeft && velX < -900f)

                    when {
                        shouldSwipeRight -> state.swipe(SwipeDirection.RIGHT)
                        shouldSwipeLeft -> state.swipe(SwipeDirection.LEFT)
                        else -> state.animateToCenter()
                    }
                },
                onDragCancel = {
                    state.animateToCenter()
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    velocityTracker.addPosition(change.uptimeMillis, change.position)
                    currentX += dragAmount.x
                    currentY += dragAmount.y
                    launch {
                        state.snap(currentX, currentY)
                    }
                }
            )
        }
    }
