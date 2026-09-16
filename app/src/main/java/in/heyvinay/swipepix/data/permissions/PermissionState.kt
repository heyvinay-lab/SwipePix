package `in`.heyvinay.swipepix.data.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Represents the current permission state for accessing user media.
 */
sealed interface PermissionState {
    /**
     * Initial unverified state before any system permission check.
     */
    data object Unknown : PermissionState

    /**
     * Actively querying the Android OS PackageManager for permission status.
     */
    data object Checking : PermissionState

    /**
     * User has granted full access to device media.
     */
    data object Granted : PermissionState

    /**
     * User has granted partial access to select photos/videos (Android 14+).
     */
    data object Partial : PermissionState

    /**
     * User has denied media permission or has not been prompted yet.
     */
    data object Denied : PermissionState

    /**
     * Explicit state indicating media permissions must be requested from the user.
     */
    data object PermissionRequired : PermissionState

    val isGrantedOrPartial: Boolean
        get() = this is Granted || this is Partial

    val isDeniedOrRequired: Boolean
        get() = this is Denied || this is PermissionRequired

    companion object {
        /**
         * Returns the list of required media permissions based on Android API level.
         */
        fun getRequiredPermissions(): Array<String> {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
                )
            } else {
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                )
            }
        }

        /**
         * Evaluates current permission state for the application.
         */
        fun checkPermissionStatus(context: Context): PermissionState {
            val hasImages = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_IMAGES,
            ) == PackageManager.PERMISSION_GRANTED

            val hasVideo = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_VIDEO,
            ) == PackageManager.PERMISSION_GRANTED

            if (hasImages && hasVideo) {
                return Granted
            }

            // If user granted either images or videos, they have granted media access
            if (hasImages || hasVideo) {
                return Partial
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val hasPartial = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
                ) == PackageManager.PERMISSION_GRANTED

                if (hasPartial) {
                    return Partial
                }
            }

            return Denied
        }
    }
}
