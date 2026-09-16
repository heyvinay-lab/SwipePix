# SwipePix 📸✨

> **Privacy-First, 100% Offline Android Gallery & High-Speed Media Cleaning Utility**

SwipePix is a high-performance Android gallery and storage optimization utility built to help users rapidly triage, organize, and clean photos and videos using an intuitive swipe interface. 

Operating **100% on-device** with **zero network permissions**, SwipePix ensures complete user privacy while delivering 60 FPS fluid gesture-driven media management.

---

## 🌟 Key Features

- 🃏 **Tinder-Style Cleaning Deck**: Rapidly swipe right to **Keep** or swipe left to **Trash**. Seamlessly review pending actions with a dedicated undo stack.
- 🗂️ **Comprehensive Gallery & Albums**: Browse all photos, videos, and categorized albums (Screenshots, Camera, Downloads, etc.) with preserved scroll positions.
- ☑️ **Multi-Selection & Batch Operations**: Long-press any media item to enter selection mode, select multiple items, and batch-trash with a single confirmation.
- 🔍 **Immersive Photo Viewer**: Fluid pinch-to-zoom, pan, double-tap zoom, adaptive aspect-ratio framing, and an interactive filmstrip strip for rapid browsing.
- 🎬 **Unified In-Deck Video Player**: Preview videos directly within the swipe deck with integrated hardware-accelerated playback and external player fallback.
- 🔒 **100% Offline & Private**: Zero internet permissions (`INTERNET` is omitted from `AndroidManifest.xml`), zero analytics trackers, and zero cloud uploads. Your memories stay strictly on your device.
- 🛡️ **Safe Deletion Protocol**: Integrates directly with Android's scoped storage `MediaStore.createTrashRequest` and `MediaStore.createDeleteRequest`, ensuring non-destructive trashing with OS-level confirmation.

---

## 🛠️ Architecture & Tech Stack

SwipePix is built adhering to modern Android architectural best practices:

- **Language & Toolchain**: Kotlin 2.2.10, JVM 17
- **Target Platform**: Android 13+ (API 33–35)
- **UI Architecture**: Jetpack Compose, Material 3, Compose BOM 2026.08.00
- **Design Paradigm**: Clean Architecture + Unidirectional Data Flow (UDF) + MVVM
- **Dependency Injection**: Dagger Hilt 2.60.1
- **Persistence**: Room 2.6.1 + Jetpack DataStore Preferences 1.1.7
- **Media Engine**: Android MediaStore (`MediaStore.Files`) + Coil 3.6.2 (Custom `MediaStoreThumbnailFetcher`)
- **Video Playback**: AndroidX Media3 / ExoPlayer 1.5.1

---

## 📁 Project Structure

```text
SwipePix/
├── app/
│   ├── src/main/
│   │   ├── java/in/heyvinay/swipepix/
│   │   │   ├── data/          # Room DB, MediaStore data source, repositories
│   │   │   ├── di/            # Dagger Hilt dependency injection modules
│   │   │   ├── model/         # Domain models (MediaItem, Album, CleanupSession)
│   │   │   ├── ui/            # Jetpack Compose UI (Screens, ViewModels, Themes)
│   │   │   │   ├── album/     # Album grid and album detail screens
│   │   │   │   ├── cleanup/   # Swipe cleaning deck and session completion
│   │   │   │   ├── gallery/   # Main photo & video gallery grid
│   │   │   │   ├── player/    # Unified video player component
│   │   │   │   ├── theme/     # Material 3 colors, typography, shapes
│   │   │   │   └── viewer/    # Fullscreen photo/media viewer
│   │   │   └── util/          # Formatting, thumbnail fetchers, permissions
│   │   └── res/               # App icons, drawables, strings, XML resources
├── gradle/                    # Gradle wrapper and version catalog (libs.versions.toml)
└── build.gradle.kts           # Top-level build configuration
```

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug (or newer)
- Android SDK 35 (Compile SDK 37)
- JDK 17+

### Build & Run
```bash
# Clone the repository
git clone https://github.com/heyvinay-lab/SWIPEPIX.git
cd SWIPEPIX

# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew testDebugUnitTest
```

---

## 📜 License & Privacy

SwipePix is built with a zero-compromise approach to user data privacy. No network requests are made, no telemetry is gathered, and all media operations are performed locally using official Android platform APIs.
