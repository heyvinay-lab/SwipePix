# SwipePix — Senior Android Engineer Interview Cheatsheet

> **Authoritative Technical Interview Reference**  
> Target Role: Senior Android Engineer / Android Architect  
> Project: SwipePix (API 33–35, Jetpack Compose, Kotlin 2.2.10, Room, MediaStore, Coil 3, Hilt)

---

## 1. Architecture in 10 Lines

1. **Unidirectional Data Flow (UDF):** User events dispatch to ViewModels via intent functions; ViewModels emit immutable `StateFlow<UiState>` to Compose UI.
2. **Clean Architecture / MVVM:** Presentation (Compose UI + ViewModel) $\to$ Domain/Repository (`MediaRepository`, `CleanupSessionRepository`) $\to$ Data Sources (`MediaStoreDataSource`, `SwipePixDatabase`).
3. **Dual Data Layer:** Android `MediaStore` is the single source of truth for physical media assets; `Room` is an isolated staging database for user cleanup decisions and sessions.
4. **Offline & Privacy-First:** Zero network permissions (`INTERNET` excluded), no network libraries, no analytics, no cloud egress—100% on-device operation.
5. **Platform Thumbnail Fetcher:** Custom Coil 3 `MediaStoreThumbnailFetcher` calls `ContentResolver.loadThumbnail()` (API 29+) bypassing slow and blocked filesystem scraping.
6. **Progressive Image Loading:** `ZoomableImage` requests full-resolution media with `.placeholderMemoryCacheKey("${uri}_256")`, instantly painting the memory thumbnail on frame 0.
7. **4-Layer Deduplication Engine:** Data Source `seenUris` cache, ViewModel job cancellation, `DateUtils` Set-based deduplication, and stable Compose `photo.contentUri.toString()` keys prevent duplicate-key crashes.
8. **Scoped Storage Compliance:** Direct batch trashing via `MediaStore.createTrashRequest(uris, true)` delegating confirmation to Android's OS dialog with zero raw file deletion permissions needed.
9. **Authoritative SQL Counts:** Total, video, and favorite counts are queried via dedicated cursor count queries (`cursor.count`) over `MediaStore.Files`, never calculated from in-memory page slices.
10. **Centralized Motion & Navigation Chrome:** Dynamic Bottom Navigation visibility based on top-level routes (`Photos` and `Albums`); unified motion tokens (`SwipePixMotion`) enforce 250ms–350ms standard curves.

---

## 2. Tech Stack Summary & Justification

| Technology | Version | Architectural Justification & Trade-off |
|---|---|---|
| **Kotlin** | 2.2.10 | Modern language features (Coroutines, StateFlow, value classes). JVM 17 target. |
| **Jetpack Compose** | BOM 2026.08.00 / M3 1.3.1 | Declarative UI, zero XML layout overhead, sub-composition optimization via `LazyVerticalGrid` and `HorizontalPager`. |
| **Dagger Hilt** | 2.60.1 | Standardized compile-time dependency injection, `@HiltViewModel` lifecycle integration, testability via `@InstallIn(SingletonComponent::class)`. |
| **Room Database** | 2.6.1 | SQLite abstraction with compile-time query verification. Manages `PhotoDecisionEntity` and `CleaningSessionEntity` state machine without modifying device files. |
| **Android MediaStore** | API 33–35 | Native OS indexed media database (`MediaStore.Files.getContentUri("external")`). Zero custom index scanning overhead; adheres to Scoped Storage. |
| **Coil** | 3.6.2 | Modern Kotlin-first image loader. Supports custom `Fetcher.Factory` (`MediaStoreThumbnailFetcher`), 25% heap memory cache, and 250MB disk cache. |
| **Jetpack DataStore** | 1.1.7 | Thread-safe, asynchronous transactional preference storage replacing legacy, error-prone `SharedPreferences` for dark mode and onboarding state. |
| **Kotlinx Coroutines** | 1.10.1 | Asynchronous threading model: `Dispatchers.IO` for MediaStore/Room, `Dispatchers.Default` for date grouping, `Dispatchers.Main.immediate` for UI state. |

---

## 3. Top 10 Critical Classes

