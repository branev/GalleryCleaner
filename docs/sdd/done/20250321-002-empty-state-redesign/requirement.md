# SDD-20250321-002: Empty State Screen Redesign

## Status: COMPLETE

## Summary

Redesign the empty state screens (NoFiltersSelected, NoMatchingItems, Empty) to match the new Stitch-generated design. The new design features a circular icon container, larger typography, updated colors, and a "Reset Filters" action button.

## Design Reference

- [design-reference.html](design-reference.html) — Stitch HTML/Tailwind output
- The design shows a single empty state with:
  - A `search_off` icon inside a gray circle
  - "No items found" title
  - "Try adjusting your filters or date range to see more photos" subtitle
  - A "Reset Filters" tonal action button

## Task Breakdown

| Task | File | Description |
|------|------|-------------|
| [Task 01](task-01-empty-state-redesign.md) | Empty state UI | Update layout, icon, typography, colors, and add Reset button |

---

## Changes Required

### 1. Empty State Layout Redesign

**Current:**
- 96dp icon (raw ImageView, alpha 0.6, uses `ic_filter_list`)
- Title: 16sp bold, `colorOnSurface`
- Subtitle: 14sp, `colorOnSurfaceVariant`
- No action button
- Tapping the entire empty state container opens the filter bottom sheet

**New:**
- Icon inside a **circular container**: 96x96dp gray circle (`#E5E5EA`), containing a 48dp `search_off` Material icon in gray (`#8E8E93`)
- Title: **22sp** bold, black/`colorOnSurface`
- Subtitle: **17sp**, gray (`#8E8E93`), max width ~240dp, line height 1.3
- **"Reset Filters" button**: tonal pill button below the subtitle
  - Background: teal at 10% opacity (`#1A008080`)
  - Text: teal (`#008080`), semi-bold
  - Shape: fully rounded pill
  - Padding: 12dp vertical, 32dp horizontal
- Remove the full-container click-to-open-filters behavior (the button replaces it)

### 2. Icon Change

**Current:** Uses `ic_filter_list` for all empty states.

**New:** Use `search_off` Material Symbol icon for NoMatchingItems and NoFiltersSelected states. The `Empty` state (no media found at all) can keep a generic icon or also use `search_off`.

**Asset needed:** `ic_search_off.xml` vector drawable (Material Symbols, 24dp viewport, filled or outlined).

### 3. Empty State Variations

The app has three empty states. All should use the same new layout, but with different text:

| State | Title | Subtitle | Show Reset Button |
|-------|-------|----------|-------------------|
| `Empty` (no media) | "No media found" | "Grant storage permission or add photos to your device" | No |
| `NoFiltersSelected` | "No categories selected" | "Select Photos or Videos and a category to see your media" | Yes |
| `NoMatchingItems` | "No items found" | "Try adjusting your filters or date range to see more photos" | Yes |

### 4. Reset Filters Button Behavior

When tapped, the "Reset Filters" button should:
- Reset all filters to defaults (all sources selected, both media types, default date range, default sort)
- This is the same action as the "Reset" button in the filter bottom sheet
- Call `viewModel.resetFilters()` (may need to add this method if not existing)

### 5. Colors

| Element | Color |
|---------|-------|
| Icon circle background | `#E5E5EA` |
| Icon color | `#8E8E93` |
| Title text | `colorOnSurface` (black) |
| Subtitle text | `#8E8E93` |
| Reset button background | `#1A008080` (teal at 10% opacity) |
| Reset button text | `#008080` |

---

## Files Affected

| File | Change Type |
|------|------------|
| `activity_main.xml` | Major — restructure empty state container, add circular icon bg, add Reset button |
| `MainActivity.kt` | Moderate — update empty state rendering, add Reset button click handler, remove container click |
| `GalleryViewModel.kt` | Minor — add `resetFilters()` method if not existing |
| `colors.xml` | Minor — add empty state colors |
| `strings.xml` | Minor — add/update empty state strings, add "Reset Filters" string |
| New drawable: `ic_search_off.xml` | New vector drawable for empty state icon |
| New drawable: `empty_state_icon_bg.xml` | Circle shape background for icon container |

## Out of Scope

- Loading state (spinner) — no changes
- Selection mode — no changes
- Main grid or header — handled in SDD-20250321-001
