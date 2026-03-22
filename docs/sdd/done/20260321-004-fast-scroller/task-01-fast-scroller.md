# Task 01: Fast Scroller with Date Tooltip

**Parent:** SDD-20260321-004 — Fast Scroller with Date Tooltip

## What You're Changing

Currently users scroll the gallery grid by swiping, which is slow for large libraries (thousands of items). You will add a fast-scroll thumb on the right edge that users can drag to jump quickly through the grid. While dragging, a tooltip bubble shows the date of the items at that position (e.g., "Mar 2026", "Yesterday").

## Before vs After

**Before:**
- User swipes up/down to scroll — slow for large libraries
- No visible scrollbar

**After:**
- A thin thumb appears on the right edge when scrolling
- User can grab and drag the thumb to fast-scroll
- A date tooltip appears next to the thumb while dragging
- Thumb auto-hides after 1.5 seconds of no scrolling

## Step-by-Step Instructions

### Step 1: Add new colors

Open `app/src/main/res/values/colors.xml` and add:

```xml
<!-- Fast scroller (SDD-20260321-004) -->
<color name="fast_scroll_track">#E0E0E0</color>
<color name="fast_scroll_thumb">#80008080</color>
<color name="fast_scroll_tooltip_bg">#008080</color>
```

### Step 2: Create drawable for the tooltip bubble

Create `app/src/main/res/drawable/fast_scroll_tooltip_bg.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <corners android:radius="8dp" />
    <solid android:color="@color/fast_scroll_tooltip_bg" />
</shape>
```

Create `app/src/main/res/drawable/fast_scroll_thumb.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <corners android:radius="2dp" />
    <solid android:color="@color/fast_scroll_thumb" />
</shape>
```

### Step 3: Add fast scroller views to activity_main.xml

Open `app/src/main/res/layout/activity_main.xml`.

Add these views **after the RecyclerView** but **before the Selection Action Bar**. They overlay the RecyclerView:

```xml
<!-- Fast Scroll Track (right edge) -->
<View
    android:id="@+id/fastScrollTrack"
    android:layout_width="2dp"
    android:layout_height="0dp"
    android:layout_marginEnd="4dp"
    android:background="@color/fast_scroll_track"
    android:alpha="0"
    app:layout_constraintTop_toBottomOf="@id/filterLoadingIndicator"
    app:layout_constraintBottom_toBottomOf="parent"
    app:layout_constraintEnd_toEndOf="parent" />

<!-- Fast Scroll Thumb (draggable) -->
<View
    android:id="@+id/fastScrollThumb"
    android:layout_width="4dp"
    android:layout_height="48dp"
    android:layout_marginEnd="3dp"
    android:background="@drawable/fast_scroll_thumb"
    android:alpha="0"
    android:elevation="2dp"
    app:layout_constraintTop_toBottomOf="@id/filterLoadingIndicator"
    app:layout_constraintEnd_toEndOf="parent" />

<!-- Fast Scroll Date Tooltip -->
<TextView
    android:id="@+id/fastScrollTooltip"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_marginEnd="16dp"
    android:background="@drawable/fast_scroll_tooltip_bg"
    android:paddingHorizontal="12dp"
    android:paddingVertical="6dp"
    android:textColor="@android:color/white"
    android:textSize="14sp"
    android:textStyle="bold"
    android:alpha="0"
    android:elevation="3dp"
    app:layout_constraintEnd_toStartOf="@id/fastScrollThumb"
    app:layout_constraintTop_toTopOf="@id/fastScrollThumb"
    app:layout_constraintBottom_toBottomOf="@id/fastScrollThumb" />
```

**Key things to notice:**
- All three views start with `alpha="0"` (invisible) — they fade in when scrolling
- The track is constrained to the RecyclerView's area (top to bottom)
- The thumb is constrained to the right edge — its vertical position will be set programmatically
- The tooltip is constrained to the left of the thumb, vertically centered on it
- The thumb and tooltip have `elevation` so they render above grid items

### Step 4: Create FastScrollHelper.kt

Create a new file `app/src/main/java/com/example/gallerycleaner/FastScrollHelper.kt`:

