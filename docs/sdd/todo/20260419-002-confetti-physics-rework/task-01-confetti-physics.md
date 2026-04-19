# Task 01: Confetti Physics Rework

**Parent:** SDD-20260419-002 — Confetti Physics Rework

## What You're Changing

`ConfettiView` currently sprays 60 pieces downward from the upper
30% of the view with a weak downward kick and constant gravity. The
effect reads as "rain." This task replaces the guts with a two-origin
fountain that pops upward, flutters with drag + turbulence, and fades
out — matching Claude Design's physics spec.

## Before vs After

| Aspect | Before | After |
|---|---|---|
| Emission | Whole view, random x/y in upper 30% | Two fountains at card-relative `x=18%` and `x=82%`, `y = cardTop−8dp` |
| Emission timing | All at `t=0` | Staggered `0–120ms` (30 from each origin) |
| Initial speed | `40–220 dp/s` downward | `460–680 dp/s` outward-upward |
| Initial angle | None (downward) | Left fountain `60°–110°`; right fountain `70°–120°` |
| Gravity | `900 dp/s²` | `720 dp/s²` (offset by new drag) |
| Drag | None | Linear `v -= v*k*dt`, `k=1.8`, plus angular `kω=1.2` |
| Turbulence | None | `vx += sin(elapsed*turbω + phase) * 40 * dt`, `turbω` per-piece 2.2–3.4 |
| Shapes | Rect + disc, 50/50 | Ribbon 55% / Streamer 25% / Disc 20% |
| Ribbon 3D illusion | None | `canvas.scale(widthScale, 1f)` with `widthScale = 0.2 + 0.8 * |cos(rot − travelAngle)|` |
| Colors | Equal 4-way | Weighted 32/32/18/18 (`accent`/`accent_soft`/`ink`/`success`); discs skip ink |
| Alpha | Opaque full life | Ease-in 0–80ms, opaque middle, ease-out last 400ms |
| Total duration | ~5s | 2.5s (2.2s visible + 0.3s fade) |
| Extras | None | Upward whoosh on 15 fastest (first 60ms) + per-piece mass jitter |
| Integration | Reintegrate from `t=0` each frame | Incremental `dt = (now − lastFrame) / 1e9` (clamped to 0.05) |

## Prerequisites

- SDD-010 merged — `ConfettiView` + `ConfettiLayout` + `MainActivity`
  trigger already in place.
- `./gradlew --stop` before you start.
- Files to understand first:
  - [ConfettiView.kt](../../../../app/src/main/java/com/example/gallerycleaner/ConfettiView.kt) — current implementation
  - [ConfettiViewTest.kt](../../../../app/src/test/java/com/example/gallerycleaner/ConfettiViewTest.kt) — current invariants
  - [MainActivity.kt showTrashSuccessCard](../../../../app/src/main/java/com/example/gallerycleaner/MainActivity.kt) — where confetti starts; look for `binding.confettiLayer.start(...)`

## Step-by-Step Instructions

### Step 1 — Full rewrite of `ConfettiView.kt`

Replace the file entirely. Key shape:

