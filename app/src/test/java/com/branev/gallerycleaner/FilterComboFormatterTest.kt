package com.branev.gallerycleaner

import android.content.Context
import android.content.res.Resources
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class FilterComboFormatterTest {

    private lateinit var context: Context
    private lateinit var resources: Resources

    @Before
    fun setUp() {
        context = mock(Context::class.java)
        resources = mock(Resources::class.java)
        `when`(context.resources).thenReturn(resources)
        `when`(context.getString(R.string.photos)).thenReturn("Photos")
        `when`(context.getString(R.string.videos)).thenReturn("Videos")
        `when`(context.getString(R.string.filter_combo_separator)).thenReturn(" · ")
        `when`(context.getString(R.string.filter_combo_any_filter)).thenReturn("your current filters")
        `when`(resources.getQuantityString(anyInt(), anyInt(), anyInt())).thenAnswer {
            val n = it.arguments[1] as Int
            "$n sources"
        }
    }

    private val allSources = setOf(
        SourceType.WHATSAPP, SourceType.VIBER, SourceType.CAMERA
    )

    @Test
    fun `no non-default filters returns fallback`() {
        val s = FilterComboFormatter.format(
            context,
            mediaTypes = setOf(MediaType.PHOTO, MediaType.VIDEO),
            sources = allSources,
            allAvailableSources = allSources,
            dateRange = DateRange(DateRangePreset.ALL_TIME),
            sortOption = SortOption.DATE_DESC,
        )
        assertEquals("your current filters", s)
    }

    @Test
    fun `only videos`() {
        val s = FilterComboFormatter.format(
            context,
            mediaTypes = setOf(MediaType.VIDEO),
            sources = allSources,
            allAvailableSources = allSources,
            dateRange = DateRange(DateRangePreset.ALL_TIME),
            sortOption = SortOption.DATE_DESC,
        )
        assertEquals("Videos", s)
    }

    @Test
    fun `one source plus date preset`() {
        val s = FilterComboFormatter.format(
            context,
            mediaTypes = setOf(MediaType.PHOTO, MediaType.VIDEO),
            sources = setOf(SourceType.VIBER),
            allAvailableSources = allSources,
            dateRange = DateRange(DateRangePreset.LAST_7_DAYS),
            sortOption = SortOption.DATE_DESC,
        )
        assertEquals("Viber · 7 days", s)
    }

    @Test
    fun `multiple sources show count`() {
        val s = FilterComboFormatter.format(
            context,
            mediaTypes = setOf(MediaType.PHOTO, MediaType.VIDEO),
            sources = setOf(SourceType.VIBER, SourceType.WHATSAPP),
            allAvailableSources = allSources,
            dateRange = DateRange(DateRangePreset.ALL_TIME),
            sortOption = SortOption.DATE_DESC,
        )
        assertEquals("2 sources", s)
    }

    @Test
    fun `videos plus viber plus 7 days`() {
        val s = FilterComboFormatter.format(
            context,
            mediaTypes = setOf(MediaType.VIDEO),
            sources = setOf(SourceType.VIBER),
            allAvailableSources = allSources,
            dateRange = DateRange(DateRangePreset.LAST_7_DAYS),
            sortOption = SortOption.DATE_DESC,
        )
        assertEquals("Videos · Viber · 7 days", s)
    }
}
