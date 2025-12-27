package com.example.gallerycleaner

import android.net.Uri
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.mock

/**
 * Unit tests for MediaItem and SourceType.
 */
class MediaItemTest {

    // ==================== MediaItem Equality Tests ====================

    @Test
    fun `MediaItem equality is based on all fields`() {
        val uri = mock(Uri::class.java)
        val item1 = MediaItem(uri, "img.jpg", "Camera", "DCIM/Camera/", SourceType.CAMERA, MediaType.PHOTO)
        val item2 = MediaItem(uri, "img.jpg", "Camera", "DCIM/Camera/", SourceType.CAMERA, MediaType.PHOTO)

        assertEquals(item1, item2)
    }

    @Test
    fun `MediaItem with different uri are not equal`() {
        val uri1 = mock(Uri::class.java)
        val uri2 = mock(Uri::class.java)
        val item1 = MediaItem(uri1, "img.jpg", "Camera", "DCIM/Camera/", SourceType.CAMERA, MediaType.PHOTO)
        val item2 = MediaItem(uri2, "img.jpg", "Camera", "DCIM/Camera/", SourceType.CAMERA, MediaType.PHOTO)

        assertNotEquals(item1, item2)
    }

    @Test
    fun `MediaItem with different source are not equal`() {
        val uri = mock(Uri::class.java)
        val item1 = MediaItem(uri, "img.jpg", "Camera", "DCIM/Camera/", SourceType.CAMERA, MediaType.PHOTO)
        val item2 = MediaItem(uri, "img.jpg", "Camera", "DCIM/Camera/", SourceType.WHATSAPP, MediaType.PHOTO)

        assertNotEquals(item1, item2)
    }

    @Test
    fun `MediaItem with different mediaType are not equal`() {
        val uri = mock(Uri::class.java)
        val item1 = MediaItem(uri, "video.mp4", "Camera", "DCIM/Camera/", SourceType.CAMERA, MediaType.PHOTO)
        val item2 = MediaItem(uri, "video.mp4", "Camera", "DCIM/Camera/", SourceType.CAMERA, MediaType.VIDEO)

        assertNotEquals(item1, item2)
    }

    @Test
    fun `MediaItem allows null displayName`() {
        val uri = mock(Uri::class.java)
        val item = MediaItem(uri, null, "Camera", "DCIM/Camera/", SourceType.CAMERA, MediaType.PHOTO)

        assertNull(item.displayName)
    }

    @Test
    fun `MediaItem allows null bucket`() {
        val uri = mock(Uri::class.java)
        val item = MediaItem(uri, "img.jpg", null, "DCIM/Camera/", SourceType.CAMERA, MediaType.PHOTO)

        assertNull(item.bucket)
    }

    @Test
    fun `MediaItem allows null relativePathOrData`() {
        val uri = mock(Uri::class.java)
        val item = MediaItem(uri, "img.jpg", "Camera", null, SourceType.CAMERA, MediaType.PHOTO)

        assertNull(item.relativePathOrData)
    }

    // ==================== SourceType Label Tests ====================

    @Test
    fun `SourceType WHATSAPP has correct label`() {
        assertEquals("WhatsApp", SourceType.WHATSAPP.label)
    }

    @Test
    fun `SourceType VIBER has correct label`() {
        assertEquals("Viber", SourceType.VIBER.label)
    }

    @Test
    fun `SourceType MESSENGER has correct label`() {
        assertEquals("Messenger", SourceType.MESSENGER.label)
    }

    @Test
    fun `SourceType TELEGRAM has correct label`() {
        assertEquals("Telegram", SourceType.TELEGRAM.label)
    }

    @Test
    fun `SourceType INSTAGRAM has correct label`() {
        assertEquals("Instagram", SourceType.INSTAGRAM.label)
    }

    @Test
    fun `SourceType SNAPCHAT has correct label`() {
        assertEquals("Snapchat", SourceType.SNAPCHAT.label)
    }

    @Test
    fun `SourceType FACEBOOK has correct label`() {
        assertEquals("Facebook", SourceType.FACEBOOK.label)
    }

    @Test
    fun `SourceType TWITTER has correct label`() {
        assertEquals("Twitter/X", SourceType.TWITTER.label)
    }

    @Test
    fun `SourceType TIKTOK has correct label`() {
        assertEquals("TikTok", SourceType.TIKTOK.label)
    }

    @Test
    fun `SourceType PINTEREST has correct label`() {
        assertEquals("Pinterest", SourceType.PINTEREST.label)
    }

    @Test
    fun `SourceType REDDIT has correct label`() {
        assertEquals("Reddit", SourceType.REDDIT.label)
    }

    @Test
    fun `SourceType CAMERA has correct label`() {
        assertEquals("Camera", SourceType.CAMERA.label)
    }

    @Test
    fun `SourceType SCREEN_CAPTURES has correct label`() {
        assertEquals("Screen Captures", SourceType.SCREEN_CAPTURES.label)
    }

    @Test
    fun `SourceType DOWNLOADS has correct label`() {
        assertEquals("Downloads", SourceType.DOWNLOADS.label)
    }

    @Test
    fun `SourceType OTHER has correct label`() {
        assertEquals("Other", SourceType.OTHER.label)
    }

    // ==================== SourceType Values Tests ====================

    @Test
    fun `SourceType has expected number of values`() {
        assertEquals(15, SourceType.values().size)
    }

    @Test
    fun `SourceType valueOf works correctly`() {
        assertEquals(SourceType.WHATSAPP, SourceType.valueOf("WHATSAPP"))
        assertEquals(SourceType.CAMERA, SourceType.valueOf("CAMERA"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `SourceType valueOf throws for invalid name`() {
        SourceType.valueOf("INVALID")
    }

    // ==================== MediaType Tests ====================

    @Test
    fun `MediaType has expected number of values`() {
        assertEquals(2, MediaType.values().size)
    }

    @Test
    fun `MediaType valueOf works correctly`() {
        assertEquals(MediaType.PHOTO, MediaType.valueOf("PHOTO"))
        assertEquals(MediaType.VIDEO, MediaType.valueOf("VIDEO"))
    }
}
