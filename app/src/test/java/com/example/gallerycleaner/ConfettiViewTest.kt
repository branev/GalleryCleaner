package com.example.gallerycleaner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ConfettiViewTest {

    @Test
    fun `same seed produces same pieces`() {
        val a = ConfettiLayout.pieces(400, 600, 12345L)
        val b = ConfettiLayout.pieces(400, 600, 12345L)
        assertEquals(a, b)
    }

    @Test
    fun `different seeds produce different pieces`() {
        val a = ConfettiLayout.pieces(400, 600, 1L)
        val b = ConfettiLayout.pieces(400, 600, 2L)
        assertNotEquals(a, b)
    }

    @Test
    fun `piece count matches PIECE_COUNT`() {
        val pieces = ConfettiLayout.pieces(400, 600, 42L)
        assertEquals(ConfettiView.PIECE_COUNT, pieces.size)
    }

    @Test
    fun `zero-dimension canvas returns empty`() {
        assertEquals(emptyList<ConfettiLayout.Piece>(), ConfettiLayout.pieces(0, 600, 42L))
        assertEquals(emptyList<ConfettiLayout.Piece>(), ConfettiLayout.pieces(400, 0, 42L))
    }

    @Test
    fun `initial positions fall within upper canvas band`() {
        val w = 400
        val h = 600
        val pieces = ConfettiLayout.pieces(w, h, 99L)
        pieces.forEach {
            assert(it.x0 in 0f..w.toFloat()) { "x0 ${it.x0} out of [0, $w]" }
            assert(it.y0 in 0f..(h * 0.3f)) { "y0 ${it.y0} should be in upper 30% of canvas" }
        }
    }
}
