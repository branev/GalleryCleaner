package com.example.gallerycleaner

import android.net.Uri

sealed class GalleryUiState {
    object Loading : GalleryUiState()

    data class Empty(val message: String) : GalleryUiState()

    /** No categories or media types selected - prompt user to select filters */
    data class NoFiltersSelected(
        val sourceCounts: Map<SourceType, Int>,
        val allSourceCounts: Map<SourceType, Int>,
        val selectedSources: Set<SourceType>,
        val selectedMediaTypes: Set<MediaType>,
        val selectedDateRange: DateRange,
        val selectedSortOption: SortOption
    ) : GalleryUiState()

    /** Filters are selected but no items match - show empty state with filter info */
    data class NoMatchingItems(
        val sourceCounts: Map<SourceType, Int>,
        val allSourceCounts: Map<SourceType, Int>,
        val selectedSources: Set<SourceType>,
        val selectedMediaTypes: Set<MediaType>,
        val selectedDateRange: DateRange,
        val selectedSortOption: SortOption
    ) : GalleryUiState()

    data class Normal(
        val items: List<MediaItem>,
        val sourceCounts: Map<SourceType, Int>,
        val allSourceCounts: Map<SourceType, Int>,
        val selectedSources: Set<SourceType>,
        val selectedMediaTypes: Set<MediaType>,
        val selectedDateRange: DateRange,
        val selectedSortOption: SortOption
    ) : GalleryUiState()

    data class Selection(
        val items: List<MediaItem>,
        val selectedItems: Set<Uri>,
        val hiddenSelectedCount: Int, // Items selected but hidden by filter
        val sourceCounts: Map<SourceType, Int>,
        val allSourceCounts: Map<SourceType, Int>,
        val selectedSources: Set<SourceType>,
        val selectedMediaTypes: Set<MediaType>,
        val selectedDateRange: DateRange,
        val selectedSortOption: SortOption
    ) : GalleryUiState()
}
