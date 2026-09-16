# SwipePix — GSD State Machine

## CURRENT_PHASE
PHASE 47 — Unified Video Player Architecture (`SwipePixVideoPlayer`) + In-Deck Cleaning Playback + External Player Fallback

## STATUS
COMPLETE & VERIFIED (Automated Test Suite: 104/104 Passing across 17 test suites, Clean Release Build signed with 4096-bit RSA Key, Live Physical Device Samsung Galaxy S24 Ultra `RZCY816ZY1M` Verification: PASSED)

## PROBLEMS_ADDRESSED
1. **PROBLEM 1 — Gallery Lacked Multi-Selection & Batch Trash Operations**:
   - Users previously had to browse photos or swipe one by one; there was no way to select multiple items from the main gallery or album view to delete or manage them together.
   - Solution:
     - Implemented long-press trigger using `Modifier.combinedClickable(onClick = ..., onLongClick = ...)` on grid items.
     - Implemented full selection mode state (`isSelectionMode`, `selectedIds: Set<Long>`) across `GalleryViewModel` and `ViewPhotosViewModel`.
     - Added visual selection indicators: circular checkmark badge at `Alignment.TopEnd`, 2dp primary border, primary tinted scrim overlay, and subtle scale-down animation (`0.93f`).
     - Animated top selection bar (`Close` button, count indicator e.g. "2 selected", `Select all` / `Deselect all` toggle, and red `Trash` action button).
     - Floating bottom navigation bar (`SwipePixBottomBar`) automatically hides when selection mode is active.
     - Android back press intercepts selection mode to clear selection before exiting screen.
     - Safe deletion workflow: preliminary in-app confirmation dialog (`AlertDialog`) -> Android OS `MediaStore.createTrashRequest` prompt -> Undo `Snackbar` ("X items moved to trash" with "Undo" action).

2. **PROBLEM 2 — Photo Viewer UX & Aspect-Ratio Framing**:
   - The photo viewer previously lacked clean aspect ratio adaptation, subtle border framing, smooth zoom handling, and safe deletion options with advance-to-next semantics.
   - Solution:
     - Implemented two-level layout in `PhotoViewerScreen`: Level 1: Stable viewer chrome and bounded stage; Level 2: Adaptive media surface deriving dimensions from native aspect ratio (`mediaWidth` / `mediaHeight`).
     - Adaptive 16dp rounded corners at resting zoom (`1f`), dynamically animating to `0dp` as zoom scales up (`zoomScale > 1.05f`).
     - Subtle 1dp border (`Color.White.copy(alpha = 0.12f)`) framing media at resting scale.
     - Zoom gestures: Unconstrained pinch-to-zoom (up to 5x), double-tap toggle (1f <-> 2.5f), pan constraints to stage bounds.
     - Video support: hardware-extracted thumbnail, central play badge, duration indicator pill.
     - Viewer Chrome: Floating top bar with Back arrow and red Trash action; bottom metadata bar displaying date/time, dimensions (e.g. `1080 × 2280`), file size (e.g. `218 kB`), and filename.
     - Safe In-Viewer Deletion: Trash button opens preliminary confirmation dialog -> Android OS trash prompt -> item is removed from the list and viewer advances smoothly to the next photo; if 0 items remain, smoothly pops back to the gallery. Undo snackbar restores the photo if tapped.

3. **PROBLEM 3 — Swipe Cleaning Architecture Isolation**:
   - Guarantee: Manual deletions and multi-select trashing from the gallery or image viewer must NEVER touch Room `SwipeSessionDao`, `SwipeDecisionDao`, or `session_progress`.
   - Verified: Review checkpoints and paused session states created in Phase 45 remain 100% intact and unaffected when photos are trashed or restored via the gallery.

## RESOLUTIONS_IMPLEMENTED
1. `app/src/main/java/in/heyvinay/swipepix/ui/gallery/components/PhotoGrid.kt`:
   - Added `@OptIn(ExperimentalFoundationApi::class) combinedClickable` support.
   - Added top-end selection badge, 2dp border, primary tinted scrim overlay, and 0.93f scale animation.
