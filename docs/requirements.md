# GalleryCleaner - Requirements

## Overview

GalleryCleaner helps users manage and clean up their photo and video gallery by organizing media by source and providing easy bulk deletion capabilities.

---

## Core Features

### 1. Source Filtering ✅ IMPLEMENTED

**Goal**: Allow users to view media from specific sources only.

- ✅ Display filter chips showing all detected sources
- ✅ Sources include:
  - WhatsApp
  - Viber
  - Messenger
  - Telegram
  - Instagram
  - Snapchat
  - Facebook
  - Twitter/X
  - TikTok
  - Pinterest
  - Reddit
  - Camera
  - Screenshots (device)
  - Downloads
  - Other
- ✅ Allow selecting one or multiple sources to filter the view
- ✅ Show image/video count per source
- ✅ "All" option to show unfiltered view
- ✅ Remember last selected filter (persist preference)

### 1b. Media Type Filtering ✅ IMPLEMENTED

**Goal**: Allow users to filter between photos and videos.

- ✅ Photos/Videos toggle chips at top of screen
- ✅ Both can be selected independently
- ✅ Deselecting both shows empty state
- ✅ Video thumbnails show play icon indicator
- ✅ Preferences persist between sessions

### 2. Screenshots Detection ✅ IMPLEMENTED

**Goal**: Clearly distinguish screenshots taken on this device for easy cleanup.

- ✅ Detect screenshots by:
  - Path containing "Screenshots" or "screenshot"
  - DCIM/Screenshots folder
  - Pictures/Screenshots folder
- ✅ Screenshots appear as separate source type with filter chip
- ⏳ Visual distinction in the grid (different badge color) - optional enhancement
- ✅ Quick filter: Select only Screenshots chip to show only screenshots

### 3. Bulk Selection Mode ✅ IMPLEMENTED

**Goal**: Allow users to select multiple images for deletion.

- ✅ **Activation**: Long-press on any image to enter selection mode
- ✅ **Selection mode UI**:
  - ✅ Toolbar changes to show:
    - ✅ Selected count (e.g., "5 selected")
    - ✅ Hidden count when filtered (e.g., "5 selected (2 hidden)")
    - ✅ "Select All" button
    - ✅ "Delete" button
    - ✅ "X" button to exit selection mode
  - ✅ Selected images show a checkmark overlay
  - ✅ Tapping an image toggles its selection
- ✅ **Trash flow** (soft delete):
  - ✅ Show confirmation dialog: "Move X items to trash?"
  - ✅ Use MediaStore.createTrashRequest for Android 11+ (30-day retention)
  - ✅ Direct deletion for Android 10 and below (no system trash)
  - ✅ Snackbar with "Undo" option to restore immediately
  - ✅ Refresh grid after trash completes
- ✅ **Exit selection mode**:
  - ✅ Press back button
  - ✅ Tap X button
  - ✅ After successful deletion

### 4. Date Range Filter ✅ IMPLEMENTED

**Goal**: Allow users to filter media by date range to avoid scrolling through entire gallery.

- ✅ Date filter UI with chip-based presets:
  - ✅ "7 days", "30 days", "3 months", "This year", "All time"
  - ✅ "Custom" with Material DateRangePicker for From/To selection
- ✅ Default: "Last 30 days" on first launch (improves performance with large galleries)
- ✅ Filter applies on top of source and media type filters (combinable)
- ✅ Source counts update based on date range (accurate counts per time period)
- ✅ Custom range shows formatted dates (e.g., "Dec 1 - Dec 25")
- ✅ Filter preference persists between sessions
- ✅ Uses DATE_ADDED from MediaStore

---

## UI/UX Requirements

### Main Screen Layout (Current Implementation)

```
┌─────────────────────────────────┐
│                                 │  <- No app bar (full screen)
├─────────────────────────────────┤
│ [Photos] [Videos]               │  <- Media type filter chips
├─────────────────────────────────┤
│ [7d] [30d] [3mo] [Year] [All]   │  <- Date range chips (horizontal scroll)
├─────────────────────────────────┤
│ [All] [WhatsApp] [Camera] [+]   │  <- Source filter chips (horizontal scroll)
├─────────────────────────────────┤
│ ┌─────┐ ┌─────┐ ┌─────┐        │
│ │▶    │ │     │ │     │        │  <- ▶ = video indicator
│ │ IMG │ │ IMG │ │ IMG │        │  <- Image/video grid (2dp spacing)
│ │[WA] │ │[CAM]│ │[SS] │        │     with source badges
│ └─────┘ └─────┘ └─────┘        │
│ ┌─────┐ ┌─────┐ ┌─────┐        │
│ ...                             │
└─────────────────────────────────┘
```

### Selection Mode Layout (Current Implementation)

```
┌─────────────────────────────────┐
│ ✕  5 selected                   │  <- Selection toolbar (shows hidden count if any)
├─────────────────────────────────┤
│ [Photos] [Videos]               │  <- Media type chips (still visible)
├─────────────────────────────────┤
│ [7d] [30d] [3mo] [Year] [All]   │  <- Date range chips (still visible)
├─────────────────────────────────┤
│ [All] [WhatsApp] [Camera] [+]   │  <- Source filter chips (still visible)
├─────────────────────────────────┤
│ ┌─────┐ ┌─────┐ ┌─────┐        │
│ │ ✓   │ │     │ │ ✓   │        │
│ │ IMG │ │ IMG │ │ IMG │        │  <- Checkmarks on selected
│ │     │ │     │ │     │        │     (badges hidden in selection mode)
│ └─────┘ └─────┘ └─────┘        │
│                                 │
├─────────────────────────────────┤
│ [Select All]     [ 🗑 Delete ]  │  <- Bottom action bar
└─────────────────────────────────┘
```

