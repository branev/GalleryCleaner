# Task 01: Pinch-to-Zoom Grid

**Parent:** SDD-20260321-006 — Pinch-to-Zoom Grid

## What You're Changing

Currently the grid is always 3 columns. You will add a pinch gesture so users can zoom in (2 columns, larger thumbnails) or zoom out (5 columns, more items visible). The column count persists across app restarts, and image loading quality adjusts to match.

## Before vs After

**Before:**
- Always 3 columns, no way to change

**After:**
- Pinch out (spread fingers) → fewer columns (2 = large thumbnails)
- Pinch in (close fingers) → more columns (5 = small thumbnails)
- Continuous: can go from 2 → 5 in one smooth pinch
- Bidirectional: can reverse mid-pinch
- Persisted: remembered on restart

## Step-by-Step Instructions

### Step 1: Add grid column persistence to FilterPreferences

Open `app/src/main/java/com/example/gallerycleaner/FilterPreferences.kt`.

**a)** Add a new StateFlow and loader near the other fields (around line 22):

```kotlin
private val _gridColumnCount = MutableStateFlow(loadGridColumnCount())
val gridColumnCount: StateFlow<Int> = _gridColumnCount.asStateFlow()
```

**b)** Add the load method near the other `load*` methods:

```kotlin
private fun loadGridColumnCount(): Int {
    return prefs.getInt(KEY_GRID_COLUMNS, 3) // Default: 3 columns
}
```

**c)** Add the save method near the other `save*` methods:

```kotlin
fun saveGridColumnCount(count: Int) {
    prefs.edit().putInt(KEY_GRID_COLUMNS, count).apply()
    _gridColumnCount.value = count
}
```

**d)** Add the key to the companion object:

```kotlin
private const val KEY_GRID_COLUMNS = "grid_column_count"
```

### Step 2: Expose grid column count in GalleryViewModel

Open `app/src/main/java/com/example/gallerycleaner/GalleryViewModel.kt`.

Add near the other filter state exposures (around line 70):

```kotlin
val gridColumnCount: StateFlow<Int> = filterPreferences.gridColumnCount

fun setGridColumnCount(count: Int) {
    filterPreferences.saveGridColumnCount(count.coerceIn(2, 5))
}
```

### Step 3: Add ScaleGestureDetector in MainActivity

Open `app/src/main/java/com/example/gallerycleaner/MainActivity.kt`.

**a)** Add a field near the other fields at the top of the class:

```kotlin
private lateinit var scaleGestureDetector: ScaleGestureDetector
```

**b)** Add the import at the top:

```kotlin
import android.view.ScaleGestureDetector
```

**c)** In `setupRecyclerView()`, after `fastScrollHelper.attach()`, add:

```kotlin
// Pinch-to-zoom grid columns
var cumulativeScale = 1.0f
scaleGestureDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
        cumulativeScale = 1.0f
        return true
    }

    override fun onScale(detector: ScaleGestureDetector): Boolean {
        cumulativeScale *= detector.scaleFactor
        val layoutManager = binding.recyclerView.layoutManager as GridLayoutManager
        val currentSpan = layoutManager.spanCount

        val newSpan = when {
            cumulativeScale < 0.8f -> {
                cumulativeScale = 1.0f // Reset so next threshold triggers another step
                (currentSpan + 1).coerceAtMost(5)
            }
            cumulativeScale > 1.2f -> {
                cumulativeScale = 1.0f
                (currentSpan - 1).coerceAtLeast(2)
            }
            else -> currentSpan
        }

        if (newSpan != currentSpan) {
            changeGridColumns(layoutManager, newSpan)
        }
        return true
    }
})
```

**d)** Forward touch events to the detector. Add this after the `scaleGestureDetector` setup:

```kotlin
binding.recyclerView.setOnTouchListener { _, event ->
    scaleGestureDetector.onTouchEvent(event)
    false // Don't consume — let RecyclerView handle scroll/click too
}
```