1. `MediaStoreDataSource.kt`: Executes content resolver cursor queries against `MediaStore.Files`, applies multi-column sort orders, runs authoritative SQL counts, and enforces initial URI deduplication.
2. `MediaStoreThumbnailFetcher.kt`: Custom Coil 3 `Fetcher` that intercepts URI requests $\le$ 512px and calls platform-native `ContentResolver.loadThumbnail()` on API 29+.
3. `PhotoGrid.kt`: Four-column Compose `LazyVerticalGrid` rendering chronological date headers and `PhotoThumbnail` components with stable String URI keys.
4. `ZoomableImage.kt`: Fullscreen image viewer component with `awaitEachGesture` gesture handling (zoom, pan, double-tap) and frame-0 placeholder memory cache bridging.
5. `PhotoViewerScreen.kt`: Immersive media viewer hosting `HorizontalPager(beyondViewportPageCount = 1)` with top bar actions (Favorite, Trash, Share, Info).
6. `GalleryViewModel.kt`: Central gallery coordinator managing pagination cursors, library-wide category filtering (`ALL`, `VIDEOS`, `FAVORITES`, `SCREENSHOTS`), and authoritative counts.
7. `CleanupSessionRepositoryImpl.kt`: Implements the cleanup staging state machine, recording swipe decisions (`KEEP`, `TRASH_PENDING`) in Room and coordinating batch system trash execution.
8. `SwipeCardPhysics.kt`: Encapsulates swipe physics, drag offset translations, rotational damping (`dragOffset / 20f`), and velocity-based fling dismiss thresholds.
9. `PermissionState.kt`: Reactive permission evaluator handling Android 13+ Granular Media Permissions (`READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`) with `LifecycleResumeEffect` resume checks.
10. `SwipePixMotion.kt`: Centralized animation specifications providing standardized tweens (`Fast`, `Standard`, `Emphasis`) and springs for app-wide motion consistency.

---

## 4. 10 Key Architectural Concepts

### 1. Dual Data Layer Architecture
- **MediaStore** is the sole source of truth for physical media presence, metadata, timestamps, and OS trash state (`IS_TRASHED`).
- **Room (`SwipePixDatabase`)** is an isolated staging engine for user session history and swipe decisions (`KEEP`, `TRASH_PENDING`, `TRASHED`). Room never duplicates media binary data or raw file paths.

### 2. Scoped Storage & Batch Trashing
- Traditional Android cleaners called `File.delete()`, which fails or requires dangerous `MANAGE_EXTERNAL_STORAGE` permissions.
- SwipePix uses `MediaStore.createTrashRequest(contentResolver, uris, true)` (API 30+). This delegates the user prompt to an Android OS dialog via `IntentSenderRequest`. If approved, the OS handles trash retention (30 days) and filesystem modification.

### 3. Progressive Thumbnail $\to$ Full-Res Memory Bridge
- `PhotoGrid` loads thumbnails into Coil's memory cache with key `${contentUri}_256`.
- `ZoomableImage` requests the full-resolution image with key `${contentUri}_full`, but passes `.placeholderMemoryCacheKey("${contentUri}_256")`. Coil paints the cached 256px thumbnail on frame 0, eliminating white/black blank frames while decoding the 48MP original.

### 4. 4-Layer Deduplication Engine
Prevents Compose `IllegalArgumentException: Key was already used` in `LazyVerticalGrid`:
1. *Data Source:* `seenUris: HashSet<Uri>` filters duplicates within cursor page batches.
2. *ViewModel:* Active query job is cancelled (`loadJob?.cancel()`) prior to launching new queries, preventing async append race conditions.
3. *Domain/Utils (`DateUtils.kt`):* Aggregates items using an explicit Set before building date sections.
4. *Compose UI:* `items(photos, key = { photo -> photo.contentUri.toString() })` ensures 100% stable, unique item keys.

### 5. Hardware-Accelerated Thumbnail Fetching
- Direct `/sdcard/DCIM/.thumbnails/` parsing was rejected due to Scoped Storage restrictions and OEM fragmentation (Samsung vs Xiaomi vs Pixel).
- SwipePix implements a custom Coil `Fetcher` invoking `ContentResolver.loadThumbnail(Uri, Size, CancellationSignal)` on API 29+, letting the platform's hardware-accelerated transcoding engine manage generation.

