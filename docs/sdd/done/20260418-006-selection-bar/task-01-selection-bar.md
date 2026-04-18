# Task 01: Selection Action Bar

**Parent:** SDD-20260418-006 — Selection Action Bar

## What You're Changing

The bottom selection bar today shows the count and size on a single line
(`4 selected · 789 kB`). You're splitting them into a two-line readout
(count bold on top, size in small mono underneath), adding a hairline
divider between the left-side controls and the readout, dropping the
elevation a bit, and adding a 1dp outline around the pill so it reads
cleanly against the cream background.

No behavior changes. The tile-side selection visuals (ring, check, dim)
were done in SDD-004 — leave those alone.

## Before vs After

| Element | Before | After |
|---|---|---|
| Bar shape | 28dp card, elevation 8dp, no outline | 28dp pill, elevation 4dp, 1dp `line` outline |
| Divider | — | 1dp × 20dp `line`-color line between Select-all and readout |
| Count + size | one TextView `"4 selected · 789 kB"` | two stacked TextViews — count (13sp ink bold) + size (11sp mono `ink3`) |

## Prerequisites

- SDD-004 (Grid Tile) merged. The 55% dim on non-selected tiles and the
  accent ring on selected tiles already work.
- `./gradlew --stop` before you start

## Step-by-Step Instructions

### Step 1 — Restructure the selection bar layout

Open `app/src/main/res/layout/activity_main.xml`. Find the selection
action bar (`android:id="@+id/selectionActionBar"`, around line 240).

Replace the whole `MaterialCardView` block (the card + its inner
`LinearLayout`) with this:

```xml
<com.google.android.material.card.MaterialCardView
    android:id="@+id/selectionActionBar"
    android:layout_width="0dp"
    android:layout_height="wrap_content"
    android:layout_margin="16dp"
    android:visibility="gone"
    app:cardCornerRadius="28dp"
    app:cardElevation="4dp"
    app:strokeColor="@color/line"
    app:strokeWidth="1dp"
    app:layout_constraintBottom_toBottomOf="parent"
    app:layout_constraintStart_toStartOf="parent"
    app:layout_constraintEnd_toEndOf="parent">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="56dp"
        android:gravity="center_vertical"
        android:orientation="horizontal"
        android:paddingHorizontal="4dp">

        <ImageButton
            android:id="@+id/btnExitSelection"
            android:layout_width="40dp"
            android:layout_height="40dp"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:src="@drawable/ic_close"
            android:contentDescription="@string/exit_selection"
            app:tint="?attr/colorOnSurface" />

        <com.google.android.material.button.MaterialButton
            android:id="@+id/btnSelectAll"
            style="@style/Widget.Material3.Button.TextButton"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/select_all" />

        <!-- Hairline divider separating controls from readout -->
        <View
            android:layout_width="1dp"
            android:layout_height="20dp"
            android:layout_marginHorizontal="8dp"
            android:background="@color/line" />

        <!-- Stacked count + size readout -->
        <LinearLayout
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:orientation="vertical"
            android:gravity="center_vertical"
            android:paddingHorizontal="4dp">

            <TextView
                android:id="@+id/selectionCount"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textSize="13sp"
                android:textStyle="bold"
                android:textColor="?attr/colorOnSurface"
                android:maxLines="1"
                android:ellipsize="end"
                tools:text="4 selected" />

            <TextView
                android:id="@+id/selectionSizeSmall"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="1dp"
                android:fontFamily="@font/jetbrains_mono"
                android:textSize="11sp"
                android:textFontWeight="500"
                android:textColor="@color/ink3"
                android:maxLines="1"
                tools:text="789 kB" />

        </LinearLayout>

        <com.google.android.material.button.MaterialButton
            android:id="@+id/btnDelete"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/delete"
            android:textColor="@color/md_theme_onError"
            app:icon="@drawable/ic_delete"
            app:iconTint="@color/md_theme_onError"
            app:backgroundTint="@color/md_theme_error" />

    </LinearLayout>

</com.google.android.material.card.MaterialCardView>
```

> **Key changes vs the old layout:**
> - `cardElevation`: `8dp` → `4dp`
> - Added `strokeColor="@color/line"` + `strokeWidth="1dp"`
> - `gravity="center"` → `center_vertical` on the inner `LinearLayout`
>   (so the stacked readout aligns top-to-bottom rather than centered by
>   letter baseline)
> - The single `selectionSize` TextView is gone. In its place: a vertical
>   container with `selectionCount` (top) and `selectionSizeSmall`
>   (bottom).
> - The old `tools` namespace is already declared at the root of
>   `activity_main.xml` — `tools:text="..."` just gives you nice preview
>   values in Android Studio.

### Step 2 — Wire both TextViews from `MainActivity`

Open `MainActivity.kt`. Find `renderState`, scroll to the
`is GalleryUiState.Selection ->` branch (around line 670). Locate the
block:

