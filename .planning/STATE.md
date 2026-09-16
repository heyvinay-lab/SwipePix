# SwipePix — Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-09-14)

**Core value:** Rapidly clean hundreds or thousands of photos through an intuitive swipe interface.
**Current focus:** Phase 11 — Photos Screen Elevation & Filters

## Milestone

**v1.1 — Liquid Glass & UI/UX Elevation**
Phase: 11 of 15

## Phase Status

| Phase | Name | Status |
|-------|------|--------|
| 1 | Project Foundation | ✅ Completed |
| 2 | Permissions & Media Engine | ✅ Completed |
| 3 | Gallery UI | ✅ Completed |
| 4 | Full-Screen Viewer | ✅ Completed |
| 5 | Clean Up Mode | ✅ Completed |
| 6 | Performance Optimization | ✅ Completed |
| 7 | Reliability & Polish | ✅ Completed |
| 8 | Final Verification & Release Build | ✅ Completed |
| 9 | Liquid Glass Design Primitives | ✅ Completed |
| 10 | Floating Bottom Navigation & App Shell | ✅ Completed |
| 11 | Photos Screen Elevation & Filters | ✅ Completed |
| 12 | Swipe Physics & Animated Undo | ⏳ Pending |
| 13 | Dedicated Trash Screen & Restore | ⏳ Pending |
| 14 | Photo Viewer Polish & Actions | ⏳ Pending |
| 15 | Performance Verification & Polish | ⏳ Pending |

## Active Decisions

- Batch trash at session end (vs per-item) — ✅ **Confirmed: batch at session end**
- Videos in cleanup MVP — ✅ **Confirmed: photos-only for MVP, videos in gallery only**
- Screenshot quick-filter — ✅ **Confirmed: deferred to v2**
- Viewer Data Loading — ✅ **Confirmed: load full list of items in PhotoViewerViewModel to support arbitrary index jumping.**
- Delete Action — ✅ **Confirmed: API 30+ MediaStore.createTrashRequest for native system dialogs.**
- Cleanup Swipes & Undo — ✅ **Confirmed: spring-animated drag gesture with rotation, in-memory undo stack, single batch trash at session completion.**
- Image Preloading & Cache — ✅ **Confirmed: Coil ImageLoader tuned with 25% heap memory cache, active N+1 and N+2 cleanup preloading, EXACT thumbnail decoding precision.**
- Theme Storage & Preferences — ✅ **Confirmed: Jetpack DataStore preferences for theme persistence with instant application via StateFlow.**
- Performance-Safe Glassmorphism — ✅ **Confirmed: gradient scrims + directional highlight stroke shaders for liquid glass aesthetic with zero GPU overhead.**
- Floating Navigation — ✅ **Confirmed: Floating pill navigation bar hovering above content with high-prominence Clean Up CTA.**
- Home Screen Architecture — ✅ **Confirmed: 4-column responsive grid, liquid glass filter chips (All/Favorites/Videos/Screenshots), date counts, video duration/favorite badges, floating bottom navigation pill + icon-only Clean Up button.**

## Recent Activity

- 2026-09-16: Phase 47 — Unified Video Player Architecture (`SwipePixVideoPlayer`) + In-Deck Playback + External Fallback (COMPLETED & VERIFIED ON PHYSICAL S24 ULTRA):
  - **Mission & System Architectural Convergence**: Eliminated the split where Photos/Albums used external `ACTION_VIEW` intent choosers and Cleaning Mode used an isolated prototype player. Built ONE canonical, reusable video playback architecture (`in.heyvinay.swipepix.ui.video`) used across Photos, Albums, PhotoViewer, and Swipe Cleaning.
  - **Core Subsystems Built**:
    1. `SwipePixPlayerEngine`: Pure Android platform `MediaPlayer` + hardware-accelerated `TextureView` (zero 3rd-party dependencies, zero Play Protect PHA risk). Integrated audio focus handling via `AudioManager`, 100ms coroutine ticker for smooth UI updates, scrubbing freeze/commit lifecycle, and lifecycle pause/release hooks.
    2. `SwipePixPlaybackState`: Unified immutable state model (`IDLE`, `PREPARING`, `READY`, `PLAYING`, `PAUSED`, `BUFFERING`, `COMPLETED`, `ERROR`) with smart `mm:ss` / `hh:mm:ss` time formatting.
    3. `SwipePixVideoSeekSlider`: Touch scrubber with 36dp touch target and pointer gesture isolation (`change.consume()`), preventing parent card swipes or pager flings during seeking.
    4. `SwipePixVideoControls`: Frosted glass central action button (Play/Pause/Replay), 3.5s auto-fade timer, and bottom dock housing the seek slider, timestamps, mute/unmute toggle, and "Open with" external player fallback button.
    5. `SwipePixVideoSurface`: Composable wrapping Android `TextureView` with `SurfaceTextureListener`, supporting Compose clipping (`AdaptiveMediaShape = RoundedCornerShape(24.dp)` / `16.dp`) and card rotation animations.
    6. `SwipePixVideoPlayer`: Master composable assembling thumbnail layer, surface, controls, loading spinner, error fallback, and lifecycle observers.
  - **Screen Integrations**:
    - `AdaptiveMediaSurface.kt`: Integrated `SwipePixVideoPlayer(mode = VideoPlayerMode.CLEANING_DECK)` for in-deck video playback.
    - `ZoomableImage.kt`: Integrated `SwipePixVideoPlayer(mode = VideoPlayerMode.VIEWER)` for full-screen photo/video viewer.
    - `PhotoViewerScreen.kt`: Dynamic header subtitle ("Video" vs "Photo").
  - **Automated Tests**: 104/104 unit tests passing across 17 test suites (100% pass rate, including 7 new tests in `SwipePixPlaybackStateTest`).
  - **Build & Release Signing**: Clean release build signed with dedicated 4096-bit RSA key (`CN=SwipePix, OU=Mobile, O=HeyVinay, L=Bengaluru, ST=Karnataka, C=IN`).
  - **Live Physical S24 Ultra Verification (`RZCY816ZY1M`)**:
    - Photos Viewer: In-app video playback verified with real-time scrubber (`00:06 / 00:15`), immersion mode, and "Open with" system chooser.
    - Cleaning Deck: In-deck playback verified on active video card, "Open with" fallback verified directly from deck, Keep swipe fling verified (clean release and advance to `2 / 160`), Undo restoration verified ("Photo restored" pill), and Save & Exit dialog verified with state checkpoint preserved in Room database.
    - Safety Guarantee: Video playback and "Open with" actions strictly NEVER touch `photo_decisions` or modify Room session progress.


