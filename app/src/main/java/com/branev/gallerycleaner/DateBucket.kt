package com.branev.gallerycleaner

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateBucket {

    fun bucketize(
        items: List<MediaItem>,
        now: Date = Date()
    ): List<GridItem> {
        if (items.isEmpty()) return emptyList()

        val result = mutableListOf<GridItem>()
        val grouped = linkedMapOf<BucketKey, MutableList<MediaItem>>()

        for (item in items) {
            val key = keyFor(Date(item.dateAdded * 1000L), now)
            grouped.getOrPut(key) { mutableListOf() }.add(item)
        }

        for ((key, bucketItems) in grouped) {
            result += GridItem.Header(
                bucketId = key.id,
                title = key.title,
                count = bucketItems.size
            )
            for (it in bucketItems) result += GridItem.Media(it)
        }
        return result
    }

    private data class BucketKey(val id: String, val title: String)

    private fun keyFor(itemDate: Date, now: Date): BucketKey {
        val nowCal = Calendar.getInstance().apply { time = now; floorToDay() }
        val itemCal = Calendar.getInstance().apply { time = itemDate; floorToDay() }
        val diffDays = ((nowCal.timeInMillis - itemCal.timeInMillis) / 86_400_000L).toInt()

        return when {
            diffDays == 0 -> BucketKey("today", "Today")
            diffDays == 1 -> BucketKey("yesterday", "Yesterday")
            diffDays in 2..7 -> BucketKey("this-week", "This week")
            diffDays in 8..14 -> BucketKey("last-week", "Last week")
            else -> {
                val y = itemCal.get(Calendar.YEAR)
                val m = itemCal.get(Calendar.MONTH)
                val title = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(itemDate)
                BucketKey("month-$y-${"%02d".format(m + 1)}", title)
            }
        }
    }

    private fun Calendar.floorToDay() {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
}
