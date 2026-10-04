package com.antigravity.iptv

import com.antigravity.iptv.data.parser.M3UParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class M3UParserTest {

    @Test
    fun parse_validM3U_extractsAllChannelsAndMetadata() {
        val m3uContent = """
            #EXTM3U
            #EXTINF:-1 tvg-id="OTV.in" tvg-name="OTV" tvg-logo="https://example.com/otv.png" group-title="News",OTV News (1080p)
            https://example.com/otv/stream.m3u8
            #EXTINF:-1 tvg-id="Kalinga.in" tvg-name="Kalinga TV" group-title="News",Kalinga TV Odia (720p)
            https://example.com/kalinga/stream.m3u8
        """.trimIndent()

        val channels = M3UParser.parse(ByteArrayInputStream(m3uContent.toByteArray()), playlistId = 1L)

        assertEquals(2, channels.size)

        val ch1 = channels[0]
        assertEquals("OTV News (1080p)", ch1.name)
        assertEquals("OTV.in", ch1.tvgId)
        assertEquals("OTV", ch1.tvgName)
        assertEquals("https://example.com/otv.png", ch1.logoUrl)
        assertEquals("News", ch1.groupTitle)
        assertEquals("https://example.com/otv/stream.m3u8", ch1.streamUrl)
        assertEquals(1L, ch1.playlistId)

        val ch2 = channels[1]
        assertEquals("Kalinga TV Odia (720p)", ch2.name)
        assertEquals("Odia", ch2.language)
    }

    @Test
    fun parse_malformedLines_recoversGracefully() {
        val malformedM3U = """
            #EXTM3U
            RANDOM INVALID NOISE LINE
            #EXTINF:-1 tvg-id="" ,
            #EXTINF:-1 group-title="Sports",Star Sports 1 (1080p)
            http://example.com/starsports.m3u8
            INVALID_URL_WITHOUT_EXTINF
            #EXTINF:BROKEN ATTRIBUTES tvg-logo
            #EXTINF:-1 group-title="Entertainment",Colors Hindi
            https://example.com/colors.m3u8
        """.trimIndent()

        val channels = M3UParser.parse(ByteArrayInputStream(malformedM3U.toByteArray()), playlistId = 2L)

        // Only valid channels with streams should be collected
        assertTrue(channels.size >= 2)
        assertEquals("Star Sports 1 (1080p)", channels[0].name)
        assertEquals("Colors Hindi", channels[1].name)
        assertEquals("Hindi", channels[1].language)
    }

    @Test
    fun detectLanguage_accuratelyIdentifiesRegionalLanguages() {
        assertEquals("Odia", M3UParser.detectLanguage("Tarang TV", "Tarang", "Entertainment", "tarang.in"))
        assertEquals("Odia", M3UParser.detectLanguage("OTV", "OTV", "News", "otv.in"))
        assertEquals("Odia", M3UParser.detectLanguage("Prameya News7", "Prameya", "News", "prameya.in"))
        assertEquals("Hindi", M3UParser.detectLanguage("Aaj Tak", "AajTak", "News", "aajtak.in"))
        assertEquals("Tamil", M3UParser.detectLanguage("Sun TV", "SunTV", "Entertainment", "suntv.in"))
        assertEquals("Telugu", M3UParser.detectLanguage("TV9 Telugu", "TV9Telugu", "News", "tv9.in"))
        assertEquals("Bengali", M3UParser.detectLanguage("Aamar Bangla", "Aamar Bangla", "General", "aamar.in"))
        assertEquals("English", M3UParser.detectLanguage("Times Now World", "TimesNow", "News", "timesnow.in"))
    }

    @Test
    fun parse_largePlaylist_completesRapidly() {
        val stringBuilder = StringBuilder("#EXTM3U\n")
        for (i in 1..1000) {
            stringBuilder.append("#EXTINF:-1 tvg-id=\"ch$i\" tvg-logo=\"https://logo.com/$i.png\" group-title=\"Category ${(i % 10)}\",Channel $i (1080p)\n")
            stringBuilder.append("https://stream.host/live/ch$i/index.m3u8\n")
        }

        val startTime = System.currentTimeMillis()
        val channels = M3UParser.parse(ByteArrayInputStream(stringBuilder.toString().toByteArray()), playlistId = 10L)
        val elapsed = System.currentTimeMillis() - startTime

        assertEquals(1000, channels.size)
        // Should parse 1000 channels in well under 1000ms
        assertTrue("Parsing took too long: ${elapsed}ms", elapsed < 1000)
    }
}
