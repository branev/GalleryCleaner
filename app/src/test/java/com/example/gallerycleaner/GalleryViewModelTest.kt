package com.example.gallerycleaner

import android.net.Uri
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

/**
 * Unit tests for GalleryViewModel logic.
 *
 * Note: These tests verify the filtering and selection logic in isolation.
 * Since GalleryViewModel is an AndroidViewModel with dependencies on Application
 * and ContentResolver, we test the logic separately using helper functions.
 *
 * For full integration tests, use Robolectric or instrumented tests.
 */
class GalleryViewModelTest {

    private lateinit var testItems: List<MediaItem>
    private lateinit var uri1: Uri
    private lateinit var uri2: Uri
    private lateinit var uri3: Uri
    private lateinit var uri4: Uri
    private lateinit var uri5: Uri

    @Before
    fun setup() {
        // Create mock URIs for testing
        uri1 = mock(Uri::class.java)
        uri2 = mock(Uri::class.java)
        uri3 = mock(Uri::class.java)
        uri4 = mock(Uri::class.java)
        uri5 = mock(Uri::class.java)

        testItems = listOf(
            MediaItem(uri1, "img1.jpg", "WhatsApp", "WhatsApp/", SourceType.WHATSAPP, MediaType.PHOTO),
            MediaItem(uri2, "img2.jpg", "WhatsApp", "WhatsApp/", SourceType.WHATSAPP, MediaType.PHOTO),
            MediaItem(uri3, "img3.jpg", "Camera", "DCIM/Camera/", SourceType.CAMERA, MediaType.PHOTO),
            MediaItem(uri4, "img4.jpg", "Screenshots", "Screenshots/", SourceType.SCREEN_CAPTURES, MediaType.PHOTO),
            MediaItem(uri5, "img5.jpg", "Download", "Download/", SourceType.DOWNLOADS, MediaType.PHOTO)
        )
    }

    // ==================== Filtering Logic Tests ====================

    @Test
    fun `filterItems returns all items when all sources selected`() {
        val selectedSources = SourceType.values().toSet()
        val result = filterItems(testItems, selectedSources)
        assertEquals(5, result.size)
        assertEquals(testItems, result)
    }

    @Test
    fun `filterItems returns only matching items for single source`() {
        val selectedSources = setOf(SourceType.WHATSAPP)
        val result = filterItems(testItems, selectedSources)
        assertEquals(2, result.size)
        assertTrue(result.all { it.source == SourceType.WHATSAPP })
    }

    @Test
    fun `filterItems returns matching items for multiple sources`() {
        val selectedSources = setOf(SourceType.WHATSAPP, SourceType.CAMERA)
        val result = filterItems(testItems, selectedSources)
        assertEquals(3, result.size)
        assertTrue(result.all { it.source in selectedSources })
    }

