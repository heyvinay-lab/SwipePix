package `in`.heyvinay.swipepix.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import `in`.heyvinay.swipepix.data.local.entity.CleaningSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CleaningSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(session: CleaningSessionEntity): Long

    @Query("SELECT * FROM cleaning_sessions WHERE status = 'ACTIVE' ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getActiveSession(): CleaningSessionEntity?

    @Query("SELECT * FROM cleaning_sessions WHERE status = 'ACTIVE' AND (albumId = :albumId OR (:albumId IS NULL AND albumId IS NULL)) ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getActiveSessionForAlbum(albumId: String?): CleaningSessionEntity?

    @Query("SELECT * FROM cleaning_sessions WHERE status IN ('ACTIVE', 'PAUSED') AND (albumId = :albumId OR (:albumId IS NULL AND albumId IS NULL)) ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getResumableSessionForAlbum(albumId: String?): CleaningSessionEntity?

    @Query("SELECT * FROM cleaning_sessions WHERE status IN ('ACTIVE', 'PAUSED') ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getResumableSession(): CleaningSessionEntity?

    @Query("SELECT * FROM cleaning_sessions WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getSession(sessionId: String): CleaningSessionEntity?

    @Query("SELECT * FROM cleaning_sessions WHERE status = 'ACTIVE' ORDER BY updatedAt DESC LIMIT 1")
    fun observeActiveSession(): Flow<CleaningSessionEntity?>

    @Query("UPDATE cleaning_sessions SET status = :status, updatedAt = :updatedAt WHERE sessionId = :sessionId")
    suspend fun updateSessionStatus(sessionId: String, status: String, updatedAt: Long = System.currentTimeMillis()): Int

    @Query("UPDATE cleaning_sessions SET status = 'PAUSED', updatedAt = :updatedAt WHERE status = 'ACTIVE'")
    suspend fun pauseAllActiveSessions(updatedAt: Long = System.currentTimeMillis()): Int

    @Query("UPDATE cleaning_sessions SET lastReviewedMediaStoreId = :mediaStoreId, updatedAt = :updatedAt WHERE sessionId = :sessionId")
    suspend fun updateLastReviewed(sessionId: String, mediaStoreId: Long, updatedAt: Long = System.currentTimeMillis()): Int

    @Query("DELETE FROM cleaning_sessions WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: String): Int

    @Query("DELETE FROM cleaning_sessions WHERE (albumId = :albumId OR (:albumId IS NULL AND albumId IS NULL))")
    suspend fun deleteSessionsForAlbum(albumId: String?): Int
}