### 6. Authoritative SQL Cursor Counts
- Calculating video/favorite counts via `allPhotos.count { it.isVideo }` starves the UI because `allPhotos` is paginated (200 items per page).
- `MediaStoreDataSource.queryMediaCount()` runs two independent `ContentResolver.query` calls projecting only `_ID`:
  - Videos: `MEDIA_TYPE = MEDIA_TYPE_VIDEO`
  - Favorites: `IS_FAVORITE = 1` (API 30+)
- `cursor.count` returns the instantaneous, authoritative total for the entire device in < 5ms.

### 7. Full-Library Filter Querying
- Filter chips (`VIDEOS`, `FAVORITES`, `SCREENSHOTS`) do not filter the in-memory 200-item page.
- Selecting a filter triggers `GalleryViewModel.setFilter(category)`, resetting pagination cursors (`offset = 0`) and injecting SQL selection clauses into `MediaStoreDataSource.queryPhotos()`, querying the entire device library with pagination.

### 8. Non-Blocking Gesture Architecture (`awaitEachGesture`)
- Standard Compose `detectTransformGestures` consumes horizontal touch slop even at 1x zoom, blocking `HorizontalPager` swipe gestures.
- `ZoomableImage` uses low-level `awaitEachGesture`:
  - When `zoomScale <= 1.05f`, horizontal drag delta is **not consumed**, allowing `HorizontalPager` to swipe naturally between photos.
  - When `zoomScale > 1.05f`, all touch events are consumed to pan the magnified image.

### 9. Navigation Chrome Isolation
- In traditional apps, bottom navigation bars linger on child screens, stealing vertical space and leaking state.
- SwipePix inspects `currentBackStackEntryAsState()`. The navigation pill and floating cleanup buttons are conditionally rendered **only** when `currentRoute == Screen.Photos.route || currentRoute == Screen.Albums.route`. Deep screens (`PhotoViewer`, `AlbumDetail`, `Cleanup`) receive a 100% immersive viewport.

### 10. Granular Permissions & Lifecycle Auto-Recovery
- Android 13+ splits storage into `READ_MEDIA_IMAGES` and `READ_MEDIA_VIDEO`. Partial access (e.g., granting images but denying video) is mapped to `PermissionState.Partial`.
- A Compose `LifecycleResumeEffect` automatically queries `checkPermissionState()` when the user returns from system Settings, updating UI state without requiring an app restart.

---

## 5. 10 Real Bugs Discovered & Concrete Fixes

### 1. Duplicate LazyGrid Key Crash (`IllegalArgumentException: Key was already used`)
- **Root Cause:** Multiple camera photos in fast burst mode or across camera/cloud directories shared identical timestamps or IDs across split queries; asynchronous pagination triggered concurrent appends without cancelling previous jobs.
- **Fix:** Implemented 4-layer deduplication contract: `seenUris` tracking in `MediaStoreDataSource`, job cancellation on query start in `GalleryViewModel`, Set deduplication in `DateUtils`, and stable String URI keys (`key = photo.contentUri.toString()`).

### 2. Old Photos Showing First (June 24 instead of Sept 16)
- **Root Cause:** Using `ContentResolver.QUERY_ARG_SORT_COLUMNS = arrayOf("date_taken", "date_modified")` without explicit directions caused Android's internal SQLite builder to default secondary columns to `ASC`, reversing sorting when primary dates were null or identical.
- **Fix:** Switched to explicit `QUERY_ARG_SQL_SORT_ORDER = "datetaken DESC, date_modified DESC, date_added DESC, _id DESC"` for both legacy and modern Android bundles.

### 3. Inaccurate Video and Favorite Header Counts
- **Root Cause:** Category counts were derived from in-memory `_uiState.value.photos.count { it.isVideo }`. Because `photos` was capped at 200 items by pagination, counts never exceeded 200 regardless of the library size.
- **Fix:** Created `MediaStoreDataSource.queryMediaCount()`, executing dedicated count queries using `cursor.count` on `MediaStore.Files` with SQL filters `MEDIA_TYPE = 3` and `IS_FAVORITE = 1`.

