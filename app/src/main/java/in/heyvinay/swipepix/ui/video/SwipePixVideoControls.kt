package `in`.heyvinay.swipepix.ui.video

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.heyvinay.swipepix.ui.theme.SwipePixMotion
import kotlinx.coroutines.delay

/**
 * Display mode configuration for [SwipePixVideoControls].
 */
enum class VideoPlayerMode {
    /** In-deck card for Swipe Cleaning; compact overlay with strict gesture isolation */
    CLEANING_DECK,

    /** Full photo & video viewer; synchronizes with viewer chrome */
    VIEWER,

    /** Generic embedded video player */
    EMBEDDED,
}

/**
 * Reusable video playback controls overlay for SwipePix.
 *
 * Provides:
 * 1. Center Action: 64dp Frosted Glass Play / Pause / Replay button.
 * 2. Bottom Scrubber Bar: [SwipePixVideoSeekSlider] with timestamp labels, mute toggle, and "Open with" fallback.
 * 3. Smart Auto-Fade: Automatically fades out controls during active playback after 3.5s of inactivity.
 * 4. Tap-to-Reveal: Tapping the surface reveals or hides controls.
 */
@Composable
fun SwipePixVideoControls(
    playbackState: SwipePixPlaybackState,
    onPlayPauseToggle: () -> Unit,
    onScrub: (positionMs: Long) -> Unit,
    onScrubFinished: (positionMs: Long) -> Unit,
    onMuteToggle: () -> Unit,
    onOpenWithClick: () -> Unit,
    modifier: Modifier = Modifier,
    mode: VideoPlayerMode = VideoPlayerMode.VIEWER,
    isInteractive: Boolean = true,
    externalControlsVisible: Boolean? = null,
) {
    var userControlsVisible by remember { mutableStateOf(true) }

    // Synchronize with external visibility if provided (e.g. PhotoViewer chrome)
    val areControlsVisible = if (externalControlsVisible != null) {
        externalControlsVisible && userControlsVisible
    } else {
        userControlsVisible
    }

    // Auto-hide controls after 3.5s when playing
    LaunchedEffect(playbackState.isPlaying, userControlsVisible) {
        if (playbackState.isPlaying && userControlsVisible) {
            delay(3500L)
            userControlsVisible = false
        }
    }

    // When paused or completed, ensure controls are visible
    LaunchedEffect(playbackState.status) {
        if (playbackState.status == PlaybackStatus.PAUSED ||
            playbackState.status == PlaybackStatus.COMPLETED ||
            playbackState.status == PlaybackStatus.READY ||
            playbackState.status == PlaybackStatus.IDLE
        ) {
            userControlsVisible = true
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    if (isInteractive) {
                        userControlsVisible = !userControlsVisible
                    }
                }
            )
    ) {
        // 1. Center Play/Pause/Replay Action Button
        AnimatedVisibility(
            visible = areControlsVisible && isInteractive,
            enter = fadeIn(tween(SwipePixMotion.DURATION_FAST)),
            exit = fadeOut(tween(SwipePixMotion.DURATION_FAST)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            val buttonIcon = when {
                playbackState.status == PlaybackStatus.COMPLETED -> Icons.Default.Replay
                playbackState.isPlaying -> Icons.Default.Pause
                else -> Icons.Default.PlayArrow
            }

            val buttonDescription = when {
                playbackState.status == PlaybackStatus.COMPLETED -> "Replay"
                playbackState.isPlaying -> "Pause"
                else -> "Play"
            }

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            onPlayPauseToggle()
                            // Keep controls briefly visible after toggle
                            userControlsVisible = true
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = buttonIcon,
                    contentDescription = buttonDescription,
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }
        }

        // 2. Bottom Scrubber & Action Deck
        AnimatedVisibility(
            visible = areControlsVisible && isInteractive && (playbackState.isReadyOrActive || playbackState.durationMs > 0),
            enter = fadeIn(tween(SwipePixMotion.DURATION_FAST)),
            exit = fadeOut(tween(SwipePixMotion.DURATION_FAST)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black.copy(alpha = 0.72f))
                    .border(0.8.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Seek Slider with embedded timestamps
                SwipePixVideoSeekSlider(
                    currentPositionMs = playbackState.currentPositionMs,
                    durationMs = playbackState.durationMs,
                    onScrub = onScrub,
                    onScrubFinished = onScrubFinished,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = Color.White.copy(alpha = 0.25f),
                    thumbColor = Color.White,
                    showTimeLabels = true,
                )

                // Secondary Action Row: Mute Toggle + Open with Fallback
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Mute/Unmute
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onMuteToggle)
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (playbackState.isMuted) {
                                Icons.AutoMirrored.Filled.VolumeOff
                            } else {
                                Icons.AutoMirrored.Filled.VolumeUp
                            },
                            contentDescription = if (playbackState.isMuted) "Unmute" else "Mute",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (playbackState.isMuted) "Unmute" else "Mute",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Right: Open With Button Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .border(0.6.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .clickable(onClick = onOpenWithClick)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Open with external player",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Open with",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
