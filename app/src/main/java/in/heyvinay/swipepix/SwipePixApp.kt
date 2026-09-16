package `in`.heyvinay.swipepix

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * SwipePix Application class.
 * Required by Hilt for dependency injection setup.
 */
@HiltAndroidApp
class SwipePixApp : Application() {

    @Inject
    lateinit var imageLoader: ImageLoader

    override fun onCreate() {
        super.onCreate()
        SingletonImageLoader.setSafe { imageLoader }
    }
}
