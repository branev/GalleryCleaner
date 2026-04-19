package com.branev.gallerycleaner

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FilterPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _selectedSources = MutableStateFlow(loadSelectedSources())
    val selectedSources: StateFlow<Set<SourceType>> = _selectedSources.asStateFlow()

    private val _selectedMediaTypes = MutableStateFlow(loadSelectedMediaTypes())
    val selectedMediaTypes: StateFlow<Set<MediaType>> = _selectedMediaTypes.asStateFlow()

    private val _selectedDateRange = MutableStateFlow(loadDateRange())
    val selectedDateRange: StateFlow<DateRange> = _selectedDateRange.asStateFlow()

    private val _selectedSortOption = MutableStateFlow(loadSortOption())
    val selectedSortOption: StateFlow<SortOption> = _selectedSortOption.asStateFlow()

    private val _gridColumnCount = MutableStateFlow(loadGridColumnCount())
    val gridColumnCount: StateFlow<Int> = _gridColumnCount.asStateFlow()

    private fun loadSelectedSources(): Set<SourceType> {
        val stored = prefs.getStringSet(KEY_SELECTED_SOURCES, null)
        return if (stored == null) {
            // Default: all sources selected
            SourceType.values().toSet()
        } else {
            stored.mapNotNull { name ->
                try {
                    SourceType.valueOf(name)
                } catch (e: IllegalArgumentException) {
                    null
                }
            }.toSet().ifEmpty {
                // Fallback to all if parsing fails
                SourceType.values().toSet()
            }
        }
    }

    private fun loadSelectedMediaTypes(): Set<MediaType> {
        val stored = prefs.getStringSet(KEY_SELECTED_MEDIA_TYPES, null)
        return if (stored == null) {
            // Default: both photos and videos selected
            MediaType.values().toSet()
        } else {
            stored.mapNotNull { name ->
                try {
                    MediaType.valueOf(name)
                } catch (e: IllegalArgumentException) {
                    null
                }
            }.toSet()
            // Note: empty set is valid (shows empty state)
        }
    }

    private fun loadDateRange(): DateRange {
        val presetName = prefs.getString(KEY_DATE_RANGE_PRESET, null)
        val preset = if (presetName != null) {
            try {
                DateRangePreset.valueOf(presetName)
            } catch (e: IllegalArgumentException) {
                DateRangePreset.LAST_30_DAYS
            }
        } else {
            DateRangePreset.LAST_30_DAYS
        }

        return if (preset == DateRangePreset.CUSTOM) {
            val start = prefs.getLong(KEY_CUSTOM_START, 0L)
            val end = prefs.getLong(KEY_CUSTOM_END, System.currentTimeMillis() / 1000)
            DateRange(preset, start, end)
        } else {
            DateRange(preset)
        }
    }

    private fun loadSortOption(): SortOption {
        val sortName = prefs.getString(KEY_SORT_OPTION, null)
        return if (sortName != null) {
            try {
                SortOption.valueOf(sortName)
            } catch (e: IllegalArgumentException) {
                SortOption.DATE_DESC
            }
        } else {
            SortOption.DATE_DESC
        }
    }

    fun saveSelectedSources(sources: Set<SourceType>) {
        prefs.edit()
            .putStringSet(KEY_SELECTED_SOURCES, sources.map { it.name }.toSet())
            .apply()
        _selectedSources.value = sources
    }

    fun saveSelectedMediaTypes(mediaTypes: Set<MediaType>) {
        prefs.edit()
            .putStringSet(KEY_SELECTED_MEDIA_TYPES, mediaTypes.map { it.name }.toSet())
            .apply()
        _selectedMediaTypes.value = mediaTypes
    }

    fun saveDateRange(dateRange: DateRange) {
        val editor = prefs.edit()
            .putString(KEY_DATE_RANGE_PRESET, dateRange.preset.name)

        if (dateRange.preset == DateRangePreset.CUSTOM) {
            editor.putLong(KEY_CUSTOM_START, dateRange.startTimestamp ?: 0L)
            editor.putLong(KEY_CUSTOM_END, dateRange.endTimestamp ?: System.currentTimeMillis() / 1000)
        }

        editor.apply()
        _selectedDateRange.value = dateRange
    }

    fun saveSortOption(sortOption: SortOption) {
        prefs.edit()
            .putString(KEY_SORT_OPTION, sortOption.name)
            .apply()
        _selectedSortOption.value = sortOption
    }

    private fun loadGridColumnCount(): Int {
        return prefs.getInt(KEY_GRID_COLUMNS, 3)
    }

    fun saveGridColumnCount(count: Int) {
        prefs.edit().putInt(KEY_GRID_COLUMNS, count).apply()
        _gridColumnCount.value = count
    }

    companion object {
        private const val PREFS_NAME = "gallery_filter_prefs"
        private const val KEY_SELECTED_SOURCES = "selected_sources"
        private const val KEY_SELECTED_MEDIA_TYPES = "selected_media_types"
        private const val KEY_DATE_RANGE_PRESET = "date_range_preset"
        private const val KEY_CUSTOM_START = "custom_date_start"
        private const val KEY_CUSTOM_END = "custom_date_end"
        private const val KEY_SORT_OPTION = "sort_option"
        private const val KEY_GRID_COLUMNS = "grid_column_count"
    }
}
