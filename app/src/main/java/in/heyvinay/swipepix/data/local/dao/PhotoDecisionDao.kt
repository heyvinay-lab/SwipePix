package `in`.heyvinay.swipepix.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import `in`.heyvinay.swipepix.data.local.entity.PhotoDecisionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDecisionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(decision: PhotoDecisionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(decisions: List<PhotoDecisionEntity>): List<Long>

    @Query("SELECT * FROM photo_decisions WHERE mediaStoreId = :mediaStoreId LIMIT 1")
    suspend fun getDecision(mediaStoreId: Long): PhotoDecisionEntity?

    @Query("SELECT * FROM photo_decisions")
    suspend fun getAllDecisions(): List<PhotoDecisionEntity>

    @Query("SELECT * FROM photo_decisions WHERE albumId = :albumId OR (:albumId IS NULL AND albumId IS NULL)")
    suspend fun getDecisionsForAlbum(albumId: String?): List<PhotoDecisionEntity>

    @Query("SELECT * FROM photo_decisions WHERE decision = :decision")
    suspend fun getDecisionsByStatus(decision: String): List<PhotoDecisionEntity>

    @Query("SELECT * FROM photo_decisions WHERE decision = :decision")
    fun observeDecisionsByStatus(decision: String): Flow<List<PhotoDecisionEntity>>

    @Query("SELECT COUNT(*) FROM photo_decisions WHERE decision = :decision")
    fun observeCountByDecision(decision: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM photo_decisions WHERE decision = :decision")
    suspend fun getCountByDecision(decision: String): Int

    @Query("SELECT COUNT(*) FROM photo_decisions WHERE (albumId = :albumId OR (:albumId IS NULL AND albumId IS NULL))")
    suspend fun getTotalReviewedCount(albumId: String?): Int

    @Query("SELECT mediaStoreId FROM photo_decisions WHERE (albumId = :albumId OR (:albumId IS NULL AND albumId IS NULL))")
    suspend fun getReviewedIdsForAlbum(albumId: String?): List<Long>

    @Query("SELECT mediaStoreId FROM photo_decisions WHERE sessionId = :sessionId")
    suspend fun getReviewedIdsForSession(sessionId: String): List<Long>

    @Query("SELECT * FROM photo_decisions WHERE sessionId = :sessionId")
    suspend fun getDecisionsForSession(sessionId: String): List<PhotoDecisionEntity>

    @Query("SELECT COUNT(*) FROM photo_decisions WHERE decision = :decision AND (albumId = :albumId OR (:albumId IS NULL AND albumId IS NULL))")
    suspend fun getCountByDecisionForAlbum(decision: String, albumId: String?): Int

    @Query("SELECT COUNT(*) FROM photo_decisions WHERE decision = :decision AND sessionId = :sessionId")
    suspend fun getCountByDecisionForSession(decision: String, sessionId: String): Int

    @Query("SELECT COUNT(*) FROM photo_decisions WHERE sessionId = :sessionId")
    suspend fun getTotalReviewedCountForSession(sessionId: String): Int

    @Query("SELECT * FROM photo_decisions WHERE decision = 'TRASH_PENDING' AND (albumId = :albumId OR (:albumId IS NULL AND albumId IS NULL))")
    suspend fun getPendingTrashDecisionsForAlbum(albumId: String?): List<PhotoDecisionEntity>

    @Query("SELECT * FROM photo_decisions WHERE decision = 'TRASH_PENDING' AND sessionId = :sessionId")
    suspend fun getPendingTrashDecisionsForSession(sessionId: String): List<PhotoDecisionEntity>

    @Query("DELETE FROM photo_decisions WHERE mediaStoreId = :mediaStoreId")
    suspend fun deleteDecision(mediaStoreId: Long): Int

    @Query("UPDATE photo_decisions SET decision = :newDecision, timestamp = :timestamp WHERE mediaStoreId IN (:mediaStoreIds)")
    suspend fun batchUpdateDecision(mediaStoreIds: List<Long>, newDecision: String, timestamp: Long = System.currentTimeMillis()): Int

    @Query("UPDATE photo_decisions SET decision = :newDecision, timestamp = :timestamp WHERE mediaStoreId = :mediaStoreId")
    suspend fun updateDecision(mediaStoreId: Long, newDecision: String, timestamp: Long = System.currentTimeMillis()): Int

    @Query("DELETE FROM photo_decisions WHERE (albumId = :albumId OR (:albumId IS NULL AND albumId IS NULL)) AND decision != 'TRASHED'")
    suspend fun clearDecisionsForScope(albumId: String?): Int

    @Query("DELETE FROM photo_decisions WHERE sessionId = :sessionId AND decision != 'TRASHED'")
    suspend fun clearDecisionsForSession(sessionId: String): Int

    @Query("DELETE FROM photo_decisions")
    suspend fun clearAll(): Int
}
