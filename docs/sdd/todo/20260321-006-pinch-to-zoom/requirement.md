# SDD-20260321-006: Pinch-to-Zoom Grid

## Summary

Allow users to change the grid column count by pinching on the RecyclerView. Pinch-in shows fewer, larger thumbnails (2 columns); pinch-out shows more, smaller thumbnails (5 columns). Useful for scanning large libraries quickly or reviewing individual photos.

---

## Changes Required

### 1. ScaleGestureDetector

Attach a `ScaleGestureDetector` to the RecyclerView to detect pinch gestures.

#### Column levels:
| Columns | Use case |
|---------|----------|
| 2 | Detail review — large thumbnails |
| 3 | Default — balanced (current) |
| 4 | Overview — more items visible |
| 5 | Scan mode — maximum density |

#### Gesture behavior:
- Pinch-in (scale factor < 1.0) → increase column count (more, smaller)
- Pinch-out (scale factor > 1.0) → decrease column count (fewer, larger)
- Threshold: only change when cumulative scale factor crosses 0.8 or 1.2 to avoid accidental changes
- Continuous: multiple column changes allowed in a single pinch gesture as long as fingers stay down (e.g., can go from 2 to 5 in one smooth pinch-in)
- Bidirectional: user can reverse direction mid-pinch (zoom in then back out without lifting fingers)

### 2. GridLayoutManager Span Update

When column count changes:
- Update `GridLayoutManager.spanCount`
- Call `adapter.notifyItemRangeChanged(0, adapter.itemCount)` to resize all visible items
- Maintain approximate scroll position (keep the same items visible)

#### Scroll position preservation:
```kotlin
// Before changing span count
val layoutManager = recyclerView.layoutManager as GridLayoutManager
val firstVisible = layoutManager.findFirstVisibleItemPosition()

// Change span count
layoutManager.spanCount = newSpanCount

// Restore position
layoutManager.scrollToPosition(firstVisible)
```

### 3. Persist Preference

Save the user's preferred column count in `SharedPreferences` (or `FilterPreferences`) so it persists across app restarts.

### 4. Coil Image Size Adjustment

Currently Coil loads thumbnails at `size(300)`. When zooming to 2 columns, load at higher resolution; at 5 columns, lower resolution is fine.

```kotlin
// Approximate target size based on screen width / column count
val targetSize = screenWidth / spanCount
```

This improves performance at high column counts and quality at low column counts.

---

## Files Summary

| File | Change Type |
|------|------------|
| `MainActivity.kt` | Moderate — ScaleGestureDetector setup, span count changes, scroll preservation |
| `ImageAdapter.kt` | Minor — adjust Coil size parameter based on span count |
| `FilterPreferences.kt` | Minor — persist grid column count |
| `integers.xml` | Minor — may want min/max column count resources |

## Acceptance Criteria

- [ ] Pinch-out on grid shows fewer, larger thumbnails (minimum 2 columns)
- [ ] Pinch-in on grid shows more, smaller thumbnails (maximum 5 columns)
- [ ] Scroll position is approximately maintained during zoom
- [ ] Column preference persists across app restarts
- [ ] Image quality adjusts appropriately for column count
- [ ] Pinch gesture doesn't interfere with normal scrolling
- [ ] Build succeeds, all tests pass

## Out of Scope

- Smooth animated column transitions (snap between levels)
- Different column counts for landscape vs portrait
- Column count indicator UI
