package com.branev.gallerycleaner

import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import android.text.style.MetricAffectingSpan

/** Applies an actual Typeface to a text span, API-21 safe. */
class CustomTypefaceSpan(private val typeface: Typeface) : MetricAffectingSpan() {
    override fun updateDrawState(ds: TextPaint) = apply(ds)
    override fun updateMeasureState(paint: TextPaint) = apply(paint)
    private fun apply(paint: Paint) {
        paint.typeface = typeface
    }
}
