package `in`.heyvinay.swipepix.data.repository

import `in`.heyvinay.swipepix.data.local.entity.CleaningSessionEntity
import `in`.heyvinay.swipepix.data.local.entity.DecisionType
import `in`.heyvinay.swipepix.data.local.entity.PhotoDecisionEntity
import kotlinx.coroutines.flow.Flow

interface CleanupSessionRepository {
    suspend fun getOrCreateActiveSession(albumId: String?, albumName: String?): CleaningSessionEntity
    suspend fun getResumableSession(albumId: String?): CleaningSessionEntity?
    suspend fun startNewSession(albumId: String?, albumName: String?): CleaningSessionEntity
    suspend fun resumeSession(sessionId: String): CleaningSessionEntity?
    suspend fun recordDecision(mediaStoreId: Long, contentUri: String, decision: DecisionType, albumId: String?, sessionId: String)
    suspend fun undoDecision(mediaStoreId: Long)
    suspend fun getReviewedIds(albumId: String?): Set<Long>
    suspend fun getReviewedIdsForSession(sessionId: String): Set<Long>
    suspend fun getPendingTrashDecisions(albumId: String? = null): List<PhotoDecisionEntity>
    suspend fun getPendingTrashDecisionsForSession(sessionId: String): List<PhotoDecisionEntity>
    suspend fun markBatchAsTrashed(mediaStoreIds: List<Long>)
    suspend fun getDecision(mediaStoreId: Long): PhotoDecisionEntity?
    fun observeTrashPendingCount(): Flow<Int>
    fun observeKeptCount(): Flow<Int>
    suspend fun getSummaryCounts(albumId: String?): Pair<Int, Int> // (kept, trashed)
    suspend fun getSummaryCountsForSession(sessionId: String): Pair<Int, Int> // (kept, trashed)
    suspend fun getTotalReviewedCountForSession(sessionId: String): Int
    suspend fun getTotalReviewedCount(albumId: String?): Int
    suspend fun pauseSession(sessionId: String)
    suspend fun completeSession(sessionId: String)
    suspend fun discardSession(albumId: String?)
    suspend fun discardSessionById(sessionId: String)
}
