package `in`.heyvinay.swipepix.ui.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import `in`.heyvinay.swipepix.data.model.MediaItem

/**
 * Safe video external playback utility.
 *
 * Enforces Content URI safety, grants read permissions, and uses Android's
 * standard ACTION_VIEW intent chooser for opening videos with installed players.
 */
object VideoIntentUtils {

    fun createVideoViewIntent(mediaItem: MediaItem): Intent {
        val mime = if (mediaItem.mimeType.startsWith("video/")) {
            mediaItem.mimeType
        } else {
            "video/*"
        }
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(mediaItem.contentUri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun createVideoChooserIntent(mediaItem: MediaItem, title: String = "Open with"): Intent {
        val viewIntent = createVideoViewIntent(mediaItem)
        return Intent.createChooser(viewIntent, title)
    }

    fun openVideoWithExternalPlayer(
        context: Context,
        mediaItem: MediaItem,
        title: String = "Open with",
        onNoPlayerFound: (() -> Unit)? = null,
    ): Boolean {
        return try {
            val chooser = createVideoChooserIntent(mediaItem, title)
            context.startActivity(chooser)
            true
        } catch (e: ActivityNotFoundException) {
            if (onNoPlayerFound != null) {
                onNoPlayerFound()
            } else {
                Toast.makeText(
                    context,
                    "No compatible video player is installed",
                    Toast.LENGTH_SHORT
                ).show()
            }
            false
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Unable to play video with external player",
                Toast.LENGTH_SHORT
            ).show()
            false
        }
    }
}
