package `in`.heyvinay.swipepix.ui.gallery

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.heyvinay.swipepix.data.model.MediaItem
import `in`.heyvinay.swipepix.data.model.MediaType
import `in`.heyvinay.swipepix.ui.components.BottomNavTab
import `in`.heyvinay.swipepix.ui.components.EmptyScreen
import `in`.heyvinay.swipepix.ui.components.ErrorScreen
import `in`.heyvinay.swipepix.ui.components.GalleryHeader
import `in`.heyvinay.swipepix.ui.components.LoadingScreen
import `in`.heyvinay.swipepix.ui.components.PhotoGrid
import `in`.heyvinay.swipepix.ui.components.SwipePixBottomBar
import `in`.heyvinay.swipepix.ui.theme.LocalDarkTheme
import `in`.heyvinay.swipepix.ui.theme.SwipePixTheme
import `in`.heyvinay.swipepix.ui.util.DateUtils
import java.text.NumberFormat

/**
 * Main Photos / Home Screen for SwipePix.
 *
 * Implements the Material 3 + Liquid Glass design with:
 * - Solid near-black background
 * - Top header with dynamic photo count and subtle glass Settings button
 * - Horizontal liquid-glass filter chips (All, Favorites, Videos, Screenshots)
 * - 4-column date-grouped photo grid with video & favorite badges
 * - Floating liquid-glass bottom pill (Photos / Albums)
 * - Circular glowing Clean Up action button (icon-only)
 */
@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel,
    onPhotoClick: (MediaItem) -> Unit,
    onAlbumsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onCleanUpClick: () -> Unit,
    modifier: Modifier = Modifier,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState(),
    showBottomBar: Boolean = false,
    showHeader: Boolean = true,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val isSelectionMode = (uiState as? GalleryUiState.Content)?.isSelectionMode ?: false

    GalleryContent(
        uiState = uiState,
        onPhotoClick = { photo ->
            if (isSelectionMode) {
                viewModel.toggleSelection(photo)
            } else {
                onPhotoClick(photo)
            }
        },
        onPhotoLongClick = { photo ->
            viewModel.enterSelection(photo)
        },
        onFilterSelected = { viewModel.setFilter(it) },
        onAlbumsClick = onAlbumsClick,
        onSettingsClick = onSettingsClick,
        onCleanUpClick = onCleanUpClick,
        onRetry = { viewModel.refresh() },
        onLoadMore = { viewModel.loadMore() },
        modifier = modifier,
        gridState = gridState,
        showBottomBar = showBottomBar,
        showHeader = showHeader,
    )
}

@Composable
fun GalleryContent(
    uiState: GalleryUiState,
    onPhotoClick: (MediaItem) -> Unit,
    onFilterSelected: (GalleryFilterCategory) -> Unit,
    onAlbumsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onCleanUpClick: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onPhotoLongClick: ((MediaItem) -> Unit)? = null,
    modifier: Modifier = Modifier,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState(),
    showBottomBar: Boolean = false,
    showHeader: Boolean = true,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        when (uiState) {
            is GalleryUiState.Loading -> {
                LoadingScreen(modifier = Modifier.fillMaxSize())
            }

            is GalleryUiState.Empty -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (showHeader) Modifier.statusBarsPadding() else Modifier),
                ) {
                    if (showHeader) {
                        HomeHeader(
                            totalCount = 0,
                            photoCount = 0,
                            videoCount = 0,
                            selectedFilter = uiState.selectedFilter,
                            onFilterSelected = onFilterSelected,
                            onSettingsClick = onSettingsClick,
                        )
                    }
                    EmptyScreen(
                        message = "No photos found",
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            is GalleryUiState.Error -> {
                ErrorScreen(
                    message = uiState.message,
                    onRetry = onRetry,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            is GalleryUiState.Content -> {
                // Photo Grid with optional integrated top header and bottom padding for floating controls
                PhotoGrid(
                    dateGroups = uiState.dateGroups,
                    isLoadingMore = uiState.isLoadingMore,
                    onPhotoClick = onPhotoClick,
                    onLoadMore = onLoadMore,
                    gridState = gridState,
                    isSelectionMode = uiState.isSelectionMode,
                    selectedIds = uiState.selectedIds,
                    onPhotoLongClick = onPhotoLongClick,
                    headerContent = if (showHeader) {
                        {
                            HomeHeader(
                                totalCount = uiState.totalCount,
                                photoCount = uiState.photoCount,
                                videoCount = uiState.videoCount,
                                selectedFilter = uiState.selectedFilter,
                                onFilterSelected = onFilterSelected,
                                onSettingsClick = onSettingsClick,
                            )
                        }
                    } else null,
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = if (showHeader) 8.dp else 4.dp,
                        bottom = 120.dp,
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (showHeader) Modifier.statusBarsPadding() else Modifier),
                )
            }
        }

        // Floating Bottom Controls: Photos/Albums Pill (Left) & Clean Up Button (Right)
        if (showBottomBar) {
            SwipePixBottomBar(
                currentTab = BottomNavTab.PHOTOS,
                onPhotosClick = {},
                onAlbumsClick = onAlbumsClick,
                onCleanUpClick = onCleanUpClick,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            )
        }
    }
}

