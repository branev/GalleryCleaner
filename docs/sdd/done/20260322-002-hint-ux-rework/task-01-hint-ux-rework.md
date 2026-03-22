# Task 01: Hint UX Rework — Bottom Card with "Got it"

**Parent:** SDD-20260322-002 — Hint UX Rework

## What You're Changing

The current hints use small dark `PopupWindow` tooltips anchored to specific views. They can appear two at once, are hard to read, and dismiss on any tap. You will replace them with a single full-width card at the bottom of the screen with a "Got it" button. The existing queue/priority/session logic stays — only the display changes.

## Before vs After

**Before:**
```
                    [small dark popup]  ← anchored to Filters button
[grid items...]
         [another dark popup]  ← anchored to RecyclerView
```

**After:**
```
[grid items...]

┌─────────────────────────────────────┐
│  💡  Long-press to select items     │
│       for deletion         [Got it] │
└─────────────────────────────────────┘
```

## Step-by-Step Instructions

### Step 1: Create the lightbulb icon

Create `app/src/main/res/drawable/ic_lightbulb.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="20dp"
    android:height="20dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="@color/fast_scroll_tooltip_bg"
        android:pathData="M9,21c0,0.55 0.45,1 1,1h4c0.55,0 1,-0.45 1,-1v-1H9v1zM12,2C8.14,2 5,5.14 5,9c0,2.38 1.19,4.47 3,5.74V17c0,0.55 0.45,1 1,1h6c0.55,0 1,-0.45 1,-1v-2.26c1.81,-1.27 3,-3.36 3,-5.74 0,-3.86 -3.14,-7 -7,-7z" />
</vector>
```

### Step 2: Add "Got it" string

Open `app/src/main/res/values/strings.xml` and add:

```xml
<string name="got_it">Got it</string>
```

### Step 3: Add the hint card to activity_main.xml

Open `app/src/main/res/layout/activity_main.xml`.

Add this **just before** the `deleteSuccessOverlay` FrameLayout (so the overlay covers it when showing):

```xml
<!-- Hint Card (bottom of screen) -->
<com.google.android.material.card.MaterialCardView
    android:id="@+id/hintCard"
    android:layout_width="0dp"
    android:layout_height="wrap_content"
    android:layout_margin="16dp"
    android:visibility="gone"
    app:cardBackgroundColor="@color/hint_tooltip_bg"
    app:cardCornerRadius="16dp"
    app:cardElevation="8dp"
    app:layout_constraintBottom_toBottomOf="parent"
    app:layout_constraintStart_toStartOf="parent"
    app:layout_constraintEnd_toEndOf="parent">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:paddingStart="16dp"
        android:paddingEnd="8dp"
        android:paddingVertical="12dp">

        <ImageView
            android:layout_width="20dp"
            android:layout_height="20dp"
            android:layout_marginEnd="12dp"
            android:src="@drawable/ic_lightbulb"
            android:importantForAccessibility="no" />

        <TextView
            android:id="@+id/hintText"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:textColor="@android:color/white"
            android:textSize="14sp"
            android:lineSpacingMultiplier="1.2" />

        <com.google.android.material.button.MaterialButton
            android:id="@+id/btnGotIt"
            style="@style/Widget.Material3.Button.TextButton"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/got_it"
            android:textColor="@color/fast_scroll_tooltip_bg"
            android:textStyle="bold"
            android:textSize="14sp" />

    </LinearLayout>

</com.google.android.material.card.MaterialCardView>
```

### Step 4: Rewrite HintManager.kt

Replace the entire contents of `app/src/main/java/com/example/gallerycleaner/HintManager.kt` with:

```kotlin
package com.example.gallerycleaner

import android.view.View
import android.widget.TextView
import com.google.android.material.card.MaterialCardView

/**
 * Manages contextual hint display using a bottom card with "Got it" button.
 * Shows one hint at a time, queues others by priority, max 2 per session.
 */
class HintManager(
    private val prefs: HintPreferences,
    private val hintCard: MaterialCardView,
    private val hintText: TextView,
    private val btnGotIt: View
) {
    private var isShowing = false
    private val queue = mutableListOf<PendingHint>()
    private var hintsShownThisSession = 0

    companion object {
        private const val MAX_HINTS_PER_SESSION = 2
        private const val SHOW_DELAY_MS = 500L
        private const val NEXT_HINT_DELAY_MS = 1000L
    }

    private data class PendingHint(
        val hintId: String,
        val message: String,
        val priority: Int
    )

    init {
        btnGotIt.setOnClickListener {
            dismissWithAnimation()
        }
    }

    /**
     * Request to show a hint. If already shown (persisted), session limit reached,
     * or another hint is visible, it queues by priority.
     */
    fun showHint(hintId: String, message: String) {
        if (prefs.isHintShown(hintId)) return
        if (hintsShownThisSession >= MAX_HINTS_PER_SESSION && !isShowing) return

        val priority = HintPreferences.PRIORITY_ORDER.indexOf(hintId).let {
            if (it < 0) Int.MAX_VALUE else it
        }

        if (isShowing) {
            if (queue.none { it.hintId == hintId }) {
                queue.add(PendingHint(hintId, message, priority))
            }
            return
        }

        if (hintsShownThisSession >= MAX_HINTS_PER_SESSION) return

        // Show with delay
        hintCard.postDelayed({
            if (!prefs.isHintShown(hintId)) {
                displayCard(hintId, message)
            }
        }, SHOW_DELAY_MS)
    }

    private fun displayCard(hintId: String, message: String) {
        hintText.text = message
        hintCard.visibility = View.VISIBLE

        // Slide up animation
        hintCard.translationY = hintCard.height.toFloat().let { if (it > 0f) it else 200f }
        hintCard.animate()
            .translationY(0f)
            .setDuration(200)
            .start()

        isShowing = true
        prefs.markHintShown(hintId)
        hintsShownThisSession++
    }

    private fun dismissWithAnimation() {
        hintCard.animate()
            .translationY(hintCard.height.toFloat() + 100f)
            .setDuration(200)
            .withEndAction {
                hintCard.visibility = View.GONE
                hintCard.translationY = 0f
                isShowing = false
                showNextQueued()
            }
            .start()
    }

    private fun showNextQueued() {
        if (queue.isEmpty() || hintsShownThisSession >= MAX_HINTS_PER_SESSION) return

        val next = queue.minByOrNull { it.priority } ?: return
        queue.remove(next)

        hintCard.postDelayed({
            if (!prefs.isHintShown(next.hintId)) {
                displayCard(next.hintId, next.message)
            }
        }, NEXT_HINT_DELAY_MS)
    }

    fun dismiss() {
        if (isShowing) {
            hintCard.visibility = View.GONE
            hintCard.translationY = 0f
            isShowing = false
        }
        queue.clear()
    }
}
```

