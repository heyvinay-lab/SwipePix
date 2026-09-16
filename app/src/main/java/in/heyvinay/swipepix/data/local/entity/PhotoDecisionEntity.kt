package `in`.heyvinay.swipepix.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class DecisionType {
    KEEP,
    TRASH_PENDING,
    TRASHED
}

@Entity(
    tableName = "photo_decisions",
    indices = [
        Index(value = ["mediaStoreId"], unique = true),
        Index(value = ["decision"]),
        Index(value = ["albumId"]),
        Index(value = ["sessionId"])
    ]
)
data class PhotoDecisionEntity(
    @PrimaryKey val mediaStoreId: Long,
    val decision: String,
    val contentUri: String,
    val albumId: String? = null,
    val sessionId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
