package `in`.heyvinay.swipepix.ui.cleanup

import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import `in`.heyvinay.swipepix.ui.util.DateUtils
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import `in`.heyvinay.swipepix.ui.components.EmptyScreen
import `in`.heyvinay.swipepix.ui.components.ErrorScreen
import `in`.heyvinay.swipepix.ui.components.LoadingScreen
import `in`.heyvinay.swipepix.ui.theme.LocalDarkTheme
import `in`.heyvinay.swipepix.ui.theme.SwipeKeepGreenDark
import `in`.heyvinay.swipepix.ui.theme.SwipeKeepGreenLight
import java.text.NumberFormat

/**
 * Modern, elevated "Select Source" screen.
 *
 * Allows users to choose between cleaning:
 * 1. "All Photos" (primary hero option with live library-wide counts and thumbnail)
 * 2. A specific album from a responsive 2-column grid.
 *
 * Features:
 * - Full Dark Theme and Light Theme support using Material 3 semantic tokens and LocalDarkTheme.
 * - Single root LazyVerticalGrid container for smooth 120Hz scrolling.
 * - Thumbnail optimization with Coil (256px bounds).
 * - Informative Scope Help Dialog.
 * - Progressive disclosure ("Show all albums") when album count exceeds 8.
 * - Persistent progress badges ("✓ X reviewed") for previously visited sources.
 * - On-device privacy guarantee footer.
 */