```kotlin
// Show selection count + size in the bottom bar
val selectedCount = state.selectedItems.size
val sizeText = Formatter.formatFileSize(this, viewModel.getSelectedItemsTotalSize())
val countText = if (state.hiddenSelectedCount > 0) {
    getString(R.string.selected_with_hidden, selectedCount, state.hiddenSelectedCount)
} else {
    getString(R.string.selected_count, selectedCount)
}
binding.selectionSize.text = getString(R.string.selection_count_size, countText, sizeText)
```

Replace it with:

```kotlin
// Show selection count on top, total size below in mono
val selectedCount = state.selectedItems.size
val totalSize = viewModel.getSelectedItemsTotalSize()
binding.selectionCount.text = if (state.hiddenSelectedCount > 0) {
    getString(R.string.selected_with_hidden, selectedCount, state.hiddenSelectedCount)
} else {
    getString(R.string.selected_count, selectedCount)
}
binding.selectionSizeSmall.visibility =
    if (totalSize > 0L) View.VISIBLE else View.GONE
binding.selectionSizeSmall.text = Formatter.formatFileSize(this, totalSize)
```

> **Why hide the size when total is 0?** Immediately after entering
> selection mode, only one tile is selected and its size may resolve
> asynchronously, producing `0 B` for a frame. Hiding it avoids the
> flash.

### Step 3 — Remove the now-unused string

Open `app/src/main/res/values/strings.xml`. Find and delete:

```xml
<string name="selection_count_size">%1$s · %2$s</string>
```

### Step 4 — Grep-verify nothing else references the deleted/renamed IDs

From the project root:

```bash
grep -r "selectionSize\b\|selection_count_size" app/
```

Expected: **no matches**. Two specific gotchas to look for:

- `binding.selectionSize` used anywhere in Kotlin (other than `renderState`
  which you just edited)
- `R.string.selection_count_size` in any other Kotlin or XML file

If any turn up, they need updating before the build will succeed.

> **Why `selectionSize\b` with a word boundary?** The new view id is
> `selectionSizeSmall`, which contains `selectionSize`. Without the
> boundary you'd get false positives on the new, valid id.

### Step 5 — Build and verify

```bash
./gradlew --stop
./gradlew clean assembleDebug testDebugUnitTest lint
```

Expected outcome: **BUILD SUCCESSFUL**, same two pre-existing
`statusBarColor` warnings as before. No new lint errors.

Likely failure modes:
- `Unresolved reference: selectionSize` → Step 2 wasn't fully updated
  (there's another `binding.selectionSize` reference somewhere).
- `resource string/selection_count_size not found` → Step 3 deleted the
  string but a Kotlin file still calls `getString(R.string.selection_count_size, ...)`.
- ViewBinding error about `selectionCount` not existing → the layout
  rewrite in Step 1 didn't land, or the file still has the old single
  `selectionSize` TextView.

### Step 6 — Visual smoke test on-device

Install. Then:

1. **Long-press any tile** — selection bar slides up. Verify:
   - The bar has visibly rounded ends (pill shape) and a faint gray
     outline against the cream background.
   - Left to right: `×`, `Select all`, a thin vertical gray line, the
     count + size stacked (`1 selected` in bold, `256 kB` in mono
     beneath), and the red `Delete` button.
2. **Tap several more tiles**. Count updates to `2 selected`,
   `3 selected`, etc. Size updates in real time.
3. **Tap Select all**. Count jumps to match the visible items; size
   updates to the library total.
4. **Tap ×**. Bar slides away, grid returns to normal (reviewed tiles
   fade again, non-selected dim lifts).
5. **Fast-scroll while in selection mode**. Bar stays put; size readout
   stays legible over whatever's behind it (the outline + 1dp stroke
   help).

## Definition of Done

- [x] All Files Changed from `requirement.md` landed
- [x] `./gradlew clean assembleDebug testDebugUnitTest lint` succeeds
- [x] No references to `selection_count_size` or old `selectionSize`
      view id anywhere in `app/`
- [x] Visual smoke test (Step 6) passes
- [x] PR opened with title
      `SDD-20260418-006 — Selection Action Bar`

## Known gotchas

- **`android:textFontWeight="500"`** requires API 28+ — we target 26+,
  so older devices will fall back to `Regular` weight of the same
  family. Acceptable; don't change it.
- **`gravity="center_vertical"`**: if you accidentally leave
  `gravity="center"` on the outer horizontal LinearLayout, the stacked
  count/size container will try to center-align vertically within a
  very tall slot and the two TextViews will jam together.
  `center_vertical` aligns each child along the horizontal midline,
  which is what you want.
- **Formatter thread**: `Formatter.formatFileSize()` is main-thread safe.
  The total-size computation `viewModel.getSelectedItemsTotalSize()`
  loops over the selected-set, which at a few thousand items is still
  well under 1ms. Don't move it off the main thread without measuring.
- **Pill corner radius of 28dp at 56dp height = full half-circle ends**.
  If you change `android:layout_height` of the inner LinearLayout to
  something other than 56dp, also update `app:cardCornerRadius` to half
  of the new height, or the pill becomes a rounded rectangle.
