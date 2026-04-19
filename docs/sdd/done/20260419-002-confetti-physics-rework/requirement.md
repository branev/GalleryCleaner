# SDD-20260419-002: Confetti Physics Rework

**Depends on:** SDD-20260418-010 ✓ (Delete Success — established the
`ConfettiView` + `ConfettiLayout` surface this SDD replaces the guts of)

## Summary

Replace the current "drifting rain" confetti in the delete-success card
with a realistic **pop-and-fall** effect per Claude Design's follow-up
specification. Two fountains emit from the top of the card over 120ms,
cross near the card's headline, drift with linear drag + turbulence,
flutter as edge-on ribbons, and fade out at end-of-life.

The visual gap today: peak velocity is 220 dp/s downward, which reads
as "gentle rain." The design's prescribed peak is ~3× that (460–680
dp/s), launched *upward* from two origins near the card's top edge —
which reads as "pop."

Source: Claude Design's reply to the prompt we drafted mid-session.
The SDD embeds the relevant numbers below; task-01 translates them
directly into Kotlin.

## Design source — physics spec

### 1. Pop — two spawn origins

- **Origins**: two points above the card's top edge.
  - `origin₁` at `x = 18% of card width`, `y = cardTop − 8dp`
  - `origin₂` at `x = 82% of card width`, `y = cardTop − 8dp`
- **Piece count**: 60 total — 30 per origin.
- **Emission window**: staggered spawn over `0–120ms` (not all at `t=0`).
- **Initial velocity** (per piece):
  - Speed magnitude: `460–680 dp/s` uniform random.
  - Angle from +x axis:
    - `origin₁` (left): `60°–110°` (up and slightly right)
    - `origin₂` (right): `70°–120°` (up and slightly left)
  - The two fountains intersect over the card center → reads as "pop"
    not "radial burst."

### 2. Fall curve — drag + turbulence

- **Linear air drag (per axis)**: `v -= v * k * dt`, `k = 1.8 /s`.
- **Gravity**: `720 dp/s²` (down from today's 900 — drag makes old
  gravity feel heavy).
- **Horizontal turbulence**:
  `vx += sin(elapsed * turbω + piece.phase) * turbAmp * dt`
  with `turbω ∈ [2.2, 3.4] rad/s` per-piece random, `turbAmp = 40 dp/s²`.
- **Edge-on ribbon scale** — the biggest visual win:
  `widthScale = 0.2 + 0.8 * |cos(rotation − atan2(vy, vx))|`.
  Apply as `canvas.scale(widthScale, 1f, px, py)` when drawing
  rectangles only. Sells the "3D ribbon" illusion.
- **Angular drag**: initial `±540°/s` (≈ ±9.4 rad/s), damped with
  `kω = 1.2 /s`.

### 3. Shape variety — three weighted shapes

| Shape | Weight | Size |
|---|---|---|
| Ribbon (rect) | 55% | 7–13dp long, aspect 0.38 |
| Streamer (narrow rect) | 25% | 14–22dp long, aspect 0.18 |
| Disc | 20% | 3–5dp radius |

