package `in`.heyvinay.swipepix.ui.video

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.view.Surface
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Canonical Video Player Engine for SwipePix.
 *
 * Encapsulates the Android platform MediaPlayer, audio focus, surface binding,
 * position ticker, seeking, and lifecycle management.
 *
 * Guarantees:
 * 1. Zero third-party player dependencies (Google Play Protect clean).
 * 2. Hardware acceleration via OpenGL ES TextureView surface.
 * 3. Atomic state updates avoiding root recomposition thrashing.
 * 4. Safe resource release on disposal, card swipe, or lifecycle pause.
 */
class SwipePixPlayerEngine(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private var mediaPlayer: MediaPlayer? = null
    private var boundSurface: Surface? = null
    private var tickerJob: Job? = null
    private var isScrubbing: Boolean = false
    private var currentUri: Uri? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    private val _playbackState = mutableStateOf(SwipePixPlaybackState())
    val playbackState: State<SwipePixPlaybackState> = _playbackState

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Lower volume or pause
                if (_playbackState.value.isPlaying) {
                    try {
                        mediaPlayer?.setVolume(0.2f, 0.2f)
                    } catch (_: Exception) {}
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (!_playbackState.value.isMuted) {
                    try {
                        mediaPlayer?.setVolume(1.0f, 1.0f)
                    } catch (_: Exception) {}
                }
            }
        }
    }

    /**
     * Initializes and prepares MediaPlayer asynchronously for [uri].
     */
    fun prepare(uri: Uri, autoPlay: Boolean = false) {
        if (currentUri == uri && _playbackState.value.isReadyOrActive) {
            if (autoPlay && !_playbackState.value.isPlaying) {
                play()
            }
            return
        }

        releaseInternal(keepState = false)
        currentUri = uri
        _playbackState.value = SwipePixPlaybackState(status = PlaybackStatus.PREPARING)

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )

                setOnPreparedListener { mp ->
                    val dur = try { mp.duration.toLong().coerceAtLeast(0L) } catch (_: Exception) { 0L }
                    val width = try { mp.videoWidth } catch (_: Exception) { 0 }
                    val height = try { mp.videoHeight } catch (_: Exception) { 0 }

                    _playbackState.value = _playbackState.value.copy(
                        status = PlaybackStatus.READY,
                        durationMs = dur,
                        videoWidth = width,
                        videoHeight = height,
                    )

                    // Re-attach surface if surface was bound prior to onPrepared
                    boundSurface?.let { surface ->
                        try {
                            mp.setSurface(surface)
                        } catch (_: Exception) {}
                    }

                    if (autoPlay) {
                        play()
                    }
                }

                setOnErrorListener { _, what, extra ->
                    val errorMsg = "Video playback error ($what, $extra)"
                    _playbackState.value = _playbackState.value.copy(
                        status = PlaybackStatus.ERROR,
                        errorMessage = errorMsg
                    )
                    stopTicker()
                    abandonAudioFocus()
                    true // Error handled
                }

                setOnCompletionListener {
                    val duration = _playbackState.value.durationMs
                    _playbackState.value = _playbackState.value.copy(
                        status = PlaybackStatus.COMPLETED,
                        currentPositionMs = duration,
                    )
                    stopTicker()
                    abandonAudioFocus()
                }

                setOnBufferingUpdateListener { _, percent ->
                    if (_playbackState.value.status == PlaybackStatus.PLAYING && percent in 1..99) {
                        // Informational buffering
                    }
                }

                setOnVideoSizeChangedListener { _, width, height ->
                    if (width > 0 && height > 0) {
                        _playbackState.value = _playbackState.value.copy(
                            videoWidth = width,
                            videoHeight = height
                        )
                    }
                }
            }

            player.setDataSource(context, uri)
            player.prepareAsync()
            mediaPlayer = player

        } catch (e: Exception) {
            _playbackState.value = SwipePixPlaybackState(
                status = PlaybackStatus.ERROR,
                errorMessage = e.message ?: "Failed to open video"
            )
            mediaPlayer = null
        }
    }

    /**
     * Starts or resumes playback.
     */
    fun play() {
        val player = mediaPlayer ?: return
        val currentStatus = _playbackState.value.status

        if (currentStatus == PlaybackStatus.COMPLETED) {
            // Restart from beginning
            seekTo(0L)
        }

        try {
            requestAudioFocus()
            applyMuteVolume()
            player.start()
            _playbackState.value = _playbackState.value.copy(status = PlaybackStatus.PLAYING)
            startTicker()
        } catch (e: Exception) {
            _playbackState.value = _playbackState.value.copy(
                status = PlaybackStatus.ERROR,
                errorMessage = e.message ?: "Failed to start playback"
            )
        }
    }

    /**
     * Pauses playback.
     */
    fun pause() {
        val player = mediaPlayer ?: return
        try {
            if (player.isPlaying) {
                player.pause()
            }
            _playbackState.value = _playbackState.value.copy(status = PlaybackStatus.PAUSED)
            stopTicker()
            abandonAudioFocus()
        } catch (e: Exception) {
            _playbackState.value = _playbackState.value.copy(
                status = PlaybackStatus.ERROR,
                errorMessage = e.message ?: "Failed to pause"
            )
        }
    }

    /**
     * Toggles between play and pause.
     */
    fun togglePlayPause() {
        when (_playbackState.value.status) {
            PlaybackStatus.PLAYING -> pause()
            PlaybackStatus.PAUSED,
            PlaybackStatus.READY,
            PlaybackStatus.COMPLETED -> play()
            PlaybackStatus.IDLE,
            PlaybackStatus.ERROR -> {
                currentUri?.let { prepare(it, autoPlay = true) }
            }
            PlaybackStatus.PREPARING,
            PlaybackStatus.BUFFERING -> {
                // In-flight
            }
        }
    }

    /**
     * User is dragging the seek bar thumb.
     * Freezes ticker updates to avoid UI jitter.
     */
    fun onScrub(positionMs: Long) {
        isScrubbing = true
        val clampedPos = positionMs.coerceIn(0L, _playbackState.value.durationMs.coerceAtLeast(0L))
        _playbackState.value = _playbackState.value.copy(currentPositionMs = clampedPos)
    }

    /**
     * User committed seek bar thumb release.
     * Seeks the MediaPlayer and resumes ticker if playing.
     */
    fun onScrubFinished(positionMs: Long) {
        isScrubbing = false
        seekTo(positionMs)
    }

    /**
     * Seeks to a specific timestamp in milliseconds.
     */
    fun seekTo(positionMs: Long) {
        val player = mediaPlayer
        val duration = _playbackState.value.durationMs
        val targetMs = positionMs.coerceIn(0L, duration.coerceAtLeast(0L))

        _playbackState.value = _playbackState.value.copy(currentPositionMs = targetMs)

        if (player != null && _playbackState.value.isReadyOrActive) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    player.seekTo(targetMs, MediaPlayer.SEEK_CLOSEST)
                } else {
                    player.seekTo(targetMs.toInt())
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Sets audio mute state.
     */
    fun setMuted(muted: Boolean) {
        _playbackState.value = _playbackState.value.copy(isMuted = muted)
        applyMuteVolume()
    }

    /**
     * Toggles audio mute state.
     */
    fun toggleMute() {
        setMuted(!_playbackState.value.isMuted)
    }

    private fun applyMuteVolume() {
        val player = mediaPlayer ?: return
        val volume = if (_playbackState.value.isMuted) 0f else 1f
        try {
            player.setVolume(volume, volume)
        } catch (_: Exception) {}
    }

    /**
     * Connects an active Surface (from TextureView) to the MediaPlayer.
     */
    fun attachSurface(surface: Surface) {
        boundSurface = surface
        try {
            mediaPlayer?.setSurface(surface)
        } catch (_: Exception) {}
    }

    /**
     * Detaches Surface when TextureView is destroyed or removed.
     */
    fun detachSurface() {
        boundSurface = null
        try {
            mediaPlayer?.setSurface(null)
        } catch (_: Exception) {}
    }

    private fun startTicker() {
        stopTicker()
        tickerJob = coroutineScope.launch(Dispatchers.Main.immediate) {
            while (isActive && _playbackState.value.isPlaying) {
                if (!isScrubbing) {
                    val player = mediaPlayer
                    if (player != null) {
                        try {
                            val currentPos = player.currentPosition.toLong().coerceAtLeast(0L)
                            val duration = player.duration.toLong().coerceAtLeast(0L)
                            if (duration > 0 && duration != _playbackState.value.durationMs) {
                                _playbackState.value = _playbackState.value.copy(
                                    currentPositionMs = currentPos,
                                    durationMs = duration
                                )
                            } else {
                                _playbackState.value = _playbackState.value.copy(
                                    currentPositionMs = currentPos
                                )
                            }
                        } catch (_: Exception) {}
                    }
                }
                delay(100L) // 100ms precision ticker for smooth slider tracking
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun requestAudioFocus() {
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            audioFocusRequest = request
            am.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
    }

    private fun abandonAudioFocus() {
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
            audioFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            am.abandonAudioFocus(audioFocusChangeListener)
        }
    }

    private fun releaseInternal(keepState: Boolean) {
        stopTicker()
        abandonAudioFocus()

        val player = mediaPlayer
        mediaPlayer = null
        if (player != null) {
            try {
                if (player.isPlaying) {
                    player.stop()
                }
            } catch (_: Exception) {}
            try {
                player.reset()
                player.release()
            } catch (_: Exception) {}
        }

        if (!keepState) {
            _playbackState.value = SwipePixPlaybackState(status = PlaybackStatus.IDLE)
        }
    }

    /**
     * Completely releases MediaPlayer and resources.
     */
    fun release() {
        currentUri = null
        boundSurface = null
        releaseInternal(keepState = false)
    }
}