### 4. Double-Deny Permission Lockout
- **Root Cause:** `PermissionState` evaluator required both `hasImages && hasVideo` for `Granted`. Denying one resulted in `PermanentlyDenied` even if the user permitted the other. Returning from Settings did not refresh state.
- **Fix:** Handled partial grants gracefully (`PermissionState.Partial`) and integrated `LifecycleResumeEffect` to re-evaluate permissions whenever the app returns to the foreground.

### 5. Album Progress Bleeding into Other Albums
- **Root Cause:** `PhotoDecisionDao` queried session progress using `SELECT COUNT(*) FROM photo_decisions WHERE decision = 'KEEP'`, failing to filter by `albumId`. Swiping photos in "Screenshots" appeared as completed progress in "WhatsApp".
- **Fix:** Updated Room DAO queries with strict album scoping: `WHERE (albumId = :albumId OR (:albumId IS NULL AND albumId IS NULL))`.

### 6. R8 Release Crash on Navigation & Room Serialization
- **Root Cause:** R8 shrinking stripped Kotlinx Serialization companion objects and Room generated DAOs in release builds.
- **Fix:** Added explicit `-keepclassmembers` rules in `proguard-rules.pro` for `@Serializable` companions, Room entity fields, and Coil fetchers.

### 7. HorizontalPager Stalled by Zoom Gestures
- **Root Cause:** `Modifier.pointerInput` with `detectTransformGestures` intercepted all pointer events regardless of scale factor.
- **Fix:** Replaced with `awaitEachGesture`. If `scale <= 1.05f`, pointer consumption is bypassed, letting `HorizontalPager` handle page swipes.

### 8. Crossfade Black Screen Flash in Photo Viewer
- **Root Cause:** Enabling `crossfade(true)` in Coil's `AsyncImage` caused Coil to crossfade from transparent black to the full-res bitmap, causing a perceptible dark flicker even when a cached thumbnail existed.
- **Fix:** Set `crossfade(false)` in `ZoomableImage` and bridged the memory cache key directly via `.placeholderMemoryCacheKey("${uri}_256")`.

### 9. Filter Pagination Hard Capping
- **Root Cause:** Selecting "Videos" filtered the in-memory list of 200 items, producing only 12 items and never loading older videos from disk.
- **Fix:** Extended `MediaStoreDataSource.queryPhotos(filter: GalleryFilterCategory)` to append SQL where clauses directly to the MediaStore query bundle, enabling infinite pagination for any category.

### 10. Direct `.thumbnails` Directory Access Security Exception
- **Root Cause:** Attempting to parse `/storage/emulated/0/DCIM/.thumbnails/` directly threw `SecurityException` under Scoped Storage on Android 11+ and failed on Samsung Knox partitions.
- **Fix:** Abandoned filesystem scraping entirely and adopted Android's platform API `ContentResolver.loadThumbnail()` inside a custom Coil `Fetcher`.

---

## 6. 20 Realistic Senior Interview Questions & Model Answers

### Q1: Why did you choose Jetpack Compose over traditional XML views for SwipePix?
> **Model Answer:**  
> Compose drastically simplifies dynamic UI state representation. In SwipePix, UI states change rapidly—swipe deck translations, dynamic multi-column grids, and animated navigation pill transitions. With Compose, state changes in `StateFlow` trigger targeted recomposition of only affected composables. Furthermore, Compose's `LazyVerticalGrid` and `HorizontalPager` provide built-in sub-composition and view recycling without the boilerplate of `RecyclerView.Adapter` and `DiffUtil`.

### Q2: How does SwipePix adhere to Scoped Storage on Android 13+ (API 33+)?
> **Model Answer:**  
> SwipePix accesses media strictly through `ContentResolver` queries on `MediaStore.Files.getContentUri("external")` using granular permissions (`READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`). We never request broad `MANAGE_EXTERNAL_STORAGE` permissions or access raw filesystem paths (`/sdcard/`). For deletions, we call `MediaStore.createTrashRequest()`, delegating file modification confirmation to the Android system dialog.

