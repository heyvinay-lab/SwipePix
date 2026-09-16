# Implementation Plan — P0/P1 Swipe Experience + Session Lifecycle + Full Preview

Fix 4 critical bugs/UX gaps in the SwipePix cleanup experience:
1. **BUG 1 — Photo vs. Video Differentiation**: Distinct video styling, real video thumbnail, clear play/video badge, duration label.
2. **BUG 2 — Complete Image Fit**: Eliminate silent cropping in swipe cards by utilizing `ContentScale.Fit` with letterbox padding.
3. **NEW UX — Full Preview**: Dedicated inspection action supporting fullscreen zoom/pan and video playback, returning to the exact same card with zero session modification.
4. **BUG 3 — Session Continuation & Lifecycle**: Ensure completed sessions are marked `COMPLETED` and never offer stale resume; paused sessions resume from the first unreviewed item; "Start New Session" isolates decisions with a fresh ID.
5. **BUG 4 — Swipe Physics, Directional Integrity & Flash Elimination**: Fix velocity direction mismatch; snap offset before completion; progressive thumbnail placeholder to eliminate white/blank flashes.

---

## User Review Required

> [!IMPORTANT]
> **Full Preview UX**:
> An accessible "Full Preview" action chip with `Icons.Default.OpenInFull` is placed at the top-end of the active card. Tapping it opens an immersive, fullscreen zoomable preview. It does not advance the session or modify decisions.

> [!NOTE]
> **Primary Card Image Scale**:
> Changing from `ContentScale.Crop` to `ContentScale.Fit` ensures complete visibility of wide, panoramic, portrait, and landscape photos without distortion or cropping.

---

## Proposed Changes

### 1. Cleanup Screen & Swipe Cards
#### [MODIFY] [CleanupScreen.kt](file:///c:/Users/vinay/AndroidStudioProjects/SwipePix/app/src/main/java/in/heyvinay/swipepix/ui/cleanup/CleanupScreen.kt)
- Update `PhotoCard`:
  - Change `contentScale` from `ContentScale.Crop` to `ContentScale.Fit`.
  - Add progressive placeholder memory cache key (`"${mediaItem.contentUri}_256"`) to eliminate white flashes.
  - Add real video indicator: prominent Play badge, duration badge (`Videocam` icon + `formatDuration(durationMs)`), and "VIDEO" chip.
  - Add "Full Preview" tap target / chip in top-right corner.
- Add `FullPreviewDialog` / fullscreen overlay:
  - Supports zoom, pan, double-tap, video playback intent, and immediate close button.
  - BackHandler closes preview and returns to the exact same card.

### 2. Swipe Physics & Direction Integrity
#### [MODIFY] [SwipeCardPhysics.kt](file:///c:/Users/vinay/AndroidStudioProjects/SwipePix/app/src/main/java/in/heyvinay/swipepix/ui/cleanup/SwipeCardPhysics.kt)
- Fix velocity fling direction check: Only allow right-fling if `offsetX > 0` and left-fling if `offsetX < 0`.
- In `swipe()`: Reset `offsetX.snapTo(0f)`, `offsetY.snapTo(0f)`, `rotation.snapTo(0f)` before invoking `onSwipeComplete` to ensure the incoming card never inherits an offscreen coordinate.
- Remove channel dropping / conflation issues for smoother drag tracking.

### 3. Session Lifecycle & Completion
#### [MODIFY] [CleanupViewModel.kt](file:///c:/Users/vinay/AndroidStudioProjects/SwipePix/app/src/main/java/in/heyvinay/swipepix/ui/cleanup/CleanupViewModel.kt)
- Add `completeSessionWithoutTrash()` for sessions where all photos were kept (`trashedCount == 0`).
- When all photos are reviewed and no pending trash exists, mark session `COMPLETED`.
- In `loadSession()`: If resuming an existing session, verify that unreviewed items exist; if all were already reviewed, complete the session.
- Ensure preloading pre-fetches thumbnails as well as card images.

#### [MODIFY] [CleanupCoordinator.kt](file:///c:/Users/vinay/AndroidStudioProjects/SwipePix/app/src/main/java/in/heyvinay/swipepix/ui/cleanup/CleanupCoordinator.kt)
- Handle "Done" / session completion properly by invoking `completeSession` when finishing a session with 0 trash.

#### [MODIFY] [CleanupSourceSelectionViewModel.kt](file:///c:/Users/vinay/AndroidStudioProjects/SwipePix/app/src/main/java/in/heyvinay/swipepix/ui/cleanup/CleanupSourceSelectionViewModel.kt)
- Ensure a session is only marked `hasResumableSession = true` if `status IN ('ACTIVE', 'PAUSED')` AND `reviewedCount < totalPhotos`.

#### [MODIFY] [AlbumDetailViewModel.kt](file:///c:/Users/vinay/AndroidStudioProjects/SwipePix/app/src/main/java/in/heyvinay/swipepix/ui/albums/AlbumDetailViewModel.kt)
- Ensure `hasResumableSession = true` only when `reviewedCount < totalPhotos`.

---

## Verification Plan

### Automated Unit Tests
- Run existing and new unit tests:
  ```bash
  ./gradlew testDebugUnitTest
  ```
- Regression tests for:
  1. Photo vs. Video identification (`MediaItem.mediaType == VIDEO`)
  2. Full Preview does not alter session state or advance queue
  3. Paused session persistence & resume at first unreviewed item
  4. Completed session is not resumable
  5. Start New Session isolation
  6. Swipe physics directional integrity
- Build checks:
  ```bash
  ./gradlew compileDebugKotlin
  ./gradlew assembleDebug
  ./gradlew assembleRelease
  ```

### Physical Device / Emulator Verification
- Verify distinct video badge and duration on video cards.
- Verify portrait, landscape, square, and panorama photos are not cropped.
- Verify Full Preview opens, zooms/pans, closes, and leaves card and progress unchanged.
- Verify swiping left and right feels natural without opposite-direction flings or blank flashes.
- Verify Save & Exit pauses session; Resume continues from first unreviewed photo; finishing completes session.