- 2026-09-16: Next-Gen Photo Viewer Redesign & High-Fidelity UI/UX Verification (COMPLETED & VERIFIED ON PHYSICAL S24 ULTRA):
  - **Mockup Match & Architectural Elevation**: Transformed the SwipePix photo/video viewer into an immersive, next-gen viewer matching the high-fidelity UI mockup (`swipepix_viewer_mockup.md` / `media_1789557222557.jpg`) inspired by Google Photos and Samsung Gallery.
  - **Key Capabilities Implemented**:
    1. **Full-Screen Dark Canvas & Ambient Depth**: Edge-to-edge `#080A0F` background canvas with an ultra-subtle, deep atmospheric Gaussian blur (`alpha = 0.20f`, `scale = 1.3f`, `blur(50.dp)`) of the active photo radiating into the dark background.
    2. **Floating Liquid Glass Top Control Row**: Translucent floating pill containing a circular Back button (`44.dp`), high-legibility position counter (`2 / 3,287`) with `Photos` scope label, and balanced action buttons: Share, Favorite (heart toggle with instant MediaStore persistence), and Delete (`#EF4444` soft red accent) with safety confirmation dialog and Undo snackbar.
    3. **Adaptive Hero Media Surface**: Exact image bounds with 16dp rounded corners, subtle 1dp white border (`alpha = 0.12f`), and soft depth shadow at resting state (`1.0×`), animating to 0dp corners when zoomed (`> 1.05×`) with unconstrained pinch-to-zoom (up to 5.0×) and panning.
    4. **Interactive Zoom Pill & Expand Button**: Positioned in the bottom-right corner of the hero image frame. Features a frosted glass pill displaying the current magnification (`1.0×` .. `5.0×`) and an expand button (`OpenInFull` / `CloseFullscreen`). Both elements are interactive and smoothly animate between `1.0×` and `2.5×` via `androidx.compose.animation.core.animate` with `FastOutSlowInEasing`.
    5. **Floating Metadata Glass Card**: Modern frosted glass card displaying formatted date (`September 14, 2026`), specs row (`5:09 pm • 8160 × 6120 • 7.7 MB`), `No location` tag, and a 3-dot overflow menu. Tapping the card opens the full Photo Details modal dialog (Title, Date, Resolution with MP, Size, MIME Type, Album).
    6. **Quick-Scrub Filmstrip Carousel**: Bottom horizontal row of thumbnail cards synchronized two-way with the HorizontalPager. The active thumbnail is highlighted with a 2dp white rounded border and auto-centers during scrolling. Tapping any thumbnail animates directly to that photo.
    7. **Immersion Mode**: Single tap on empty stage/photo smoothly fades all top and bottom chrome for a 100% unobstructed view.
    8. **MediaStore Favorite Integration**: Full support for toggling photo favorite state via `MediaStore.createFavoriteRequest` (Android 11+), persisting to the system MediaStore and instantly updating the gallery grid.
  - **Jetpack Compose Touch Hit-Testing Trap Solved**: Replaced `graphicsLayer { translationY = ... }` with layout-level `Modifier.offset(y = ...)` for the image stage and zoom controls overlay, guaranteeing that the hit-testing coordinates match the visually rendered position on high-DPI displays.
  - **Verification**: 79/79 unit tests passing across all test suites, `assembleRelease` BUILD SUCCESSFUL with R8 minification and release keystore signing. Sideloaded and tested directly on physical Samsung Galaxy S24 Ultra (`RZCY816ZY1M`, Android 14 One UI 6.1, 450 DPI): Verified zoom toggling (`1.0×` <-> `2.5×`), details dialog, filmstrip scrubbing, favorite toggle with instant grid sync, and immersion mode. Full photographic verification saved in artifact directory (`s24u_verified_*.png`).


- 2026-09-16: P1 Adaptive Swipe Media Frame Resolution & Zero-Letterbox Hugging (COMPLETED & VERIFIED ON PHYSICAL S24 ULTRA):
  - **Issue Investigated**: On the swipe cleaning screen (`CleanupScreen.kt`), the visible rounded frame did not match the actual image bounds for portrait photos (e.g. portrait photo of a woman holding a shirt was displayed inside a wide 4:3 landscape card with dark pillarboxing on the sides inside the 24dp rounded border).
  - **Root Cause**: `MediaStoreDataSource.kt` queried `WIDTH` and `HEIGHT` but omitted `MediaStore.MediaColumns.ORIENTATION`. Camera sensors capture raw photos in sensor landscape dimensions (e.g. 4000x3000) with EXIF `ORIENTATION = 90`. `CleanupScreen.kt` passed raw `width` and `height` to `calculateAdaptiveSize`, producing a wide landscape card (360x270dp). Coil 3 decoded the photo and rotated it 90 degrees to portrait (3000x4000). `AsyncImage` with `ContentScale.Fit` centered the portrait photo inside the wide landscape card, leaving dark letterbox/pillarbox space inside the card border. Additionally, `AdaptivePhotoCard` wrapped `AsyncImage` in a separate `Box` with background `#0F1219` and border, creating an outer frame distinct from the image.
  - **Remediations Implemented**:
    1. `MediaItem.kt`: Added `val orientation: Int = 0`, `displayedWidth`, `displayedHeight` (swapping width and height when `orientation == 90 || orientation == 270`), and `displayedAspectRatio`.
    2. `MediaStoreDataSource.kt`: Added `MediaStore.MediaColumns.ORIENTATION` to `mediaProjection` and populated `orientation` across `queryPhotos`, `queryMediaItemById`, and `queryTrashedMedia`.
    3. `AdaptiveMediaSurface.kt`: Created reusable `AdaptiveMediaSurface` composable guaranteeing `FRAME SIZE == IMAGE SURFACE SIZE`. Implemented `calculateAdaptiveMediaSize(aspectRatio, maxStageWidth, maxStageHeight)` with aspect ratio clamping (`0.35f..2.8f`). Applied 24dp rounded corners directly to the image container with 1dp subtle border hugging the image edge. Used `ContentScale.Crop` inside the exact aspect-ratio surface for 100% zero-letterbox and zero-pillarbox display. Added Coil `AsyncImagePainter.State.Success` `painter.intrinsicSize` listener as dynamic fallback for media with missing MediaStore dimensions. Overlaid video play badge and duration pill when `MediaType.VIDEO`.
    4. `CleanupScreen.kt`: Replaced legacy `AdaptivePhotoCard` with `AdaptiveMediaSurface` for both Card 1 and Card 2, preserving Level 1 Stable Media Stage (`BoxWithConstraints` with fixed flex weight) for full-stage touch target and stable layout.
    5. `ZoomableImage.kt`: Updated aspect ratio derivation to use `displayedWidth`, `displayedHeight`, and `displayedAspectRatio`.
  - **Verification**: 79/79 unit tests passing (100% across 14 test suites, including 10 new tests in `AdaptiveMediaSurfaceTest`), `assembleRelease` BUILD SUCCESSFUL. Tested live on physical Samsung Galaxy S24 Ultra (`SM-S928B` / `RZCY816ZY1M`): Confirmed the portrait photo of the woman displays with the 24dp rounded border perfectly hugging the portrait image boundary with **ZERO letterbox and ZERO pillarbox** (`physical_adaptive_screen5.png`). Swiped and advanced through tall screenshots and 3:4 portrait photos, verified Undo restoration, and confirmed session checkpoint ("In Progress (14 reviewed)") was completely preserved.

