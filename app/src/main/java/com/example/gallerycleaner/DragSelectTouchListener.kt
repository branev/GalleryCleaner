package com.example.gallerycleaner

import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.recyclerview.widget.RecyclerView

/**
 * Handles drag-to-select on a RecyclerView grid.
 *
 * Two activation modes:
 * 1. External: call startDragSelection() from long-press handler (initial entry)
 * 2. Auto: when inSelectionMode is true, any touch-and-drag automatically starts
 *    a new drag gesture (allows multiple drag passes without long-pressing again)
 *
 * Reports the full selected range (start to current position) on each move.
 * Auto-scrolls when finger is near top/bottom edges.
 */
class DragSelectTouchListener(
    private val onDragRangeChanged: (startPosition: Int, endPosition: Int) -> Unit,
    private val onDragStarted: () -> Unit = {}
) : RecyclerView.OnItemTouchListener {

    var inSelectionMode = false

    private var isActive = false
    private var startPosition = RecyclerView.NO_POSITION
    private var lastPosition = RecyclerView.NO_POSITION

    // Pending drag: tracks ACTION_DOWN to detect drag vs tap
    private var pendingDragPosition = RecyclerView.NO_POSITION
    private var downX = 0f
    private var downY = 0f
    private var touchSlopPx = 0

    // Auto-scroll
    private var recyclerView: RecyclerView? = null
    private val autoScrollRunnable = Runnable { autoScroll() }
    private var autoScrollVelocity = 0

    // Edge detection threshold
    private var edgeThresholdPx = 0

    companion object {
        private const val EDGE_THRESHOLD_DP = 64
        private const val AUTO_SCROLL_BASE_SPEED = 8
    }

    fun attachToRecyclerView(rv: RecyclerView) {
        recyclerView = rv
        edgeThresholdPx = (EDGE_THRESHOLD_DP * rv.resources.displayMetrics.density).toInt()
        touchSlopPx = ViewConfiguration.get(rv.context).scaledTouchSlop
        rv.addOnItemTouchListener(this)
    }

    /**
     * Call this to start a drag-select gesture from the given adapter position.
     * Used for the initial long-press entry into selection mode.
     */
    fun startDragSelection(position: Int) {
        isActive = true
        startPosition = position
        lastPosition = position
    }

    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        // Handle active drag
        if (isActive) {
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

        // Auto-start drag when already in selection mode
        if (inSelectionMode) {
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    val child = rv.findChildViewUnder(e.x, e.y)
                    if (child != null) {
                        pendingDragPosition = rv.getChildAdapterPosition(child)
                        downX = e.x
                        downY = e.y
                    }
                    return false
                }
                MotionEvent.ACTION_MOVE -> {
                    if (pendingDragPosition != RecyclerView.NO_POSITION) {
                        val dx = Math.abs(e.x - downX)
                        val dy = Math.abs(e.y - downY)
                        val distance = dx * dx + dy * dy
                        if (distance > touchSlopPx * touchSlopPx) {
                            if (dx > dy) {
                                // Horizontal-dominant movement — start drag-select
                                onDragStarted()
                                startDragSelection(pendingDragPosition)
                                pendingDragPosition = RecyclerView.NO_POSITION
                                updateSelection(rv, e)
                                handleAutoScroll(rv, e)
                                return true
                            } else {
                                // Vertical-dominant movement — let RecyclerView scroll
                                pendingDragPosition = RecyclerView.NO_POSITION
                                return false
                            }
                        }
                    }
                    return false
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    pendingDragPosition = RecyclerView.NO_POSITION
                    return false
                }
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
            pendingDragPosition = RecyclerView.NO_POSITION
        }
    }

    private fun updateSelection(rv: RecyclerView, e: MotionEvent) {
        val child = rv.findChildViewUnder(e.x, e.y) ?: return
        val position = rv.getChildAdapterPosition(child)
        if (position == RecyclerView.NO_POSITION || position == lastPosition) return

        lastPosition = position

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