2. `app/src/main/java/in/heyvinay/swipepix/ui/gallery/GalleryViewModel.kt`:
   - Added `selectedIds: Set<Long>`, `isSelectionMode: Boolean`, `totalLoaded: Int` to `GalleryUiState.Content`.
   - Implemented `enterSelection`, `toggleSelection`, `toggleSelectAll`, `clearSelection`, `createTrashRequestForSelected`, `onTrashOperationSuccess`, `createUndoTrashRequest`, `onUndoOperationSuccess`.
3. `app/src/main/java/in/heyvinay/swipepix/ui/gallery/MainGalleryShell.kt`:
   - Wired selection mode: Animated top selection bar, back button interception, floating bar hide/show, preliminary delete dialog, `trashLauncher` and `undoLauncher` with Undo snackbar.
4. `app/src/main/java/in/heyvinay/swipepix/ui/albums/ViewPhotosViewModel.kt` & `ViewPhotosScreen.kt`:
   - Added selection mode, batch deletion, and undo restoration to album viewer.
5. `app/src/main/java/in/heyvinay/swipepix/ui/viewer/ZoomableImage.kt`:
   - Added aspect-ratio aware sizing, dynamic corner radius (16dp -> 0dp), subtle border, double-tap zoom/reset, pinch-to-zoom up to 5x, and video badge overlay.
6. `app/src/main/java/in/heyvinay/swipepix/ui/viewer/PhotoViewerScreen.kt` & `PhotoViewerViewModel.kt`:
   - Redesigned two-level layout, top bar with Back & Delete, bottom metadata bar, advance-to-next on delete, auto-exit when empty, and undo restoration.
7. `app/src/test/java/in/heyvinay/swipepix/ui/gallery/GallerySelectionTest.kt`:
   - 6 unit tests: selection toggle, select all/none, batch trash intent generation, filter change selection clearance.
8. `app/src/test/java/in/heyvinay/swipepix/ui/viewer/PhotoViewerDeleteTest.kt`:
   - 4 unit tests: initial index preservation, item removal & index adjustment, auto-exit flag when list emptied, undo reinsertion.

## TEST_STATUS
- 69/69 Unit Tests Passing (100% pass rate across 13 test suites):
  - `GallerySelectionTest`: 6/6 passed (NEW)
  - `PhotoViewerDeleteTest`: 4/4 passed (NEW)
  - `CleanupSessionTest`: 18/18 passed
  - `CleanupSourceSelectionViewModelTest`: 4/4 passed
  - `GalleryViewModelTest`: 5/5 passed
  - `DateUtilsTest`: 9/9 passed
  - `SwipePixWebsiteConfigTest`: 7/7 passed
  - `MediaItemTest`: 4/4 passed
  - `TrashViewModelTest`: 3/3 passed
  - `SwipePixMotionTest`: 3/3 passed
  - `PermissionStateTest`: 3/3 passed
  - `NavigationChromeTest`: 2/2 passed
  - `ExampleUnitTest`: 1/1 passed
- `compileDebugKotlin`: PASS
- `testDebugUnitTest`: PASS (BUILD SUCCESSFUL)

## ON_DEVICE_VERIFICATION_STATUS
- Verified on Android device `emulator-5554` and captured verification artifacts:
  - `screen_phase46_gallery2.png`: Photos grid at resting state.
  - `screen_phase46_selection_mode.png`: Long press activates selection mode; top bar with "1 selected", circular checkmark badge, 2dp primary border, bottom bar hidden.
  - `screen_phase46_multi_selected.png` & `screen_phase46_3_selected.png`: Multi-selection across grid items with dynamic count updates.
  - `screen_phase46_viewer_chrome.png`: Redesigned two-level photo viewer with adaptive aspect-ratio frame, 16dp rounded corners, subtle border, top chrome (Back + Delete), and bottom metadata bar.
  - `screen_phase46_zoomed.png`: Double-tap zoom to 2.5x with dynamic corner radius transitioning to 0dp.
  - `screen_phase46_reset_zoom.png`: Double-tap reset returning to 1f resting frame and 16dp corner radius.
  - `screen_phase46_viewer_delete_dialog.png`: Preliminary confirmation dialog in viewer before deletion.
  - `screen_phase46_android_os_trash_dialog.png`: Android OS `MediaStore.createTrashRequest` system confirmation prompt ("Allow SwipePix to move this photo to trash?").
  - `screen_phase46_viewer_trashed.png`: Trashed item removed, viewer smoothly advanced to next photo, Undo snackbar displayed.
  - `screen_phase46_batch_selected.png`: Multiple photos selected in gallery grid.
  - `screen_phase46_batch_dialog2.png`: Preliminary batch delete confirmation dialog ("Move 2 items to Android Trash?").
  - `screen_phase46_batch_os_dialog.png`: System OS batch trash confirmation dialog displaying thumbnail previews.
  - `screen_phase46_batch_trashed.png`: Grid updated with items removed, selection mode exited, bottom navigation bar restored.
  - `screen_phase46_cleanup_source.png` & `screen_phase46_cleanup_resume_dialog.png` & `screen_phase46_resumed_clean.png`: Verification that Swipe Cleaning session checkpoint (`1 / 27`, `26 remaining · 1 reviewed`) remained 100% intact and unaffected by gallery manual deletions.

