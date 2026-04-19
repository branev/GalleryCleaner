# SDD-20260418-010: Delete Success

**Parent:** SDD-20260418-001 — Visual Redesign (umbrella)

**Depends on:** SDD-20260418-002 ✓

## Summary

Rework the delete-success experience from a full-screen dark scrim with a
bouncy green checkmark + bottom Snackbar into a **compact centered card**
that celebrates the freed size, with **Undo and Continue as peer
actions inside the card** and a **7-second progress ring** visually draining
around the Undo icon. The Snackbar goes away on the Android-11+ path.

The celebration is intentionally small-but-joyful: a **hero display-size
readout** ("789 kB"), a soft accent-check badge, and a spray of **22
deterministic brand-colored confetti pieces** behind the card. No rainbow,
no lottie, no physics — a static spray seeded from the freed-size value
so repeat deletes look consistent but never identical.

### Visual changes

- Full-screen dark scrim (`overlay_bg` #99000000) stays — keeps the grid
  muted so the card stands out. Elevation stays at 10dp.
- **Centered card** on the scrim: `surface` fill (`#FFFDF8`), 20dp radius,
  elevation 4, ~280dp wide, vertical padding 28dp.
- **Check badge** at top of card: 56dp circle in `accent_soft` (`#FBE0D5`),
  accent-colored check glyph inside (24dp). Replaces the 120dp green
  checkmark with bouncy scale.
- **Hero readout**: freed size rendered at display scale — 30sp, weight 700,
  color `ink`, JetBrains Mono (e.g. `789 kB`). Single line.
- **Subtitle**: `freed · 4 items moved to trash` — 13sp, `ink3`, Inter,
  plural-aware.
- **Action row** (new): two buttons side-by-side at bottom of card.
  - **Undo** — outline pill (1dp `line_strong`), ink text, 44dp tall,
    with a 7-second **progress ring** drawn around the leading undo icon.
    The ring drains (arc sweep shrinks toward 0) over 7s; when it hits 0,
    the card auto-dismisses and the delete is committed.
  - **Continue** — ink-filled pill, white text, 44dp tall. Tapping it
    dismisses the card immediately and commits (same end-state as the
    timer running out).
  - Tapping **Undo** (anywhere on the button, not just the ring) cancels
    the timer and restores the trashed items via the existing
    `performRestore()` path.
- **Confetti** behind the card: 22 pieces, deterministic positions seeded
  from the freed-size `Long`. Mix of rect + small-circle shapes, rotated,
  sized 6–14dp. Colors sampled from `accent`, `ink`, `accent_soft`,
  `success`. Drawn on a full-screen transparent layer *under* the card
  (so the card's white surface reads on top).

### Behavioral changes

- The **bottom Snackbar** with "X of space saved" + "Undo" action is
  removed on the Android-11+ path. Its role is absorbed into the card.
- The **commit trigger** moves from "Snackbar auto-dismiss at 8000ms" to
  "progress ring hits 0 at 7000ms OR user taps Continue". Undo still
  cancels both.
- The overlay **tap-to-dismiss** behavior on the scrim is removed —
  dismiss is intentional via one of the two buttons or the ring
  timeout. Keeps an accidental tap from committing a delete the user
  was about to undo.
- Pre-Android-11 path is **unchanged** — still shows the plain Snackbar
  ("X items deleted, Y freed") with no card, because that path has no
  trash/undo semantics. Will be removed entirely when SDD-20260419-001
  (minSdk 30) lands; until then, we keep it simple.

## Scope

1. **`activity_main.xml` overlay rebuild**: keep the `FrameLayout` scrim
   and its `id="deleteSuccessOverlay"` (other code still references it),
   but replace its inner `LinearLayout` with a `MaterialCardView` +
   confetti layer. New child IDs: `confettiLayer`, `successCard`,
   `successCheckBadge`, `successHeroSize`, `successSubtitle`,
   `undoProgressRing`, `btnOverlayUndo`, `btnOverlayContinue`.
   The old `successCheckmark`, `successTitle`, `btnOverlayOk` IDs are
   removed.
2. **`drawable/check_badge_bg.xml`** — new. 56dp `accent_soft` oval.
3. **`drawable/ic_undo.xml`** — verify exists; if missing add a
   standard 24dp rotate-left arrow in `ink`.
4. **`ConfettiView.kt`** — new. `View` subclass that takes a `Long` seed
   and draws 22 pieces at deterministic positions using `Random(seed)`.
   Colors pulled from a palette array; shape (rect vs circle) and
   rotation also seeded. `onDraw` only — no animation.
5. **`UndoProgressRing.kt`** — new. `View` subclass rendering an arc
   around the undo icon. Exposes a `progress: Float` property
   ([0f, 1f], 1f = full ring). Drawn as a 2dp stroke, `accent` color,
   on a `line` track. No self-animation; `MainActivity` drives it via
   `ValueAnimator`.
6. **`MainActivity.showTrashSuccessSnackbar()` rewrite** — rename to
   `showTrashSuccessCard()` (function still called from
   `trashRequestLauncher`'s success callback). Removes the Snackbar
   construction. Wires:
   - Hero text to formatted size
   - Subtitle to pluralized count
   - Confetti seed to `pendingTrashSize`
   - 7000ms `ValueAnimator` driving `UndoProgressRing.progress` 1f→0f
   - `btnOverlayUndo` click → cancel animator + `performRestore()` +
     dismiss card
   - `btnOverlayContinue` click → cancel animator + dismiss card
   - Animator end → dismiss card (commit — no restore)
7. **`dismissSuccessOverlay()`** — keep the status-bar restore + fade-out
   animation. Just works with the new overlay children.
8. **String updates**:
   - Add `freed_subtitle_plural` = plural: `freed · %1$d item moved to trash`
     / `freed · %1$d items moved to trash`.
   - Add `undo` — already exists, reuse.
   - Add `continue_action` = "Continue" (or reuse an existing string
     if present — verify).
   - Remove: `delete_success_title`, `overlay_ok`, `space_saved`
     (snackbar copy), *after* confirming no other callers reference them
     (`grep`).

## Why a card not a fullscreen

The current fullscreen check-mark overlay is moment-of-triumph UI for a
task where the user mostly wants to keep going. A card keeps the grid
visible behind the scrim (faded), so the user's sense of place is
preserved. The hero size shift ("789 kB") redirects the celebration
from *the act of deleting* to *the storage freed*, which is the actual
value the app provides.

## Not in scope

- Backdrop blur (needs API 31+; minSdk stays at 26 for now).
- Replacing the Snackbar on the pre-Android-11 path. That whole path
  goes away when minSdk bumps to 30 (SDD-20260419-001); touching it
  now is churn.
- Showing per-item thumbnails inside the card ("here's what you deleted").
  Out of scope — hero-size framing is the point.
- Localized plural handling beyond Android's standard `plurals.xml`
  system (existing language coverage applies).
- Haptic feedback on ring complete / Undo. Defer.

## Files changed

| File | Change |
|------|--------|
| `res/layout/activity_main.xml` | Overlay `FrameLayout` kept; inner `LinearLayout` replaced with confetti layer + `MaterialCardView`. Old IDs (`successCheckmark`, `successTitle`, `btnOverlayOk`) removed. |
| `res/drawable/check_badge_bg.xml` | **New** — 56dp `accent_soft` oval |
| `res/drawable/ic_undo.xml` | **New or verify** — 24dp undo arrow in ink |
| `res/values/strings.xml` | Add `freed_subtitle_plural` (plural), `continue_action`; remove `delete_success_title`, `overlay_ok`, `space_saved` after confirming no other callers |
| `res/values/strings.xml` (plurals.xml if present) | Add plural for freed subtitle |
| `ConfettiView.kt` | **New** — 60-piece falling-confetti animation, fresh random seed per call |
| `UndoProgressRing.kt` | **New** — custom View rendering a draining arc |
| `MainActivity.kt` | `showTrashSuccessSnackbar()` → `showTrashSuccessCard()`; wire card + animator; remove Snackbar construction on Android-11+ path; update binding references |
| `app/src/test/…/ConfettiViewTest.kt` | **New** — covers: same seed → same positions, different seeds → different positions, count = PIECE_COUNT, upper-band start position |
| `res/values/colors.xml` | Added `card_scrim` (#4D000000, 30% black) — lighter than the existing `overlay_bg` scrim |

## Acceptance criteria

- [x] After deleting one or more items on Android 11+, a centered white
      card appears over the dimmed grid. No bottom Snackbar.
- [x] Card shows a 56dp accent-soft circle with an accent check, then
      the freed size in mono display type (e.g. `789 kB`), then
      `freed · N items moved to trash`.
- [x] Two buttons inside the card: `Undo` (outline) and `Continue`
      (ink-filled). Undo has a visible progress ring around its leading
      undo icon.
- [x] The progress ring drains smoothly over ~7 seconds; when it hits
      zero the card auto-dismisses (commit, no restore).
- [x] Tapping `Continue` dismisses the card immediately and commits.
      The status bar returns to light.
- [x] Tapping `Undo` restores the trashed items via
      `createTrashRequest(isTrashed=false)` and dismisses the card.
- [x] Tapping the dark scrim outside the card does **not** dismiss.
- [x] Confetti is visible behind the card — 60 pieces falling from the
      top with gravity, sideways drift, and tumble rotation. Fresh
      random pattern every time the card opens; animation auto-stops
      when all pieces leave the canvas.
- [x] No reference to `successCheckmark`, `successTitle`, `btnOverlayOk`,
      `space_saved`, or `delete_success_title` remains in code or
      layouts.
- [x] `./gradlew clean assembleDebug testDebugUnitTest lint` succeeds.
- [x] `ConfettiViewTest` covers ≥ 3 cases (determinism, count,
      seed-variation).

## Task breakdown

- **[task-01-delete-success.md](task-01-delete-success.md)** — the full
  piece. Layout rebuild, two new custom views, animator wiring, string
  cleanup, tests. ~10 steps.

Recommended order: new-file work first (drawables, custom views, tests),
then the layout rebuild, then `MainActivity` wiring last so the compile
errors guide you through what still needs replacing.