- 2026-09-16: P0 Security Audit — Google Play Protect "Harmful App Blocked" (Hostile Downloader) Remediation (COMPLETED & VERIFIED):
  - **Issue Investigated**: Physical Samsung Galaxy S24 Ultra (Android 14/15 One UI 6.1) blocked installation via Google Play Protect with error: `Harmful app blocked — SwipePix: “This app can install potentially harmful apps without your permission.”` (PHA Category: Hostile Downloader / Dropper).
  - **Code & Manifest Audit**: Confirmed zero malicious capability. `android.permission.INTERNET` is completely absent (100% offline). Zero package installer permissions (`REQUEST_INSTALL_PACKAGES` absent). Zero dynamic code loading, zero process execution, zero file dropping.
  - **Root Cause**: `app/build.gradle.kts` previously specified `buildTypes.release.signingConfig = signingConfigs.getByName("debug")`. Sideloading an obfuscated (`isMinifyEnabled = true`), non-debuggable (`android:debuggable="false"`) APK signed with the shared public `CN=Android Debug` key triggers Google Play Protect and Samsung Auto Blocker cloud heuristics, which classify unknown obfuscated debug-signed APKs as hostile droppers.
  - **Fix Implemented**: Generated dedicated RSA 4096-bit release keystore (`keystore/swipepix-release.jks`, DN: `CN=SwipePix, OU=Mobile, O=HeyVinay, L=Bengaluru, ST=Karnataka, C=IN`, SHA-256: `48:31:9B:8D:...`). Configured Gradle release signing via `keystore.properties` (protected by `.gitignore`). Added `-dontwarn in.heyvinay.swipepix.Hilt_*` to `proguard-rules.pro`.
  - **Verification**: 69/69 unit tests passing (100%), `assembleRelease` BUILD SUCCESSFUL. Tested on physical Samsung Galaxy S24 Ultra (`SM-S928B`): Streamed install succeeded with 0 warnings. Google Play Protect device scan completed with **"No harmful apps found"** (green shield checkmark), scanning SwipePix at the top of recently scanned apps. Photos screen rendered live with 3,026 photos and 274 videos.
  - **Multi-Select & Batch Trashing**: Added `Modifier.combinedClickable` to gallery and album photo grids to enter selection mode via long-press. Selection mode includes circular checkmark badges, 2dp primary border, 0.93f scale down, animated top selection bar with count, Select all / Deselect all, red Trash action, back button interception, floating navigation bar dismissal, preliminary dialog confirmation, Android OS `MediaStore.createTrashRequest` integration, and Material 3 Snackbar with Undo restoration.
  - **Image Viewer Redesign**: Built two-level layout (Level 1: Stable viewer chrome and bounded stage; Level 2: Adaptive media surface matching native aspect ratio). Added adaptive 16dp rounded corners at resting zoom (`1f`) animating to 0dp as zoom increases, subtle 1dp border, unconstrained pinch-to-zoom (up to 5x), double-tap zoom/reset (1f <-> 2.5f), video thumbnail/badge support, and top/bottom chrome.
  - **In-Viewer Safe Deletion**: Trash button prompts preliminary confirmation, invokes MediaStore trash, deletes photo, and smoothly advances to the next photo (or auto-exits to gallery when empty) with Undo snackbar.
  - **Architecture Isolation**: Gallery manual deletions and batch trash operations strictly manipulate MediaStore trash and never touch Room `SwipeSessionDao`, `SwipeDecisionDao`, or `session_progress`. Swiping checkpoints remain 100% intact.
  - **VERIFICATION**: 69/69 unit tests passing (100% across 13 test suites), `assembleDebug` PASS, on-device verification across 14 lifecycle states documented with screenshots.

- 2026-09-16: Phase 45 — Complete Swipe UX + Adaptive Media Frame + Persistent Review Checkpoint (COMPLETED & VERIFIED):
  - **Adaptive Media Frame & Redundant Full Preview Removal**: Replaced fixed 3:4 container with a two-level layout (Level 1: Stable Media Stage with fixed flex weight `Modifier.weight(1f)`, Level 2: Adaptive Visual Media Surface fitting native aspect ratio with 24dp rounded corners and subtle 1dp border). Removed "Full Preview" floating action chip and `FullPreviewDialog`. Anchored video badges (central play button + duration pill) directly to the adaptive frame.
  - **Persistent Review Checkpoint & Continuation Lifecycle**: Decoupled review progress from applied cleanup batches. Mid-session completions (e.g. 700 reviewed out of 5,000; 200 trash, 500 keep) preserve the session as `PAUSED` when unreviewed items remain. Resuming continues at item 701 (first unreviewed item in deterministic MediaStore order), never re-presenting previously kept or trashed photos. "Start Fresh" creates a new session starting from photo 1. Session is only marked `COMPLETED` when 0 unreviewed photos remain in the scope.
  - **Authoritative Review Counts**: Added `getTotalReviewedCountForSession` in Room repository querying all decision states (`KEEP`, `TRASH_PENDING`, `TRASHED`), ensuring reviewed progress is never reset when trash operations commit.
  - **VERIFICATION**: 59/59 unit tests passing (100%), `compileDebugKotlin` PASS, on-device verification across live emulator states documented with PNG artifacts.

