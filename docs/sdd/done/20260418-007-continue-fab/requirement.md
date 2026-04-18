# SDD-20260418-007: Continue FAB States

**Status:** COMPLETE

**Parent:** SDD-20260418-001 — Visual Redesign (umbrella)

**Depends on:** SDD-20260418-002 ✓

## Summary

Replace the FAB's current "alpha 0.5 + disabled" fallback with cleaner
visibility semantics: the FAB appears **only** when tapping it is useful,
otherwise it hides entirely.

- **Active** — ink-filled pill, white text + icon, label `Continue`,
  elevation 6dp. Tapping scrolls to the next unreviewed tile.
- **Hidden** — whenever the user is at or past the first unreviewed
  item (or everything is reviewed). No ghost, no disabled-looking
  button. Less chrome in the grid during normal scroll-down review.

The design spec shows a second "All caught up" ghost state for the
edge case where the user has reviewed *everything*; in practice
gallery-cleaner users delete or keep as they go, so that state is rare
and deliberately deferred. If we want it back, reintroduce it via the
commented-out block in `updateContinueFabState`.

## Why it's separate

Small, focused visual SDD. Touches only the FAB layout attributes, two
string resources, and one Kotlin helper (`updateContinueFabState`). Keeps
the review-flow refresh isolated from other surfaces.

## Scope

1. **String rename**:
   - Remove `continue_reviewing` ("Continue reviewing")
   - Add `fab_continue` = "Continue"
2. **Layout update** (`activity_main.xml` `fabContinue`):
   - Default `android:text="@string/fab_continue"`
   - Drop `android:contentDescription` (ExtendedFAB reads text for a11y)
   - Add `app:shapeAppearanceOverlay="@style/PillShape"` (explicit pill
     corners after adding `strokeWidth`)
   - Add `app:strokeWidth="1dp"` (reserved for future ghost state; color
     set transparent in Kotlin)
   - Remove hard-coded `android:textColor` and `app:iconTint`
3. **Visibility + state logic** — `updateContinueFabState` is now the
   single authority on FAB visibility:
   - Hide if not in `Normal`/`Selection` state, no viewed items, no
     media items, all items reviewed, or user is at/past first
     unreviewed
   - Otherwise show the active pill: `@color/ink` bg, white text/icon,
     transparent stroke, elevation 6dp, `isEnabled = true`
4. **Hint trigger**: fire `HINT_CONTINUE_FAB` only when the FAB ended
   up visible after `updateContinueFabState`, not before.

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
| `res/values/strings.xml` | Add `fab_continue`, remove `continue_reviewing` |
| `res/layout/activity_main.xml` | FAB attrs trimmed, default `text` updated, `strokeWidth`, pill shape overlay added |
| `MainActivity.kt` | `updateContinueFabState` becomes the sole visibility+state authority; `observeViewedItems` stops toggling FAB visibility directly |

## Acceptance criteria

- [x] Fresh launch (nothing reviewed yet): FAB hidden.
- [x] Scroll down through the top of the grid, reviewing tiles along
      the way: FAB stays hidden (user is currently at/past the first
      unreviewed tile).
- [x] Scroll back up (past some reviewed tiles): FAB appears as a
      solid ink pill labeled `Continue`. Tap → grid scrolls to the
      first unreviewed tile; FAB hides once the user's view catches up
      to it.
- [x] Review every item in the current filter: FAB stays hidden (no
      ghost state in this iteration).
- [x] `continue_reviewing` string no longer exists.
- [x] `./gradlew clean assembleDebug testDebugUnitTest lint` succeeds.
