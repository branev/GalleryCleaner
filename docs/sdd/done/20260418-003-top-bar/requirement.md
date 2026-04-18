# SDD-20260418-003: Top Bar Redesign

**Status:** COMPLETE

**Parent:** SDD-20260418-001 — Visual Redesign (umbrella)

**Depends on:** SDD-20260418-002 — Design Tokens ✓

## Summary

Restyle the main-screen top bar to match Direction B: remove the yellow Ko-fi
button, switch the green title to ink, replace the red dot filter indicator
with a soft-accent pill active state, thin the progress bar to a 2dp accent
hairline, and add a right-aligned `N LEFT` unreviewed counter. Also refresh
the fast-scroller thumb/tooltip typography.

After this SDD ships, the main-screen chrome above the grid is done.

## Why it's separate

The top bar is one coherent surface touched by many redesign bullets. Keeping
it in one SDD (instead of six micro-SDDs) preserves visual cohesion — the
title, help icon, filters pill, chips, counter, and progress bar read as one
row and should change together.

## Scope

1. **Remove Ko-fi button** from the top bar (click listener, view, one layout
   row). The `kofi_logo` drawable and `support_developer` string stay in the
   project — SDD-012 will reuse them in the Help sheet.
2. **Title → ink**. Change `textColor="@color/header_title_green"` to
   `"@color/ink"` on `appTitle`.
3. **Filter pill active state**: replace today's background-tint-swap + red
   dot with a single driven-by-`isActivated` state:
   - inactive: transparent fill, 1.5dp `line_strong` outline, `ink2` text
   - active: `accent_soft` fill, 1.5dp `accent` outline, `accent_deep` text
   - Remove the `filterActiveDot` view and its drawable.
4. **Photos/Videos chips + Filters pill**: size to **32dp height** per the
   design (via `chipMinHeight="32dp"` + `ensureMinTouchTargetSize="false"` on
   the Chip style; `minHeight="32dp"` + zero insets on the Filters
   MaterialButton). 13sp text, 18dp horizontal padding on chips. Filters
   button corner radius → 16dp (half-height pill).
5. **Help icon**: change `ic_help_outline.xml` `fillColor` from
   `@color/badge_unviewed_text` (now orange after SDD-002 remap) to
   `@color/ink3` (neutral gray). Per design, the help icon is neutral chrome,
   not an accent-colored control.
6. **Review progress bar**: 2dp height (was 3dp), `accent` indicator color
   (was `fast_scroll_tooltip_bg` → ink).
7. **`N LEFT` counter**: new right-aligned TextView on the chip row showing
   the unreviewed-items count. JetBrains Mono 10sp, `ink4`, 0.5sp letter
   spacing, all-caps.
8. **Fast scroller**: thumb becomes a full pill (999dp corner radius) in ink;
   tooltip uses JetBrains Mono 10sp all-caps (e.g. `APR 12`).
9. **Legacy cleanup**: remove `header_title_green`, `filter_btn_active_bg`,
   and `filter_btn_active_text` from `colors.xml` — nothing references them
   after this SDD.

## Not in scope

- Date section headers (SDD-005)
- Continue FAB states (SDD-007)
- Relocating Ko-fi to the Help sheet (SDD-012)
- Any grid-tile changes below the top bar (SDD-004)

## Files changed

| File | Change |
|------|--------|
| `res/layout/activity_main.xml` | Ko-fi button removed, title color, filter pill attrs, chip size, N LEFT TextView, progress bar attrs, tooltip font/size |
| `res/drawable/filter_active_dot.xml` | **Deleted** |
| `res/color/filter_pill_bg_tint.xml` | **New** |
| `res/color/filter_pill_text_tint.xml` | **New** |
| `res/color/filter_pill_stroke_tint.xml` | **New** |
| `res/drawable/fast_scroll_thumb.xml` | Corner radius → 999dp |
| `res/values/strings.xml` | New `n_left_counter` string |
| `res/values/colors.xml` | Remove 3 unused legacy tokens |
| `MainActivity.kt` | Ko-fi click listener removed, `updateFiltersButtonAppearance` rewritten to toggle `isActivated`, N LEFT updater added |

## Acceptance criteria

- [x] Top bar has **one row** of actions: help (?), Filters pill (Ko-fi gone)
- [x] App title renders in near-black ink, not green
- [x] Filters pill inactive state: neutral outline, `ink2` text
- [x] Filters pill active state: soft-orange fill, accent outline, deep-orange
      text (no red dot anywhere)
- [x] Photos/Videos chips: 40dp height (match Filters pill), 13sp text, tighter horizontal padding
- [x] Progress bar: 2dp hairline in warm orange
- [x] `N LEFT` counter visible right of the chip row in JetBrains Mono when
      there are unreviewed items; updates live as items are viewed/deleted
- [x] Fast-scroll thumb is a full pill (visibly rounder); tooltip shows dates
      like `APR 12` in JetBrains Mono, all-caps
- [x] `filter_active_dot.xml` drawable is deleted
- [x] `colors.xml` no longer contains `header_title_green`,
      `filter_btn_active_bg`, `filter_btn_active_text`
- [x] `./gradlew clean assembleDebug testDebugUnitTest lint` succeeds
- [x] Manual smoke: apply/remove a filter and confirm the pill toggles
      between the two states; scroll the grid and confirm the fast-scroller
      tooltip text reads as `APR 12` format
