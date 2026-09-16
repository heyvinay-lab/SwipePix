package `in`.heyvinay.swipepix.ui.cleanup

import android.net.FakeUri
import androidx.compose.ui.unit.dp
import `in`.heyvinay.swipepix.data.local.entity.CleaningSessionEntity
import `in`.heyvinay.swipepix.data.local.entity.DecisionType
import `in`.heyvinay.swipepix.data.local.entity.PhotoDecisionEntity
import `in`.heyvinay.swipepix.data.local.entity.SessionStatus
import `in`.heyvinay.swipepix.data.model.Album
import `in`.heyvinay.swipepix.data.model.MediaItem
import `in`.heyvinay.swipepix.data.model.MediaType
import `in`.heyvinay.swipepix.data.repository.CleanupSessionRepository
import `in`.heyvinay.swipepix.data.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CleanupSessionTest {

    private lateinit var fakeMediaRepo: FakeMediaRepository
    private lateinit var fakeSessionRepo: FakeCleanupSessionRepository

    @Before
    fun setUp() {
        fakeMediaRepo = FakeMediaRepository()
        fakeSessionRepo = FakeCleanupSessionRepository()
    }

    @Test
    fun testSwipeRight_recordsKeepDecisionLocally() = runBlocking {
        val photo1 = createMediaItem(101L, "IMG_101.jpg")
        val photo2 = createMediaItem(102L, "IMG_102.jpg")
        fakeMediaRepo.photos = listOf(photo1, photo2)

        fakeSessionRepo.recordDecision(
            mediaStoreId = photo1.id,
            contentUri = photo1.contentUri.toString(),
            decision = DecisionType.KEEP,
            albumId = null,
            sessionId = "test-session"
        )

        val decision = fakeSessionRepo.getDecision(photo1.id)
        assertNotNull(decision)
        assertEquals(DecisionType.KEEP.name, decision?.decision)
        assertEquals(1, fakeSessionRepo.getReviewedIds(null).size)
        assertTrue(fakeSessionRepo.getReviewedIds(null).contains(101L))
    }

    @Test
    fun testSwipeLeft_recordsTrashPendingLocally_withoutActualAndroidTrash() = runBlocking {
        val photo1 = createMediaItem(201L, "IMG_201.jpg")
        fakeMediaRepo.photos = listOf(photo1)

        fakeSessionRepo.recordDecision(
            mediaStoreId = photo1.id,
            contentUri = photo1.contentUri.toString(),
            decision = DecisionType.TRASH_PENDING,
            albumId = null,
            sessionId = "test-session"
        )

        val decision = fakeSessionRepo.getDecision(photo1.id)
        assertNotNull(decision)
        assertEquals(DecisionType.TRASH_PENDING.name, decision?.decision)

        // Verify it is in pending trash decisions
        val pending = fakeSessionRepo.getPendingTrashDecisions()
        assertEquals(1, pending.size)
        assertEquals(201L, pending[0].mediaStoreId)

        // Ensure MediaRepository was NOT invoked for actual OS trash yet!
        assertEquals(0, fakeMediaRepo.trashedMediaItemsCalledCount)
    }

    @Test
    fun testUndo_removesDecision_restoresToUnreviewed() = runBlocking {
        val photo1 = createMediaItem(301L, "IMG_301.jpg")

        // Record a trash pending decision
        fakeSessionRepo.recordDecision(
            mediaStoreId = photo1.id,
            contentUri = photo1.contentUri.toString(),
            decision = DecisionType.TRASH_PENDING,
            albumId = null,
            sessionId = "test-session"
        )

        assertEquals(1, fakeSessionRepo.getPendingTrashDecisions().size)

        // Undo
        fakeSessionRepo.undoDecision(photo1.id)

        // Verify decision is deleted
        val decision = fakeSessionRepo.getDecision(photo1.id)
        assertEquals(null, decision)
        assertEquals(0, fakeSessionRepo.getPendingTrashDecisions().size)
        assertEquals(0, fakeSessionRepo.getReviewedIds(null).size)
    }

    @Test
    fun testResumeSession_skipsAlreadyReviewedPhotos() = runBlocking {
        val photo1 = createMediaItem(1L, "1.jpg")
        val photo2 = createMediaItem(2L, "2.jpg")
        val photo3 = createMediaItem(3L, "3.jpg")
        val allPhotos = listOf(photo1, photo2, photo3)

        // Photo 1 was KEPT, Photo 2 was TRASH_PENDING
        fakeSessionRepo.recordDecision(1L, "uri1", DecisionType.KEEP, null, "s1")
        fakeSessionRepo.recordDecision(2L, "uri2", DecisionType.TRASH_PENDING, null, "s1")

        val reviewedIds = fakeSessionRepo.getReviewedIds(null)
        val remaining = allPhotos.filter { it.id !in reviewedIds }

        assertEquals(1, remaining.size)
        assertEquals(3L, remaining[0].id)
    }

    @Test
    fun testBatchTrashOperation_marksItemsAsTrashedOnConfirmation() = runBlocking {
        fakeSessionRepo.recordDecision(501L, "uri501", DecisionType.TRASH_PENDING, null, "s1")
        fakeSessionRepo.recordDecision(502L, "uri502", DecisionType.TRASH_PENDING, null, "s1")

        val pending = fakeSessionRepo.getPendingTrashDecisions()
        assertEquals(2, pending.size)

        // Simulate OS confirmation success
        fakeSessionRepo.markBatchAsTrashed(pending.map { it.mediaStoreId })

        // Pending trash should now be empty (converted to TRASHED)
        val pendingAfter = fakeSessionRepo.getPendingTrashDecisions()
        assertEquals(0, pendingAfter.size)

        val item1 = fakeSessionRepo.getDecision(501L)
        val item2 = fakeSessionRepo.getDecision(502L)
        assertEquals(DecisionType.TRASHED.name, item1?.decision)
        assertEquals(DecisionType.TRASHED.name, item2?.decision)
    }

    @Test
    fun testCancelledTrashRequest_preservesPendingDecisions() = runBlocking {
        fakeSessionRepo.recordDecision(601L, "uri601", DecisionType.TRASH_PENDING, null, "s1")

        // User denies or cancels the Android dialog
        // markBatchAsTrashed is NOT called!

        val pending = fakeSessionRepo.getPendingTrashDecisions()
        assertEquals(1, pending.size)
        assertEquals(DecisionType.TRASH_PENDING.name, pending[0].decision)
    }

    @Test
    fun testAlbumProgressIsolation_decisionsInOneAlbumDoNotAffectAnotherAlbum() = runBlocking {
        // Record decisions for Album A
        fakeSessionRepo.recordDecision(701L, "uri701", DecisionType.KEEP, "albumA", "sessionA")
        fakeSessionRepo.recordDecision(702L, "uri702", DecisionType.TRASH_PENDING, "albumA", "sessionA")

        // Record decision for Album B
        fakeSessionRepo.recordDecision(801L, "uri801", DecisionType.KEEP, "albumB", "sessionB")

        // Reviewed IDs for Album A should only contain 701, 702
        val reviewedA = fakeSessionRepo.getReviewedIds("albumA")
        assertEquals(2, reviewedA.size)
        assertTrue(reviewedA.contains(701L))
        assertTrue(reviewedA.contains(702L))
        assertFalse(reviewedA.contains(801L))

        // Reviewed IDs for Album B should only contain 801
        val reviewedB = fakeSessionRepo.getReviewedIds("albumB")
        assertEquals(1, reviewedB.size)
        assertTrue(reviewedB.contains(801L))
        assertFalse(reviewedB.contains(701L))

        // Pending trash for Album A should only contain 702
        val pendingA = fakeSessionRepo.getPendingTrashDecisions("albumA")
        assertEquals(1, pendingA.size)
        assertEquals(702L, pendingA[0].mediaStoreId)

        // Pending trash for Album B should be empty
        val pendingB = fakeSessionRepo.getPendingTrashDecisions("albumB")
        assertEquals(0, pendingB.size)

        // Summary counts for Album A
        val countsA = fakeSessionRepo.getSummaryCounts("albumA")
        assertEquals(1, countsA.first)
        assertEquals(1, countsA.second)

        // Summary counts for Album B
        val countsB = fakeSessionRepo.getSummaryCounts("albumB")
        assertEquals(1, countsB.first)
        assertEquals(0, countsB.second)
    }

    @Test
    fun testDiscardSession_clearsUnappliedDecisions_preservesOtherScopesAndTrashedItems() = runBlocking {
        // Setup Album A decisions
        fakeSessionRepo.recordDecision(901L, "uri901", DecisionType.KEEP, "albumA", "sessionA")
        fakeSessionRepo.recordDecision(902L, "uri902", DecisionType.TRASH_PENDING, "albumA", "sessionA")
        fakeSessionRepo.recordDecision(903L, "uri903", DecisionType.TRASHED, "albumA", "sessionA")

        // Setup Album B decision
        fakeSessionRepo.recordDecision(904L, "uri904", DecisionType.KEEP, "albumB", "sessionB")

        // Discard session for Album A
        fakeSessionRepo.discardSession("albumA")

        // Album A KEEP and TRASH_PENDING should be deleted
        assertEquals(null, fakeSessionRepo.getDecision(901L))
        assertEquals(null, fakeSessionRepo.getDecision(902L))

        // Album A TRASHED should remain (already committed to Android OS Trash)
        val trashedDecision = fakeSessionRepo.getDecision(903L)
        assertNotNull(trashedDecision)
        assertEquals(DecisionType.TRASHED.name, trashedDecision?.decision)

        // Album B decision should remain completely untouched
        val albumBDecision = fakeSessionRepo.getDecision(904L)
        assertNotNull(albumBDecision)
        assertEquals(DecisionType.KEEP.name, albumBDecision?.decision)
    }

    @Test
    fun testDiscardSession_doesNotEraseDecisionsFromCompletedSessions() = runBlocking {
        // Completed session A recorded KEEP decision
        fakeSessionRepo.recordDecision(910L, "uri910", DecisionType.KEEP, "albumA", "session-completed-1")

        // Start new active session A
        val active = fakeSessionRepo.getOrCreateActiveSession("albumA", "Camera")
        // Active session records a KEEP and TRASH_PENDING
        fakeSessionRepo.recordDecision(911L, "uri911", DecisionType.KEEP, "albumA", active.sessionId)
        fakeSessionRepo.recordDecision(912L, "uri912", DecisionType.TRASH_PENDING, "albumA", active.sessionId)

        // Discard current active session
        fakeSessionRepo.discardSession("albumA")

        // Active session decisions 911 and 912 must be removed
        assertEquals(null, fakeSessionRepo.getDecision(911L))
        assertEquals(null, fakeSessionRepo.getDecision(912L))

        // Previously completed session decision 910 MUST STILL EXIST!
        val preserved = fakeSessionRepo.getDecision(910L)
        assertNotNull(preserved)
        assertEquals(DecisionType.KEEP.name, preserved?.decision)
    }

    @Test
    fun testStartNewSession_clearsPreviousUnappliedDecisions_createsDistinctSessionId() = runBlocking {
        // Start session 1, swipe photos
        val session1 = fakeSessionRepo.startNewSession("camera", "Camera")
        fakeSessionRepo.recordDecision(101L, "uri101", DecisionType.KEEP, "camera", session1.sessionId)
        fakeSessionRepo.recordDecision(102L, "uri102", DecisionType.TRASH_PENDING, "camera", session1.sessionId)

        // User starts a NEW session for the same album
        val session2 = fakeSessionRepo.startNewSession("camera", "Camera")

        // Must have distinct session IDs
        assertTrue(session1.sessionId != session2.sessionId)

        // Previous unapplied decisions must be cleared
        val reviewedSession2 = fakeSessionRepo.getReviewedIdsForSession(session2.sessionId)
        assertEquals(0, reviewedSession2.size)
        assertEquals(null, fakeSessionRepo.getDecision(101L))
        assertEquals(null, fakeSessionRepo.getDecision(102L))
    }

    @Test
    fun testSaveAndExit_thenResume_preservesReviewedCountsAndUnreviewedItems() = runBlocking {
        val photo1 = createMediaItem(1L, "1.jpg")
        val photo2 = createMediaItem(2L, "2.jpg")
        val photo3 = createMediaItem(3L, "3.jpg")
        val allPhotos = listOf(photo1, photo2, photo3)

        val session = fakeSessionRepo.startNewSession("camera", "Camera")
        fakeSessionRepo.recordDecision(1L, "uri1", DecisionType.KEEP, "camera", session.sessionId)
        fakeSessionRepo.recordDecision(2L, "uri2", DecisionType.TRASH_PENDING, "camera", session.sessionId)

        // Save & Exit (pauses session)
        fakeSessionRepo.pauseSession(session.sessionId)

        // Verify session is resumable
        val resumable = fakeSessionRepo.getResumableSession("camera")
        assertNotNull(resumable)
        assertEquals(SessionStatus.PAUSED.name, resumable?.status)

        // Resume session
        val resumed = fakeSessionRepo.getOrCreateActiveSession("camera", "Camera")
        assertEquals(session.sessionId, resumed.sessionId)
        assertEquals(SessionStatus.ACTIVE.name, resumed.status)

        // Reviewed IDs for this session
        val reviewedIds = fakeSessionRepo.getReviewedIdsForSession(resumed.sessionId)
        assertEquals(2, reviewedIds.size)
        assertTrue(reviewedIds.contains(1L))
        assertTrue(reviewedIds.contains(2L))

        // Remaining deck items: photo 1 and 2 skipped, starts at photo 3
        val remaining = allPhotos.filter { it.id !in reviewedIds }
        assertEquals(1, remaining.size)
        assertEquals(3L, remaining[0].id)

        // Counts preserved
        val counts = fakeSessionRepo.getSummaryCountsForSession(resumed.sessionId)
        assertEquals(1, counts.first)
        assertEquals(1, counts.second)
    }

    @Test
    fun testCompleteSession_marksStatusCompleted_andResumableReturnsNull() = runBlocking {
        val session = fakeSessionRepo.startNewSession("camera", "Camera")
        fakeSessionRepo.recordDecision(1L, "uri1", DecisionType.KEEP, "camera", session.sessionId)

        // Verify active session is found
        val active = fakeSessionRepo.getResumableSession("camera")
        assertNotNull(active)
        assertEquals(session.sessionId, active?.sessionId)

        // Complete session
        fakeSessionRepo.completeSession(session.sessionId)

        // Resumable session should now be null
        val resumableAfterComplete = fakeSessionRepo.getResumableSession("camera")
        assertEquals(null, resumableAfterComplete)
    }

    @Test
    fun testDiscardSessionById_removesOnlyTargetSessionDecisions() = runBlocking {
        val session1 = fakeSessionRepo.startNewSession("album1", "Album 1")
        fakeSessionRepo.recordDecision(1L, "uri1", DecisionType.KEEP, "album1", session1.sessionId)
        fakeSessionRepo.recordDecision(2L, "uri2", DecisionType.TRASH_PENDING, "album1", session1.sessionId)

        val session2 = fakeSessionRepo.startNewSession("album2", "Album 2")
        fakeSessionRepo.recordDecision(3L, "uri3", DecisionType.KEEP, "album2", session2.sessionId)

        fakeSessionRepo.discardSessionById(session1.sessionId)

        // Session 1 decisions gone
        assertEquals(null, fakeSessionRepo.getDecision(1L))
        assertEquals(null, fakeSessionRepo.getDecision(2L))

        // Session 2 decision intact
        assertNotNull(fakeSessionRepo.getDecision(3L))
    }

    @Test
    fun testPersistentReviewCheckpoint_5000Items_review700_finish_thenContinue_startsAt701() = runBlocking {
        // Step 1: Create deterministic 5,000 item collection (IDs 1L..5000L)
        val allPhotos = (1L..5000L).map { id ->
            createMediaItem(id, "IMG_$id.jpg")
        }
        fakeMediaRepo.photos = allPhotos

        // Step 2: Start cleaning session
        val session = fakeSessionRepo.startNewSession(null, "All Photos")
        assertEquals(SessionStatus.ACTIVE.name, session.status)

        // Step 3: Review 700 items (500 Keep, 200 Trash)
        // Photo 1..500 -> KEEP
        for (id in 1L..500L) {
            fakeSessionRepo.recordDecision(id, "content://media/$id", DecisionType.KEEP, null, session.sessionId)
        }
        // Photo 501..700 -> TRASH_PENDING
        for (id in 501L..700L) {
            fakeSessionRepo.recordDecision(id, "content://media/$id", DecisionType.TRASH_PENDING, null, session.sessionId)
        }

        // Step 4: User taps "Finish" (Apply & Finish)
        // 200 Trash items committed to Android Trash
        val pendingTrash = fakeSessionRepo.getPendingTrashDecisionsForSession(session.sessionId)
        assertEquals(200, pendingTrash.size)
        fakeSessionRepo.markBatchAsTrashed(pendingTrash.map { it.mediaStoreId })

        // 4,300 photos remain unreviewed in the source!
        // Session must be PAUSED, preserving the review checkpoint
        fakeSessionRepo.pauseSession(session.sessionId)

        val pausedSession = fakeSessionRepo.getResumableSession(null)
        assertNotNull(pausedSession)
        assertEquals(SessionStatus.PAUSED.name, pausedSession?.status)
        assertEquals(session.sessionId, pausedSession?.sessionId)

        // Step 5: Verify review checkpoint
        val totalReviewed = fakeSessionRepo.getTotalReviewedCountForSession(session.sessionId)
        assertEquals(700, totalReviewed)

        val reviewedIds = fakeSessionRepo.getReviewedIdsForSession(session.sessionId)
        assertEquals(700, reviewedIds.size)

        // Step 6: User returns later and selects "Continue Cleaning"
        // Active session is resumed
        val resumedSession = fakeSessionRepo.getOrCreateActiveSession(null, "All Photos")
        assertEquals(session.sessionId, resumedSession.sessionId)

        // MediaStore now has 4,800 items (since 200 were deleted)
        val remainingInMediaStore = allPhotos.filter { it.id !in (501L..700L) }
        assertEquals(4800, remainingInMediaStore.size)

        // Filter against persisted review checkpoint
        val unreviewed = remainingInMediaStore.filter { it.id !in reviewedIds }

        // Must have exactly 4,300 items remaining
        assertEquals(4300, unreviewed.size)

        // CRITICAL CHECK: First unreviewed photo is photo 701!
        assertEquals(701L, unreviewed.first().id)

        // None of the 500 Kept photos appear again
        val keptIds = (1L..500L).toSet()
        assertTrue(unreviewed.none { it.id in keptIds })
    }

    @Test
    fun testFinish_doesNotMarkSessionCompleted_whenUnreviewedPhotosRemain() = runBlocking {
        val session = fakeSessionRepo.startNewSession("vacation", "Vacation")
        fakeSessionRepo.recordDecision(1L, "uri1", DecisionType.KEEP, "vacation", session.sessionId)
        fakeSessionRepo.recordDecision(2L, "uri2", DecisionType.TRASH_PENDING, "vacation", session.sessionId)

        // User finishes early with photos remaining
        fakeSessionRepo.markBatchAsTrashed(listOf(2L))
        fakeSessionRepo.pauseSession(session.sessionId)

        // Session must still be resumable
        val resumable = fakeSessionRepo.getResumableSession("vacation")
        assertNotNull(resumable)
        assertEquals(SessionStatus.PAUSED.name, resumable?.status)
    }

    @Test
    fun testFinish_marksSessionCompleted_onlyWhenAllPhotosReviewed() = runBlocking {
        val session = fakeSessionRepo.startNewSession("vacation", "Vacation")
        fakeSessionRepo.recordDecision(1L, "uri1", DecisionType.KEEP, "vacation", session.sessionId)
        fakeSessionRepo.recordDecision(2L, "uri2", DecisionType.TRASH_PENDING, "vacation", session.sessionId)

        // All photos in scope reviewed (deck empty)
        fakeSessionRepo.markBatchAsTrashed(listOf(2L))
        fakeSessionRepo.completeSession(session.sessionId)

        // Session must NOT be resumable
        val resumable = fakeSessionRepo.getResumableSession("vacation")
        assertEquals(null, resumable)
    }

    @Test
    fun testStartNewSession_explicitChoice_startsFreshPass_fromItem1() = runBlocking {
        // Session 1 reviews 50 photos
        val session1 = fakeSessionRepo.startNewSession("albumA", "Album A")
        for (i in 1L..50L) {
            fakeSessionRepo.recordDecision(i, "uri$i", DecisionType.KEEP, "albumA", session1.sessionId)
        }
        fakeSessionRepo.pauseSession(session1.sessionId)

        // User explicitly chooses "Start Fresh"
        val session2 = fakeSessionRepo.startNewSession("albumA", "Album A")
        assertTrue(session1.sessionId != session2.sessionId)

        val reviewedSession2 = fakeSessionRepo.getReviewedIdsForSession(session2.sessionId)
        assertEquals(0, reviewedSession2.size)
        assertEquals(0, fakeSessionRepo.getTotalReviewedCountForSession(session2.sessionId))
    }

    @Test
    fun testAdaptiveSizeCalculation_landscape_portrait_square_tall() {
        val stageWidth = 360.dp
        val stageHeight = 480.dp

        // Landscape 16:9 (1920x1080)
        val landscapeSize = calculateAdaptiveSize(
            mediaWidth = 1920,
            mediaHeight = 1080,
            maxStageWidth = stageWidth,
            maxStageHeight = stageHeight
        )
        assertEquals(stageWidth, landscapeSize.width)
        assertTrue(landscapeSize.height < stageHeight)
        // ~202.5dp height
        assertTrue(landscapeSize.height.value in 200f..205f)

        // Portrait 3:4 (1200x1600) -> same aspect as 360x480 stage (0.75)
        val portraitSize = calculateAdaptiveSize(
            mediaWidth = 1200,
            mediaHeight = 1600,
            maxStageWidth = stageWidth,
            maxStageHeight = stageHeight
        )
        assertEquals(stageWidth, portraitSize.width)
        assertEquals(stageHeight, portraitSize.height)

        // Tall Portrait 9:16 (1080x1920)
        val tallSize = calculateAdaptiveSize(
            mediaWidth = 1080,
            mediaHeight = 1920,
            maxStageWidth = stageWidth,
            maxStageHeight = stageHeight
        )
        assertEquals(stageHeight, tallSize.height)
        assertTrue(tallSize.width < stageWidth)
        // ~270dp width
        assertTrue(tallSize.width.value in 268f..272f)

        // Square 1:1 (1000x1000)
        val squareSize = calculateAdaptiveSize(
            mediaWidth = 1000,
            mediaHeight = 1000,
            maxStageWidth = stageWidth,
            maxStageHeight = stageHeight
        )
        assertEquals(stageWidth, squareSize.width)
        assertEquals(stageWidth, squareSize.height)
    }

    private fun createMediaItem(id: Long, name: String): MediaItem {
        return MediaItem(
            id = id,
            contentUri = FakeUri("content://media/test/$id"),
            displayName = name,
            mimeType = "image/jpeg",
            dateAdded = 1000L,
            dateTaken = 1000L,
            dateModified = 1000L,
            size = 1024L,
            width = 1920,
            height = 1080,
            bucketId = "camera",
            bucketDisplayName = "Camera",
            mediaType = MediaType.PHOTO
        )
    }
}

