# SwipePix — Complete Technical Knowledge & Architectural Specification

**Role:** Senior Android Engineer & Software Architect  
**Project:** SwipePix (Android 13+ / API 33–35)  
**Authoritative Reference:** Current Codebase (Post-Phase 33)

---

## 1. Project Overview

SwipePix is a high-performance, offline, privacy-first Android gallery and storage optimization utility designed to let users rapidly clean hundreds or thousands of photos and videos through an intuitive swipe interface. 

Unlike traditional cloud-tethered gallery cleaners that compromise privacy or require subscriptions, SwipePix operates **100% locally on the device**. It possesses **zero network permissions** (`INTERNET` is omitted from `AndroidManifest.xml`), makes no external network calls, includes no analytics trackers, and directly integrates with the Android platform's native `MediaStore` APIs and `Room` persistence engine.

### Key Metrics & Environment
- **Minimum SDK:** API 33 (Android 13, Tiramisu)
- **Target SDK:** API 35 (Android 15, Vanilla Ice Cream)
- **Compile SDK:** 37
- **Language & Runtime:** Kotlin 2.2.10, JVM 17
- **UI Toolkit:** Jetpack Compose (Compose BOM 2026.08.00, Material 3 1.3.1)
- **Dependency Injection:** Dagger Hilt 2.60.1
- **Local Persistence:** Room 2.6.1 + Jetpack DataStore Preferences 1.1.7
- **Media Engine:** Android MediaStore (`MediaStore.Files`) + Coil 3.6.2 (Custom Platform Fetcher)

---

## 2. Problem Statement

Modern smartphone users capture dozens of photos and videos daily—burst shots, duplicate screenshots, WhatsApp forwarded memes, and temporary documents. This results in bloated device storage and cluttered gallery grids.

Existing solutions in the Android ecosystem suffer from severe architectural and usability shortcomings:
1. **Cloud & Privacy Risks:** Most cleaner apps upload media metadata, thumbnails, or whole photos to third-party servers for "AI analysis".
2. **Destructive/Risky Deletions:** Many utilities attempt direct filesystem `File.delete()` or raw `.thumbnails` cache purging, violating Scoped Storage, triggering OS permission errors, or permanently destroying files without user confirmation.
3. **Sluggish, Memory-Heavy Grids:** Loading full-resolution (12MP–48MP) camera photos directly into scrollable lists leads to intense garbage collection (GC) thrashing, dropped frames, and `OutOfMemoryError` (OOM).
4. **Tedious Manual Triage:** Traditional multi-select checkboxes in galleries are friction-heavy. Reviewing 500 photos requires 500 individual taps and cognitive decisions.

**SwipePix's Solution:**  
Provide a native, fluid swipe gesture interface (Tinder-style card deck) backed by hardware-accelerated MediaStore thumbnail queries, an isolated Room session staging engine, and native system-level Android Trash dialogs (`MediaStore.createTrashRequest`).

---

## 3. Features & Status Matrix

