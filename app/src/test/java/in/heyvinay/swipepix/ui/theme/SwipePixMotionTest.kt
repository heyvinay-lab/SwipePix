package `in`.heyvinay.swipepix.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SwipePixMotionTest {

    @Test
    fun motionDurations_fallWithinExpectedPerformanceRanges() {
        // FAST: 100–150ms
        assertTrue(SwipePixMotion.DURATION_FAST in 100..150)
        assertEquals(120, SwipePixMotion.DURATION_FAST)

        // STANDARD: 180–250ms
        assertTrue(SwipePixMotion.DURATION_STANDARD in 180..250)
        assertEquals(220, SwipePixMotion.DURATION_STANDARD)

        // EMPHASIS: 250–350ms
        assertTrue(SwipePixMotion.DURATION_EMPHASIS in 250..350)
        assertEquals(300, SwipePixMotion.DURATION_EMPHASIS)

        // Screen Enter / Exit
        assertTrue(SwipePixMotion.DURATION_ENTER in 200..300)
        assertTrue(SwipePixMotion.DURATION_EXIT in 150..220)
    }

    @Test
    fun easingsAndSprings_areProperlyConfigured() {
        assertNotNull(SwipePixMotion.EASING_STANDARD)
        assertNotNull(SwipePixMotion.EASING_EMPHASIS)
        assertNotNull(SwipePixMotion.EASING_DECELERATE)
        assertNotNull(SwipePixMotion.EASING_ACCELERATE)
        assertNotNull(SwipePixMotion.SPRING_TACTILE)
        assertNotNull(SwipePixMotion.SPRING_SLIDE_PILL)
        assertNotNull(SwipePixMotion.SPRING_CARD)
    }

    @Test
    fun tweenGenerators_constructValidSpecs() {
        val fast = SwipePixMotion.fastTween<Float>()
        assertEquals(120, fast.durationMillis)

        val standard = SwipePixMotion.standardTween<Float>()
        assertEquals(220, standard.durationMillis)

        val enter = SwipePixMotion.enterTween<Float>()
        assertEquals(240, enter.durationMillis)

        val exit = SwipePixMotion.exitTween<Float>()
        assertEquals(180, exit.durationMillis)
    }
}