## P0_SECURITY_AUDIT_PLAY_PROTECT_REMEDIATION
- **DATE**: 2026-09-16
- **OBSERVED ISSUE**: Physical Samsung Galaxy S24 Ultra (Android 14/15 One UI 6.1) blocked installation via Google Play Protect:
  - Header: `Google Play Protect — Harmful app blocked — SwipePix`
  - Message: `“This app can install potentially harmful apps without your permission.”`
- **POTENTIALLY HARMFUL APPLICATION (PHA) CLASSIFICATION**:
  - Google Play Protect official category: **Hostile Downloader** / Dropper.
- **ROOT CAUSE ANALYSIS**:
  1. **Release APK Signed with Shared Public Debug Key**:
     - `app/build.gradle.kts` previously configured `buildTypes.release.signingConfig = signingConfigs.getByName("debug")`.
     - The release APK has `isMinifyEnabled = true`, `isShrinkResources = true`, and `android:debuggable="false"`.
     - When an APK with production release flags (`debuggable="false"`) and R8 obfuscated classes is signed with the public `CN=Android Debug` certificate (`e6625697b4f5114e1880f611ee2a8d161818eb554069e2374392044d4b22022c`), Play Protect and Samsung Auto Blocker cloud heuristics classify it as an untrusted dropper/malware because malware authors frequently use debug keys to evade identity attribution.
  2. **Absence of Any Malicious Capability**:
     - Manifest audit: `android.permission.INTERNET` is completely absent. Zero network sockets can be opened.
     - `REQUEST_INSTALL_PACKAGES` and `INSTALL_PACKAGES` are completely absent.
     - Zero dynamic code loading (`DexClassLoader`, `PathClassLoader`).
     - Zero process execution (`Runtime.getRuntime()`, `ProcessBuilder`).
     - Zero APK handling, download managers, or file outputs.
- **REMEDIATIONS IMPLEMENTED**:
  1. **Dedicated Release Keystore**:
     - Generated dedicated RSA 4096-bit release keystore: `keystore/swipepix-release.jks`.
     - DN: `CN=SwipePix, OU=Mobile, O=HeyVinay, L=Bengaluru, ST=Karnataka, C=IN`.
     - SHA-256 Digest: `48:31:9B:8D:60:F1:9A:9D:9F:D2:4D:A8:3B:D8:EE:C6:42:38:86:50:DB:A8:B1:A1:F9:31:13:32:FA:25:52:02`.
     - Algorithm: SHA384withRSA, validity 10,000 days (until 2054).
     - Credentials safely encapsulated in `keystore.properties` (added to `.gitignore`).
  2. **Gradle Release Signing Configuration**:
     - Configured `signingConfigs.create("release")` in `app/build.gradle.kts` reading from `keystore.properties` with fallback support.
     - Updated `buildTypes.release.signingConfig = signingConfigs.getByName("release")`.
  3. **R8 Proguard Keep Rules**:
     - Added `-dontwarn in.heyvinay.swipepix.Hilt_*` to `app/proguard-rules.pro` ensuring seamless R8 bytecode shrinking.
- **TEST & BUILD STATUS**:
  - `testDebugUnitTest`: 69/69 passed across 13 test suites (100%).
  - `assembleRelease`: BUILD SUCCESSFUL.
  - `assembleDebug`: BUILD SUCCESSFUL.
  - `apksigner verify --print-certs`: Confirmed Signer #1 DN: `CN=SwipePix, OU=Mobile, O=HeyVinay, L=Bengaluru, ST=Karnataka, C=IN`.