| Feature | Implementation Status | Implementation Details / Classes |
|---|---|---|
| **4-Column Chronological Gallery** | ✅ **IMPLEMENTED** | `GalleryScreen.kt`, `PhotoGrid.kt`, `MediaStoreDataSource.queryPhotos` |
| **Date-Grouped Sections** | ✅ **IMPLEMENTED** | `DateUtils.groupPhotosByDate`, local date grouping in device timezone |
| **Media Filter Categories** | ✅ **IMPLEMENTED** | `ALL`, `VIDEOS`, `FAVORITES`, `SCREENSHOTS` via SQL selection in `MediaStoreDataSource` |
| **Authoritative Media Counts** | ✅ **IMPLEMENTED** | `MediaStoreDataSource.queryMediaCount()` using SQL cursor counts (`MEDIA_TYPE_VIDEO`, `IS_FAVORITE = 1`) |
| **2-Column Album Discovery** | ✅ **IMPLEMENTED** | `AlbumsScreen.kt`, `MediaStoreDataSource.queryAlbums()` |
| **Album Detail & Dedicated Viewer** | ✅ **IMPLEMENTED** | `AlbumDetailScreen.kt`, `ViewPhotosScreen.kt`, `PhotoViewerScreen.kt` |
| **Pinch-to-Zoom & Pan Fullscreen Viewer** | ✅ **IMPLEMENTED** | `ZoomableImage.kt`, `awaitEachGesture` handling zoom, pan, and double-tap |
| **Adjacent Page Preloading** | ✅ **IMPLEMENTED** | `PhotoViewerScreen.kt`, `HorizontalPager(beyondViewportPageCount = 1)` |
| **Progressive Thumbnail $\to$ Fullres** | ✅ **IMPLEMENTED** | `ZoomableImage.kt`, `.placeholderMemoryCacheKey("${uri}_256")` |
| **Platform Hardware Thumbnail Cache** | ✅ **IMPLEMENTED** | `MediaStoreThumbnailFetcher.kt`, `ContentResolver.loadThumbnail()` (API 29+) |
| **Video Badge & Poster Rendering** | ✅ **IMPLEMENTED** | `PhotoThumbnail`, `CoilModule.kt`, `VideoFrameDecoder.Factory` |
| **Native Video Playback Trigger** | ✅ **IMPLEMENTED** | `ZoomableImage.kt`, `Intent(Intent.ACTION_VIEW)` with read URI permission |
| **Swipe Card Cleanup Deck** | ✅ **IMPLEMENTED** | `CleanupScreen.kt`, `SwipeableCard.kt`, `SwipeCardPhysics.kt` |
| **Mid-Session Pause & Reversible Undo** | ✅ **IMPLEMENTED** | `UndoToastCapsule.kt`, `CleaningPauseDialog.kt`, `PhotoDecisionDao` |
| **Batch Android Trash Request** | ✅ **IMPLEMENTED** | `MediaStore.createTrashRequest()`, batch system confirmation dialog |
| **Trash Management & Restore** | ✅ **IMPLEMENTED** | `TrashScreen.kt`, `MediaStore.createTrashRequest(items, false)` for restore |
| **Dark / Light / Liquid Glass Theme** | ✅ **IMPLEMENTED** | `Theme.kt`, `Glass.kt`, `UserPreferencesRepository.kt` |
| **Centralized Motion Tokens** | ✅ **IMPLEMENTED** | `SwipePixMotion.kt`, standardized fast/standard/emphasis tweens & springs |
| **Navigation Chrome Isolation** | ✅ **IMPLEMENTED** | Global bottom bar isolated strictly to `Photos` and `Albums` |
| **Duplicate Key Deduplication Contract** | ✅ **IMPLEMENTED** | 4-layer deduplication (`MediaStoreDataSource`, ViewModel, `DateUtils`, Compose keys) |
| **AI Blur / Duplicate Clustering** | ⏳ **PLANNED ONLY** | Documented for v2.0 roadmap; no on-device ML models currently bundled |
| **Cloud Backup / Sync** | ❌ **NOT IMPLEMENTED** | Explicitly out of scope; SwipePix is strictly offline |

---

## 4. Architecture

SwipePix strictly follows **Google's Recommended Modern Android Architecture (MAD)** using the **MVVM (Model-View-ViewModel)** architectural pattern combined with the **Repository Pattern** and **Unidirectional Data Flow (UDF)**.

### Architectural Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        Jetpack Compose UI                       │
│  MainGalleryShell │ PhotoGrid │ PhotoViewer │ CleanupCoordinator │
└───────────────────────────────▲─────────────────────────────────┘
                                │ StateFlow<UiState> / Events