- 2026-09-16: Phase 44 — P0/P1 Swipe Experience + Session Lifecycle + Full Preview (COMPLETED & VERIFIED):
  - **BUG 1 — Photo vs Video Distinct Differentiation**: Videos now display hardware-extracted video frame previews, a central circular play badge, and a bottom-left duration pill with `Videocam` icon and formatted `mm:ss` timestamp without interfering with gestures.
  - **BUG 2 — Image Fit & Aspect Ratio**: Replaced `ContentScale.Crop` with `ContentScale.Fit` surrounded by a dark theater letterbox (`#0F1219` / `#1E2430`), eliminating edge cropping across ultra-wide panoramas, square, tall portrait, and standard landscape media.
  - **NEW UX — Full Preview**: Added non-destructive "Full Preview" floating action chip opening `FullPreviewDialog` powered by `ZoomableImage` with pinch-to-zoom, panning, file title, and top-bar close action. Back button or close action safely returns to the exact active card with zero deck corruption.
  - **BUG 3 — Session Continuation & Completion**: Distinguishes `ACTIVE`, `PAUSED`, and `COMPLETED`. Completed sessions (deck finished or trash executed) are marked `COMPLETED` and never offered as resumable. `CleanupSourceSelectionViewModel` and `AlbumDetailViewModel` guard resumable checks with `reviewedCount < totalCount`. `CleanupCoordinator` triggers `completeSessionWithoutTrash()` if `trashedCount == 0`.
  - **BUG 4 — Swipe Physics Directional Integrity & Flash Elimination**: Fixed velocity direction mismatch by guarding velocity flings with `isDraggedRight && velX > 900f` and `isDraggedLeft && velX < -900f`. Reset card offsets (`snapTo(0f)`) before triggering `onSwipeComplete`. Added progressive thumbnail placeholder (`${uri}_256`) and card memory key (`${uri}_card`) to completely eliminate blank/white flashes during card swiping.
  - **VERIFICATION**: 54/54 unit tests passing (100%), `compileDebugKotlin` PASS, on-device verification across 14 lifecycle states documented with screenshots.

- 2026-09-16: Phase 41 — Complete Technical Knowledge & Architectural Interview Audit (COMPLETED):
  - Created standalone comprehensive technical document: `SWIPEPIX_COMPLETE_TECHNICAL_KNOWLEDGE.md` (37 sections, all layers).
  - Created standalone interview cheatsheet: `SWIPEPIX_INTERVIEW_CHEATSHEET.md` (10-line architecture, 10 critical classes, 10 key concepts, 10 bug fixes, 20 senior interview Q&As).
  - Verified unit test suite: 40/40 tests passing (100%) across 10 test suites (including `NavigationChromeTest` and `SwipePixMotionTest`).
  - Verified build status: `compileDebugKotlin` PASS, `assembleDebug` PASS, `assembleRelease` with R8 minification and resource shrinking PASS.
  - Recorded offline privacy guarantees, platform thumbnail architecture, 4-layer deduplication engine, and authoritative count pipelines.

- 2026-09-16: Phase 32 — Thumbnail Cache + Media Performance + Accurate Media Counts (COMPLETED & VERIFIED ON EMULATOR; PHYSICAL DEVICE PENDING):
  - **CURRENT_PHASE**: Phase 32 — Thumbnail Cache + Media Performance + Accurate Media Counts.
  - **ROOT_CAUSES**:
    1. Repeated image loading / flashing in viewer was caused by missing placeholder bridge between `PhotoGrid` cached thumbnails (`${uri}_256`) and `ZoomableImage`, disruptive `crossfade(true)` fading through black, and blocking `detectTransformGestures` consuming horizontal drag gestures away from `HorizontalPager`.
    2. Inaccurate video and favorite counts were caused by computing counts from the in-memory `allPhotos` list (capped at `PAGE_SIZE = 200`), starving older items beyond offset 200 and failing to query `IS_FAVORITE = 1` in SQL.
  - **THUMBNAIL_ARCHITECTURE**: Created custom Coil 3 `MediaStoreThumbnailFetcher` registered ahead of decoders, delegating to `ContentResolver.loadThumbnail(Uri, Size, null)` on API 29+ for hardware-accelerated system thumbnails <= 512px. Direct `.thumbnails` filesystem scraping was rejected due to Scoped Storage restrictions and OEM fragmentation.
  - **CACHE_ARCHITECTURE**: Memory cache (25% heap) + Disk cache (250MB). Progressive viewer bridge via `.placeholderMemoryCacheKey("${mediaItem.contentUri}_256")` for 0ms visual rendering.
  - **COUNT_ARCHITECTURE**: Authoritative SQL cursor counts via `MediaStoreDataSource.queryMediaCount()` for `MEDIA_TYPE_VIDEO` and `IS_FAVORITE = 1`. SQL selections in `queryPhotos` for `GalleryFilterCategory` (`ALL`, `VIDEOS`, `FAVORITES`, `SCREENSHOTS`) enabling true library-wide pagination.
  - **TEST_STATUS**: 35/35 unit tests passed (100%), `assembleDebug` PASS, `assembleRelease` PASS.
  - **PHYSICAL_DEVICE_STATUS**: UNVERIFIED (Hardware `RZCY816ZY1M` detached from ADB host; verified live on `emulator-5554`).

