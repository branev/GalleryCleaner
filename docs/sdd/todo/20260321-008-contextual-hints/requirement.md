# SDD-20260321-008: Contextual Hints & Help

## Summary

Add contextual tooltips that appear once at the right moment to teach users key features, plus a `?` help button in the header that opens a tips bottom sheet with a "Reset tips" option.

---

## Changes Required

### 1. Contextual Hint Tooltips

One-time tooltips triggered by specific user actions. Each hint is shown once, then permanently dismissed (tracked via SharedPreferences).

#### Hints to implement:

| ID | Trigger | Message | Anchor |
|----|---------|---------|--------|
| `hint_filters` | First time gallery loads with items | "Filter by source, date range, or type" | Filters button |
| `hint_long_press` | First tap on an image in Normal mode | "Long-press to select items for deletion" | The tapped image |
| `hint_drag_select` | First time selection mode is entered | "Swipe horizontally across items to select more" | RecyclerView area |
| `hint_pinch_zoom` | First time gallery loads with items (after hint_filters) | "Pinch to zoom the grid in or out" | RecyclerView center |
| `hint_fast_scroll` | First scroll in gallery | "Drag the right edge to scroll quickly" | Fast scroll thumb area |
| `hint_trash_undo` | First successful delete | "Items go to trash for 30 days — you can undo" | Snackbar area |
| `hint_continue_fab` | First time Continue FAB appears | "Tap to jump to where you left off" | Continue FAB |
| `hint_progress_bar` | First time progress bar appears | "This bar shows how much you've reviewed" | Progress bar |

#### Tooltip appearance:
- **Style**: Material `Tooltip` or a custom small bubble (rounded rectangle, dark background `#2C2C2E`, white text, 13sp)
- **Arrow**: pointing toward the anchor element
- **Duration**: visible until user taps anywhere (no auto-dismiss timer)
- **Animation**: fade in (200ms)
- **Position**: above or below the anchor, whichever has more space

#### Timing:
- Show with a 500ms delay after the trigger event (don't interrupt the action itself)
- Don't show hints during selection mode (except `hint_drag_select`)

#### Queuing & Priority:
- **One hint visible at a time** — if multiple hints trigger simultaneously, they queue
- **Priority order** (highest first):
  1. `hint_long_press` — core interaction, must learn first
  2. `hint_filters` — important for finding content
  3. `hint_drag_select` — only relevant once user knows long-press
  4. `hint_pinch_zoom` — nice-to-know
  5. `hint_fast_scroll` — nice-to-know
  6. `hint_continue_fab` — contextual, shows when FAB appears
  7. `hint_progress_bar` — contextual, shows when bar appears
  8. `hint_trash_undo` — contextual, shows after first delete
- **Max 2 hints per session** — after showing 2 hints, remaining queued hints are saved for the next app open. Prevents tooltip fatigue
- **Dismiss → next** — when user taps to dismiss a hint, the next queued hint shows after 500ms delay
- **Session tracking** — use an in-memory counter (not persisted) that resets each time the app starts

### 2. Hint Preferences

A simple SharedPreferences wrapper to track which hints have been shown.

```kotlin
class HintPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("hints", Context.MODE_PRIVATE)

    fun isHintShown(hintId: String): Boolean = prefs.getBoolean(hintId, false)

    fun markHintShown(hintId: String) {
        prefs.edit().putBoolean(hintId, true).apply()
    }

    fun resetAllHints() {
        prefs.edit().clear().apply()
    }
}
```

### 3. Help Button in Header

A small `?` icon button in the title row of the header (between the title and the Filters button).

#### Specification:
- **Icon**: Material Symbols `help_outline` (circled "?" — the standard Material Design help icon, widely recognized across Android/iOS/web)
- **Size**: 40dp touch target, 24dp icon
- **Color**: same as Filters button text (`@color/badge_unviewed_text`)
- **Position**: to the left of the Filters button, with 8dp gap
- **Action**: opens a Help bottom sheet

### 4. Help Bottom Sheet

A simple bottom sheet listing the app's key features/tips with a "Reset tips" button at the bottom.

#### Content:

```
              Tips & Shortcuts
              ─────────────────

  touch_app       Long-press an image to start selecting

  swipe           Swipe horizontally across items to select multiple

  pinch_zoom_in   Pinch to zoom the grid in or out (2–5 columns)

  filter_list     Use Filters to narrow by source, date, or type

  scroll          Drag the right edge to scroll quickly through your library

  arrow_downward  Tap Continue to jump to unreviewed items

  delete          Deleted items stay in trash for 30 days

  undo            Tap Undo after deleting to restore items


              [ Reset Tips ]
```

> **Note:** Icon names above are Material Symbols identifiers. Use vector drawables from Material Symbols (not emoji).

#### Specification:
- **Style**: standard Material bottom sheet with drag handle
- **List**: each tip is an icon (24dp, teal) + text (15sp) row, 16dp vertical padding between rows
- **Reset button**: tonal pill button at the bottom (same style as Reset Filters in empty state)
- **Reset action**: calls `HintPreferences.resetAllHints()`, shows a snackbar "Tips will appear again as you use the app", dismisses the sheet

---

## Files Summary

| File | Change Type |
|------|------------|
| `HintPreferences.kt` | **New file** — SharedPreferences wrapper for hint flags |
| `HelpBottomSheetFragment.kt` | **New file** — bottom sheet with tips list + reset |
| `bottom_sheet_help.xml` | **New file** — layout for help bottom sheet |
| `activity_main.xml` | Minor — add `?` help button in header row |
| `MainActivity.kt` | Moderate — hint trigger logic, help button click handler |
| `strings.xml` | Minor — hint messages, help sheet text |
| `drawable/ic_help_outline.xml` | **New file** — help icon |

## Acceptance Criteria

- [ ] Each contextual hint appears once at the right moment
- [ ] Hints are never shown again after dismissal
- [ ] `?` button in header opens help bottom sheet
- [ ] Help sheet lists key tips with icons
- [ ] "Reset Tips" re-enables all contextual hints
- [ ] Snackbar confirms tips were reset
- [ ] Hints don't interfere with normal app usage
- [ ] Build succeeds, all tests pass

## Out of Scope

- Coach marks / multi-step walkthrough
- Onboarding screens on first launch
- Video tutorials
- Hint analytics / tracking which hints users see
- Localized hint text (English only for now)

## Dependencies

All referenced features are now implemented:
- SDD-002 (drag-to-select) — COMPLETE
- SDD-004 (fast scroller) — COMPLETE
- SDD-006 (pinch-to-zoom) — COMPLETE
- SDD-007 (review progress bar) — COMPLETE