┌───────────────────────────────┴─────────────────────────────────┐
│                           ViewModels                            │
│  GalleryViewModel │ AlbumsViewModel │ CleanupViewModel │ Viewer │
└───────────────────────────────▲─────────────────────────────────┘
                                │ Coroutine suspend / Result<T> / Flow
┌───────────────────────────────┴─────────────────────────────────┐
│                        Repository Layer                         │
│       MediaRepositoryImpl      │  CleanupSessionRepositoryImpl  │
└───────────────────▲─────────────────────────────▲───────────────┘
                    │                             │
       ┌────────────┴────────────┐   ┌────────────┴────────────┐
       │     Data Source Layer   │   │    Local Room Database  │
       │   MediaStoreDataSource  │   │     SwipePixDatabase    │
       │ MediaStoreThumbnailFetch│   │PhotoDecisionDao│SessionDao│
       └────────────▲────────────┘   └────────────▲────────────┘
                    │                             │
┌───────────────────┴─────────────────────────────┴───────────────┐
│                    Android OS & Local Device                    │
│   MediaStore (SQLite ContentProvider) │ Filesystem │ DataStore  │
└─────────────────────────────────────────────────────────────────┘
```

### Architectural Separation of Concerns
1. **UI Layer (Compose):** Passive, declarative views. Never executes business logic, never executes SQL, and never directly communicates with `ContentResolver`. Observes `StateFlow<UiState>` using `collectAsStateWithLifecycle()`.
2. **ViewModel Layer:** Survives configuration changes (`SavedStateHandle`). Manages presentation state, orchestrates coroutines (`viewModelScope`), catches domain errors, and maps repository data into immutable UI states.
3. **Repository Layer:** Single source of truth boundary. Coordinates between transient device media (`MediaStoreDataSource`), persistent cleaning state (`Room`), and user settings (`DataStore`). Exposes suspend functions returning `Result<T>` and reactive `Flow<T>`.
4. **Data Source Layer:** Direct, low-level platform APIs. Handles Android cursors, projections, SQL selections, `ContentResolver` queries, batch intent generation, and exception encapsulation.

---

## 5. Complete Project Structure

```
app/src/main/java/in/heyvinay/swipepix/
├── MainActivity.kt                       # Single activity entry point; edge-to-edge; theme observation
├── SwipePixApp.kt                        # Hilt Application class
├── data/
│   ├── datasource/
│   │   ├── MediaStoreDataSource.kt       # Low-level ContentResolver queries for images, videos, albums, trash
│   │   └── MediaStoreThumbnailFetcher.kt # Custom Coil 3 fetcher utilizing ContentResolver.loadThumbnail
│   ├── local/
│   │   ├── SwipePixDatabase.kt           # Room Database definition (v1)
│   │   ├── dao/
│   │   │   ├── CleaningSessionDao.kt     # DAO for active and historical cleanup sessions
│   │   │   └── PhotoDecisionDao.kt       # DAO for KEEP / TRASH_PENDING / TRASHED swipe decisions
│   │   └── entity/
│   │       ├── CleaningSessionEntity.kt  # Room table for session lifecycle and last reviewed item
│   │       └── PhotoDecisionEntity.kt    # Room table for individual media decisions with album scoping
│   ├── model/
│   │   ├── Album.kt                      # Domain model representing media folders/buckets
│   │   ├── AlbumMediaCount.kt            # Authoritative total, video, and favorite counts
│   │   ├── GalleryFilterCategory.kt      # Filter enum (ALL, VIDEOS, FAVORITES, SCREENSHOTS)
│   │   ├── MediaItem.kt                  # Domain model for photo/video with effectiveTimestamp
│   │   └── MediaType.kt                  # Enum: IMAGE, VIDEO
│   ├── permissions/
│   │   └── PermissionState.kt            # Permission state machine (Unknown, Checking, Granted, Partial, Denied)
│   ├── preferences/
│   │   └── UserPreferencesRepository.kt  # Jetpack DataStore Preferences for app theme and settings
│   └── repository/
│       ├── CleanupSessionRepository.kt   # Contract for cleanup sessions and staged decisions
│       ├── CleanupSessionRepositoryImpl.kt
│       ├── MediaRepository.kt            # Contract for gallery media, counts, and Android Trash operations
│       └── MediaRepositoryImpl.kt
├── di/
│   ├── AppModule.kt                      # Context & ContentResolver injection
│   ├── CoilModule.kt                     # Custom ImageLoader with 25% heap memory cache & thumbnail fetcher
│   ├── DatabaseModule.kt                 # Room database and DAO providers
│   └── RepositoryModule.kt               # Binds Repository interfaces to implementations
├── ui/
│   ├── albums/                           # Album grid, Album Detail, and View Photos screens & ViewModels
│   ├── cleanup/                          # Tinder-style card deck, swipe physics, undo capsule, pause dialog
│   ├── components/                       # Shared UI: PhotoGrid, GalleryHeader, SwipePixBottomBar, StateScreens
│   ├── gallery/                          # MainGalleryShell, GalleryScreen, GalleryViewModel, DateGroup
│   ├── navigation/                       # SwipePixNavGraph, SwipePixRoute (type-safe serializable routes)
│   ├── onboarding/                       # Welcome and initial permission explanation screen
│   ├── permissions/                      # Dedicated permission request screen & ViewModel
│   ├── settings/                         # App settings screen (theme toggles, cleanup preferences)
│   ├── theme/                            # Color, Glass primitives, Theme, Typography, SwipePixMotion
│   ├── trash/                            # Dedicated Trash screen with restore & delete permanently actions
│   ├── util/                             # DateUtils (formatting, relative date headers, grouping)
│   └── viewer/                           # PhotoViewerScreen, PhotoViewerViewModel, ZoomableImage
```

---

## 6. Technology Stack Deep Dive

### 1. Jetpack Compose & Material 3
- **What it is:** Modern declarative UI toolkit for Android.
- **Why SwipePix uses it:** Enables reactive UI that responds directly to StateFlow changes, eliminates XML boilerplate, and allows fluid gesture physics (`pointerInput`, `detectTransformGestures`, `awaitEachGesture`).
- **Interview Question:** *"Why use Compose over XML for a media app?"*  
  *Answer:* Compose eliminates `findViewById` and View hierarchy overhead. In a media app with dynamic date headers and responsive grid sizing, Compose's declarative state model and lazy layouts (`LazyVerticalGrid`) ensure efficient recomposition without the view inflation penalties of RecyclerView.

### 2. Android MediaStore (`MediaStore.Files`)
- **What it is:** Android's platform database containing metadata for all shared external storage media.
- **Why SwipePix uses it:** Direct source of truth for all media on the device. Complies with Scoped Storage (Android 10+).
- **Alternative:** Direct java.io.`File` scraping was rejected because Scoped Storage restricts filesystem access without `MANAGE_EXTERNAL_STORAGE`, which Google Play prohibits for standard gallery applications.

### 3. Coil 3 (`io.coil-kt.coil3`)
- **What it is:** Kotlin-first image loading library built on Kotlin Coroutines.
- **Why SwipePix uses it:** Coil 3 supports custom `Fetcher` plugins (`MediaStoreThumbnailFetcher`), native Android video frame decoding (`VideoFrameDecoder`), memory cache keys, disk caching, and seamless Jetpack Compose integration via `AsyncImage`.

### 4. Room Persistence Library
- **What it is:** SQLite object mapping abstraction library.
- **Why SwipePix uses it:** Tracks offline cleanup sessions, staged `TRASH_PENDING` decisions, and undo history across app restarts without mutating the physical media until the user confirms.

### 5. Dagger Hilt
- **What it is:** Google's standardized dependency injection library for Android built on Dagger.
- **Why SwipePix uses it:** Eliminates manual factory boilerplate for ViewModels (`@HiltViewModel`), scopes database singletons to the Application lifecycle, and allows mock injection during unit tests.

---

## 7. App Startup & Navigation Lifecycle Flow

```
[App Launch: Cold Start]
       │
       ▼
