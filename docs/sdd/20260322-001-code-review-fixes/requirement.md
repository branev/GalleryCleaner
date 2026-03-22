# SDD-20260322-001: Code Review Fixes & Test Coverage

## Status: COMPLETE

## Summary

Address 7 issues from senior Android developer code review and fill test coverage gaps for drag selection, size calculation, and range mapping logic.

---

## Code Review Issues Fixed

| # | Issue | Severity | Fix |
|---|-------|----------|-----|
| 1 | DragSelectTouchListener memory leak | Bug | WeakReference + detachFromRecyclerView() |
| 2 | Auto-scroll doesn't update selection | Bug | Track last touch coords, update selection in autoScroll() |
| 3 | Redundant formatFileSize | Cleanup | Removed custom, use Formatter.formatFileSize() everywhere |
| 4 | preDragSelection fragile coupling | Cleanup | Always snapshot from ViewModel |
| 5 | String concatenation for i18n | Best practice | String resource `selection_count_size` |
| 6 | Animation listener leak | Bug | Clear animations + snackbar in onDestroy() |
| 7 | Dead selectionToolbar code | Cleanup | Removed from layout and MainActivity |

## Test Coverage Added

16 new tests in `GalleryViewModelTest.kt`:

- **Drag selection logic** (7 tests): union, shrink, pre-existing preservation, no-op when not in selection mode
- **Selected items total size** (4 tests): sum, zero, missing URIs, empty items
- **Drag range to URI mapping** (5 tests): valid range, out-of-bounds, empty items, single position

See [test-coverage-assessment.md](test-coverage-assessment.md) for full gap analysis.

## Files Changed

| File | Change |
|------|--------|
| `DragSelectTouchListener.kt` | WeakReference, detach, auto-scroll selection update |
| `MainActivity.kt` | onDestroy cleanup, removed dead toolbar code, Formatter, string resource |
| `activity_main.xml` | Removed selectionToolbar |
| `strings.xml` | Added selection_count_size resource |
| `GalleryViewModelTest.kt` | 16 new tests |
