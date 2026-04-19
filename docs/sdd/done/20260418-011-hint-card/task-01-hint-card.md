# Task 01: Hint Card Restyle

**Parent:** SDD-20260418-011 — Hint Card

## What You're Changing

The bottom hint card (the tooltip that says things like "Long-press to
select items for deletion") gets a visual refresh and a content split.
The card stays exactly where it is and dismisses the same way ("Got it"
button, slide-down). What changes:

- Single-line message → **two stacked lines**: a short bold title that
  starts with `Tip · ` + a dimmer detail line (70% white) below.
- Bare 20dp lightbulb → **lightbulb inside a 28dp 12%-white circle**.
- Card corner 16dp → 14dp, padding 16/8/12/12 → uniform 12dp.
- **Bug fix:** the `Got it` button currently uses `textColor
  @color/fast_scroll_tooltip_bg` which remaps to ink — the same color
  as the card background, so the text is almost invisible. Switch
  it to `@color/accent`.
- Every hint string is split into a `*_title` and `*_detail` pair.

## Before vs After

| Thing | Before | After |
|---|---|---|
| Message | 1 TextView, 14sp white | 2 TextViews: bold title 14sp white + detail 13sp #B3FFFFFF |
| Icon | 20dp lightbulb | 28dp oval 12%-white fill + 16dp lightbulb inside |
| Card corner | 16dp | 14dp |
| Card padding | 16/8/12/12 | 12dp uniform |
| `Got it` color | `fast_scroll_tooltip_bg` (ink, invisible on ink bg) | `accent` |
| Strings | 8 × single-string `hint_*` | 16 × `hint_*_title` + `hint_*_detail` |

## Prerequisites

- SDD-002 (Design Tokens) merged. `accent`, `ink`, `hint_tooltip_bg`
  tokens present.
- `./gradlew --stop` before you start (Windows file-lock prevention).
- Files to understand first:
  - [activity_main.xml:434-485](../../../../app/src/main/res/layout/activity_main.xml#L434-L485) — current card layout
  - [HintManager.kt](../../../../app/src/main/java/com/example/gallerycleaner/HintManager.kt) — dispatch + animation
  - [strings.xml:102-111](../../../../app/src/main/res/values/strings.xml#L102-L111) — current hint strings
  - `MainActivity.kt` — 8 call sites, find with
    `grep -n "hintManager.showHint" app/src/main/java/com/example/gallerycleaner/MainActivity.kt`

## Step-by-Step Instructions

### Step 1 — Drawable for the icon backdrop

Create `res/drawable/hint_icon_bg.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <solid android:color="#1FFFFFFF" />
</shape>
```

`#1FFFFFFF` = 12% white. (`1F` ≈ 31/255 ≈ 12%.)

### Step 2 — Rebuild the hint-card inner layout

In `res/layout/activity_main.xml`, replace the **inner `LinearLayout`**
of `@+id/hintCard` (the block currently at lines 448–483) with the
structure below. Keep the outer `MaterialCardView` and its constraints,
but change `app:cardCornerRadius="16dp"` → `"14dp"`.

```xml
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="horizontal"
    android:gravity="center_vertical"
    android:padding="12dp">

    <!-- 28dp circular icon container -->
    <FrameLayout
        android:layout_width="28dp"
        android:layout_height="28dp"
        android:layout_marginEnd="12dp"
        android:background="@drawable/hint_icon_bg">

        <ImageView
            android:layout_width="16dp"
            android:layout_height="16dp"
            android:layout_gravity="center"
            android:src="@drawable/ic_lightbulb"
            app:tint="@android:color/white"
            android:importantForAccessibility="no" />
    </FrameLayout>

    <!-- Two-line text block -->
    <LinearLayout
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:orientation="vertical">

        <TextView
            android:id="@+id/hintTitle"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textColor="@android:color/white"
            android:textSize="14sp"
            android:textStyle="bold"
            android:maxLines="1"
            android:ellipsize="end"
            tools:text="Tip · swipe to select" />

        <TextView
            android:id="@+id/hintDetail"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="2dp"
            android:textColor="#B3FFFFFF"
            android:textSize="13sp"
            android:maxLines="2"
            tools:text="Long-press one, then drag across others." />
    </LinearLayout>

    <com.google.android.material.button.MaterialButton
        android:id="@+id/btnGotIt"
        style="@style/Widget.Material3.Button.TextButton"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/got_it"
        android:textColor="@color/accent"
        android:textStyle="bold"
        android:textSize="13sp" />
</LinearLayout>
```

Make sure `tools` xmlns is on the file's root element; if not, add
`xmlns:tools="http://schemas.android.com/tools"`.

### Step 3 — Swap the hint strings

Open `res/values/strings.xml`. Find the block starting at
`<!-- Contextual hints (SDD-20260321-008) -->` (around line 102). Delete
the 8 single-line `hint_*` entries below it and replace with 16 new
entries:

```xml
<!-- Contextual hints (SDD-20260418-011) — title + detail pairs -->
<string name="hint_long_press_title">Tip · long-press to select</string>
<string name="hint_long_press_detail">Hold any tile to enter selection mode.</string>

<string name="hint_filters_title">Tip · filter your library</string>
<string name="hint_filters_detail">Narrow by source, date, or type.</string>

<string name="hint_drag_select_title">Tip · swipe to select</string>
<string name="hint_drag_select_detail">Long-press one, then drag across others.</string>

<string name="hint_pinch_zoom_title">Tip · pinch to zoom</string>
<string name="hint_pinch_zoom_detail">Spread or pinch to change the column count.</string>

<string name="hint_fast_scroll_title">Tip · fly through dates</string>
<string name="hint_fast_scroll_detail">Drag the right edge to scroll quickly.</string>

<string name="hint_continue_fab_title">Tip · resume review</string>
<string name="hint_continue_fab_detail">Tap Continue to jump back to where you left off.</string>

<string name="hint_progress_bar_title">Tip · track your progress</string>
<string name="hint_progress_bar_detail">The thin bar at the top shows how much you\'ve reviewed.</string>

<string name="hint_trash_undo_title">Tip · 30-day trash</string>
<string name="hint_trash_undo_detail">Deleted items sit in system trash for 30 days — you can restore them.</string>
```

Leave `hint_got_it` alone (still used by `btnGotIt`).

### Step 4 — Update `HintManager.kt`

Change the signature to take title + detail, and the internal
data class to carry both. Full rewrite of the header (keep the
init block + dismiss logic below as-is):

```kotlin
class HintManager(
    private val prefs: HintPreferences,
    private val hintCard: MaterialCardView,
    private val hintTitle: TextView,
    private val hintDetail: TextView,
    btnGotIt: View
) {
    // ...(unchanged fields)

    private data class PendingHint(
        val hintId: String,
        val title: String,
        val detail: String,
        val priority: Int
    )

    // ...(unchanged init)

    fun showHint(hintId: String, title: String, detail: String) {
        if (prefs.isHintShown(hintId)) return
        if (hintsShownThisSession >= MAX_HINTS_PER_SESSION) return

        val priority = HintPreferences.PRIORITY_ORDER.indexOf(hintId).let {
            if (it < 0) Int.MAX_VALUE else it
        }

        if (isShowing || isPending) {
            if (queue.none { it.hintId == hintId }) {
                queue.add(PendingHint(hintId, title, detail, priority))
            }
            return
        }

        isPending = true
        val runnable = Runnable {
            isPending = false
            pendingRunnable = null
            if (!prefs.isHintShown(hintId)) {
                displayCard(hintId, title, detail)
            }
        }
        pendingRunnable = runnable
        hintCard.postDelayed(runnable, SHOW_DELAY_MS)
    }

    private fun displayCard(hintId: String, title: String, detail: String) {
        hintTitle.text = title
        hintDetail.text = detail
        hintCard.visibility = View.VISIBLE
        // ...(rest unchanged: slide-up animation, prefs.markHintShown, counter bump)
    }

    private fun showNextQueued() {
        if (queue.isEmpty() || hintsShownThisSession >= MAX_HINTS_PER_SESSION) return

        val next = queue.minByOrNull { it.priority } ?: return
        queue.remove(next)

        isPending = true
        val runnable = Runnable {
            isPending = false
            pendingRunnable = null
            if (!prefs.isHintShown(next.hintId)) {
                displayCard(next.hintId, next.title, next.detail)
            }
        }
        pendingRunnable = runnable
        hintCard.postDelayed(runnable, NEXT_HINT_DELAY_MS)
    }
}
```

### Step 5 — Update the `HintManager` construction in `MainActivity`

Find where `HintManager(...)` is instantiated (around line 185). Current
construction passes `binding.hintText`; change to pass `binding.hintTitle`
and `binding.hintDetail`:

```kotlin
hintManager = HintManager(
    prefs = hintPreferences,
    hintCard = binding.hintCard,
    hintTitle = binding.hintTitle,
    hintDetail = binding.hintDetail,
    btnGotIt = binding.btnGotIt,
)
```

### Step 6 — Update all 8 `showHint` call sites

Find them:

```bash
grep -n "hintManager.showHint" app/src/main/java/com/example/gallerycleaner/MainActivity.kt
```

For each call, split the single `getString(R.string.hint_X)` arg into
`getString(R.string.hint_X_title), getString(R.string.hint_X_detail)`.
Example:

```kotlin
// Before
hintManager.showHint(
    HintPreferences.HINT_TRASH_UNDO,
    getString(R.string.hint_trash_undo)
)

// After
hintManager.showHint(
    HintPreferences.HINT_TRASH_UNDO,
    getString(R.string.hint_trash_undo_title),
    getString(R.string.hint_trash_undo_detail),
)
```

Apply the same transform to all 8: `HINT_FAST_SCROLL`, `HINT_FILTERS`,
`HINT_PINCH_ZOOM`, `HINT_LONG_PRESS`, `HINT_DRAG_SELECT`,
`HINT_CONTINUE_FAB`, `HINT_PROGRESS_BAR`, `HINT_TRASH_UNDO`.

### Step 7 — Build, test, lint, smoke

```bash
./gradlew clean assembleDebug testDebugUnitTest lint
```

All green. If lint flags an unused string (any `hint_X` without `_title`
/ `_detail` you missed deleting), delete it. Re-run.

Install on your phone:

```bash
./gradlew installDebug
```

Smoke-test:

1. Fresh install (or clear app storage) so no hints are marked shown.
2. Open the app — `HINT_LONG_PRESS` should appear within ~1s. Verify:
   - Bold title starts with `Tip · `
   - Detail below is dimmer (not pure white)
   - Circular soft-white icon with the lightbulb
   - `Got it` text is clearly visible (accent, not invisible ink)
3. Tap `Got it` — card slides down, next hint (`HINT_FILTERS`?) appears
   after ~1s.
4. Dismiss 3 hints — 4th should not show (session cap).
5. Force-close + reopen app — hints already shown should not re-appear.

## When you're done

1. Flip every `[ ]` to `[x]` on the requirement.md acceptance list.
2. Move `docs/sdd/todo/20260418-011-hint-card/` →
   `docs/sdd/done/20260418-011-hint-card/`.
3. Update `docs/sdd/STATUS.md` — row moves from Pending to Completed,
   Tasks 1/1, short summary:
   `Two-line title+detail, 28dp lightbulb-in-circle, accent Got it, 16 new hint strings`.
4. **Atomic commits:**
   - Implementation: layout + drawable + strings + HintManager + 8 call
     sites.
   - Doc-done: acceptance ticks + folder move + STATUS.md update.
