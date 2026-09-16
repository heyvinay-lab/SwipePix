package `in`.heyvinay.swipepix.ui.permissions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.heyvinay.swipepix.data.permissions.PermissionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PermissionViewModel @Inject constructor(
    application: Application,
) : AndroidViewModel(application) {

    private val _permissionState = MutableStateFlow(
        PermissionState.checkPermissionStatus(application)
    )
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()

    fun refreshPermissionStatus() {
        viewModelScope.launch {
            _permissionState.value = PermissionState.checkPermissionStatus(getApplication())
        }
    }
}
