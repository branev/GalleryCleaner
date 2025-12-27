package com.example.gallerycleaner

import android.net.Uri

enum class MediaType {
    PHOTO,
    VIDEO
}

data class MediaItem(
    val uri: Uri,
    val displayName: String?,
    val bucket: String?,
    val relativePathOrData: String?,
    val source: SourceType,
    val mediaType: MediaType,
    val dateAdded: Long = 0L, // Unix timestamp in seconds
    val size: Long = 0L // File size in bytes
)

enum class SourceType(val label: String) {
    WHATSAPP("WhatsApp"),
    VIBER("Viber"),
    MESSENGER("Messenger"),
    TELEGRAM("Telegram"),
    INSTAGRAM("Instagram"),
    SNAPCHAT("Snapchat"),
    FACEBOOK("Facebook"),
    TWITTER("Twitter/X"),
    TIKTOK("TikTok"),
    PINTEREST("Pinterest"),
    REDDIT("Reddit"),
    CAMERA("Camera"),
    SCREEN_CAPTURES("Screen Captures"),
    DOWNLOADS("Downloads"),
    OTHER("Other")
}

enum class DateRangePreset(val label: String, val days: Int?) {
    LAST_7_DAYS("7 days", 7),
    LAST_30_DAYS("30 days", 30),
    LAST_3_MONTHS("3 months", 90),
    THIS_YEAR("This year", -1),
    ALL_TIME("All time", null),
    CUSTOM("Custom", null)
}

data class DateRange(
    val preset: DateRangePreset,
    val startTimestamp: Long? = null,
    val endTimestamp: Long? = null
) {
    companion object {
        val DEFAULT = DateRange(DateRangePreset.LAST_30_DAYS)
    }
}

enum class SortOption(val label: String) {
    DATE_DESC("Newest first"),
    DATE_ASC("Oldest first"),
    NAME_ASC("Name A-Z"),
    NAME_DESC("Name Z-A"),
    SIZE_DESC("Largest first"),
    SIZE_ASC("Smallest first")
}
