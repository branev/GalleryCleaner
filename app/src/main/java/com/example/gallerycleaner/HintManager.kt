package com.example.gallerycleaner

import android.view.View
import android.widget.TextView
import com.google.android.material.card.MaterialCardView

/**
 * Manages contextual hint display using a bottom card with "Got it" button.
 * Shows one hint at a time, queues others by priority, max 2 per session.
 */
class HintManager(
    private val prefs: HintPreferences,
    private val hintCard: MaterialCardView,
    private val hintText: TextView,
    btnGotIt: View
) {
    private var isShowing = false
    private var isPending = false // A hint is scheduled but not yet visible
    private val queue = mutableListOf<PendingHint>()
    private var hintsShownThisSession = 0

    companion object {
        private const val MAX_HINTS_PER_SESSION = 3
        private const val SHOW_DELAY_MS = 1000L
        private const val NEXT_HINT_DELAY_MS = 1000L
    }

    private data class PendingHint(
        val hintId: String,
        val message: String,
        val priority: Int
    )

    init {
        btnGotIt.setOnClickListener {
            dismissWithAnimation()
        }
    }

    fun showHint(hintId: String, message: String) {
        if (prefs.isHintShown(hintId)) return
        if (hintsShownThisSession >= MAX_HINTS_PER_SESSION) return

        val priority = HintPreferences.PRIORITY_ORDER.indexOf(hintId).let {
            if (it < 0) Int.MAX_VALUE else it
        }

        // If a hint is showing or pending, queue this one
        if (isShowing || isPending) {
            if (queue.none { it.hintId == hintId }) {
                queue.add(PendingHint(hintId, message, priority))
            }
            return
        }

        // Schedule with delay
        isPending = true
        hintCard.postDelayed({
            isPending = false
            if (!prefs.isHintShown(hintId)) {
                displayCard(hintId, message)
            }
        }, SHOW_DELAY_MS)
    }

    private fun displayCard(hintId: String, message: String) {
        hintText.text = message
        hintCard.visibility = View.VISIBLE

        // Slide up animation
        hintCard.translationY = 300f
        hintCard.animate()
            .translationY(0f)
            .setDuration(250)
            .start()

        isShowing = true
        prefs.markHintShown(hintId)
        hintsShownThisSession++
    }

    private fun dismissWithAnimation() {
        hintCard.animate()
            .translationY(hintCard.height.toFloat() + 100f)
            .setDuration(200)
            .withEndAction {
                hintCard.visibility = View.GONE
                hintCard.translationY = 0f
                isShowing = false
                showNextQueued()
            }
            .start()
    }

    private fun showNextQueued() {
        if (queue.isEmpty() || hintsShownThisSession >= MAX_HINTS_PER_SESSION) return

        val next = queue.minByOrNull { it.priority } ?: return
        queue.remove(next)

        isPending = true
        hintCard.postDelayed({
            isPending = false
            if (!prefs.isHintShown(next.hintId)) {
                displayCard(next.hintId, next.message)
            }
        }, NEXT_HINT_DELAY_MS)
    }

    fun dismiss() {
        if (isShowing) {
            hintCard.animate().cancel()
            hintCard.visibility = View.GONE
            hintCard.translationY = 0f
            isShowing = false
        }
        isPending = false
        queue.clear()
    }
}