- **LIVE PHYSICAL DEVICE VERIFICATION (Samsung Galaxy S24 Ultra — SM-S928B / Serial RZCY816ZY1M)**:
  - Uninstalled old debug-signed app from device.
  - Installed newly signed release APK (`app-release.apk`) via ADB streamed install: `Success`.
  - Triggered Google Play Protect full device scan (`com.google.android.finsky.protect.impl.PlayProtectHomeDeepLinkActivity`):
    - **RESULT**: **"No harmful apps found"** with green checkmark shield.
    - Verified SwipePix (`in.heyvinay.swipepix`) scanned at the top of recently scanned apps with 0 security risks.
  - Launched SwipePix: Welcome screen and Photos gallery rendered flawlessly with 3,026 photos and 274 videos.
  - Artifacts captured: `physical_playprotect_clean.png`, `physical_swipepix_welcome.png`, `physical_gallery_live.png`.

## P1_ADAPTIVE_SWIPE_MEDIA_FRAME_RESOLUTION
- **DATE**: 2026-09-16
- **OBSERVED ISSUE**: On the swipe cleaning screen (`CleanupScreen.kt`), the visible rounded frame did not match the actual image bounds for portrait photos (e.g. portrait photo of a woman holding a shirt was displayed inside a wide 4:3 landscape card with dark pillarboxing on the sides inside the 24dp rounded border).
- **ROOT CAUSE ANALYSIS**:
  1. `MediaStoreDataSource.kt` queried `WIDTH` and `HEIGHT` but omitted `MediaStore.MediaColumns.ORIENTATION`. Camera sensors capture raw photos in sensor landscape dimensions (e.g. 4000x3000) with EXIF `ORIENTATION = 90`.
  2. `CleanupScreen.kt` passed raw `width` and `height` to `calculateAdaptiveSize`, producing a wide landscape card (360x270dp).
  3. Coil 3 decoded the photo and rotated it 90 degrees to portrait (3000x4000). `AsyncImage` with `ContentScale.Fit` centered the portrait photo inside the wide landscape card, leaving dark letterbox/pillarbox space inside the card border.
  4. `AdaptivePhotoCard` wrapped `AsyncImage` in a separate `Box` with background `#0F1219` and border, creating an outer frame distinct from the image.
- **REMEDIATIONS IMPLEMENTED**:
  1. **EXIF-Aware Data Model (`MediaItem.kt`)**:
     - Added `val orientation: Int = 0`.
     - Added `displayedWidth`: swaps width and height when `orientation == 90 || orientation == 270`.
     - Added `displayedHeight`: swaps height and width when `orientation == 90 || orientation == 270`.
     - Added `displayedAspectRatio`: derives true visual aspect ratio (`displayedWidth.toFloat() / displayedHeight.toFloat()`, fallback `0.75f`).
  2. **MediaStore Data Source Query (`MediaStoreDataSource.kt`)**:
     - Added `MediaStore.MediaColumns.ORIENTATION` to `mediaProjection`.
     - Populated `orientation` in `queryPhotos`, `queryMediaItemById`, and `queryTrashedMedia`.
  3. **Unified Adaptive Media Surface (`AdaptiveMediaSurface.kt`)**:
     - Architecture: `FRAME SIZE == IMAGE SURFACE SIZE` — zero letterbox, zero pillarbox, border and 24dp rounded corners directly hug the media boundary.
     - Implemented `calculateAdaptiveMediaSize(aspectRatio: Float, maxStageWidth: Dp, maxStageHeight: Dp): DpSize` with clamping (`0.35f..2.8f`).
     - Added compatibility overload `calculateAdaptiveSize(mediaWidth, mediaHeight, maxStageWidth, maxStageHeight)`.
     - Implemented `AdaptiveMediaSurface` composable:
       - Sized directly from `effectiveAspectRatio` (derived from `MediaItem.displayedAspectRatio` on initial frame).
       - Added fallback listener via Coil `AsyncImagePainter.State.Success` `painter.intrinsicSize` for photos with missing/zero dimensions in MediaStore.
       - Clipped with `AdaptiveMediaShape = RoundedCornerShape(24.dp)` and bordered with 1dp subtle border.
       - Uses `ContentScale.Crop` inside the exact aspect-ratio surface so the image fills 100% of the rounded frame with zero gaps.
       - Overlays video play badge & duration pill when `MediaType.VIDEO`.
  4. **Cleanup Screen Integration (`CleanupScreen.kt`)**:
     - Integrated `AdaptiveMediaSurface` for Card 1 (Interactive) and Card 2 (Middle pre-rendered).
     - Retained Level 1 Stable Media Stage (`BoxWithConstraints` with fixed flex weight) ensuring stable UI layout and full-stage swipe touch target.
  5. **Photo Viewer Aspect Handling (`ZoomableImage.kt`)**:
      - Updated aspect ratio derivation to use `displayedWidth`, `displayedHeight`, and `displayedAspectRatio`.
