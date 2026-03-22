# Task 01: Hint Preferences & Tooltip System

**Parent:** SDD-20260321-008 — Contextual Hints & Help

## What You're Building

The foundation for contextual hints: a preferences wrapper to track which hints have been shown, and a reusable tooltip view that can be anchored to any UI element.

## Step-by-Step Instructions

### Step 1: Add hint strings

Open `app/src/main/res/values/strings.xml` and add:

```xml
<!-- Contextual hints (SDD-20260321-008) -->
<string name="hint_long_press">Long-press to select items for deletion</string>
<string name="hint_filters">Filter by source, date range, or type</string>
<string name="hint_drag_select">Swipe horizontally across items to select more</string>
<string name="hint_pinch_zoom">Pinch to zoom the grid in or out</string>
<string name="hint_fast_scroll">Drag the right edge to scroll quickly</string>
<string name="hint_continue_fab">Tap to jump to where you left off</string>
<string name="hint_progress_bar">This bar shows how much you\'ve reviewed</string>
<string name="hint_trash_undo">Items go to trash for 30 days — you can undo</string>
<string name="hints_reset_confirmation">Tips will appear again as you use the app</string>
```

### Step 2: Add tooltip color

Open `app/src/main/res/values/colors.xml` and add:

```xml
<!-- Hint tooltip (SDD-20260321-008) -->
<color name="hint_tooltip_bg">#2C2C2E</color>
```

### Step 3: Create tooltip background drawable

Create `app/src/main/res/drawable/hint_tooltip_bg.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <corners android:radius="8dp" />
    <solid android:color="@color/hint_tooltip_bg" />
</shape>
```

### Step 4: Create HintPreferences.kt

Create `app/src/main/java/com/example/gallerycleaner/HintPreferences.kt`:

```kotlin
package com.example.gallerycleaner

import android.content.Context

/**
 * Tracks which contextual hints have been shown.
 * Each hint is shown once, then permanently dismissed.
 * resetAllHints() re-enables them all.
 */
class HintPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isHintShown(hintId: String): Boolean = prefs.getBoolean(hintId, false)

    fun markHintShown(hintId: String) {
        prefs.edit().putBoolean(hintId, true).apply()
    }

    fun resetAllHints() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "hint_prefs"

        // Hint IDs
        const val HINT_LONG_PRESS = "hint_long_press"
        const val HINT_FILTERS = "hint_filters"
        const val HINT_DRAG_SELECT = "hint_drag_select"
        const val HINT_PINCH_ZOOM = "hint_pinch_zoom"
        const val HINT_FAST_SCROLL = "hint_fast_scroll"
        const val HINT_CONTINUE_FAB = "hint_continue_fab"
        const val HINT_PROGRESS_BAR = "hint_progress_bar"
        const val HINT_TRASH_UNDO = "hint_trash_undo"

        // Priority order (index = priority, lower = higher priority)
        val PRIORITY_ORDER = listOf(
            HINT_LONG_PRESS,
            HINT_FILTERS,
            HINT_DRAG_SELECT,
            HINT_PINCH_ZOOM,
            HINT_FAST_SCROLL,
            HINT_CONTINUE_FAB,
            HINT_PROGRESS_BAR,
            HINT_TRASH_UNDO
        )
    }
}
```

### Step 5: Create HintManager.kt

Create `app/src/main/java/com/example/gallerycleaner/HintManager.kt`:

