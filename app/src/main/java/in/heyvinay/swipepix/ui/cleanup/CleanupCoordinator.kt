package `in`.heyvinay.swipepix.ui.cleanup

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.heyvinay.swipepix.ui.components.ErrorScreen
import `in`.heyvinay.swipepix.ui.components.LoadingScreen
import kotlinx.coroutines.launch

@Composable
fun CleanupCoordinator(
    viewModel: CleanupViewModel,
    onNavigateBack: () -> Unit,
    onCleanupComplete: () -> Unit,
    onViewTrash: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var isApplying by remember { mutableStateOf(false) }
    var isExiting by remember { mutableStateOf(false) }

    val trashLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        isApplying = false
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onTrashOperationSuccess()
        }
    }

    val handleApplyAndFinish: () -> Unit = {
        if (!isApplying) {
            isApplying = true
            scope.launch {
                try {
                    val intentSender = viewModel.createBatchTrashRequest()
                    if (intentSender != null) {
                        trashLauncher.launch(
                            IntentSenderRequest.Builder(intentSender).build()
                        )
                    } else {
                        isApplying = false
                        viewModel.onTrashOperationSuccess()
                    }
                } catch (e: Exception) {
                    isApplying = false
                }
            }
        }
    }

    val handleSaveAndExit: () -> Unit = {
        if (!isExiting) {
            isExiting = true
            viewModel.saveAndExit(onNavigateBack)
        }
    }

    val handleDiscardSession: () -> Unit = {
        if (!isExiting) {
            isExiting = true
            viewModel.discardSession(onNavigateBack)
        }
    }

    when (val state = uiState) {
        is CleanupUiState.Loading -> LoadingScreen()
        is CleanupUiState.Error -> ErrorScreen(
            message = state.message,
            onRetry = onNavigateBack
        )
        is CleanupUiState.Empty -> {
            CleanupEmptyScreen(
                onViewTrash = onViewTrash,
                onDone = onCleanupComplete
            )
        }
        is CleanupUiState.Active -> {
            CleanupScreen(
                state = state,
                onSwipeLeft = viewModel::onSwipeLeft,
                onSwipeRight = viewModel::onSwipeRight,
                onUndo = viewModel::onUndo,
                onFinishEarly = handleApplyAndFinish,
                onSaveAndExit = handleSaveAndExit,
                onDiscardSession = handleDiscardSession,
            )
        }
        is CleanupUiState.Summary -> {
            CleanupSummaryScreen(
                state = state,
                viewModel = viewModel,
                onNavigateBack = onNavigateBack,
                onContinueLater = {
                    if (state.trashedCount == 0) {
                        viewModel.completeSessionWithoutTrash(onCleanupComplete)
                    } else {
                        onCleanupComplete()
                    }
                },
            )
        }
        is CleanupUiState.Success -> {
            CleanupSuccessScreen(
                trashedCount = state.trashedCount,
                onViewTrash = onViewTrash,
                onDone = onCleanupComplete
            )
        }
    }
}