```kotlin
package com.example.gallerycleaner

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class ConfettiShape { RIBBON, STREAMER, DISC }

class ConfettiView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    companion object {
        const val PIECE_COUNT = 60
        private const val FAST_LAUNCH_COUNT = 15

        // Physics constants (in dp or dp-derived units)
        private const val DRAG_LINEAR = 1.8f   // /s
        private const val DRAG_ANGULAR = 1.2f  // /s
        private const val GRAVITY_DP = 720f    // dp/s²
        private const val TURB_AMP_DP = 40f    // dp/s²
        private const val WHOOSH_DP = 180f     // dp/s², first 60ms on 15 fastest
        private const val WHOOSH_WINDOW_S = 0.06f
        private const val TOTAL_LIFE_S = 2.5f
        private const val FADE_IN_S = 0.08f
        private const val FADE_OUT_S = 0.4f
        private const val DT_CLAMP = 0.05f     // max dt per frame
        private const val KILL_BELOW_PAD_DP = 20f
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Weighted palettes: (weight, colorResId).
    private val ribbonPalette = listOf(
        0.32f to R.color.accent,
        0.32f to R.color.accent_soft,
        0.18f to R.color.ink,
        0.18f to R.color.success,
    )
    private val discPalette = listOf(
        // discs skip ink; re-normalize remaining three to sum 1
        0.32f / 0.82f to R.color.accent,
        0.32f / 0.82f to R.color.accent_soft,
        0.18f / 0.82f to R.color.success,
    )
    private val shapeWeights = listOf(
        0.55f to ConfettiShape.RIBBON,
        0.25f to ConfettiShape.STREAMER,
        0.20f to ConfettiShape.DISC,
    )

    private var pieces: MutableList<Piece> = mutableListOf()
    private var startTimeNs: Long = 0L
    private var lastFrameNs: Long = 0L
    private var animating: Boolean = false
    private var cardBounds: Rect = Rect()

    fun start(seed: Long, cardRect: Rect) {
        cardBounds.set(cardRect)
        pieces = ConfettiLayout.pieces(
            cardLeft = cardBounds.left.toFloat(),
            cardTop = cardBounds.top.toFloat(),
            cardWidth = cardBounds.width().toFloat(),
            seed = seed,
            density = resources.displayMetrics.density,
            ribbonPalette = ribbonPalette.map { (w, res) -> w to ContextCompat.getColor(context, res) },
            discPalette = discPalette.map { (w, res) -> w to ContextCompat.getColor(context, res) },
            shapeWeights = shapeWeights,
        )
        startTimeNs = System.nanoTime()
        lastFrameNs = startTimeNs
        animating = true
        postInvalidateOnAnimation()
    }

    fun stop() {
        animating = false
        pieces = mutableListOf()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!animating || pieces.isEmpty()) return

        val now = System.nanoTime()
        val dt = ((now - lastFrameNs) / 1_000_000_000f).coerceAtMost(DT_CLAMP)
        lastFrameNs = now
        val elapsed = (now - startTimeNs) / 1_000_000_000f

        if (elapsed > TOTAL_LIFE_S) {
            animating = false
            return
        }

        val density = resources.displayMetrics.density
        val gravityPx = GRAVITY_DP * density
        val turbAmpPx = TURB_AMP_DP * density
        val whooshPx = WHOOSH_DP * density
        val killY = height + KILL_BELOW_PAD_DP * density

        var anyVisible = false
        for (p in pieces) {
            val age = elapsed - p.born
            if (age < 0f) {
                anyVisible = true  // will appear later
                continue
            }
            if (age > TOTAL_LIFE_S) continue

            // Integrate
            p.vx += -p.vx * DRAG_LINEAR * dt
            p.vy += -p.vy * DRAG_LINEAR * dt
            p.vx += sin(elapsed * p.turbOmega + p.phase) * turbAmpPx * dt
            p.vy += gravityPx * p.massFactor * dt
            if (age < WHOOSH_WINDOW_S && p.isFastLaunch) {
                p.vy -= whooshPx * dt
            }
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.rot += p.vrot * dt
            p.vrot += -p.vrot * DRAG_ANGULAR * dt

            if (p.y > killY) continue
            anyVisible = true

            // Alpha curve
            val alpha = when {
                age < FADE_IN_S -> age / FADE_IN_S
                age > TOTAL_LIFE_S - FADE_OUT_S -> (TOTAL_LIFE_S - age) / FADE_OUT_S
                else -> 1f
            }.coerceIn(0f, 1f)

            paint.color = p.color
            paint.alpha = (alpha * 255f).toInt()

            drawPiece(canvas, p, density)
        }

        if (anyVisible && elapsed < TOTAL_LIFE_S) {
            postInvalidateOnAnimation()
        } else {
            animating = false
        }
    }

    private fun drawPiece(canvas: Canvas, p: Piece, density: Float) {
        when (p.shape) {
            ConfettiShape.DISC -> {
                canvas.drawCircle(p.x, p.y, p.sizeLong * density, paint)
            }
            ConfettiShape.RIBBON,
            ConfettiShape.STREAMER -> {
                val aspect = if (p.shape == ConfettiShape.RIBBON) 0.38f else 0.18f
                val halfLong = p.sizeLong * density / 2f
                val halfShort = halfLong * aspect

                // Edge-on width illusion: scale width based on rotation vs travel angle.
                val travelAngle = atan2(p.vy, p.vx)
                val edgeness = abs(cos(p.rot - travelAngle))
                val widthScale = 0.2f + 0.8f * edgeness

                canvas.save()
                canvas.rotate(Math.toDegrees(p.rot.toDouble()).toFloat(), p.x, p.y)
                canvas.scale(widthScale, 1f, p.x, p.y)
                canvas.drawRect(
                    RectF(p.x - halfLong, p.y - halfShort, p.x + halfLong, p.y + halfShort),
                    paint,
                )
                canvas.restore()
            }
        }
    }
}

data class Piece(
    var x: Float, var y: Float,
    var vx: Float, var vy: Float,
    var rot: Float,
    var vrot: Float,
    val shape: ConfettiShape,
    val sizeLong: Float,           // in dp; drawPiece scales by density
    val color: Int,
    val phase: Float,              // 0..2π turbulence phase
    val massFactor: Float,         // 0.85..1.15
    val turbOmega: Float,          // 2.2..3.4 rad/s
    val born: Float,               // seconds offset from start
    val isFastLaunch: Boolean,
)

/**
 * Pure, deterministic piece generator. Seeded for testability.
 * All coordinates returned in pixels (density already applied).
 */
object ConfettiLayout {

    fun pieces(
        cardLeft: Float,
        cardTop: Float,
        cardWidth: Float,
        seed: Long,
        density: Float,
        ribbonPalette: List<Pair<Float, Int>>,
        discPalette: List<Pair<Float, Int>>,
        shapeWeights: List<Pair<Float, ConfettiShape>>,
        count: Int = ConfettiView.PIECE_COUNT,
    ): MutableList<Piece> {
        if (cardWidth <= 0f) return mutableListOf()
        val rng = Random(seed)

        val originLeftX = cardLeft + 0.18f * cardWidth
        val originRightX = cardLeft + 0.82f * cardWidth
        val originY = cardTop - 8f * density

        val half = count / 2
        val rawPieces = mutableListOf<Piece>()

        // Two lists to track speed later for whoosh tagging
        repeat(count) { i ->
            val leftSide = i < half
            val ox = if (leftSide) originLeftX else originRightX
            val oy = originY

            // Speed 460..680 dp/s (convert to px/s)
            val speed = (460f + rng.nextFloat() * 220f) * density

            // Angle in degrees: left side 60..110, right 70..120.
            // 0° = +x, 90° = up (negative-y). We convert a "compass angle"
            // measured from +x to radians, then velocity = speed * (cos, -sin)
            // because screen-y grows downward.
            val angleDeg = if (leftSide) 60f + rng.nextFloat() * 50f
            else 70f + rng.nextFloat() * 50f
            val angleRad = Math.toRadians(angleDeg.toDouble()).toFloat()
            val vx = speed * cos(angleRad)
            val vy = -speed * sin(angleRad)  // up = negative

            val shape = pickWeighted(rng, shapeWeights)
            val palette = if (shape == ConfettiShape.DISC) discPalette else ribbonPalette
            val color = pickWeighted(rng, palette)
            val sizeLong = when (shape) {
                ConfettiShape.RIBBON -> 7f + rng.nextFloat() * 6f
                ConfettiShape.STREAMER -> 14f + rng.nextFloat() * 8f
                ConfettiShape.DISC -> 3f + rng.nextFloat() * 2f
            }
            val born = rng.nextFloat() * 0.12f  // 0..120ms
            val phase = rng.nextFloat() * (Math.PI.toFloat() * 2f)
            val massFactor = 0.85f + rng.nextFloat() * 0.3f
            val turbOmega = 2.2f + rng.nextFloat() * 1.2f
            val vrot = (rng.nextFloat() * 2f - 1f) * 9.4f  // ±9.4 rad/s = ±540°/s
            val rot = rng.nextFloat() * (Math.PI.toFloat() * 2f)

            rawPieces.add(
                Piece(
                    x = ox, y = oy, vx = vx, vy = vy,
                    rot = rot, vrot = vrot,
                    shape = shape, sizeLong = sizeLong, color = color,
                    phase = phase, massFactor = massFactor, turbOmega = turbOmega,
                    born = born, isFastLaunch = false,  // set below
                )
            )
        }

        // Mark top-15 by speed as fast-launch (for the whoosh extra).
        val sortedBySpeed = rawPieces.sortedByDescending { p ->
            p.vx * p.vx + p.vy * p.vy
        }
        val fastSet = sortedBySpeed.take(ConfettiView.FAST_LAUNCH_COUNT_INTERNAL).toSet()
        // Re-emit with isFastLaunch set correctly.
        return rawPieces.map { p ->
            if (p in fastSet) p.copy(isFastLaunch = true) else p
        }.toMutableList()
    }

    private fun <T> pickWeighted(rng: Random, weighted: List<Pair<Float, T>>): T {
        val total = weighted.sumOf { it.first.toDouble() }.toFloat()
        var roll = rng.nextFloat() * total
        for ((w, v) in weighted) {
            roll -= w
            if (roll <= 0f) return v
        }
        return weighted.last().second
    }
}
```

