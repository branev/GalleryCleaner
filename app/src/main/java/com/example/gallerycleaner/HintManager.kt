package com.example.gallerycleaner

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView

/**
 * Manages the display of contextual hint tooltips.
 * Shows one hint at a time, queues others, and respects the
 * max-2-per-session limit.
 */
class HintManager(
    private val prefs: HintPreferences,
    private val rootView: ViewGroup
) {
    private var currentPopup: PopupWindow? = null
    private val queue = mutableListOf<PendingHint>()
    private var hintsShownThisSession = 0

    companion object {
        private const val MAX_HINTS_PER_SESSION = 2
        private const val SHOW_DELAY_MS = 500L
        private const val NEXT_HINT_DELAY_MS = 500L
    }

    private data class PendingHint(
        val hintId: String,
        val message: String,
        val anchorView: View,
        val priority: Int
    )

    /**
     * Request to show a hint. If the hint has already been shown (persisted),
     * or the session limit is reached, this is a no-op.
     * If another hint is currently showing, this one is queued by priority.
     */
    fun showHint(hintId: String, message: String, anchorView: View) {
        if (prefs.isHintShown(hintId)) return
        if (hintsShownThisSession >= MAX_HINTS_PER_SESSION && currentPopup == null) return

        val priority = HintPreferences.PRIORITY_ORDER.indexOf(hintId).let {
            if (it < 0) Int.MAX_VALUE else it
        }

        if (currentPopup != null) {
            // Queue it (sorted by priority on dequeue)
            queue.add(PendingHint(hintId, message, anchorView, priority))
            return
        }

        if (hintsShownThisSession >= MAX_HINTS_PER_SESSION) return

        // Show with delay
        anchorView.postDelayed({
            if (!prefs.isHintShown(hintId) && anchorView.isAttachedToWindow) {
                displayTooltip(hintId, message, anchorView)
            }
        }, SHOW_DELAY_MS)
    }

    private fun displayTooltip(hintId: String, message: String, anchorView: View) {
        val inflater = LayoutInflater.from(rootView.context)
        val tooltipView = TextView(rootView.context).apply {
            text = message
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 13f
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setBackgroundResource(R.drawable.hint_tooltip_bg)
            elevation = dp(4).toFloat()
        }

        val popup = PopupWindow(
            tooltipView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            false // Not focusable — we handle dismiss ourselves
        )
        popup.isOutsideTouchable = true
        popup.setOnDismissListener {
            currentPopup = null
            showNextQueued()
        }

        // Position: below the anchor, centered horizontally
        val location = IntArray(2)
        anchorView.getLocationInWindow(location)
        val anchorCenterX = location[0] + anchorView.width / 2

        // Measure tooltip
        tooltipView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val tooltipWidth = tooltipView.measuredWidth
        val xOffset = anchorCenterX - tooltipWidth / 2

        popup.showAtLocation(
            rootView,
            Gravity.NO_GRAVITY,
            xOffset.coerceAtLeast(dp(8)),
            location[1] + anchorView.height + dp(4)
        )

        // Fade in
        tooltipView.alpha = 0f
        tooltipView.animate().alpha(1f).setDuration(200).start()

        currentPopup = popup
        prefs.markHintShown(hintId)
        hintsShownThisSession++

        // Dismiss on any tap on root
        rootView.setOnClickListener {
            dismiss()
            rootView.setOnClickListener(null)
        }
    }

    private fun showNextQueued() {
        if (queue.isEmpty() || hintsShownThisSession >= MAX_HINTS_PER_SESSION) return

        // Pick highest priority (lowest index)
        val next = queue.minByOrNull { it.priority } ?: return
        queue.remove(next)

        next.anchorView.postDelayed({
            if (!prefs.isHintShown(next.hintId) && next.anchorView.isAttachedToWindow) {
                displayTooltip(next.hintId, next.message, next.anchorView)
            }
        }, NEXT_HINT_DELAY_MS)
    }

    fun dismiss() {
        currentPopup?.dismiss()
    }

    private fun dp(value: Int): Int {
        return (value * rootView.resources.displayMetrics.density).toInt()
    }
}
