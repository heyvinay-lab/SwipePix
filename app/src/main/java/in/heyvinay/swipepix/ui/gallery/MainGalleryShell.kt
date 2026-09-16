package `in`.heyvinay.swipepix.ui.gallery

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.TextButton
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import `in`.heyvinay.swipepix.data.model.Album
import `in`.heyvinay.swipepix.data.model.MediaItem
import `in`.heyvinay.swipepix.ui.albums.AlbumsScreen
import `in`.heyvinay.swipepix.ui.albums.AlbumsUiState
import `in`.heyvinay.swipepix.ui.albums.AlbumsViewModel
import `in`.heyvinay.swipepix.ui.components.BottomNavTab
import `in`.heyvinay.swipepix.ui.components.SwipePixBottomBar
import `in`.heyvinay.swipepix.ui.theme.LocalDarkTheme
import `in`.heyvinay.swipepix.ui.util.DateUtils
import java.text.NumberFormat

/**
 * Type alias to match both GalleryTab and BottomNavTab naming.
 */
typealias GalleryTab = BottomNavTab

/**
 * MainGalleryShell is the unified, shared application shell for both Photos and Albums.
 *
 * Architecture:
 * MainGalleryShell
 * │
 * ├── Shared GalleryHeader (stationary Settings button, animated title/subtitle, filter chips only on Photos)
 * │
 * ├── Page Content (Photos / Albums in subtle slide+fade transition)
 * │
 * └── SwipePixBottomBar (2-tab pill with sliding spring indicator + glowing Clean button)
 *
 * Ensures:
 * - Single shell instance: bottom navigation changes `selectedTab` internally without destroying state.
 * - Stationary Settings button: Settings button never moves, jumps, or jitters between tab switches.
 * - Subtle transitions: 200–250ms fade + small horizontal motion.
 * - Photos-specific filter chips remain ONLY on Photos; Albums never inherits them.
 * - Back gesture on Albums switches back to Photos before exiting.
 */
