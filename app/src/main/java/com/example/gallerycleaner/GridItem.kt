package com.example.gallerycleaner

sealed class GridItem {
    data class Header(val bucketId: String, val title: String, val count: Int) : GridItem() {
        val stableId: Long get() = (bucketId.hashCode().toLong() shl 32) or 0xFFFFFFFFL
    }
    data class Media(val item: MediaItem) : GridItem() {
        val stableId: Long get() = item.uri.hashCode().toLong() and 0x7FFFFFFFL
    }
}
