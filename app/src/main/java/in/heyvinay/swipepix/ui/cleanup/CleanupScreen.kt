package `in`.heyvinay.swipepix.ui.cleanup

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import `in`.heyvinay.swipepix.data.model.MediaItem
import `in`.heyvinay.swipepix.data.model.MediaType
import `in`.heyvinay.swipepix.ui.theme.SwipeKeepGreen
import `in`.heyvinay.swipepix.ui.theme.SwipeTrashRed
import kotlinx.coroutines.delay
import java.text.NumberFormat
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanupScreen(
    state: CleanupUiState.Active,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onUndo: () -> Unit,
    onFinishEarly: () -> Unit,
    onSaveAndExit: () -> Unit,
    onDiscardSession: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showFinishDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var undoToastVisible by remember { mutableStateOf(false) }
    var lastRestoredUri by remember { mutableStateOf<Uri?>(null) }
    val numberFormat = remember { NumberFormat.getIntegerInstance() }

    // Intercept system back button to prevent accidental data loss
    BackHandler {
        showExitDialog = true
    }

    // Swipe Card Gesture State
    val swipeState = rememberSwipeCardState { direction ->
        when (direction) {
            SwipeDirection.LEFT -> onSwipeLeft()
            SwipeDirection.RIGHT -> onSwipeRight()
        }
    }

    // Trigger toast when a photo is restored via undo
    LaunchedEffect(state.lastRestoredPhoto) {
        if (state.lastRestoredPhoto != null) {
            lastRestoredUri = state.lastRestoredPhoto.contentUri
            undoToastVisible = true
            swipeState.animateUndoReturn(SwipeDirection.LEFT)
            delay(2400)
            undoToastVisible = false
        }
    }

    val remainingCount = (state.totalCount - state.reviewedCount).coerceAtLeast(0)
    val progress = if (state.totalCount > 0) {
        state.reviewedCount.toFloat() / state.totalCount.toFloat()
    } else 0f

    val animatedProgress by animateFloatAsState(targetValue = progress, label = "cleanupProgress")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
                TopAppBar(
                    title = {
                        Text(
                            text = "${numberFormat.format(state.reviewedCount)} / ${numberFormat.format(state.totalCount)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { showExitDialog = true }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Exit cleaning",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    },
                    actions = {
                        TextButton(onClick = { showFinishDialog = true }) {
                            Text(
                                text = "Finish",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )

                // Subtle Progress Indicator
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .padding(horizontal = 24.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    strokeCap = StrokeCap.Round
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${numberFormat.format(remainingCount)} remaining · ${numberFormat.format(state.reviewedCount)} reviewed",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Level 1: Stable Media Stage (flex weight guarantees rock-solid UI boundaries)
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val stageWidth = maxWidth
                    val stageHeight = maxHeight

                    // Level 2: Adaptive Visual Media Surface for Card 2 (Middle - pre-rendered)
                    state.nextPhoto?.let { next ->
                        key(next.contentUri.toString()) {
                            AdaptiveMediaSurface(
                                mediaItem = next,
                                maxStageWidth = stageWidth,
                                maxStageHeight = stageHeight,
                                modifier = Modifier.scale(0.96f),
                                isInteractive = false,
                            )
                        }
                    }

                    // Level 2: Adaptive Visual Media Surface for Card 1 (Top / Interactive)
                    state.currentPhoto?.let { current ->
                        key(current.contentUri.toString()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .swipeCardGesture(swipeState),
                                contentAlignment = Alignment.Center
                            ) {
                                AdaptiveMediaSurface(
                                    mediaItem = current,
                                    maxStageWidth = stageWidth,
                                    maxStageHeight = stageHeight,
                                ) {
                                    // Real-time progressive feedback overlay clipped to the adaptive card shape
                                    SwipeFeedbackOverlay(
                                        dragProgress = swipeState.dragProgress,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Controls Bottom Area
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val isDark = `in`.heyvinay.swipepix.ui.theme.LocalDarkTheme.current

                    // Central Undo Pill (governed by UserPreferences)
                    if (state.showUndo) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (state.canUndo) {
                                        if (isDark) Color(0x331C1C26) else MaterialTheme.colorScheme.surfaceVariant
                                    } else {
                                        if (isDark) Color(0x111C1C26) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    }
                                )
                                .border(
                                    1.dp,
                                    if (state.canUndo) {
                                        if (isDark) Color(0x44FFFFFF) else MaterialTheme.colorScheme.outline
                                    } else {
                                        if (isDark) Color(0x11FFFFFF) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    },
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            IconButton(
                                onClick = onUndo,
                                enabled = state.canUndo,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Undo,
                                    contentDescription = "Undo last action",
                                    tint = if (state.canUndo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Circular Trash & Keep Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 48.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Trash Button
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = { swipeState.swipe(SwipeDirection.LEFT) },
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x22EF4444))
                                    .border(1.5.dp, Color(0x55EF4444), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Move to trash",
                                    tint = SwipeTrashRed,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Trash",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Keep Button
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = { swipeState.swipe(SwipeDirection.RIGHT) },
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x2210B981))
                                    .border(1.5.dp, Color(0x5510B981), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Keep photo",
                                    tint = SwipeKeepGreen,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Keep",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Floating Undo Toast Capsule near top
            UndoToastCapsule(
                visible = undoToastVisible,
                thumbnailUri = lastRestoredUri,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            )

            // Mid-Session Finish Dialog
            if (showFinishDialog) {
                MidSessionFinishDialog(
                    reviewedCount = state.reviewedCount,
                    keptCount = state.keptCount,
                    trashedCount = state.trashedCount,
                    remainingCount = remainingCount,
                    onApplyAndFinish = {
                        showFinishDialog = false
                        onFinishEarly()
                    },
                    onSaveAndExit = {
                        showFinishDialog = false
                        onSaveAndExit()
                    },
                    onDismiss = { showFinishDialog = false }
                )
            }

            // Safe Exit Dialog (Back gesture / Back icon)
            if (showExitDialog) {
                ExitCleanupDialog(
                    reviewedCount = state.reviewedCount,
                    keptCount = state.keptCount,
                    trashedCount = state.trashedCount,
                    onSaveAndExit = {
                        showExitDialog = false
                        onSaveAndExit()
                    },
                    onDiscardSession = {
                        showExitDialog = false
                        onDiscardSession()
                    },
                    onDismiss = { showExitDialog = false }
                )
            }

        }
    }
}