SwipePixApp (@HiltAndroidApp)
       │  Initializes Dagger Hilt Component Tree
       ▼
MainActivity (ComponentActivity)
       │  1. enableEdgeToEdge()
       │  2. Collects UserPreferencesRepository.appTheme
       │  3. Renders SwipePixTheme { SwipePixNavGraph }
       ▼
SwipePixNavGraph
       │
       ├── LifecycleResumeEffect: Checks PermissionState.checkPermissionStatus(context)
       │
       ├── [State: Denied or Required] ───► Navigates to SwipePixRoute.Permission
       │                                     └── Renders OnboardingScreen
       │                                           └── Request READ_MEDIA_IMAGES / VIDEO
       │
       └── [State: Granted or Partial] ───► Navigates to SwipePixRoute.Gallery
                                             └── Mounts MainGalleryShell
                                                   ├── Pre-selects Photos tab (initialTab)
                                                   └── Launches GalleryViewModel.init & loadInitial()
```

---

## 8. Permission System & "Double Deny" Remediation

### Permission Spectrum Across Android Versions
- **API 33+ (Android 13+):** Requires granular `READ_MEDIA_IMAGES` and `READ_MEDIA_VIDEO`. Legacy `READ_EXTERNAL_STORAGE` is ignored.
- **API 34+ (Android 14+):** Introduces Partial Access via `READ_MEDIA_VISUAL_USER_SELECTED`. The user can grant access to a limited subset of photos via the platform photo picker.

### The "Double Deny $\to$ Grant" Production Bug
- **The Bug:** If a user denied permission twice, the system dialog suppressed further prompts (`shouldShowRequestPermissionRationale == false`). The user navigated to **Android System Settings $\to$ App Permissions**, manually enabled "Photos", and returned to SwipePix. However, SwipePix remained permanently locked on the Permission screen.
- **Root Cause:** 
  1. `PermissionState.checkPermissionStatus` evaluated `hasImages && hasVideo` with strict conjunction. If the user granted access to Photos but kept Videos denied, SwipePix classified the state as `Denied`.
  2. The navigation graph only evaluated permissions in `onCreate` or `LaunchedEffect(Unit)`, failing to re-check when the user returned from background Settings.
- **The Fix:**
  1. Updated `PermissionState.kt` to treat single-type grants as `PermissionStatus.Partial`:
     ```kotlin
     hasImages || hasVideo -> PermissionStatus.Partial
     ```
  2. Implemented Compose's `LifecycleResumeEffect(Unit)` in `SwipePixNavGraph.kt`. Whenever the app receives `ON_RESUME`, it queries `PermissionState.checkPermissionStatus(context)`. If status transitioned to `Granted` or `Partial`, it automatically triggers:
     ```kotlin
     navController.navigate(SwipePixRoute.Gallery) {
         popUpTo(SwipePixRoute.Permission) { inclusive = true }
     }
     ```

---

## 9. MediaStore Query Architecture

SwipePix queries `MediaStore.Files.getContentUri("external")` directly via `ContentResolver`.

### Exact Projection Columns
```kotlin
val projection = arrayOf(
    MediaStore.MediaColumns._ID,
    MediaStore.MediaColumns.DISPLAY_NAME,
    MediaStore.MediaColumns.MIME_TYPE,
    MediaStore.MediaColumns.SIZE,
    MediaStore.MediaColumns.DATE_TAKEN,
    MediaStore.MediaColumns.DATE_MODIFIED,
    MediaStore.MediaColumns.DATE_ADDED,
    MediaStore.MediaColumns.WIDTH,
    MediaStore.MediaColumns.HEIGHT,
    MediaStore.MediaColumns.MEDIA_TYPE,
    MediaStore.MediaColumns.BUCKET_ID,
    MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
    MediaStore.MediaColumns.DURATION,     // API 29+
    MediaStore.MediaColumns.IS_FAVORITE,  // API 30+
    MediaStore.MediaColumns.IS_TRASHED,   // API 30+
)
```

### Chronological Sort Order Architecture
In Android SQLite, `ContentResolver.createSqlSortClause` defaults unannotated sort columns to `ASC`. To prevent reverse chronological truncation where page 1 returned the oldest photos from 2015, SwipePix passes an explicit multi-column sort clause:
```kotlin
val sortOrder = "${MediaStore.MediaColumns.DATE_TAKEN} DESC, " +
                "${MediaStore.MediaColumns.DATE_MODIFIED} DESC, " +
                "${MediaStore.MediaColumns.DATE_ADDED} DESC, " +
                "${MediaStore.MediaColumns._ID} DESC"