// In-Memory Fake Repositories for Unit Testing
class FakeMediaRepository : MediaRepository {
    var photos = listOf<MediaItem>()
    var trashed = listOf<MediaItem>()
    var trashedMediaItemsCalledCount = 0

    override suspend fun getPhotos(
        bucketId: String?,
        limit: Int,
        offset: Int,
        filter: `in`.heyvinay.swipepix.data.model.GalleryFilterCategory,
    ): Result<List<MediaItem>> {
        return Result.success(photos)
    }

    override suspend fun getAlbums(): Result<List<Album>> = Result.success(emptyList())

    override suspend fun getMediaCount(bucketId: String?): Result<`in`.heyvinay.swipepix.data.model.AlbumMediaCount> {
        val filtered = if (bucketId != null) photos.filter { it.bucketId == bucketId } else photos
        val pCount = filtered.count { it.mediaType == `in`.heyvinay.swipepix.data.model.MediaType.PHOTO }
        val vCount = filtered.count { it.mediaType == `in`.heyvinay.swipepix.data.model.MediaType.VIDEO }
        return Result.success(`in`.heyvinay.swipepix.data.model.AlbumMediaCount(totalCount = filtered.size, photoCount = pCount, videoCount = vCount))
    }

    override suspend fun getPhotoById(id: Long): Result<MediaItem?> {
        return Result.success(photos.firstOrNull { it.id == id })
    }

