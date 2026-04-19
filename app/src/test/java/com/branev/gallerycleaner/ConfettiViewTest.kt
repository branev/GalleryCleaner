package com.branev.gallerycleaner

import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfettiViewTest {

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
            ribbonPalette = ribbonPalette,
            discPalette = discPalette,
            shapeWeights = shapeWeights,
        )
        val b = ConfettiLayout.pieces(
            cardLeft = 40f, cardTop = 300f, cardWidth = 800f,
            seed = 12345L, density = 2f,
            ribbonPalette = ribbonPalette,
            discPalette = discPalette,
            shapeWeights = shapeWeights,
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
        val leftCount = pieces.count { it.x == originLeftX }
        val rightCount = pieces.count { it.x == originRightX }
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
            assertTrue(
                "born=${p.born} should be in [0, 0.12]",
                p.born in 0f..0.12f,
            )
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
