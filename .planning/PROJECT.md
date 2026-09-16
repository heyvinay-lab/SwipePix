# SwipePix

## What This Is

SwipePix is a privacy-first, on-device Android photo gallery with a Tinder-style swipe-to-clean workflow. Users browse their local photo library in a polished gallery (grid, albums, dates), then tap "Clean Up" to rapidly triage photos one-at-a-time — swipe right to keep, swipe left to trash. No AI, no cloud, no internet required.

## Core Value

**Rapidly clean hundreds or thousands of photos through an intuitive swipe interface — faster than any conventional gallery.**

## Requirements

### Validated

(None yet — ship to validate)

### Active

- [ ] Local photo gallery with grid, albums/folders, date grouping
- [ ] Full-screen photo viewer with zoom/pan
- [ ] Clean Up mode with Tinder-style swipe cards
- [ ] Keep (right) / Trash (left) swipe actions
- [ ] Undo during cleanup sessions
- [ ] Progress tracking during cleanup
- [ ] Session completion summary
- [ ] Trash management via Android MediaStore trash API
- [ ] Permission handling (READ_MEDIA_IMAGES, READ_MEDIA_VIDEO, partial access)
- [ ] Smooth 60fps scrolling and swipe animations
- [ ] Works with 10,000+ photo libraries
- [ ] Settings (theme, permissions, about)
- [ ] Dark/light theme support

### Out of Scope

- AI/ML photo classification — privacy violation, adds complexity, not needed
- Cloud backup/sync — contradicts privacy-first principle
- Google Photos integration — contradicts on-device-only principle
- Video playback in cleanup mode — complexity; defer to v2
- Photo editing — not core to cleanup workflow
- Duplicate detection — requires ML or expensive hashing; defer to v2
- Social sharing — not relevant to core value
- iOS version — Android-only project

## Context

- **Platform**: Android only, minSdk 33, targetSdk 37
- **Build system**: AGP 9.3.2 with built-in Kotlin 2.2.10
- **Existing state**: Blank Android project scaffold (no Activity, no Compose)
- **Media access**: Android MediaStore is the only correct way to access photos
- **Trash**: `MediaStore.createTrashRequest` (API 30+) provides native 30-day trash
- **Permissions**: API 33+ uses `READ_MEDIA_IMAGES`/`READ_MEDIA_VIDEO`; API 34+ adds `READ_MEDIA_VISUAL_USER_SELECTED` for partial access
- **Development model**: AI-assisted development; correctness > speed

## Constraints

- **Privacy**: 100% on-device, zero network calls, zero photo uploads — non-negotiable
- **No AI/ML**: No image classification, vision models, or cloud AI of any kind
- **Performance**: 60fps scrolling and smooth swipe animations are release-blocking requirements
- **Android APIs**: Never invent APIs — verify against documentation before implementing
- **MinSdk 33**: Android 13+ only; simplifies permissions, avoids legacy storage APIs
- **Solo developer**: Architecture must be maintainable by one person
- **Correctness first**: Priority order: Correctness > Data Safety > Performance > UI Smoothness > Privacy > Reliability > Maintainability > UX Polish > Features

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Kotlin + Jetpack Compose | Modern Android stack, AGP 9.3 built-in Kotlin | ⏳ Pending |
| MVVM + Repository architecture | Clean separation, ViewModel survives config changes, appropriate for solo dev | ⏳ Pending |
| Coil 3 for image loading | Kotlin-first, Compose-native, automatic caching/downsampling | ⏳ Pending |
| Hilt for DI | Standard Android DI, hiltViewModel() integration | ⏳ Pending |
| Batch trash at session end | Preserves rapid-swiping flow (one system dialog vs per-swipe), free undo | ✅ Good |
| Custom Compose gesture for swipe | Full control over rotation, opacity, stacking, fling detection | ⏳ Pending |
| No Paging 3 for MVP | Manual cursor pagination is simpler; MediaStore + LIMIT/OFFSET is fast enough | ⏳ Pending |
| Photos-only cleanup for MVP | Videos shown in gallery grid but excluded from swipe cleanup; reduces scope | ✅ Good |
| Screenshot filter deferred to v2 | Just a folder filter; not worth MVP complexity | ✅ Good |
| minSdk 33 | Avoids legacy READ_EXTERNAL_STORAGE, simplifies MediaStore access | ✅ Good |

---
*Last updated: 2026-09-14 after GSD project initialization*
