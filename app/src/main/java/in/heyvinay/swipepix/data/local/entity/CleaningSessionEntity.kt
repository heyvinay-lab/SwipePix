package `in`.heyvinay.swipepix.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SessionStatus {
    ACTIVE,
    PAUSED,
    COMPLETED
}

@Entity(tableName = "cleaning_sessions")
data class CleaningSessionEntity(
    @PrimaryKey val sessionId: String,
    val albumId: String? = null,
    val albumName: String? = null,
    val startedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val status: String = SessionStatus.ACTIVE.name,
    val lastReviewedMediaStoreId: Long? = null
)
