package `in`.heyvinay.swipepix.data.model

/**
 * Represents a media album/folder on the device.
 *
 * Derived from MediaStore BUCKET_ID / BUCKET_DISPLAY_NAME columns.
 */
data class Album(
    val id: String,
    val displayName: String,
    val coverUri: android.net.Uri?,
    val mediaCount: Int,
    val photoCount: Int = 0,
    val videoCount: Int = 0,
)
