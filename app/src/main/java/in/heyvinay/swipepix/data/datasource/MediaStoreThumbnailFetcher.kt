package `in`.heyvinay.swipepix.data.datasource

import android.content.ContentResolver
import android.content.ContentResolver.SCHEME_CONTENT
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import coil3.ImageLoader
import coil3.Uri
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.request.Options
import coil3.size.Dimension
import coil3.toAndroidUri

/**
 * High-performance thumbnail fetcher utilizing Android's platform API
 * ContentResolver.loadThumbnail(Uri, Size, CancellationSignal) on API 29+.
 *
 * This allows reading pre-computed hardware-accelerated OS thumbnails for MediaStore
 * images and videos without reading and parsing full multi-megabyte raw files.
 *
 * Falls back cleanly (returning null) for non-thumbnail requests (> 512px), pre-API 29,
 * or when the system thumbnail call fails.
 */
class MediaStoreThumbnailFetcher(
    private val contentResolver: ContentResolver,
    private val data: Uri,
    private val width: Int,
    private val height: Int,
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return null
        }

        return try {
            val androidUri = data.toAndroidUri()
            val targetSize = Size(width.coerceAtLeast(1), height.coerceAtLeast(1))
            val bitmap: Bitmap = contentResolver.loadThumbnail(androidUri, targetSize, null)
            ImageFetchResult(
                image = bitmap.asImage(),
                isSampled = true,
                dataSource = DataSource.DISK,
            )
        } catch (e: Exception) {
            // Silently fall back to standard Coil pipeline (BitmapFactoryDecoder / VideoFrameDecoder)
            null
        }
    }

    class Factory(
        private val contentResolver: ContentResolver,
    ) : Fetcher.Factory<Uri> {

        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
            if (data.scheme != SCHEME_CONTENT) return null
            if (data.authority != MediaStore.AUTHORITY) return null

            val widthPx = (options.size.width as? Dimension.Pixels)?.px ?: return null
            val heightPx = (options.size.height as? Dimension.Pixels)?.px ?: return null

            // Only handle thumbnail requests (e.g. <= 512px) so full-screen viewers decode original quality
            if (widthPx > 512 || heightPx > 512) return null

            return MediaStoreThumbnailFetcher(contentResolver, data, widthPx, heightPx)
        }
    }
}
