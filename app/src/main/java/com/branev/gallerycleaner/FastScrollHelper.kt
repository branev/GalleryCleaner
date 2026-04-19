package com.branev.gallerycleaner

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Manages fast-scroll behavior: thumb positioning, drag handling,
 * date tooltip display, and auto-hide.
 */
class FastScrollHelper(
    private val recyclerView: RecyclerView,
    private val track: View,
    private val thumb: View,
    private val tooltip: TextView,
    private val getDateAtPosition: (Int) -> String,
    private val onFastScrollPositionChanged: (Int) -> Unit = {}
) {
    private var isDragging = false
    private val hideRunnable = Runnable { hide() }
    private var scrollListener: RecyclerView.OnScrollListener? = null

    companion object {
        private const val SHOW_DURATION_MS = 1500L
        private const val FADE_DURATION_MS = 200L
    }

    @SuppressLint("ClickableViewAccessibility")
    fun attach() {
        scrollListener = object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (!isDragging && dy != 0) {
                    updateThumbPosition()
                    show()
                    scheduleHide()
                }
            }
        }
        recyclerView.addOnScrollListener(scrollListener!!)

        thumb.setOnTouchListener { _, event ->
            handleThumbTouch(event)
            true
        }

        track.setOnTouchListener { _, event ->
            handleThumbTouch(event)
            true
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun detach() {
        scrollListener?.let { recyclerView.removeOnScrollListener(it) }
        scrollListener = null
        thumb.setOnTouchListener(null)
        track.setOnTouchListener(null)
        thumb.handler?.removeCallbacks(hideRunnable)
    }

    private fun handleThumbTouch(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isDragging = true
                thumb.handler?.removeCallbacks(hideRunnable)
                showTooltip()
                scrollToY(event.rawY)
            }
            MotionEvent.ACTION_MOVE -> {
                scrollToY(event.rawY)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                hideTooltip()
                scheduleHide()
            }
        }
    }

    private fun scrollToY(rawY: Float) {
        val trackLocation = IntArray(2)
        track.getLocationOnScreen(trackLocation)
        val trackTop = trackLocation[1].toFloat()
        val trackHeight = track.height.toFloat()

        if (trackHeight <= 0) return

        val relativeY = (rawY - trackTop).coerceIn(0f, trackHeight)
        val proportion = relativeY / trackHeight

        val layoutManager = recyclerView.layoutManager as? GridLayoutManager ?: return
        val itemCount = recyclerView.adapter?.itemCount ?: return
        if (itemCount == 0) return

        val targetPosition = (proportion * (itemCount - 1)).toInt().coerceIn(0, itemCount - 1)

        layoutManager.scrollToPositionWithOffset(targetPosition, 0)

        updateThumbPositionFromProportion(proportion)

        tooltip.text = getDateAtPosition(targetPosition)

        // Notify that items up to this position have been scrolled past
        onFastScrollPositionChanged(targetPosition)
    }

    fun updateThumbPosition() {
        val layoutManager = recyclerView.layoutManager as? GridLayoutManager ?: return
        val itemCount = recyclerView.adapter?.itemCount ?: return
        if (itemCount == 0) return

        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        if (firstVisible == RecyclerView.NO_POSITION) return

        val proportion = firstVisible.toFloat() / itemCount.toFloat()
        updateThumbPositionFromProportion(proportion)
    }

    private fun updateThumbPositionFromProportion(proportion: Float) {
        val trackHeight = track.height.toFloat()
        val thumbHeight = thumb.height.toFloat()
        if (trackHeight <= 0) return

        val maxTranslation = trackHeight - thumbHeight
        val translation = (proportion * maxTranslation).coerceIn(0f, maxTranslation)

        thumb.translationY = translation
        tooltip.translationY = translation
    }

    private fun show() {
        track.animate().alpha(1f).setDuration(FADE_DURATION_MS).start()
        thumb.animate().alpha(1f).setDuration(FADE_DURATION_MS).start()
    }

    private fun hide() {
        if (isDragging) return
        track.animate().alpha(0f).setDuration(FADE_DURATION_MS).start()
        thumb.animate().alpha(0f).setDuration(FADE_DURATION_MS).start()
    }

    private fun showTooltip() {
        show()
        tooltip.animate().alpha(1f).setDuration(FADE_DURATION_MS).start()
    }

    private fun hideTooltip() {
        tooltip.animate().alpha(0f).setDuration(FADE_DURATION_MS).start()
    }

    private fun scheduleHide() {
        thumb.handler?.removeCallbacks(hideRunnable)
        thumb.handler?.postDelayed(hideRunnable, SHOW_DURATION_MS)
    }
}
