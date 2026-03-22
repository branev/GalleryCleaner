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
| `hint_drag_select` | First time selection mode is entered (after SDD-002) | "Drag across items to select more" | RecyclerView area |
| `hint_trash_undo` | First successful delete | "Items go to trash for 30 days — you can undo" | Snackbar area |
| `hint_continue_fab` | First time Continue FAB appears | "Tap to jump to where you left off" | Continue FAB |

#### Tooltip appearance:
- **Style**: Material `Tooltip` or a custom small bubble (rounded rectangle, dark background `#2C2C2E`, white text, 13sp)
- **Arrow**: pointing toward the anchor element
- **Duration**: visible until user taps anywhere (no auto-dismiss timer)
- **Animation**: fade in (200ms)
- **Position**: above or below the anchor, whichever has more space

#### Timing:
- Show with a 500ms delay after the trigger event (don't interrupt the action itself)
- Only one hint visible at a time — queue if multiple triggers happen simultaneously
- Don't show hints during selection mode (except `hint_drag_select`)

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
- **Icon**: Material `ic_help_outline` (24dp)
- **Size**: 40dp touch target
- **Color**: same as Filters button text (`@color/badge_unviewed_text`)
- **Position**: to the left of the Filters button, with 8dp gap
- **Action**: opens a Help bottom sheet

### 4. Help Bottom Sheet

A simple bottom sheet listing the app's key features/tips with a "Reset tips" button at the bottom.

#### Content:

```
              Tips & Shortcuts
              ─────────────────

  📱  Long-press an image to start selecting

  👆  Drag across images to select multiple

  🔍  Use Filters to narrow by source or date

  ⬇️  Tap Continue to jump to unreviewed items

  🗑️  Deleted items stay in trash for 30 days


              [ Reset Tips ]
```

> **Note:** Use actual Material icons, not emoji. The emoji above is for illustration only.

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

- SDD-002 (drag-to-select) should be implemented first, as `hint_drag_select` references that feature
- Other hints can be implemented independently
