package `in`.heyvinay.swipepix.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes for SwipePix.
 * Uses Kotlin serialization for Navigation Compose type-safe routing.
 */
sealed interface SwipePixRoute {

    @Serializable
    data object Permission : SwipePixRoute

    @Serializable
    data object Gallery : SwipePixRoute

    @Serializable
    data object Albums : SwipePixRoute

    @Serializable
    data class AlbumDetail(val albumId: String, val albumName: String) : SwipePixRoute

    @Serializable
    data class ViewPhotos(val albumId: String, val albumName: String) : SwipePixRoute

    @Serializable
    data object CleanupSourceSelection : SwipePixRoute

    @Serializable
    data class Cleanup(
        val albumId: String? = null,
        val albumName: String? = null,
        val isNewSession: Boolean = false,
    ) : SwipePixRoute

    @Serializable
    data class PhotoViewer(val mediaId: Long, val albumId: String? = null) : SwipePixRoute

    @Serializable
    data object Trash : SwipePixRoute

    @Serializable
    data object Settings : SwipePixRoute

    /**
     * Determines whether the global gallery navigation (Photos/Albums pill & Clean Up button)
     * is permitted on this destination.
     *
     * Rule: Shown ONLY on top-level Gallery (Photos/Albums shell).
     * Hidden on all secondary / full-screen destinations (Album Detail, View Photos, Photo Viewer,
     * Cleanup, Trash, Settings, Onboarding, Permission).
     */
    val showsGlobalGalleryNavigation: Boolean
        get() = when (this) {
            is Gallery, is Albums -> true
            is AlbumDetail,
            is ViewPhotos,
            is CleanupSourceSelection,
            is Cleanup,
            is PhotoViewer,
            is Trash,
            is Settings,
            is Permission -> false
        }
}

