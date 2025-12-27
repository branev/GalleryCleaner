package com.example.gallerycleaner

import org.junit.Assert.assertEquals
import org.junit.Test

class SourceDetectorTest {

    // ==================== WhatsApp Detection ====================

    @Test
    fun `detect WhatsApp from relative path`() {
        assertEquals(
            SourceType.WHATSAPP,
            SourceDetector.detect("Pictures/WhatsApp Images/", null)
        )
    }

    @Test
    fun `detect WhatsApp from relative path case insensitive`() {
        assertEquals(
            SourceType.WHATSAPP,
            SourceDetector.detect("pictures/whatsapp images/", null)
        )
    }

    @Test
    fun `detect WhatsApp from bucket name`() {
        assertEquals(
            SourceType.WHATSAPP,
            SourceDetector.detect(null, "WhatsApp Images")
        )
    }

    @Test
    fun `detect WhatsApp from absolute path`() {
        assertEquals(
            SourceType.WHATSAPP,
            SourceDetector.detect("/storage/emulated/0/WhatsApp/Media/WhatsApp Images/IMG.jpg", null)
        )
    }

    // ==================== Viber Detection ====================

    @Test
    fun `detect Viber from relative path`() {
        assertEquals(
            SourceType.VIBER,
            SourceDetector.detect("Viber/media/", null)
        )
    }

    @Test
    fun `detect Viber from bucket name`() {
        assertEquals(
            SourceType.VIBER,
            SourceDetector.detect(null, "Viber Images")
        )
    }

    // ==================== Messenger Detection ====================

    @Test
    fun `detect Messenger from relative path`() {
        assertEquals(
            SourceType.MESSENGER,
            SourceDetector.detect("Pictures/Messenger/", null)
        )
    }

    @Test
    fun `detect Messenger from bucket name`() {
        assertEquals(
            SourceType.MESSENGER,
            SourceDetector.detect(null, "Messenger")
        )
    }

    // ==================== Telegram Detection ====================

    @Test
    fun `detect Telegram from relative path`() {
        assertEquals(
            SourceType.TELEGRAM,
            SourceDetector.detect("Telegram/Telegram Images/", null)
        )
    }

    @Test
    fun `detect Telegram from bucket name`() {
        assertEquals(
            SourceType.TELEGRAM,
            SourceDetector.detect(null, "Telegram Images")
        )
    }

    // ==================== Instagram Detection ====================

    @Test
    fun `detect Instagram from relative path`() {
        assertEquals(
            SourceType.INSTAGRAM,
            SourceDetector.detect("Pictures/Instagram/", null)
        )
    }

    @Test
    fun `detect Instagram from bucket name`() {
        assertEquals(
            SourceType.INSTAGRAM,
            SourceDetector.detect(null, "Instagram")
        )
    }

    // ==================== Snapchat Detection ====================

    @Test
    fun `detect Snapchat from relative path`() {
        assertEquals(
            SourceType.SNAPCHAT,
            SourceDetector.detect("Snapchat/", null)
        )
    }

    @Test
    fun `detect Snapchat from bucket name`() {
        assertEquals(
            SourceType.SNAPCHAT,
            SourceDetector.detect(null, "Snapchat")
        )
    }

    // ==================== Facebook Detection ====================

    @Test
    fun `detect Facebook from relative path`() {
        assertEquals(
            SourceType.FACEBOOK,
            SourceDetector.detect("Pictures/Facebook/", null)
        )
    }

    @Test
    fun `detect Facebook from bucket name`() {
        assertEquals(
            SourceType.FACEBOOK,
            SourceDetector.detect(null, "Facebook")
        )
    }

    @Test
    fun `detect Facebook from fb_temp path`() {
        assertEquals(
            SourceType.FACEBOOK,
            SourceDetector.detect("Android/data/com.facebook.katana/fb_temp/", null)
        )
    }

    // ==================== Twitter Detection ====================

    @Test
    fun `detect Twitter from relative path`() {
        assertEquals(
            SourceType.TWITTER,
            SourceDetector.detect("Pictures/Twitter/", null)
        )
    }

    @Test
    fun `detect Twitter from bucket name`() {
        assertEquals(
            SourceType.TWITTER,
            SourceDetector.detect(null, "Twitter")
        )
    }

    @Test
    fun `detect Twitter from X images bucket`() {
        assertEquals(
            SourceType.TWITTER,
            SourceDetector.detect(null, "X Images")
        )
    }

    // ==================== TikTok Detection ====================

    @Test
    fun `detect TikTok from relative path`() {
        assertEquals(
            SourceType.TIKTOK,
            SourceDetector.detect("Pictures/TikTok/", null)
        )
    }

    @Test
    fun `detect TikTok from bucket name`() {
        assertEquals(
            SourceType.TIKTOK,
            SourceDetector.detect(null, "TikTok")
        )
    }

    @Test
    fun `detect TikTok from musically path`() {
        assertEquals(
            SourceType.TIKTOK,
            SourceDetector.detect("Pictures/musically/", null)
        )
    }

    // ==================== Pinterest Detection ====================

    @Test
    fun `detect Pinterest from relative path`() {
        assertEquals(
            SourceType.PINTEREST,
            SourceDetector.detect("Pictures/Pinterest/", null)
        )
    }

    @Test
    fun `detect Pinterest from bucket name`() {
        assertEquals(
            SourceType.PINTEREST,
            SourceDetector.detect(null, "Pinterest")
        )
    }