    override fun observeChanges(): Flow<Unit> = flowOf(Unit)

    override suspend fun trashMediaItem(mediaItem: MediaItem): android.content.IntentSender? = null

    override suspend fun trashMediaItems(mediaItems: List<MediaItem>): android.content.IntentSender? {
        trashedMediaItemsCalledCount++
        return null
    }

    override suspend fun getTrashedMedia(): Result<List<MediaItem>> = Result.success(trashed)

    override suspend fun restoreMediaItems(mediaItems: List<MediaItem>): android.content.IntentSender? = null

    override suspend fun deleteMediaItemsPermanently(mediaItems: List<MediaItem>): android.content.IntentSender? = null

    override suspend fun setFavorite(mediaItems: List<MediaItem>, isFavorite: Boolean): android.content.IntentSender? = null
}

class FakeCleanupSessionRepository : CleanupSessionRepository {
    private val decisions = mutableMapOf<Long, PhotoDecisionEntity>()
    private val sessions = mutableMapOf<String, CleaningSessionEntity>()
    private var activeSession: CleaningSessionEntity? = null

    override suspend fun getResumableSession(albumId: String?): CleaningSessionEntity? {
        return sessions.values.firstOrNull {
            it.status in listOf(SessionStatus.ACTIVE.name, SessionStatus.PAUSED.name) &&
                (if (albumId == null) it.albumId == null else it.albumId == albumId)
        }
    }

