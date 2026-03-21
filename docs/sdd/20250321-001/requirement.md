# SDD-20250321-001: Gallery Grid Visual Redesign

## Summary

Update the gallery grid item layout, header, video indicators, source badges, and Continue FAB to match the new Stitch-generated design. This is a visual refresh of the main gallery screen — no changes to business logic, state management, or navigation.

## Design Reference

The new design shows the main screen in three states (see attached screenshot):
1. **Left panel**: Top of the gallery showing header + filter chips + first row of images (fresh/unviewed)
2. **Middle panel**: Scrolled gallery with mixed content including videos, viewed items (dimmed), and a "FAB" Continue button
3. **Right panel**: Further scrolled with more video items and an extended "Continue" FAB

## Task Breakdown

| Task | File | Description |
|------|------|-------------|
| [Task 01](task-01-header-redesign.md) | Header redesign | Add app title, restructure top bar to two rows |
| [Task 02](task-02-source-badge-redesign.md) | Source badge redesign | Pill shape, view-state-dependent colors |
| [Task 03](task-03-video-duration-badge.md) | Video duration badge | Replace play icon with duration text badge |
| [Task 04](task-04-grid-and-fab.md) | Grid styling + Extended FAB | Edge-to-edge grid, rounded corners, extended Continue FAB |

Tasks can be done in any order. Tasks 02 and 03 both modify `item_image.xml` and `ImageAdapter.kt`, so coordinate if doing them in parallel.

---

## Changes Required

### 1. Header / Top Bar Redesign

**Current:** Single horizontal row containing ChipGroup (Photos/Videos) + Filters tonal button, no app title.

**New:**
- Add app title "Gallery Cleaner" as a prominent heading (24sp semi-bold, dark green `#1B5E20`) on the left
- "Filters" button on the right of the title row — styled as a pill/rounded-full button with light teal background (`#E0F2F1`) and teal text (`#00796B`)
- Photos/Videos chips move to a **second row** below the title, as standalone pill-shaped toggle buttons (not Material Filter Chips)
  - Selected state: light teal background (`#E0F2F1`), teal text (`#00796B`), teal border
  - Unselected state: white background, gray border (`#D1D5DB`), gray text (`#4B5563`)
- The header section should have a subtle bottom shadow (`shadow-sm` equivalent = ~1dp elevation or a thin divider)
- Vertical spacing: 16dp between title row and chips row, 16dp bottom padding

**Files affected:**
- `activity_main.xml` — restructure `topBar` LinearLayout
- `MainActivity.kt` — update binding references if view IDs change
- `colors.xml` — add new colors (`badge_dark_teal`, `badge_light_teal`, `header_title_green`)

### 2. Source Badge Redesign

**Current:** All badges use the same style — white text on semi-transparent black background (`#66000000`), 12dp corners, 12sp text, positioned at bottom-left with 8dp margin.

**New:** View-state-dependent badge colors (text only, no icons), positioned closer to the edge.

#### Badge Color Rules (based on viewed state, not source):
| State | Background | Text Color |
|-------|-----------|------------|
| **Unviewed** (fresh) | Dark teal `#C9E5E4` | White |
| **Viewed** (scrolled past) | Semi-transparent white `#D9FFFFFF` (85% white) | Dark gray `#1F2937` |

#### Badge Layout Changes:
- **Position**: bottom-left, **4dp** margin (was 8dp)
- **Shape**: fully rounded pill (`rounded-full` = 999dp corner radius, or use a capsule shape)
- **Text size**: 11sp (was 12sp)
- **Padding**: 8dp horizontal, 2dp vertical (was 8dp h / 4dp v — slimmer)
- **Layout**: Keep as `TextView` (no icons needed)

**Files affected:**
- `item_image.xml` — update margin, padding, corner radius on `sourceBadge`
- `ImageAdapter.kt` — update `bind()` to set badge background and text color per source type
- Add new drawables: `badge_bg_dark_teal.xml`, `badge_bg_light_teal.xml`, `badge_bg_white.xml`