@Composable
fun MainGalleryShell(
    galleryViewModel: GalleryViewModel,
    albumsViewModel: AlbumsViewModel,
    onPhotoClick: (MediaItem) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onSettingsClick: () -> Unit,
    onCleanUpClick: () -> Unit,
    modifier: Modifier = Modifier,
    initialTab: BottomNavTab = BottomNavTab.PHOTOS,
) {
    var selectedTab by rememberSaveable { mutableStateOf(initialTab) }
    val isDark = LocalDarkTheme.current
    val numberFormat = remember { NumberFormat.getNumberInstance() }

    val photosGridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    val albumsGridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()

    val galleryUiState by galleryViewModel.uiState.collectAsStateWithLifecycle()
    val albumsUiState by albumsViewModel.uiState.collectAsStateWithLifecycle()

    var lastKnownPhotoCount by rememberSaveable { mutableStateOf(0) }
    var lastKnownVideoCount by rememberSaveable { mutableStateOf(0) }
    var lastKnownTotalAlbums by rememberSaveable { mutableStateOf(0) }

    val photoCount = when (val state = galleryUiState) {
        is GalleryUiState.Content -> {
            lastKnownPhotoCount = state.photoCount
            state.photoCount
        }
        else -> lastKnownPhotoCount
    }

    val videoCount = when (val state = galleryUiState) {
        is GalleryUiState.Content -> {
            lastKnownVideoCount = state.videoCount
            state.videoCount
        }
        else -> lastKnownVideoCount
    }

    val totalAlbums = when (val state = albumsUiState) {
        is AlbumsUiState.Content -> {
            lastKnownTotalAlbums = state.albums.size
            state.albums.size
        }
        else -> lastKnownTotalAlbums
    }

    val mediaSubtitle = if (photoCount == 0 && videoCount == 0 && galleryUiState is GalleryUiState.Loading) {
        ""
    } else {
        DateUtils.formatMediaSubtitle(photoCount, videoCount)
    }

    val selectedFilter = when (val state = galleryUiState) {
        is GalleryUiState.Content -> state.selectedFilter
        else -> GalleryFilterCategory.ALL
    }

    val isSelectionMode = selectedTab == BottomNavTab.PHOTOS &&
        (galleryUiState as? GalleryUiState.Content)?.isSelectionMode == true
    val selectedIds = (galleryUiState as? GalleryUiState.Content)?.selectedIds ?: emptySet()
    val totalPhotosLoaded = (galleryUiState as? GalleryUiState.Content)?.totalLoaded ?: 0

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showTrashConfirmation by remember { mutableStateOf(false) }
    var pendingTrashCount by remember { mutableStateOf(0) }

    val undoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            galleryViewModel.onUndoOperationSuccess()
        }
    }

    val trashLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val count = pendingTrashCount
            galleryViewModel.onTrashOperationSuccess()
            scope.launch {
                val snackbarResult = snackbarHostState.showSnackbar(
                    message = if (count == 1) "1 item moved to trash" else "$count items moved to trash",
                    actionLabel = "Undo",
                    duration = SnackbarDuration.Short,
                )
                if (snackbarResult == SnackbarResult.ActionPerformed) {
                    val undoSender = galleryViewModel.createUndoTrashRequest()
                    if (undoSender != null) {
                        undoLauncher.launch(IntentSenderRequest.Builder(undoSender).build())
                    }
                }
            }
        }
    }

    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        galleryViewModel.refresh()
        albumsViewModel.refresh()
        onPauseOrDispose { }
    }

    // Intercept back when selection mode is active
    BackHandler(enabled = isSelectionMode) {
        galleryViewModel.clearSelection()
    }

    // Intercept back on Albums tab to return to Photos tab first
    BackHandler(enabled = !isSelectionMode && selectedTab == BottomNavTab.ALBUMS) {
        selectedTab = BottomNavTab.PHOTOS
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            // ============================================================
            // SHARED GALLERY HEADER / SELECTION HEADER
            // ============================================================
            AnimatedContent(
                targetState = isSelectionMode,
                transitionSpec = {
                    fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(180))
                },
                label = "headerSelectionTransition",
            ) { selectionActive ->
                if (selectionActive) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            IconButton(onClick = { galleryViewModel.clearSelection() }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close selection",
                                    tint = MaterialTheme.colorScheme.onBackground,
                                )
                            }
                            Text(
                                text = "${selectedIds.size} selected",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            val isAllSelected = selectedIds.size == totalPhotosLoaded && totalPhotosLoaded > 0
                            TextButton(onClick = { galleryViewModel.toggleSelectAll() }) {
                                Text(
                                    text = if (isAllSelected) "Deselect all" else "Select all",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                            IconButton(
                                onClick = {
                                    if (selectedIds.isNotEmpty()) {
                                        pendingTrashCount = selectedIds.size
                                        showTrashConfirmation = true
                                    }
                                },
                                enabled = selectedIds.isNotEmpty(),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Trash selected",
                                    tint = if (selectedIds.isNotEmpty()) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                    },
                                )
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp),
                    ) {
                        // Top Title & Settings Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Animated Title & Subtitle
                            AnimatedContent(
                                targetState = selectedTab,
                                transitionSpec = {
                                    if (targetState == BottomNavTab.ALBUMS) {
                                        (fadeIn(animationSpec = tween(220)) + slideInHorizontally(animationSpec = tween(220)) { it / 8 })
                                            .togetherWith(
                                                fadeOut(animationSpec = tween(180)) + slideOutHorizontally(animationSpec = tween(220)) { -it / 8 }
                                            )
                                    } else {
                                        (fadeIn(animationSpec = tween(220)) + slideInHorizontally(animationSpec = tween(220)) { -it / 8 })
                                            .togetherWith(
                                                fadeOut(animationSpec = tween(180)) + slideOutHorizontally(animationSpec = tween(220)) { it / 8 }
                                            )
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                label = "headerTextTransition",
                            ) { tab ->
                                Column {
                                    Text(
                                        text = if (tab == BottomNavTab.PHOTOS) "Photos" else "Albums",
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        letterSpacing = (-0.5).sp,
                                    )
                                    Text(
                                        text = if (tab == BottomNavTab.PHOTOS) {
                                            mediaSubtitle
                                        } else {
                                            "$mediaSubtitle • $totalAlbums albums"
                                        },
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            // Pinned stationary Settings button (100% stable across tab transitions)
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0x24FFFFFF) else Color(0xFFF1F5F9))
                                    .border(
                                        width = 1.dp,
                                        color = if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1),
                                        shape = CircleShape,
                                    )
                                    .clickable(onClick = onSettingsClick),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }

                        // Photos-only Filter Chips (Collapses smoothly when switching to Albums)
                        AnimatedVisibility(
                            visible = selectedTab == BottomNavTab.PHOTOS,
                            enter = fadeIn(animationSpec = tween(200)) + expandVertically(animationSpec = tween(220)),
                            exit = fadeOut(animationSpec = tween(180)) + shrinkVertically(animationSpec = tween(200)),
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    items(GalleryFilterCategory.entries) { category ->
                                        FilterChipItem(
                                            category = category,
                                            isSelected = category == selectedFilter,
                                            onClick = { galleryViewModel.setFilter(category) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ============================================================
            // PAGE CONTENT (Photos / Albums with subtle 200-250ms slide+fade)
            // ============================================================
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        if (targetState == BottomNavTab.ALBUMS) {
                            (fadeIn(animationSpec = tween(220)) + slideInHorizontally(animationSpec = tween(250)) { width -> width / 8 })
                                .togetherWith(
                                    fadeOut(animationSpec = tween(180)) + slideOutHorizontally(animationSpec = tween(250)) { width -> -width / 8 }
                                )
                        } else {
                            (fadeIn(animationSpec = tween(220)) + slideInHorizontally(animationSpec = tween(250)) { width -> -width / 8 })
                                .togetherWith(
                                    fadeOut(animationSpec = tween(180)) + slideOutHorizontally(animationSpec = tween(250)) { width -> width / 8 }
                                )
                        }
                    },
                    label = "galleryPageContentTransition",
                    modifier = Modifier.fillMaxSize(),
                ) { tab ->
                    when (tab) {
                        BottomNavTab.PHOTOS -> {
                            GalleryScreen(
                                viewModel = galleryViewModel,
                                onPhotoClick = onPhotoClick,
                                onAlbumsClick = {
                                    galleryViewModel.clearSelection()
                                    selectedTab = BottomNavTab.ALBUMS
                                },
                                onSettingsClick = onSettingsClick,
                                onCleanUpClick = onCleanUpClick,
                                gridState = photosGridState,
                                showBottomBar = false,
                                showHeader = false,
                            )
                        }
                        BottomNavTab.ALBUMS -> {
                            AlbumsScreen(
                                viewModel = albumsViewModel,
                                onAlbumClick = onAlbumClick,
                                onPhotosClick = { selectedTab = BottomNavTab.PHOTOS },
                                onSettingsClick = onSettingsClick,
                                onCleanUpClick = onCleanUpClick,
                                gridState = albumsGridState,
                                showBottomBar = false,
                                showHeader = false,
                            )
                        }
                    }
                }
            }
        }

        // ============================================================
        // PERSISTENT FLOATING BOTTOM BAR WITH SLIDING INDICATOR PILL
        // ============================================================
        AnimatedVisibility(
            visible = !isSelectionMode,
            enter = fadeIn(animationSpec = tween(200)) + slideInVertically(animationSpec = tween(250)) { it },
            exit = fadeOut(animationSpec = tween(180)) + slideOutVertically(animationSpec = tween(250)) { it },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            SwipePixBottomBar(
                currentTab = selectedTab,
                onPhotosClick = {
                    galleryViewModel.clearSelection()
                    selectedTab = BottomNavTab.PHOTOS
                },
                onAlbumsClick = {
                    galleryViewModel.clearSelection()
                    selectedTab = BottomNavTab.ALBUMS
                },
                onCleanUpClick = onCleanUpClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = if (isSelectionMode) 16.dp else 92.dp),
        )

        if (showTrashConfirmation) {
            AlertDialog(
                onDismissRequest = { showTrashConfirmation = false },
                title = { Text(text = "Move to Trash?") },
                text = {
                    Text(
                        text = if (pendingTrashCount == 1)
                            "Move 1 item to Android Trash? You can restore it from Trash."
                        else
                            "Move $pendingTrashCount items to Android Trash? You can restore them from Trash."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showTrashConfirmation = false
                            scope.launch {
                                val intentSender = galleryViewModel.createTrashRequestForSelected()
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
                    TextButton(onClick = { showTrashConfirmation = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
