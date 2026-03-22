# Task 01: Drag-to-Select

**Parent:** SDD-20260321-002 — Drag-to-Select

## What You're Changing

Currently users must tap each item individually to select it. You will add drag-to-select: after long-pressing to enter selection mode, the user can keep their finger down and drag across items to select them all in one gesture. The RecyclerView will auto-scroll when the finger reaches the top or bottom edge.

This is implemented manually using `RecyclerView.OnItemTouchListener` — no external library needed.

## Before vs After

**Before:**
1. Long-press item → enters selection mode, selects that item
2. Tap item → toggles selection (one at a time)

**After:**
1. Long-press item → enters selection mode, selects that item
2. **Keep finger down and drag** → selects all items the finger passes over
3. Lift finger → drag ends, selection stays
4. Tap item → still toggles selection (one at a time, unchanged)

## Step-by-Step Instructions

### Step 1: Create DragSelectTouchListener.kt

Create a new file `app/src/main/java/com/example/gallerycleaner/DragSelectTouchListener.kt`:

```kotlin
package com.example.gallerycleaner

import android.view.MotionEvent
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Handles drag-to-select on a RecyclerView grid.
 * After activation, tracks finger movement across items and reports
 * selected positions. Auto-scrolls when finger is near top/bottom edges.
 */
class DragSelectTouchListener(
    private val onDragSelection: (Int) -> Unit
) : RecyclerView.OnItemTouchListener {

    private var isActive = false
    private var startPosition = RecyclerView.NO_POSITION
    private var lastPosition = RecyclerView.NO_POSITION

    // Auto-scroll
    private var recyclerView: RecyclerView? = null
    private val autoScrollRunnable = Runnable { autoScroll() }
    private var autoScrollVelocity = 0

    // Edge detection threshold (dp converted to px at attach time)
    private var edgeThresholdPx = 0

    companion object {
        private const val EDGE_THRESHOLD_DP = 64
        private const val AUTO_SCROLL_BASE_SPEED = 8
        private const val AUTO_SCROLL_INTERVAL_MS = 16L
    }

    fun attachToRecyclerView(rv: RecyclerView) {
        recyclerView = rv
        edgeThresholdPx = (EDGE_THRESHOLD_DP * rv.resources.displayMetrics.density).toInt()
        rv.addOnItemTouchListener(this)
    }

    /**
     * Call this to start a drag-select gesture from the given adapter position.
     * Typically called from the long-press handler.
     */
    fun startDragSelection(position: Int) {
        isActive = true
        startPosition = position
        lastPosition = position
    }

    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        if (!isActive) return false

        when (e.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                updateSelection(rv, e)
                handleAutoScroll(rv, e)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                stopDragSelection()
                return true
            }
        }
        return true
    }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
        when (e.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                updateSelection(rv, e)
                handleAutoScroll(rv, e)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                stopDragSelection()
            }
        }
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        if (disallowIntercept) {
            stopDragSelection()
        }
    }

    private fun updateSelection(rv: RecyclerView, e: MotionEvent) {
        val child = rv.findChildViewUnder(e.x, e.y) ?: return
        val position = rv.getChildAdapterPosition(child)
        if (position == RecyclerView.NO_POSITION || position == lastPosition) return

        // Select all items between lastPosition and current position
        val start = minOf(lastPosition, position)
        val end = maxOf(lastPosition, position)
        for (i in start..end) {
            onDragSelection(i)
        }
        lastPosition = position
    }

    private fun handleAutoScroll(rv: RecyclerView, e: MotionEvent) {
        val y = e.y.toInt()
        val height = rv.height

        autoScrollVelocity = when {
            y < edgeThresholdPx -> {
                // Near top edge — scroll up (negative velocity)
                val distance = edgeThresholdPx - y
                -(AUTO_SCROLL_BASE_SPEED + distance / 4)
            }
            y > height - edgeThresholdPx -> {
                // Near bottom edge — scroll down (positive velocity)
                val distance = y - (height - edgeThresholdPx)
                AUTO_SCROLL_BASE_SPEED + distance / 4
            }
            else -> 0
        }

        if (autoScrollVelocity != 0) {
            rv.removeCallbacks(autoScrollRunnable)
            rv.postOnAnimation(autoScrollRunnable)
        } else {
            rv.removeCallbacks(autoScrollRunnable)
        }
    }

    private fun autoScroll() {
        val rv = recyclerView ?: return
        if (!isActive || autoScrollVelocity == 0) return

        rv.scrollBy(0, autoScrollVelocity)
        rv.postOnAnimation(autoScrollRunnable)
    }

    private fun stopDragSelection() {
        isActive = false
        startPosition = RecyclerView.NO_POSITION
        lastPosition = RecyclerView.NO_POSITION
        autoScrollVelocity = 0
        recyclerView?.removeCallbacks(autoScrollRunnable)
    }
}
```

