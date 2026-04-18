# SDD-20260418-004: Grid Tile Redesign

**Parent:** SDD-20260418-001 — Visual Redesign (umbrella)

**Depends on:** SDD-20260418-002 — Design Tokens ✓

## Summary

Rework the grid tile visuals so photos are the hero and chrome steps back.
One glass-dark chip style for both source and video badges, softer 14dp
corners, a gentler reviewed-tile fade, and a selection state built around
an accent ring + corner check circle instead of a darkening overlay.

Design reference: *One square, five states.*

| State | Visual |
|-------|--------|
| Unviewed · default | full color, tiny glass-dark source pill |
| Reviewed | photo at 85% opacity + 75% saturation, same source pill |
| Video | same as unviewed + glass-dark duration badge top-left |
| Selected | 3dp accent ring + 14% accent tint + 22dp accent check circle top-right |
| Not selected (during selection mode) | 55% white overlay dims unselected tiles |

## Scope

1. **Tile corner radius**: `RoundedImageCorners` style 4dp → **14dp**.
2. **Glass-dark chip style** — new single drawable used by both badges:
   4dp radius, `#99000000` fill. Replaces `badge_bg_unviewed.xml`,
   `badge_bg_viewed.xml`, and the current `video_badge_bg.xml`.
3. **Source badge**: text 10sp white, font-weight 600, letter-spacing 0.02sp,
   padding 6dp/2dp horizontal/vertical. Same glass-dark bg.
4. **Video badge**: same chip language — 10sp white, play glyph + duration.
5. **Reviewed calibration**: drop root-view alpha trick. Apply alpha **0.85**
   and saturation **0.75** directly to the `ImageView` (photo only — badges
   stay readable). No more badge color swap for reviewed state.
6. **Selected state** (photo stays visible, no darkening):
   - **Ring**: 3dp accent stroke overlay, same 14dp corner radius.
   - **Accent tint**: 14% accent solid on top of the photo (warm glow).
   - **Check circle**: 22dp solid accent circle with white check glyph,
     top-right, subtle shadow.
7. **Dimmed (unselected-during-selection)**: 55% white overlay. Currently
   the `selectionOverlay` is shown on selected items with 30% black — repurpose
   it for the *opposite* case (shown on unselected items during selection
   mode).
8. **Adapter logic** (`ImageAdapter.kt`): stop mutating root-view alpha and
   badge color-swapping. Apply `colorFilter` + `alpha` to `binding.imageView`
   only. Show/hide the new ring/tint/dim views based on
   `isSelected` and `isSelectionMode`.
9. **Legacy cleanup**: remove `badge_unviewed_*`, `badge_viewed_*` color
   tokens from `colors.xml`; delete old `badge_bg_unviewed.xml`,
   `badge_bg_viewed.xml`, `video_badge_bg.xml`, and
   `selection_overlay.xml` (replaced).

## Not in scope

- Date section headers (SDD-005)
- Long-press/drag-select behavior (already exists; untouched)
- Tile click → MediaViewerActivity (SDD-013)
- Tile radius interaction with pinch-zoom (existing, untouched)

## Files changed

| File | Change |
|------|--------|
| `res/values/themes.xml` | `RoundedImageCorners` cornerSize 4dp → 14dp |
| `res/drawable/glass_dark_chip.xml` | **New** — shared chip bg |
| `res/drawable/tile_selection_ring.xml` | **New** — 3dp accent ring, 14dp corners |
| `res/drawable/tile_selection_tint.xml` | **New** — 14% accent, 14dp corners |
| `res/drawable/tile_dim_overlay.xml` | **New** — 55% white fill, 14dp corners |
| `res/drawable/tile_selected_check_bg.xml` | **New** — 22dp accent circle |
| `res/drawable/badge_bg_unviewed.xml` | **Deleted** |
| `res/drawable/badge_bg_viewed.xml` | **Deleted** |
| `res/drawable/video_badge_bg.xml` | **Deleted** (replaced by glass_dark_chip) |
| `res/drawable/selection_overlay.xml` | **Deleted** |
| `res/layout/item_image.xml` | Reworked: new ring + tint + dim views, check 22dp, source badge 10sp white, video badge 10sp white |
| `ImageAdapter.kt` | `updateViewedVisuals` applies saturation+alpha to `imageView`; `updateSelectionVisuals` toggles ring/tint/dim/check |
| `res/values/colors.xml` | Remove `badge_unviewed_bg`, `badge_unviewed_text`, `badge_viewed_bg`, `badge_viewed_text` |

## Acceptance criteria

- [ ] Grid tiles render with 14dp corner radius (visibly softer than before)
- [ ] Source pill is a small dark chip with white text (not peach/orange)
- [ ] Video tile shows a matching dark chip with play glyph + duration top-left
- [ ] Reviewed tiles: photo subtly faded (85% opacity, 75% saturation) —
      **readable**, not muddy. Source pill on reviewed tile stays at full
      color/contrast
- [ ] Enter selection mode: unselected tiles dim to ~55% lightness
- [ ] Selected tile: accent-colored ring around it, subtle warm tint on the
      photo, orange check circle top-right. Photo details still visible
- [ ] Exiting selection mode restores all tiles to their normal state
- [ ] `./gradlew clean assembleDebug testDebugUnitTest lint` succeeds
- [ ] No references remain to `badge_unviewed_bg`, `badge_unviewed_text`,
      `badge_viewed_bg`, `badge_viewed_text`, or the four deleted drawables
- [ ] Manual smoke test: scroll through large library, enter/exit selection,
      select and deselect items, mark items as viewed → all states render
      correctly
