package `in`.heyvinay.swipepix.ui.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationChromeTest {

    @Test
    fun showsGlobalGalleryNavigation_allowedOnlyOnGalleryAndAlbums() {
        // Must be shown on main gallery tabs
        assertTrue(SwipePixRoute.Gallery.showsGlobalGalleryNavigation)
        assertTrue(SwipePixRoute.Albums.showsGlobalGalleryNavigation)
    }

    @Test
    fun showsGlobalGalleryNavigation_hiddenOnAllSecondaryAndImmersiveDestinations() {
        // Album Detail & View Photos
        assertFalse(SwipePixRoute.AlbumDetail("album_1", "WhatsApp").showsGlobalGalleryNavigation)
        assertFalse(SwipePixRoute.ViewPhotos("album_1", "WhatsApp").showsGlobalGalleryNavigation)

        // Photo Viewer
        assertFalse(SwipePixRoute.PhotoViewer(mediaId = 12345L, albumId = "album_1").showsGlobalGalleryNavigation)
        assertFalse(SwipePixRoute.PhotoViewer(mediaId = 12345L, albumId = null).showsGlobalGalleryNavigation)

        // Cleanup flows
        assertFalse(SwipePixRoute.CleanupSourceSelection.showsGlobalGalleryNavigation)
        assertFalse(SwipePixRoute.Cleanup().showsGlobalGalleryNavigation)
        assertFalse(SwipePixRoute.Cleanup(albumId = "album_1", albumName = "WhatsApp").showsGlobalGalleryNavigation)

        // Trash, Settings, Permission
        assertFalse(SwipePixRoute.Trash.showsGlobalGalleryNavigation)
        assertFalse(SwipePixRoute.Settings.showsGlobalGalleryNavigation)
        assertFalse(SwipePixRoute.Permission.showsGlobalGalleryNavigation)
    }
}
