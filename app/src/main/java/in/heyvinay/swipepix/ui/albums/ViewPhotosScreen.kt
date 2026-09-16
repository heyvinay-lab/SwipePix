package `in`.heyvinay.swipepix.ui.albums

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.heyvinay.swipepix.data.model.MediaItem
import `in`.heyvinay.swipepix.ui.components.EmptyScreen
import `in`.heyvinay.swipepix.ui.components.ErrorScreen
import `in`.heyvinay.swipepix.ui.components.LoadingScreen
import `in`.heyvinay.swipepix.ui.components.PhotoGrid
import `in`.heyvinay.swipepix.ui.util.DateUtils
import kotlinx.coroutines.launch
import java.text.NumberFormat

/**
 * View Photos screen — read-only 4-column photo browser for a specific album with multi-select and batch trash.
 * Uses the shared PhotoGrid component with date grouping and pagination.
 * Browsing does NOT modify cleaning progress.
 * Global bottom navigation is hidden to keep album browsing focused and clean.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewPhotosScreen(
    viewModel: ViewPhotosViewModel,
    onPhotoClick: (MediaItem) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val contentState = uiState as? ViewPhotosUiState.Content
    val isSelectionMode = contentState?.isSelectionMode == true
    val selectedIds = contentState?.selectedIds ?: emptySet()
    val totalLoaded = contentState?.totalLoaded ?: 0

    var showTrashConfirmation by remember { mutableStateOf(false) }
    var pendingTrashCount by remember { mutableStateOf(0) }

    val undoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onUndoOperationSuccess()
        }
    }

    val trashLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val count = pendingTrashCount
            viewModel.onTrashOperationSuccess()
            scope.launch {
                val snackbarResult = snackbarHostState.showSnackbar(
                    message = if (count == 1) "1 item moved to trash" else "$count items moved to trash",
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

    BackHandler(enabled = isSelectionMode) {
        viewModel.clearSelection()
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = {
                        Text(
                            text = "${selectedIds.size} selected",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close selection",
                            )
                        }
                    },
                    actions = {
                        val isAllSelected = selectedIds.size == totalLoaded && totalLoaded > 0
                        TextButton(onClick = { viewModel.toggleSelectAll() }) {
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
                    },
                    scrollBehavior = scrollBehavior,
                )
            } else {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = viewModel.albumName,
                                fontWeight = FontWeight.Bold,
                            )
                            if (contentState != null) {
                                val subtitle = DateUtils.formatMediaSubtitle(contentState.photoCount, contentState.videoCount)
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (val state = uiState) {
                is ViewPhotosUiState.Loading -> {
                    LoadingScreen(modifier = Modifier.fillMaxSize())
                }

                is ViewPhotosUiState.Empty -> {
                    EmptyScreen(
                        message = "No photos in this album",
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                is ViewPhotosUiState.Error -> {
                    ErrorScreen(
                        message = state.message,
                        onRetry = { viewModel.refresh() },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                is ViewPhotosUiState.Content -> {
                    PhotoGrid(
                        dateGroups = state.dateGroups,
                        isLoadingMore = state.isLoadingMore,
                        onPhotoClick = { photo ->
                            if (state.isSelectionMode) {
                                viewModel.toggleSelection(photo)
                            } else {
                                onPhotoClick(photo)
                            }
                        },
                        onLoadMore = { viewModel.loadMore() },
                        isSelectionMode = state.isSelectionMode,
                        selectedIds = state.selectedIds,
                        onPhotoLongClick = { photo ->
                            viewModel.enterSelection(photo)
                        },
                        contentPadding = PaddingValues(
                            start = 12.dp,
                            end = 12.dp,
                            top = 8.dp,
                            bottom = 24.dp,
                        ),
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding(),
                    )
                }
            }
        }

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
                                val intentSender = viewModel.createTrashRequestForSelected()
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
