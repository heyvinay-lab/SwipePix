# Roadmap: SwipePix

**Milestone:** v1.0 — MVP
**Goal:** A working, polished Android gallery with swipe-to-clean that handles real photo libraries performantly.

## Phase 1: Project Foundation
**Status:** Completed (2026-09-14)
**Focus:** Convert blank scaffold to a buildable Compose + Hilt project with navigation shell.

**Delivers:**
- Gradle config with Kotlin/Compose/Hilt/Coil/Navigation dependencies
- Package structure
- Material 3 theme (dark + light)
- Single Activity + NavHost with placeholder screens
- Application class with @HiltAndroidApp
- Successful debug build

**Requirements:** (Foundation — no requirements directly completed)

---

## Phase 2: Permissions & Media Engine
**Status:** Completed (2026-09-14)
**Focus:** MediaStore access, permission flows, photo/album queries.

**Delivers:**
- Permission request UI with full/partial/denied handling
- MediaStoreDataSource: paginated photo queries, album discovery, metadata
- MediaRepository interface
- All queries on Dispatchers.IO
- Empty state handling

**Requirements:** PERM-01 to PERM-06, MEDIA-01 to MEDIA-07

---

## Phase 3: Gallery UI
**Status:** Completed (2026-09-14)
**Focus:** Photo grid, album browsing, date organization.

**Delivers:**
- LazyVerticalGrid photo grid with Coil thumbnails
- Date-grouped display
- Album list with photo counts
- Album detail view
- Loading/empty/error states
- Clean Up button (placeholder action)

**Requirements:** GALL-01 to GALL-05, ALBM-01 to ALBM-05

---

## Phase 4: Full-Screen Viewer
**Status:** Completed (2026-09-14)
**Focus:** Image viewing experience.

**Delivers:**
- Zoomable/pannable full-screen viewer
- Swipe between photos
- Metadata display
- Delete action

**Requirements:** VIEW-01 to VIEW-04

---

## Phase 5: Clean Up Mode
**Status:** Completed (2026-09-14)
**Focus:** The core differentiator — swipe-to-clean workflow.

**Delivers:**
- Source selection (All Photos / specific album)
- Custom swipe card with gesture detection
- Keep (right) / Trash (left) visual indicators
- Undo stack (in-memory)
- Progress counter
- Image preloading (N+1, N+2)
- Completion summary screen
- Batch createTrashRequest at session end
- System dialog handling

**Requirements:** CLEAN-01 to CLEAN-13, TRASH-01 to TRASH-06

---

## Phase 6: Performance Optimization
**Status:** Completed (2026-09-14)
**Focus:** Profile and optimize for large libraries.

**Delivers:**
- Profiled scrolling with 1K, 5K, 10K+ photos
- Memory usage optimization
- Swipe animation smoothness verification
- Startup time measurement
- Fix identified bottlenecks

**Requirements:** PERF-01 to PERF-06

---

## Phase 7: Reliability & Polish
**Status:** Completed (2026-09-14)
**Focus:** Lifecycle, error handling, UI polish, settings.

**Delivers:**
- Lifecycle testing (rotation, background, kill)
- Error handling for all edge cases
- Settings screen (theme, permissions, about)
- Animation polish
- Dark/light theme verification
- Empty/loading state polish

**Requirements:** LIFE-01 to LIFE-04, UX-01 to UX-05, SETT-01 to SETT-03

---

## Phase 8: Final Verification & Release Build
**Status:** Completed (2026-09-14)
**Focus:** MVP acceptance checklist verification.

**Delivers:**
- Complete MVP acceptance report
- Release build generation
- Known issues documentation
- PASS / PASS WITH LIMITATIONS / NOT READY classification

**Requirements:** (Verification — all requirements must be verified)

---

# Milestone: v1.1 — Liquid Glass & UI/UX Elevation

**Goal:** Transform SwipePix into a premium Android application with solid dark background + selective liquid glass components, floating bottom navigation, spring-based swipe physics, animated undo, and a dedicated Trash management screen.

## Phase 9: Liquid Glass Design Primitives & Theme Foundation
**Status:** In Progress
**Focus:** Design tokens, glass surfaces, highlights, and contrast backgrounds.

**Delivers:**
- `ui/theme/Glass.kt` with `GlassSurface`, `Modifier.glassEffect()`
- High-contrast deep solid dark background palette (`#0B0B0E`)
- Dynamic glass highlight strokes and translucent scrims

---

## Phase 10: Floating Bottom Navigation & App Shell
**Status:** Pending
**Focus:** Floating liquid-glass navigation bar and prominent Clean Up CTA.

**Delivers:**
- Floating pill-shaped bottom navigation (`[ Photos | Albums | Clean Up | Search ]`)
- Prominent visual hierarchy for Clean Up action
- Integration into `MainActivity` and `SwipePixNavGraph`

---

## Phase 11: Home / Photos Screen Elevation & Quick Filters
**Status:** Pending
**Focus:** Header photo counts, search bar, and metadata-driven filters.

**Delivers:**
- Header with photo count and options
- Rounded glass search interaction
- Quick filters: All, Favorites, Videos, Screenshots
- Multi-type media query in `MediaStoreDataSource`

---

## Phase 12: Swipe Physics, Directional Feedback & Animated Undo
**Status:** Pending
**Focus:** Spring-based card physics, velocity tracking, and animated undo.

**Delivers:**
- Re-engineered `SwipeableCard` with spring physics and velocity sensing
- Progressive "KEEP" (green) and "TRASH" (red) stamp overlays during drag
- Animated undo sliding back onto stack from screen edge
- Dynamic card stack scale transition

---

## Phase 13: Dedicated Trash Screen & Restoration Engine
**Status:** Pending
**Focus:** Full Trash management and restoration.

**Delivers:**
- Query trashed media via `MediaStore.QUERY_ARG_MATCH_TRASHED`
- Trashed items grid with retention days remaining
- Restore items (`createTrashRequest(uris, false)`)
- Permanent deletion option

---

## Phase 14: Full-Screen Viewer Polish & Extended Actions
**Status:** Pending
**Focus:** Modern viewer controls and glass metadata panel.

**Delivers:**
- Translucent glass metadata panel overlay
- Share action (`ACTION_SEND`)
- Favorite toggle (`createFavoriteRequest`)
- Detailed image information dialog

---

## Phase 15: Performance Verification & Final Polish
**Status:** Pending
**Focus:** 60/120fps verification, blur optimization, responsive testing.

**Delivers:**
- Frame rate and memory profile verification
- Release build generation (`assembleRelease`)
- Acceptance report for v1.1

---
*Roadmap defined: 2026-09-14*
*Last updated: 2026-09-15 (Milestone v1.1 Liquid Glass Elevation Active)*
