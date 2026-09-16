package `in`.heyvinay.swipepix.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.Dp

/**
 * Centralized motion tokens for SwipePix.
 *
 * Enforces visual hierarchy, tactile responsiveness, and performance consistency:
 * - FAST (100–150ms): immediate toggles, chrome fade, micro-feedback.
 * - STANDARD (180–250ms): tab transitions, page navigation, dialogs.
 * - EMPHASIS (250–350ms): modal expansions, card entries.
 * - ENTER / EXIT: screen navigation specs with matching decelerate/accelerate curves.
 */
object SwipePixMotion {

    // Standard Durations (ms)
    const val DURATION_FAST = 120
    const val DURATION_STANDARD = 220
    const val DURATION_EMPHASIS = 300
    const val DURATION_ENTER = 240
    const val DURATION_EXIT = 180

    // Easings
    val EASING_STANDARD: Easing = FastOutSlowInEasing
    val EASING_EMPHASIS: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val EASING_DECELERATE: Easing = LinearOutSlowInEasing
    val EASING_ACCELERATE: Easing = FastOutLinearInEasing

    // Interactive Springs
    val SPRING_TACTILE = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessHigh,
    )

    val SPRING_SLIDE_PILL = spring<Dp>(
        dampingRatio = 0.82f,
        stiffness = 380f,
    )

    val SPRING_CARD = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessHigh,
    )

    // Standard Tweens
    fun <T> fastTween() = tween<T>(
        durationMillis = DURATION_FAST,
        easing = EASING_STANDARD,
    )

    fun <T> standardTween() = tween<T>(
        durationMillis = DURATION_STANDARD,
        easing = EASING_STANDARD,
    )

    fun <T> enterTween() = tween<T>(
        durationMillis = DURATION_ENTER,
        easing = EASING_DECELERATE,
    )

    fun <T> exitTween() = tween<T>(
        durationMillis = DURATION_EXIT,
        easing = EASING_ACCELERATE,
    )
}
