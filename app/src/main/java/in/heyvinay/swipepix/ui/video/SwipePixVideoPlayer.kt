package `in`.heyvinay.swipepix.ui.video

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import `in`.heyvinay.swipepix.data.model.MediaItem
import `in`.heyvinay.swipepix.ui.theme.SwipePixMotion
import `in`.heyvinay.swipepix.ui.util.DateUtils
import `in`.heyvinay.swipepix.ui.util.VideoIntentUtils

/**
 * The Canonical SwipePix Video Player Component.
 *
 * Implements ONE unified video architecture used across:
 * - Photos (`PhotoViewerScreen`)
 * - Albums (`ViewPhotosScreen` -> `PhotoViewerScreen`)
 * - Photo/Video Viewer (`ZoomableImage`)
 * - Swipe Cleaning Mode (`AdaptiveMediaSurface`)
 *
 * Guarantees:
 * - Single video engine ([SwipePixPlayerEngine]) with zero 3rd-party dependencies.
 * - Hardware acceleration via OpenGL ES TextureView surface ([SwipePixVideoSurface]).
 * - Precision seek slider with strict touch event consumption ([SwipePixVideoSeekSlider]).
 * - Native external player fallback ("Open with...") via [VideoIntentUtils].
 * - Full lifecycle protection (pauses on background, releases on disposal).
 * - Zero interference with Room cleanup session checkpoints and decisions.
 */
@Composable
fun SwipePixVideoPlayer(
    mediaItem: MediaItem,
    modifier: Modifier = Modifier,
    mode: VideoPlayerMode = VideoPlayerMode.VIEWER,
    isInteractive: Boolean = true,
    autoPlay: Boolean = false,
    shape: Shape = RoundedCornerShape(16.dp),
    externalControlsVisible: Boolean? = null,
    onOpenWithClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Instantiate single player engine per mediaItem URI
    val engine = remember(mediaItem.contentUri) {
        SwipePixPlayerEngine(
            context = context,
            coroutineScope = coroutineScope
        )
    }

    val playbackState by engine.playbackState

    val handleOpenWith: () -> Unit = {
        if (onOpenWithClick != null) {
            onOpenWithClick()
        } else {
            VideoIntentUtils.openVideoWithExternalPlayer(
                context = context,
                mediaItem = mediaItem,
                title = "Open with"
            )
        }
    }

    // Prepare player if autoPlay requested
    LaunchedEffect(mediaItem.contentUri, autoPlay, isInteractive) {
        if (isInteractive && autoPlay) {
            engine.prepare(mediaItem.contentUri, autoPlay = true)
        }
    }

    // Lifecycle observers: pause on background
    DisposableEffect(lifecycleOwner, engine) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                engine.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Release engine on disposal
    DisposableEffect(engine) {
        onDispose {
            engine.release()
        }
    }

    val thumbnailKey = remember(mediaItem.contentUri) { "${mediaItem.contentUri}_video_thumb" }
    val isSurfaceActive = playbackState.status in setOf(
        PlaybackStatus.PLAYING,
        PlaybackStatus.PAUSED,
        PlaybackStatus.BUFFERING,
        PlaybackStatus.COMPLETED
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // 1. Static Thumbnail Layer (Always present behind surface or while idle)
        val contentScale = if (mode == VideoPlayerMode.CLEANING_DECK) ContentScale.Crop else ContentScale.Fit
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(mediaItem.contentUri)
                .placeholderMemoryCacheKey(thumbnailKey)
                .memoryCacheKey(thumbnailKey)
                .crossfade(false)
                .build(),
            contentDescription = mediaItem.displayName,
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Hardware-Accelerated Video Surface (Active during playback/pause)
        if (isInteractive && isSurfaceActive) {
            SwipePixVideoSurface(
                engine = engine,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 3. Loading State Overlay (Preparing / Buffering)
        if (isInteractive && playbackState.isLoading) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.70f))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = Color.White,
                    strokeWidth = 2.5.dp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (playbackState.status == PlaybackStatus.BUFFERING) "Buffering..." else "Preparing video...",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // 4. Interactive Video Controls Overlay
        if (isInteractive && !playbackState.isError) {
            // When IDLE: Resting state with duration pill & central Play button
            if (playbackState.status == PlaybackStatus.IDLE) {
                // Central Play Button
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                        .clickable {
                            engine.prepare(mediaItem.contentUri, autoPlay = true)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Video",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }

                // Resting Duration Pill (Bottom-Start)
                val durationText = DateUtils.formatDuration(mediaItem.durationMs)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .border(0.8.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (durationText.isNotEmpty()) durationText else "VIDEO",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Resting Open With Pill (Bottom-End)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(14.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .border(0.8.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .clickable(onClick = handleOpenWith)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Open with external player",
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Open with",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                // Active Controls: Seeker, Play/Pause, Mute, Open with
                SwipePixVideoControls(
                    playbackState = playbackState,
                    onPlayPauseToggle = { engine.togglePlayPause() },
                    onScrub = { pos -> engine.onScrub(pos) },
                    onScrubFinished = { pos -> engine.onScrubFinished(pos) },
                    onMuteToggle = { engine.toggleMute() },
                    onOpenWithClick = handleOpenWith,
                    mode = mode,
                    isInteractive = isInteractive,
                    externalControlsVisible = externalControlsVisible,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 5. Error State Overlay
        if (isInteractive && playbackState.isError) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.85f))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = Color(0xFFFF6B6B),
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Couldn't play this video",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { engine.prepare(mediaItem.contentUri, autoPlay = true) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
                    ) {
                        Text("Try Again", fontSize = 12.sp)
                    }
                    Button(
                        onClick = handleOpenWith,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Open With", fontSize = 12.sp)
                    }
                }
            }
        }

        // 6. Non-Interactive Mode (e.g. Card 2 in Cleaning Deck)
        if (!isInteractive) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Video",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            val durationText = DateUtils.formatDuration(mediaItem.durationMs)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .border(0.8.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = "Video",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (durationText.isNotEmpty()) durationText else "VIDEO",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
