package `in`.heyvinay.swipepix.ui.albums

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import `in`.heyvinay.swipepix.ui.components.EmptyScreen
import `in`.heyvinay.swipepix.ui.components.ErrorScreen
import `in`.heyvinay.swipepix.ui.components.LoadingScreen
import `in`.heyvinay.swipepix.ui.theme.GlassCard
import `in`.heyvinay.swipepix.ui.theme.glassEffect
import `in`.heyvinay.swipepix.ui.util.DateUtils
import java.text.NumberFormat

/**
 * Album Detail landing screen — premium design with cover image,
 * View Photos / Start Cleaning actions, and compact album info cards.
 */
@Composable
fun AlbumDetailScreen(
    viewModel: AlbumDetailViewModel,
    onViewPhotos: (albumId: String, albumName: String) -> Unit,
    onStartCleaning: (albumId: String, albumName: String, isNewSession: Boolean) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        when (val state = uiState) {
            is AlbumDetailUiState.Loading -> {
                LoadingScreen()
            }

            is AlbumDetailUiState.Error -> {
                ErrorScreen(
                    message = state.message,
                    onRetry = { viewModel.refresh() },
                )
            }

            is AlbumDetailUiState.Content -> {
                AlbumDetailContent(
                    state = state,
                    albumId = viewModel.albumId,
                    onViewPhotos = { onViewPhotos(viewModel.albumId, state.albumName) },
                    onStartCleaning = { isNewSession -> onStartCleaning(viewModel.albumId, state.albumName, isNewSession) },
                    onNavigateBack = onNavigateBack,
                )
            }
        }
    }
}

@Composable
private fun AlbumDetailContent(
    state: AlbumDetailUiState.Content,
    albumId: String,
    onViewPhotos: () -> Unit,
    onStartCleaning: (isNewSession: Boolean) -> Unit,
    onNavigateBack: () -> Unit,
) {
    val numberFormat = remember { NumberFormat.getNumberInstance() }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.navigationBars),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // ── Top Bar ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.align(Alignment.Center),
            ) {
                Text(
                    text = state.albumName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                val subtitle = DateUtils.formatMediaSubtitle(state.photoCount, state.videoCount)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Album Cover ──
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) +
                    slideInVertically(initialOffsetY = { it / 8 }),
        ) {
            AlbumCoverImage(
                coverUri = state.coverUri,
                albumName = state.albumName,
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ── Two Action Buttons ──
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            // View Photos — secondary glass button
            ActionButton(
                label = "View Photos",
                icon = Icons.Default.PhotoLibrary,
                onClick = onViewPhotos,
                isEnabled = state.totalCount > 0,
                isPrimary = false,
                contentDesc = "View photos in ${state.albumName}",
                modifier = Modifier.weight(1f),
            )

            // Start Cleaning — primary gradient button (shows Resume Cleaning if in progress)
            val cleaningLabel = if (state.hasResumableSession) "Resume Cleaning" else "Start Cleaning"
            val cleaningIcon = if (state.hasResumableSession) Icons.Default.PlayArrow else Icons.Default.AutoFixHigh

            ActionButton(
                label = cleaningLabel,
                icon = cleaningIcon,
                onClick = { onStartCleaning(false) },
                isEnabled = state.totalCount > 0,
                isPrimary = true,
                contentDesc = "$cleaningLabel ${state.albumName}",
                modifier = Modifier.weight(1f),
            )
        }

        // ── Progress indicator if partially cleaned ──
        if (state.hasResumableSession) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "${numberFormat.format(state.reviewedCount)} / ${numberFormat.format(state.totalCount)} reviewed in active session",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(
                onClick = { showResetConfirmDialog = true },
            ) {
                Text(
                    text = "Start New Session",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        } else if (state.reviewedCount > 0) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "${numberFormat.format(state.reviewedCount)} / ${numberFormat.format(state.totalCount)} reviewed",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }

        if (showResetConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showResetConfirmDialog = false },
                title = {
                    Text(
                        text = "Start New Session?",
                        fontWeight = FontWeight.Bold,
                    )
                },
                text = {
                    Text(
                        text = "Your ${numberFormat.format(state.reviewedCount)} review decisions for this session will be cleared, and you will start cleaning from photo 1.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showResetConfirmDialog = false
                            onStartCleaning(true)
                        }
                    ) {
                        Text(
                            text = "Start Fresh",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetConfirmDialog = false }) {
                        Text("Cancel")
                    }
                },
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Info Cards ──
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 24.dp),
        ) {
            InfoCard(
                icon = Icons.Default.Image,
                title = "${numberFormat.format(state.photoCount)} Photos",
                subtitle = "Will be included",
            )
            if (state.videoCount > 0) {
                InfoCard(
                    icon = Icons.Default.Videocam,
                    title = "${numberFormat.format(state.videoCount)} Videos",
                    subtitle = "Will be included",
                )
            }
            InfoCard(
                icon = Icons.Default.Folder,
                title = "Album only",
                subtitle = "Only items from this album",
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ── Album Cover Image ──

@Composable
private fun AlbumCoverImage(
    coverUri: android.net.Uri?,
    albumName: String,
) {
    val context = LocalContext.current
    val isDark = `in`.heyvinay.swipepix.ui.theme.LocalDarkTheme.current
    val coverShape = RoundedCornerShape(20.dp)

    Box(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth()
            .aspectRatio(16f / 10f)
            .shadow(
                elevation = 16.dp,
                shape = coverShape,
                ambientColor = Color(0xFF1E60FF).copy(alpha = 0.15f),
                spotColor = Color(0xFF651FFF).copy(alpha = 0.1f),
            )
            .clip(coverShape)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    if (isDark) listOf(Color(0x4DFFFFFF), Color(0x14FFFFFF))
                    else listOf(Color(0x26000000), Color(0x0A000000)),
                ),
                shape = coverShape,
            )
            .semantics { contentDescription = "Album cover for $albumName" },
    ) {
        if (coverUri != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(coverUri)
                    .crossfade(true)
                    .size(1024)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                )
            }
        }
    }
}

// ── Action Button ──

@Composable
private fun ActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    isEnabled: Boolean,
    isPrimary: Boolean,
    contentDesc: String,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "buttonScale",
    )

    val buttonShape = RoundedCornerShape(16.dp)
    val alpha = if (isEnabled) 1f else 0.4f

    val backgroundModifier = if (isPrimary) {
        Modifier.background(
            Brush.horizontalGradient(
                listOf(Color(0xFF1E60FF), Color(0xFF651FFF)),
            ),
        )
    } else {
        Modifier.glassEffect(shape = buttonShape)
    }

    val borderModifier = if (isPrimary) {
        Modifier.border(
            width = 1.dp,
            brush = Brush.verticalGradient(
                listOf(Color(0x8093C5FD), Color(0x3393C5FD)),
            ),
            shape = buttonShape,
        )
    } else {
        Modifier
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .scale(scale)
            .clip(buttonShape)
            .then(backgroundModifier)
            .then(borderModifier)
            .clickable(
                enabled = isEnabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 16.dp, horizontal = 8.dp)
            .semantics { contentDescription = contentDesc },
    ) {
        val iconTint = if (isPrimary) Color.White else MaterialTheme.colorScheme.onSurface
        val textColor = if (isPrimary) Color.White else MaterialTheme.colorScheme.onSurface

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint.copy(alpha = alpha),
            modifier = Modifier.size(28.dp),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isPrimary) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor.copy(alpha = alpha),
            textAlign = TextAlign.Center,
        )
    }
}

// ── Info Card ──

@Composable
private fun InfoCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = 2.dp,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
        }
    }
}