```kotlin
package com.example.gallerycleaner

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView

/**
 * Manages the display of contextual hint tooltips.
 * Shows one hint at a time, queues others, and respects the
 * max-2-per-session limit.
 */
class HintManager(
    private val prefs: HintPreferences,
    private val rootView: ViewGroup
) {
    private var currentPopup: PopupWindow? = null
    private val queue = mutableListOf<PendingHint>()
    private var hintsShownThisSession = 0

    companion object {
        private const val MAX_HINTS_PER_SESSION = 2
        private const val SHOW_DELAY_MS = 500L
        private const val NEXT_HINT_DELAY_MS = 500L
    }

    private data class PendingHint(
        val hintId: String,
        val message: String,
        val anchorView: View,
        val priority: Int
    )

    /**
     * Request to show a hint. If the hint has already been shown (persisted),
     * or the session limit is reached, this is a no-op.
     * If another hint is currently showing, this one is queued by priority.
     */
    fun showHint(hintId: String, message: String, anchorView: View) {
        if (prefs.isHintShown(hintId)) return
        if (hintsShownThisSession >= MAX_HINTS_PER_SESSION && currentPopup == null) return

        val priority = HintPreferences.PRIORITY_ORDER.indexOf(hintId).let {
            if (it < 0) Int.MAX_VALUE else it
        }

        if (currentPopup != null) {
            // Queue it (sorted by priority on dequeue)
            queue.add(PendingHint(hintId, message, anchorView, priority))
            return
        }

        if (hintsShownThisSession >= MAX_HINTS_PER_SESSION) return

        // Show with delay
        anchorView.postDelayed({
            if (!prefs.isHintShown(hintId) && anchorView.isAttachedToWindow) {
                displayTooltip(hintId, message, anchorView)
            }
        }, SHOW_DELAY_MS)
    }

    private fun displayTooltip(hintId: String, message: String, anchorView: View) {
        val inflater = LayoutInflater.from(rootView.context)
        val tooltipView = TextView(rootView.context).apply {
            text = message
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 13f
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setBackgroundResource(R.drawable.hint_tooltip_bg)
            elevation = dp(4).toFloat()
        }

        val popup = PopupWindow(
            tooltipView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            false // Not focusable — we handle dismiss ourselves
        )
        popup.isOutsideTouchable = true
        popup.setOnDismissListener {
            currentPopup = null
            showNextQueued()
        }

        // Position: below the anchor, centered horizontally
        val location = IntArray(2)
        anchorView.getLocationInWindow(location)
        val anchorCenterX = location[0] + anchorView.width / 2

        // Measure tooltip
        tooltipView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val tooltipWidth = tooltipView.measuredWidth
        val xOffset = anchorCenterX - tooltipWidth / 2

        popup.showAtLocation(
            rootView,
            Gravity.NO_GRAVITY,
            xOffset.coerceAtLeast(dp(8)),
            location[1] + anchorView.height + dp(4)
        )

        // Fade in
        tooltipView.alpha = 0f
        tooltipView.animate().alpha(1f).setDuration(200).start()

        currentPopup = popup
        prefs.markHintShown(hintId)
        hintsShownThisSession++

        // Dismiss on any tap on root
        rootView.setOnClickListener {
            dismiss()
            rootView.setOnClickListener(null)
        }
    }

    private fun showNextQueued() {
        if (queue.isEmpty() || hintsShownThisSession >= MAX_HINTS_PER_SESSION) return

        // Pick highest priority (lowest index)
        val next = queue.minByOrNull { it.priority } ?: return
        queue.remove(next)

        next.anchorView.postDelayed({
            if (!prefs.isHintShown(next.hintId) && next.anchorView.isAttachedToWindow) {
                displayTooltip(next.hintId, next.message, next.anchorView)
            }
        }, NEXT_HINT_DELAY_MS)
    }

    fun dismiss() {
        currentPopup?.dismiss()
    }

    private fun dp(value: Int): Int {
        return (value * rootView.resources.displayMetrics.density).toInt()
    }
}
```

**Key things to notice:**
- `showHint()` is the only public method for triggering hints — call it from anywhere in MainActivity
- If a hint was already shown (persisted), it's silently ignored
- If another hint is visible, the new one is queued by priority
- Max 2 hints per session — after that, no more until app restart
- `PopupWindow` is used for positioning (anchored to specific views)
- Tapping anywhere dismisses the current hint and shows the next queued one

### Step 6: Build and verify

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

No visible changes yet — the system is built but no hints are triggered. That happens in Task 03.

## Files Changed

| File | What changed |
|------|-------------|
| `app/src/main/res/values/strings.xml` | Added 9 hint strings |
| `app/src/main/res/values/colors.xml` | Added tooltip background color |
| `app/src/main/res/drawable/hint_tooltip_bg.xml` | **New file** — dark rounded rectangle |
| `app/src/main/java/.../HintPreferences.kt` | **New file** — SharedPreferences wrapper with hint IDs and priority |
| `app/src/main/java/.../HintManager.kt` | **New file** — tooltip display, queue, session limit |

## Acceptance Criteria

- [ ] `HintPreferences` correctly stores and retrieves hint flags
- [ ] `HintManager` shows a tooltip anchored to a view
- [ ] Only one tooltip visible at a time
- [ ] Max 2 tooltips per session
- [ ] Queued hints show after current one is dismissed
- [ ] Build succeeds, all tests pass
