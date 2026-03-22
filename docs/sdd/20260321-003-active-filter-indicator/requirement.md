# SDD-20260321-003: Active Filter Indicator

## Summary

Add a visual indicator on the "Filters" button when any filters differ from their defaults, so users know filters are active and may be hiding items.

---

## Changes Required

### 1. Filter State Detection

Determine whether current filters differ from defaults:
- **Default sources**: all `SourceType` values selected
- **Default date range**: `DateRangePreset.LAST_30_DAYS`
- **Default sort**: `SortOption.DATE_DESC`
- **Default media types**: both Photos and Videos checked (handled separately by chips, but relevant for the indicator)

If ANY of these differ from default → filters are "active".

#### Add to GalleryViewModel:
```kotlin
val hasActiveFilters: StateFlow<Boolean>
```
A derived flow that combines all filter states and emits `true` when any differ from defaults.

### 2. Visual Indicator on Filters Button

When filters are active, modify the Filters button to show a small dot badge.

#### Specification:
- **Dot**: 8dp circle, positioned at top-right of the Filters button, overlapping the edge
- **Color**: dark teal (`#008080` or reuse existing teal)
- **Animation**: none (just appears/disappears)
- **Alternative**: instead of a dot, change the button background to a slightly more saturated teal when active, and revert to the default light teal when inactive

#### Implementation options:
1. **BadgeDrawable** from Material Components — attach to the button via `BadgeUtils.attachBadgeDrawable()`
2. **Wrap in FrameLayout** with a small View positioned at top-right
3. **Change button styling** — use a bolder background tint when filters are active (simplest, no extra views)

**Recommended: Option 3** — change `backgroundTint` between default (`@color/badge_unviewed_bg`) and active (a slightly darker teal) based on `hasActiveFilters`. Simple, no layout changes.

### 3. Observe and Update

In `MainActivity.kt`, collect `hasActiveFilters` and update the button styling accordingly in the UI state rendering.

---

## Files Summary

| File | Change Type |
|------|------------|
| `GalleryViewModel.kt` | Minor — add `hasActiveFilters` derived StateFlow |
| `MainActivity.kt` | Minor — observe and update Filters button styling |
| `colors.xml` | Minor — add active filter button color (if using tint approach) |

## Acceptance Criteria

- [ ] Filters button visually changes when any filter is non-default
- [ ] Filters button reverts to normal when all filters are at defaults
- [ ] Resetting filters (via Reset in bottom sheet or Reset Filters button) clears the indicator
- [ ] Build succeeds, all tests pass

## Out of Scope

- Showing which specific filters are active on the button itself
- Filter count badge (e.g., "3 active")