### Q3: Why do you have both Room and MediaStore in the same application? Isn't that redundant?
> **Model Answer:**  
> Not at all; they serve two completely distinct responsibilities with zero duplication. `MediaStore` is the OS-level source of truth for physical media assets, timestamps, and actual file existence. `Room` acts purely as an internal staging and session engine. When a user swipes left to trash 50 photos, we don't immediately execute destructive OS file deletes. We record the decisions in Room as `TRASH_PENDING`. Only when the user explicitly taps "Review & Delete" do we read the staged IDs from Room and batch-execute the system `createTrashRequest`. This makes swiping instantaneous and 100% reversible.

### Q4: Explain how you prevented the duplicate key crash in `LazyVerticalGrid`.
> **Model Answer:**  
> We established a 4-layer deduplication contract. First, `MediaStoreDataSource` maintains a `seenUris: HashSet<Uri>` during pagination. Second, `GalleryViewModel` cancels any running coroutine job (`loadJob?.cancel()`) before initiating a new query to eliminate race conditions. Third, `DateUtils` groups items using Set operations. Fourth, `LazyVerticalGrid` items are keyed by `photo.contentUri.toString()` rather than numeric database IDs or timestamps, guaranteeing stable, collision-free keys across device partitions.

### Q5: How did you fix the bug where photos were sorted in reverse chronological order on some devices?
> **Model Answer:**  
> On Android 10+, `QUERY_ARG_SORT_COLUMNS` accepts a string array of column names, but Android's underlying SQLite provider defaults secondary sort columns to `ASC` if directions are unspecified. If `DATE_TAKEN` was null (common in screenshots or downloaded media), it sorted by `DATE_MODIFIED ASC`. We resolved this by supplying an explicit SQL string via `QUERY_ARG_SQL_SORT_ORDER`: `"datetaken DESC, date_modified DESC, date_added DESC, _id DESC"`, ensuring identical, deterministic descending order across all Android OEM variants.

### Q6: How does image caching work in SwipePix, and how do you prevent OOM with large photo libraries?
> **Model Answer:**  
> We use Coil 3 with a two-tier cache: an in-memory LRU cache limited to 25% of available app heap (`maxSizePercent(0.25)`) and a dedicated 250MB disk cache. In the grid, photos are loaded at 256px resolution via our custom `MediaStoreThumbnailFetcher`, which invokes platform `ContentResolver.loadThumbnail()`. We never decode full 48MP bitmaps into the grid. When opening the full viewer, we bridge the thumbnail cache key into `.placeholderMemoryCacheKey()`, ensuring instantaneous display without GC pressure.

### Q7: Why did you write a custom `MediaStoreThumbnailFetcher` instead of using Coil's default URI fetcher?
> **Model Answer:**  
> Coil's default URI fetcher opens an `InputStream` to the content URI and decodes the stream via Android's `BitmapFactory`. For high-resolution camera images, this requires reading file headers and subsampling in app process memory. On Android 10+ (API 29+), `ContentResolver.loadThumbnail(Uri, Size, CancellationSignal)` delegates thumbnail generation directly to the OS media service, which utilizes hardware acceleration and system-level thumbnail caches. Our custom fetcher intercepts URI requests $\le$ 512px and leverages this platform API, reducing decode latency by over 60%.

### Q8: How did you solve the gesture conflict between pinch-to-zoom and horizontal paging in `PhotoViewerScreen`?
> **Model Answer:**  
> Standard Compose `detectTransformGestures` consumes pointer drag deltas during initial touch slop calculation, which completely starves the parent `HorizontalPager`. We replaced it with low-level `awaitEachGesture`. Inside the gesture loop, we evaluate the current zoom scale: if `zoomScale <= 1.05f`, we do not consume the pointer event, allowing the `HorizontalPager` to swipe naturally between photos. When `zoomScale > 1.05f`, we consume the deltas and apply pan offsets, clamping boundaries to image dimensions.

### Q9: How does SwipePix handle Android 13+ Granular Media Permissions?
> **Model Answer:**  
> We evaluate `READ_MEDIA_IMAGES` and `READ_MEDIA_VIDEO` independently. We define a sealed class `PermissionState`: `Granted` (both permitted), `Partial` (one permitted), `Denied` (first time or can ask again), and `PermanentlyDenied` (user checked "Don't ask again"). Furthermore, we attach a `LifecycleResumeEffect` in the UI to re-evaluate permissions whenever the user returns from system settings, transitioning state seamlessly without requiring an app process kill.