    override suspend fun startNewSession(albumId: String?, albumName: String?): CleaningSessionEntity {
        // Discard existing resumable session if any
        val existing = getResumableSession(albumId)
        if (existing != null) {
            discardSessionById(existing.sessionId)
        }

        val newSession = CleaningSessionEntity(
            sessionId = "session-${System.currentTimeMillis()}-${(1..1000).random()}",
            albumId = albumId,
            albumName = albumName,
            status = SessionStatus.ACTIVE.name
        )
        sessions[newSession.sessionId] = newSession
        activeSession = newSession
        return newSession
    }

    override suspend fun resumeSession(sessionId: String): CleaningSessionEntity? {
        val session = sessions[sessionId]?.copy(status = SessionStatus.ACTIVE.name)
        if (session != null) {
            sessions[sessionId] = session
            activeSession = session
        }
        return session
    }

    override suspend fun getOrCreateActiveSession(albumId: String?, albumName: String?): CleaningSessionEntity {
        val resumable = getResumableSession(albumId)
        if (resumable != null) {
            val activated = resumable.copy(status = SessionStatus.ACTIVE.name)
            sessions[activated.sessionId] = activated
            activeSession = activated
            return activated
        }
        return startNewSession(albumId, albumName)
    }

    override suspend fun recordDecision(
        mediaStoreId: Long,
        contentUri: String,
        decision: DecisionType,
        albumId: String?,
        sessionId: String
    ) {
        decisions[mediaStoreId] = PhotoDecisionEntity(
            mediaStoreId = mediaStoreId,
            decision = decision.name,
            contentUri = contentUri,
            albumId = albumId,
            sessionId = sessionId
        )
    }

