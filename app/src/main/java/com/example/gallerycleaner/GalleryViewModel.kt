package com.example.gallerycleaner

import android.app.Application
import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

// Intermediate state classes for type-safe flow combining
private data class LoadingState(
    val isLoading: Boolean,
    val errorMessage: String?
)

private data class FilterState(
    val allItems: List<MediaItem>,
    val filteredItems: List<MediaItem>,
    val sourceCounts: Map<SourceType, Int>,          // Counts after media type + date filtering
    val allSourceCounts: Map<SourceType, Int>,       // Counts from all items (for filter UI)
    val selectedSources: Set<SourceType>,
    val selectedMediaTypes: Set<MediaType>,
    val selectedDateRange: DateRange,
    val selectedSortOption: SortOption
)

private data class SelectionState(
    val isSelectionMode: Boolean,
    val selectedItems: Set<Uri>
)

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val filterPreferences = FilterPreferences(application)
    private val contentResolver: ContentResolver = application.contentResolver

    // All items loaded from MediaStore
    private val _allItems = MutableStateFlow<List<MediaItem>>(emptyList())

    // Loading state
    private val _isLoading = MutableStateFlow(false)

    // Error/empty message
    private val _errorMessage = MutableStateFlow<String?>(null)

    // Selection mode state
    private val _isSelectionMode = MutableStateFlow(false)
    private val _selectedItems = MutableStateFlow<Set<Uri>>(emptySet())

    // Viewed items tracking (session-only, not persisted)
    private val _viewedItems = MutableStateFlow<Set<Uri>>(emptySet())
    val viewedItems: StateFlow<Set<Uri>> = _viewedItems.asStateFlow()

    // Expose filter states from preferences
    val selectedSources: StateFlow<Set<SourceType>> = filterPreferences.selectedSources
    val selectedMediaTypes: StateFlow<Set<MediaType>> = filterPreferences.selectedMediaTypes
    val selectedDateRange: StateFlow<DateRange> = filterPreferences.selectedDateRange
    val selectedSortOption: StateFlow<SortOption> = filterPreferences.selectedSortOption

    // Stage 1: Combine loading-related flows
    private val loadingState: StateFlow<LoadingState> = combine(
        _isLoading,
        _errorMessage
    ) { isLoading, errorMessage ->
        LoadingState(isLoading, errorMessage)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, LoadingState(false, null))

    // Stage 2: Combine filter-related flows (use nested combine for 5 flows)
    private val filterState: StateFlow<FilterState> = combine(
        combine(_allItems, selectedSources) { items, sources -> Pair(items, sources) },
        combine(selectedMediaTypes, selectedDateRange) { types, range -> Pair(types, range) },
        selectedSortOption
    ) { (allItems, sources), (mediaTypes, dateRange), sortOption ->
        // 0. Calculate source counts from ALL items (for filter UI to always show all sources)
        val allSourceCounts = allItems.groupBy { it.source }.mapValues { it.value.size }

        // 1. Filter by media type
        val mediaTypeFiltered = if (mediaTypes.size == MediaType.values().size) {
            allItems
        } else {
            allItems.filter { it.mediaType in mediaTypes }
        }

        // 2. Filter by date range
        val dateFiltered = filterByDateRange(mediaTypeFiltered, dateRange)

        // 3. Calculate source counts (from date-filtered items)
        val sourceCounts = dateFiltered.groupBy { it.source }.mapValues { it.value.size }

        // 4. Filter by source
        val sourceFiltered = if (sources.size == SourceType.values().size) {
            dateFiltered
        } else {
            dateFiltered.filter { it.source in sources }
        }

        // 5. Sort items
        val filteredItems = sortItems(sourceFiltered, sortOption)

        FilterState(allItems, filteredItems, sourceCounts, allSourceCounts, sources, mediaTypes, dateRange, sortOption)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, FilterState(emptyList(), emptyList(), emptyMap(), emptyMap(), emptySet(), emptySet(), DateRange.DEFAULT, SortOption.DATE_DESC))

    // Stage 3: Combine selection-related flows
    private val selectionState: StateFlow<SelectionState> = combine(
        _isSelectionMode,
        _selectedItems
    ) { isSelectionMode, selectedItems ->
        SelectionState(isSelectionMode, selectedItems)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SelectionState(false, emptySet()))

    // Stage 4: Final combine - all parameters are fully typed (no casts needed)
    val uiState: StateFlow<GalleryUiState> = combine(
        loadingState,
        filterState,
        selectionState
    ) { loading, filter, selection ->
        when {
            loading.isLoading -> GalleryUiState.Loading
            loading.errorMessage != null -> GalleryUiState.Empty(loading.errorMessage)
            filter.allItems.isEmpty() -> GalleryUiState.Empty("No media found")
            filter.selectedMediaTypes.isEmpty() || filter.selectedSources.isEmpty() -> GalleryUiState.NoFiltersSelected(
                filter.sourceCounts,
                filter.allSourceCounts,
                filter.selectedSources,
                filter.selectedMediaTypes,
                filter.selectedDateRange,
                filter.selectedSortOption
            )
            filter.filteredItems.isEmpty() -> GalleryUiState.NoMatchingItems(
                filter.sourceCounts,
                filter.allSourceCounts,
                filter.selectedSources,
                filter.selectedMediaTypes,
                filter.selectedDateRange,
                filter.selectedSortOption
            )
            selection.isSelectionMode -> {
                // Calculate how many selected items are hidden by filter
                val visibleUris = filter.filteredItems.map { it.uri }.toSet()
                val hiddenSelectedCount = selection.selectedItems.count { it !in visibleUris }
                GalleryUiState.Selection(
                    filter.filteredItems,
                    selection.selectedItems,
                    hiddenSelectedCount,
                    filter.sourceCounts,
                    filter.allSourceCounts,
                    filter.selectedSources,
                    filter.selectedMediaTypes,
                    filter.selectedDateRange,
                    filter.selectedSortOption
                )
            }
            else -> GalleryUiState.Normal(
                filter.filteredItems,
                filter.sourceCounts,
                filter.allSourceCounts,
                filter.selectedSources,
                filter.selectedMediaTypes,
                filter.selectedDateRange,
                filter.selectedSortOption
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, GalleryUiState.Loading)

    // Actions
    fun loadMedia() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val items = withContext(Dispatchers.IO) {
                val images = queryMedia(MediaType.PHOTO)
                val videos = queryMedia(MediaType.VIDEO)
                (images + videos).sortedByDescending { it.dateAdded }
            }

            _allItems.value = items
            _isLoading.value = false

            if (items.isEmpty()) {
                _errorMessage.value = "No media found"
            }
        }
    }

    @Deprecated("Use loadMedia() instead", ReplaceWith("loadMedia()"))
    fun loadImages() = loadMedia()

    fun toggleSourceFilter(source: SourceType) {
        val current = selectedSources.value.toMutableSet()
        if (source in current) {
            current.remove(source)
        } else {
            current.add(source)
        }
        filterPreferences.saveSelectedSources(current)
    }

    fun toggleAllSources() {
        val current = selectedSources.value
        // Use allSourceCounts to get ALL available sources (not filtered by date)
        val availableSources = filterState.value.allSourceCounts.keys
        if (current.containsAll(availableSources) && availableSources.isNotEmpty()) {
            // All available sources selected -> deselect all (clear)
            filterPreferences.saveSelectedSources(emptySet())
        } else {
            // Not all selected -> select all available sources
            filterPreferences.saveSelectedSources(availableSources)
        }
    }

    fun toggleMediaType(mediaType: MediaType) {
        val current = selectedMediaTypes.value.toMutableSet()
        if (mediaType in current) {
            current.remove(mediaType)
        } else {
            current.add(mediaType)
        }
        filterPreferences.saveSelectedMediaTypes(current)
    }

    fun setDateRangePreset(preset: DateRangePreset) {
        filterPreferences.saveDateRange(DateRange(preset))
    }

    fun setCustomDateRange(startMillis: Long, endMillis: Long) {
        filterPreferences.saveDateRange(DateRange(
            preset = DateRangePreset.CUSTOM,
            startTimestamp = startMillis / 1000,
            endTimestamp = endMillis / 1000
        ))
    }

    fun setSortOption(sortOption: SortOption) {
        filterPreferences.saveSortOption(sortOption)
    }

    fun resetFilters() {
        filterPreferences.saveSelectedSources(SourceType.values().toSet())
        filterPreferences.saveSelectedMediaTypes(MediaType.values().toSet())
        filterPreferences.saveDateRange(DateRange.DEFAULT)
        filterPreferences.saveSortOption(SortOption.DATE_DESC)
    }

    fun enterSelectionMode(initialItem: Uri) {
        _isSelectionMode.value = true
        _selectedItems.value = setOf(initialItem)
    }

    fun exitSelectionMode() {
        _isSelectionMode.value = false
        _selectedItems.value = emptySet()
    }

    fun toggleItemSelection(uri: Uri) {
        val current = _selectedItems.value.toMutableSet()
        if (uri in current) {
            current.remove(uri)
            if (current.isEmpty()) {
                exitSelectionMode()
                return
            }
        } else {
            current.add(uri)
        }
        _selectedItems.value = current
    }

    fun selectAll() {
        _selectedItems.value = filterState.value.filteredItems.map { it.uri }.toSet()
    }

    fun getSelectedItems(): Set<Uri> = _selectedItems.value

    fun getSelectedCount(): Int = _selectedItems.value.size

    fun getSelectedItemsTotalSize(): Long {
        val selectedUris = _selectedItems.value
        return _allItems.value.filter { it.uri in selectedUris }.sumOf { it.size }
    }

    fun markAsViewed(uri: Uri) {
        if (uri !in _viewedItems.value) {
            _viewedItems.value = _viewedItems.value + uri
        }
    }

    fun markItemsAsViewed(uris: Collection<Uri>) {
        val current = _viewedItems.value
        val newUris = uris.filter { it !in current }
        if (newUris.isNotEmpty()) {
            _viewedItems.value = current + newUris
        }
    }

    fun clearViewedItems() {
        _viewedItems.value = emptySet()
    }

    fun getFirstUnviewedIndex(items: List<MediaItem>): Int {
        val viewed = _viewedItems.value
        return items.indexOfFirst { it.uri !in viewed }
    }

    fun removeDeletedItems(deletedUris: Set<Uri>) {
        // Remove deleted items from the all items list
        _allItems.value = _allItems.value.filter { it.uri !in deletedUris }
        // Remove from viewed items as well
        _viewedItems.value = _viewedItems.value - deletedUris
        // Clear selection and exit selection mode
        exitSelectionMode()
    }

    private fun queryMedia(mediaType: MediaType): List<MediaItem> {
        val collection = when (mediaType) {
            MediaType.PHOTO -> if (Build.VERSION.SDK_INT >= 29) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
            MediaType.VIDEO -> if (Build.VERSION.SDK_INT >= 29) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }
        }

        val projection = mutableListOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.SIZE
        )

        if (mediaType == MediaType.VIDEO) {
            projection += MediaStore.Video.VideoColumns.DURATION
        }

        if (Build.VERSION.SDK_INT >= 29) {
            projection += MediaStore.MediaColumns.RELATIVE_PATH
        } else {
            @Suppress("DEPRECATION")
            projection += MediaStore.MediaColumns.DATA
        }

        val sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} DESC"

        val items = ArrayList<MediaItem>()

        contentResolver.query(
            collection,
            projection.toTypedArray(),
            null,
            null,
            sortOrder
        )?.use { cursor ->

            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)

            val durationCol = if (mediaType == MediaType.VIDEO) {
                cursor.getColumnIndex(MediaStore.Video.VideoColumns.DURATION)
            } else -1

            val pathCol = if (Build.VERSION.SDK_INT >= 29) {
                cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
            } else {
                @Suppress("DEPRECATION")
                cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
            }

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val name = cursor.getString(nameCol)
                val bucket = cursor.getString(bucketCol)
                val path = if (pathCol >= 0) cursor.getString(pathCol) else null
                val dateAdded = cursor.getLong(dateAddedCol)
                val size = cursor.getLong(sizeCol)
                val duration = if (durationCol >= 0) cursor.getLong(durationCol) else 0L

                val uri = ContentUris.withAppendedId(collection, id)
                val source = SourceDetector.detect(path, bucket)

                items += MediaItem(
                    uri = uri,
                    displayName = name,
                    bucket = bucket,
                    relativePathOrData = path,
                    source = source,
                    mediaType = mediaType,
                    dateAdded = dateAdded,
                    size = size,
                    duration = duration
                )
            }
        }

        return items
    }

    private fun sortItems(items: List<MediaItem>, sortOption: SortOption): List<MediaItem> {
        return when (sortOption) {
            SortOption.DATE_DESC -> items.sortedByDescending { it.dateAdded }
            SortOption.DATE_ASC -> items.sortedBy { it.dateAdded }
            SortOption.NAME_ASC -> items.sortedBy { it.displayName?.lowercase() ?: "" }
            SortOption.NAME_DESC -> items.sortedByDescending { it.displayName?.lowercase() ?: "" }
            SortOption.SIZE_DESC -> items.sortedByDescending { it.size }
            SortOption.SIZE_ASC -> items.sortedBy { it.size }
        }
    }

    private fun filterByDateRange(items: List<MediaItem>, dateRange: DateRange): List<MediaItem> {
        if (dateRange.preset == DateRangePreset.ALL_TIME) return items

        val (start, end) = getDateRangeBounds(dateRange)
        return items.filter { it.dateAdded in start..end }
    }

    private fun getDateRangeBounds(dateRange: DateRange): Pair<Long, Long> {
        val now = System.currentTimeMillis() / 1000

        return when (dateRange.preset) {
            DateRangePreset.CUSTOM -> {
                Pair(dateRange.startTimestamp ?: 0L, dateRange.endTimestamp ?: now)
            }
            DateRangePreset.THIS_YEAR -> {
                val cal = Calendar.getInstance()
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                Pair(cal.timeInMillis / 1000, now)
            }
            DateRangePreset.ALL_TIME -> Pair(0L, now)
            else -> Pair(now - (dateRange.preset.days!! * 24 * 60 * 60L), now)
        }
    }
}