### Q10: Why did you use `beyondViewportPageCount = 1` in `HorizontalPager`?
> **Model Answer:**  
> By default, `HorizontalPager` only composes and renders the currently visible page. When the user swipes horizontally, the next image must compose, fetch its thumbnail from cache, and decode on demand, causing a visible micro-stutter. Setting `beyondViewportPageCount = 1` retains one page ahead and one page behind in the composition hierarchy. The adjacent images pre-decode into the frame buffer, delivering an instantaneous 60fps swipe transition.

### Q11: How do authoritative media counts work in `GalleryViewModel`, and why not use the in-memory list?
> **Model Answer:**  
> SwipePix uses pagination (`PAGE_SIZE = 200`) to keep memory consumption constant regardless of whether the user has 500 or 50,000 photos. If we calculated video or favorite counts from `_uiState.value.photos.count { it.isVideo }`, the count would be truncated at 200. To provide real, instantaneous counts, `MediaStoreDataSource.queryMediaCount()` executes dedicated count queries using `cursor.count` on `MediaStore.Files` projecting only `_ID`. This returns in under 5ms without loading any bitmaps into RAM.

### Q12: How does the swipe cleanup card physics work in `CleanupScreen`?
> **Model Answer:**  
> `SwipeCardPhysics` models physical card dynamics using Compose `Animatable`. As the user drags, the card translates on the X and Y axes and rotates proportionally: `rotation = (dragOffset.x / screenWidth) * 20f`. When released, we check swipe velocity and displacement thresholds. If `abs(dragOffset.x) > threshold` or velocity exceeds 1000dp/s, the card flings off-screen via `animateTo` with a fast tween, and dispatches the decision (`KEEP` or `TRASH_PENDING`). Otherwise, it springs back to origin with `Spring.StiffnessMediumLow`.

### Q13: How does the reversible Undo mechanism work during a cleanup session?
> **Model Answer:**  
> When a photo is swiped, its decision is recorded in Room with `decision = TRASH_PENDING` and its ID is pushed to an in-memory stack. An `UndoToastCapsule` is displayed. If the user taps "Undo", the repository pops the last decision from the stack, deletes the record from `PhotoDecisionDao`, and pushes the media item back to the front of the ViewModel's card deck. Because physical deletion is deferred until final session commit, undo operations are 100% instantaneous and non-destructive.

### Q14: How is network privacy guaranteed in SwipePix?
> **Model Answer:**  
> Network privacy is enforced at the OS level: `AndroidManifest.xml` does not declare `android.permission.INTERNET`. Under the Android security sandbox, an application without this permission cannot open network sockets or transmit data. Furthermore, our Gradle build file contains zero HTTP client libraries (no Retrofit, OkHttp, or Ktor) and zero analytics or crash reporting SDKs. All operations are 100% on-device.

### Q15: How does SwipePix prevent UI lockups during heavy MediaStore queries?
> **Model Answer:**  
> All cursor iteration, content resolver queries, and Room database operations are dispatched to `Dispatchers.IO`. Date grouping and sorting algorithms run on `Dispatchers.Default`. The UI thread (`Dispatchers.Main.immediate`) only receives immutable data classes (`GalleryUiState`) via `StateFlow`. In Compose, we utilize `remember(photos)` and derived state to avoid recomputing date section headers on every frame.

### Q16: How do you handle R8 code shrinking and minification for release builds?
> **Model Answer:**  
> In release builds with `isMinifyEnabled = true` and `isShrinkResources = true`, R8 can aggressively strip reflection-dependent code. In SwipePix, we added explicit rules in `proguard-rules.pro`: preserving Kotlinx Serialization companion objects (`@kotlinx.serialization.Serializable class *`), Room generated DAO implementations, and Coil's custom `Fetcher.Factory`. We verified this by running `./gradlew assembleRelease` and confirming clean DEX generation.

