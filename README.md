# SwipePix 📸✨

<div align="center">

> **Privacy-First, 100% Offline Android Gallery & High-Speed Media Declutter Application**

[![Android 13+](https://img.shields.io/badge/Platform-Android_13+_(API_33--35)-green.svg?style=flat-square&logo=android)](https://github.com/heyvinay-lab/SwipePix)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-blue.svg?style=flat-square&logo=kotlin)](https://kotlinlang.org)
[![Compose BOM](https://img.shields.io/badge/Compose_BOM-2026.08.00-4285F4.svg?style=flat-square&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Offline](https://img.shields.io/badge/Offline-100%25_On--Device-success.svg?style=flat-square)](https://swipepix.heyvinay.in/privacy)
[![Zero Network](https://img.shields.io/badge/Internet_Permission-NONE_(0KB)-red.svg?style=flat-square)](https://swipepix.heyvinay.in/privacy)
[![Sponsor](https://img.shields.io/badge/Sponsor-heyvinay--lab-ea4aaa.svg?style=flat-square&logo=githubsponsors)](https://github.com/sponsors/heyvinay-lab)
[![License](https://img.shields.io/badge/License-MIT-lightgrey.svg?style=flat-square)](LICENSE)

<br />

### 🚀 Primary Actions

[![Download APK](https://img.shields.io/badge/📥_Download_SwipePix-v1.0.0_APK-2ea44f?style=for-the-badge&logo=android&logoColor=white)](https://github.com/heyvinay-lab/SwipePix/releases/latest)
[![Latest Release](https://img.shields.io/badge/🏷️_Latest_Release-GitHub-0969da?style=for-the-badge&logo=github&logoColor=white)](https://github.com/heyvinay-lab/SwipePix/releases)

[![Send Feedback & Bugs](https://img.shields.io/badge/💬_Send_Feedback_%26_Bugs-GitHub_Issues-bf8700?style=for-the-badge&logo=github&logoColor=white)](https://github.com/heyvinay-lab/SwipePix/issues/new/choose)
[![Support Development](https://img.shields.io/badge/💖_Support_Development-Sponsor_%26_UPI-cf222e?style=for-the-badge&logo=githubsponsors&logoColor=white)](https://swipepix.heyvinay.in/donate)

[![Official Website](https://img.shields.io/badge/🌐_Official_Website-swipepix.heyvinay.in-8250df?style=for-the-badge&logo=googlechrome&logoColor=white)](https://swipepix.heyvinay.in/)

<br />

**[ [Download SwipePix APK](https://github.com/heyvinay-lab/SwipePix/releases/latest) ]** &bull; 
**[ [All Releases](https://github.com/heyvinay-lab/SwipePix/releases) ]** &bull; 
**[ [Send Feedback & Bugs](https://github.com/heyvinay-lab/SwipePix/issues/new/choose) ]** &bull; 
**[ [Support Development](https://swipepix.heyvinay.in/donate) ]** &bull; 
**[ [Official Website](https://swipepix.heyvinay.in/) ]**

</div>

---

## 📖 Overview

**SwipePix** is an ultra-fast, privacy-first Android photo and video declutter utility. It combines intuitive swipe-based decision making with safe system-level media operations so you can rapidly organize your gallery, reclaim gigabytes of storage, and keep only what truly matters.

Operating **100% on-device** with **zero internet permissions**, SwipePix ensures your personal photos and videos never leave your phone.

---

## 🌟 Key Features

### 🃏 Intuitive Swipe Deck
- Rapidly triage photos and videos: **Swipe Right to Keep**, **Swipe Left to Trash**.
- Fluid 60 FPS gesture animations with touch velocity tracking.
- Instant single-step **Undo Stack** to safely review or revert recent decisions.

### 🎬 Unified In-App Video Player *(New)*
- **Native Embedded Video Playback**: Preview video clips directly within the swipe deck cards without launching external applications.
- **Hardware-Accelerated Engine**: Powered by AndroidX Media3 / ExoPlayer 1.5.1 for low latency, smooth seeking, and minimal battery impact.
- **In-Deck Controls**: Inline play/pause toggle, audio toggle, and scrubbable timeline.
- **External Player Fallback**: Easily open the video in your preferred external media player with one tap whenever needed.

### 🔍 Immersive Photo Viewer
- Fluid multi-touch pinch-to-zoom, pan, and double-tap zoom.
- Adaptive aspect-ratio framing preserving original image quality.
- Interactive bottom filmstrip for rapid media switching.

### 🗂️ Gallery & Album Organization
- Browse all on-device media or filter by specific albums (Screenshots, Camera, Downloads, WhatsApp, etc.).
- Instant scroll position preservation when navigating between screens.

### ☑️ Batch Multi-Selection
- Long-press any media item in the gallery to enter multi-selection mode.
- Select dozens or hundreds of items and batch-trash them with a single action.

### 🛡️ Safe Deletion via MediaStore Trash
- Full integration with Android's scoped storage (`MediaStore.createTrashRequest`).
- Trashed items are moved to Android's system Trash (retained for 30 days before permanent removal by the OS).
- No silent or irreversible file deletion—every purge requires explicit system-level confirmation.

### 🔒 100% Offline & Private
- `android.permission.INTERNET` is completely omitted from `AndroidManifest.xml`.
- Zero analytics, zero crash telemetry, zero cloud tracking, zero external APIs.

---

## 📥 Download & Installation

### Option 1: Official GitHub Releases (Recommended)
Download the latest APK directly from our verified GitHub Releases page:

👉 **[Download Latest Release (v1.0.0 APK)](https://github.com/heyvinay-lab/SwipePix/releases/latest)**

👉 **[View All Versioned Releases](https://github.com/heyvinay-lab/SwipePix/releases)**

#### How to Install the APK:
1. Download `SwipePix-1.0.0.apk` onto your Android device (Android 13+).
2. Open the downloaded file from your notifications or browser downloads.
3. If prompted by Android, enable **"Install unknown apps"** for your browser or file manager.
4. Tap **Install**, then open **SwipePix**.
5. Grant the standard Android **Photos and Videos** permission (`READ_MEDIA_IMAGES` and `READ_MEDIA_VIDEO`) when prompted so SwipePix can display your local gallery.

### Option 2: Build From Source
SwipePix is fully open-source. You can inspect the source code and compile the APK yourself using Gradle (see [Building From Source](#-building-from-source)).

---

## 💬 Feedback & Bug Reports

Encountered an issue, unexpected crash, or have an idea to make SwipePix even better? We actively triage all community input!

- **Report a Bug**: [Open a Bug Report on GitHub](https://github.com/heyvinay-lab/SwipePix/issues/new?template=bug_report.yml)
- **Request a Feature**: [Open a Feature Request on GitHub](https://github.com/heyvinay-lab/SwipePix/issues/new?template=feature_request.yml)
- **Web Portal**: [Visit the Official Feedback Page](https://swipepix.heyvinay.in/feedback)
- **Direct Email**: [vinay@heyvinay.in](mailto:vinay@heyvinay.in)

---

## 💖 Support Development

SwipePix is an independent, ad-free, tracker-free open-source project. If SwipePix helped you reclaim storage on your phone, consider supporting ongoing development:

- **GitHub Sponsors**: [Sponsor @heyvinay-lab](https://github.com/sponsors/heyvinay-lab)
- **UPI (India)**: `6204078366@kotak` (Vinay Kumar, Kotak Mahindra Bank)
- **Official Donation Page**: [swipepix.heyvinay.in/donate](https://swipepix.heyvinay.in/donate) *(includes dynamic QR code generator & instant deep link)*

---

## 🛠️ Architecture & Tech Stack

SwipePix is built following modern Android architectural best practices and official Jetpack guidelines:

| Component | Technology | Description |
| :--- | :--- | :--- |
| **Language** | Kotlin 2.2.10 | Modern, concise, memory-safe language targeting JVM 17 |
| **Min / Target SDK** | Android 13 (API 33) / Android 15 (API 35) | Fully compliant with modern Android scoped storage & permissions |
| **UI Framework** | Jetpack Compose + Material 3 | Declarative UI with Compose BOM `2026.08.00` |
| **Architecture** | MVVM + Clean Architecture | Unidirectional Data Flow (UDF) with reactive Kotlin Coroutines & StateFlow |
| **Dependency Injection** | Dagger Hilt 2.60.1 | Robust compile-time dependency injection |
| **Video Engine** | AndroidX Media3 / ExoPlayer 1.5.1 | Hardware-accelerated embedded video playback |
| **Image Loading** | Coil 3.6.2 | Async image engine with custom `MediaStoreThumbnailFetcher` |
| **Local Persistence** | Room 2.6.1 + Jetpack DataStore 1.1.7 | Local session tracking, caching, and user preference storage |
| **Media Operations** | Android `MediaStore` | Scoped storage operations using `createTrashRequest` & `createDeleteRequest` |

---

## 📁 Project Structure

```text
SwipePix/
├── app/
│   ├── src/main/
│   │   ├── java/in/heyvinay/swipepix/
│   │   │   ├── data/          # Room DB, MediaStore queries, repositories
│   │   │   ├── di/            # Dagger Hilt dependency injection modules
│   │   │   ├── model/         # Domain models (MediaItem, Album, CleanupSession)
│   │   │   ├── ui/            # Jetpack Compose UI (Screens, ViewModels, Themes)
│   │   │   │   ├── album/     # Album grid and album detail screens
│   │   │   │   ├── cleanup/   # Swipe cleaning deck and session completion
│   │   │   │   ├── gallery/   # Main photo & video gallery grid
│   │   │   │   ├── player/    # Unified in-app video player (Media3 / ExoPlayer)
│   │   │   │   ├── theme/     # Material 3 colors, typography, shapes
│   │   │   │   └── viewer/    # Fullscreen photo/media viewer with zoom & pan
│   │   │   └── util/          # Formatting, thumbnail fetchers, permissions
│   │   └── res/               # App icons, drawables, strings, XML resources
│   └── build.gradle.kts       # Application build configuration
├── gradle/                    # Gradle wrapper and version catalog (libs.versions.toml)
└── build.gradle.kts           # Top-level build configuration
```

---

## 🚀 Building From Source

### Prerequisites
- Android Studio Ladybug (2024.2.1) or newer
- Android SDK 35 (Compile SDK 37)
- JDK 17+
- Git

### Build Instructions
```bash
# Clone the repository
git clone https://github.com/heyvinay-lab/SwipePix.git
cd SwipePix

# Build the debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew testDebugUnitTest

# Install debug APK to connected device
./gradlew installDebug
```

---

## 🔒 Privacy & Security Policy

1. **Zero Network Transmission**: SwipePix does not request or possess the `android.permission.INTERNET` permission. It is physically impossible for the app to send data to any remote server.
2. **On-Device Storage**: All thumbnails, album caches, and cleanup logs are stored locally in private application sandbox storage.
3. **Non-Destructive Deletion**: Media items marked for deletion are sent to Android's system Trash via `MediaStore.createTrashRequest`. You retain 30 days to restore them via your phone's default Gallery app before permanent deletion.

---

## 🌐 Official Channels & Community

- **Official Website**: [https://swipepix.heyvinay.in/](https://swipepix.heyvinay.in/)
- **Release Downloads**: [https://github.com/heyvinay-lab/SwipePix/releases](https://github.com/heyvinay-lab/SwipePix/releases)
- **Issue Tracker**: [https://github.com/heyvinay-lab/SwipePix/issues](https://github.com/heyvinay-lab/SwipePix/issues)
- **Support & Donations**: [https://swipepix.heyvinay.in/donate](https://swipepix.heyvinay.in/donate)

---

## 📜 License

SwipePix is open-source software released under the [MIT License](LICENSE).
