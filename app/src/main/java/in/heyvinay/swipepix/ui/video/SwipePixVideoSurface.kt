package `in`.heyvinay.swipepix.ui.video

import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Hardware-accelerated video rendering surface using [TextureView].
 *
 * Designed specifically for Jetpack Compose:
 * 1. Supports Compose clipping (e.g. 24dp rounded corners) without punching holes in the z-buffer.
 * 2. Seamlessly animates with rotation and translation during Swipe Cleaning card flings.
 * 3. Safely binds and releases [Surface] to [SwipePixPlayerEngine].
 */
@Composable
fun SwipePixVideoSurface(
    engine: SwipePixPlayerEngine,
    modifier: Modifier = Modifier,
) {
    var boundSurface by remember { mutableStateOf<Surface?>(null) }

    DisposableEffect(engine) {
        onDispose {
            boundSurface?.release()
            boundSurface = null
            engine.detachSurface()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                TextureView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(
                            surfaceTexture: SurfaceTexture,
                            width: Int,
                            height: Int
                        ) {
                            val newSurface = Surface(surfaceTexture)
                            boundSurface?.release()
                            boundSurface = newSurface
                            engine.attachSurface(newSurface)
                        }

                        override fun onSurfaceTextureSizeChanged(
                            surfaceTexture: SurfaceTexture,
                            width: Int,
                            height: Int
                        ) {}

                        override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                            engine.detachSurface()
                            boundSurface?.release()
                            boundSurface = null
                            return true
                        }

                        override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {}
                    }
                }
            },
            update = {
                // Re-bind if surface exists
                boundSurface?.let { surface ->
                    engine.attachSurface(surface)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
