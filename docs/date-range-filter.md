# Date Range Filter - Implementation Plan

## Overview
Date range filter with quick presets plus custom date pickers that works seamlessly with existing source and media type filters. Defaults to "Last 30 days" on first launch for better performance.

## UI Design
```
┌─────────────────────────────────────────────────────┐
│ [Photos] [Videos]                                   │  ← Media type chips
├─────────────────────────────────────────────────────┤
│ [7d] [30d] [3mo] [Year] [All] [Custom ▼]           │  ← Date range chips
├─────────────────────────────────────────────────────┤
│ [All (523)] [WhatsApp (120)] [Camera (89)] ...      │  ← Source chips
└─────────────────────────────────────────────────────┘
```

When "Custom" is tapped → Show Material DateRangePicker dialog.

## Filter Chain Order
```
All Items → Media Type Filter → Date Range Filter → Source Filter
```

**Why this order:**
1. Media type first (Photos/Videos) - broadest filter
2. Date range second - narrows down by time
3. Source filter last - so source counts show items within the date range

## Source Counts Behavior
Source counts reflect items **after** date range filtering. When user selects "Last 7 days", source chips show counts only for that week.

## Edge Cases

| Scenario | Behavior |
|----------|----------|
| No date range selected | Show all items (no date filtering) |
| Date range + no sources | Show "No filters selected" state |
| Date range + no media types | Show "No filters selected" state |
| Date range with 0 items | Show empty state with date info |
| Filter changes | Reset scroll to top, show loading indicator |
| "All time" preset | Clears date range filter entirely |

## Data Model

```kotlin
enum class DateRangePreset(val label: String, val days: Int?) {
    LAST_7_DAYS("7 days", 7),
    LAST_30_DAYS("30 days", 30),
    LAST_3_MONTHS("3 months", 90),
    THIS_YEAR("This year", -1),  // Special: calculate from Jan 1
    ALL_TIME("All time", null),  // No date filter
    CUSTOM("Custom", null)       // User-selected range
}

data class DateRange(
    val preset: DateRangePreset,
    val startTimestamp: Long? = null,  // For CUSTOM: start date (seconds)
    val endTimestamp: Long? = null     // For CUSTOM: end date (seconds)
)
```

## Files Modified

1. `MediaItem.kt` - Added DateRangePreset enum + DateRange data class
2. `FilterPreferences.kt` - Added date range persistence
3. `GalleryViewModel.kt` - Added date filtering logic in filterState combine
4. `GalleryUiState.kt` - Added selectedDateRange to UI states
5. `activity_main.xml` - Added date range chip group
6. `MainActivity.kt` - Added UI handling and date picker
7. `strings.xml` - Added string resources
