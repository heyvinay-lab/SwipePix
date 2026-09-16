package `in`.heyvinay.swipepix.ui.albums

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import `in`.heyvinay.swipepix.data.model.Album
import `in`.heyvinay.swipepix.ui.components.BottomNavTab
import `in`.heyvinay.swipepix.ui.components.EmptyScreen
import `in`.heyvinay.swipepix.ui.components.ErrorScreen
import `in`.heyvinay.swipepix.ui.components.GalleryHeader
import `in`.heyvinay.swipepix.ui.components.LoadingScreen
import `in`.heyvinay.swipepix.ui.components.SwipePixBottomBar
import java.text.NumberFormat

import `in`.heyvinay.swipepix.ui.theme.LocalDarkTheme

/**
 * Albums screen displaying all device albums/folders in a 2-column grid.
 *
 * Implements the same navigation and visual language as Home:
 * - Theme-aware background
 * - Top header with dynamic photo and album count, plus glass Settings button
 * - 2-column grid of albums with full-bleed cover art and bottom glass scrim
 * - Floating bottom navigation bar with "Albums" active and glowing Clean Up button
 */
@Composable
fun AlbumsScreen(
    viewModel: AlbumsViewModel,
    onAlbumClick: (Album) -> Unit,
    onPhotosClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onCleanUpClick: () -> Unit,
    modifier: Modifier = Modifier,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState(),
    showBottomBar: Boolean = false,
    showHeader: Boolean = true,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AlbumsContent(
        uiState = uiState,
        onAlbumClick = onAlbumClick,
        onPhotosClick = onPhotosClick,
        onSettingsClick = onSettingsClick,
        onCleanUpClick = onCleanUpClick,
        onRetry = { viewModel.refresh() },
        modifier = modifier,
        gridState = gridState,
        showBottomBar = showBottomBar,
        showHeader = showHeader,
    )
}

@Composable
fun AlbumsContent(
    uiState: AlbumsUiState,
    onAlbumClick: (Album) -> Unit,
    onPhotosClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onCleanUpClick: () -> Unit,
    onRetry: () -> Unit,
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
            is AlbumsUiState.Loading -> {
                LoadingScreen(modifier = Modifier.fillMaxSize())
            }

            is AlbumsUiState.Empty -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (showHeader) Modifier.statusBarsPadding() else Modifier),
                ) {
                    if (showHeader) {
                        AlbumsHeader(
                            totalPhotos = 0,
                            totalAlbums = 0,
                            onSettingsClick = onSettingsClick,
                        )
                    }
                    EmptyScreen(
                        message = "No albums found",
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            is AlbumsUiState.Error -> {
                ErrorScreen(
                    message = uiState.message,
                    onRetry = onRetry,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            is AlbumsUiState.Content -> {
                val totalPhotos = remember(uiState.albums) {
                    uiState.albums.sumOf { it.mediaCount }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (showHeader) Modifier.statusBarsPadding() else Modifier),
                ) {
                    if (showHeader) {
                        AlbumsHeader(
                            totalPhotos = totalPhotos,
                            totalAlbums = uiState.albums.size,
                            onSettingsClick = onSettingsClick,
                        )
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        state = gridState,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(
                            start = 12.dp,
                            end = 12.dp,
                            top = if (showHeader) 8.dp else 4.dp,
                            bottom = 120.dp,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        val seenAlbumKeys = mutableSetOf<String>()
                        val uniqueAlbums = uiState.albums.filter { seenAlbumKeys.add(it.id) }
                        items(
                            items = uniqueAlbums,
                            key = { album -> album.id },
                        ) { album ->
                            AlbumCard(
                                album = album,
                                onClick = { onAlbumClick(album) },
                            )
                        }
                    }
                }
            }
        }

        // Floating Bottom Controls: Photos/Albums Pill (Left) & Clean Up Button (Right)
        if (showBottomBar) {
            SwipePixBottomBar(
                currentTab = BottomNavTab.ALBUMS,
                onPhotosClick = onPhotosClick,
                onAlbumsClick = {},
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
 * Top Header section for Albums:
 * Shared GalleryHeader with "Albums" title, dynamic count, and Settings button.
 */
@Composable
private fun AlbumsHeader(
    totalPhotos: Int,
    totalAlbums: Int,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val numberFormat = remember { NumberFormat.getNumberInstance() }

    GalleryHeader(
        title = "Albums",
        subtitle = "${numberFormat.format(totalPhotos)} photos • $totalAlbums albums",
        onSettingsClick = onSettingsClick,
        modifier = modifier,
    )
}

/**
 * High-fidelity album card with photo cover, glass gradient borders, and media counts.
 */
@Composable
fun AlbumCard(
    album: Album,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isDark = LocalDarkTheme.current
    val shape = RoundedCornerShape(16.dp)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = SwipePixMotion.SPRING_CARD,
        label = "albumCardScale",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.25f)
            .scale(scale)
            .clip(shape)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    if (isDark) listOf(Color(0x33FFFFFF), Color(0x0DFFFFFF))
                    else listOf(Color(0x26000000), Color(0x0A000000))
                ),
                shape = shape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        if (album.coverUri != null) {
            val cacheKey = album.coverUri.toString() + "_album_cover"
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(album.coverUri)
                    .memoryCacheKey(cacheKey)
                    .diskCacheKey(cacheKey)
                    .crossfade(false)
                    .size(384)
                    .precision(coil3.size.Precision.EXACT)
                    .build(),
                contentDescription = album.displayName,
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
                    imageVector = Icons.Outlined.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
        }

        // Bottom gradient scrim with album name, photo count, and 3-dots icon
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xCC0B0E14),
                            Color(0xF00B0E14),
                        ),
                    ),
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = album.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val countSubtitle = if (album.videoCount > 0 && album.photoCount > 0) {
                        "${NumberFormat.getNumberInstance().format(album.photoCount)} photos • ${album.videoCount} videos"
                    } else if (album.videoCount > 0 && album.photoCount == 0) {
                        "${NumberFormat.getNumberInstance().format(album.videoCount)} videos"
                    } else {
                        "${NumberFormat.getNumberInstance().format(album.mediaCount)} photos"
                    }
                    Text(
                        text = countSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f),
                    )
                }
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
