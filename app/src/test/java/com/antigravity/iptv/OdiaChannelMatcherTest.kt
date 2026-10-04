package com.antigravity.iptv

import com.antigravity.iptv.domain.model.Channel
import com.antigravity.iptv.domain.model.WatchHistoryItem
import com.antigravity.iptv.domain.util.OdiaChannelMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OdiaChannelMatcherTest {

    private fun createOdiaTestChannels(): List<Channel> {
        return listOf(
            Channel(
                id = 10L,
                playlistId = 1L,
                tvgId = "News7Tamil.in@SD",
                name = "News 7 Tamil (576p)",
                groupTitle = "News",
                streamUrl = "https://example.com/tamil.m3u8"
            ),
            Channel(
                id = 1L,
                playlistId = 1L,
                tvgId = "OdishaTV.in@SD",
                name = "Odisha TV (720p)",
                groupTitle = "Culture",
                streamUrl = "https://example.com/otv.m3u8"
            ),
            Channel(
                id = 2L,
                playlistId = 1L,
                tvgId = "KanakNews.in@SD",
                name = "Kanak News (576p)",
                groupTitle = "News",
                streamUrl = "https://example.com/kanak.m3u8"
            ),
            Channel(
                id = 3L,
                playlistId = 1L,
                tvgId = "KalingaTV.in@SD",
                name = "Kalinga TV (720p)",
                groupTitle = "News",
                streamUrl = "https://example.com/kalinga.m3u8"
            ),
            Channel(
                id = 4L,
                playlistId = 1L,
                tvgId = "News18Odia.in@SD",
                name = "News18 Odia (1080p)",
                groupTitle = "News",
                streamUrl = "https://example.com/news18odia.m3u8"
            ),
            Channel(
                id = 5L,
                playlistId = 1L,
                tvgId = "PrameyaNews7.in@SD",
                name = "Prameya News7 (576p)",
                groupTitle = "News",
                streamUrl = "https://example.com/prameya.m3u8"
            ),
            Channel(
                id = 6L,
                playlistId = 1L,
                tvgId = "DDOdia.in@SD",
                name = "DD Odia (576p)",
                groupTitle = "General",
                streamUrl = "https://example.com/ddodia.m3u8"
            ),
            Channel(
                id = 7L,
                playlistId = 1L,
                tvgId = "TarangMusic.in@SD",
                name = "Tarang Music (720p)",
                groupTitle = "Music",
                streamUrl = "https://example.com/tarangmusic.m3u8"
            ),
            Channel(
                id = 20L,
                playlistId = 1L,
                tvgId = "MetroTV.in@SD",
                name = "Metro TV (India) (1080p)",
                groupTitle = "General",
                streamUrl = "https://example.com/metrotv.m3u8"
            )
        )
    }

    @Test
    fun isOdiaChannel_accuratelyMatchesOdiaAndRejectsNonOdia() {
        val channels = createOdiaTestChannels()

        // Odia channels should match
        assertTrue(OdiaChannelMatcher.isOdiaChannel(channels.first { it.id == 1L })) // Odisha TV / OTV
        assertTrue(OdiaChannelMatcher.isOdiaChannel(channels.first { it.id == 2L })) // Kanak News
        assertTrue(OdiaChannelMatcher.isOdiaChannel(channels.first { it.id == 3L })) // Kalinga TV
        assertTrue(OdiaChannelMatcher.isOdiaChannel(channels.first { it.id == 4L })) // News18 Odia
        assertTrue(OdiaChannelMatcher.isOdiaChannel(channels.first { it.id == 5L })) // Prameya News7
        assertTrue(OdiaChannelMatcher.isOdiaChannel(channels.first { it.id == 6L })) // DD Odia
        assertTrue(OdiaChannelMatcher.isOdiaChannel(channels.first { it.id == 7L })) // Tarang Music

        // Non-Odia false positives MUST NOT match
        assertFalse("News 7 Tamil must not match Odia News7", OdiaChannelMatcher.isOdiaChannel(channels.first { it.id == 10L }))
        assertFalse("Metro TV must not match OTV", OdiaChannelMatcher.isOdiaChannel(channels.first { it.id == 20L }))
    }

    @Test
    fun sortOdiaChannels_newUserWithNoHistory_usesCuratedPriorityOrder() {
        val channels = createOdiaTestChannels()
        val sorted = OdiaChannelMatcher.sortOdiaChannels(
            channels = channels,
            watchHistory = emptyList(),
            favorites = emptyList()
        )

        // Only Odia channels are in sorted list (no Tamil, no Metro TV)
        assertEquals(7, sorted.size)

        // 1. OTV (id 1)
        assertEquals(1L, sorted[0].id)
        // 2. Kanak News (id 2)
        assertEquals(2L, sorted[1].id)
        // 3. Kalinga TV (id 3)
        assertEquals(3L, sorted[2].id)
        // 4. News18 Odia (id 4)
        assertEquals(4L, sorted[3].id)
        // 5. Prameya News7 (id 5)
        assertEquals(5L, sorted[4].id)
        // 6. DD Odia (id 6)
        assertEquals(6L, sorted[5].id)
        // 7. Tarang Music (id 7)
        assertEquals(7L, sorted[6].id)
    }

    @Test
    fun sortOdiaChannels_withFavorites_elevatesFavoritesToTheTop() {
        val channels = createOdiaTestChannels()
        // User favorited Tarang Music (id 7)
        val favoriteTarang = channels.first { it.id == 7L }.copy(isFavorite = true)

        val sorted = OdiaChannelMatcher.sortOdiaChannels(
            channels = channels,
            watchHistory = emptyList(),
            favorites = listOf(favoriteTarang)
        )

        // Tarang Music must be first because it is favorited!
        assertEquals(7L, sorted[0].id)
        // OTV follows next
        assertEquals(1L, sorted[1].id)
    }

    @Test
    fun sortOdiaChannels_withWatchHistory_prioritizesWatchedChannelsAfterFavorites() {
        val channels = createOdiaTestChannels()
        // User favorited DD Odia (id 6)
        val fav = channels.first { it.id == 6L }.copy(isFavorite = true)
        // User recently watched Kalinga TV (id 3)
        val history = listOf(
            WatchHistoryItem(
                id = 99L,
                channelId = 3L,
                channelName = "Kalinga TV",
                channelLogo = "",
                streamUrl = "",
                groupTitle = "News",
                playlistId = 1L,
                watchedAt = 50000L
            )
        )

        val sorted = OdiaChannelMatcher.sortOdiaChannels(
            channels = channels,
            watchHistory = history,
            favorites = listOf(fav)
        )

        // Position 0: Favorite (DD Odia)
        assertEquals(6L, sorted[0].id)
        // Position 1: Recently Watched (Kalinga TV)
        assertEquals(3L, sorted[1].id)
        // Position 2: Next curated (OTV)
        assertEquals(1L, sorted[2].id)
    }

    @Test
    fun sortOdiaChannels_emptyList_returnsEmpty() {
        val sorted = OdiaChannelMatcher.sortOdiaChannels(
            channels = emptyList(),
            watchHistory = emptyList(),
            favorites = emptyList()
        )
        assertTrue(sorted.isEmpty())
    }
}
