# SwipePix — Official Website Copy & Content Blueprint

**Official URL:** [https://swipepix.heyvinay.in/](https://swipepix.heyvinay.in/)  
**Application ID:** `in.heyvinay.swipepix`  
**Current Version:** 1.0 (Build 1)  
**Supported Platforms:** Android 13, 14, 15 (API 33–35)  
**Tone & Style:** Confident, human, technically rigorous, direct. Zero generic SaaS fluff. Zero empty buzzwords.

---

## 1. Hero & Value Proposition

### Primary Headline
**CLEAN YOUR GALLERY.**  
**ONE SWIPE AT A TIME.**

### Subheadline
SwipePix is a local-first Android gallery and storage cleaner. Triage thousands of photos with fluid swipe gestures or long-press multi-selection — 100% offline, zero cloud uploads, zero trackers, next-gen photo viewer, and protected by native Android system trash.

### Badges / Stickers
- `100% OFFLINE`
- `NO CLOUD`
- `NO TRACKERS`
- `ANDROID 13+`
- `SYSTEM TRASH SAFE`

### Primary Calls to Action
- **Primary:** `DOWNLOAD APK v1.0` (Routes to `/updates`)
- **Secondary:** `EXPLORE FEATURES` (Smooth-scrolls to `#features`)
- **Tertiary:** `TRY INTERACTIVE DEMO` (Smooth-scrolls to `#demo`)

---

## 2. Product Positioning

Smartphone galleries don't become messy because users are lazy. They become messy because camera sensors take 48-megapixel photos, messaging apps save every meme twice, and default gallery apps require tapping microscopic checkboxes across an endless grid.

Most cleaner utilities in the Play Store make things worse:
- They plaster intrusive full-screen video ads.
- They upload thumbnails to remote servers for "cloud AI analysis."
- They demand dangerous broad storage permissions.
- Or they execute silent, unrecoverable file deletions.

SwipePix takes the opposite approach:
1. **Physical Card Velocity:** Rapid one-by-one visual triage inspired by swipe mechanics.
2. **True Local-First Architecture:** The app does not even declare the Android `INTERNET` permission.
3. **OS-Level Deletion Safety:** Files are never destroyed silently; they are passed to Android's official system trash dialog with a 30-day recovery safety net.

---

## 3. The Problem: The Modern Storage Trap

### Headline
**Your phone's storage is full. Your gallery app isn't helping.**

### The Reality
- **Burst Photos & Duplicates:** You take five shots of the same moment. You only need one. The other four sit in storage forever.
- **Accidental Screen Grabs:** Screenshots taken to share a single address or meme remain forgotten in your camera roll.
- **Micro-Checkbox Hell:** To delete 300 photos in default gallery apps, you must scroll, squint, tap tiny circles, and pray you didn't accidentally select a keepsake.
- **Predatory Cleaners:** Third-party cleaners aggressively push subscriptions, track your device identifier, and silently purge folders.

### The Turn
**SwipePix turns gallery cleanup from a chore into rapid visual triage.**

---

## 4. How It Works (5-Step Workflow)

```
[01] BROWSE & FILTER
Explore your entire camera roll or isolate specific problem albums (WhatsApp,
Screenshots, Downloads). Tap filter chips to view Videos, Favorites, or Screenshots.

[02] SWIPE TRIAGE OR MULTI-SELECT
• Fluid Swipe Deck: Swipe RIGHT to Keep, Swipe LEFT to stage for trash.
• Zero-Letterbox Frame: Exact aspect ratio with 0 pillarbox/letterbox.
• Long-Press Multi-Select: Tap and hold in the gallery to batch-select and trash.

[03] INSPECT IN NEXT-GEN VIEWER
Edge-to-edge #080A0F canvas with atmospheric ambient photo glow. Pinch-to-zoom
up to 5x with interactive zoom pill, 2.5x expand toggle, favorite sync, and filmstrip scrub.

[04] PERSISTENCE & CHECKPOINTS
Tap "Save & Exit" anytime to preserve your exact review progress in local SQLite.
Accidentally swiped or trashed? Reversible 1-tap Undo brings items back immediately.

[05] CONFIRM SYSTEM TRASH
All deletions invoke Android's official MediaStore.createTrashRequest system dialog.
If approved, items move to Android's 30-day recoverable system trash.
```

---

## 5. Interactive Swipe Demo Copy

- **Top Sticker:** `INTERACTIVE PROTOTYPE`
- **Instruction:** `Drag or swipe the card left or right to test the triage mechanic.`
- **Left Badge (Trash):** `← TRASH (STAGE FOR REVIEW)`
- **Right Badge (Keep):** `KEEP (PRESERVE IN GALLERY) →`
- **Undo Button:** `↺ UNDO DEMO CARD`
- **Notice:** `* Illustrative demo only. Does not touch your device filesystem.`

---

## 6. Feature Grid (Verified Features Only)

### Card 1: Physics-Driven Swipe Deck
**One Photo at a Time. Instant Clarity.**  
Review photos through a responsive card deck engineered with spring physics, velocity fling detection, and next-card background preloading. No lag, no decision fatigue.

### Card 2: Adaptive Zero-Letterbox Surface
**Exact Photo Bounds. 24dp Hugging Frame.**  
Card dimensions dynamically adapt to each photo's true aspect ratio (accounting for camera EXIF orientation). The 24dp rounded frame hugs the exact image boundary with zero black bars, zero letterboxing, and zero pillarboxing.

### Card 3: Interactive Long-Press Multi-Select
**Batch Select & Trash Directly From Grid.**  
Long-press any photo or video in the gallery to activate Selection Mode. Selected items display circular checkmark badges, primary border accents, and scale down smoothly. Select all or deselect in one tap, and batch-trash with native OS protection.

### Card 4: Next-Gen Liquid Glass Photo Viewer
**Atmospheric Glow • Filmstrip • 5x Zoom Pill.**  
Deep near-black canvas (#080A0F) with real-time blurred atmospheric glow of the active photo. Features floating liquid glass chrome, an interactive zoom pill (1.0x to 5.0x) with 2.5x expand toggle, floating EXIF metadata card, and synchronized thumbnail filmstrip.

### Card 5: Native Android MediaStore Favorite Sync
**System-Level Heart Integration.**  
Favorite photos directly from the viewer. Integrates natively with Android's `MediaStore.createFavoriteRequest` system API, ensuring favorite status is reflected across all your device apps and gallery managers.
**One Photo at a Time. Instant Clarity.**  
Review photos through a responsive card deck engineered with spring physics, velocity fling detection, and next-card background preloading. No lag, no decision fatigue.

### Card 6: Chronological 4-Column Grid
**Your Entire Library, Cleanly Organized.**  
A responsive, date-grouped photo and video gallery featuring sticky relative headers (*Today*, *Yesterday*, and formatted dates) with stable Content URI keys for stutter-free scrolling.

### Card 7: Album & Folder Isolation
**Target Problem Folders Directly.**  
Don't want to clean your whole phone at once? Isolate high-bloat albums like WhatsApp Media, Screenshots, or Downloads with accurate photo/video counts.

### Card 8: Mid-Session Save & Exit
**Clean at Your Own Pace.**  
Have two minutes on the subway? Swipe 40 photos and tap "Save & Exit". Your session state and pending decisions are saved to a local SQLite database. Pick up right where you left off.

### Card 9: Instant In-Session Undo
**Mistakes Happen. Undo Is One Tap.**  
Accidentally swiped left on a cherished family photo? The floating Undo button instantly snaps the photo back into the deck and clears the staged trash flag.

### Card 10: Native Android System Trash
**Zero Silent Deletions.**  
SwipePix never deletes files directly from the filesystem. Instead, it delegates final deletion to Android's official `MediaStore.createTrashRequest` API, prompting the native OS confirmation dialog.

### Card 11: 30-Day Recovery & In-App Trash
**Accidental Deletion Is History.**  
Approved photos move to Android's system Trash, where they remain restorable for 30 days. Inspect or restore them directly from SwipePix's built-in Trash screen.

### Card 12: Hardware-Accelerated Thumbnails
**Lightning Fast Across 10,000+ Items.**  
SwipePix calls Android's native `ContentResolver.loadThumbnail(256x256)` API, reading directly from the OS-level pre-rendered hardware cache instead of decoding 48MP raw camera files.

### Card 13: 0ms Progressive Fast Preloading
**Instant Fullscreen Inspection.**  
Double-tap to zoom, pinch, and inspect fine details. The fullscreen viewer renders the 256px memory thumbnail on Frame 0 while loading full-resolution media seamlessly in the background.

### Card 14: Category Quick Filters
**One-Tap Media Filtering.**  
Instantly filter your gallery view with dedicated chips for All Media, Videos, Favorites, and Screenshots.

### Card 15: Liquid Glass & Dark Theme
**Designed for OLED & Focus.**  
Features custom Liquid Glass styling with automatic system matching, crisp light mode, and a deep slate dark mode designed to save battery on OLED screens.

### Card 16: Roadmap — On-Device AI Clustering
**Coming in v2.0 (Planned).**  
Smart on-device detection of blurry shots and near-duplicate burst photos using local machine learning models with zero cloud dependencies.

---

## 7. Safety & Trust Deep Dive

### Headline
**WE DON'T DELETE YOUR MEMORIES. YOU DO — SAFELY.**

### The 4-Layer Safety Net
1. **Decision Staging:** Swiping left only writes a `TRASH_PENDING` record into an internal SQLite table. The media file on your phone remains completely untouched.
2. **Reversible Undo:** Every swipe action can be undone immediately during the session.
3. **Native OS Confirmation:** When you finish a session, SwipePix hands the list to Android's native `createTrashRequest`. Android asks you: *"Allow SwipePix to move X photos to trash?"*
4. **30-Day System Trash:** Photos approved for trash are not erased. They reside in Android's official 30-day trash folder and can be restored anytime from your phone's default gallery or SwipePix.
5. **Google Play Protect Clean Verification:** Built with a dedicated 4096-bit RSA release keystore. Verified clean with Google Play Protect ("No harmful apps found") on physical Samsung Galaxy S24 Ultra hardware with zero security risks.

---

## 8. Privacy Architecture

### Headline
**YOUR PHOTOS STAY YOUR PHOTOS.**  
**NO CLOUD. NO TRACKING. NO ANALYTICS.**

### The Technical Privacy Proof
- **No Internet Permission:** Open `AndroidManifest.xml`. You will find `android.permission.INTERNET` is completely absent. The Android kernel will physically block the app if it attempts any network call.
- **No Third-Party SDKs:** Zero Google Firebase, zero Facebook SDK, zero telemetry tools, zero crash reporters, zero ad libraries.
- **Local SQLite Storage:** Session state, pending decisions, and undo queues live in a private, sandboxed AndroidX Room SQLite database on your device.
- **Scoped Media Access:** SwipePix requests only standard Android 13+ media permissions (`READ_MEDIA_IMAGES` and `READ_MEDIA_VIDEO`). It never asks for contacts, location, or full disk root access.

---

## 9. Performance Engineering

### Headline
**ENGINEERED FOR FLUID 120Hz DISPLAYS.**

- **Hardware Thumbnail Engine:** Android's platform thumbnail provider computes hardware-scaled 256x256 bitmaps, reducing RAM consumption from ~48MB per raw photo to under 250KB per thumbnail.
- **Dual-Tier Cache:** Coil 3 allocation reserves 25% of JVM heap for memory caching and maintains a 100MB disk cache to eliminate repeat disk reads.
- **Deck Preloading:** The cleaning view pre-fetches the next 2 images in background coroutines, ensuring zero stutter during rapid flings.
- **Non-Truncating SQLite Counts:** Media counts (Photos, Videos, Favorites) are queried via direct SQLite projections, returning sub-5ms exact totals across 20,000+ photo libraries.

---

## 10. Technology Stack

```
KOTLIN 2.2.10          Coroutines, Flow, Kotlinx Serialization
JETPACK COMPOSE        Declarative reactive UI & fluid gesture physics
MATERIAL 3             Modern tokens, adaptive dark/light surfaces
ANDROIDX ROOM 2.6.1    Local SQLite transactional session staging
COIL 3.6.2             Hardware-accelerated media decoding pipeline
DAGGER HILT 2.60.1     Clean architecture & lifecycle dependency injection
ANDROID MEDIASTORE     Modern Scoped Storage & native system trash APIs
JETPACK DATASTORE      Async coroutine-backed user preference storage
```

---

## 11. Frequently Asked Questions (FAQ)

#### Is SwipePix completely offline?
Yes. The app does not request the Android `INTERNET` permission in its manifest. The operating system prevents SwipePix from making network calls or connecting to servers.

#### Does SwipePix upload photos to the cloud?
No. All photo indexing, thumbnail generation, session records, and swipe triage happen strictly on your device's internal storage and processor.

#### Does SwipePix delete photos immediately on swipe left?
No. Swiping left stages the photo into a local pending review queue. Nothing is moved to trash until you complete your session and confirm Android's official system confirmation dialog.

#### Can I undo an accidental swipe?
Yes. Tap the Undo button at any time during a cleaning session to bring back the last photo and reverse the decision.

#### Can I pause a cleaning session and continue later?
Yes. Tap "Save & Exit" at any point. When you return to that album, SwipePix prompts you to either resume from your exact photo position or start fresh.

#### Can I restore photos after they are moved to trash?
Yes. Trashed items are moved to Android's native system trash, where they remain recoverable for 30 days. You can restore them from your phone's default gallery app or directly inside SwipePix's Trash screen.

#### What Android versions are supported?
SwipePix requires Android 13 (API 33) or higher. Older Android versions (12 and below) lack the granular media permissions and modern Scoped Storage APIs required for SwipePix's privacy model.

#### Does SwipePix clean videos as well?
Yes! SwipePix features a unified in-app video player. Videos can be played directly inside the fullscreen media viewer and inside the swipe cleaning card deck. You can watch, seek using the precision scrubber, pause, mute, or open with your preferred external player, and swipe right to Keep or left to Trash videos with instant Undo and safe 30-day Android system trash protection.

#### Does SwipePix use AI to analyze photos?
Not in version 1.0. All triage is 100% human-controlled. On-device offline machine learning for duplicate clustering is on the roadmap for v2.0.

#### How much does SwipePix cost?
SwipePix is 100% free with no paywalls, subscriptions, or ads. If SwipePix helps you reclaim storage, voluntary donations can be made on our website.

---

## 12. Updates Page Copy (`/updates`)

### Headline
**OFFICIAL UPDATE CENTER & RELEASES**

### Current Release: Version 1.0 (Build 1)
- **Status:** Production Release
- **Release Date:** September 2026
- **Target OS:** Android 13, 14, 15 (API 33–35)
- **Package:** `in.heyvinay.swipepix`

### Release Highlights
- Initial production release of SwipePix for modern Android 13, 14, and 15.
- Physics-driven swipe card triage deck with Keep, Trash, and reversible Undo.
- Adaptive zero-letterbox media surface hugging exact photo boundaries (EXIF-aware).
- Interactive long-press multi-select with batch trash in gallery and album grids.
- Next-Gen liquid glass photo viewer with atmospheric glow, filmstrip scrub, and 5x zoom pill.
- Native Android MediaStore favorite synchronization.
- Mid-session Save & Exit with deterministic resume or start-fresh prompt.
- Chronological gallery grid with sticky date headers and category filter chips.
- 2-column album explorer with non-truncating MediaStore counts.
- Native Android system trash confirmation integration via MediaStore.createTrashRequest.
- Built-in 30-day Trash inspection and restore screen.
- Liquid Glass dark and light themes.
- Dedicated RSA 4096-bit release keystore with clean Google Play Protect verification.

### Download Section
- **Direct APK:** `Download SwipePix v1.0 (Direct APK)`
- **Verification Note:** All releases are cryptographically signed with the official developer release key.
- **GitHub Releases:** Alternative direct download source available on GitHub.

---

## 13. Donate Page Copy (`/donate`)

### Headline
**SUPPORT INDEPENDENT, PRIVACY-FIRST SOFTWARE**

### Why Support SwipePix?
SwipePix is independently developed and maintained by **Vinay** (`heyvinay.in`). 

We believe essential mobile utilities should be:
- **Free of intrusive ads**
- **Free of predatory monthly subscriptions**
- **Free of tracking and cloud data harvesting**

Building and maintaining high-performance software for evolving Android releases takes significant time, physical test devices, and dedicated engineering. 

If SwipePix helped you reclaim gigabytes of storage and organize your photo library, voluntary contributions help support:
1. Ongoing Android platform compatibility updates (Android 15+).
2. Development of roadmap features (custom date ranges, storage savings dashboard, on-device AI clustering).
3. Maintaining direct APK distribution and open documentation.

### Contribution Channels
*(Note: Donation channels are provided directly on `heyvinay.in`. No unverified payment links or fake UPI IDs are presented here.)*

---

## 14. Feedback Page Copy (`/feedback`)

### Headline
**SEND FEEDBACK & REPORT ISSUES**

### Subheadline
Encountered a bug, have an album that won't load, or want to suggest a feature? We read every submission.

### Form Fields
1. **Your Name:** (Required)
2. **Email Address:** (Required for follow-up responses)
3. **Feedback Category:**
   - Bug Report
   - Feature Suggestion
   - UX / Gesture Feedback
   - Performance / Frame Drop
   - Other
4. **Subject:** (Brief summary)
5. **Detailed Message:** (Describe what happened, expected behavior, and steps to reproduce)
6. **Device Model:** (Optional, e.g. Samsung Galaxy S24 Ultra, Pixel 8)
7. **Android Version:** (e.g. Android 14 / API 34)
8. **App Version:** (Default: `SwipePix v1.0`)

### Backend Transparency Notice
*Note: Because SwipePix operates without remote servers, web feedback is routed directly to the developer inbox via secure mailto or client copy. No marketing telemetry is collected.*

---

## 15. Privacy Page Copy (`/privacy`)

### Headline
**SWIPEPIX PRIVACY POLICY & VERIFICATION**  
*Effective Date: September 2026 • Version 1.0*

### Core Commitment
SwipePix is built on an uncompromising privacy principle: **your personal photos never leave your device.**

### 1. Permissions We Request
- `android.permission.READ_MEDIA_IMAGES`: Allows reading photos from Android's MediaStore.
- `android.permission.READ_MEDIA_VIDEO`: Allows reading video files and extracting poster thumbnails.
- `android.permission.READ_MEDIA_VISUAL_USER_SELECTED`: Supports Android 14+ partial media permissions if you choose to grant access to select photos only.

### 2. Permissions We Explicitly Omit
- `android.permission.INTERNET`: **NOT REQUESTED.** SwipePix physically cannot establish a socket, communicate with remote servers, or transmit data over Wi-Fi or cellular networks.
- `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`: **NOT REQUESTED.**
- `READ_CONTACTS` / `READ_PHONE_STATE`: **NOT REQUESTED.**

### 3. Local-Only Data Storage
- **MediaStore Indexing:** Read-only access through Android's `ContentResolver`.
- **Session State:** Stored in an internal SQLite database (`cleaning_sessions` and `photo_decisions`) sandboxed inside the app's private internal storage (`/data/data/in.heyvinay.swipepix/databases/`).
- **Preferences:** Appearance and cleaning settings stored locally in Jetpack DataStore Preferences.

### 4. Third-Party Trackers & Analytics
SwipePix contains **zero** third-party analytics, crash trackers, ad libraries, or tracking pixels.

### 5. Deletion & Android Trash
SwipePix does not possess permissions to silently delete media from your filesystem. Deletions are mediated by Android's official `MediaStore.createTrashRequest` API, requiring your explicit system dialog confirmation.

---

## 16. About Page Copy (`/about`)

### Headline
**ABOUT SWIPEPIX & THE DEVELOPER**

### The Story
SwipePix was built to solve a personal frustration: modern smartphones take incredible photos, but curating them is an exhausting chore. Gallery apps make you feel like you are doing database data entry, while popular "storage cleaners" treat your phone like an advertising billboard and your photos like marketing telemetry.

SwipePix was engineered to prove that mobile utilities can be fast, respectful, and joyful to use.

### Engineering Philosophy
- **Local-First:** Hardware in your pocket is fast enough. There is zero reason for a gallery cleaner to touch the cloud.
- **Safety First:** Never destroy data silently. Rely on platform primitives (Android Trash) rather than brute-force file removal.
- **Delight in Details:** Fluid gesture physics, progressive loading, and zero UI stutter across 10,000+ photo collections.

### About the Developer
SwipePix is designed and built by **Vinay**, an independent software engineer focused on Android architecture, responsive web systems, and high-performance offline tools.

- **Developer Portfolio:** [https://portfolio.heyvinay.in/](https://portfolio.heyvinay.in/)
- **Official App Site:** [https://swipepix.heyvinay.in/](https://swipepix.heyvinay.in/)