- 2026-09-16: P0 Data Correctness Audit & MediaStore Chronological Sort Fix (COMPLETED & VERIFIED):
  - **CURRENT_PHASE**: P0 Data Correctness Audit & Physical Device Synchronization.
  - **ROOT_CAUSE**: In `MediaStoreDataSource.kt`, passing multiple sort columns via `QUERY_ARG_SORT_COLUMNS` caused Android's internal `ContentResolver.createSqlSortClause` to synthesize `ORDER BY datetaken, date_modified, date_added, _id DESC`. In SQLite, unannotated columns default to `ASC`, which caused the database query to return the 200 *oldest* photos on the phone (which stopped at June 24). When `GalleryViewModel` sorted those 200 oldest photos in memory descending, June 24 appeared at the top, and all September photos (stored at offsets > 200) were completely omitted from page 1.
  - **DATA_SOURCE_DECISION**: Replace `QUERY_ARG_SORT_COLUMNS` with explicit `QUERY_ARG_SQL_SORT_ORDER` specifying `${MediaStore.MediaColumns.DATE_TAKEN} DESC, ${MediaStore.MediaColumns.DATE_MODIFIED} DESC, ${MediaStore.MediaColumns.DATE_ADDED} DESC, ${MediaStore.MediaColumns._ID} DESC`. MediaStore query directly returns the newest items at offset 0.
  - **TIMESTAMP_DECISION**: Updated `MediaItem.effectiveTimestamp` to normalize `dateTaken` from seconds to milliseconds when `dateTaken < 100_000_000_000L`. Fallbacks: `dateModified * 1000L` -> `dateAdded * 1000L` -> `0L`.
  - **IDENTITY_DECISION**: Retain `contentUri.toString()` as canonical stable Compose key, preventing collision between photos/videos with duplicate integer IDs across external storage volumes.
  - **COMPLETED_TASKS**:
    1. Fixed `MediaStoreDataSource.kt` query sort order to explicit multi-column `DESC`.
    2. Normalized `MediaItem.kt` `effectiveTimestamp` to prevent seconds/milliseconds unit skew.
    3. Added diagnostic logging in `GalleryViewModel.kt` (`SwipePixGallery`) logging the first 20 raw MediaStore items and verifying offsets.
    4. Verified on live Android instance: confirmed `Item #0` is the newest item (`effectiveTs=1789513442000`, `header=Today`), followed sequentially by older items down to item #6.
    5. Verified foreground resume: confirmed old list is replaced rather than appended, preserving exact item counts and zero duplicate keys.
    6. Added regression test `effectiveTimestamp_normalizesSecondsToMilliseconds` in `DateUtilsTest.kt`.
    7. Built and verified both Debug and Release APKs (`assembleDebug` and `assembleRelease` with R8 minification).
  - **TEST_STATUS**: `testDebugUnitTest` 24/24 passed (100%), `compileDebugKotlin` PASS, `assembleDebug` PASS, `assembleRelease` PASS.
  - **PHYSICAL_DEVICE_STATUS**: Verified on physical hardware (Samsung Galaxy S24 Ultra, `SM-S928B`, serial `RZCY816ZY1M`). Logcat confirmed: `MediaStore raw query returned 200 items (offset=0). Total=3417`, `Item #0: id=1000182218, header=September 14`, `Item #5: header=September 13`, `Item #9: header=September 12`, `Item #10: header=September 11`. Zero duplicate keys (`DUPLICATE_IDS=0, DUPLICATE_URIS=0`), zero Compose crashes (`IllegalArgumentException: Key was already used` eliminated), pagination up to 400 items confirmed, Keep/Trash swiping and reversible undo verified live.
  - **KNOWN_ISSUES**: None.
  - **NEXT_TASK**: Ready for next phase or feature development.

- 2026-09-16: Physical Device P0 Crash Remediation — Duplicate Lazy Grid Keys (COMPLETED & VERIFIED):
  - **Root Cause Analysis**: Physical device crash `IllegalArgumentException: Key "1000138102" was already used` in `PhotoGrid.kt:130` triggered by concurrent pagination/refresh race conditions (`LifecycleResumeEffect` vs `init` coroutine overlap), MediaStore multi-volume cursor duplicates, and bare Long IDs without collection namespace isolation.
  - **Layer 1 (MediaStore Data Source)**: Added `seenUris = mutableSetOf<Uri>()` deduplication in `MediaStoreDataSource.queryPhotos` and `queryTrashedMedia` ensuring no cursor row duplicates.
  - **Layer 2 (ViewModel Pagination & Job Tracking)**:
    - Added `loadJob: Job?` and `loadMoreJob: Job?` with strict mutual cancellation in `GalleryViewModel`, `ViewPhotosViewModel`, `AlbumsViewModel`, and `TrashViewModel`.
    - Added `existingUris.add(...)` filtering in pagination `loadMore()` to prevent duplicate items when cursor offsets shift.
    - Added `distinctBy { it.contentUri.toString() }` before publishing UI states.
  - **Layer 3 (Date Grouping Pipeline)**: Updated `DateUtils.groupPhotosByDate` to deduplicate incoming items and merge header lists by `contentUri.toString()`.
  - **Layer 4 (Compose Lazy Grid & Pager Key Contracts)**:
    - `PhotoGrid.kt`: Replaced `photo.id` key with `photo.contentUri.toString()`, memoizing unique items with `remember(group.photos) { group.photos.distinctBy { it.contentUri.toString() } }`.
    - `TrashScreen.kt`: Replaced `it.id` key with `it.contentUri.toString()`, deduplicating `state.displayedItems`.
    - `PhotoViewerScreen.kt`: Replaced `photo.id` key in `HorizontalPager` with `photo.contentUri.toString()`.
    - `AlbumsScreen.kt` & `CleanupSourceSelectionScreen.kt`: Guaranteed album ID uniqueness before grid composition.
  - **Verification**: Added regression test `groupPhotosByDate_withDuplicateUris_deduplicatesGracefully` in `DateUtilsTest.kt`. All unit tests passed (100%), `compileDebugKotlin` ✅, `assembleDebug` ✅, `assembleRelease` (with R8 minification & resource shrinking) ✅.