    override suspend fun undoDecision(mediaStoreId: Long) {
        decisions.remove(mediaStoreId)
    }

    override suspend fun getReviewedIds(albumId: String?): Set<Long> {
        return decisions.values
            .filter { if (albumId == null) it.albumId == null else it.albumId == albumId }
            .map { it.mediaStoreId }
            .toSet()
    }

    override suspend fun getReviewedIdsForSession(sessionId: String): Set<Long> {
        return decisions.values
            .filter { it.sessionId == sessionId }
            .map { it.mediaStoreId }
            .toSet()
    }

    override suspend fun getPendingTrashDecisions(albumId: String?): List<PhotoDecisionEntity> {
        return decisions.values.filter {
            it.decision == DecisionType.TRASH_PENDING.name &&
                (if (albumId == null) it.albumId == null else it.albumId == albumId)
        }
    }

    override suspend fun getPendingTrashDecisionsForSession(sessionId: String): List<PhotoDecisionEntity> {
        return decisions.values.filter {
            it.decision == DecisionType.TRASH_PENDING.name && it.sessionId == sessionId
        }
    }

    override suspend fun markBatchAsTrashed(mediaStoreIds: List<Long>) {
        mediaStoreIds.forEach { id ->
            decisions[id]?.let { existing ->
                decisions[id] = existing.copy(decision = DecisionType.TRASHED.name)
            }
        }
    }

