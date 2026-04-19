package com.example.gallerycleaner

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Turns the four filter fields into a single human-readable string,
 * e.g. "Videos · Viber · Last 7 days". Segments are dropped when
 * the field is at its default/all-selected value.
 *
 * Returns a fallback string when nothing is non-default.
 */
object FilterComboFormatter {

    fun format(
        context: Context,
        mediaTypes: Set<MediaType>,
        sources: Set<SourceType>,
        allAvailableSources: Set<SourceType>,
        dateRange: DateRange,
        sortOption: SortOption,
    ): String {
        val parts = mutableListOf<String>()

        // Media type — only include if one of the two is off
        if (mediaTypes.size == 1) {
            parts += if (mediaTypes.first() == MediaType.PHOTO) {
                context.getString(R.string.photos)
            } else {
                context.getString(R.string.videos)
            }
        }

        // Sources — omit if "all" selected
        val nonDefaultSources = sources.intersect(allAvailableSources)
        if (nonDefaultSources.isNotEmpty() && nonDefaultSources.size != allAvailableSources.size) {
            parts += when (nonDefaultSources.size) {
                1 -> nonDefaultSources.first().label
                else -> context.resources.getQuantityString(
                    R.plurals.filter_combo_sources_count,
                    nonDefaultSources.size,
                    nonDefaultSources.size,
                )
            }
        }

        // Date — omit if preset is ALL_TIME (effectively "no filter")
        if (dateRange.preset != DateRangePreset.ALL_TIME) {
            parts += when (dateRange.preset) {
                DateRangePreset.CUSTOM -> formatCustomRange(dateRange)
                else -> dateRange.preset.label
            }
        }

        // Sort — only include if non-default (newest-first is the app default)
        if (sortOption != SortOption.DATE_DESC) {
            parts += sortOption.label.lowercase(Locale.getDefault())
        }

        return if (parts.isEmpty()) {
            context.getString(R.string.filter_combo_any_filter)
        } else {
            parts.joinToString(context.getString(R.string.filter_combo_separator))
        }
    }

    private fun formatCustomRange(range: DateRange): String {
        val fmt = SimpleDateFormat("MMM d", Locale.getDefault())
        val start = Date((range.startTimestamp ?: 0L) * 1000)
        val end = Date((range.endTimestamp ?: System.currentTimeMillis() / 1000) * 1000)
        return "${fmt.format(start)}–${fmt.format(end)}"
    }
}