```

---

## 10. Media Identity & The P0 Duplicate-Key Crash

### The Crash
```
java.lang.IllegalArgumentException: Key "1000138102" was already used.
at androidx.compose.foundation.lazy.layout.LazyLayoutMeasuredItemProvider...
at androidx.compose.foundation.lazy.grid.LazyGridMeasure...
```

### Why Did This Happen?
1. **Multi-Volume Collisions:** Android devices with SD cards or multiple storage partitions can generate identical raw integer `_ID` values across `internal` and `external` volumes.
2. **Cursor Shift During Pagination:** While paging with `LIMIT 200 OFFSET 200`, if new media was taken or modified in the background, cursor offsets shifted, causing items to appear in both Page 1 and Page 2.
3. **Lifecycle Concurrency Race:** `init { loadInitial() }` overlapped with `LifecycleResumeEffect { refresh() }`, appending results to an existing list without canceling the in-flight query.

### The 4-Layer Defensive Remediation Contract
1. **Layer 1 (MediaStore Data Source):** `seenUris = mutableSetOf<Uri>()` deduplicates rows during cursor traversal before domain models are emitted.
2. **Layer 2 (ViewModel Concurrency & Job Cancellation):** Dedicated `loadJob: Job?` cancelled before launching `loadMore()`. Paged lists apply `.distinctBy { it.contentUri.toString() }`.
3. **Layer 3 (Date Grouping Pipeline):** `DateUtils.groupPhotosByDate` merges photos using Set-based URI tracking.
4. **Layer 4 (Compose Lazy Layouts):** Keys are strictly declared as `key = { photo.contentUri.toString() }` rather than bare Long IDs. Additionally, grids filter duplicate keys with `seenGridKeys.add(it.contentUri.toString())`.

---

## 11. Pagination vs. Authoritative Media Counts

### The Trap: Calculating Counts from In-Memory Paged Lists
In early phases, the gallery header displayed:
```kotlin
val totalPhotos = allPhotos.size
val videoCount = allPhotos.count { it.isVideo }
val favoriteCount = allPhotos.count { it.isFavorite }
```
Because `allPhotos` was paginated (`PAGE_SIZE = 200`), any library with > 200 photos truncated counts. A device with 3,417 photos and 277 videos displayed `200 photos • 0 videos`!

### The Solution: Direct SQL Cursor Count Queries
`MediaStoreDataSource.queryMediaCount()` runs targeted projection queries using `cursor.count`:
- **Total Library Count:** `selection = "media_type IN (1, 3)"` (Image & Video)
- **Video Count:** `selection = "media_type = 3"` (`MEDIA_TYPE_VIDEO`)
- **Favorite Count:** `selection = "is_favorite = 1"` (`MediaStore.MediaColumns.IS_FAVORITE`)
- **Album-Specific Counts:** Appends `AND bucket_id = ?`

```kotlin
// Conceptual implementation in MediaStoreDataSource:
val countUri = MediaStore.Files.getContentUri("external")
context.contentResolver.query(countUri, arrayOf(MediaStore.MediaColumns._ID), selection, args, null)?.use { cursor ->
    cursor.count // Authoritative total across entire device in < 5ms
}
```

---

## 12. Thumbnail Pipeline & Android `.thumbnails` Architectural Decision

### Why Loading Original Images into a Grid is Fatal
A typical modern smartphone image is 4000x3000 pixels (12 megapixels). In Android memory (`ARGB_8888`), uncompressed:
$$4000 \times 3000 \times 4 \text{ bytes} \approx 48 \text{ MB per bitmap}$$
Displaying a 4-column grid with 20 visible items would consume $20 \times 48\text{ MB} \approx 960\text{ MB}$, instantly crashing with `OutOfMemoryError`.

### The Decision: Rejecting Direct `.thumbnails` Directory Scraping
- **Scoped Storage Lockdown:** On Android 10+ (API 29+), accessing `/sdcard/DCIM/.thumbnails` is forbidden without `MANAGE_EXTERNAL_STORAGE`.
- **OEM Inconsistency:** The `.thumbnails` directory layout is an internal OEM implementation detail that differs between Samsung OneUI, Google Pixel, and Xiaomi HyperOS.
- **Modern Solution:** SwipePix implements a custom Coil 3 fetcher: `MediaStoreThumbnailFetcher.kt`. On API 29+, it calls the official platform API:
  ```kotlin
  context.contentResolver.loadThumbnail(uri, Size(256, 256), null)
  ```
  Android's underlying MediaProvider handles hardware-accelerated downsampling and caches the compressed thumbnail internally.

---

## 13. Coil 3 Caching Architecture

Configured in `di/CoilModule.kt`:
1. **Memory Cache:** Configured to consume **25% of available JVM heap**:
   ```kotlin
   MemoryCache.Builder(context).maxSizePercent(0.25).build()
   ```
2. **Disk Cache:** Dedicated **250 MB directory** in `context.cacheDir.resolve("image_cache")`.
3. **Deterministic Cache Keys:**
   - Grid thumbnails: `${photo.contentUri}_256`
   - Fullscreen images: `${photo.contentUri}_full`
4. **Progressive Display Bridge:** `ZoomableImage` sets:
   ```kotlin
   ImageRequest.Builder(context)
       .data(mediaItem.contentUri)
       .placeholderMemoryCacheKey("${mediaItem.contentUri}_256")
       .memoryCacheKey("${mediaItem.contentUri}_full")
       .crossfade(false)
       .build()
   ```
   When tapping a grid item to open the fullscreen viewer, Coil renders the in-memory 256px thumbnail on **Frame 0 (0ms latency)** while decoding the full-resolution bitmap asynchronously, eliminating white/black loading flashes.

---

## 14. Room Database & Cleaning Session Architecture

### Database Schema
- **`photo_decisions` Table:**
  - `mediaStoreId` (Long, Primary Key, Indexed)
  - `decision` (String: `KEEP`, `TRASH_PENDING`, `TRASHED`)
  - `contentUri` (String)
  - `albumId` (String?, Indexed)
  - `sessionId` (String?, Indexed)
  - `timestamp` (Long)
- **`cleaning_sessions` Table:**
  - `sessionId` (String, Primary Key)
  - `albumId` (String?, Nullable for Full Library)
  - `albumName` (String?)
  - `startedAt` (Long), `updatedAt` (Long)
  - `status` (String: `ACTIVE`, `PAUSED`, `COMPLETED`)
  - `lastReviewedMediaStoreId` (Long?)

### Album Progress Isolation Bug Remediation
- **The Bug:** Reviewing photos in "WhatsApp" album caused "Camera" album to show reviewed counts and skip photos.
- **The Fix:** Scoped all DAO count queries and decision queries by `albumId`:
  ```sql
  SELECT COUNT(*) FROM photo_decisions 
  WHERE decision = :decision 
    AND (albumId = :albumId OR (:albumId IS NULL AND albumId IS NULL))
  ```

---

## 15. Android Native Trash (`MediaStore.createTrashRequest`)

### System Trash vs. Permanent Delete
SwipePix never deletes media silently. On API 30+, it delegates to Android's native scoped trash mechanism:
```kotlin
val intentSender = MediaStore.createTrashRequest(
    contentResolver,
    listOfUris,
    true // true = Trash, false = Restore
).intentSender
```
1. The app launches `ActivityResultContracts.StartIntentSenderForResult()`.
2. The Android OS displays the official modal: *"Allow SwipePix to move 45 items to trash?"*
3. If approved, Android sets `IS_TRASHED = 1`. Items remain in the system trash for 30 days before OS purge.
4. On `Activity.RESULT_OK`, SwipePix updates Room decisions to `TRASHED`.

---

## 16. Navigation Chrome Isolation & Motion Design

### Navigation Chrome Rule
Global navigation chrome (`SwipePixBottomBar` consisting of Photos/Albums tab pill and Clean Up FAB) must be shown **ONLY** on:
- `SwipePixRoute.Gallery` (Photos and Albums tabs)

It is **strictly hidden** on:
- `AlbumDetail`, `ViewPhotos`, `PhotoViewer`, `CleanupSourceSelection`, `Cleanup`, `Trash`, `Settings`, `Permission`.

### Motion Design System (`SwipePixMotion.kt`)
- `DURATION_FAST = 120ms` (toggles, chrome fade)
- `DURATION_STANDARD = 220ms` (screen transitions, dialogs)
- `DURATION_EMPHASIS = 300ms` (card elevation)
- `SPRING_TACTILE`: Damping 0.82, stiffness 380f for button presses (`scale = 0.95f`).
- `PhotoViewer` Transition: `fadeIn(220ms) + scaleIn(0.92f)` on enter; `fadeOut(120ms) + scaleOut(0.92f)` on exit.

---

## 17. Security & Privacy Architecture

- **Zero Network Exposure:** No network permissions in manifest. No HTTP clients (no Retrofit, OkHttp, Ktor).
- **Zero Third-Party Trackers:** No Firebase Analytics, no Crashlytics, no ad networks.
- **Local Storage Scoping:** Staged swipe decisions are stored in internal app SQLite database (`/data/data/in.heyvinay.swipepix/databases/swipepix.db`).
- **Scoped Storage Compliance:** All media mutations pass through `ContentResolver` and system permission contracts.

---

## 18. Testing & Verification Summary

### Automated Unit Test Suites
SwipePix features **40 automated unit tests** (100% pass rate) across 10 test suites:
1. `MediaItemTest` (4 tests): Effective timestamp normalization (seconds to ms conversion).
2. `PermissionStateTest` (3 tests): State evaluation across Granted, Partial, and Denied.
3. `CleanupSessionTest` (9 tests): Session lifecycle, state machine, and decision staging.
4. `CleanupSourceSelectionViewModelTest` (3 tests): Album source selection and count mapping.
5. `GalleryViewModelTest` (3 tests): Authoritative count publishing and filter switching.
6. `NavigationChromeTest` (2 tests): Architectural verification of global navigation chrome visibility.
7. `SwipePixMotionTest` (3 tests): Motion token durations and easing specifications.
8. `TrashViewModelTest` (3 tests): Trashed media loading, restore, and delete flows.
9. `DateUtilsTest` (9 tests): Relative date headers, timezone grouping, deduplication.
10. `ExampleUnitTest` (1 test): JVM environment smoke test.

### Build Verification
- `compileDebugKotlin`: PASS
- `assembleDebug`: PASS
- `assembleRelease` (with R8 minification & resource shrinking): PASS