Note: `FAST_LAUNCH_COUNT_INTERNAL` — add a matching `internal` const
inside `ConfettiView`'s companion object alongside `FAST_LAUNCH_COUNT`
that exposes the value to `ConfettiLayout`:

```kotlin
internal const val FAST_LAUNCH_COUNT_INTERNAL = 15
```

### Step 2 — Expose card bounds from `MainActivity`

`binding.successCard` needs to be laid out before we can read its
position. In `showTrashSuccessCard`, replace the current
`binding.confettiLayer.start(System.nanoTime())` call with:

```kotlin
binding.successCard.doOnLayout { card ->
    val loc = IntArray(2)
    card.getLocationOnScreen(loc)
    val confettiLoc = IntArray(2)
    binding.confettiLayer.getLocationOnScreen(confettiLoc)
    // Card bounds in ConfettiView's coordinate space
    val left = loc[0] - confettiLoc[0]
    val top = loc[1] - confettiLoc[1]
    val rect = android.graphics.Rect(left, top, left + card.width, top + card.height)
    binding.confettiLayer.start(System.nanoTime(), rect)
}
```

Add the import: `import androidx.core.view.doOnLayout`.

### Step 3 — Update `ConfettiViewTest.kt`

The `Piece` shape changed — tests need to update. Full replacement:

```kotlin
package com.example.gallerycleaner

import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfettiViewTest {

    // Synthetic palettes — real colors don't matter for these tests.
    private val ribbonPalette = listOf(
        0.5f to Color.RED,
        0.5f to Color.BLUE,
    )
    private val discPalette = listOf(
        1.0f to Color.GREEN,
    )
    private val shapeWeights = listOf(
        0.55f to ConfettiShape.RIBBON,
        0.25f to ConfettiShape.STREAMER,
        0.20f to ConfettiShape.DISC,
    )

    @Test
    fun `same seed produces same pieces`() {
        val a = ConfettiLayout.pieces(
            cardLeft = 40f, cardTop = 300f, cardWidth = 800f,
            seed = 12345L, density = 2f,
            ribbonPalette, discPalette, shapeWeights,
        )
        val b = ConfettiLayout.pieces(
            cardLeft = 40f, cardTop = 300f, cardWidth = 800f,
            seed = 12345L, density = 2f,
            ribbonPalette, discPalette, shapeWeights,
        )
        assertEquals(a, b)
    }

    @Test
    fun `different seeds produce different pieces`() {
        val a = ConfettiLayout.pieces(
            40f, 300f, 800f, 1L, 2f,
            ribbonPalette, discPalette, shapeWeights,
        )
        val b = ConfettiLayout.pieces(
            40f, 300f, 800f, 2L, 2f,
            ribbonPalette, discPalette, shapeWeights,
        )
        assertNotEquals(a, b)
    }

    @Test
    fun `piece count matches PIECE_COUNT`() {
        val pieces = ConfettiLayout.pieces(
            40f, 300f, 800f, 42L, 2f,
            ribbonPalette, discPalette, shapeWeights,
        )
        assertEquals(ConfettiView.PIECE_COUNT, pieces.size)
    }

    @Test
    fun `zero card width returns empty`() {
        val pieces = ConfettiLayout.pieces(
            40f, 300f, 0f, 42L, 2f,
            ribbonPalette, discPalette, shapeWeights,
        )
        assertTrue(pieces.isEmpty())
    }

    @Test
    fun `pieces split evenly between left and right origins`() {
        val cardLeft = 40f
        val cardWidth = 800f
        val originLeftX = cardLeft + 0.18f * cardWidth
        val originRightX = cardLeft + 0.82f * cardWidth

        val pieces = ConfettiLayout.pieces(
            cardLeft, 300f, cardWidth, 7L, 2f,
            ribbonPalette, discPalette, shapeWeights,
        )
        val leftCount = pieces.count { p -> p.x == originLeftX }
        val rightCount = pieces.count { p -> p.x == originRightX }
        assertEquals(ConfettiView.PIECE_COUNT / 2, leftCount)
        assertEquals(ConfettiView.PIECE_COUNT / 2, rightCount)
    }

    @Test
    fun `born times fall within spawn window`() {
        val pieces = ConfettiLayout.pieces(
            40f, 300f, 800f, 9L, 2f,
            ribbonPalette, discPalette, shapeWeights,
        )
        pieces.forEach { p ->
            assertTrue("born=${p.born} should be in [0, 0.12]",
                p.born in 0f..0.12f)
        }
    }

    @Test
    fun `exactly 15 pieces are flagged fast-launch`() {
        val pieces = ConfettiLayout.pieces(
            40f, 300f, 800f, 11L, 2f,
            ribbonPalette, discPalette, shapeWeights,
        )
        assertEquals(15, pieces.count { it.isFastLaunch })
    }
}
```

