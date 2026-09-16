package `in`.heyvinay.swipepix.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import `in`.heyvinay.swipepix.ui.theme.SwipePixMotion
import `in`.heyvinay.swipepix.data.permissions.PermissionState
import `in`.heyvinay.swipepix.ui.onboarding.OnboardingScreen
import `in`.heyvinay.swipepix.ui.permissions.PermissionViewModel
import `in`.heyvinay.swipepix.ui.gallery.GalleryScreen
import `in`.heyvinay.swipepix.ui.gallery.GalleryViewModel
import `in`.heyvinay.swipepix.ui.gallery.MainGalleryShell
import `in`.heyvinay.swipepix.ui.gallery.MainGalleryScreen
import `in`.heyvinay.swipepix.ui.albums.AlbumsScreen
import `in`.heyvinay.swipepix.ui.albums.AlbumsViewModel
import `in`.heyvinay.swipepix.ui.albums.AlbumDetailScreen
import `in`.heyvinay.swipepix.ui.albums.AlbumDetailViewModel
import `in`.heyvinay.swipepix.ui.albums.ViewPhotosScreen
import `in`.heyvinay.swipepix.ui.albums.ViewPhotosViewModel
import `in`.heyvinay.swipepix.ui.viewer.PhotoViewerScreen
import `in`.heyvinay.swipepix.ui.viewer.PhotoViewerViewModel
import `in`.heyvinay.swipepix.ui.cleanup.CleanupSourceSelectionScreen
import `in`.heyvinay.swipepix.ui.cleanup.CleanupSourceSelectionViewModel
import `in`.heyvinay.swipepix.ui.cleanup.CleanupCoordinator
import `in`.heyvinay.swipepix.ui.cleanup.CleanupViewModel
import `in`.heyvinay.swipepix.ui.settings.SettingsScreen
import `in`.heyvinay.swipepix.ui.settings.SettingsViewModel
import `in`.heyvinay.swipepix.ui.trash.TrashScreen
import `in`.heyvinay.swipepix.ui.trash.TrashViewModel

/**
 * Main navigation graph for SwipePix.
 * Defines all destinations and navigation flows.
 */
