# SDD-20260321-004: Fast Scroller with Date Tooltip

## Summary

Add a draggable fast-scroll thumb on the right edge of the RecyclerView that shows a floating date label as the user drags, enabling quick navigation through large photo libraries.

---

## Changes Required

### 1. Fast Scroll Thumb

A thin scrollbar track on the right edge of the RecyclerView with a draggable thumb that appears when scrolling and can be grabbed for fast scrolling.

#### Specification:
- **Track**: thin vertical line (2dp wide), right edge, subtle gray (`#E0E0E0`)
- **Thumb**: rounded rectangle (4dp wide, 48dp tall), teal-tinted (`#80008080`)
- **Behavior**: appears after any scroll, auto-hides after 1.5 seconds of inactivity, can be grabbed and dragged at any time when visible
- **Position**: maps proportionally to scroll position in the full list

### 2. Date Tooltip Bubble

When the user drags the fast-scroll thumb, a floating tooltip appears to the left of the thumb showing the date of the items at that scroll position.

#### Specification:
- **Shape**: rounded rectangle (8dp radius), teal background (`#008080`)
- **Text**: white, 14sp, semi-bold
- **Format**: "Mar 2026", "Jan 2025" (month + year for distant dates), "Today", "Yesterday" (for recent)
- **Position**: horizontally to the left of the thumb, vertically centered on the thumb
- **Animation**: fades in when drag starts, fades out when drag ends
- **Margin**: 8dp from the thumb

### 3. Date Resolution from Scroll Position

To show the correct date in the tooltip:
- Calculate which adapter position corresponds to the current scroll thumb position
- Get the `dateAdded` field from the `MediaItem` at that position
- Format it as a human-readable date

#### Implementation:
```kotlin
// In MainActivity or a helper
fun getDateAtPosition(position: Int): String {
    val items = adapter.currentList
    if (position in items.indices) {
        val timestamp = items[position].dateAdded
        // Format as "Mar 2026" or "Today"/"Yesterday"
    }
}
```

### 4. Implementation Approach

#### Option A: RecyclerView FastScroller (built-in)
Android's `RecyclerView` has built-in fast scroll support via `fastScrollEnabled="true"` in XML, but it doesn't support a date tooltip out of the box.

#### Option B: Custom implementation
- Add a custom `View` overlaid on the RecyclerView (thumb + tooltip)
- Listen to RecyclerView scroll events to position the thumb
- Listen to touch events on the thumb to enable dragging
- On drag, compute target scroll position and call `scrollToPosition()`

#### Option C: Library
Libraries like `RecyclerView-FastScroller` or `FastScrollRecyclerView` provide fast scroll with section indicators. Evaluate for compatibility.

**Recommended: Option B or C** — the built-in fast scroller is too limited for the date tooltip requirement.

---

## Files Summary

| File | Change Type |
|------|------------|
| `activity_main.xml` | Moderate — add fast scroll thumb + tooltip views overlaid on RecyclerView |
| `MainActivity.kt` | Moderate — scroll event handling, thumb drag logic, date formatting |
| New drawable(s) | Minor — thumb and tooltip backgrounds |
| `colors.xml` | Minor — fast scroller colors if needed |

## Acceptance Criteria

- [ ] A scroll thumb appears on the right edge when scrolling
- [ ] The thumb can be grabbed and dragged to fast-scroll through the list
- [ ] A date tooltip appears while dragging showing the date at the current position
- [ ] The thumb auto-hides after inactivity
- [ ] Dates are formatted appropriately ("Today", "Yesterday", "Mar 2026")
- [ ] Build succeeds, all tests pass

## Out of Scope

- Alphabetical section indicators
- Date section headers in the grid (separate idea)
- Scroll position persistence across app restarts
