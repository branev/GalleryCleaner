package com.branev.gallerycleaner

private fun String?.icContains(vararg needles: String): Boolean {
    val hay = this ?: return false
    val lower = hay.lowercase()
    return needles.any { lower.contains(it.lowercase()) }
}

object SourceDetector {

    /**
     * Best-effort guess using folder-ish info.
     * On Android 10+ this is usually RELATIVE_PATH; on older devices it's DATA (deprecated).
     */
    fun detect(relativePathOrData: String?, bucket: String?): SourceType {
        val rp = relativePathOrData ?: ""
        val b = bucket ?: ""

        return when {
            // Messaging apps
            rp.icContains("whatsapp") || b.icContains("whatsapp") -> SourceType.WHATSAPP
            rp.icContains("viber") || b.icContains("viber") -> SourceType.VIBER
            rp.icContains("messenger") || b.icContains("messenger") -> SourceType.MESSENGER
            rp.icContains("telegram") || b.icContains("telegram") -> SourceType.TELEGRAM

            // Social media apps
            rp.icContains("instagram") || b.icContains("instagram") -> SourceType.INSTAGRAM
            rp.icContains("snapchat") || b.icContains("snapchat") -> SourceType.SNAPCHAT
            rp.icContains("facebook", "fb_temp") || b.icContains("facebook") -> SourceType.FACEBOOK
            rp.icContains("twitter", "/x/") || b.icContains("twitter", "x images") -> SourceType.TWITTER
            rp.icContains("tiktok", "musically") || b.icContains("tiktok") -> SourceType.TIKTOK
            rp.icContains("pinterest") || b.icContains("pinterest") -> SourceType.PINTEREST
            rp.icContains("reddit") || b.icContains("reddit") -> SourceType.REDDIT

            // Camera / DCIM variations across vendors
            rp.icContains("dcim/camera", "/camera/", "/dcim/", "100media", "100andro") ||
                    b.icContains("camera", "dcim") -> SourceType.CAMERA

            rp.icContains("screenshots", "screenshot", "screen_recording", "screenrecord", "screen recorder", "screenrecording") ||
                    b.icContains("screenshots", "screenshot", "screen recording", "screenrecord") ->
                SourceType.SCREEN_CAPTURES

            rp.icContains("download") || b.icContains("download") -> SourceType.DOWNLOADS

            else -> SourceType.OTHER
        }
    }
}
