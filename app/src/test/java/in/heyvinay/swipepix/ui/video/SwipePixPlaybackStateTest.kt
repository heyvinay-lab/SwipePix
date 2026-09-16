package `in`.heyvinay.swipepix.ui.video

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test suite for [SwipePixPlaybackState] and [PlaybackStatus].
 * Verifies playback status transitions, time formatting (mm:ss vs hh:mm:ss),
 * progress calculation, and bounds enforcement.
 */
class SwipePixPlaybackStateTest {

    @Test
    fun testDefaultState_isIdle() {
        val state = SwipePixPlaybackState()
        assertEquals(PlaybackStatus.IDLE, state.status)
        assertEquals(0L, state.currentPositionMs)
        assertEquals(0L, state.durationMs)
        assertFalse(state.isMuted)
        assertFalse(state.isPlaying)
        assertFalse(state.isPaused)
        assertFalse(state.isLoading)
        assertFalse(state.isError)
        assertFalse(state.isEnded)
        assertFalse(state.isReadyOrActive)
        assertEquals(0f, state.progress, 0.001f)
    }

    @Test
    fun testStatusFlags() {
        val preparingState = SwipePixPlaybackState(status = PlaybackStatus.PREPARING)
        assertTrue(preparingState.isLoading)
        assertFalse(preparingState.isPlaying)

        val bufferingState = SwipePixPlaybackState(status = PlaybackStatus.BUFFERING)
        assertTrue(bufferingState.isLoading)

        val readyState = SwipePixPlaybackState(status = PlaybackStatus.READY)
        assertTrue(readyState.isReadyOrActive)
        assertFalse(readyState.isPlaying)

        val playingState = SwipePixPlaybackState(status = PlaybackStatus.PLAYING)
        assertTrue(playingState.isPlaying)
        assertTrue(playingState.isReadyOrActive)
        assertFalse(playingState.isLoading)

        val pausedState = SwipePixPlaybackState(status = PlaybackStatus.PAUSED)
        assertTrue(pausedState.isPaused)
        assertTrue(pausedState.isReadyOrActive)
        assertFalse(pausedState.isPlaying)

        val completedState = SwipePixPlaybackState(status = PlaybackStatus.COMPLETED)
        assertTrue(completedState.isEnded)
        assertTrue(completedState.isReadyOrActive)
        assertFalse(completedState.isPlaying)

        val errorState = SwipePixPlaybackState(
            status = PlaybackStatus.ERROR,
            errorMessage = "Codec error"
        )
        assertTrue(errorState.isError)
        assertEquals("Codec error", errorState.errorMessage)
        assertFalse(errorState.isReadyOrActive)
    }

    @Test
    fun testProgressCalculation() {
        // Zero duration returns 0
        val zeroState = SwipePixPlaybackState(currentPositionMs = 5000L, durationMs = 0L)
        assertEquals(0f, zeroState.progress, 0.001f)

        // Halfway
        val halfState = SwipePixPlaybackState(currentPositionMs = 15000L, durationMs = 30000L)
        assertEquals(0.5f, halfState.progress, 0.001f)

        // Fully completed
        val fullState = SwipePixPlaybackState(currentPositionMs = 30000L, durationMs = 30000L)
        assertEquals(1.0f, fullState.progress, 0.001f)

        // Clamped if position exceeds duration
        val overState = SwipePixPlaybackState(currentPositionMs = 40000L, durationMs = 30000L)
        assertEquals(1.0f, overState.progress, 0.001f)
    }

    @Test
    fun testTimeFormatting_underOneHour() {
        // 0 seconds
        assertEquals("00:00", SwipePixPlaybackState.formatTime(0L))
        assertEquals("00:00", SwipePixPlaybackState.formatTime(-1000L))

        // 5 seconds
        assertEquals("00:05", SwipePixPlaybackState.formatTime(5000L))

        // 45 seconds
        assertEquals("00:45", SwipePixPlaybackState.formatTime(45000L))

        // 1 minute 15 seconds
        assertEquals("01:15", SwipePixPlaybackState.formatTime(75000L))

        // 12 minutes 34 seconds
        assertEquals("12:34", SwipePixPlaybackState.formatTime((12 * 60 + 34) * 1000L))

        // 59 minutes 59 seconds
        assertEquals("59:59", SwipePixPlaybackState.formatTime((59 * 60 + 59) * 1000L))
    }

    @Test
    fun testTimeFormatting_overOneHour() {
        // Exactly 1 hour
        val oneHourMs = 3600 * 1000L
        assertEquals("01:00:00", SwipePixPlaybackState.formatTime(oneHourMs))

        // 1 hour 23 minutes 45 seconds
        val complexMs = (3600 + 23 * 60 + 45) * 1000L
        assertEquals("01:23:45", SwipePixPlaybackState.formatTime(complexMs))

        // 10 hours 0 minutes 1 second
        val tenHoursMs = (10 * 3600 + 1) * 1000L
        assertEquals("10:00:01", SwipePixPlaybackState.formatTime(tenHoursMs))
    }

    @Test
    fun testTimeFormatting_forcesHoursWhenTotalDurationIsOverOneHour() {
        val totalOverHour = 3700 * 1000L // 1h 1m 40s

        // When total video duration is over 1 hour, elapsed time should display hh:mm:ss
        // even at the start (e.g. 00:00:15 / 01:01:40)
        assertEquals("00:00:00", SwipePixPlaybackState.formatTime(0L, totalOverHour))
        assertEquals("00:00:15", SwipePixPlaybackState.formatTime(15000L, totalOverHour))
        assertEquals("00:05:30", SwipePixPlaybackState.formatTime(330000L, totalOverHour))
        assertEquals("01:01:40", SwipePixPlaybackState.formatTime(totalOverHour, totalOverHour))
    }

    @Test
    fun testPlaybackStateFormatMethods() {
        val state = SwipePixPlaybackState(
            currentPositionMs = 25000L,
            durationMs = 90000L
        )
        assertEquals("00:25", state.formatPosition())
        assertEquals("01:30", state.formatDuration())
    }
}
