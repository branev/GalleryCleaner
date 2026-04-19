# SDD-20260418-011: Hint Card

**Parent:** SDD-20260418-001 — Visual Redesign (umbrella)

**Depends on:** SDD-20260418-002 ✓

## Summary

Re-skin the contextual hint card that pops up at the bottom of the grid.
The layout moves from a single-line message ("Long-press to select items
for deletion") to a **two-line format**: a short bold title prefixed by
`Tip · ` and a longer supporting detail rendered in 70% white. The
lightbulb icon gets a **28dp 12%-white circular backdrop** so it reads
as a badge rather than a floating glyph.

Dismiss control swaps from a "Got it" text button to a small `×`
close icon in the top-right, per the design handoff. (Umbrella
§Open decisions #1 is resolved in the design's favor after visual
review — the `×` reads cleaner next to the two-line text block and
the card's reduced width.) The card itself loses its default
Material outline to match the solid-ink design reference.

### Visual changes

- Card container: existing `MaterialCardView` kept; corner radius 16dp
  → **14dp**, padding 16/8/12/12 → **uniform 12dp**, `strokeWidth=0dp`
  to drop the default Material outline. Elevation stays at 8dp.
  Background stays `@color/hint_tooltip_bg` (ink, already correct).
  Outer LinearLayout `gravity="top"` so the icon and close align with
  the title row, not the centerline of the two-line text block.
- **New circular icon container**: 28dp oval, fill `#1FFFFFFF`
  (12% white). Replaces the bare 20dp lightbulb.
- **Lightbulb glyph**: 16dp centered inside the circle, white tint.
- **Two-line text block** replacing the current single TextView:
  - `hintTitle` — 14sp 700 white, single-line, e.g. `Tip · swipe to select`
  - `hintDetail` — 13sp 400 `#B3FFFFFF` (70% white), up to 2 lines,
    e.g. `Long-press one, then drag across others.`
  - 2dp vertical spacing between the two.
- `btnGotIt` — **replaced** with a 32dp touch-target ImageView
  containing a 20dp `ic_close` glyph (60% white tint). Top-right
  aligned. ID is kept for backward-compat in `HintManager.init`.
  `contentDescription` reuses the existing `got_it` string.

### Behavioral changes

None. Queueing, priority order, 3-hints-per-session cap, slide-in/
slide-out animations, and SharedPreferences tracking all remain as
SDD-20260322-002 defined them.

### String changes

Every displayable hint is split into `*_title` and `*_detail`. Current
single strings (`hint_long_press`, etc.) are deleted.

| Hint ID (unchanged) | New title | New detail |
|---|---|---|
| `HINT_LONG_PRESS` | `Tip · long-press to select` | `Hold any tile to enter selection mode.` |
| `HINT_FILTERS` | `Tip · filter your library` | `Narrow by source, date, or type.` |
| `HINT_DRAG_SELECT` | `Tip · swipe to select` | `Long-press one, then drag across others.` |
| `HINT_PINCH_ZOOM` | `Tip · pinch to zoom` | `Spread or pinch to change the column count.` |
| `HINT_FAST_SCROLL` | `Tip · fly through dates` | `Drag the right edge to scroll quickly.` |
| `HINT_CONTINUE_FAB` | `Tip · resume review` | `Tap Continue to jump back to where you left off.` |
| `HINT_PROGRESS_BAR` | `Tip · track your progress` | `The thin bar at the top shows how much you've reviewed.` |
| `HINT_TRASH_UNDO` | `Tip · 30-day trash` | `Deleted items sit in system trash for 30 days — you can restore them.` |

## Why two lines instead of one

A single-line hint forces every message to read like a button label
("Long-press to select items for deletion"). Splitting lets the title
do rapid pattern-matching ("swipe to select — oh, that's a *thing*")
while the detail line teaches *how*. Same character count, better
hierarchy, and lines up visually with other cards in the redesign
(filter sheet footer, delete-success card).

## Scope

1. **`res/drawable/hint_icon_bg.xml`** — new. 28dp oval, `#1FFFFFFF` fill.
2. **`res/layout/activity_main.xml`** — rebuild the hint-card inner
   layout: icon block → 28dp `FrameLayout` with `hint_icon_bg` + 16dp
   lightbulb ImageView inside; replace `@+id/hintText` TextView with a
   vertical `LinearLayout` containing `@+id/hintTitle` and
   `@+id/hintDetail`. Adjust card corner radius + padding.
3. **`res/values/strings.xml`** — delete the 8 `hint_*` single-string
   entries, add 16 new `hint_*_title` / `hint_*_detail` entries from
   the table above. Leave `hint_got_it` alone.
4. **`HintManager.kt`** — signature change:
   `showHint(id: String, message: String)` →
   `showHint(id: String, title: String, detail: String)`. Populate
   `binding.hintTitle` and `binding.hintDetail`. Update the pending
   queue data class to carry both strings.
5. **`MainActivity.kt`** — update all 8 call sites to pass the split
   strings via `getString(R.string.hint_X_title)` and
   `getString(R.string.hint_X_detail)`.

## Not in scope

- Changing hint copy beyond the title/detail split captured in the
  table above.
- Auto-dismiss after a timeout. Hints stay until user acknowledges.
- Per-hint custom icons (all hints share the lightbulb). Could be a
  follow-up if any hint genuinely warrants a different glyph.
- Localization of the new strings beyond the existing `values/` files.
- Refactoring the priority-queue logic.

## Files changed

| File | Change |
|------|--------|
| `res/drawable/hint_icon_bg.xml` | **New** — 28dp oval, 12%-white fill |
| `res/layout/activity_main.xml` | Hint card block rebuilt: circular icon container, two stacked text views, corner 14dp, padding 12dp |
| `res/values/strings.xml` | 8 × delete + 16 × add (title/detail pairs) |
| `HintManager.kt` | Signature takes `title + detail`, populates both text views |
| `MainActivity.kt` | 8 call-site updates: `hint_X` → `hint_X_title` + `hint_X_detail` |

## Acceptance criteria

- [x] When a hint fires, the card shows:
      - a 28dp oval with a soft white fill and a small lightbulb inside
      - a bold white title that begins with `Tip · `
      - a dimmer second line (detail) below the title
      - a small `×` close glyph top-right (60% white tint)
      - no outline / stroke around the card
- [x] All 8 hint IDs produce title + detail strings per the table
      above — no hint renders with a missing title or detail.
- [x] Tapping the `×` dismisses the card with the existing slide-down
      animation, then shows the next queued hint (if any) after the
      1s inter-hint delay.
- [x] `./gradlew clean assembleDebug testDebugUnitTest lint` succeeds.
- [x] No reference to the old `hint_long_press`, `hint_filters`,
      `hint_drag_select`, `hint_pinch_zoom`, `hint_fast_scroll`,
      `hint_continue_fab`, `hint_progress_bar`, `hint_trash_undo`
      strings remains in code or layouts.

## Task breakdown

- **[task-01-hint-card.md](task-01-hint-card.md)** — the full piece.
  New drawable, layout rebuild, string swap, `HintManager` signature
  change, 8 call-site updates. ~7 steps.
