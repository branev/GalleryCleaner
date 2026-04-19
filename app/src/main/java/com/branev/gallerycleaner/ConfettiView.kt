package com.branev.gallerycleaner

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
        internal const val FAST_LAUNCH_COUNT = 15

        // Physics constants (dp-derived where noted; multiplied by density at runtime)
        private const val DRAG_LINEAR = 1.8f    // /s
        private const val DRAG_ANGULAR = 1.2f   // /s
        private const val GRAVITY_DP = 720f     // dp/s²
        private const val TURB_AMP_DP = 40f     // dp/s²
        private const val WHOOSH_DP = 180f      // dp/s² extra upward on fastest, first 60ms
        private const val WHOOSH_WINDOW_S = 0.06f
        private const val TOTAL_LIFE_S = 2.5f
        private const val FADE_IN_S = 0.08f
        private const val FADE_OUT_S = 0.4f
        private const val DT_CLAMP = 0.05f      // cap per-frame dt so a dropped frame doesn't explode physics
        private const val KILL_BELOW_PAD_DP = 20f
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Shape distribution
    private val shapeWeights = listOf(
        0.55f to ConfettiShape.RIBBON,
        0.25f to ConfettiShape.STREAMER,
        0.20f to ConfettiShape.DISC,
    )

    // Color palettes resolved lazily from theme
    private val ribbonPalette: List<Pair<Float, Int>> by lazy {
        listOf(
            0.32f to ContextCompat.getColor(context, R.color.accent),
            0.32f to ContextCompat.getColor(context, R.color.accent_soft),
            0.18f to ContextCompat.getColor(context, R.color.ink),
            0.18f to ContextCompat.getColor(context, R.color.success),
        )
    }
    // Discs skip ink — black dots read as debris on the white card
    private val discPalette: List<Pair<Float, Int>> by lazy {
        listOf(
            0.32f to ContextCompat.getColor(context, R.color.accent),
            0.32f to ContextCompat.getColor(context, R.color.accent_soft),
            0.18f to ContextCompat.getColor(context, R.color.success),
        )
    }

    private var pieces: MutableList<Piece> = mutableListOf()
    private var startTimeNs: Long = 0L
    private var lastFrameNs: Long = 0L
    private var animating: Boolean = false
    private val cardBounds: Rect = Rect()

    /**
     * Starts the confetti effect. [cardRect] is the card's bounds in
     * this view's coordinate space — used to place the two emission
     * origins (18% and 82% of card width, 8dp above cardTop).
     */
    fun start(seed: Long, cardRect: Rect) {
        cardBounds.set(cardRect)
        pieces = ConfettiLayout.pieces(
            cardLeft = cardBounds.left.toFloat(),
            cardTop = cardBounds.top.toFloat(),
            cardWidth = cardBounds.width().toFloat(),
            seed = seed,
            density = resources.displayMetrics.density,
            ribbonPalette = ribbonPalette,
            discPalette = discPalette,
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
                // Pending spawn — still within effect window.
                anyVisible = true
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

            // Alpha curve: ease-in at spawn, hold, ease-out at end
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

                // Edge-on width illusion: scale width by how perpendicular
                // the rotation is to the travel direction. 0 = edge-on, 1 = face-on.
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
    val sizeLong: Float,           // dp; drawPiece multiplies by density
    val color: Int,
    val phase: Float,              // 0..2π turbulence phase
    val massFactor: Float,         // 0.85..1.15
    val turbOmega: Float,          // 2.2..3.4 rad/s
    val born: Float,               // seconds offset from start (0..0.12)
    val isFastLaunch: Boolean,
)

/**
 * Pure deterministic piece generator. Seeded for testability.
 * Coordinates returned in pixels (density already applied).
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
        val raw = mutableListOf<Piece>()

        repeat(count) { i ->
            val leftSide = i < half
            val ox = if (leftSide) originLeftX else originRightX

            // Speed 460..680 dp/s → px/s
            val speed = (460f + rng.nextFloat() * 220f) * density

            // Emission angle from +x axis (mathematical convention — 90° = up).
            // Left fountain 60°–110°, right 70°–120°.
            val angleDeg = if (leftSide) 60f + rng.nextFloat() * 50f
            else 70f + rng.nextFloat() * 50f
            val angleRad = Math.toRadians(angleDeg.toDouble()).toFloat()
            val vx = speed * cos(angleRad)
            val vy = -speed * sin(angleRad)   // screen Y grows downward → flip

            val shape = pickWeighted(rng, shapeWeights)
            val palette = if (shape == ConfettiShape.DISC) discPalette else ribbonPalette
            val color = pickWeighted(rng, palette)
            val sizeLong = when (shape) {
                ConfettiShape.RIBBON -> 7f + rng.nextFloat() * 6f    // 7..13
                ConfettiShape.STREAMER -> 14f + rng.nextFloat() * 8f // 14..22
                ConfettiShape.DISC -> 3f + rng.nextFloat() * 2f      // 3..5
            }
            val born = rng.nextFloat() * 0.12f
            val phase = rng.nextFloat() * (Math.PI.toFloat() * 2f)
            val massFactor = 0.85f + rng.nextFloat() * 0.3f
            val turbOmega = 2.2f + rng.nextFloat() * 1.2f
            val vrot = (rng.nextFloat() * 2f - 1f) * 9.4f // ±540°/s ≈ ±9.4 rad/s
            val rot = rng.nextFloat() * (Math.PI.toFloat() * 2f)

            raw.add(
                Piece(
                    x = ox, y = originY, vx = vx, vy = vy,
                    rot = rot, vrot = vrot,
                    shape = shape, sizeLong = sizeLong, color = color,
                    phase = phase, massFactor = massFactor, turbOmega = turbOmega,
                    born = born, isFastLaunch = false,
                )
            )
        }

        // Mark the fastest 15 as fast-launch (for the whoosh extra).
        val fastIndices = raw
            .withIndex()
            .sortedByDescending { (_, p) -> p.vx * p.vx + p.vy * p.vy }
            .take(ConfettiView.FAST_LAUNCH_COUNT)
            .map { it.index }
            .toSet()
        return raw.mapIndexed { i, p ->
            if (i in fastIndices) p.copy(isFastLaunch = true) else p
        }.toMutableList()
    }

    private fun <T> pickWeighted(rng: Random, weighted: List<Pair<Float, T>>): T {
        val total = weighted.fold(0f) { acc, (w, _) -> acc + w }
        var roll = rng.nextFloat() * total
        for ((w, v) in weighted) {
            roll -= w
            if (roll <= 0f) return v
        }
        return weighted.last().second
    }
}
