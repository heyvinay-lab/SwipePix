package `in`.heyvinay.swipepix.ui.video

import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * High-level playback status states for the unified SwipePix video player.
 */
enum class PlaybackStatus {
    /** Showing static thumbnail; player not yet instantiated */
    IDLE,

    /** Asynchronously allocating MediaPlayer and preparing video stream */
    PREPARING,

    /** Codec and headers ready; waiting for user or auto-play */
    READY,

    /** Video is actively rendering frames and playing audio */
    PLAYING,

    /** Video is paused at current position */
    PAUSED,

    /** Media pipeline is buffering frames */
    BUFFERING,

    /** Video reached end of stream; replay available */
    COMPLETED,

    /** Playback failed; error details available with recovery actions */
    ERROR,
}

/**
 * Immutable playback state container for [SwipePixPlayerEngine].
 */
data class SwipePixPlaybackState(
    val status: PlaybackStatus = PlaybackStatus.IDLE,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isMuted: Boolean = false,
    val errorMessage: String? = null,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
) {
    val isPlaying: Boolean get() = status == PlaybackStatus.PLAYING
    val isPaused: Boolean get() = status == PlaybackStatus.PAUSED
    val isEnded: Boolean get() = status == PlaybackStatus.COMPLETED
    val isError: Boolean get() = status == PlaybackStatus.ERROR
    val isLoading: Boolean get() = status == PlaybackStatus.PREPARING || status == PlaybackStatus.BUFFERING
    val isReadyOrActive: Boolean get() = status in setOf(
        PlaybackStatus.READY,
        PlaybackStatus.PLAYING,
        PlaybackStatus.PAUSED,
        PlaybackStatus.BUFFERING,
        PlaybackStatus.COMPLETED
    )

    /**
     * Normalized progress value in range [0f, 1f].
     */
    val progress: Float
        get() = if (durationMs > 0) {
            (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    /**
     * Formats milliseconds into human-readable time string:
     * - `< 1 hour`: `mm:ss` (e.g. `01:23`)
     * - `>= 1 hour`: `hh:mm:ss` (e.g. `01:14:02`)
     */
    fun formatPosition(): String = formatTime(currentPositionMs, durationMs)

    fun formatDuration(): String = formatTime(durationMs, durationMs)

    companion object {
        fun formatTime(timeMs: Long, totalDurationMs: Long = 0L): String {
            if (timeMs <= 0L) {
                return if (totalDurationMs >= 3600000L) "00:00:00" else "00:00"
            }

            val hours = TimeUnit.MILLISECONDS.toHours(timeMs)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(timeMs) % 60
            val seconds = TimeUnit.MILLISECONDS.toSeconds(timeMs) % 60

            val forceHours = totalDurationMs >= 3600000L || hours > 0

            return if (forceHours) {
                String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format(Locale.US, "%02d:%02d", minutes, seconds)
            }
        }
    }
}
