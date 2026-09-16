package `in`.heyvinay.swipepix.ui.cleanup

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween

/**
 * A modifier that enables Tinder-style swiping for cards.
 * @deprecated Replaced by Modifier.swipeCardPhysics in SwipeCardPhysics.kt
 */
@Deprecated(
    message = "Replaced by Modifier.swipeCardPhysics for improved spring physics and performance",
    replaceWith = ReplaceWith("swipeCardPhysics(state = state)"),
    level = DeprecationLevel.WARNING
)
fun Modifier.swipeableCard(
    onSwipedLeft: () -> Unit,
    onSwipedRight: () -> Unit,
    swipeThreshold: Float = 400f,
): Modifier = composed {
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val rotation = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    
    val screenWidth = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val maximumDrag = screenWidth * 1.5f

    this
        .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
        .graphicsLayer(
            rotationZ = rotation.value,
        )
        .pointerInput(Unit) {
            detectDragGestures(
                onDragEnd = {
                    scope.launch {
                        if (offsetX.value > swipeThreshold) {
                            // Fly out to right
                            launch { offsetX.animateTo(maximumDrag, tween(300)) }
                            launch { rotation.animateTo(15f, tween(300)) }
                            onSwipedRight()
                        } else if (offsetX.value < -swipeThreshold) {
                            // Fly out to left
                            launch { offsetX.animateTo(-maximumDrag, tween(300)) }
                            launch { rotation.animateTo(-15f, tween(300)) }
                            onSwipedLeft()
                        } else {
                            // Snap back to center
                            launch { offsetX.animateTo(0f, tween(300)) }
                            launch { offsetY.animateTo(0f, tween(300)) }
                            launch { rotation.animateTo(0f, tween(300)) }
                        }
                    }
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    scope.launch {
                        offsetX.snapTo(offsetX.value + dragAmount.x)
                        offsetY.snapTo(offsetY.value + dragAmount.y)
                        
                        // Rotate slightly based on x offset, max 15 degrees at screen edge
                        val targetRotation = (offsetX.value / screenWidth) * 15f
                        rotation.snapTo(targetRotation)
                    }
                }
            )
        }
}