### 3. Video Indicator Redesign

**Current:** A white play circle icon (24dp) at top-left corner, no duration text.

**New:** A duration badge at top-left showing play icon + timestamp.

#### Specification:
- **Position**: top-left, 4dp margin (same as current)
- **Background**: semi-transparent black `#99000000` (60% black), small rounded rectangle (4dp corners)
- **Content**: Play triangle icon (10dp, white) + duration text (e.g., "0:45", "1:30")
- **Text**: 10sp, white, medium weight
- **Padding**: 6dp horizontal, 2dp vertical
- **Layout**: horizontal with 4dp gap between icon and text

#### Data requirement:
- `MediaItem` needs a `duration: Long` field (milliseconds) — queried from `MediaStore.Video.VideoColumns.DURATION`
- Duration formatted as `M:SS` (e.g., 45000ms → "0:45", 90000ms → "1:30")

**Files affected:**
- `item_image.xml` — replace `videoIndicator` ImageView with a container (LinearLayout or TextView with compound drawable)
- `MediaItem.kt` — add `duration: Long = 0L` field
- `GalleryViewModel.kt` — query `DURATION` column from MediaStore for video items
- `ImageAdapter.kt` — format and display duration, update visibility logic

### 4. Grid Styling Changes

**Current:** 2dp margin on each item, 8dp padding on RecyclerView, no item rounding.

**New:**
- **Item gap**: 2px (keep as 1dp, which is effectively the 2px from the design on most screens)
- **Item corners**: Small rounded corners (~4dp radius) on each grid cell using `clipToOutline` with a rounded background or `ShapeableImageView`
- **RecyclerView padding**: Remove the 8dp padding (grid goes edge-to-edge in the design, items touch screen edges)

**Files affected:**
- `item_image.xml` — reduce margin to 1dp, add corner rounding to the root or imageView
- `activity_main.xml` — remove or reduce RecyclerView padding

### 5. Viewed Items Opacity

**Current:** 50% opacity (`alpha = 0.5f`)

**New:** 60% opacity (`alpha = 0.6f`) — slightly more visible than current.

**Files affected:**
- `ImageAdapter.kt` — change `0.5f` to `0.6f` in `updateViewedVisuals()`

### 6. Continue FAB → Extended FAB

**Current:** Standard FAB with just a down-arrow icon.

**New:** Extended FAB with download/arrow icon + "Continue" text label.

#### Specification:
- Use `ExtendedFloatingActionButton` instead of `FloatingActionButton`
- Icon: down-arrow (keep existing `ic_arrow_downward`)
- Text: "Continue"
- Background: teal `#057A70`
- Text/icon color: white
- Position: bottom-right, 24dp margin from edges

**Files affected:**
- `activity_main.xml` — replace `FloatingActionButton` with `ExtendedFloatingActionButton`
- `MainActivity.kt` — update type reference from FAB to ExtendedFAB (API is mostly compatible)

---

## Files Summary

| File | Change Type |
|------|------------|
| `activity_main.xml` | Major — header restructure, RecyclerView padding, ExtendedFAB |
| `item_image.xml` | Major — badge container, video duration badge, corners, margins |
| `ImageAdapter.kt` | Major — badge colors/icons per source, duration formatting, opacity |
| `MediaItem.kt` | Minor — add `duration` field |
| `GalleryViewModel.kt` | Minor — query video duration from MediaStore |
| `MainActivity.kt` | Minor — updated view references, ExtendedFAB type |
| `colors.xml` | Minor — add new badge/header colors |
| New drawables | Add badge backgrounds and source icons |

## Out of Scope

- Selection mode visuals (no changes shown in the design)
- Filter bottom sheet (no changes shown)
- Media viewer (no changes shown)
- Empty states (no changes shown)
- Dark theme (apply light theme changes first)
- Business logic, filtering, sorting, state management
