# Task 03: Wire Up Hint Triggers

**Parent:** SDD-20260321-008 — Contextual Hints & Help

**Prerequisites:** Task 01 (HintPreferences + HintManager) and Task 02 (Help bottom sheet) must be completed first.

## What You're Building

Connect the HintManager to actual user actions so hints appear at the right moments. Each hint triggers once, is shown at most twice per session, and never repeats after dismissal.

## Step-by-Step Instructions

### Step 1: Initialize HintManager in MainActivity

Open `app/src/main/java/com/example/gallerycleaner/MainActivity.kt`.

**a)** Add fields near the top of the class:

```kotlin
private lateinit var hintPreferences: HintPreferences
private lateinit var hintManager: HintManager
```

**b)** Initialize them in `onCreate`, after `setContentView`:

```kotlin
hintPreferences = HintPreferences(this)
hintManager = HintManager(hintPreferences, binding.root)
```

### Step 2: Trigger hint_long_press

Find `handleItemClick()`. When in Normal mode (not selection), the user taps an image to view it. Before opening the viewer, check if we should show the long-press hint:

```kotlin
private fun handleItemClick(item: MediaItem) {
    val state = viewModel.uiState.value
    if (state is GalleryUiState.Selection) {
        viewModel.toggleItemSelection(item.uri)
    } else {
        // Show hint before opening viewer (if not yet shown)
        // Find the tapped view in the RecyclerView
        val layoutManager = binding.recyclerView.layoutManager as? GridLayoutManager
        val position = adapter.currentList.indexOfFirst { it.uri == item.uri }
        if (position >= 0) {
            val viewHolder = binding.recyclerView.findViewHolderForAdapterPosition(position)
            viewHolder?.itemView?.let { anchorView ->
                hintManager.showHint(
                    HintPreferences.HINT_LONG_PRESS,
                    getString(R.string.hint_long_press),
                    anchorView
                )
            }
        }
        openMediaViewer(item)
    }
}
```

> **Note:** The hint shows briefly before the viewer opens. If this feels awkward, you can skip it for this trigger and rely on the help sheet instead.

### Step 3: Trigger hint_filters

In the `renderState()` method, inside the `GalleryUiState.Normal` block, after items are submitted:

```kotlin
// Show filters hint on first load with items
hintManager.showHint(
    HintPreferences.HINT_FILTERS,
    getString(R.string.hint_filters),
    binding.btnFilters
)
```

### Step 4: Trigger hint_drag_select

In `handleItemLongClick()`, after entering selection mode:

```kotlin
hintManager.showHint(
    HintPreferences.HINT_DRAG_SELECT,
    getString(R.string.hint_drag_select),
    binding.recyclerView
)
```

### Step 5: Trigger hint_pinch_zoom

In `renderState()` inside the `GalleryUiState.Normal` block, after the filters hint (it will queue if filters hint is showing):

```kotlin
hintManager.showHint(
    HintPreferences.HINT_PINCH_ZOOM,
    getString(R.string.hint_pinch_zoom),
    binding.recyclerView
)
```

### Step 6: Trigger hint_fast_scroll

In the scroll listener inside `setupRecyclerView()`, on the first scroll:

```kotlin
override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
    super.onScrolled(recyclerView, dx, dy)
    if (dy > 0) {
        markItemsAboveViewportAsViewed(layoutManager)
        // Show fast scroll hint on first scroll
        hintManager.showHint(
            HintPreferences.HINT_FAST_SCROLL,
            getString(R.string.hint_fast_scroll),
            binding.fastScrollTrack
        )
    }
    updateContinueFabState(layoutManager)
}
```

### Step 7: Trigger hint_continue_fab

In `observeViewedItems()`, when the FAB becomes visible:

```kotlin
if (showFab) {
    binding.fabContinue.visibility = View.VISIBLE
    hintManager.showHint(
        HintPreferences.HINT_CONTINUE_FAB,
        getString(R.string.hint_continue_fab),
        binding.fabContinue
    )
} else {
    binding.fabContinue.visibility = View.GONE
}
```

### Step 8: Trigger hint_progress_bar

In `observeViewedItems()`, when the progress bar becomes visible:

```kotlin
if (totalItems > 0 && viewedCount > 0 && ...) {
    binding.reviewProgressBar.visibility = View.VISIBLE
    binding.reviewProgressBar.max = totalItems
    binding.reviewProgressBar.setProgressCompat(viewedCount, true)
    hintManager.showHint(
        HintPreferences.HINT_PROGRESS_BAR,
        getString(R.string.hint_progress_bar),
        binding.reviewProgressBar
    )
}
```

### Step 9: Trigger hint_trash_undo

In `showTrashSuccessSnackbar()`, after showing the snackbar:

```kotlin
hintManager.showHint(
    HintPreferences.HINT_TRASH_UNDO,
    getString(R.string.hint_trash_undo),
    binding.root // Anchor to root since snackbar position varies
)
```

### Step 10: Dismiss hints on state changes

Add cleanup calls where appropriate to avoid stale hints:

```kotlin
// In renderState(), at the top of the method (before the when block):
hintManager.dismiss()
```

This ensures hints from a previous state don't linger when the UI changes.

### Step 11: Build and verify

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

### Step 12: Test on device

1. **Fresh install** (or clear app data): Open app → filters hint should appear near Filters button
2. **Dismiss**: Tap anywhere → hint disappears, pinch-zoom hint may show next (if within 2-per-session limit)
3. **Reopen app**: Only un-shown hints should appear (max 2 per session)
4. **Long-press**: Tap an image → long-press hint may show (if not yet shown)
5. **Enter selection**: Long-press → drag-select hint shows
6. **Scroll**: Scroll down → fast scroll hint shows
7. **Delete**: Delete items → trash/undo hint shows
8. **Reset tips**: Open `?` → Reset Tips → reopen app → hints start over
9. **No spam**: After 2 hints in one session, no more appear until restart

## Files Changed

| File | What changed |
|------|-------------|
| `app/src/main/java/.../MainActivity.kt` | Initialize HintManager, add 8 trigger points, dismiss on state change |

## Acceptance Criteria

- [ ] Each hint triggers at the correct moment
- [ ] Max 2 hints per session
- [ ] Hints queue correctly (higher priority first)
- [ ] Dismissed hints never reappear (unless Reset Tips)
- [ ] Hints don't block user interaction
- [ ] State changes dismiss any visible hint
- [ ] Build succeeds, all tests pass