---

## Technical Requirements

### Permissions
- ✅ `READ_MEDIA_IMAGES` (Android 13+)
- ✅ `READ_MEDIA_VIDEO` (Android 13+)
- ✅ `READ_EXTERNAL_STORAGE` (Android 12 and below)
- ✅ Trash permission handled via MediaStore.createTrashRequest (Android 11+)

### Data
- ✅ Query MediaStore for images and videos with:
  - `_ID`, `DISPLAY_NAME`, `BUCKET_DISPLAY_NAME`
  - `RELATIVE_PATH` or `DATA` (for source detection)
  - `DATE_ADDED` or `DATE_TAKEN` (for date filtering)
- ✅ Source detection from path/bucket name

### Performance
- ✅ Efficient RecyclerView with ListAdapter + DiffUtil
- ✅ Coil for image/video thumbnail loading with caching
- ✅ No artificial limits on media count

---

## Implementation Phases

### Phase 1: Source Filtering ✅ COMPLETE
- [x] Add source filter chips to UI
- [x] Implement filter logic in ViewModel
- [x] Show image/video count per source
- [x] Persist selected filter
- [x] Add Photos/Videos media type filter

### Phase 2: Enhanced Screenshots Detection ✅ COMPLETE
- [x] Detect screenshots by path patterns
- [x] Screenshots appear as separate source type
- [ ] Add distinct visual style for screenshots badge (optional enhancement)

### Phase 3: Bulk Selection Mode ✅ COMPLETE
- [x] Implement long-press to enter selection mode
- [x] Add selection toolbar UI
- [x] Track selected items in adapter
- [x] Implement select all / deselect all
- [x] Show hidden count when filtered items selected
- [x] Add trash confirmation dialog
- [x] Implement MediaStore trash with createTrashRequest (Android 11+)
- [x] Handle trash result with Undo snackbar
- [x] Exit selection mode after trash

### Phase 4: Date Range Filter ✅ COMPLETE
- [x] Add date range chip group UI
- [x] Implement quick presets (7d, 30d, 3mo, This year, All time)
- [x] Add Custom option with Material DateRangePicker
- [x] Default to "Last 30 days" for better performance
- [x] Apply date filter in ViewModel filter chain
- [x] Source counts reflect date-filtered items
- [x] Persist date range preference

---

### 5. Trash Bin (Safe Delete)

**Goal**: Give users a safety net before permanent deletion.

- **Soft delete**: Move images to trash instead of permanent deletion
- **Trash storage**:
  - Use Android's built-in trash (Android 11+, MediaStore.createTrashRequest)
  - For older Android: app-managed trash folder
- **Trash retention**: Auto-delete after 30 days (configurable)
- **Trash screen**:
  - Accessible from menu/navigation
  - Show trashed images with deletion date
  - "Restore" button to recover images
  - "Empty Trash" to permanently delete all
  - "Delete Forever" for individual items
- **Visual feedback**: Toast/snackbar after delete with "Undo" option

---

## Recommended Additional Features (Industry Standards)

### 6. Storage Insights

**Goal**: Help users understand what's consuming their storage.

- **Storage summary card** at top of screen:
  - Total images count
  - Total storage used by images
  - Breakdown by source (pie chart or bar)
- **Largest images**: Quick filter to show biggest files first
- **"Quick Clean" suggestions**:
  - "You have 234 screenshots using 1.2 GB"
  - "47 WhatsApp images from 2+ years ago"

### 7. Similar/Duplicate Detection

**Goal**: Find redundant images that can be safely deleted.

- Detect duplicates by:
  - Exact file hash (MD5/SHA)
  - Similar images (perceptual hash - optional, more complex)
- Group duplicates together
- Suggest keeping "best" version (highest resolution)
- "Remove duplicates" bulk action

### 8. Empty/Blurry Image Detection

**Goal**: Find low-quality images that are likely unwanted.

- Detect:
  - Very small file sizes (likely corrupted/empty)
  - Blurry images (using image analysis - optional)
  - Screenshots of error messages, loading screens
- Mark as "Suggested for cleanup"
- User can review and bulk delete

### 9. Smart Grouping Options

**Goal**: More ways to organize and find images.

- Group by:
  - Month/Year (timeline view)
  - Location (if GPS data available)
  - Source (current)
  - Size (large/medium/small)
- Collapsible groups with counts

### 10. Undo & History

**Goal**: Prevent accidental data loss.

- Undo last action (snackbar with "Undo" for ~5 seconds)
- Recent actions history (optional settings screen)
- "Recently deleted" quick access

---

## Priority Recommendation

**Must Have (MVP)**:
1. Source Filtering
2. Bulk Selection Mode
3. Trash Bin (Safe Delete)
4. Date Range Filter

**Should Have (v1.1)**:
5. Storage Insights
6. Screenshots Enhancement

**Nice to Have (v2.0)**:
7. Duplicate Detection
8. Smart Grouping
9. Blurry/Empty Detection

---

## Future Considerations

- Cloud backup integration
- Share selected images
- Move images to different folders
- Export cleanup report
- Widget for quick access to storage stats
