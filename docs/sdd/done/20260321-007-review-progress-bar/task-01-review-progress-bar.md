# Task 01: Review Progress Bar

**Parent:** SDD-20260321-007 — Review Progress Bar

## What You're Changing

There's no visual indicator of how much of the gallery the user has reviewed. You will add a thin progress bar below the header that fills as the user scrolls through items. It also updates instantly when items are deleted (progress jumps up) or restored via undo (progress drops back).

## Before vs After

**Before:**
- Viewed items are dimmed (opacity 0.6) but there's no overall progress indicator
- No sense of "how far through the library am I?"

**After:**
```
Gallery Cleaner                    [Filters]
[Photos] [Videos]
[============================--------]  ← thin teal bar (75% reviewed)
[grid items...]
```

## Step-by-Step Instructions

### Step 1: Add the progress bar to activity_main.xml

Open `app/src/main/res/layout/activity_main.xml`.

Find the `filterLoadingIndicator` (around line 118). Add the review progress bar **right after it**, before the RecyclerView:

```xml
<!-- Review Progress Bar -->
<com.google.android.material.progressindicator.LinearProgressIndicator
    android:id="@+id/reviewProgressBar"
    android:layout_width="0dp"
    android:layout_height="wrap_content"
    android:visibility="gone"
    app:indicatorColor="@color/fast_scroll_tooltip_bg"
    app:trackColor="#E0E0E0"
    app:trackThickness="3dp"
    app:trackCornerRadius="2dp"
    app:layout_constraintTop_toBottomOf="@id/filterLoadingIndicator"
    app:layout_constraintStart_toStartOf="parent"
    app:layout_constraintEnd_toEndOf="parent" />
```

Then update the RecyclerView's top constraint to chain below the progress bar instead of the filter loading indicator:

Change:
```xml
app:layout_constraintTop_toBottomOf="@id/filterLoadingIndicator"
```

To:
```xml
app:layout_constraintTop_toBottomOf="@id/reviewProgressBar"
```

Also update the fast scroll track's top constraint the same way:

Change its `app:layout_constraintTop_toBottomOf="@id/filterLoadingIndicator"` to:
```xml
app:layout_constraintTop_toBottomOf="@id/reviewProgressBar"
```

And the fast scroll thumb's top constraint too:
```xml
app:layout_constraintTop_toBottomOf="@id/reviewProgressBar"
```

> **Why reuse `fast_scroll_tooltip_bg` color?** It's the same teal `#008080` we want for the progress bar. No need for a new color.

### Step 2: Update the progress bar in observeViewedItems()

Open `app/src/main/java/com/example/gallerycleaner/MainActivity.kt`.

Find `observeViewedItems()`. Inside the `.collect` block, after the FAB visibility logic, add the progress bar update:

```kotlin
// Update review progress bar
val totalItems = items.size
val viewedCount = if (totalItems > 0) {
    items.count { it.uri in viewedItems }
} else 0

if (totalItems > 0 && viewedCount > 0 &&
    (uiState is GalleryUiState.Normal || uiState is GalleryUiState.Selection)) {
    binding.reviewProgressBar.visibility = View.VISIBLE
    binding.reviewProgressBar.max = totalItems
    binding.reviewProgressBar.setProgressCompat(viewedCount, true)
} else {
    binding.reviewProgressBar.visibility = View.GONE
}
```

> **Note:** `setProgressCompat(value, true)` animates the progress change smoothly. The `true` parameter enables animation.

> **Why check for both Normal and Selection?** The user might be in selection mode while reviewing. The progress bar should stay visible.

### Step 3: Hide the progress bar in non-grid states

In `renderState()`, make sure the progress bar is hidden for Loading, Empty, NoFiltersSelected, and NoMatchingItems states.

Find each of these state blocks and add:

```kotlin
binding.reviewProgressBar.visibility = View.GONE
```

Add this line in the following blocks:
- `is GalleryUiState.Loading`
- `is GalleryUiState.Empty`
- `is GalleryUiState.NoFiltersSelected`
- `is GalleryUiState.NoMatchingItems`

You do NOT need to add it to `Normal` or `Selection` — the `observeViewedItems` collector handles those.

### Step 4: Build and verify

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

### Step 5: Test on device

1. **Initial state**: Open app — progress bar should be hidden (0% viewed)
2. **Scroll down**: Scroll through some items — progress bar appears and fills
3. **Scroll all the way**: Scroll to the end — bar should be near 100%
4. **Delete items**: Select some items, delete them — progress bar should jump up (fewer total items = higher %)
5. **Undo delete**: Tap Undo — progress bar drops back to previous level
6. **Change filters**: Change source filter — progress bar resets (different item set, viewed set may differ)
7. **Empty state**: Deselect all sources — progress bar hidden
8. **Selection mode**: Enter selection mode — progress bar stays visible
9. **Subtle**: The bar should feel unobtrusive — 3dp thin, no text

## Files Changed

| File | What changed |
|------|-------------|
| `app/src/main/res/layout/activity_main.xml` | Added `reviewProgressBar`, updated RecyclerView + fast scroll constraints |
| `app/src/main/java/.../MainActivity.kt` | Progress calculation in `observeViewedItems()`, hidden in non-grid states |

## Status: COMPLETE

## Acceptance Criteria

- [x] A thin teal progress bar appears below the header when items have been viewed
- [x] Progress updates as the user scrolls through items
- [x] Progress jumps up immediately after deletion (fewer total items)
- [x] Progress drops back after undo (items restored)
- [x] Progress bar is hidden when 0 items viewed
- [x] Progress bar is hidden in Loading, Empty, NoFiltersSelected, NoMatchingItems states
- [x] Progress bar stays visible in both Normal and Selection modes
- [x] The bar is subtle (3dp, no text)
- [x] Build succeeds, all tests pass
