package com.example.gallerycleaner

import android.content.Context

/**
 * Tracks which contextual hints have been shown.
 * Each hint is shown once, then permanently dismissed.
 * resetAllHints() re-enables them all.
 */
class HintPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isHintShown(hintId: String): Boolean = prefs.getBoolean(hintId, false)

    fun markHintShown(hintId: String) {
        prefs.edit().putBoolean(hintId, true).apply()
    }

    fun resetAllHints() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "hint_prefs"

        // Hint IDs
        const val HINT_LONG_PRESS = "hint_long_press"
        const val HINT_FILTERS = "hint_filters"
        const val HINT_DRAG_SELECT = "hint_drag_select"
        const val HINT_PINCH_ZOOM = "hint_pinch_zoom"
        const val HINT_FAST_SCROLL = "hint_fast_scroll"
        const val HINT_CONTINUE_FAB = "hint_continue_fab"
        const val HINT_PROGRESS_BAR = "hint_progress_bar"
        const val HINT_TRASH_UNDO = "hint_trash_undo"

        // Priority order (index = priority, lower = higher priority)
        val PRIORITY_ORDER = listOf(
            HINT_LONG_PRESS,
            HINT_FILTERS,
            HINT_DRAG_SELECT,
            HINT_PINCH_ZOOM,
            HINT_FAST_SCROLL,
            HINT_CONTINUE_FAB,
            HINT_PROGRESS_BAR,
            HINT_TRASH_UNDO
        )
    }
}
