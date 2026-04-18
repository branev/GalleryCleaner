# SDD-20260418-002: Design Tokens

**Status:** COMPLETE

**Parent:** SDD-20260418-001 — Visual Redesign (umbrella)

## Summary

Lay the foundation for the visual redesign by replacing the color palette,
adding JetBrains Mono as a second font family, and rewiring Material 3 theme
role bindings. **No layouts or fragments are modified in this SDD** — only
resource files.

After this SDD ships, the app still builds and runs. Existing layouts use the
old color names; those names are remapped to the new Direction B values so
nothing looks broken. Every subsequent SDD (003–014) then refactors its
surface to use the new token names directly and removes the legacy mappings
as it goes.

## Why it's separate

The whole 13-child redesign depends on these tokens existing. Shipping them
first in isolation lets each following SDD assume the palette, font, and theme
are ready. It also lets us ship something small and mergeable on day one.

## Scope

1. Bundle JetBrains Mono 4 weights (400, 500, 600, 700) + OFL license notice
2. Add all 17 Direction B tokens to `colors.xml`
3. Rewire Material 3 `md_theme_*` role bindings to the new tokens
4. Update chip color state lists (`color/chip_*.xml`) to the new ink-vs-line
   logic
5. Remap legacy color names so existing layouts keep working until their
   respective SDD migrates them

## Not in scope

- Any changes to layouts (`res/layout/*.xml`) or Kotlin code
- Replacing per-surface drawables (filter pill active state, selection overlay,
  grid-item background, source pill, video badge) — each belongs to its
  respective child SDD
- Using JetBrains Mono anywhere on-screen yet — the font is bundled and wired,
  but no TextView references it until later SDDs
- Removing legacy color names from `colors.xml` — happens gradually as each
  surface is migrated

## Files changed

| File | Change |
|------|--------|
| `res/font/jetbrains_mono_*.ttf` (×4) | **New** — bundled TTFs |
| `res/font/jetbrains_mono.xml` | **New** — family definition |
| `THIRD_PARTY_NOTICES.md` | **New** — OFL text for Inter + JetBrains Mono |
| `README.md` | Tiny — reference the notices file |
| `res/values/colors.xml` | Rewritten — new tokens + remapped legacy names |
| `res/values/themes.xml` | Minor — status bar color to `@color/bg` |
| `res/color/chip_bg_color.xml` | Rewritten |
| `res/color/chip_text_color.xml` | Rewritten |
| `res/color/chip_stroke_color.xml` | Rewritten |

## Acceptance criteria

- [x] 4 JetBrains Mono TTFs and family XML present under `res/font/`
- [x] `THIRD_PARTY_NOTICES.md` exists at project root with both OFL texts
- [x] All 17 Direction B tokens in `colors.xml` with correct hex values
- [x] All `md_theme_*` roles point to Direction B tokens
- [x] Every legacy color name still resolves (build doesn't fail for missing
      references)
- [x] Chip color state lists follow the ink-vs-line logic
- [x] `./gradlew assembleDebug` succeeds
- [x] `./gradlew testDebugUnitTest` passes
- [x] `./gradlew lint` has no new errors
- [x] Launching the app shows a cream background, ink-colored title, and
      orange-tinted Filters button — no crashes, no missing resources
