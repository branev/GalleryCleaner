# SDD-20260418-008: Filter Sheet & Empty State

**Parent:** SDD-20260418-001 — Visual Redesign (umbrella)

**Depends on:** SDD-20260418-002 ✓

**Replaces:** SDD-20260418-009 (empty state, merged in — single area of
the product: "filters produced nothing / let me edit them").

## Summary

Rework the filter area end to end: the bottom sheet where filters are
picked, and the empty-state screen that appears when those filters match
nothing. Both are touched by the same user journey, and their copy and
actions cross-reference each other ("Edit filters" on the empty state
opens the sheet). Keeping them in one SDD makes the before/after easier
to review.

### Filter sheet changes
- Four titled sections: **Type**, **Source**, **Date**, **Sort by**
  (adds the Type section that's currently only in the top bar).
- Section labels: 11sp 600 `ink4` uppercase, 0.8 letter-spacing.
- Source chips carry counts in **JetBrains Mono** (e.g. `WhatsApp  5`).
- Sheet handle: 36×4dp `line` color (bump from 32dp).
- Header row: title `Filters` (20sp 700 ink) on the left,
  `Reset` as an accent-colored text link on the right (no MaterialButton).
- **Pending state** — tapping a chip updates a local pending copy. The
  viewmodel isn't touched until the user presses Apply. Cancel and swipe-
  dismiss discard pending changes.
- Footer: outline `Cancel` (1/3 width) + ink-filled
  `Apply · N changes` (2/3 width). The count reflects the diff between
  pending and applied state; Apply is disabled (or just "Apply") when
  `N == 0`.
- While the sheet is shown, status-bar icons go **light** so the system
  time/battery stay legible over the dark scrim; restore on dismiss.

### Empty-state changes
- Icon container: 72dp circle with 1.5dp `line_strong` outline (no fill),
  icon inside in `ink3`. Replaces the current 96dp filled disc.
- Title: 22sp → 20sp, bold, ink.
- Subtitle: 15sp, `ink3`, names the active filter combo dynamically
  (e.g. "Nothing fits **Videos · Viber · Last 7 days**. Try widening
  the range.").
- Primary action: **ink-filled `Reset filters`** (unchanged behavior,
  restyled consistent with other ink pills).
- **New secondary action**: text button `Edit filters` that opens the
  filter sheet. Sits 12dp below the Reset pill.

## Why merged

SDD-008 and SDD-009 both sit on the "find stuff to clean" path. Changes
in one leak into the other: the empty-state subtitle has to name the
same filter combo the sheet exposes; the empty-state `Edit filters`
action is literally the entrypoint to the sheet. Splitting them into
two SDDs would duplicate filter-naming code and force an awkward
dependency chain. One atomic change here is cleaner.

## Scope

1. **`bottom_sheet_filters.xml` rebuild**: Type section added, section
   label style unified, handle 36dp, accent-colored Reset text link,
   chips wrap (remove `HorizontalScrollView` wrappers), new footer row.
2. **`FilterBottomSheetFragment` pending state**: local copies of the
   four filter fields initialized on `onViewCreated`; chip taps update
   local copies; `Apply` commits via `viewModel.*` setters; `Cancel`
   dismisses; swipe-dismiss equals Cancel. Change count is computed as
   `pending != applied` across the four fields.
3. **Source chip mono counts**: build each chip's label as a
   `SpannableString` with a JetBrains Mono span over the `N` count. The
   label becomes `"WhatsApp  5"` rather than `"WhatsApp (5)"`.
4. **Status-bar flip**: override `getTheme()` or set
   `window.decorView.systemUiVisibility` in `onStart()` / restore in
   `onDismiss()` so the sheet's scrim gets light system icons.
5. **Empty-state layout**: drawable `drawable/empty_state_ring.xml`
   (1.5dp stroke, transparent fill, 999dp corners) replaces the tinted
   disc; title to 20sp; secondary `btnEditFilters` TextButton added
   below `btnResetFilters`.
6. **Filter-combo subtitle helper**: pure function taking the current
   filter state and producing a single-line string like
   `Videos · Viber · Last 7 days`. Handles "all selected" = omit,
   multiple sources = `2 sources`, custom date range = `Mar 1–Mar 15`.
7. **`MainActivity` wiring**: compose the subtitle dynamically in the
   `NoMatchingItems` branch of `renderState`, wire `btnEditFilters`
   to open `FilterBottomSheetFragment`.
8. **String updates**:
   - Add `edit_filters` = "Edit filters".
   - Add `no_matches_subtitle` = `"Nothing fits %1$s. Try widening the range."`.
   - Keep `reset_filters` as-is.

## Not in scope

- Animated chip selection (Material's default ripple is fine).
- "Smart filter summary chips" below the top bar (SDD-20260321-005,
  separate pending SDD).
- Collapsing/reordering section order based on which filters are active.
- Persisting pending state across sheet dismiss-and-reopen (pending
  resets to applied on every open; standard bottom-sheet UX).

## Files changed

| File | Change |
|------|--------|
| `res/layout/bottom_sheet_filters.xml` | Rebuilt: handle size, sections, footer, chips wrap |
| `res/drawable/bottom_sheet_handle.xml` | Verify `line` color + 36×4 — touch if off |
| `res/drawable/empty_state_ring.xml` | **New** — 1.5dp `line_strong` ring |
| `res/layout/activity_main.xml` | Empty-state block: swap icon container to `empty_state_ring`, title 20sp, add `btnEditFilters` under `btnResetFilters` |
| `res/values/strings.xml` | Add `edit_filters`, `no_matches_subtitle`, `filter_combo_sources_count` plural, `filter_combo_dot_separator` |
| `FilterBottomSheetFragment.kt` | Pending state, Type section, mono source counts, Apply/Cancel, status-bar flip |
| `MainActivity.kt` | `NoMatchingItems` branch composes subtitle; `btnEditFilters` click handler; remove now-orphaned `empty_state_icon_bg` drawable reference in layout (drawable file stays until SDD-009 cleanup pass — or delete if not referenced anywhere else) |
| `FilterComboFormatter.kt` | **New** — pure util that formats a `(mediaTypes, sources, dateRange, sort)` tuple into a display string |
| `app/src/test/…/FilterComboFormatterTest.kt` | **New** — covers: all-selected → omit, one source → label, many sources → count, preset date → label, custom date → range, no filters → fallback |

## Acceptance criteria

- [x] Tapping the Filters pill opens the sheet with four visible
      sections in order: Type, Source, Date, Sort by.
- [x] Source chips show `Name  N` with the N in JetBrains Mono.
- [x] Tapping chips updates the pill but does NOT refresh the grid
      behind the sheet (changes are pending).
- [x] `Apply · N changes` label updates live as chips toggle. When
      pending equals applied, the button reads `Apply` (no change
      count, still enabled).
- [x] Tapping Apply commits changes, dismisses the sheet, grid updates.
- [x] Tapping Cancel or swiping the sheet down discards pending
      changes, grid is unchanged.
- [x] While sheet is open, status-bar time/icons render light
      (visible against the dark scrim). Restores to dark on dismiss.
- [x] Empty state shows a thin outlined circle, not a filled disc.
- [x] Empty-state subtitle mentions the specific filter combo (e.g.
      `Videos · Viber · Last 7 days`).
- [x] `Edit filters` text button appears below the Reset filters pill
      on the empty state; tapping it opens the filter sheet.
- [x] `./gradlew clean assembleDebug testDebugUnitTest lint` succeeds.
- [x] `FilterComboFormatterTest` covers ≥ 5 cases.

## Task breakdown

- **[task-01-filter-sheet.md](task-01-filter-sheet.md)** — the heavier
  piece. Layout rebuild, pending-state rewrite, mono counts, status-bar
  flip, sheet-only concerns. ~10 steps.
- **[task-02-empty-state.md](task-02-empty-state.md)** — layout tweak,
  new ring drawable, `FilterComboFormatter` utility + tests, `MainActivity`
  wiring. ~6 steps.

Recommended order: 01 first (establishes the sheet + the formatter
concept), 02 consumes the formatter.
