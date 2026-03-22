# SDD-20260321-005: Smart Filter Summary Chips

## Summary

When filters are active (non-default), show a horizontal row of dismissible chips below the header summarizing each active filter. Tapping the X on a chip resets that specific filter to its default.

---

## Changes Required

### 1. Filter Summary Chip Row

A horizontally scrollable row of small chips that appears between the header and the grid when any filters are non-default.

#### Specification:
- **Position**: below the Photos/Videos chips row, above the RecyclerView
- **Layout**: `HorizontalScrollView` containing a `ChipGroup`
- **Visibility**: `GONE` when all filters are at defaults, `VISIBLE` otherwise
- **Padding**: 16dp horizontal, 8dp vertical

### 2. Chip Types

Each non-default filter gets its own chip:

| Filter | Default | Chip text example | On dismiss |
|--------|---------|-------------------|------------|
| Date range | LAST_30_DAYS | "Last 7 days" or "Mar 1 - Mar 15" | Reset to LAST_30_DAYS |
| Sources | All selected | "WhatsApp, Camera" (list active) or "3 sources" | Select all sources |
| Sort | DATE_DESC | "Sort: Name A-Z" | Reset to DATE_DESC |

> **Note:** Media type (Photos/Videos) is already visible in the top chips row, so it does NOT get a summary chip.

### 3. Chip Styling

- **Style**: Material3 `Chip.Assist` or `Chip.Input` with close icon
- **Background**: light teal (`@color/badge_unviewed_bg`)
- **Text color**: dark (`@color/badge_unviewed_text`)
- **Close icon**: small X, same text color
- **Size**: compact (32dp height)

### 4. Dismiss Behavior

- Tapping the X on a chip resets ONLY that specific filter to its default
- The chip disappears
- If no more active filters remain, the entire chip row hides
- The grid updates immediately to reflect the reset filter

---

## Files Summary

| File | Change Type |
|------|------------|
| `activity_main.xml` | Minor — add HorizontalScrollView + ChipGroup below header |
| `MainActivity.kt` | Moderate — generate chips dynamically based on active filters, handle dismiss |
| `GalleryViewModel.kt` | Minor — add individual filter reset methods if not already present |

## Acceptance Criteria

- [ ] Chips appear when any filter is non-default
- [ ] Each chip shows a readable summary of its filter
- [ ] Tapping X on a chip resets only that filter
- [ ] Chip row hides when all filters are at defaults
- [ ] Grid updates immediately when a chip is dismissed
- [ ] Build succeeds, all tests pass

## Out of Scope

- Animated chip entrance/exit
- Reordering chips by importance
- "Clear all filters" chip (covered by Reset Filters button in empty state)
