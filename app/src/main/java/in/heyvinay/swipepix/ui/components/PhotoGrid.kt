package `in`.heyvinay.swipepix.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.scale
import `in`.heyvinay.swipepix.ui.theme.SwipePixMotion
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.Check
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import `in`.heyvinay.swipepix.data.model.MediaItem
import `in`.heyvinay.swipepix.data.model.MediaType
import `in`.heyvinay.swipepix.ui.gallery.DateGroup
import `in`.heyvinay.swipepix.ui.util.DateUtils

/**
 * Shared photo grid composable displaying media in a 4-column layout
 * with date section headers, photo counts, and video/favorite indicators.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoGrid(
    dateGroups: List<DateGroup>,
    isLoadingMore: Boolean,
    onPhotoClick: (MediaItem) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState = rememberLazyGridState(),
    headerContent: (@Composable () -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    isSelectionMode: Boolean = false,
    selectedIds: Set<Long> = emptySet(),
    onPhotoLongClick: ((MediaItem) -> Unit)? = null,
) {
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 10 && lastVisibleIndex >= totalItems - 10
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            onLoadMore()
        }
    }

    // Diagnostics before rendering the grid
    val allPhotosInGrid = remember(dateGroups) { dateGroups.flatMap { it.photos } }
    val duplicateUris = remember(allPhotosInGrid) {
        val seen = mutableSetOf<String>()
        val dupes = mutableSetOf<String>()
        for (p in allPhotosInGrid) {
            val uri = p.contentUri.toString()
            if (!seen.add(uri)) {
                dupes.add(uri)
            }
        }
        dupes
    }
    val duplicateIds = remember(allPhotosInGrid) {
        val seen = mutableSetOf<Long>()
        val dupes = mutableSetOf<Long>()
        for (p in allPhotosInGrid) {
            if (!seen.add(p.id)) {
                dupes.add(p.id)
            }
        }
        dupes
    }

    LaunchedEffect(dateGroups) {
        android.util.Log.d(
            "SwipePixGrid",
            "GRID_SIZE=${allPhotosInGrid.size}, DUPLICATE_IDS=${duplicateIds.size} (${duplicateIds.take(5)}), DUPLICATE_URIS=${duplicateUris.size} (${duplicateUris.take(5)})"
        )
        android.util.Log.d(
            "SwipePixGrid",
            "FIRST_20_IDS=${allPhotosInGrid.take(20).map { it.id }}"
        )
        android.util.Log.d(
            "SwipePixGrid",
            "FIRST_20_URIS=${allPhotosInGrid.take(20).map { it.contentUri.toString() }}"
        )
        allPhotosInGrid.forEachIndexed { index, p ->
            if (p.id == 1000138102L || p.contentUri.toString().contains("1000138102")) {
                android.util.Log.e(
                    "SwipePixGrid",
                    "KEY_1000138102 OCCURRENCE: index=$index, id=${p.id}, uri=${p.contentUri}, mime=${p.mimeType}, name=${p.displayName}, bucketId=${p.bucketId}, effectiveTimestamp=${p.effectiveTimestamp}"
                )
            }
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        state = gridState,
        contentPadding = contentPadding,
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Global seen keys set guaranteeing zero duplicate keys across the entire grid
        val seenGridKeys = mutableSetOf<String>()

        // Optional top header (Photos title + filter chips) spanning full grid width
        if (headerContent != null) {
            val topHeaderKey = "grid_top_header"
            if (seenGridKeys.add(topHeaderKey)) {
                item(
                    key = topHeaderKey,
                    span = { GridItemSpan(maxLineSpan) },
                ) {
                    headerContent()
                }
            }
        }

        dateGroups.forEach { group ->
            // Date header — Spans all 4 columns
            val headerKey = "header_${group.dateKey}"
            if (seenGridKeys.add(headerKey)) {
                item(
                    key = headerKey,
                    span = { GridItemSpan(maxLineSpan) },
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, top = 20.dp, bottom = 8.dp),
                    ) {
                        Text(
                            text = group.header,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = DateUtils.formatMediaSubtitle(group.photos),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                }
            }

            // Photo / Video thumbnails — guaranteed globally unique across groups
            val uniquePhotosInGroup = group.photos.filter { photo ->
                seenGridKeys.add(photo.contentUri.toString())
            }
            items(
                items = uniquePhotosInGroup,
                key = { photo -> photo.contentUri.toString() },
            ) { photo ->
                val isSelected = selectedIds.contains(photo.id)
                PhotoThumbnail(
                    photo = photo,
                    isSelected = isSelected,
                    isSelectionMode = isSelectionMode,
                    onClick = { onPhotoClick(photo) },
                    onLongClick = onPhotoLongClick?.let { callback -> { callback(photo) } },
                )
            }
        }

        // Loading-more indicator
        if (isLoadingMore) {
            val loadingKey = "loading_more"
            if (seenGridKeys.add(loadingKey)) {
                item(
                    key = loadingKey,
                    span = { GridItemSpan(maxLineSpan) },
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}

/**
 * A rounded square photo or video thumbnail with video/favorite overlays and selection support.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoThumbnail(
    photo: MediaItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val cacheKey = photo.contentUri.toString() + "_256"

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val targetScale = when {
        isPressed -> 0.95f
        isSelected -> 0.93f
        else -> 1f
    }
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = SwipePixMotion.SPRING_TACTILE,
        label = "thumbnailPressScale",
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp),
                    )
                } else Modifier
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(photo.contentUri)
                .memoryCacheKey(cacheKey)
                .diskCacheKey(cacheKey)
                .crossfade(false)
                .size(256)
                .precision(coil3.size.Precision.EXACT)
                .build(),
            contentDescription = photo.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        // Selected tint scrim
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            )
        }

        // Selection Checkmark Circle Badge (Top-End)
        if (isSelectionMode) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else Color(0x88000000)
                    )
                    .border(
                        width = 1.5.dp,
                        color = Color.White,
                        shape = CircleShape
                    )
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Video Duration Indicator (Bottom-Left)
        if (photo.mediaType == MediaType.VIDEO) {
            val durationText = formatDuration(photo.durationMs)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xCC000000))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = "Video",
                    tint = Color.White,
                    modifier = Modifier.size(11.dp),
                )
                if (durationText.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = durationText,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        // Favorite Heart Indicator (Top-Right or Top-Left if selection mode active)
        if (photo.isFavorite) {
            Box(
                modifier = Modifier
                    .align(if (isSelectionMode) Alignment.TopStart else Alignment.TopEnd)
                    .padding(4.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color(0x66000000)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Favorite",
                    tint = Color.White,
                    modifier = Modifier.size(13.dp),
                )
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0) return ""
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(java.util.Locale.US, "%d:%02d", minutes, seconds)
}
