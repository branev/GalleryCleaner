# Test Coverage Assessment — March 22, 2026

## Current Coverage

| File | What's tested | Coverage |
|------|--------------|---------|
| `SourceDetectorTest` | All source detection paths, priority, edge cases | Excellent |
| `FilterPreferencesTest` | Load/save, invalid data, empty sets | Solid |
| `MediaItemTest` | Equality, nullability, enum labels/values | Adequate |
| `GalleryViewModelTest` | Filtering, source counts, toggle, select, delete — via mirrored helper functions | Good breadth, but indirect |

## Gaps to Address

### 1. Drag selection logic (new code, zero coverage)
`setDragSelection` method and its interaction with pre-existing selection.

**Tests needed:**
- Union of drag range + pre-existing: dragging over 3 items when 2 were already selected yields 5 (or fewer if overlapping)
- Drag shrinks when reversed: drag 1→5, then back to 1→3 — items 4,5 drop out
- No-op when not in selection mode
- Pre-existing items preserved outside drag range

### 2. getSelectedItemsTotalSize (new code, no coverage)
- Sum of size for selected URIs
- Zero when nothing selected
- Correct when some URIs don't match any items

### 3. Edge case: drag across empty adapter positions
The `onDragRangeChanged` callback does `.filter { it in items.indices }` — verify out-of-bounds positions are dropped.

### 4. enterSelectionMode / exitSelectionMode state transitions
Using mirrored-function approach (same pattern as existing tests) since `GalleryViewModel` is an `AndroidViewModel`.

## Not Adding

- **DragSelectTouchListener tests** — touch handler coupled to RecyclerView, needs instrumented tests
- **More MediaItemTest label tests** — already exhaustive
- **Animation / overlay tests** — UI layer, belongs in Espresso if at all

## Implementation Plan

Add all new tests to `GalleryViewModelTest.kt` using the existing mirrored-function approach. This covers the new feature logic with minimal effort and no new dependencies.
