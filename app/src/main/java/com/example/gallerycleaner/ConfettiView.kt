package com.example.gallerycleaner

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.random.Random

class ConfettiView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    companion object {
        const val PIECE_COUNT = 60
        private const val GRAVITY_DP_PER_S2 = 900f
        private const val MAX_LIFE_S = 5f
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val palette = intArrayOf(
        ContextCompat.getColor(context, R.color.accent),
        ContextCompat.getColor(context, R.color.ink),
        ContextCompat.getColor(context, R.color.accent_soft),
        ContextCompat.getColor(context, R.color.success),
    )

    private var seed: Long = 0L
    private var pieces: List<ConfettiLayout.Piece> = emptyList()
    private var startTimeNs: Long = 0L
    private var animating: Boolean = false

    /**
     * Regenerates the confetti layout with the given seed and starts the
     * falling animation. Pass a different seed each call (e.g. System.nanoTime())
     * to get a fresh pattern every time.
     */
    fun start(seed: Long) {
        this.seed = seed
        regenerate()
        startTimeNs = System.nanoTime()
        animating = true
        postInvalidateOnAnimation()
    }

    fun stop() {
        animating = false
        pieces = emptyList()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (animating) regenerate()
    }

    private fun regenerate() {
        val density = resources.displayMetrics.density
        pieces = ConfettiLayout.pieces(width, height, seed, density, palette.size)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!animating || pieces.isEmpty()) return

        val density = resources.displayMetrics.density
        val gravity = GRAVITY_DP_PER_S2 * density
        val elapsed = (System.nanoTime() - startTimeNs) / 1_000_000_000f

        var anyVisible = false
        for (p in pieces) {
            val x = p.x0 + p.vx * elapsed
            val y = p.y0 + p.vy0 * elapsed + 0.5f * gravity * elapsed * elapsed
            if (y - p.size > height) continue
            anyVisible = true

            paint.color = palette[p.colorIndex]
            val half = p.size / 2f
            val rot = p.rot0 + p.rotV * elapsed
            if (p.isRect) {
                canvas.save()
                canvas.rotate(rot, x, y)
                canvas.drawRect(
                    RectF(x - half, y - half * 0.45f, x + half, y + half * 0.45f),
                    paint,
                )
                canvas.restore()
            } else {
                canvas.drawCircle(x, y, half * 0.6f, paint)
            }
        }

        if (anyVisible && elapsed < MAX_LIFE_S) {
            postInvalidateOnAnimation()
        } else {
            animating = false
        }
    }
}

/**
 * Pure deterministic layout helper so piece generation is unit-testable
 * without instantiating a View (which needs a Context / Resources).
 */
object ConfettiLayout {
    data class Piece(
        val x0: Float,
        val y0: Float,
        val vx: Float,
        val vy0: Float,
        val size: Float,
        val rot0: Float,
        val rotV: Float,
        val isRect: Boolean,
        val colorIndex: Int,
    )

    fun pieces(
        w: Int,
        h: Int,
        seed: Long,
        density: Float = 1f,
        paletteSize: Int = 4,
        count: Int = ConfettiView.PIECE_COUNT,
    ): List<Piece> {
        if (w <= 0 || h <= 0) return emptyList()
        val rng = Random(seed)
        val minSize = density * 6f
        val maxSize = density * 14f
        return List(count) {
            Piece(
                // Scatter starts across full width, upper 30% of canvas.
                x0 = rng.nextFloat() * w,
                y0 = rng.nextFloat() * h * 0.3f,
                // Sideways drift: -120..120 dp/s
                vx = (rng.nextFloat() * 2f - 1f) * 120f * density,
                // Initial downward kick: 40..220 dp/s
                vy0 = (40f + rng.nextFloat() * 180f) * density,
                size = minSize + rng.nextFloat() * (maxSize - minSize),
                rot0 = rng.nextFloat() * 360f,
                // Tumble: -360..360 deg/s
                rotV = (rng.nextFloat() * 2f - 1f) * 360f,
                isRect = rng.nextBoolean(),
                colorIndex = rng.nextInt(paletteSize),
            )
        }
    }
}