data class PendingResumeSource(
    val albumId: String?,
    val albumName: String,
    val reviewedCount: Int,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanupSourceSelectionScreen(
    viewModel: CleanupSourceSelectionViewModel,
    onNavigateBack: () -> Unit,
    onSourceSelected: (albumId: String?, albumName: String, isNewSession: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showHelpDialog by rememberSaveable { mutableStateOf(false) }
    var pendingResumeSource by remember { mutableStateOf<PendingResumeSource?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Select Source",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = "About cleaning sources",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize(),
    ) { paddingValues ->
        when (val state = uiState) {
            is SourceSelectionUiState.Loading -> {
                LoadingScreen(modifier = Modifier.padding(paddingValues))
            }

            is SourceSelectionUiState.Error -> {
                ErrorScreen(
                    message = state.message,
                    onRetry = { viewModel.loadSources() },
                    modifier = Modifier.padding(paddingValues),
                )
            }

            is SourceSelectionUiState.Empty -> {
                EmptyScreen(
                    message = "No photos or albums found to clean",
                    modifier = Modifier.padding(paddingValues),
                )
            }

            is SourceSelectionUiState.Content -> {
                var isExpanded by rememberSaveable { mutableStateOf(false) }
                val displayedAlbums = remember(state.albums, isExpanded) {
                    if (state.albums.size > 8 && !isExpanded) {
                        state.albums.take(8)
                    } else {
                        state.albums
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 24.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = paddingValues.calculateTopPadding())
                        .navigationBarsPadding(),
                ) {
                    // Subtitle header
                    item(span = { GridItemSpan(2) }) {
                        Text(
                            text = "Choose a gallery or album to start cleaning",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }

                    // Primary Hero Card: All Photos
                    item(span = { GridItemSpan(2) }) {
                        AllPhotosHeroCard(
                            source = state.allPhotos,
                            onClick = {
                                if (state.allPhotos.hasResumableSession) {
                                    pendingResumeSource = PendingResumeSource(null, "All Photos", state.allPhotos.reviewedCount)
                                } else {
                                    onSourceSelected(null, "All Photos", false)
                                }
                            },
                        )
                    }

                    // Albums Section Header
                    if (state.albums.isNotEmpty()) {
                        item(span = { GridItemSpan(2) }) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp, bottom = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Albums",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground,
                                )

                                val numberFormat = NumberFormat.getIntegerInstance()
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                ) {
                                    Text(
                                        text = "${numberFormat.format(state.albums.size)} albums",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }

                        // 2-Column Album Items
                        val seenSourceKeys = mutableSetOf<String>()
                        val uniqueAlbums = displayedAlbums.filter { seenSourceKeys.add(it.album.id) }
                        items(
                            items = uniqueAlbums,
                            key = { it.album.id },
                        ) { item ->
                            AlbumSourceCard(
                                item = item,
                                onClick = {
                                    if (item.hasResumableSession) {
                                        pendingResumeSource = PendingResumeSource(item.album.id, item.album.displayName, item.reviewedCount)
                                    } else {
                                        onSourceSelected(item.album.id, item.album.displayName, false)
                                    }
                                },
                            )
                        }

                        // Progressive disclosure button if > 8 albums
                        if (state.albums.size > 8) {
                            item(span = { GridItemSpan(2) }) {
                                val remainingCount = state.albums.size - 8
                                val isDark = LocalDarkTheme.current
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateContentSize()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (isDark) Color(0x14FFFFFF)
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isDark) Color(0x1FFFFFFF)
                                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(14.dp),
                                        )
                                        .clickable { isExpanded = !isExpanded }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                    ) {
                                        Text(
                                            text = if (isExpanded) {
                                                "Show fewer albums"
                                            } else {
                                                "Show all albums ($remainingCount more)"
                                            },
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.SemiBold,
                                            ),
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = if (isExpanded) {
                                                Icons.Default.KeyboardArrowUp
                                            } else {
                                                Icons.Default.KeyboardArrowDown
                                            },
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Privacy Guarantee Footer
                    item(span = { GridItemSpan(2) }) {
                        PrivacyGuaranteeFooter()
                    }
                }
            }
        }
    }

    if (showHelpDialog) {
        SourceSelectionHelpDialog(onDismiss = { showHelpDialog = false })
    }

    pendingResumeSource?.let { source ->
        ResumeOrNewSessionDialog(
            source = source,
            onResume = {
                val s = source
                pendingResumeSource = null
                onSourceSelected(s.albumId, s.albumName, false)
            },
            onStartNew = {
                val s = source
                pendingResumeSource = null
                onSourceSelected(s.albumId, s.albumName, true)
            },
            onDismiss = { pendingResumeSource = null },
        )
    }
}

/**
 * Primary Hero Card representing "All Photos".
 */
@Composable
private fun AllPhotosHeroCard(
    source: AllPhotosSource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isDark = LocalDarkTheme.current
    val numberFormat = remember { NumberFormat.getIntegerInstance() }
    val cardShape = RoundedCornerShape(20.dp)

    val backgroundBrush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF181E2E),
                Color(0xFF121622),
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFFFFF),
                Color(0xFFF8FAFC),
            )
        )
    }

    val borderBrush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0x4D38BDF8),
                Color(0x1A38BDF8),
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0x331E60FF),
                Color(0x141E60FF),
            )
        )
    }

    val accessibilityDesc = buildString {
        append("All Photos. Clean up your entire library. ")
        append("${numberFormat.format(source.photoCount)} photos, ")
        if (source.videoCount > 0) append("${numberFormat.format(source.videoCount)} videos. ")
        if (source.reviewedCount > 0) append("${numberFormat.format(source.reviewedCount)} reviewed.")
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(backgroundBrush)
            .border(width = 1.5.dp, brush = borderBrush, shape = cardShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = accessibilityDesc }
            .padding(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Thumbnail / Icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                if (source.coverUri != null) {
                    val cacheKey = source.coverUri.toString() + "_all_photos_cover"
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(source.coverUri)
                            .memoryCacheKey(cacheKey)
                            .diskCacheKey(cacheKey)
                            .crossfade(false)
                            .size(256)
                            .precision(coil3.size.Precision.EXACT)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Details Column
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "All Photos",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // "ENTIRE LIBRARY" subtle badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "ALL MEDIA",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                letterSpacing = 0.5.sp,
                            ),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                Text(
                    text = "Clean up your entire library",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )

                // Photos & Videos Count
                val countsText = DateUtils.formatMediaSubtitle(source.photoCount, source.videoCount)
                Text(
                    text = countsText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp),
                )

                // Reviewed Progress Pill if available
                if (source.reviewedCount > 0) {
                    val progressColor = if (isDark) SwipeKeepGreenDark else SwipeKeepGreenLight
                    val badgeText = if (source.hasResumableSession) {
                        "▶ In Progress (${numberFormat.format(source.reviewedCount)} reviewed)"
                    } else {
                        "✓ ${numberFormat.format(source.reviewedCount)} reviewed"
                    }
                    Box(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(progressColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp,
                            ),
                            color = progressColor,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Navigation chevron
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Album card displayed in the 2-column grid.
 */
@Composable
private fun AlbumSourceCard(
    item: AlbumSourceItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val album = item.album
    val context = LocalContext.current
    val isDark = LocalDarkTheme.current
    val numberFormat = remember { NumberFormat.getIntegerInstance() }
    val cardShape = RoundedCornerShape(16.dp)

    val accessibilityDesc = buildString {
        append("${album.displayName}, ${numberFormat.format(album.photoCount)} photos. ")
        if (album.videoCount > 0) append("${numberFormat.format(album.videoCount)} videos. ")
        if (item.reviewedCount > 0) append("${numberFormat.format(item.reviewedCount)} reviewed.")
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.15f)
            .clip(cardShape)
            .border(
                width = 1.dp,
                color = if (isDark) Color(0x26FFFFFF) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = cardShape,
            )
            .clickable(onClick = onClick)
            .semantics { contentDescription = accessibilityDesc },
    ) {
        // Full bleed cover image
        if (album.coverUri != null) {
            val cacheKey = album.coverUri.toString() + "_source_cover"
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(album.coverUri)
                    .memoryCacheKey(cacheKey)
                    .diskCacheKey(cacheKey)
                    .crossfade(false)
                    .size(256)
                    .precision(coil3.size.Precision.EXACT)
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
                    imageVector = Icons.Outlined.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
        }

        // Bottom gradient scrim ensuring high contrast readability
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xCC0B0E14),
                            Color(0xF20B0E14),
                        ),
                    ),
                )
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = album.displayName,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp),
                    )
                }

                // Photo and Video Counts
                val mediaDetails = DateUtils.formatMediaSubtitle(album.photoCount, album.videoCount)
                Text(
                    text = mediaDetails,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                // Reviewed Progress Pill if available
                if (item.reviewedCount > 0) {
                    val badgeText = if (item.hasResumableSession) {
                        "▶ In Progress (${numberFormat.format(item.reviewedCount)})"
                    } else {
                        "✓ ${numberFormat.format(item.reviewedCount)} reviewed"
                    }
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x3300E676))
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 9.sp,
                            ),
                            color = Color(0xFF00E676),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact privacy guarantee footer.
 */