@Composable
fun SwipePixNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var permissionStatus by remember { mutableStateOf(PermissionState.checkPermissionStatus(context)) }

    LifecycleResumeEffect(Unit) {
        permissionStatus = PermissionState.checkPermissionStatus(context)
        onPauseOrDispose { }
    }

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val isPermissionScreen = currentBackStackEntry?.destination?.hasRoute<SwipePixRoute.Permission>() == true ||
        currentBackStackEntry?.destination?.route?.contains("Permission", ignoreCase = true) == true

    LaunchedEffect(permissionStatus, isPermissionScreen, currentBackStackEntry) {
        if (permissionStatus.isGrantedOrPartial && isPermissionScreen) {
            navController.navigate(SwipePixRoute.Gallery) {
                popUpTo(SwipePixRoute.Permission) { inclusive = true }
                launchSingleTop = true
            }
        } else if (permissionStatus.isDeniedOrRequired && !isPermissionScreen && currentBackStackEntry != null) {
            navController.navigate(SwipePixRoute.Permission) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    val startDestination: Any = if (permissionStatus.isGrantedOrPartial) {
        SwipePixRoute.Gallery
    } else {
        SwipePixRoute.Permission
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = {
            fadeIn(animationSpec = tween(SwipePixMotion.DURATION_ENTER, easing = SwipePixMotion.EASING_STANDARD)) +
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(SwipePixMotion.DURATION_ENTER, easing = SwipePixMotion.EASING_STANDARD),
                    initialOffset = { it / 4 },
                )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(SwipePixMotion.DURATION_EXIT, easing = SwipePixMotion.EASING_ACCELERATE)) +
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(SwipePixMotion.DURATION_EXIT, easing = SwipePixMotion.EASING_ACCELERATE),
                    targetOffset = { -it / 6 },
                )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(SwipePixMotion.DURATION_ENTER, easing = SwipePixMotion.EASING_DECELERATE)) +
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(SwipePixMotion.DURATION_ENTER, easing = SwipePixMotion.EASING_DECELERATE),
                    initialOffset = { -it / 6 },
                )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(SwipePixMotion.DURATION_EXIT, easing = SwipePixMotion.EASING_ACCELERATE)) +
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(SwipePixMotion.DURATION_EXIT, easing = SwipePixMotion.EASING_ACCELERATE),
                    targetOffset = { it / 4 },
                )
        },
    ) {
        composable<SwipePixRoute.Permission> {
            val viewModel: PermissionViewModel = hiltViewModel()
            OnboardingScreen(
                viewModel = viewModel,
                onPermissionGranted = {
                    permissionStatus = PermissionState.checkPermissionStatus(context)
                    navController.navigate(SwipePixRoute.Gallery) {
                        popUpTo(SwipePixRoute.Permission) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable<SwipePixRoute.Gallery> {
            val galleryViewModel: GalleryViewModel = hiltViewModel()
            val albumsViewModel: AlbumsViewModel = hiltViewModel()
            MainGalleryShell(
                galleryViewModel = galleryViewModel,
                albumsViewModel = albumsViewModel,
                onPhotoClick = { mediaItem ->
                    navController.navigate(SwipePixRoute.PhotoViewer(mediaId = mediaItem.id))
                },
                onAlbumClick = { album ->
                    navController.navigate(SwipePixRoute.AlbumDetail(album.id, album.displayName))
                },
                onSettingsClick = { navController.navigate(SwipePixRoute.Settings) },
                onCleanUpClick = { navController.navigate(SwipePixRoute.CleanupSourceSelection) },
            )
        }

        composable<SwipePixRoute.Albums> {
            // Redirect to single Gallery destination to maintain a single shell instance
            androidx.compose.runtime.LaunchedEffect(Unit) {
                navController.navigate(SwipePixRoute.Gallery) {
                    popUpTo(SwipePixRoute.Gallery) { inclusive = false }
                    launchSingleTop = true
                }
            }
        }

        composable<SwipePixRoute.AlbumDetail> {
            val viewModel: AlbumDetailViewModel = hiltViewModel()
            AlbumDetailScreen(
                viewModel = viewModel,
                onViewPhotos = { albumId, albumName ->
                    navController.navigate(SwipePixRoute.ViewPhotos(albumId, albumName))
                },
                onStartCleaning = { albumId, albumName, isNewSession ->
                    navController.navigate(SwipePixRoute.Cleanup(albumId, albumName, isNewSession))
                },
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable<SwipePixRoute.ViewPhotos> {
            val viewModel: ViewPhotosViewModel = hiltViewModel()
            ViewPhotosScreen(
                viewModel = viewModel,
                onPhotoClick = { mediaItem ->
                    navController.navigate(SwipePixRoute.PhotoViewer(mediaId = mediaItem.id, albumId = viewModel.albumId))
                },
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable<SwipePixRoute.CleanupSourceSelection> {
            val viewModel: CleanupSourceSelectionViewModel = hiltViewModel()
            CleanupSourceSelectionScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onSourceSelected = { albumId, albumName, isNewSession ->
                    navController.navigate(SwipePixRoute.Cleanup(albumId, albumName, isNewSession)) {
                        popUpTo(SwipePixRoute.CleanupSourceSelection) { inclusive = true }
                    }
                }
            )
        }

        composable<SwipePixRoute.Cleanup> {
            val viewModel: CleanupViewModel = hiltViewModel()
            CleanupCoordinator(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onCleanupComplete = {
                    navController.navigate(SwipePixRoute.Gallery) {
                        popUpTo(SwipePixRoute.Gallery) { inclusive = true }
                    }
                },
                onViewTrash = {
                    navController.navigate(SwipePixRoute.Trash)
                }
            )
        }

        composable<SwipePixRoute.PhotoViewer>(
            enterTransition = {
                fadeIn(animationSpec = tween(SwipePixMotion.DURATION_STANDARD)) +
                    scaleIn(
                        initialScale = 0.92f,
                        animationSpec = tween(SwipePixMotion.DURATION_STANDARD, easing = SwipePixMotion.EASING_DECELERATE),
                    )
            },
            exitTransition = {
                fadeOut(animationSpec = tween(SwipePixMotion.DURATION_FAST))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(SwipePixMotion.DURATION_FAST))
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(SwipePixMotion.DURATION_FAST)) +
                    scaleOut(
                        targetScale = 0.92f,
                        animationSpec = tween(SwipePixMotion.DURATION_FAST, easing = SwipePixMotion.EASING_ACCELERATE),
                    )
            },
        ) { backStackEntry ->
            val viewModel: PhotoViewerViewModel = hiltViewModel()
            PhotoViewerScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<SwipePixRoute.Trash> {
            val viewModel: TrashViewModel = hiltViewModel()
            TrashScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<SwipePixRoute.Settings> {
            val viewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onViewTrashClick = { navController.navigate(SwipePixRoute.Trash) },
            )
        }
    }
}

/**
 * Temporary placeholder screen displayed during early development phases.
 * Will be replaced with real implementations in subsequent phases.
 */
@Composable
private fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
