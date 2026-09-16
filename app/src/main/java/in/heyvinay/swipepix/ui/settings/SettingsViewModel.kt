package `in`.heyvinay.swipepix.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import `in`.heyvinay.swipepix.data.permissions.PermissionState
import `in`.heyvinay.swipepix.data.preferences.AppTheme
import `in`.heyvinay.swipepix.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    val currentTheme: StateFlow<AppTheme> = preferencesRepository.appTheme
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppTheme.SYSTEM,
        )

    val rememberProgress: StateFlow<Boolean> = preferencesRepository.rememberProgress
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true,
        )

    val showUndoOption: StateFlow<Boolean> = preferencesRepository.showUndoOption
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true,
        )

    val confirmBeforeApplying: StateFlow<Boolean> = preferencesRepository.confirmBeforeApplying
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true,
        )

    private val _permissionState = MutableStateFlow(PermissionState.checkPermissionStatus(context))
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()

    fun updateTheme(theme: AppTheme) {
        viewModelScope.launch {
            preferencesRepository.setAppTheme(theme)
        }
    }

    fun setRememberProgress(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setRememberProgress(enabled)
        }
    }

    fun setShowUndoOption(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setShowUndoOption(enabled)
        }
    }

    fun setConfirmBeforeApplying(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setConfirmBeforeApplying(enabled)
        }
    }

    fun refreshPermissions() {
        _permissionState.value = PermissionState.checkPermissionStatus(context)
    }

    fun openAppSettings(activityContext: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activityContext.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        activityContext.startActivity(intent)
    }
}