    override suspend fun getDecision(mediaStoreId: Long): PhotoDecisionEntity? {
        return decisions[mediaStoreId]
    }

    override fun observeTrashPendingCount(): Flow<Int> {
        return flowOf(decisions.values.count { it.decision == DecisionType.TRASH_PENDING.name })
    }

    override fun observeKeptCount(): Flow<Int> {
        return flowOf(decisions.values.count { it.decision == DecisionType.KEEP.name })
    }

    override suspend fun getSummaryCounts(albumId: String?): Pair<Int, Int> {
        val filtered = decisions.values.filter {
            if (albumId == null) it.albumId == null else it.albumId == albumId
        }
        val kept = filtered.count { it.decision == DecisionType.KEEP.name }
        val trashed = filtered.count { it.decision == DecisionType.TRASH_PENDING.name }
        return Pair(kept, trashed)
    }

    override suspend fun getSummaryCountsForSession(sessionId: String): Pair<Int, Int> {
        val filtered = decisions.values.filter { it.sessionId == sessionId }
        val kept = filtered.count { it.decision == DecisionType.KEEP.name }
        val trashed = filtered.count { it.decision == DecisionType.TRASH_PENDING.name }
        return Pair(kept, trashed)
    }

    override suspend fun getTotalReviewedCountForSession(sessionId: String): Int {
        return decisions.values.count { it.sessionId == sessionId }
    }