(Dropped: today's 0.45-aspect "squarish" rectangles — read as debris.)

### 4. Color weighting

Equal split today is replaced with 2:2:1:1:

| Color | Weight |
|---|---|
| `accent` `#E85A3D` | 32% |
| `accent_soft` `#FBE0D5` | 32% |
| `ink` `#1D1710` | 18% |
| `success` `#2E7D5B` | 18% |

**Per-shape rule**: discs skip `ink` (black dots read as debris / fly
specks on a white card). Ribbons and streamers use all four.

### 5. Timing

- **Total effect life**: `2200ms` visible + `300ms` fade tail =
  `2500ms` total.
- **Sync**: pop peaks **before** the card's fade-in finishes. Card
  fade is ~180ms; confetti spawn window is 0–120ms — so the card
  feels *caught by* the confetti, not decorated after.
- **Per-piece alpha curve**:
  - `0–80ms`: ease-in `0 → 1`
  - `80ms → (life − 400ms)`: `α = 1`
  - last `400ms`: ease-out `1 → 0`
- **Hard kill**: `piece.y > canvasHeight + 20dp` OR `life > 2500ms`,
  whichever first.

### 6. Extras (picked: A + B)

- **A. Upward "whoosh" acceleration** — for the first `60ms`, apply an
  extra `+180 dp/s²` upward beyond gravity on the **15 fastest** pieces.
  Creates a visible fountain peak even with drag present.
- **B. Per-piece mass jitter** — effective gravity multiplied by
  `0.85 + random * 0.3`. Heavier pieces fall faster; you get a
  staggered descent instead of a flat wall.
- **NOT picked: C. Global wind.** Would need the card to react too or
  it reads artificial; deferred.

### 7. Piece data class (target shape)

```kotlin
enum class Shape { RIBBON, STREAMER, DISC }

data class Piece(
    var x: Float, var y: Float,          // dp
    var vx: Float, var vy: Float,        // dp/s
    var rot: Float,                      // radians
    var vrot: Float,                     // rad/s
    val shape: Shape,
    val sizeLong: Float,                 // 7–13 ribbon, 14–22 streamer, 3–5 disc radius
    val color: Int,
    val phase: Float,                    // 0..2π for turbulence phase
    val massFactor: Float,               // 0.85..1.15
    val turbOmega: Float,                // per-piece turbulence frequency
    val born: Float,                     // spawn time in seconds (0..0.120)
    val isFastLaunch: Boolean,           // top-15 by speed, for whoosh extra
)
```

### 8. Per-frame integration (target)

```kotlin
val k = 1.8f          // linear drag
val kOmega = 1.2f     // angular drag
val gravity = 720f    // dp/s²
val turbAmp = 40f

val dt = frameDtSeconds
for (p in pieces) {
    val age = elapsed - p.born
    if (age < 0f) continue  // not yet spawned

    p.vx += -p.vx * k * dt
    p.vy += -p.vy * k * dt
    p.vx += sin(elapsed * p.turbOmega + p.phase) * turbAmp * dt
    p.vy += gravity * p.massFactor * dt
    if (age < 0.06f && p.isFastLaunch) p.vy -= 180f * dt

    p.x += p.vx * dt
    p.y += p.vy * dt
    p.rot += p.vrot * dt
    p.vrot += -p.vrot * kOmega * dt
}
```

## Scope

1. **Rewrite `ConfettiView.kt`** — new `Shape` enum, new `Piece` data
   class matching §7, new `start(seed, cardBounds)` signature that
   takes the card's screen-space rect so origins can be placed
   relative to it.
2. **Rewrite `ConfettiLayout.pieces(...)`** — two-origin emission with
   staggered spawn times; weighted shape + color picks from the tables
   above.
3. **Rewrite `onDraw`** — new integration loop (drag, turbulence,
   gravity, whoosh, angular drag), per-piece alpha curve, edge-on
   ribbon `canvas.scale(widthScale, 1f)` trick, hard-kill conditions.
4. **Frame-time accounting** — today's loop uses `elapsed` derived
   from `startTimeNs` each frame and reintegrates positions from t=0
   (not quite, but effectively). Switch to an incremental `dt =
   (now − lastFrameNs) / 1e9` integration so turbulence and drag
   compose correctly frame-over-frame. Clamp `dt` to `0.05f` so a
   dropped frame doesn't blow up the physics.
5. **MainActivity wiring** — `binding.confettiLayer.start(seed,
   cardBounds)` where `cardBounds` is derived from
   `binding.successCard` after layout
   (`doOnLayout { ... getLocationOnScreen / view coords }`).
6. **`ConfettiViewTest.kt`** — update to the new `Piece` shape. Keep:
   `same seed → same pieces`, `different seeds → different`, `count =
   PIECE_COUNT`, `zero canvas → empty`. Add: `pieces split 30/30 between
   origins`, `shape distribution approximately matches weights on a
   large sample`.

## Not in scope

- **Global wind (extra C).** Deferred; card-wide response needed first.
- **Audio / haptics.** Visual-only change.
- **Changing the trigger.** Still fires from
  `MainActivity.showTrashSuccessCard` on a successful trash.
- **Varying confetti based on size deleted** (different effect for
  100 MB vs 500 KB). Possibly nice, not requested.
- **3D rotation** — the edge-on scale trick fakes it in 2D; a true
  `Camera.rotateX/Y` approach isn't worth the frame cost.

## Files changed

| File | Change |
|------|--------|
| `ConfettiView.kt` | Full rewrite: new Shape, Piece, integration loop, alpha fade, edge-on scale |
| `ConfettiViewTest.kt` | Updated for new Piece shape; new distribution tests |
| `MainActivity.kt` | `confettiLayer.start(seed, cardBounds)` after `successCard` lays out |
| `activity_main.xml` | No change — ConfettiView stays full-scrim |

## Acceptance criteria

- [x] Triggering a delete produces a visible upward **pop** — pieces
      visibly launch from two points above the card, cross over its
      headline, peak, then fall.
- [x] At least three visibly different shapes are present: wide
      ribbons, thin streamers, small discs.
- [x] Ribbons and streamers visibly appear to twist / go "edge-on"
      as they rotate (width shrinks/grows based on rotation angle
      vs travel direction).
- [x] Colors read brand-forward: orange and soft-pink dominate;
      ink and green accent; no black discs.
- [x] Total effect duration is ~2.5s from pop to last piece faded;
      alpha fades smoothly at end of life (no hard cut-off).
- [x] Heavier pieces visibly fall faster than lighter ones
      (staggered descent, not a flat wall).
- [x] `./gradlew clean assembleDebug testDebugUnitTest lint` succeeds.
- [x] `ConfettiViewTest` still covers the determinism + count +
      empty-canvas invariants; adds origin-split test.

## Task breakdown

- **[task-01-confetti-physics.md](task-01-confetti-physics.md)** —
  single task, ~7 steps: rewrite Piece + emission + integration +
  draw, update MainActivity wiring + card-bounds plumbing, update
  tests, build + lint + smoke.