```kotlin
package com.example.gallerycleaner

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Manages fast-scroll behavior: thumb positioning, drag handling,
 * date tooltip display, and auto-hide.
 */
class FastScrollHelper(
    private val recyclerView: RecyclerView,
    private val track: View,
    private val thumb: View,
    private val tooltip: TextView,
    private val getDateAtPosition: (Int) -> String
) {
    private var isDragging = false
    private val hideRunnable = Runnable { hide() }

    // Touch zone: touches within this distance from the right edge activate fast scroll
    private val touchZonePx = (24 * recyclerView.resources.displayMetrics.density).toInt()

    private val monthYearFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())

    companion object {
        private const val SHOW_DURATION_MS = 1500L
        private const val FADE_DURATION_MS = 200L
    }

    @SuppressLint("ClickableViewAccessibility")
    fun attach() {
        // Update thumb position when RecyclerView scrolls
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (!isDragging && (dy != 0)) {
                    updateThumbPosition()
                    show()
                    scheduleHide()
                }
            }
        })

        // Handle drag on the thumb area
        // We use a touch listener on the RecyclerView's parent area
        // to catch touches in the fast-scroll zone
        thumb.setOnTouchListener { _, event ->
            handleThumbTouch(event)
            true
        }

        // Also intercept touches on the track
        track.setOnTouchListener { _, event ->
            handleThumbTouch(event)
            true
        }
    }

    private fun handleThumbTouch(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isDragging = true
                thumb.handler?.removeCallbacks(hideRunnable)
                showTooltip()
                scrollToY(event.rawY)
            }
            MotionEvent.ACTION_MOVE -> {
                scrollToY(event.rawY)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                hideTooltip()
                scheduleHide()
            }
        }
    }

    private fun scrollToY(rawY: Float) {
        // Convert raw Y to position within the track
        val trackLocation = IntArray(2)
        track.getLocationOnScreen(trackLocation)
        val trackTop = trackLocation[1].toFloat()
        val trackHeight = track.height.toFloat()

        if (trackHeight <= 0) return

        val relativeY = (rawY - trackTop).coerceIn(0f, trackHeight)
        val proportion = relativeY / trackHeight

        // Calculate adapter position
        val layoutManager = recyclerView.layoutManager as? GridLayoutManager ?: return
        val itemCount = recyclerView.adapter?.itemCount ?: return
        if (itemCount == 0) return

        val targetPosition = (proportion * (itemCount - 1)).toInt().coerceIn(0, itemCount - 1)

        // Scroll to position
        layoutManager.scrollToPositionWithOffset(targetPosition, 0)

        // Update thumb position
        updateThumbPositionFromProportion(proportion)

        // Update tooltip text
        tooltip.text = getDateAtPosition(targetPosition)
    }

    fun updateThumbPosition() {
        val layoutManager = recyclerView.layoutManager as? GridLayoutManager ?: return
        val itemCount = recyclerView.adapter?.itemCount ?: return
        if (itemCount == 0) return

        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        if (firstVisible == RecyclerView.NO_POSITION) return

        val proportion = firstVisible.toFloat() / itemCount.toFloat()
        updateThumbPositionFromProportion(proportion)
    }

    private fun updateThumbPositionFromProportion(proportion: Float) {
        val trackHeight = track.height.toFloat()
        val thumbHeight = thumb.height.toFloat()
        if (trackHeight <= 0) return

        val maxTranslation = trackHeight - thumbHeight
        val translation = (proportion * maxTranslation).coerceIn(0f, maxTranslation)

        thumb.translationY = translation
        tooltip.translationY = translation
    }

    private fun show() {
        track.animate().alpha(1f).setDuration(FADE_DURATION_MS).start()
        thumb.animate().alpha(1f).setDuration(FADE_DURATION_MS).start()
    }

    private fun hide() {
        if (isDragging) return
        track.animate().alpha(0f).setDuration(FADE_DURATION_MS).start()
        thumb.animate().alpha(0f).setDuration(FADE_DURATION_MS).start()
    }

    private fun showTooltip() {
        show()
        tooltip.animate().alpha(1f).setDuration(FADE_DURATION_MS).start()
    }

    private fun hideTooltip() {
        tooltip.animate().alpha(0f).setDuration(FADE_DURATION_MS).start()
    }

    private fun scheduleHide() {
        thumb.handler?.removeCallbacks(hideRunnable)
        thumb.handler?.postDelayed(hideRunnable, SHOW_DURATION_MS)
    }
}
```

