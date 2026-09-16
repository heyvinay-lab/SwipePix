# Requirements: SwipePix

**Defined:** 2026-09-14
**Core Value:** Rapidly clean hundreds or thousands of photos through an intuitive swipe interface — faster than any conventional gallery.

## v1 Requirements

### Permissions & Privacy

- [x] **PERM-01**: App requests only READ_MEDIA_IMAGES and READ_MEDIA_VIDEO (API 33+)
- [x] **PERM-02**: App handles READ_MEDIA_VISUAL_USER_SELECTED for partial access (API 34+)
- [x] **PERM-03**: Permission denial is handled gracefully with explanatory UI
- [x] **PERM-04**: App functions without internet connection
- [x] **PERM-05**: No photos are uploaded, copied, or sent anywhere
- [x] **PERM-06**: No cloud service, Google Photos, or sync integration exists

### Media Engine

- [x] **MEDIA-01**: Photos discovered through Android MediaStore APIs
- [x] **MEDIA-02**: MediaStore queries run off the main/UI thread
- [x] **MEDIA-03**: Photo library is NOT loaded entirely into memory
- [x] **MEDIA-04**: Folder/album information derived from BUCKET_ID/BUCKET_DISPLAY_NAME
- [x] **MEDIA-05**: Empty folders and empty libraries handled gracefully
- [x] **MEDIA-06**: Newly added photos appear without app reinstall
- [x] **MEDIA-07**: Deleted/moved photos handled correctly (no crash)

### Gallery

- [x] **GALL-01**: Photo grid displays photos in date-descending order
- [x] **GALL-02**: Thumbnails load efficiently via Coil caching
- [x] **GALL-03**: Scrolling remains smooth with 1K+ photos
- [x] **GALL-04**: Loading, empty, and error states exist
- [x] **GALL-05**: Back navigation works correctly

### Albums & Folders

- [x] **ALBM-01**: User can view list of available folders/albums
- [x] **ALBM-02**: User can select a folder to view its photos
- [x] **ALBM-03**: User can return to All Photos
- [x] **ALBM-04**: Empty folders handled correctly
- [x] **ALBM-05**: Album photo counts displayed

### Full-Screen Viewer

- [x] **VIEW-01**: Full-screen photo viewing with zoom/pan
- [x] **VIEW-02**: Navigation between photos (swipe or arrows)
- [x] **VIEW-03**: Basic metadata displayed (date, size, folder)
- [x] **VIEW-04**: Delete action available

### Clean Up Mode

- [x] **CLEAN-01**: Clean Up action accessible from main gallery
- [x] **CLEAN-02**: User can choose All Photos or a specific album
- [x] **CLEAN-03**: One photo displayed at a time in card format
- [x] **CLEAN-04**: Swipe LEFT moves photo toward Trash
- [x] **CLEAN-05**: Swipe RIGHT keeps the photo
- [x] **CLEAN-06**: Swipe animations are smooth
- [x] **CLEAN-07**: Keep button works
- [x] **CLEAN-08**: Trash/Delete button works
- [x] **CLEAN-09**: Undo works (restores last swiped photo)
- [x] **CLEAN-10**: Progress indicator shows current/total (e.g., 127 / 1,542)
- [x] **CLEAN-11**: Rapid swiping does not break the session
- [x] **CLEAN-12**: End-of-session summary displayed (reviewed, kept, trashed)
- [x] **CLEAN-13**: User can exit a cleanup session safely at any point

### Trash & Deletion

- [x] **TRASH-01**: Left-swiped photos batched and trashed via MediaStore.createTrashRequest at session end
- [x] **TRASH-02**: App does NOT falsely claim permanent deletion
- [x] **TRASH-03**: System confirmation dialog handled correctly
- [x] **TRASH-04**: Failed trash operations reported to user
- [x] **TRASH-05**: Already-missing media does not crash
- [x] **TRASH-06**: Undo restores session state before system trash

### Performance

- [x] **PERF-01**: Gallery responsive with 1,000+ photos
- [x] **PERF-02**: Gallery responsive with 5,000+ photos
- [x] **PERF-03**: Swipe interactions remain responsive
- [x] **PERF-04**: No OOM crashes during normal use
- [x] **PERF-05**: MediaStore queries do not block UI
- [x] **PERF-06**: Next cleanup photos preloaded

### Lifecycle & Reliability

- [x] **LIFE-01**: App survives background/foreground transitions
- [x] **LIFE-02**: App handles configuration changes without state corruption
- [x] **LIFE-03**: App handles missing/inaccessible media gracefully
- [x] **LIFE-04**: No critical runtime exceptions during normal use

### UI/UX

- [x] **UX-01**: Swipe direction and consequences visually clear
- [x] **UX-02**: Destructive actions clearly communicated
- [x] **UX-03**: Touch targets appropriately sized
- [x] **UX-04**: Dark and light theme support
- [x] **UX-05**: Loading and empty states polished

### Settings

- [x] **SETT-01**: Theme selection (system/light/dark)
- [x] **SETT-02**: App permissions info
- [x] **SETT-03**: About/privacy information

## v2 Requirements

### Enhanced Gallery

- **GALL-V2-01**: Date-based sections with sticky headers
- **GALL-V2-02**: Video playback in viewer
- **GALL-V2-03**: Multi-select for batch operations
- **GALL-V2-04**: Search by filename/date

### Enhanced Cleanup

- **CLEAN-V2-01**: Filter cleanup by date range
- **CLEAN-V2-02**: Filter cleanup by file size (large files first)
- **CLEAN-V2-03**: Screenshots quick-filter
- **CLEAN-V2-04**: Video cleanup support
- **CLEAN-V2-05**: Cleanup session persistence across app restart

### Trash Management

- **TRASH-V2-01**: Dedicated Trash screen showing trashed items
- **TRASH-V2-02**: Restore individual items from trash
- **TRASH-V2-03**: Permanent delete action
- **TRASH-V2-04**: Retention time remaining display

## Out of Scope

| Feature | Reason |
|---------|--------|
| AI/ML photo classification | Privacy violation; explicitly prohibited |
| Cloud backup/sync | Contradicts privacy-first, on-device principle |
| Google Photos integration | Contradicts on-device-only requirement |
| Photo editing | Not core to cleanup workflow |
| Duplicate detection | Requires ML or expensive hashing |
| Social sharing | Not relevant to core value |
| iOS version | Android-only project |
| Video playback in cleanup | Complexity for MVP; defer to v2 |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| PERM-01 to PERM-06 | Phase 2 | ✅ Completed |
| MEDIA-01 to MEDIA-07 | Phase 2 | ✅ Completed |
| GALL-01 to GALL-05 | Phase 3 | ✅ Completed |
| ALBM-01 to ALBM-05 | Phase 3 | ✅ Completed |
| VIEW-01 to VIEW-04 | Phase 4 | ✅ Completed |
| CLEAN-01 to CLEAN-13 | Phase 5 | ✅ Completed |
| TRASH-01 to TRASH-06 | Phase 5 | ✅ Completed |
| PERF-01 to PERF-06 | Phase 6 | ✅ Completed |
| LIFE-01 to LIFE-04 | Phase 7 | ✅ Completed |
| UX-01 to UX-05 | Phase 7 | ✅ Completed |
| SETT-01 to SETT-03 | Phase 7 | ✅ Completed |

**Coverage:**
- v1 requirements: 52 total
- Mapped to phases: 52
- Completed: 52 / 52 (100%) ✅
- Unmapped: 0 ✅

---
*Requirements defined: 2026-09-14*
*Last updated: 2026-09-14 after GSD project initialization*
