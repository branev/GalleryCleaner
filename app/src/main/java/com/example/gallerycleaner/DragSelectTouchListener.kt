package com.example.gallerycleaner

import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView

/**
 * Handles drag-to-select on a RecyclerView grid.
 * After activation, tracks finger movement across items and reports
 * the full selected range (start to current position).
 * Auto-scrolls when finger is near top/bottom edges.
 */
class DragSelectTouchListener(
    private val onDragRangeChanged: (startPosition: Int, endPosition: Int) -> Unit
) : RecyclerView.OnItemTouchListener {

    private var isActive = false
    private var startPosition = RecyclerView.NO_POSITION
    private var lastPosition = RecyclerView.NO_POSITION

    // Auto-scroll
    private var recyclerView: RecyclerView? = null
    private val autoScrollRunnable = Runnable { autoScroll() }
    private var autoScrollVelocity = 0

    // Edge detection threshold (dp converted to px at attach time)
    private var edgeThresholdPx = 0

    companion object {
        private const val EDGE_THRESHOLD_DP = 64
        private const val AUTO_SCROLL_BASE_SPEED = 8
    }

    fun attachToRecyclerView(rv: RecyclerView) {
        recyclerView = rv
        edgeThresholdPx = (EDGE_THRESHOLD_DP * rv.resources.displayMetrics.density).toInt()
        rv.addOnItemTouchListener(this)
    }

    /**
     * Call this to start a drag-select gesture from the given adapter position.
     * Typically called from the long-press handler.
     */
    fun startDragSelection(position: Int) {
        isActive = true
        startPosition = position
        lastPosition = position
    }

    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        if (!isActive) return false

        when (e.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                updateSelection(rv, e)
                handleAutoScroll(rv, e)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                stopDragSelection()
                return false
            }
        }
        return false
    }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
        when (e.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                updateSelection(rv, e)
                handleAutoScroll(rv, e)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                stopDragSelection()
            }
        }
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        if (disallowIntercept) {
            stopDragSelection()
        }
    }

    private fun updateSelection(rv: RecyclerView, e: MotionEvent) {
        val child = rv.findChildViewUnder(e.x, e.y) ?: return
        val position = rv.getChildAdapterPosition(child)
        if (position == RecyclerView.NO_POSITION || position == lastPosition) return

        lastPosition = position

        // Report the full range from start to current position
        val rangeStart = minOf(startPosition, position)
        val rangeEnd = maxOf(startPosition, position)
        onDragRangeChanged(rangeStart, rangeEnd)
    }

    private fun handleAutoScroll(rv: RecyclerView, e: MotionEvent) {
        val y = e.y.toInt()
        val height = rv.height

        autoScrollVelocity = when {
            y < edgeThresholdPx -> {
                val distance = edgeThresholdPx - y
                -(AUTO_SCROLL_BASE_SPEED + distance / 4)
            }
            y > height - edgeThresholdPx -> {
                val distance = y - (height - edgeThresholdPx)
                AUTO_SCROLL_BASE_SPEED + distance / 4
            }
            else -> 0
        }

        if (autoScrollVelocity != 0) {
            rv.removeCallbacks(autoScrollRunnable)
            rv.postOnAnimation(autoScrollRunnable)
        } else {
            rv.removeCallbacks(autoScrollRunnable)
        }
    }

    private fun autoScroll() {
        val rv = recyclerView ?: return
        if (!isActive || autoScrollVelocity == 0) return

        rv.scrollBy(0, autoScrollVelocity)
        rv.postOnAnimation(autoScrollRunnable)
    }

    private fun stopDragSelection() {
        isActive = false
        startPosition = RecyclerView.NO_POSITION
        lastPosition = RecyclerView.NO_POSITION
        autoScrollVelocity = 0
        recyclerView?.removeCallbacks(autoScrollRunnable)
    }
}