### Q17: What is the purpose of `CleaningSessionEntity` in Room?
> **Model Answer:**  
> `CleaningSessionEntity` tracks the lifecycle of an optimization workflow (`sessionId`, `albumId`, `sourceType`, `startTime`, `completedTime`, `totalReviewed`, `totalBytesFreed`). This allows users to pause cleaning mid-way, exit the app, and resume later from the exact photo they left off without rescanning the entire gallery.

### Q18: Why is the global Bottom Navigation Bar hidden on `PhotoViewerScreen` and `CleanupScreen`?
> **Model Answer:**  
> Viewing individual media and reviewing swipe cards require high visual focus and maximal screen real estate. The bottom navigation bar and floating cleanup pill consume vertical space and introduce accidental touch targets. We isolate the global navigation chrome strictly to top-level routes (`Photos` and `Albums`), providing an edge-to-edge immersive experience on child screens.

### Q19: How do you handle videos in SwipePix? Do you decode video frames yourself?
> **Model Answer:**  
> In the grid, videos display a duration badge and thumbnail. We register Coil's `VideoFrameDecoder.Factory` in `CoilModule.kt`, which leverages Android's `MediaMetadataRetriever` to extract the frame at timestamp 0 without decoding the entire video stream. For full video playback, rather than bundling a heavy ExoPlayer/Media3 player into an offline photo cleaner, we dispatch an explicit `Intent(Intent.ACTION_VIEW)` with `FLAG_GRANT_READ_URI_PERMISSION`, delegating playback to the user's preferred native media player.

### Q20: What are the current architectural limitations of SwipePix and how would you evolve them?
> **Model Answer:**  
> 1. *Cursor Pagination Window:* `MediaStore` cursor pagination currently uses SQL `OFFSET / LIMIT`. In extremely large libraries (50,000+ items), high offsets can slow cursor seek time. We could evolve to keyset pagination using `DATE_TAKEN < :lastDate AND _ID < :lastId`.  
> 2. *Automated AI Clustering:* While manual swiping is fast, adding on-device ML (e.g., TensorFlow Lite or MediaPipe) to cluster duplicate burst shots and detect blurry photos would allow users to auto-select low-quality photos before swiping.  
> 3. *Cloud-Synced Trash State:* Because we rely purely on Android's native MediaStore Trash, photos synced to Google Photos may prompt secondary confirmations in Google Photos' own cloud trash bin.

---

## 7. MediaStore Architecture Quick Reference

```
MediaStore.Files.getContentUri("external")
│
├── Projections:
│   ├── _ID, DISPLAY_NAME, DATA, MIME_TYPE
│   ├── DATE_TAKEN, DATE_MODIFIED, DATE_ADDED
│   ├── SIZE, WIDTH, HEIGHT, DURATION
│   ├── MEDIA_TYPE (1 = Image, 3 = Video)
│   ├── BUCKET_ID, BUCKET_DISPLAY_NAME
│   └── IS_FAVORITE (API 30+)
│
├── Sort Order (Universal Descending):
│   "datetaken DESC, date_modified DESC, date_added DESC, _id DESC"
│
└── Queries:
    ├── queryPhotos(offset, limit, filter) ──> List<MediaItem> (Chunked 200)
    ├── queryMediaCount() ──────────────────> AlbumMediaCount (Total, Videos, Favorites)
    └── queryAlbums() ──────────────────────> List<Album> (Grouped by BUCKET_ID)
```

---

## 8. Coil 3 Pipeline Quick Reference

```
AsyncImage(model = ImageRequest.Builder(context)...)
│
├── Memory Cache Key: "${contentUri}_256" (Grid) or "${contentUri}_full" (Viewer)
├── Placeholder Key:  "${contentUri}_256" (Instant Frame-0 Paint in Viewer)
│
└── Fetcher Resolution Hierarchy:
    1. MediaStoreThumbnailFetcher (Custom, Size <= 512px)
       └── Calls ContentResolver.loadThumbnail(Uri, Size, null) [API 29+]
           └── Returns ImageFetchResult(Bitmap, isSampled = true, DataSource.DISK)
    2. VideoFrameDecoder (Coil, for video MIME types)
       └── Extracts frame at time 0 via MediaMetadataRetriever
    3. Default File/Uri Fetcher
       └── Decodes full-res bitmap stream for PhotoViewer
```
