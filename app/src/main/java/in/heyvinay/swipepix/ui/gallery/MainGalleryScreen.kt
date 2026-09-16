package `in`.heyvinay.swipepix.ui.gallery

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import `in`.heyvinay.swipepix.data.model.Album
import `in`.heyvinay.swipepix.data.model.MediaItem
import `in`.heyvinay.swipepix.ui.albums.AlbumsViewModel
import `in`.heyvinay.swipepix.ui.components.BottomNavTab

/**
 * Compatibility wrapper delegating to [MainGalleryShell].
 */
@Composable
fun MainGalleryScreen(
    galleryViewModel: GalleryViewModel,
    albumsViewModel: AlbumsViewModel,
    onPhotoClick: (MediaItem) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onSettingsClick: () -> Unit,
    onCleanUpClick: () -> Unit,
    modifier: Modifier = Modifier,
    initialTab: BottomNavTab = BottomNavTab.PHOTOS,
) {
    MainGalleryShell(
        galleryViewModel = galleryViewModel,
        albumsViewModel = albumsViewModel,
        onPhotoClick = onPhotoClick,
        onAlbumClick = onAlbumClick,
        onSettingsClick = onSettingsClick,
        onCleanUpClick = onCleanUpClick,
        modifier = modifier,
        initialTab = initialTab,
    )
}
