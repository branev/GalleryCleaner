package com.branev.gallerycleaner

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.*

/**
 * Unit tests for FilterPreferences.
 *
 * Uses Mockito to mock SharedPreferences since this is a unit test
 * (not an instrumented test running on device).
 */
class FilterPreferencesTest {

    private fun createMocks(savedSources: Set<String>? = null): Triple<Context, SharedPreferences, SharedPreferences.Editor> {
        val mockContext: Context = mock()
        val mockPrefs: SharedPreferences = mock()
        val mockEditor: SharedPreferences.Editor = mock()

        whenever(mockContext.getSharedPreferences(any(), any())).thenReturn(mockPrefs)
        whenever(mockPrefs.getStringSet(any(), isNull())).thenReturn(savedSources)
        whenever(mockPrefs.edit()).thenReturn(mockEditor)
        whenever(mockEditor.putStringSet(any(), any())).thenReturn(mockEditor)

        return Triple(mockContext, mockPrefs, mockEditor)
    }

    // ==================== Load Selected Sources Tests ====================

    @Test
    fun `loadSelectedSources returns all sources when no saved preference`() {
        val (mockContext, _, _) = createMocks(savedSources = null)

        val prefs = FilterPreferences(mockContext)

        assertEquals(SourceType.values().toSet(), prefs.selectedSources.value)
    }

    @Test
    fun `loadSelectedSources returns saved sources`() {
        val savedSources = setOf("WHATSAPP", "CAMERA")
        val (mockContext, _, _) = createMocks(savedSources)

        val prefs = FilterPreferences(mockContext)

        assertEquals(setOf(SourceType.WHATSAPP, SourceType.CAMERA), prefs.selectedSources.value)
    }

    @Test
    fun `loadSelectedSources ignores invalid source names`() {
        val savedSources = setOf("WHATSAPP", "INVALID_SOURCE", "CAMERA")
        val (mockContext, _, _) = createMocks(savedSources)

        val prefs = FilterPreferences(mockContext)

        assertEquals(setOf(SourceType.WHATSAPP, SourceType.CAMERA), prefs.selectedSources.value)
    }

    @Test
    fun `loadSelectedSources returns all sources when all saved are invalid`() {
        val savedSources = setOf("INVALID1", "INVALID2")
        val (mockContext, _, _) = createMocks(savedSources)

        val prefs = FilterPreferences(mockContext)

        // Falls back to all sources
        assertEquals(SourceType.values().toSet(), prefs.selectedSources.value)
    }

    @Test
    fun `loadSelectedSources returns all sources when saved set is empty`() {
        val savedSources = emptySet<String>()
        val (mockContext, _, _) = createMocks(savedSources)

        val prefs = FilterPreferences(mockContext)

        // Empty set falls back to all sources
        assertEquals(SourceType.values().toSet(), prefs.selectedSources.value)
    }

    // ==================== Save Selected Sources Tests ====================

    @Test
    fun `saveSelectedSources persists to SharedPreferences`() {
        val (mockContext, _, mockEditor) = createMocks(savedSources = null)
        val prefs = FilterPreferences(mockContext)

        val sourcesToSave = setOf(SourceType.WHATSAPP, SourceType.SCREEN_CAPTURES)
        prefs.saveSelectedSources(sourcesToSave)

        verify(mockEditor).putStringSet(
            eq("selected_sources"),
            eq(setOf("WHATSAPP", "SCREEN_CAPTURES"))
        )
        verify(mockEditor).apply()
    }

    @Test
    fun `saveSelectedSources updates StateFlow`() {
        val (mockContext, _, _) = createMocks(savedSources = null)
        val prefs = FilterPreferences(mockContext)

        val sourcesToSave = setOf(SourceType.TELEGRAM, SourceType.DOWNLOADS)
        prefs.saveSelectedSources(sourcesToSave)

        assertEquals(sourcesToSave, prefs.selectedSources.value)
    }

    @Test
    fun `saveSelectedSources with empty set`() {
        val (mockContext, _, mockEditor) = createMocks(savedSources = null)
        val prefs = FilterPreferences(mockContext)

        prefs.saveSelectedSources(emptySet())

        verify(mockEditor).putStringSet(eq("selected_sources"), eq(emptySet()))
        assertEquals(emptySet<SourceType>(), prefs.selectedSources.value)
    }

    @Test
    fun `saveSelectedSources with all sources`() {
        val (mockContext, _, mockEditor) = createMocks(savedSources = null)
        val prefs = FilterPreferences(mockContext)

        val allSources = SourceType.values().toSet()
        prefs.saveSelectedSources(allSources)

        val expectedNames = SourceType.values().map { it.name }.toSet()
        verify(mockEditor).putStringSet(eq("selected_sources"), eq(expectedNames))
        assertEquals(allSources, prefs.selectedSources.value)
    }
}
