package `in`.heyvinay.swipepix.data.repository

import `in`.heyvinay.swipepix.data.local.dao.CleaningSessionDao
import `in`.heyvinay.swipepix.data.local.dao.PhotoDecisionDao
import `in`.heyvinay.swipepix.data.local.entity.CleaningSessionEntity
import `in`.heyvinay.swipepix.data.local.entity.DecisionType
import `in`.heyvinay.swipepix.data.local.entity.PhotoDecisionEntity
import `in`.heyvinay.swipepix.data.local.entity.SessionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CleanupSessionRepositoryImpl @Inject constructor(
    private val decisionDao: PhotoDecisionDao,
    private val sessionDao: CleaningSessionDao,
) : CleanupSessionRepository {

    override suspend fun getResumableSession(albumId: String?): CleaningSessionEntity? = withContext(Dispatchers.IO) {
        sessionDao.getResumableSessionForAlbum(albumId)
    }

    override suspend fun startNewSession(
        albumId: String?,
        albumName: String?
    ): CleaningSessionEntity = withContext(NonCancellable + Dispatchers.IO) {
        // Discard any existing resumable (unfinished) session and its decisions for this scope
        val existingResumable = sessionDao.getResumableSessionForAlbum(albumId)
        if (existingResumable != null) {
            decisionDao.clearDecisionsForSession(existingResumable.sessionId)
            sessionDao.deleteSession(existingResumable.sessionId)
        }

        // Pause any other active sessions
        sessionDao.pauseAllActiveSessions(System.currentTimeMillis())

        val newSession = CleaningSessionEntity(
            sessionId = UUID.randomUUID().toString(),
            albumId = albumId,
            albumName = albumName,
            startedAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            status = SessionStatus.ACTIVE.name,
            lastReviewedMediaStoreId = null
        )
        sessionDao.insertOrUpdate(newSession)
        newSession
    }

    override suspend fun resumeSession(sessionId: String): CleaningSessionEntity? = withContext(NonCancellable + Dispatchers.IO) {
        sessionDao.pauseAllActiveSessions(System.currentTimeMillis())
        sessionDao.updateSessionStatus(sessionId, SessionStatus.ACTIVE.name, System.currentTimeMillis())
        sessionDao.getSession(sessionId)
    }

    override suspend fun getOrCreateActiveSession(
        albumId: String?,
        albumName: String?
    ): CleaningSessionEntity = withContext(Dispatchers.IO) {
        val existing = sessionDao.getResumableSessionForAlbum(albumId)
        if (existing != null) {
            // If it was paused, reactivate it and pause others
            if (existing.status != SessionStatus.ACTIVE.name) {
                sessionDao.pauseAllActiveSessions(System.currentTimeMillis())
                sessionDao.updateSessionStatus(existing.sessionId, SessionStatus.ACTIVE.name, System.currentTimeMillis())
            }
            return@withContext existing.copy(status = SessionStatus.ACTIVE.name)
        }

        startNewSession(albumId, albumName)
    }

    override suspend fun recordDecision(
        mediaStoreId: Long,
        contentUri: String,
        decision: DecisionType,
        albumId: String?,
        sessionId: String
    ): Unit = withContext(NonCancellable + Dispatchers.IO) {
        val entity = PhotoDecisionEntity(
            mediaStoreId = mediaStoreId,
            decision = decision.name,
            contentUri = contentUri,
            albumId = albumId,
            sessionId = sessionId,
            timestamp = System.currentTimeMillis()
        )
        decisionDao.insertOrUpdate(entity)
        sessionDao.updateLastReviewed(sessionId, mediaStoreId, System.currentTimeMillis())
    }

    override suspend fun undoDecision(mediaStoreId: Long): Unit = withContext(NonCancellable + Dispatchers.IO) {
        decisionDao.deleteDecision(mediaStoreId)
    }

    override suspend fun getReviewedIds(albumId: String?): Set<Long> = withContext(Dispatchers.IO) {
        val ids = decisionDao.getReviewedIdsForAlbum(albumId)
        ids.toSet()
    }

    override suspend fun getReviewedIdsForSession(sessionId: String): Set<Long> = withContext(Dispatchers.IO) {
        decisionDao.getReviewedIdsForSession(sessionId).toSet()
    }

    override suspend fun getPendingTrashDecisions(albumId: String?): List<PhotoDecisionEntity> = withContext(Dispatchers.IO) {
        decisionDao.getPendingTrashDecisionsForAlbum(albumId)
    }

    override suspend fun getPendingTrashDecisionsForSession(sessionId: String): List<PhotoDecisionEntity> = withContext(Dispatchers.IO) {
        decisionDao.getPendingTrashDecisionsForSession(sessionId)
    }

    override suspend fun markBatchAsTrashed(mediaStoreIds: List<Long>): Unit = withContext(NonCancellable + Dispatchers.IO) {
        decisionDao.batchUpdateDecision(
            mediaStoreIds = mediaStoreIds,
            newDecision = DecisionType.TRASHED.name,
            timestamp = System.currentTimeMillis()
        )
    }

    override suspend fun getDecision(mediaStoreId: Long): PhotoDecisionEntity? = withContext(Dispatchers.IO) {
        decisionDao.getDecision(mediaStoreId)
    }

    override fun observeTrashPendingCount(): Flow<Int> {
        return decisionDao.observeCountByDecision(DecisionType.TRASH_PENDING.name)
    }

    override fun observeKeptCount(): Flow<Int> {
        return decisionDao.observeCountByDecision(DecisionType.KEEP.name)
    }

    override suspend fun getSummaryCounts(albumId: String?): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val kept = decisionDao.getCountByDecisionForAlbum(DecisionType.KEEP.name, albumId)
        val trashed = decisionDao.getCountByDecisionForAlbum(DecisionType.TRASH_PENDING.name, albumId)
        Pair(kept, trashed)
    }

    override suspend fun getSummaryCountsForSession(sessionId: String): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val kept = decisionDao.getCountByDecisionForSession(DecisionType.KEEP.name, sessionId)
        val trashed = decisionDao.getCountByDecisionForSession(DecisionType.TRASH_PENDING.name, sessionId)
        Pair(kept, trashed)
    }

    override suspend fun getTotalReviewedCountForSession(sessionId: String): Int = withContext(Dispatchers.IO) {
        decisionDao.getTotalReviewedCountForSession(sessionId)
    }

    override suspend fun getTotalReviewedCount(albumId: String?): Int = withContext(Dispatchers.IO) {
        decisionDao.getTotalReviewedCount(albumId)
    }

    override suspend fun pauseSession(sessionId: String): Unit = withContext(NonCancellable + Dispatchers.IO) {
        sessionDao.updateSessionStatus(sessionId, SessionStatus.PAUSED.name, System.currentTimeMillis())
    }

    override suspend fun completeSession(sessionId: String): Unit = withContext(NonCancellable + Dispatchers.IO) {
        sessionDao.updateSessionStatus(sessionId, SessionStatus.COMPLETED.name, System.currentTimeMillis())
    }

    override suspend fun discardSession(albumId: String?): Unit = withContext(NonCancellable + Dispatchers.IO) {
        val active = sessionDao.getResumableSessionForAlbum(albumId)
        if (active != null) {
            decisionDao.clearDecisionsForSession(active.sessionId)
            sessionDao.deleteSession(active.sessionId)
        } else {
            decisionDao.clearDecisionsForScope(albumId)
            sessionDao.deleteSessionsForAlbum(albumId)
        }
    }

    override suspend fun discardSessionById(sessionId: String): Unit = withContext(NonCancellable + Dispatchers.IO) {
        decisionDao.clearDecisionsForSession(sessionId)
        sessionDao.deleteSession(sessionId)
    }
}