@Composable
private fun PrivacyGuaranteeFooter(
    modifier: Modifier = Modifier,
) {
    val isDark = LocalDarkTheme.current

    Spacer(modifier = Modifier.height(6.dp))
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isDark) Color(0x1A1E2433)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            )
            .border(
                width = 1.dp,
                color = if (isDark) Color(0x1FFFFFFF)
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                shape = RoundedCornerShape(16.dp),
            )
            .padding(14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "100% On-Device & Private",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Your photos stay on your device. No cloud uploads, no accounts, no tracking.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}

/**
 * Explanatory dialog explaining cleaning scopes (All Photos vs Specific Album).
 */
@Composable
private fun SourceSelectionHelpDialog(
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Cleaning Scope",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "SwipePix lets you clean photos with two scope options:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                ScopeBulletPoint(
                    title = "All Photos",
                    description = "Reviews media across your entire device library in chronological order.",
                )

                ScopeBulletPoint(
                    title = "Specific Album",
                    description = "Focuses only on media within the selected folder (e.g. Camera or Screenshots).",
                )

                ScopeBulletPoint(
                    title = "Independent Progress",
                    description = "Progress is saved separately for each album and for All Photos. You can pause and resume anytime.",
                )

                ScopeBulletPoint(
                    title = "Safe Confirmation",
                    description = "Nothing is permanently removed during swiping. Trash decisions are only applied when you confirm in the summary.",
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Got it",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                )
            }
        },
    )
}

@Composable
private fun ScopeBulletPoint(
    title: String,
    description: String,
) {
    Column {
        Text(
            text = "• $title",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 10.dp, top = 2.dp),
        )
    }
}

@Composable
private fun ResumeOrNewSessionDialog(
    source: PendingResumeSource,
    onResume: () -> Unit,
    onStartNew: () -> Unit,
    onDismiss: () -> Unit,
) {
    val numberFormat = remember { NumberFormat.getIntegerInstance() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Unfinished Session",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "You have an unfinished cleaning session for \"${source.albumName}\" with ${numberFormat.format(source.reviewedCount)} photos reviewed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Would you like to resume where you left off, or start fresh from the beginning?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onResume,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text(
                    text = "Resume Cleaning",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = "Cancel",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onStartNew) {
                    Text(
                        text = "Start Fresh",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        },
    )
}
