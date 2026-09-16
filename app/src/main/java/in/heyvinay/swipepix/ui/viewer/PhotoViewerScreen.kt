package `in`.heyvinay.swipepix.ui.viewer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import `in`.heyvinay.swipepix.data.model.MediaItem
import `in`.heyvinay.swipepix.data.model.MediaType
import `in`.heyvinay.swipepix.ui.components.ErrorScreen
import `in`.heyvinay.swipepix.ui.components.LoadingScreen
import `in`.heyvinay.swipepix.ui.theme.SwipePixMotion
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * Redesigned Photo Viewer Screen for SwipePix.
 *
 * Implements high-fidelity mockup:
 * - Deep dark canvas (#080A0F) with ambient atmospheric background glow
 * - Floating translucent top control bar (back, counter, share, favorite, delete)
 * - Hero media stage with adaptive aspect-ratio frame, zoom indicator pill & toggle
 * - Floating frosted metadata panel with date, specs, location, and overflow actions
 * - Quick-navigation thumbnail filmstrip synchronized two-way with horizontal pager
 * - Single-tap immersion mode toggle
 * - Technical details modal dialog
 */
@Composable
fun PhotoViewerScreen(
    viewModel: PhotoViewerViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showChrome by remember { mutableStateOf(true) }
    var showTrashConfirmation by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var itemForDetails by remember { mutableStateOf<MediaItem?>(null) }
    var pendingItemToTrash by remember { mutableStateOf<MediaItem?>(null) }
    var currentItemToTrash by remember { mutableStateOf<MediaItem?>(null) }
    var pendingFavoriteItem by remember { mutableStateOf<MediaItem?>(null) }

    val undoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onUndoOperationSuccess()
        }
    }

    val favoriteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            pendingFavoriteItem?.let { photo ->
                viewModel.onFavoriteOperationSuccess(photo.id, !photo.isFavorite)
            }
        }
        pendingFavoriteItem = null
    }

    val trashLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            currentItemToTrash?.let { item ->
                viewModel.onItemTrashed(item)
                scope.launch {
                    val snackbarResult = snackbarHostState.showSnackbar(
                        message = "1 item moved to trash",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short,
                    )
                    if (snackbarResult == SnackbarResult.ActionPerformed) {
                        val undoSender = viewModel.createUndoTrashRequest()
                        if (undoSender != null) {
                            undoLauncher.launch(IntentSenderRequest.Builder(undoSender).build())
                        }
                    }
                }
            }
        }
        currentItemToTrash = null
    }

    // Automatically navigate back if the user trashes all remaining photos
    LaunchedEffect(uiState) {
        if (uiState is ViewerUiState.Content && (uiState as ViewerUiState.Content).photos.isEmpty()) {
            onNavigateBack()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF080A0F))
    ) {
        when (val state = uiState) {
            is ViewerUiState.Loading -> {
                LoadingScreen()
            }

            is ViewerUiState.Error -> {
                ErrorScreen(
                    message = state.message,
                    onRetry = { onNavigateBack() },
                )
            }

            is ViewerUiState.Content -> {
                if (state.photos.isEmpty()) {
                    return@Box
                }

                val pagerState = rememberPagerState(
                    initialPage = state.initialIndex.coerceIn(0, state.photos.size - 1),
                    pageCount = { state.photos.size }
                )

                val filmstripState = rememberLazyListState()

                // Auto-center filmstrip as pager scrolls
                LaunchedEffect(pagerState.currentPage) {
                    val targetIndex = (pagerState.currentPage - 2).coerceAtLeast(0)
                    filmstripState.animateScrollToItem(targetIndex)
                }

                val currentPhoto = state.photos.getOrNull(pagerState.currentPage)
                    ?: state.photos.firstOrNull()

                // 1. Ambient Atmospheric Glow behind active photo
                if (currentPhoto != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(currentPhoto.contentUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = 0.20f
                                scaleX = 1.3f
                                scaleY = 1.3f
                            }
                            .blur(50.dp),
                    )
                }

                // 2. Horizontal Pager (Adaptive media stage)
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1,
                    key = { index -> state.photos.getOrNull(index)?.contentUri?.toString() ?: index.toString() }
                ) { page ->
                    val photo = state.photos.getOrNull(page) ?: return@HorizontalPager
                    ZoomableImage(
                        mediaItem = photo,
                        onTap = { showChrome = !showChrome },
                        showChrome = showChrome,
                    )
                }

                // 3. Floating Top Bar
                AnimatedVisibility(
                    visible = showChrome,
                    enter = fadeIn(animationSpec = tween(SwipePixMotion.DURATION_FAST)),
                    exit = fadeOut(animationSpec = tween(SwipePixMotion.DURATION_FAST)),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Back button
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E2430).copy(alpha = 0.75f))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                                .clickable(onClick = onNavigateBack),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Center: Position counter & subtitle
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${pagerState.currentPage + 1} / ${NumberFormat.getNumberInstance().format(state.photos.size)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Text(
                                text = if (currentPhoto?.mediaType == MediaType.VIDEO) "Video" else "Photo",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Normal,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        // Right: Actions row (Share, Favorite, Delete)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Share
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E2430).copy(alpha = 0.75f))
                                    .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                                    .clickable {
                                        currentPhoto?.let { shareMedia(context, it) }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Favorite
                            val isFav = currentPhoto?.isFavorite == true
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E2430).copy(alpha = 0.75f))
                                    .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                                    .clickable {
                                        currentPhoto?.let { photo ->
                                            pendingFavoriteItem = photo
                                            scope.launch {
                                                val intentSender = viewModel.createFavoriteRequest(photo)
                                                if (intentSender != null) {
                                                    favoriteLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                                                } else {
                                                    viewModel.onFavoriteOperationSuccess(photo.id, !photo.isFavorite)
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = if (isFav) "Favorited" else "Favorite",
                                    tint = if (isFav) Color(0xFFEF4444) else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Delete
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E2430).copy(alpha = 0.75f))
                                    .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f), CircleShape)
                                    .clickable {
                                        currentPhoto?.let { photo ->
                                            pendingItemToTrash = photo
                                            showTrashConfirmation = true
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // 4. Floating Bottom Chrome (Info Panel + Filmstrip)
                AnimatedVisibility(
                    visible = showChrome,
                    enter = fadeIn(animationSpec = tween(SwipePixMotion.DURATION_FAST)),
                    exit = fadeOut(animationSpec = tween(SwipePixMotion.DURATION_FAST)),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // A. Metadata Info Card
                        if (currentPhoto != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color(0xFF131822).copy(alpha = 0.85f))
                                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                                    .clickable {
                                        itemForDetails = currentPhoto
                                        showDetailsDialog = true
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        val dateFormatted = remember(currentPhoto.effectiveTimestamp) {
                                            if (currentPhoto.effectiveTimestamp > 0) {
                                                Instant.ofEpochMilli(currentPhoto.effectiveTimestamp)
                                                    .atZone(ZoneId.systemDefault())
                                                    .toLocalDate()
                                                    .format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.getDefault()))
                                            } else {
                                                "Unknown date"
                                            }
                                        }
                                        Text(
                                            text = dateFormatted,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )

                                        val timeFormatted = remember(currentPhoto.effectiveTimestamp) {
                                            if (currentPhoto.effectiveTimestamp > 0) {
                                                Instant.ofEpochMilli(currentPhoto.effectiveTimestamp)
                                                    .atZone(ZoneId.systemDefault())
                                                    .toLocalTime()
                                                    .format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
                                            } else ""
                                        }
                                        val resFormatted = if (currentPhoto.width > 0 && currentPhoto.height > 0) {
                                            "${currentPhoto.width} × ${currentPhoto.height}"
                                        } else ""
                                        val sizeFormatted = Formatter.formatShortFileSize(context, currentPhoto.size)
                                        val metaSpecs = listOf(timeFormatted, resFormatted, sizeFormatted)
                                            .filter { it.isNotBlank() }
                                            .joinToString(" • ")

                                        Text(
                                            text = metaSpecs,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF94A3B8)
                                        )

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.LocationOn,
                                                contentDescription = null,
                                                tint = Color(0xFF64748B),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = "No location",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    }

                                    // Overflow menu
                                    Box {
                                        var menuExpanded by remember { mutableStateOf(false) }
                                        IconButton(
                                            onClick = { menuExpanded = true },
                                            modifier = Modifier.size(44.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MoreVert,
                                                contentDescription = "More",
                                                tint = Color.White.copy(alpha = 0.75f),
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }

                                        DropdownMenu(
                                            expanded = menuExpanded,
                                            onDismissRequest = { menuExpanded = false },
                                            modifier = Modifier.background(Color(0xFF1E2430))
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("View details", color = Color.White) },
                                                onClick = {
                                                    menuExpanded = false
                                                    itemForDetails = currentPhoto
                                                    showDetailsDialog = true
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Set as wallpaper", color = Color.White) },
                                                onClick = {
                                                    menuExpanded = false
                                                    setAsWallpaper(context, currentPhoto)
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Share", color = Color.White) },
                                                onClick = {
                                                    menuExpanded = false
                                                    shareMedia(context, currentPhoto)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // B. Filmstrip Carousel
                        LazyRow(
                            state = filmstripState,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            itemsIndexed(
                                items = state.photos,
                                key = { _, item -> item.contentUri.toString() }
                            ) { index, item ->
                                val isSelected = index == pagerState.currentPage
                                val borderModifier = if (isSelected) {
                                    Modifier.border(2.dp, Color.White, RoundedCornerShape(8.dp))
                                } else {
                                    Modifier.border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                }

                                val thumbKey = item.contentUri.toString() + "_256"

                                Box(
                                    modifier = Modifier
                                        .size(width = 44.dp, height = 56.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .then(borderModifier)
                                        .clickable {
                                            scope.launch {
                                                pagerState.animateScrollToPage(index)
                                            }
                                        }
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(item.contentUri)
                                            .placeholderMemoryCacheKey(thumbKey)
                                            .memoryCacheKey(thumbKey)
                                            .diskCacheKey(thumbKey)
                                            .crossfade(false)
                                            .build(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        )

        // Deletion Confirmation Dialog
        if (showTrashConfirmation && pendingItemToTrash != null) {
            val item = pendingItemToTrash!!
            val isVideo = item.mediaType == MediaType.VIDEO
            AlertDialog(
                onDismissRequest = {
                    showTrashConfirmation = false
                    pendingItemToTrash = null
                },
                title = { Text("Move to Trash?") },
                text = {
                    Text(
                        if (isVideo)
                            "Move this video to Android Trash? You can restore it from Trash."
                        else
                            "Move this photo to Android Trash? You can restore it from Trash."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showTrashConfirmation = false
                            currentItemToTrash = item
                            pendingItemToTrash = null
                            scope.launch {
                                val intentSender = viewModel.createTrashRequest(item)
                                if (intentSender != null) {
                                    trashLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                                }
                            }
                        }
                    ) {
                        Text(
                            text = "Move to Trash",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showTrashConfirmation = false
                            pendingItemToTrash = null
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Details Modal Dialog
        if (showDetailsDialog && itemForDetails != null) {
            val detailsItem = itemForDetails!!
            AlertDialog(
                onDismissRequest = {
                    showDetailsDialog = false
                    itemForDetails = null
                },
                title = {
                    Text(
                        text = if (detailsItem.mediaType == MediaType.VIDEO) "Video Details" else "Photo Details",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DetailItem(label = "Title", value = detailsItem.displayName)
                        val formattedDate = remember(detailsItem.effectiveTimestamp) {
                            if (detailsItem.effectiveTimestamp > 0) {
                                Instant.ofEpochMilli(detailsItem.effectiveTimestamp)
                                    .atZone(ZoneId.systemDefault())
                                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.getDefault()))
                            } else "Unknown"
                        }
                        DetailItem(label = "Date", value = formattedDate)
                        if (detailsItem.width > 0 && detailsItem.height > 0) {
                            val mp = (detailsItem.width.toLong() * detailsItem.height.toLong()) / 1_000_000f
                            DetailItem(
                                label = "Resolution",
                                value = "${detailsItem.width} × ${detailsItem.height} (${String.format(Locale.US, "%.1f MP", mp)})"
                            )
                        }
                        DetailItem(label = "Size", value = Formatter.formatShortFileSize(context, detailsItem.size))
                        DetailItem(label = "MIME Type", value = detailsItem.mimeType)
                        if (detailsItem.bucketDisplayName.isNotBlank()) {
                            DetailItem(label = "Album", value = detailsItem.bucketDisplayName)
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDetailsDialog = false
                            itemForDetails = null
                        }
                    ) {
                        Text("Close", color = MaterialTheme.colorScheme.primary)
                    }
                },
                containerColor = Color(0xFF1E2430),
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF94A3B8)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )
    }
}

private fun shareMedia(context: Context, mediaItem: MediaItem) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = mediaItem.mimeType
        putExtra(Intent.EXTRA_STREAM, mediaItem.contentUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(Intent.createChooser(shareIntent, "Share"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun setAsWallpaper(context: Context, mediaItem: MediaItem) {
    val wallpaperIntent = Intent(Intent.ACTION_ATTACH_DATA).apply {
        setDataAndType(mediaItem.contentUri, mediaItem.mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        putExtra("mimeType", mediaItem.mimeType)
    }
    try {
        context.startActivity(Intent.createChooser(wallpaperIntent, "Set as wallpaper"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