**Key changes from old version:**
- No more `PopupWindow`, `rootView`, or `anchorView` — just card views
- `showHint()` no longer takes an `anchorView` parameter (card is always at bottom)
- `displayCard()` replaces `displayTooltip()` — sets text and slides in
- `dismissWithAnimation()` slides out, then triggers next queued hint
- `dismiss()` for instant hide (used on state changes)
- No `rootView.setOnClickListener` — "Got it" is the only dismiss method

### Step 5: Update all showHint() calls in MainActivity.kt

The `showHint()` signature changed — it no longer takes an `anchorView`. Open `app/src/main/java/com/example/gallerycleaner/MainActivity.kt`.

**a)** Update HintManager initialization. Find where `hintManager` is created and replace:

```kotlin
// Old:
hintManager = HintManager(hintPreferences, binding.root)

// New:
hintManager = HintManager(
    hintPreferences,
    binding.hintCard,
    binding.hintText,
    binding.btnGotIt
)
```

**b)** Update every `showHint()` call to remove the third parameter (anchorView). Find and replace each one:

```kotlin
// Old pattern:
hintManager.showHint(HintPreferences.HINT_FILTERS, getString(R.string.hint_filters), binding.btnFilters)

// New pattern:
hintManager.showHint(HintPreferences.HINT_FILTERS, getString(R.string.hint_filters))
```

Do this for all 8 hint triggers:
1. `HINT_LONG_PRESS` — remove `anchorView` parameter
2. `HINT_FILTERS` — remove `binding.btnFilters`
3. `HINT_DRAG_SELECT` — remove `binding.recyclerView`
4. `HINT_PINCH_ZOOM` — remove `binding.recyclerView`
5. `HINT_FAST_SCROLL` — remove `binding.fastScrollTrack`
6. `HINT_CONTINUE_FAB` — remove `binding.fabContinue`
7. `HINT_PROGRESS_BAR` — remove `binding.reviewProgressBar`
8. `HINT_TRASH_UNDO` — remove `binding.root`

Also remove the `viewHolder?.itemView?.let` wrapper around `HINT_LONG_PRESS` since we no longer need the anchor view — just call `showHint()` directly.

### Step 6: Delete old drawable

Delete `app/src/main/res/drawable/hint_tooltip_bg.xml` — it was the PopupWindow background and is no longer used. The card uses `app:cardBackgroundColor` instead.

### Step 7: Build and verify

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

### Step 8: Test on device

1. **Fresh install** (or clear app data, or tap Reset Tips in help sheet): Open app → a card slides up from the bottom with a hint + "Got it" button
2. **One at a time**: Only one card visible, never two
3. **"Got it"**: Tap "Got it" → card slides down → after 1 second, next queued hint slides up
4. **Max 2**: After dismissing 2 hints, no more appear until app restart
5. **Readable**: Card is full-width, white text on dark background, lightbulb icon, clear "Got it" button
6. **State changes**: Changing filters, entering selection mode → card dismissed instantly
7. **Delete overlay**: When delete overlay shows, it covers the hint card
8. **All triggers**: Reset tips, then test each trigger (scroll, select, delete, etc.)

## Files Changed

| File | What changed |
|------|-------------|
| `app/src/main/res/drawable/ic_lightbulb.xml` | **New file** — lightbulb icon |
| `app/src/main/res/drawable/hint_tooltip_bg.xml` | **Deleted** — old PopupWindow background |
| `app/src/main/res/values/strings.xml` | Added "Got it" string |
| `app/src/main/res/layout/activity_main.xml` | Added hint card layout at bottom |
| `app/src/main/java/.../HintManager.kt` | **Rewritten** — card-based instead of PopupWindow |
| `app/src/main/java/.../MainActivity.kt` | Updated HintManager init and all showHint() calls (removed anchorView) |

## Acceptance Criteria

- [ ] Hints appear as a full-width card at the bottom of the screen
- [ ] Only one hint card visible at a time (never two)
- [ ] "Got it" button dismisses the card (no tap-outside dismiss)
- [ ] Next queued hint appears 1 second after "Got it"
- [ ] Max 2 hints per session
- [ ] Slide-up/down animation on show/dismiss
- [ ] Card is readable and clearly a tip (lightbulb icon, "Got it" button)
- [ ] Old PopupWindow code and drawable fully removed
- [ ] Existing hint triggers (all 8) still work
- [ ] Build succeeds, all tests pass