**Key things to notice:**
- `onDragSelection(position)` is called for each adapter position the finger crosses — the caller decides what to do (select the item)
- Auto-scroll uses `postOnAnimation` for smooth 60fps scrolling
- Velocity increases the closer the finger gets to the edge
- `startDragSelection()` must be called externally (from the long-press handler) to activate dragging
- When not active, the listener does nothing and doesn't intercept touches (normal scroll/tap works)

### Step 2: Add a bulk select method to GalleryViewModel

Open `app/src/main/java/com/example/gallerycleaner/GalleryViewModel.kt`.

Add this method near the other selection methods:

```kotlin
fun selectItem(uri: Uri) {
    if (!_isSelectionMode.value) return
    val current = _selectedItems.value
    if (uri !in current) {
        _selectedItems.value = current + uri
    }
}
```

> **Why not `toggleItemSelection`?** During drag, we only want to **add** items to the selection — never deselect. If the user drags back over an already-selected item, it should stay selected. `toggleItemSelection` would deselect it, which feels wrong during a drag gesture.

### Step 3: Wire up drag-select in MainActivity.kt

Open `app/src/main/java/com/example/gallerycleaner/MainActivity.kt`.

**a)** Add a field for the drag listener (near the `adapter` field at the top of the class):

```kotlin
private val dragSelectListener = DragSelectTouchListener { position ->
    val items = adapter.currentList
    if (position in items.indices) {
        viewModel.selectItem(items[position].uri)
    }
}
```

**b)** Attach the listener in `setupRecyclerView()`. Add this line after `binding.recyclerView.adapter = adapter`:

```kotlin
dragSelectListener.attachToRecyclerView(binding.recyclerView)
```

**c)** Update `handleItemLongClick()` to start drag-select. Replace the current method:

```kotlin
private fun handleItemLongClick(item: MediaItem) {
    val state = viewModel.uiState.value
    if (state !is GalleryUiState.Selection) {
        viewModel.enterSelectionMode(item.uri)

        // Find the adapter position and start drag-select
        val position = adapter.currentList.indexOfFirst { it.uri == item.uri }
        if (position >= 0) {
            dragSelectListener.startDragSelection(position)
        }
    }
}
```

**What changed:**
- After entering selection mode, we call `dragSelectListener.startDragSelection(position)` so the user can keep their finger down and drag to select more items
- If the user just long-presses and lifts immediately, only the one item is selected (same as before)

### Step 4: Build and verify

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

### Step 5: Test on device

1. **Basic drag-select**: Long-press an item, keep finger down, drag across items → all items under the finger should be selected
2. **Auto-scroll**: Drag to the bottom edge of the screen → the grid should auto-scroll, continuing to select items
3. **Lift and tap**: After drag-selecting, lift finger, then tap individual items → should toggle as before
4. **Back/X exit**: Press back or X in toolbar → exits selection mode, clears selection
5. **Select All**: Tap Select All → still selects all visible items
6. **Normal scrolling**: When NOT in selection mode, scrolling the grid should work normally (no interference)
7. **Single long-press**: Long-press and immediately lift → only that one item selected (same as before)

## Files Changed

| File | What changed |
|------|-------------|
| `app/src/main/java/.../DragSelectTouchListener.kt` | **New file** — touch listener with auto-scroll |
| `app/src/main/java/.../GalleryViewModel.kt` | Added `selectItem()` method (add-only, no toggle) |
| `app/src/main/java/.../MainActivity.kt` | Wired drag listener, updated long-press handler |

## Implementation Notes

### Range-based drag selection
The drag listener reports the full range (start → current position) on each move. The ViewModel receives the drag range URIs plus the pre-drag selection and sets `selectedItems = preDragSelection + dragRange`. This means:
- Dragging forward selects items in the range
- Dragging back **deselects** items that fall outside the range
- Items selected before the drag (via previous taps) are preserved

### Overscroll disabled
`android:overScrollMode="never"` is set on the RecyclerView to prevent the Android 12+ stretch effect, which caused a visual "shiver" when touching the grid near the top/bottom edges.

### Selection toolbar removed
The top selection toolbar ("X selected" bar) was removed — it pushed the grid down on enter/exit, causing a visible layout shift. The selection count and file size are now shown together in the bottom action bar (e.g., "3 selected · 1.2 MB"). Exiting selection mode is done via the back button (already wired).

### Touch intercept
The `DragSelectTouchListener` only intercepts `ACTION_MOVE` during an active drag. All other events (`ACTION_DOWN`, `ACTION_UP`, `ACTION_CANCEL`) pass through to the RecyclerView so normal scrolling and tap behavior are unaffected.

## Acceptance Criteria

- [ ] Long-press + drag selects multiple items in one gesture
- [ ] Auto-scrolls when dragging near top/bottom edges
- [ ] Dragging back deselects items outside the current range
- [ ] Previously tapped items stay selected during drag
- [ ] Tap-to-toggle still works in selection mode
- [ ] Normal scrolling works when not in selection mode (no shiver/jitter)
- [ ] X button in bottom action bar exits selection mode
- [ ] All existing selection features (Select All, back to exit, hidden count) still work
- [ ] Build succeeds, all tests pass