/**
 * Top Header section containing:
 * - Shared GalleryHeader with "Photos" title, dynamic count, and Settings button
 * - Horizontal liquid-glass filter chips
 */
@Composable
private fun HomeHeader(
    totalCount: Int,
    photoCount: Int,
    videoCount: Int,
    selectedFilter: GalleryFilterCategory,
    onFilterSelected: (GalleryFilterCategory) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val numberFormat = remember { NumberFormat.getNumberInstance() }

    val subtitle = when (selectedFilter) {
        GalleryFilterCategory.ALL -> DateUtils.formatMediaSubtitle(photoCount, videoCount)
        GalleryFilterCategory.VIDEOS -> "${numberFormat.format(videoCount)} ${if (videoCount == 1) "video" else "videos"}"
        GalleryFilterCategory.FAVORITES -> "${numberFormat.format(totalCount)} ${if (totalCount == 1) "favorite" else "favorites"}"
        GalleryFilterCategory.SCREENSHOTS -> "${numberFormat.format(totalCount)} ${if (totalCount == 1) "screenshot" else "screenshots"}"
    }

    GalleryHeader(
        title = "Photos",
        subtitle = subtitle,
        onSettingsClick = onSettingsClick,
        modifier = modifier,
        chipsContent = {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(GalleryFilterCategory.entries) { category ->
                    FilterChipItem(
                        category = category,
                        isSelected = category == selectedFilter,
                        onClick = { onFilterSelected(category) },
                    )
                }
            }
        },
    )
}

/**
 * A single filter chip item with active glow/gradient or subtle surface.
 */
@Composable
internal fun FilterChipItem(
    category: GalleryFilterCategory,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = LocalDarkTheme.current
    val (icon, label) = when (category) {
        GalleryFilterCategory.ALL -> Icons.Default.GridView to "All"
        GalleryFilterCategory.FAVORITES -> Icons.Default.Favorite to "Favorites"
        GalleryFilterCategory.VIDEOS -> Icons.Default.Videocam to "Videos"
        GalleryFilterCategory.SCREENSHOTS -> Icons.Default.Image to "Screenshots"
    }

    val iconTint = if (category == GalleryFilterCategory.FAVORITES && !isSelected) {
        Color(0xFFFF3366)
    } else if (isSelected) {
        Color.White
    } else {
        if (isDark) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
    }

    val textColor = if (isSelected) {
        Color.White
    } else {
        if (isDark) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
    }

    val backgroundModifier = if (isSelected) {
        Modifier
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF2962FF),
                        Color(0xFF651FFF),
                    )
                )
            )
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(Color(0x9960A5FA), Color(0x3360A5FA))
                ),
                CircleShape
            )
    } else {
        Modifier
            .background(if (isDark) Color(0x1F222C) else MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                if (isDark) Color(0x2EFFFFFF) else MaterialTheme.colorScheme.outline,
                CircleShape
            )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(CircleShape)
            .then(backgroundModifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconTint,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}


// -------------------------------------------------------------------------
// Compose Previews for Instant IDE Rendering
// -------------------------------------------------------------------------

@Preview(name = "Home Screen - Dark", showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
private fun GalleryScreenPreview() {
    SwipePixTheme {
        GalleryContent(
            uiState = GalleryUiState.Content(
                totalCount = 12842,
                selectedFilter = GalleryFilterCategory.ALL,
                dateGroups = listOf(
                    DateGroup(
                        header = "Today",
                        photos = emptyList(),
                    ),
                    DateGroup(
                        header = "Yesterday",
                        photos = emptyList(),
                    ),
                ),
            ),
            onPhotoClick = {},
            onFilterSelected = {},
            onAlbumsClick = {},
            onSettingsClick = {},
            onCleanUpClick = {},
            onRetry = {},
            onLoadMore = {},
        )
    }
}
