# SDD-20260418-007: Continue FAB States

**Parent:** SDD-20260418-001 — Visual Redesign (umbrella)

**Depends on:** SDD-20260418-002 ✓

## Summary

Replace the FAB's current "alpha 0.5 + disabled" fallback with two visually
distinct states:

- **Active** — ink-filled pill, white text + icon, label `Continue`,
  elevation 6dp. Tapping scrolls to the next unreviewed tile.
- **Caught up** — translucent white pill (72% white), 1dp `line_strong`
  outline, `ink4` text + icon, label `All caught up`, no shadow, not
  clickable.

Behavior is unchanged: the FAB still shows only when there are both viewed
*and* unviewed items in the current filter. What changes is what it looks
like when the user is already scrolled past the first unreviewed tile —
instead of fading, it declares "all caught up" clearly.

## Why it's separate

Small, focused visual SDD. Touches only the FAB layout attributes, two
string resources, and one Kotlin helper (`updateContinueFabState`). Keeps
the review-flow refresh isolated from other surfaces.

## Scope

1. **Rename label strings**:
   - Keep `continue_reviewing` (legacy; leave as-is until anything else
     that references it is found — it isn't referenced elsewhere, so we
     remove it at the end of this SDD)
   - Add `fab_continue` = "Continue"
   - Add `fab_all_caught_up` = "All caught up"
2. **Layout update** (`activity_main.xml` `fabContinue`):
   - Default `android:text="@string/fab_continue"` (overridden at runtime)
   - Drop `android:contentDescription` — ExtendedFAB uses `text` as
     accessibility label automatically
   - Add `app:strokeWidth="1dp"` (always present; color varies by state
     via Kotlin)
   - Remove `android:textColor` and `app:iconTint` hard-codes (both set
     from Kotlin)
3. **Styling helper** — replace the current `alpha = 0.5f` fallback in
   `updateContinueFabState` with programmatic state swaps:
   - Active → `@color/ink` bg, white text/icon, stroke color
     transparent, elevation 6dp, `isEnabled = true`, label
     `fab_continue`
   - Caught up → `#B8FFFFFF` bg (72% white), `@color/ink4` text/icon,
     `@color/line_strong` stroke, elevation 0, `isEnabled = false`,
     label `fab_all_caught_up`
4. **Click listener guard**: the listener is a no-op when `!isEnabled`
   (Material default), but assert in review.
5. **Legacy string cleanup**: remove `continue_reviewing` from
   `strings.xml` after the layout and Kotlin switch to the new strings.

## Not in scope

- Showing the "caught up" state when *all* items are reviewed (FAB still
  hides entirely when there are no unviewed items). The caught-up state
  only appears when unviewed items exist but the user has scrolled past
  them.
- Animating the transition between states (Material's default tint
  interpolation is fine)
- Icon change between states — both keep `ic_arrow_downward`

## Files changed

| File | Change |
|------|--------|
| `res/values/strings.xml` | Add `fab_continue`, `fab_all_caught_up`; remove `continue_reviewing` |
| `res/layout/activity_main.xml` | FAB attrs trimmed, default `text` updated, `strokeWidth` added |
| `MainActivity.kt` | `updateContinueFabState` rewritten to set bg/text/icon/stroke/elevation/label per state |

## Acceptance criteria

- [ ] Scroll state where user has reviewed top items but not bottom
      ones: FAB shows as a solid ink pill labeled `Continue` with a
      visible shadow. Tap → grid scrolls to the first unreviewed tile.
- [ ] Scroll to the bottom (past the last unreviewed item): FAB
      transitions to a translucent outlined pill labeled
      `All caught up`, no shadow, tap does nothing.
- [ ] Scroll back up above unreviewed items: FAB returns to the active
      solid ink look.
- [ ] FAB still hides (visibility `GONE`) when there are no unviewed
      items in the current filter *and* no viewed items — no regression
      on the visibility logic.
- [ ] `continue_reviewing` string no longer exists.
- [ ] `./gradlew clean assembleDebug testDebugUnitTest lint` succeeds.