- 2026-09-16: Official 3D SwipePix App Icon & In-App Brand Art Integration (COMPLETED & VERIFIED):
  - **Source Assets**: Processed high-resolution (1254x1254) custom 3D art (`background.png` and `foreground.png` from `/temp/`).
  - **Foreground Scale Calibration**: Reduced the foreground elements by ~35% (in-app composite scaled to 0.65, adaptive launcher scaled to 0.46) to provide generous, balanced breathing room and showcase the vibrant curved gradient background around the 3D cards and orbiting cyan swoosh.
  - **Adaptive Launcher Icon Pipeline**:
    - Generated density-specific adaptive backgrounds (`ic_launcher_background.png`) and scaled foregrounds (`ic_launcher_foreground.png`, scale factor 0.46 centered inside Google's 72dp safe circle) for `mdpi` (108px), `hdpi` (162px), `xhdpi` (216px), `xxhdpi` (324px), and `xxxhdpi` (432px).
    - Generated legacy squircle icons (`ic_launcher.png`) and circular icons (`ic_launcher_round.png`) with 0.65 foreground scale for all 5 densities (48px to 192px).
    - Configured `mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml` with adaptive foreground, background, and monochrome layers.
  - **In-App Branding**:
    - Generated 512x512 high-resolution composite with 0.65 foreground scale in `res/drawable-nodpi/ic_swipepix_logo.png`.
    - Maintained `SwipePixAppIcon` in `ui/components/SwipePixAppIcon.kt` with continuous squircle clipping (`size * 0.22f`), subtle 1dp glass stroke border (`#FFFFFF` with vertical alpha gradient), and colored ambient/spot drop shadow elevation.
    - Rendered in Onboarding (`WelcomeScreen` at 96dp and `PhotoAccessScreen` at 72dp) with optimal proportions across Light and Dark themes.
  - **Verification**: `compileDebugKotlin` ✅, `testDebugUnitTest` (22/22 unit tests passed — 100%), `assembleDebug` ✅, `assembleRelease` ✅.

- 2026-09-16: Phase 31B — Real Device Gallery, Onboarding, and Video Hardening (COMPLETED & VERIFIED):
  - **Bug 1 — Photos Date Grouping & Sorting**: Added `MediaItem.effectiveTimestamp` (prioritizing `dateTaken`, falling back to `dateModified * 1000L` and `dateAdded * 1000L`). Updated `MediaStoreDataSource.queryPhotos` to sort by `DATE_TAKEN DESC, DATE_MODIFIED DESC, DATE_ADDED DESC, _ID DESC`. Refactored `DateUtils.groupPhotosByDate` to group by `LocalDate` in device timezone and sort groups in strict descending order, ensuring chronological consistency across pagination chunks.
  - **Bug 2 — Reactive Permission State Machine**: Formalized explicit state transitions (`Unknown`, `Checking`, `Granted`, `Partial`, `Denied`, `PermissionRequired`) with `isGrantedOrPartial` and `isDeniedOrRequired` helpers. Replaced fragile destination checks with `currentBackStackEntryAsState()` and `hasRoute<SwipePixRoute.Permission>()`. Used Compose `LifecycleResumeEffect(Unit)` in `SwipePixNavGraph` and `OnboardingScreen` to dynamically handle backstack restoration from `savedInstanceState` after returning from Android Settings.
  - **Bug 3 — Video Frame Thumbnails**: Integrated `io.coil-kt.coil3:coil-video:3.6.2` and registered `VideoFrameDecoder.Factory()` in `CoilModule.kt`. Video thumbnails in grids and full-screen viewers now extract and display accurate hardware-accelerated video frame previews.
  - **Bug 4 — Pluralized Media Counts**: Implemented `DateUtils.formatMediaSubtitle` across `PhotoGrid`, `HomeHeader`, `MainGalleryShell`, `AlbumDetailScreen`, and `ViewPhotosScreen`. Corrected grammar errors (e.g. "1 photo" instead of "1 photos") and disclosed mixed photo and video counts (e.g. "1,250 photos • 48 videos", "4 photos • 2 videos").
  - **Bug 5 — R8 Release Navigation Stability**: Proguard rules confirmed for Kotlin serialization, navigation routes, Room entities, and `PhotoDecisionEntity`. Executed `assembleRelease` with R8 minification and resource shrinking completing with 0 errors.
  - **Bug 6 — Resume & MediaStore Reload**: Added `LifecycleResumeEffect(Unit)` to `MainGalleryShell` to immediately trigger `galleryViewModel.refresh()` and `albumsViewModel.refresh()` when app returns to the foreground, eliminating `EMPTY_LIBRARY` stalls after permission grant.
  - **Verification**: Created `DateUtilsTest.kt` covering sorting, timestamps, subtitles, and grouping; extended `PermissionStateTest.kt`. 22/22 unit tests passing (100%), `compileDebugKotlin` ✅, `assembleDebug` ✅, `assembleRelease` ✅.

- 2026-09-16: Gallery Navigation & Cleaning Experience Stabilization:
  - **Unified Gallery Shell (`MainGalleryScreen.kt`)**: Hosts both Photos and Albums tabs inside a single shared container with animated content transitions (fade + subtle horizontal slide), preserving the persistent bottom navigation pill across tab switches without unmounting churn.
  - **Sliding Animated Bottom Navigation Bar (`SwipePixBottomBar.kt`)**: Re-engineered bottom pill (212dp) with an animated offset indicator (106dp) utilizing Compose spring physics (damping 0.82f, stiffness 380f) and `animateColorAsState` for smooth, responsive gliding between Photos ↔ Albums.
  - **Standardized Top Gallery Header (`GalleryHeader.kt`)**: Unified typography across Photos and Albums with pixel-identical 32sp bold title, 14sp subtitle, 42dp circular glass Settings button, and padding.
  - **Mid-Session Finish Workflow (`MidSessionFinishDialog`)**: Replaced unused `MoreVert` menu with a high-contrast "Finish" button in `CleanupScreen`. Prompts user with "Apply & Finish" (triggers batch OS trash for pending items), "Save & Exit" (persists unapplied decisions and exits), and "Keep Cleaning".
  - **BackHandler Interception (`ExitCleanupDialog`)**: Intercepts Android back gesture and TopAppBar back navigation icon to protect against accidental session abandonment, offering "Save & Exit", "Discard Session" (with permanent discard confirmation alert), and "Keep Cleaning".
  - **Cleaning Scope & Discard Session Architecture**: Added `discardSession(albumId)` to `CleanupSessionRepository` and Room DAO (`clearDecisionsForScope`), clearing unapplied decisions (`decision != 'TRASHED'`) while keeping committed OS Trash intact and preserving other album scopes.
  - **Unit Testing & Build Verification**: Added unit test `testDiscardSession_clearsUnappliedDecisions_preservesOtherScopesAndTrashedItems` in `CleanupSessionTest.kt`; 18/18 unit tests passing across all test suites (100%), 0 Kotlin compiler errors/warnings, and `assembleDebug` successful.

- 2026-09-16: Select Source Screen Elevation:
  - **Modernized Select Source UI**: Replaced basic list prototype with a high-fidelity, dual-theme screen (`CleanupSourceSelectionScreen.kt`).
  - **Hero Option "All Photos"**: Full-width card with dynamic library photo/video counts (`NumberFormat`), "ALL MEDIA" badge, latest thumbnail stack, and persistent reviewed progress indicator.
  - **Responsive 2-Column Album Grid**: Real album covers with 256px thumbnail bounds, dark gradient scrims, single-line names, photo/video counts, persistent reviewed pills, and trailing chevrons.
  - **Progressive Disclosure**: Automatic folding if > 8 albums with smooth animated "Show all albums" button.
  - **Informative Scope Help**: TopAppBar action opening a Dialog detailing All Photos vs Album cleaning scopes, persistence, and safe confirmation.
  - **Dedicated ViewModel**: Created `CleanupSourceSelectionViewModel` isolating source selection, sorting Camera/DCIM first, aggregating counts, mapping album progress, and debouncing MediaStore changes.
  - **Navigation Integration**: Wired `CleanupSourceSelectionViewModel` into `SwipePixNavGraph.kt`.
  - **Unit Testing & Verification**: Created `CleanupSourceSelectionViewModelTest.kt`; 17/17 tests passing (100%), 0 Kotlin compiler warnings/errors, and `assembleDebug` PASS.

- 2026-09-16: Post-Audit Remediation & Verification:
  - **[P0-01] Startup Crash Fix**: Created vector logo `ic_swipepix_logo.xml` and replaced adaptive icon mipmap in `OnboardingScreen.kt` (lines 216 & 395) eliminating cold-start crash.
  - **[P1-01] Album Progress Isolation**: Updated `PhotoDecisionDao`, `CleaningSessionDao`, and `CleanupSessionRepositoryImpl` with album-scoped queries and filtering.
  - **[P1-02] Settings Feature Wiring**: Injected `UserPreferencesRepository` into `CleanupViewModel`, respecting `rememberProgress` in photo loading, `showUndoOption` in `CleanupScreen`, and `confirmBeforeApplying` with a confirmation dialog in `CleanupSummaryScreen`.
  - **[P1-03] Theme Context Safety**: Replaced unsafe `view.context as Activity` in `Theme.kt` with a recursive `Context.findActivity()` helper.
  - **[P1-04] Photo Viewer Boundary Safety**: Guarded `PhotoViewerScreen.kt` index access with `getOrNull` and fallback bounds to eliminate `IndexOutOfBoundsException` risks when trashing items.
  - **[P2-02] Drag Physics Optimization**: Replaced per-touch coroutine allocations in `SwipeCardPhysics.kt` with a conflated channel (`Channel.CONFLATED`) for zero-allocation 120Hz gestures.
  - **[P3-01] DataStore IO Safety**: Hardened `UserPreferencesRepository` with `safePreferences` catching `IOException`.
  - **[P3-02 & P3-03] Code Hygiene**: Deprecated legacy `Modifier.swipeableCard` and removed dead `CleanupComplete` route.
  - **Verification**: 14/14 unit tests pass (`CleanupSessionTest` + album isolation test), 0 compiler errors, and APK successfully assembled (`assembleDebug` PASS).

- 2026-09-15: Complete UI System Refinement (Dark & Light Mode, Onboarding, Settings):
  - **Complete Design System & Dual-Theme Architecture**:
    - Defined comprehensive Material 3 Light (`#F8FAFC` background, `#FFFFFF` surface, high-contrast typography) and Dark palettes (`#0B0E14` background, `#141824` surface) in `Color.kt`.
    - Implemented `LocalDarkTheme` composition local in `Theme.kt`, ensuring all liquid glass brushes, dynamic shadows, borders, and icon tints resolve seamlessly in both dark and light modes.
    - Disabled wallpaper-based `dynamicColor` to maintain SwipePix's signature brand identity and precision contrast.
  - **Onboarding Experience**:
    - Created 2-screen onboarding flow matching design reference with real app icon (`R.mipmap.ic_launcher`), fanned photo cards illustration with Keep/Trash gesture feedback badges, "Private by design" security badge, feature cards, pagination indicators, and runtime permission launcher.
  - **Settings Single-Page Redesign**:
    - Redesigned `SettingsScreen.kt` according to single-page design reference with Appearance theme picker modal bottom sheet (`System Default`, `Light Theme`, `Dark Theme` persisted in DataStore), Cleaning toggles (`rememberProgress`, `showUndoOption`, `confirmBeforeApplying`), Trash actions (`View Trash`, auto-cleanup info), live Permissions check with Settings deep-link, and Privacy & About footer with license dialog.
  - **Screen-by-Screen Dual-Theme Harmonization**:
    - Harmonized all screens: `GalleryScreen`, `AlbumsScreen`, `AlbumDetailScreen`, `ViewPhotosScreen`, `CleanupScreen`, `CleanupSummaryScreen`, `CleanupEmptyScreen`, `CleanupSuccessScreen`, `TrashScreen`, `CleaningPauseDialog`, `SwipePixBottomBar`, and `StateScreens`.
    - Fully verified: `.\gradlew compileDebugKotlin`, `.\gradlew testDebugUnitTest`, and `.\gradlew assembleDebug` all build and pass cleanly.

- 2026-09-15: Phase 11 (Home / Photos Screen Implementation & Liquid Glass Elevation) completed & verified:
  - Extended `MediaItem` and `MediaStoreDataSource` to query both photos and videos with `IS_FAVORITE` and `DURATION`.
  - Added filter categories (`All`, `Favorites`, `Videos`, `Screenshots`) and reactive filtering in `GalleryViewModel`.
  - Built custom `HomeHeader` with bold "Photos" title, live photo count (`NumberFormat`), subtle glass circular Settings button, and horizontal scrolling liquid-glass filter chips with glowing active states.
  - Upgraded `PhotoGrid` to a 4-column responsive layout with date headers ("Today", "Yesterday") showing section counts ("256 photos") and thumbnail overlays (video duration pill and favorite heart).
  - Implemented floating liquid-glass bottom pill (`[ Photos ]` active / `[ Albums ]` inactive) and circular floating Clean Up button (icon-only, broom/brush icon).
  - Added Compose `@Preview` with dark background and mock data for instant IDE design preview.
  - Automated tests and build verified (`.\gradlew testDebugUnitTest`, `.\gradlew assembleDebug`, `.\gradlew assembleRelease` all pass with 0 errors).
  - Created `FloatingBottomBar` using `GlassSurface(shape = CircleShape)` with animated selection states.
  - Implemented high-prominence gradient Clean Up action pill (`#00E676` to `#00B0FF`).
  - Integrated into `MainActivity.kt` with automatic show/hide for immersive screens.
  - Elevated `GalleryScreen.kt` with clean "Photos" title, live photo count `GlassBadge`, and removed old FAB.
  - Elevated `AlbumsScreen.kt` using `GlassCard` and added bottom scroll clearance.
  - Tests and build pass cleanly (`.\gradlew test assembleDebug` in 17s).
- 2026-09-15: Phase 9 (Liquid Glass Design Primitives & Theme Foundation) completed & verified:
  - Deep solid dark background (`#0B0B0E`), solid dark surfaces (`#13131A`, `#1C1C26`), and directional tokens (`KeepGreen = #00E676`, `TrashRed = #FF1744`) in `Color.kt`.
  - Updated `DarkColorScheme` and `Theme.kt` with high-contrast surfaces and edge-to-edge window styling.
  - Implemented `ui/theme/Glass.kt` with `GlassDefaults`, `Modifier.glassEffect()`, `GlassSurface`, `GlassCard`, and `GlassBadge`.
  - Clean compilation and 100% test pass rate (`.\gradlew test assembleDebug` in 13s).
- 2026-09-14: Phase 8 (Final Verification & Release Build) completed & verified:
  - Configured release build type with debug signing fallback for reproducible verification.
  - Verified R8 minification, resource shrinking, and lintVitalRelease pass cleanly (`.\gradlew test assembleRelease`).
  - Verified all 52 MVP requirements across all 8 phases; updated `REQUIREMENTS.md` and `ROADMAP.md`.
  - Generated full verification acceptance report in `.planning/VERIFICATION.md`.
  - Milestone v1.0 MVP declared **PASS** and ready for release.
- 2026-09-14: Phase 7 (Reliability & Polish) execution completed & verified:
  - Jetpack DataStore preferences (`UserPreferencesRepository`) for `AppTheme` (`SYSTEM`, `LIGHT`, `DARK`).
  - Wired `SwipePixTheme` and `MainActivity` to dynamically react to theme preference.
  - Built `SettingsViewModel` and `SettingsScreen` with theme switcher, live permission inspection, system settings launcher, and on-device privacy guarantee.
  - Replaced placeholder Settings route in `SwipePixNavGraph`.
  - Build and tests pass cleanly (`.\gradlew test assembleDebug`).
- 2026-09-14: Phase 6 (Performance Optimization) execution completed & verified:
  - Custom `CoilModule` providing `ImageLoader` with 25% heap MemoryCache.
  - Initialized `SingletonImageLoader` in `SwipePixApp`.
  - Active preloading of upcoming cards (`N+1`, `N+2`) into memory cache in `CleanupViewModel`.
  - Grid thumbnail optimization using `Precision.EXACT` and reduced crossfade churn in `PhotoGrid.kt`.
  - Tests and build pass cleanly (`.\gradlew test assembleDebug` in 6s).
- 2026-09-14: Phase 5 (Clean Up Mode) execution completed & verified:
  - Source selection (`CleanupSourceSelectionScreen`) for All Photos or specific album.
  - Tinder-style swipe cards (`SwipeableCard`) with rotation and threshold-based spring animations.
  - Stack management & in-memory undo mechanism (`CleanupViewModel`).
  - Session UI (`CleanupScreen`) with progress counter, keep/trash buttons, and undo action.
  - End-of-session summary (`CleanupSummaryScreen`) with batched `MediaStore.createTrashRequest`.
  - Seamless coordinator (`CleanupCoordinator`) and navigation graph integration.
  - Unit tests and build pass cleanly (`.\gradlew test assembleDebug`).
- 2026-09-14: Phase 4 (Full-Screen Viewer) execution completed & verified:
  - Built `PhotoViewerScreen` with `HorizontalPager` for swiping.
  - Built custom `ZoomableImage` for pan/zoom using `detectTransformGestures`.
  - Added bottom metadata overlay (date, resolution, size).
  - Integrated `MediaStore.createTrashRequest` for native deletion prompts.
  - Build and tests pass.
- 2026-09-14: Phase 3 (Gallery UI) execution completed & verified:
  - Shared `PhotoGrid` component with date-grouping and adaptive columns.
  - `GalleryScreen` with `GalleryViewModel` for paginated photo fetching.
  - `AlbumsScreen` with `AlbumsViewModel` to list all folders with photo counts.
  - `AlbumDetailScreen` with `AlbumDetailViewModel` for album-scoped browsing.
  - Clean Up FAB wired to navigate to placeholder mode.
  - Automated tests passing (`.\gradlew test assembleDebug`).
- 2026-09-14: Phase 2 execution completed & verified:
  - `PermissionState` with Android 13/14 detection
  - `PermissionViewModel` and `PermissionScreen`
  - `MediaStoreDataSource` and `MediaRepository` implementations
- 2026-09-14: Phase 1 execution completed & verified

## Known Issues

(None yet)

---
*Last updated: 2026-09-14*