    @Test
    fun `filterItems returns empty list when no sources selected`() {
        val selectedSources = emptySet<SourceType>()
        val result = filterItems(testItems, selectedSources)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `filterItems returns empty for non-matching source`() {
        val selectedSources = setOf(SourceType.TELEGRAM)
        val result = filterItems(testItems, selectedSources)
        assertTrue(result.isEmpty())
    }

    // ==================== Source Counts Tests ====================

    @Test
    fun `calculateSourceCounts returns correct counts`() {
        val counts = calculateSourceCounts(testItems)
        assertEquals(2, counts[SourceType.WHATSAPP])
        assertEquals(1, counts[SourceType.CAMERA])
        assertEquals(1, counts[SourceType.SCREEN_CAPTURES])
        assertEquals(1, counts[SourceType.DOWNLOADS])
        assertNull(counts[SourceType.TELEGRAM])
    }

    @Test
    fun `calculateSourceCounts returns empty map for empty list`() {
        val counts = calculateSourceCounts(emptyList())
        assertTrue(counts.isEmpty())
    }

    @Test
    fun `sourceCounts should reflect date-filtered items not all items`() {
        // Scenario: All items exist, but date filter narrows down the list
        // sourceCounts should be calculated from date-filtered items
        // allSourceCounts should be calculated from ALL items
        val now = System.currentTimeMillis() / 1000
        val weekAgo = now - (7 * 24 * 60 * 60)
        val monthAgo = now - (30 * 24 * 60 * 60)

        val allItems = listOf(
            MediaItem(uri1, "recent_wa.jpg", "WhatsApp", "WhatsApp/", SourceType.WHATSAPP, MediaType.PHOTO, dateAdded = now - 1000),
            MediaItem(uri2, "old_wa.jpg", "WhatsApp", "WhatsApp/", SourceType.WHATSAPP, MediaType.PHOTO, dateAdded = monthAgo - 1000),
            MediaItem(uri3, "recent_cam.jpg", "Camera", "DCIM/Camera/", SourceType.CAMERA, MediaType.PHOTO, dateAdded = now - 2000),
            MediaItem(uri4, "old_cam.jpg", "Camera", "DCIM/Camera/", SourceType.CAMERA, MediaType.PHOTO, dateAdded = monthAgo - 2000)
        )

        // allSourceCounts = from ALL items (not filtered by date)
        val allSourceCounts = calculateSourceCounts(allItems)
        assertEquals(2, allSourceCounts[SourceType.WHATSAPP])
        assertEquals(2, allSourceCounts[SourceType.CAMERA])

        // sourceCounts = from date-filtered items (last 7 days)
        val dateFilteredItems = allItems.filter { it.dateAdded >= weekAgo }
        val sourceCounts = calculateSourceCounts(dateFilteredItems)
        assertEquals(1, sourceCounts[SourceType.WHATSAPP]) // Only recent WhatsApp
        assertEquals(1, sourceCounts[SourceType.CAMERA])   // Only recent Camera
    }

    @Test
    fun `sourceCounts can have fewer sources than allSourceCounts after date filter`() {
        // Scenario: Date filter removes all items from one source
        val now = System.currentTimeMillis() / 1000
        val weekAgo = now - (7 * 24 * 60 * 60)
        val monthAgo = now - (30 * 24 * 60 * 60)

        val allItems = listOf(
            MediaItem(uri1, "recent_wa.jpg", "WhatsApp", "WhatsApp/", SourceType.WHATSAPP, MediaType.PHOTO, dateAdded = now - 1000),
            MediaItem(uri2, "old_cam.jpg", "Camera", "DCIM/Camera/", SourceType.CAMERA, MediaType.PHOTO, dateAdded = monthAgo - 2000)
        )

        val allSourceCounts = calculateSourceCounts(allItems)
        assertEquals(2, allSourceCounts.size)
        assertTrue(allSourceCounts.containsKey(SourceType.WHATSAPP))
        assertTrue(allSourceCounts.containsKey(SourceType.CAMERA))

        // After date filter (last 7 days), Camera is gone
        val dateFilteredItems = allItems.filter { it.dateAdded >= weekAgo }
        val sourceCounts = calculateSourceCounts(dateFilteredItems)
        assertEquals(1, sourceCounts.size)
        assertTrue(sourceCounts.containsKey(SourceType.WHATSAPP))
        assertFalse(sourceCounts.containsKey(SourceType.CAMERA)) // Camera not in filtered counts
    }

    // ==================== Selection Logic Tests ====================

    @Test
    fun `toggleItemSelection adds item when not selected`() {
        val currentSelection = setOf(uri1)
        val result = toggleItemSelection(currentSelection, uri2)
        assertEquals(setOf(uri1, uri2), result)
    }

    @Test
    fun `toggleItemSelection removes item when selected`() {
        val currentSelection = setOf(uri1, uri2)
        val result = toggleItemSelection(currentSelection, uri1)
        assertEquals(setOf(uri2), result)
    }

    @Test
    fun `toggleItemSelection returns empty set when last item deselected`() {
        val currentSelection = setOf(uri1)
        val result = toggleItemSelection(currentSelection, uri1)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `selectAll selects all filtered items`() {
        val filteredItems = listOf(
            MediaItem(uri1, "img1.jpg", "WhatsApp", "WhatsApp/", SourceType.WHATSAPP, MediaType.PHOTO),
            MediaItem(uri2, "img2.jpg", "WhatsApp", "WhatsApp/", SourceType.WHATSAPP, MediaType.PHOTO)
        )
        val result = selectAll(filteredItems)
        assertEquals(setOf(uri1, uri2), result)
    }

    @Test
    fun `selectAll returns empty set for empty list`() {
        val result = selectAll(emptyList())
        assertTrue(result.isEmpty())
    }

    // ==================== Hidden Selection Count Tests ====================

    @Test
    fun `calculateHiddenSelectedCount returns zero when all selected items visible`() {
        val filteredItems = testItems.take(3)
        val selectedItems = setOf(uri1, uri2)
        val result = calculateHiddenSelectedCount(filteredItems, selectedItems)
        assertEquals(0, result)
    }

    @Test
    fun `calculateHiddenSelectedCount counts hidden selected items`() {
        // Filtered items only show WhatsApp (uri1, uri2)
        val filteredItems = testItems.filter { it.source == SourceType.WHATSAPP }
        // But we have Camera item (uri3) selected too
        val selectedItems = setOf(uri1, uri3)
        val result = calculateHiddenSelectedCount(filteredItems, selectedItems)
        assertEquals(1, result) // uri3 is hidden
    }

    @Test
    fun `calculateHiddenSelectedCount returns all when no filtered items match`() {
        val filteredItems = testItems.filter { it.source == SourceType.WHATSAPP }
        val selectedItems = setOf(uri3, uri4, uri5) // Camera, Screenshots, Downloads
        val result = calculateHiddenSelectedCount(filteredItems, selectedItems)
        assertEquals(3, result) // All selected items are hidden
    }

    // ==================== Toggle All Sources Tests ====================

    @Test
    fun `toggleAllSources clears when all selected`() {
        val currentSources = SourceType.values().toSet()
        val result = toggleAllSources(currentSources, SourceType.values().toSet())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `toggleAllSources selects all when none selected`() {
        val currentSources = emptySet<SourceType>()
        val allAvailableSources = setOf(SourceType.WHATSAPP, SourceType.CAMERA)
        val result = toggleAllSources(currentSources, allAvailableSources)
        assertEquals(allAvailableSources, result)
    }

    @Test
    fun `toggleAllSources selects all when partially selected`() {
        val currentSources = setOf(SourceType.WHATSAPP)
        val allAvailableSources = setOf(SourceType.WHATSAPP, SourceType.CAMERA, SourceType.DOWNLOADS)
        val result = toggleAllSources(currentSources, allAvailableSources)
        assertEquals(allAvailableSources, result)
    }

    @Test
    fun `toggleAllSources uses allSourceCounts not filtered sourceCounts`() {
        // Scenario: User has date filter that shows only WhatsApp items
        // But there are also Camera items in the full dataset
        // When user taps "All", it should select ALL sources, not just WhatsApp
        val currentSources = setOf(SourceType.WHATSAPP)
        val filteredSourceCounts = mapOf(SourceType.WHATSAPP to 5) // Only WhatsApp visible after date filter
        val allSourceCounts = mapOf(SourceType.WHATSAPP to 10, SourceType.CAMERA to 3, SourceType.DOWNLOADS to 2)

        // Should use allSourceCounts.keys, not filteredSourceCounts.keys
        val result = toggleAllSources(currentSources, allSourceCounts.keys)
        assertEquals(allSourceCounts.keys, result)
        assertTrue(result.contains(SourceType.CAMERA)) // Camera should be selected even if not in filtered counts
        assertTrue(result.contains(SourceType.DOWNLOADS))
    }

    @Test
    fun `toggleAllSources clears all when all allSourceCounts sources are selected`() {
        // Even if some sources have 0 items in filtered view, if all sources from allSourceCounts are selected, clear
        val allSourceCounts = mapOf(SourceType.WHATSAPP to 10, SourceType.CAMERA to 3)
        val currentSources = setOf(SourceType.WHATSAPP, SourceType.CAMERA)

        val result = toggleAllSources(currentSources, allSourceCounts.keys)
        assertTrue(result.isEmpty())
    }

    // ==================== Deletion Logic Tests ====================

    @Test
    fun `removeDeletedItems removes specified items from list`() {
        val deletedUris = setOf(uri1, uri3)
        val result = removeDeletedItems(testItems, deletedUris)
        assertEquals(3, result.size)
        assertFalse(result.any { it.uri == uri1 })
        assertFalse(result.any { it.uri == uri3 })
        assertTrue(result.any { it.uri == uri2 })
        assertTrue(result.any { it.uri == uri4 })
        assertTrue(result.any { it.uri == uri5 })
    }

    @Test
    fun `removeDeletedItems returns all items when deleting non-existent uris`() {
        val nonExistentUri = mock(Uri::class.java)
        val deletedUris = setOf(nonExistentUri)
        val result = removeDeletedItems(testItems, deletedUris)
        assertEquals(5, result.size)
        assertEquals(testItems, result)
    }

    @Test
    fun `removeDeletedItems returns empty list when deleting all items`() {
        val deletedUris = setOf(uri1, uri2, uri3, uri4, uri5)
        val result = removeDeletedItems(testItems, deletedUris)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `removeDeletedItems handles empty deletion set`() {
        val deletedUris = emptySet<Uri>()
        val result = removeDeletedItems(testItems, deletedUris)
        assertEquals(5, result.size)
        assertEquals(testItems, result)
    }

    @Test
    fun `removeDeletedItems handles empty items list`() {
        val deletedUris = setOf(uri1)
        val result = removeDeletedItems(emptyList(), deletedUris)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `removeDeletedItems only removes exact uri matches`() {
        // Ensure we're only deleting by URI, not by any other property
        val deletedUris = setOf(uri2)  // Only uri2
        val result = removeDeletedItems(testItems, deletedUris)

        assertEquals(4, result.size)
        // uri2 should be gone, but uri1 (same source WhatsApp) should remain
        assertTrue(result.any { it.uri == uri1 })
        assertFalse(result.any { it.uri == uri2 })
    }

    @Test
    fun `removeDeletedItems preserves order of remaining items`() {
        val deletedUris = setOf(uri2, uri4)  // Remove 2nd and 4th items
        val result = removeDeletedItems(testItems, deletedUris)

        assertEquals(3, result.size)
        // Order should be: uri1, uri3, uri5 (original order preserved)
        assertEquals(uri1, result[0].uri)
        assertEquals(uri3, result[1].uri)
        assertEquals(uri5, result[2].uri)
    }

    @Test
    fun `deletion does not affect items with different sources`() {
        // Delete only WhatsApp items
        val whatsappUris = testItems.filter { it.source == SourceType.WHATSAPP }.map { it.uri }.toSet()
        val result = removeDeletedItems(testItems, whatsappUris)

        assertEquals(3, result.size)
        // No WhatsApp items should remain
        assertFalse(result.any { it.source == SourceType.WHATSAPP })
        // Other sources should be intact
        assertTrue(result.any { it.source == SourceType.CAMERA })
        assertTrue(result.any { it.source == SourceType.SCREEN_CAPTURES })
        assertTrue(result.any { it.source == SourceType.DOWNLOADS })
    }

    // ==================== Toggle Source Filter Tests ====================

    @Test
    fun `toggleSourceFilter adds source when not selected`() {
        val currentSources = setOf(SourceType.WHATSAPP)
        val result = toggleSourceFilter(currentSources, SourceType.CAMERA)
        assertEquals(setOf(SourceType.WHATSAPP, SourceType.CAMERA), result)
    }

    @Test
    fun `toggleSourceFilter removes source when selected`() {
        val currentSources = setOf(SourceType.WHATSAPP, SourceType.CAMERA)
        val result = toggleSourceFilter(currentSources, SourceType.WHATSAPP)
        assertEquals(setOf(SourceType.CAMERA), result)
    }

    @Test
    fun `toggleSourceFilter prevents removing last source`() {
        val currentSources = setOf(SourceType.WHATSAPP)
        val result = toggleSourceFilter(currentSources, SourceType.WHATSAPP)
        // Should keep the source to prevent empty filter
        assertEquals(setOf(SourceType.WHATSAPP), result)
    }

    // ==================== Drag Selection Logic Tests ====================

    @Test
    fun `setDragSelection unions drag range with pre-existing selection`() {
        val preExisting = setOf(uri1, uri2)
        val dragRange = setOf(uri3, uri4)
        val result = setDragSelection(true, dragRange, preExisting)
        assertEquals(setOf(uri1, uri2, uri3, uri4), result)
    }

    @Test
    fun `setDragSelection with overlapping items does not duplicate`() {
        val preExisting = setOf(uri1, uri2)
        val dragRange = setOf(uri2, uri3)
        val result = setDragSelection(true, dragRange, preExisting)
        assertEquals(setOf(uri1, uri2, uri3), result)
    }

    @Test
    fun `setDragSelection shrinks when drag range reduces`() {
        // First drag: items 1-5
        val preExisting = setOf(uri1)
        val firstDrag = setOf(uri1, uri2, uri3, uri4, uri5)
        val result1 = setDragSelection(true, firstDrag, preExisting)
        assertEquals(setOf(uri1, uri2, uri3, uri4, uri5), result1)

        // Drag back to 1-3: items 4,5 should drop out
        val secondDrag = setOf(uri1, uri2, uri3)
        val result2 = setDragSelection(true, secondDrag, preExisting)
        assertEquals(setOf(uri1, uri2, uri3), result2)
    }

    @Test
    fun `setDragSelection preserves pre-existing items outside drag range`() {
        val preExisting = setOf(uri1, uri5) // items 1 and 5 were tapped before drag
        val dragRange = setOf(uri2, uri3) // drag covers items 2-3
        val result = setDragSelection(true, dragRange, preExisting)
        assertEquals(setOf(uri1, uri2, uri3, uri5), result)
    }

    @Test
    fun `setDragSelection returns null when not in selection mode`() {
        val result = setDragSelection(false, setOf(uri1, uri2), emptySet())
        assertNull(result)
    }

    @Test
    fun `setDragSelection with empty drag range preserves pre-existing`() {
        val preExisting = setOf(uri1, uri2)
        val result = setDragSelection(true, emptySet(), preExisting)
        assertEquals(preExisting, result)
    }

    @Test
    fun `setDragSelection with empty pre-existing and empty drag returns empty`() {
        val result = setDragSelection(true, emptySet(), emptySet())
        assertEquals(emptySet<Uri>(), result)
    }

    // ==================== Selected Items Total Size Tests ====================

    @Test
    fun `getSelectedItemsTotalSize sums sizes of selected items`() {
        val items = listOf(
            MediaItem(uri1, "a.jpg", null, null, SourceType.CAMERA, MediaType.PHOTO, size = 1000L),
            MediaItem(uri2, "b.jpg", null, null, SourceType.CAMERA, MediaType.PHOTO, size = 2000L),
            MediaItem(uri3, "c.jpg", null, null, SourceType.CAMERA, MediaType.PHOTO, size = 3000L)
        )
        val selected = setOf(uri1, uri3)
        val result = getSelectedItemsTotalSize(items, selected)
        assertEquals(4000L, result)
    }

    @Test
    fun `getSelectedItemsTotalSize returns zero when nothing selected`() {
        val items = listOf(
            MediaItem(uri1, "a.jpg", null, null, SourceType.CAMERA, MediaType.PHOTO, size = 1000L)
        )
        val result = getSelectedItemsTotalSize(items, emptySet())
        assertEquals(0L, result)
    }

    @Test
    fun `getSelectedItemsTotalSize ignores URIs not in items list`() {
        val items = listOf(
            MediaItem(uri1, "a.jpg", null, null, SourceType.CAMERA, MediaType.PHOTO, size = 1000L)
        )
        val nonExistentUri = mock(Uri::class.java)
        val selected = setOf(uri1, nonExistentUri)
        val result = getSelectedItemsTotalSize(items, selected)
        assertEquals(1000L, result)
    }

    @Test
    fun `getSelectedItemsTotalSize with empty items returns zero`() {
        val result = getSelectedItemsTotalSize(emptyList(), setOf(uri1))
        assertEquals(0L, result)
    }

    // ==================== Drag Range to URI Mapping Tests ====================

    @Test
    fun `mapDragRangeToUris returns correct URIs for valid range`() {
        val result = mapDragRangeToUris(testItems, 1, 3)
        assertEquals(setOf(uri2, uri3, uri4), result)
    }

    @Test
    fun `mapDragRangeToUris drops out-of-bounds positions`() {
        val result = mapDragRangeToUris(testItems, 3, 7) // items only have index 0-4
        assertEquals(setOf(uri4, uri5), result)
    }

    @Test
    fun `mapDragRangeToUris with fully out-of-bounds range returns empty`() {
        val result = mapDragRangeToUris(testItems, 10, 15)
        assertEquals(emptySet<Uri>(), result)
    }

    @Test
    fun `mapDragRangeToUris with single position returns single URI`() {
        val result = mapDragRangeToUris(testItems, 2, 2)
        assertEquals(setOf(uri3), result)
    }

    @Test
    fun `mapDragRangeToUris with empty items returns empty`() {
        val result = mapDragRangeToUris(emptyList(), 0, 5)
        assertEquals(emptySet<Uri>(), result)
    }

    // ==================== Helper Functions (mirror ViewModel logic) ====================

    private fun filterItems(items: List<MediaItem>, selectedSources: Set<SourceType>): List<MediaItem> {
        if (selectedSources.size == SourceType.values().size) return items
        return items.filter { it.source in selectedSources }
    }

    private fun calculateSourceCounts(items: List<MediaItem>): Map<SourceType, Int> {
        return items.groupBy { it.source }.mapValues { it.value.size }
    }

    private fun toggleItemSelection(currentSelection: Set<Uri>, uri: Uri): Set<Uri> {
        val mutableSet = currentSelection.toMutableSet()
        if (uri in mutableSet) {
            mutableSet.remove(uri)
        } else {
            mutableSet.add(uri)
        }
        return mutableSet
    }

    private fun selectAll(filteredItems: List<MediaItem>): Set<Uri> {
        return filteredItems.map { it.uri }.toSet()
    }

    private fun calculateHiddenSelectedCount(filteredItems: List<MediaItem>, selectedItems: Set<Uri>): Int {
        val visibleUris = filteredItems.map { it.uri }.toSet()
        return selectedItems.count { it !in visibleUris }
    }

    private fun toggleAllSources(currentSources: Set<SourceType>, allAvailableSources: Set<SourceType>): Set<SourceType> {
        // This mirrors the ViewModel logic: use allSourceCounts.keys, not sourceCounts.keys
        return if (currentSources.containsAll(allAvailableSources) && allAvailableSources.isNotEmpty()) {
            emptySet()
        } else {
            allAvailableSources
        }
    }

    private fun toggleSourceFilter(currentSources: Set<SourceType>, source: SourceType): Set<SourceType> {
        val mutableSet = currentSources.toMutableSet()
        if (source in mutableSet) {
            if (mutableSet.size > 1) {
                mutableSet.remove(source)
            }
        } else {
            mutableSet.add(source)
        }
        return mutableSet
    }

    private fun removeDeletedItems(items: List<MediaItem>, deletedUris: Set<Uri>): List<MediaItem> {
        return items.filter { it.uri !in deletedUris }
    }

    private fun setDragSelection(
        isSelectionMode: Boolean,
        dragUris: Set<Uri>,
        preExisting: Set<Uri>
    ): Set<Uri>? {
        if (!isSelectionMode) return null
        return preExisting + dragUris
    }

    private fun getSelectedItemsTotalSize(items: List<MediaItem>, selectedUris: Set<Uri>): Long {
        return items.filter { it.uri in selectedUris }.sumOf { it.size }
    }

    private fun mapDragRangeToUris(items: List<MediaItem>, rangeStart: Int, rangeEnd: Int): Set<Uri> {
        return (rangeStart..rangeEnd)
            .filter { it in items.indices }
            .map { items[it].uri }
            .toSet()
    }
}
