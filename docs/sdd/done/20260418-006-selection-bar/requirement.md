# SDD-20260418-006: Selection Action Bar

**Parent:** SDD-20260418-001 — Visual Redesign (umbrella)

**Depends on:** SDD-20260418-002 ✓, SDD-20260418-004 ✓

## Summary

Tighten the bottom selection action bar to match Direction B: a single
floating pill instead of a broad rail, a split count + size readout (bold
ink for the count, small mono `ink3` for the size), a thin vertical divider
between the close/select-all controls and the readout, and a lighter
elevation with a subtle outline so the pill feels grounded without shouting.

## Why it's separate

The tile-level side of selection (55% dim overlay on non-selected tiles,
accent ring + tint + check circle on selected ones) already shipped in
SDD-004. This SDD only restyles the bottom bar itself — its own file,
its own commit.

## Scope

1. **Pill container**: keep `MaterialCardView` with 28dp corner radius
   (half-height of 56dp → already a pill). Drop elevation `8dp → 4dp` and
   add a 1dp `@color/line` stroke so the pill doesn't float gray-on-gray
   on the cream background.
2. **Vertical divider**: 1dp wide × 20dp tall `View` filled with
   `@color/line`, sitting between `btnSelectAll` and the count/size
   readout. Provides the visual cue for the two "zones" of the bar.
3. **Stacked count + size**: replace the single `selectionSize` `TextView`
   with a vertical `LinearLayout` containing two rows:
   - **`selectionCount`** — `N selected`, 13sp 700 `@color/ink`,
     left-aligned, ellipsize if there's a `(H hidden)` suffix
   - **`selectionSizeSmall`** — `789 kB`, 11sp 500 JetBrains Mono
     `@color/ink3`, left-aligned; visibility `gone` when size is 0
4. **MainActivity wiring**: set both TextViews separately inside the
   `Selection` branch of `renderState`. Remove the now-unused
   `selection_count_size` concatenation.
5. **String cleanup**: delete the `selection_count_size` string. Keep
   `selected_count` and `selected_with_hidden`.
6. **Delete button**: stays danger-colored (`md_theme_error` bg,
   `md_theme_onError` text + icon tint). No change.
7. **Close + Select-all**: stay ink-colored via `?attr/colorOnSurface`.
   No change.

## Not in scope

- 55% dim on non-selected tiles (shipped in SDD-004)
- Accent ring + check on selected tiles (shipped in SDD-004)
- Trash confirmation dialog rework (deferred; no matching SDD)
- FAB behavior during selection (SDD-007)

## Files changed

| File | Change |
|------|--------|
| `res/layout/activity_main.xml` | Restructure the selection bar's inner LinearLayout: add divider view, swap single TextView for stacked count + size container; lighter elevation; stroke on the card |
| `res/values/strings.xml` | Remove `selection_count_size` |
| `MainActivity.kt` | Rewrite the selection-text setup in `renderState`'s `Selection` branch to feed two TextViews |

## Acceptance criteria

- [ ] Selection bar is a pill (visibly rounded ends), not a wide card
- [ ] 1dp `line`-color outline visible on the pill
- [ ] Close (×) on the left, then Select all, then a 1dp gray divider,
      then the stacked count + size, then the Delete button on the right
- [ ] Count text: `N selected` in bold ink, 13sp
- [ ] Size text: `X kB`/`X MB`/`X GB` below the count, 11sp mono, muted
      `ink3` color
- [ ] Size text hides when `N selected` is 0 or selection total size is 0
- [ ] Selecting more items updates both texts live, with no layout jump
- [ ] `selection_count_size` no longer exists in `strings.xml`
- [ ] `./gradlew clean assembleDebug testDebugUnitTest lint` succeeds
- [ ] Smoke test: long-press a tile → bar appears. Tap several more →
      count + size update. Tap Select all → whole library picked. × →
      bar disappears, grid returns to normal