- **TEST & BUILD STATUS**:
  - `AdaptiveMediaSurfaceTest`: 10/10 unit tests passed (testing 16:9, 3:4, 9:16, 1:1, extreme wide 5.0, extreme tall 0.1, fallback 0, and orientation 90/180/270 swapping).
  - Total Unit Test Suite: **79/79 passed** (100% pass rate across 14 test suites).
  - `assembleRelease`: BUILD SUCCESSFUL.
- **LIVE PHYSICAL DEVICE VERIFICATION (Samsung Galaxy S24 Ultra — SM-S928B / Serial RZCY816ZY1M)**:
  - Installed release APK on device via ADB streamed install (`Success`).
  - Tested "All Photos" swipe cleaning:
    - Session resume checkpoint preserved ("In Progress (14 reviewed)").
    - The reproduced photo of the woman holding a shirt now displays with the 24dp rounded frame perfectly hugging the portrait image boundary with **ZERO letterbox and ZERO pillarbox** (`physical_adaptive_screen5.png`).
    - Swiped / advanced to photo 15 (tall screenshot): frame perfectly hugs the tall screenshot bounds (`physical_adaptive_screen8.png`).
    - Swiped / advanced to photo 16 (3:4 photo in woods): frame perfectly hugs the photo bounds (`physical_adaptive_screen9.png`).
    - Tested Undo button: successfully restored prior reviewed item (`physical_adaptive_screen10.png`).
    - Verified layout stability: top header, progress bar, undo pill, trash and keep buttons remain firmly anchored.

## PHASE_47_UNIFIED_VIDEO_PLAYER_SYSTEM
- **DATE**: 2026-09-16
- **MISSION**: Build ONE canonical, unified video player system (`SwipePixVideoPlayer`) across the entire app:
  1. Photos (`GalleryScreen` -> `PhotoViewerScreen`)
  2. Albums (`ViewPhotosScreen` -> `PhotoViewerScreen`)
  3. Photo/Video Viewer (`PhotoViewerScreen` -> `ZoomableImage`)
  4. Cleaning Mode (`CleanupScreen` -> `AdaptiveMediaSurface`)
- **ARCHITECTURAL SPLIT ELIMINATED**:
  - Previously: Photos/Albums used external `ACTION_VIEW` intent choosers, while Cleaning mode had an isolated prototype.
  - Resolved: ONE unified video engine, ONE state model, ONE control system, ONE video surface, and ONE seek scrubber used across every screen.
- **CORE ARCHITECTURE (`in.heyvinay.swipepix.ui.video`)**:
  1. `SwipePixPlaybackState.kt`:
     - Immutable state model: `PlaybackStatus` (`IDLE`, `PREPARING`, `READY`, `PLAYING`, `PAUSED`, `BUFFERING`, `COMPLETED`, `ERROR`).
     - Progress calculation and dual time formatters (`mm:ss` for < 1hr, `hh:mm:ss` for >= 1hr).
  2. `SwipePixPlayerEngine.kt`:
     - Platform `MediaPlayer` + hardware-accelerated `TextureView` (zero 3rd-party dependencies, zero Play Protect PHA risk).
     - Audio focus management via `AudioManager` (`AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`).
     - 100ms precision coroutine ticker for real-time progress tracking.
     - Scrubbing lifecycle: freeze updates on scrub start, commit on scrub finish.
     - Robust error listener and lifecycle observer releasing resources on background or disposal.
  3. `SwipePixVideoSeekSlider.kt`:
     - Dedicated touch scrubber with 36dp touch target and animated thumb (`10dp` -> `16dp`).
     - Gesture Isolation: Uses `detectDragGestures { change, _ -> change.consume() }`, strictly preventing parent card swipes or pager flings during seeking.
  4. `SwipePixVideoControls.kt`:
     - 64dp frosted glass central action button (Play/Pause/Replay).
     - Auto-fade timer: Automatically hides controls after 3.5 seconds during active playback.
     - Bottom dock with embedded seek slider, timestamps, mute/unmute toggle, and "Open with" external player fallback button.
  5. `SwipePixVideoSurface.kt`:
     - Composable wrapping Android `TextureView` with `SurfaceTextureListener` for zero-overhead hardware video decoding.
     - Compatible with Compose clipping (`AdaptiveMediaShape = RoundedCornerShape(24.dp)`) and card rotation animations.
  6. `SwipePixVideoPlayer.kt`:
     - Master composable assembling thumbnail layer, surface, controls, loading spinner, error fallback, and lifecycle observers.
     - Supported modes: `VideoPlayerMode.VIEWER` and `VideoPlayerMode.CLEANING_DECK`.
