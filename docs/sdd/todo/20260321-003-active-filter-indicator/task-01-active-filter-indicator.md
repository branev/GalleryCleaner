# Task 01: Active Filter Indicator

**Parent:** SDD-20260321-003 — Active Filter Indicator

## What You're Changing

The Filters button currently looks the same whether filters are at their defaults or not. Users can have hidden items without realizing filters are active. You will change the button's appearance when any filter differs from its default, so it's visually obvious.

## Before vs After

**Before (always):**
```
[ Filters ]  ← light teal background, dark text
```

**After (filters at defaults):**
```
[ Filters ]  ← light teal background, dark text (unchanged)
```

**After (filters active / non-default):**
```
[ Filters ]  ← darker teal background, white text
```

## Step-by-Step Instructions

### Step 1: Add new colors

Open `app/src/main/res/values/colors.xml` and add:

```xml
<!-- Active filter indicator (SDD-20260321-003) -->
<color name="filter_btn_active_bg">#008080</color>
<color name="filter_btn_active_text">#FFFFFF</color>
```

### Step 2: Add hasActiveFilters StateFlow to GalleryViewModel

Open `app/src/main/java/com/example/gallerycleaner/GalleryViewModel.kt`.

Find the filter state exposures (around line 67, where `selectedSources`, `selectedMediaTypes`, etc. are declared). Add this derived flow after them:

```kotlin
// Derived: true when any filter differs from defaults
val hasActiveFilters: StateFlow<Boolean> = combine(
    selectedSources,
    selectedMediaTypes,
    selectedDateRange,
    selectedSortOption
) { sources, mediaTypes, dateRange, sortOption ->
    val allSourcesSelected = sources.size == SourceType.values().size
    val allMediaTypesSelected = mediaTypes.size == MediaType.values().size
    val defaultDateRange = dateRange.preset == DateRangePreset.LAST_30_DAYS
    val defaultSort = sortOption == SortOption.DATE_DESC

    !allSourcesSelected || !allMediaTypesSelected || !defaultDateRange || !defaultSort
}.stateIn(viewModelScope, SharingStarted.Eagerly, false)
```

**What this does:**
- Combines all 4 filter StateFlows
- Checks each against its default value
- Emits `true` if ANY filter is non-default
- `SharingStarted.Eagerly` means it starts computing immediately (same as other flows in this file)

**Why not include media types?** Actually we DO include them — if the user unchecks Photos or Videos, that's a non-default filter too.

### Step 3: Observe hasActiveFilters in MainActivity

Open `app/src/main/java/com/example/gallerycleaner/MainActivity.kt`.

Find `observeViewedItems()` (around line 285) — this is where other StateFlow observations happen. Add a new observation nearby. You can add it inside the same `lifecycleScope.launch` + `repeatOnLifecycle` block, or create a separate one.

The simplest approach: add a new `launch` inside the existing `repeatOnLifecycle` block in `observeUiState()` or `observeViewedItems()`. Or add a new method:

```kotlin
private fun observeActiveFilters() {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.hasActiveFilters.collect { isActive ->
                updateFiltersButtonAppearance(isActive)
            }
        }
    }
}

private fun updateFiltersButtonAppearance(isActive: Boolean) {
    if (isActive) {
        binding.btnFilters.setBackgroundColor(getColor(R.color.filter_btn_active_bg))
        binding.btnFilters.setTextColor(getColor(R.color.filter_btn_active_text))
    } else {
        binding.btnFilters.setBackgroundColor(getColor(R.color.badge_unviewed_bg))
        binding.btnFilters.setTextColor(getColor(R.color.badge_unviewed_text))
    }
}
```

> **Important:** `MaterialButton` uses `backgroundTint` not `backgroundColor`. Use `binding.btnFilters.backgroundTintList = ColorStateList.valueOf(getColor(...))` instead of `setBackgroundColor()`:

```kotlin
private fun updateFiltersButtonAppearance(isActive: Boolean) {
    val bgColor = if (isActive) R.color.filter_btn_active_bg else R.color.badge_unviewed_bg
    val textColor = if (isActive) R.color.filter_btn_active_text else R.color.badge_unviewed_text
    binding.btnFilters.backgroundTintList = android.content.res.ColorStateList.valueOf(getColor(bgColor))
    binding.btnFilters.setTextColor(getColor(textColor))
}
```

Then call `observeActiveFilters()` from `onCreate`, next to the other observe calls:

```kotlin
observeUiState()
observeViewedItems()
observeActiveFilters()  // Add this line
```

### Step 4: Build and verify

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

### Step 5: Test on device

1. **Default state**: Open app fresh → Filters button should be light teal (inactive)
2. **Change date range**: Open Filters → change date range to "Last 7 days" → close → button should be darker teal (active)
3. **Change source**: Deselect a source → button should be darker teal
4. **Change sort**: Change sort to "Name A-Z" → button should be darker teal
5. **Reset**: Open Filters → tap Reset → button should revert to light teal
6. **Reset Filters button** (empty state): If in NoMatchingItems, tap "Reset Filters" → button should revert to light teal
7. **Multiple changes**: Change date + source → button active. Reset only date → button still active (source still non-default). Reset source too → button inactive.

## Files Changed

| File | What changed |
|------|-------------|
| `app/src/main/res/values/colors.xml` | Added 2 active filter colors |
| `app/src/main/java/.../GalleryViewModel.kt` | Added `hasActiveFilters` derived StateFlow |
| `app/src/main/java/.../MainActivity.kt` | Added `observeActiveFilters()`, `updateFiltersButtonAppearance()` |

## Acceptance Criteria

- [ ] Filters button changes to darker teal + white text when any filter is non-default
- [ ] Filters button reverts to light teal + dark text when all filters are at defaults
- [ ] Resetting via bottom sheet Reset button clears the indicator
- [ ] Resetting via empty state Reset Filters button clears the indicator
- [ ] Indicator updates immediately when filters change (no delay)
- [ ] Build succeeds, all tests pass