    override suspend fun getTotalReviewedCount(albumId: String?): Int {
        return decisions.values.count {
            if (albumId == null) it.albumId == null else it.albumId == albumId
        }
    }

    override suspend fun pauseSession(sessionId: String) {
        sessions[sessionId]?.let {
            val paused = it.copy(status = SessionStatus.PAUSED.name)
            sessions[sessionId] = paused
            if (activeSession?.sessionId == sessionId) activeSession = paused
        }
    }

    override suspend fun completeSession(sessionId: String) {
        sessions[sessionId]?.let {
            val completed = it.copy(status = SessionStatus.COMPLETED.name)
            sessions[sessionId] = completed
            if (activeSession?.sessionId == sessionId) activeSession = null
        }
    }

    override suspend fun discardSession(albumId: String?) {
        val currSession = activeSession
        val toRemove = if (currSession != null) {
            decisions.filter { (_, v) ->
                v.sessionId == currSession.sessionId && v.decision != DecisionType.TRASHED.name
            }.keys
        } else {
            decisions.filter { (_, v) ->
                (if (albumId == null) v.albumId == null else v.albumId == albumId) && v.decision != DecisionType.TRASHED.name
            }.keys
        }
        toRemove.forEach { decisions.remove(it) }
        currSession?.let { sessions.remove(it.sessionId) }
        activeSession = null
    }

    override suspend fun discardSessionById(sessionId: String) {
        val toRemove = decisions.filter { (_, v) ->
            v.sessionId == sessionId && v.decision != DecisionType.TRASHED.name
        }.keys
        toRemove.forEach { decisions.remove(it) }
        sessions.remove(sessionId)
        if (activeSession?.sessionId == sessionId) activeSession = null
    }
}
