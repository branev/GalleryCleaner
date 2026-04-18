package com.example.gallerycleaner

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import java.util.Calendar
import java.util.Date

class DateBucketTest {

    private fun now(year: Int, month: Int, day: Int): Date =
        Calendar.getInstance().apply {
            clear()
            set(year, month - 1, day, 12, 0, 0)
        }.time

    private fun item(id: String, year: Int, month: Int, day: Int): MediaItem {
        val epoch = Calendar.getInstance().apply {
            clear()
            set(year, month - 1, day, 10, 0, 0)
        }.timeInMillis / 1000L
        return MediaItem(
            uri = mock(Uri::class.java),
            displayName = id,
            bucket = null,
            relativePathOrData = null,
            source = SourceType.CAMERA,
            mediaType = MediaType.PHOTO,
            dateAdded = epoch,
        )
    }

    @Test
    fun `empty list returns empty grid`() {
        assertEquals(emptyList<GridItem>(), DateBucket.bucketize(emptyList(), now(2026, 4, 18)))
    }

    @Test
    fun `today and yesterday buckets`() {
        val now = now(2026, 4, 18)
        val items = listOf(
            item("a", 2026, 4, 18),   // Today
            item("b", 2026, 4, 17),   // Yesterday
        )
        val result = DateBucket.bucketize(items, now)
        assertEquals(4, result.size)
        assertEquals("Today", (result[0] as GridItem.Header).title)
        assertEquals("Yesterday", (result[2] as GridItem.Header).title)
    }

    @Test
    fun `this week and last week boundaries`() {
        val now = now(2026, 4, 18)
        val items = listOf(
            item("a", 2026, 4, 16),  // 2 days ago → This week
            item("b", 2026, 4, 11),  // 7 days ago → This week
            item("c", 2026, 4, 10),  // 8 days ago → Last week
            item("d", 2026, 4, 4),   // 14 days ago → Last week
        )
        val result = DateBucket.bucketize(items, now)
        val headers = result.filterIsInstance<GridItem.Header>().map { it.title }
        assertEquals(listOf("This week", "Last week"), headers)
    }

    @Test
    fun `older items group by month`() {
        val now = now(2026, 4, 18)
        val items = listOf(
            item("a", 2026, 3, 20),
            item("b", 2026, 3, 1),
            item("c", 2025, 12, 5),
        )
        val result = DateBucket.bucketize(items, now)
        val headers = result.filterIsInstance<GridItem.Header>().map { it.title }
        assertEquals(listOf("March 2026", "December 2025"), headers)
    }

    @Test
    fun `header count matches item count in bucket`() {
        val now = now(2026, 4, 18)
        val items = listOf(
            item("a", 2026, 4, 18),
            item("b", 2026, 4, 18),
            item("c", 2026, 4, 18),
        )
        val result = DateBucket.bucketize(items, now)
        assertEquals(3, (result[0] as GridItem.Header).count)
    }
}