**Key things to notice:**
- `attach()` sets up the scroll listener and touch handlers
- `scrollToY()` converts a raw screen Y coordinate to an adapter position and scrolls there
- `updateThumbPosition()` is called on normal scrolls to keep the thumb in sync
- `show()`/`hide()` fade the track + thumb in/out
- `showTooltip()`/`hideTooltip()` fade the date bubble
- `getDateAtPosition` is a lambda provided by the caller (MainActivity) — it returns the formatted date string for a given adapter position
- Touch handlers are on the thumb and track views themselves, so they don't interfere with `DragSelectTouchListener` which listens on the RecyclerView

### Step 5: Wire up in MainActivity.kt

Open `app/src/main/java/com/example/gallerycleaner/MainActivity.kt`.

**a)** Add a field for the helper (near the other fields at the top):

```kotlin
private lateinit var fastScrollHelper: FastScrollHelper
```

**b)** Initialize it at the end of `setupRecyclerView()`:

```kotlin
fastScrollHelper = FastScrollHelper(
    recyclerView = binding.recyclerView,
    track = binding.fastScrollTrack,
    thumb = binding.fastScrollThumb,
    tooltip = binding.fastScrollTooltip,
    getDateAtPosition = { position -> formatDateForPosition(position) }
)
fastScrollHelper.attach()
```

**c)** Add the date formatting method:

```kotlin
private fun formatDateForPosition(position: Int): String {
    val items = adapter.currentList
    if (position !in items.indices) return ""

    val timestamp = items[position].dateAdded
    val itemDate = Date(timestamp * 1000L)
    val now = Calendar.getInstance()
    val itemCal = Calendar.getInstance().apply { time = itemDate }

    return when {
        // Today
        now.get(Calendar.YEAR) == itemCal.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == itemCal.get(Calendar.DAY_OF_YEAR) -> "Today"

        // Yesterday
        now.get(Calendar.YEAR) == itemCal.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) - itemCal.get(Calendar.DAY_OF_YEAR) == 1 -> "Yesterday"

        // Same year — show "Mar 15"
        now.get(Calendar.YEAR) == itemCal.get(Calendar.YEAR) ->
            SimpleDateFormat("MMM d", Locale.getDefault()).format(itemDate)

        // Different year — show "Mar 2025"
        else -> SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(itemDate)
    }
}
```

### Step 6: Build and verify

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

### Step 7: Test on device

1. **Scroll normally**: Swipe the grid up/down — a thin thumb should appear on the right edge, then auto-hide after 1.5 seconds
2. **Drag the thumb**: Touch and drag the thumb up/down — the grid should jump quickly, and a date tooltip ("Mar 2026", "Yesterday", etc.) should appear next to the thumb
3. **Release**: Lift finger — tooltip fades out, thumb auto-hides after 1.5s
4. **Drag-to-select compatibility**: Enter selection mode, then try horizontal drag-select — it should still work. The fast scroller should only activate from touches on the right edge.
5. **Selection mode + fast scroll**: In selection mode, drag the fast scroll thumb — it should scroll without selecting items
6. **Small library**: If there are very few items (e.g., 6), the thumb should still appear but dragging won't move far
7. **Tap the track**: Tapping on the track area should jump to that position

## Files Changed

| File | What changed |
|------|-------------|
| `app/src/main/res/values/colors.xml` | Added 3 fast scroller colors |
| `app/src/main/res/drawable/fast_scroll_tooltip_bg.xml` | **New file** — teal rounded rectangle |
| `app/src/main/res/drawable/fast_scroll_thumb.xml` | **New file** — semi-transparent teal thumb |
| `app/src/main/res/layout/activity_main.xml` | Added track, thumb, and tooltip views |
| `app/src/main/java/.../FastScrollHelper.kt` | **New file** — scroll/drag/tooltip logic |
| `app/src/main/java/.../MainActivity.kt` | Wired FastScrollHelper, added date formatting |

## Status: COMPLETE

## Acceptance Criteria

- [x] A scroll thumb appears on the right edge when scrolling
- [x] The thumb can be grabbed and dragged to fast-scroll through the list
- [x] A date tooltip appears while dragging showing the date at the current position
- [x] The thumb auto-hides after 1.5 seconds of inactivity
- [x] Dates are formatted appropriately ("Today", "Yesterday", "Mar 15", "Mar 2025")
- [x] Fast scroll does NOT interfere with drag-to-select
- [x] Fast scroll works in both Normal and Selection modes (scroll only, no selecting)
- [x] Transparent track (no visible gray bar)
- [x] Build succeeds, all tests pass