- **SCREEN INTEGRATIONS**:
  - `AdaptiveMediaSurface.kt`: Replaced ~320 lines of isolated player code with `SwipePixVideoPlayer(mode = VideoPlayerMode.CLEANING_DECK)`.
  - `ZoomableImage.kt`: Integrated `SwipePixVideoPlayer(mode = VideoPlayerMode.VIEWER)`. Hides zoom pill on videos to eliminate UI collisions.
  - `PhotoViewerScreen.kt`: Updated header subtitle dynamically to "Video" vs "Photo".
- **TEST & BUILD STATUS**:
  - Unit Test Suite: **104/104 passed** (100% pass rate across 17 test suites, including 7 new tests in `SwipePixPlaybackStateTest`).
  - `assembleRelease`: BUILD SUCCESSFUL with R8 minification and resource shrinking.
  - APK signature verified with `apksigner`: Signer #1 DN: `CN=SwipePix, OU=Mobile, O=HeyVinay, L=Bengaluru, ST=Karnataka, C=IN` (RSA 4096-bit key).
- **LIVE PHYSICAL DEVICE VERIFICATION (Samsung Galaxy S24 Ultra — SM-S928B / Serial RZCY816ZY1M)**:
  - Installed signed release APK via ADB streamed install (`Success`).
  - **Photos Viewer Playback**:
    - Tapped video thumbnail in gallery: opened full-screen photo/video viewer with ambient edge-to-edge depth blur (`s24u_p47_viewer_playing.png`).
    - Verified video playback: center pause button displayed, elapsed/total time (`00:06 / 00:15`) advancing smoothly.
    - Verified "Open with" external player button: tapping opened the Samsung native system chooser with Video Player, Google Photos, VLC, MX Player (`s24u_p47_openwith_system_chooser.png`).
    - Verified pager navigation and chrome toggle without gesture conflicts.
  - **Cleaning Deck Playback & Swipe Lifecycle**:
    - Navigated to Clean Up -> Selected "All Photos" deck (`s24u_cleanup_sources.png`).
    - Card 2 (video `0:02`): Rendered in-deck video with 24dp rounded corners, zero letterbox, and zero pillarbox.
    - Tapped center play button: video started playing directly inside the cleaning deck (`s24u_p47_cleaning_deck_playing.png`).
    - Tapped "Open with": system chooser opened directly from the deck and automatically paused playback (`s24u_p47_cleaning_deck_openwith.png`).
    - Swiped right (Keep): video card smoothly flung away, player released immediately without audio overlap, counter advanced to `2 / 160` (`s24u_p47_cleaning_deck_swiped_keep.png`).
    - Tapped Undo: previous video card returned smoothly to top of deck with "Photo restored" feedback pill (`s24u_p47_cleaning_deck_undo.png`).
    - Swiped left (Trash): video card smoothly flung away to trash pending.
    - Tapped Back -> "Save & Exit": session checkpoint safely preserved in Room database and user smoothly returned to gallery (`s24u_back_source.png`).
- **DATABASE & ARCHITECTURE SAFETY**:
  - In-deck video playback, seeking, scrubbing, pausing, and "Open with" fallback operations NEVER write to `photo_decisions` or modify Room session progress.
  - Full isolation between playback state and cleanup decision state confirmed.

