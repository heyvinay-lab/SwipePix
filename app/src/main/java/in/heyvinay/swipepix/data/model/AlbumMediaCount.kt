package `in`.heyvinay.swipepix.data.model

/**
 * Authoritative count of media items in a specific scope or library-wide.
 * Queried directly from MediaStore cursors rather than derived from loaded UI lists.
 */
data class AlbumMediaCount(
    val totalCount: Int,
    val photoCount: Int,
    val videoCount: Int,
    val favoriteCount: Int = 0,
)
