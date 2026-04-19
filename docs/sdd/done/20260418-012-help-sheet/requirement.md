# SDD-20260418-012: Help / Tips Sheet

**Parent:** SDD-20260418-001 — Visual Redesign (umbrella)

**Depends on:** SDD-20260418-002 ✓, SDD-20260418-008 ✓ (borrows the
filter sheet's header / status-bar-flip pattern)

## Summary

Rework the Help bottom sheet (the "Tips & shortcuts" panel behind the
top-bar `?` button) end-to-end. Four user-visible changes:

1. **Tip rows restyled** — icons lose their ink tint and become accent
   orange at 22dp; labels shrink to 13sp `ink2`; a 1dp `line`-color
   divider sits between each row (no bullet indentation).
2. **`Reset tips` moves to the header** — currently a centered
   underlined link at the bottom, now a small accent-colored text link
   to the right of the sheet title, matching the Filters sheet's
   `Reset` affordance.
3. **Ko-fi button → Ko-fi support card** — the current "Buy me a
   coffee" pill button is replaced with a full-width `surface_alt`
   card at the bottom (14dp radius, accent-soft round icon, two-line
   copy, chevron right). Still opens `https://ko-fi.com/branev`.
4. **Explicit `OK` button removed** — dismissal via swipe-down or
   tap-outside, same as the Filters sheet. The `overlay_ok` string
   drops from `strings.xml` since this was its last caller.

Status-bar icons go **light** while the sheet is open (to stay legible
against the dark scrim) and revert on dismiss — same
`WindowCompat.getInsetsController` pattern as the Filters sheet.

## Visual changes

- **Sheet container**: root bg `@color/surface` (already; confirm),
  top corners 20dp, drag handle 36×4dp `line` — same as Filters sheet.
- **Header row**:
  - Left: title `Tips & shortcuts` (20sp 700 ink). Lowercase `s` in
    "shortcuts" to match the design handoff.
  - Right: `Reset tips` — text link in accent, 12sp 600, small padding,
    `selectableItemBackgroundBorderless`. Clicking resets all hint
    flags (existing `HintPreferences.resetAllHints()`), shows the
    existing confirmation snackbar, and **leaves the sheet open**
    so the user stays in context.
- **Tip rows** (LinearLayout container populated in code):
  - Row: 22dp icon (left, `accent` tint) + 12dp gap + 13sp label
    (`ink2`). Vertical padding 12dp.
  - Between every row (but **not** above the first or below the last):
    a 1dp `line` divider.
- **Ko-fi support card** (new, replaces the OK button + pill button
  block):
  - Full-width (16dp horizontal margin), 14dp radius, `surface_alt`
    fill, no elevation.
  - Left: 32dp `accent_soft` round icon container, Ko-fi glyph
    centered inside (existing `kofi_logo` drawable, tinted accent).
  - Middle: two lines — title `Enjoying the app?` (14sp 700 ink),
    subtitle `One coffee keeps it ad-free.` (12sp `ink3`).
  - Right: 20dp chevron-right icon in `ink3`.
  - Whole card clickable via `foreground="?selectableItemBackground"`.
- **No explicit OK button.**

## Behavioral changes

- Status bar flip on `onStart` / revert on `onDestroyView` — identical
  code path to `FilterBottomSheetFragment`.
- `Reset tips` now lives in the header. Click behavior unchanged
  (`HintPreferences.resetAllHints()` → snackbar). The sheet no
  longer auto-dismisses on reset — the snackbar confirms the action
  and the user can keep reading tips or swipe down when ready.
- No explicit OK button; swipe-down and tap-outside remain the only
  dismissal paths. (Tested behaviors already supported by
  `BottomSheetDialogFragment` default.)

## String changes

| Change | Before | After |
|---|---|---|
| Title | `tips_and_shortcuts` = "Tips & Shortcuts" | same key, value `Tips & shortcuts` |
| Reset tips link | `reset_tips` (existing; confirm value) | reuse as-is |
| **New** | — | `kofi_card_title` = "Enjoying the app?" |
| **New** | — | `kofi_card_subtitle` = "One coffee keeps it ad-free." |
| **Delete** | `overlay_ok` = "OK" (last caller here) | removed |

Keep the existing 8 `tip_*` tip strings unchanged.

## Scope

1. **`bottom_sheet_help.xml` rebuild** — drag handle, header row with
   title + `Reset tips` text link, tip-rows LinearLayout container,
   Ko-fi card. Remove the old OK button, bottom Reset Tips link, and
   outer Ko-fi pill button.
2. **Drawables (new)**:
   - `kofi_icon_bg.xml` — 32dp oval, `accent_soft` fill.
   - `ic_chevron_right.xml` — 20dp vector (simple `>` arrow, ink3
     tint applied in layout).
3. **`HelpBottomSheetFragment.kt`**:
   - Populate tips dynamically into the new container — icon 22dp
     accent, label 13sp `ink2`, insert a 1dp `line` divider between
     rows.
   - Move `setupResetButton()` logic to the header `Reset tips`
     TextView (`btnResetTips` or similar id). The bottom-of-sheet
     reset link is gone.
   - Wire the Ko-fi support card's root click to the existing Ko-fi
     URL-open intent.
   - Add `onStart()` / `onDestroyView()` override to flip
     `isAppearanceLightStatusBars` (mirror of
     `FilterBottomSheetFragment`).
   - Remove the `btnOkHelp` / OK-button handler entirely.
4. **`strings.xml`**:
   - Change `tips_and_shortcuts` value.
   - Add `kofi_card_title`, `kofi_card_subtitle`.
   - Remove `overlay_ok` (final caller dies with this SDD).
5. **`MainActivity.kt`** — no changes expected; the sheet trigger
   (`setupHelpButton()` → `HelpBottomSheetFragment.newInstance().show`)
   stays the same.

## Not in scope

- Adding/removing tips. The 8 existing tips stay as-is.
- Per-tip color categorization (e.g. destructive tips in danger). All
  icons share the `accent` tint.
- Collapsing groups of tips into categories.
- Searching / filtering tips.
- Adding haptic feedback on the Ko-fi card tap.
- Localization beyond existing languages for the new strings.

## Files changed

| File | Change |
|------|--------|
| `res/layout/bottom_sheet_help.xml` | Rebuilt end-to-end |
| `res/drawable/kofi_icon_bg.xml` | **New** — 32dp accent-soft oval |
| `res/drawable/ic_chevron_right.xml` | **New** — 20dp chevron vector |
| `res/values/strings.xml` | Title value changed; `kofi_card_title` + `kofi_card_subtitle` added; `overlay_ok` removed |
| `HelpBottomSheetFragment.kt` | Tip row style, Reset-tips moved to header, Ko-fi card click, status-bar flip, OK button removed |

## Acceptance criteria

- [x] Opening the Help sheet shows `Tips & shortcuts` (lowercase "s")
      on the left and a small accent `Reset tips` link on the right,
      both in the header row.
- [x] Tapping `Reset tips` clears hint preferences and shows the
      existing snackbar confirmation. The sheet stays open.
- [x] Each of the 8 tip rows renders with an accent-tinted 22dp icon
      and a 13sp `ink2` label. A thin `line`-color 1dp divider sits
      between rows and nowhere else.
- [x] The bottom of the sheet shows a **Ko-fi support card**
      (surface_alt fill, 14dp radius) with an accent-soft 32dp round
      icon, title `Enjoying the app?`, subtitle `One coffee keeps it
      ad-free.`, and a chevron right. Tapping the card opens
      `https://ko-fi.com/branev` in a browser.
- [x] No explicit `OK` button is present. Swipe-down and tap-outside
      both dismiss the sheet.
- [x] While the sheet is open, status-bar icons render **light**.
      They revert to dark after dismiss.
- [x] `overlay_ok` string is removed from `strings.xml` with no
      compilation errors (confirm by greping `R.string.overlay_ok`
      + `@string/overlay_ok` — both empty).
- [x] `./gradlew clean assembleDebug testDebugUnitTest lint` succeeds.

## Task breakdown

- **[task-01-help-sheet.md](task-01-help-sheet.md)** — the full piece.
  Layout rebuild, two new drawables, fragment rewrite (tips render,
  reset in header, Ko-fi card click, status-bar flip), string
  updates. ~8 steps.
