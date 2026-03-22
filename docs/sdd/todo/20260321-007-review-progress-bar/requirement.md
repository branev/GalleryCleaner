# SDD-20260321-007: Review Progress Bar

## Summary

Add a thin progress bar at the top of the grid (below the header) showing what percentage of items the user has scrolled past ("reviewed"). Provides a sense of progress when cleaning a large library.

---

## Changes Required

### 1. Progress Bar View

A thin horizontal progress indicator below the header, above the grid.

#### Specification:
- **Type**: `LinearProgressIndicator` (Material, determinate mode)
- **Height**: 3dp (thin, unobtrusive)
- **Color**: teal (`#008080`) on light gray track (`#E0E0E0`)
- **Position**: constrained below `topBar`, above `recyclerView`
- **Visibility**: visible only in Normal mode (not in Loading, Empty, Selection, etc.)

### 2. Progress Calculation

Progress = (viewed items count / total filtered items count) × 100

#### Source data:
- Viewed count: `viewModel.viewedItems.value.size`
- Total count: items from current UI state (`Normal.items.size`)

#### Update triggers:
- When `viewedItems` StateFlow emits (items marked as viewed on scroll)
- When filtered items list changes (filter change may change total)

### 3. Progress Text (Optional)

A small text label at the right end of the progress bar or overlaid:
- Format: "45%" or "128 / 284 reviewed"
- Style: 10sp, gray, subtle
- Could be shown only on tap/interaction to avoid clutter

**Recommendation:** Start without text — just the bar. Add text later if users find the bar alone unclear.

### 4. Integration with Existing Viewed Items Logic

The app already tracks viewed items via `_viewedItems` StateFlow in `GalleryViewModel` and marks items as viewed when scrolled past. The progress bar simply visualizes this existing data.

---

## Files Summary

| File | Change Type |
|------|------------|
| `activity_main.xml` | Minor — add `LinearProgressIndicator` between header and RecyclerView |
| `MainActivity.kt` | Minor — observe viewed items count, update progress |

## Acceptance Criteria

- [ ] A thin progress bar appears below the header in Normal mode
- [ ] Progress updates as the user scrolls through items
- [ ] Progress resets when filters change (new item set)
- [ ] Progress bar is hidden in Loading, Empty, Selection states
- [ ] The bar is subtle and doesn't feel cluttered
- [ ] Build succeeds, all tests pass

## Out of Scope

- Progress persistence across sessions
- "Reviewed" vs "unreviewed" label toggle
- Progress-based suggestions ("You've reviewed 80%! Ready to clean up?")
- Replacing the Continue FAB (keep both for now, evaluate later)

## Experiment Note

This is an experimental feature. After implementation, evaluate whether it adds value or visual clutter. If it feels noisy, consider:
- Making it fade out after initial scroll
- Only showing it when > 0% progress
- Removing it entirely if the dimmed-viewed-items visual is sufficient