### Step 4 — Build

```bash
./gradlew --stop
./gradlew assembleDebug testDebugUnitTest
```

Both green. Fix compiler errors as they surface — most likely your
`ConfettiView.start(Long)` old signature is called somewhere else;
the new signature is `start(Long, Rect)`. Update callers.

### Step 5 — Lint

```bash
./gradlew lint
```

### Step 6 — On-device smoke

```bash
./gradlew installDebug
```

1. Delete 1–2 items. Confetti pops upward from two points above the
   card, visibly crosses over the headline, peaks, then falls with
   drift.
2. Ribbons visibly flatten + widen as they rotate (edge-on trick).
3. Disc "flyspecks" are colored — no black dots.
4. Total effect ends cleanly ~2.5s after trigger; no pieces linger.
5. Retriggering produces a different pattern (random seed each time).

### Step 7 — Mark done + commit

1. Flip every `[ ]` to `[x]` on `requirement.md` acceptance list.
2. Move `docs/sdd/todo/20260419-002-confetti-physics-rework/` →
   `docs/sdd/done/20260419-002-confetti-physics-rework/`.
3. Update `STATUS.md` — Completed row, Tasks 1/1, short summary:
   `Two-origin fountain pop (460–680 dp/s up), linear drag + turbulence,
    edge-on ribbon illusion, weighted 3-shape / 4-color distribution,
    2.5s life with ease-in/out alpha, whoosh + mass jitter.`
4. **Atomic commits**: implementation + doc-done.