    // ==================== Reddit Detection ====================

    @Test
    fun `detect Reddit from relative path`() {
        assertEquals(
            SourceType.REDDIT,
            SourceDetector.detect("Pictures/Reddit/", null)
        )
    }

    @Test
    fun `detect Reddit from bucket name`() {
        assertEquals(
            SourceType.REDDIT,
            SourceDetector.detect(null, "Reddit")
        )
    }

    // ==================== Camera Detection ====================

    @Test
    fun `detect Camera from DCIM Camera path`() {
        assertEquals(
            SourceType.CAMERA,
            SourceDetector.detect("DCIM/Camera/", null)
        )
    }

    @Test
    fun `detect Camera from dcim camera path lowercase`() {
        assertEquals(
            SourceType.CAMERA,
            SourceDetector.detect("dcim/camera/", null)
        )
    }

    @Test
    fun `detect Camera from DCIM path`() {
        assertEquals(
            SourceType.CAMERA,
            SourceDetector.detect("/DCIM/", null)
        )
    }

    @Test
    fun `detect Camera from camera folder`() {
        assertEquals(
            SourceType.CAMERA,
            SourceDetector.detect("/Camera/IMG_001.jpg", null)
        )
    }

    @Test
    fun `detect Camera from bucket Camera`() {
        assertEquals(
            SourceType.CAMERA,
            SourceDetector.detect(null, "Camera")
        )
    }

    @Test
    fun `detect Camera from bucket DCIM`() {
        assertEquals(
            SourceType.CAMERA,
            SourceDetector.detect(null, "DCIM")
        )
    }

    @Test
    fun `detect Camera from 100MEDIA folder`() {
        assertEquals(
            SourceType.CAMERA,
            SourceDetector.detect("DCIM/100MEDIA/", null)
        )
    }

    @Test
    fun `detect Camera from 100ANDRO folder`() {
        assertEquals(
            SourceType.CAMERA,
            SourceDetector.detect("DCIM/100ANDRO/", null)
        )
    }

    // ==================== Screen Captures Detection ====================

    @Test
    fun `detect Screen Captures from Screenshots folder`() {
        assertEquals(
            SourceType.SCREEN_CAPTURES,
            SourceDetector.detect("Pictures/Screenshots/", null)
        )
    }

    @Test
    fun `detect Screen Captures from screenshot in path`() {
        assertEquals(
            SourceType.SCREEN_CAPTURES,
            SourceDetector.detect("DCIM/Screenshot/Screenshot_2024.png", null)
        )
    }

    @Test
    fun `detect Screen Captures from bucket Screenshots`() {
        assertEquals(
            SourceType.SCREEN_CAPTURES,
            SourceDetector.detect(null, "Screenshots")
        )
    }

    @Test
    fun `detect Screen Captures case insensitive`() {
        assertEquals(
            SourceType.SCREEN_CAPTURES,
            SourceDetector.detect("pictures/screenshots/", null)
        )
    }

    @Test
    fun `detect Screen Captures from screen recording folder`() {
        assertEquals(
            SourceType.SCREEN_CAPTURES,
            SourceDetector.detect("Pictures/Screen_Recording/", null)
        )
    }

    @Test
    fun `detect Screen Captures from screenrecord path`() {
        assertEquals(
            SourceType.SCREEN_CAPTURES,
            SourceDetector.detect("Movies/ScreenRecord/video.mp4", null)
        )
    }

    // ==================== Downloads Detection ====================

    @Test
    fun `detect Downloads from Download folder`() {
        assertEquals(
            SourceType.DOWNLOADS,
            SourceDetector.detect("Download/", null)
        )
    }

    @Test
    fun `detect Downloads from Downloads folder`() {
        assertEquals(
            SourceType.DOWNLOADS,
            SourceDetector.detect("Downloads/", null)
        )
    }

    @Test
    fun `detect Downloads from bucket Download`() {
        assertEquals(
            SourceType.DOWNLOADS,
            SourceDetector.detect(null, "Download")
        )
    }

    // ==================== Other Detection ====================

    @Test
    fun `detect Other when no pattern matches`() {
        assertEquals(
            SourceType.OTHER,
            SourceDetector.detect("Pictures/MyAlbum/", "MyAlbum")
        )
    }

    @Test
    fun `detect Other when path is null and bucket is unknown`() {
        assertEquals(
            SourceType.OTHER,
            SourceDetector.detect(null, "RandomFolder")
        )
    }

    @Test
    fun `detect Other when both path and bucket are null`() {
        assertEquals(
            SourceType.OTHER,
            SourceDetector.detect(null, null)
        )
    }

    @Test
    fun `detect Other when both path and bucket are empty`() {
        assertEquals(
            SourceType.OTHER,
            SourceDetector.detect("", "")
        )
    }

    // ==================== Priority Tests ====================

    @Test
    fun `path takes priority when both match different sources`() {
        // Path says WhatsApp, bucket says Camera - WhatsApp is checked first
        assertEquals(
            SourceType.WHATSAPP,
            SourceDetector.detect("WhatsApp/Media/", "Camera")
        )
    }

    @Test
    fun `messenger detected before camera even in DCIM`() {
        // Messenger is checked before Camera
        assertEquals(
            SourceType.MESSENGER,
            SourceDetector.detect("DCIM/Messenger/", null)
        )
    }
}
