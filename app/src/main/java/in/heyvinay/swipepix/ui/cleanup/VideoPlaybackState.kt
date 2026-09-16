package `in`.heyvinay.swipepix.ui.cleanup

/**
 * State machine for in-deck video playback in SwipePix Cleaning mode.
 */
enum class VideoPlaybackState {
    /** Showing static thumbnail, duration pill, and central play button */
    IDLE,

    /** Asynchronously initializing MediaPlayer and decoding first video frame */
    PREPARING,

    /** Actively playing video frames on the TextureView surface */
    PLAYING,

    /** Paused on the current video frame with play/resume button */
    PAUSED,

    /** Reached end of video; replay button available */
    COMPLETED,

    /** Playback failed; error UI with Try Again and Open With fallback */
    ERROR,
}
