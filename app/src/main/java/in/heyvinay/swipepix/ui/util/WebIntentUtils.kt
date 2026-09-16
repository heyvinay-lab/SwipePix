package `in`.heyvinay.swipepix.ui.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Safe external URL opening utility.
 *
 * Enforces HTTPS scheme and safely catches ActivityNotFoundException if no browser is installed.
 */
object WebIntentUtils {

    fun isValidHttpsUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return url.startsWith("https://", ignoreCase = true)
    }

    fun createViewIntent(url: String): Intent {
        require(isValidHttpsUrl(url)) {
            "Only HTTPS URLs are permitted: $url"
        }
        val uri = Uri.parse(url)
        return Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun openUrl(context: Context, url: String) {
        try {
            val intent = createViewIntent(url)
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No web browser found to open link", Toast.LENGTH_SHORT).show()
        } catch (e: IllegalArgumentException) {
            Toast.makeText(context, e.message ?: "Invalid URL", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to open link", Toast.LENGTH_SHORT).show()
        }
    }
}
