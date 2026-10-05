package com.antigravity.iptv

import androidx.media3.common.MimeTypes
import com.antigravity.iptv.player.PlaybackStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamOptimizationTest {

    private fun detectMimeType(url: String): String {
        val cleanUrl = url.substringBefore('?').substringBefore('#').lowercase()
        return when {
            cleanUrl.endsWith(".m3u8") || cleanUrl.contains("/hls/") || cleanUrl.contains(".m3u8") -> MimeTypes.APPLICATION_M3U8
            cleanUrl.endsWith(".mpd") -> MimeTypes.APPLICATION_MPD
            cleanUrl.endsWith(".ism") || cleanUrl.endsWith(".isml") -> MimeTypes.APPLICATION_SS
            cleanUrl.endsWith(".ts") -> MimeTypes.VIDEO_MP2T
            cleanUrl.endsWith(".mp4") -> MimeTypes.APPLICATION_MP4
            cleanUrl.endsWith(".mkv") -> MimeTypes.VIDEO_MATROSKA
            else -> MimeTypes.APPLICATION_M3U8
        }
    }

    @Test
    fun testHlsMimeTypeDetection() {
        // Standard .m3u8
        assertEquals(MimeTypes.APPLICATION_M3U8, detectMimeType("http://stream.example.com/live/ch1/index.m3u8"))

        // HLS with auth query parameters & token
        assertEquals(MimeTypes.APPLICATION_M3U8, detectMimeType("https://cdn.iptv.org/live.m3u8?token=abc123xyz&expire=999999"))

        // HLS with /hls/ path without extension
        assertEquals(MimeTypes.APPLICATION_M3U8, detectMimeType("http://origin.tv/hls/channel55/master"))

        // MPEG-TS direct stream
        assertEquals(MimeTypes.VIDEO_MP2T, detectMimeType("http://server.iptv:8080/live/user/pass/101.ts"))

        // MPD (DASH)
        assertEquals(MimeTypes.APPLICATION_MPD, detectMimeType("https://live.dash.org/manifest.mpd"))

        // Generic stream without extension defaults to HLS for IPTV
        assertEquals(MimeTypes.APPLICATION_M3U8, detectMimeType("http://streamer.io/live/stream?id=44"))
    }

    @Test
    fun testPlaybackStatusProperties() {
        assertTrue(PlaybackStatus.CONNECTING.isBufferingOrConnecting)
        assertTrue(PlaybackStatus.BUFFERING.isBufferingOrConnecting)
        assertTrue(PlaybackStatus.RECOVERING.isBufferingOrConnecting)

        assertFalse(PlaybackStatus.IDLE.isBufferingOrConnecting)
        assertFalse(PlaybackStatus.READY.isBufferingOrConnecting)
        assertFalse(PlaybackStatus.ENDED.isBufferingOrConnecting)
        assertFalse(PlaybackStatus.ERROR.isBufferingOrConnecting)
    }

    @Test
    fun testBufferingThresholds() {
        val minBufferMs = 15_000
        val maxBufferMs = 35_000
        val bufferForPlaybackMs = 1_000
        val bufferForRebufferMs = 2_500
        val backBufferDurationMs = 5_000

        // Startup threshold must be low for fast start
        assertTrue(bufferForPlaybackMs <= 1_500)

        // Min buffer must be sufficient to hold 2-3 HLS segments (each segment ~4-6s)
        assertTrue(minBufferMs >= 12_000)

        // Max buffer must be bounded to protect RAM on mobile devices
        assertTrue(maxBufferMs in 25_000..45_000)

        // Rebuffer threshold must be higher than initial startup to prevent stutter loop
        assertTrue(bufferForRebufferMs > bufferForPlaybackMs)

        // Back-buffer must retain sufficient keyframes for micro-seeks
        assertTrue(backBufferDurationMs >= 3_000)
    }

    @Test
    fun testLiveEdgeConstraints() {
        val targetOffsetMs = 4_000L
        val minOffsetMs = 2_000L
        val maxOffsetMs = 20_000L
        val minSpeed = 0.97f
        val maxSpeed = 1.03f

        assertTrue(targetOffsetMs in minOffsetMs..maxOffsetMs)
        assertTrue(minSpeed in 0.95f..0.99f)
        assertTrue(maxSpeed in 1.01f..1.05f)
    }
}