**e)** Add the `changeGridColumns` method:

```kotlin
private fun changeGridColumns(layoutManager: GridLayoutManager, newSpanCount: Int) {
    // Preserve scroll position
    val firstVisible = layoutManager.findFirstVisibleItemPosition()

    // Update span count
    layoutManager.spanCount = newSpanCount

    // Restore position
    if (firstVisible != RecyclerView.NO_POSITION) {
        layoutManager.scrollToPosition(firstVisible)
    }

    // Update image loading size
    adapter.thumbnailSize = resources.displayMetrics.widthPixels / newSpanCount

    // Persist
    viewModel.setGridColumnCount(newSpanCount)
}
```

### Step 4: Add thumbnailSize to ImageAdapter

Open `app/src/main/java/com/example/gallerycleaner/ImageAdapter.kt`.

**a)** Add a mutable field near the top of the class (after `viewedItems`):

```kotlin
var thumbnailSize: Int = 300
```

**b)** In the `bind()` method, replace the hardcoded `size(300)` with:

```kotlin
size(thumbnailSize)
```

### Step 5: Initialize grid columns from saved preference

Back in `MainActivity.kt`, update `setupRecyclerView()` to use the saved column count instead of the XML default:

Replace:
```kotlin
val layoutManager = GridLayoutManager(this, resources.getInteger(R.integer.grid_span_count))
```

With:
```kotlin
val savedColumns = viewModel.gridColumnCount.value
val layoutManager = GridLayoutManager(this, savedColumns)
```

And set the initial thumbnail size after creating the adapter:
```kotlin
adapter.thumbnailSize = resources.displayMetrics.widthPixels / savedColumns
```

Add this line right before `binding.recyclerView.adapter = adapter`.

### Step 6: Build and verify

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

### Step 7: Test on device

1. **Pinch out** (spread two fingers): columns decrease (3 → 2), thumbnails get bigger
2. **Pinch in** (close two fingers): columns increase (3 → 4 → 5), thumbnails get smaller
3. **Continuous pinch**: one long pinch-in goes 2 → 3 → 4 → 5 smoothly
4. **Reverse mid-pinch**: start pinching in, then spread — columns should reverse
5. **Boundaries**: can't go below 2 or above 5 columns
6. **Persistence**: change to 4 columns, kill and reopen app — should start at 4 columns
7. **Scroll position**: zoom while scrolled down — should stay at approximately the same items
8. **Image quality**: at 2 columns thumbnails should be sharper, at 5 they can be slightly softer (smaller load size)
9. **Normal scrolling**: single-finger scroll still works normally
10. **Drag-to-select**: long-press + drag still works after zooming
11. **Fast scroller**: fast scroll thumb still works after zooming

## Files Changed

| File | What changed |
|------|-------------|
| `app/src/main/java/.../FilterPreferences.kt` | Added `gridColumnCount` StateFlow + save/load |
| `app/src/main/java/.../GalleryViewModel.kt` | Exposed `gridColumnCount`, added `setGridColumnCount()` |
| `app/src/main/java/.../MainActivity.kt` | ScaleGestureDetector, `changeGridColumns()`, saved column init |
| `app/src/main/java/.../ImageAdapter.kt` | Added `thumbnailSize` field, use in Coil load |

## Acceptance Criteria

- [ ] Pinch-out shows fewer, larger thumbnails (minimum 2 columns)
- [ ] Pinch-in shows more, smaller thumbnails (maximum 5 columns)
- [ ] Continuous: multiple steps in one pinch gesture
- [ ] Bidirectional: can reverse direction mid-pinch
- [ ] Scroll position approximately maintained during zoom
- [ ] Column preference persists across app restarts
- [ ] Image loading size adjusts for column count
- [ ] Pinch gesture doesn't interfere with normal scrolling, drag-to-select, or fast scroll
- [ ] Build succeeds, all tests pass
