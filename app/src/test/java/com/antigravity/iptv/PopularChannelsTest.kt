package com.antigravity.iptv

import com.antigravity.iptv.domain.model.Channel
import com.antigravity.iptv.domain.model.WatchHistoryItem
import com.antigravity.iptv.ui.home.HomeViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PopularChannelsTest {

    private fun createSampleChannels(): List<Channel> {
        return listOf(
            Channel(
                id = 1L,
                playlistId = 1L,
                name = "Aaj Tak (1080p)",
                logoUrl = "https://example.com/aajtak.png",
                groupTitle = "News",
                language = "Hindi",
                streamUrl = "https://stream.example.com/aajtak.m3u8"
            ),
            Channel(
                id = 2L,
                playlistId = 1L,
                name = "9XM (1080p)",
                logoUrl = "https://example.com/9xm.png",
                groupTitle = "Music",
                language = "Hindi",
                streamUrl = "https://stream.example.com/9xm.m3u8"
            ),
            Channel(
                id = 3L,
                playlistId = 1L,
                name = "&TV HD (1080p)",
                logoUrl = "https://example.com/andtv.png",
                groupTitle = "Entertainment",
                language = "Hindi",
                streamUrl = "https://stream.example.com/andtv.m3u8"
            ),
            Channel(
                id = 4L,
                playlistId = 1L,
                name = "NDTV 24x7 (720p)",
                logoUrl = "https://example.com/ndtv.png",
                groupTitle = "News",
                language = "English",
                streamUrl = "https://stream.example.com/ndtv.m3u8"
            ),
            Channel(
                id = 5L,
                playlistId = 1L,
                name = "Local Cable News",
                logoUrl = "",
                groupTitle = "Local",
                language = "",
                streamUrl = "https://stream.example.com/local.m3u8"
            )
        )
    }

    @Test
    fun popularChannels_newUserWithNoHistory_selectsRealChannelsFromLoadedPlaylist() {
        val allChannels = createSampleChannels()
        val emptyHistory = emptyList<WatchHistoryItem>()
        val emptyFavorites = emptyList<Channel>()

        val popular = HomeViewModel.computePopularChannels(
            allChannels = allChannels,
            history = emptyHistory,
            favorites = emptyFavorites
        )

        // Verify all returned channels are real channels from the playlist
        assertTrue(popular.isNotEmpty())
        assertTrue(popular.all { ch -> allChannels.any { it.id == ch.id } })

        // No fake IDs or fake names
        popular.forEach { ch ->
            val match = allChannels.first { it.id == ch.id }
            assertEquals(match.name, ch.name)
            assertEquals(match.streamUrl, ch.streamUrl)
            assertEquals(match.groupTitle, ch.groupTitle)
        }

        // Channels with logos & recognized keywords are prioritized
        assertTrue(popular.first().logoUrl.isNotBlank())
    }

    @Test
    fun popularChannels_withUserHistory_ranksMostWatchedChannelsFirst() {
        val allChannels = createSampleChannels()
        val history = listOf(
            WatchHistoryItem(id = 101L, channelId = 4L, channelName = "NDTV", channelLogo = "", streamUrl = "", groupTitle = "", playlistId = 1L, watchedAt = 1000L),
            WatchHistoryItem(id = 102L, channelId = 4L, channelName = "NDTV", channelLogo = "", streamUrl = "", groupTitle = "", playlistId = 1L, watchedAt = 2000L),
            WatchHistoryItem(id = 103L, channelId = 4L, channelName = "NDTV", channelLogo = "", streamUrl = "", groupTitle = "", playlistId = 1L, watchedAt = 3000L),
            WatchHistoryItem(id = 104L, channelId = 2L, channelName = "9XM", channelLogo = "", streamUrl = "", groupTitle = "", playlistId = 1L, watchedAt = 1500L)
        )
        val favorites = emptyList<Channel>()

        val popular = HomeViewModel.computePopularChannels(
            allChannels = allChannels,
            history = history,
            favorites = favorites
        )

        // NDTV (id=4) was watched 3 times, so it must be ranked first
        assertEquals(4L, popular[0].id)
        // 9XM (id=2) was watched 1 time, so it must be second
        assertEquals(2L, popular[1].id)
    }

    @Test
    fun popularChannels_emptyPlaylist_returnsEmpty() {
        val popular = HomeViewModel.computePopularChannels(
            allChannels = emptyList(),
            history = emptyList(),
            favorites = emptyList()
        )
        assertTrue(popular.isEmpty())
    }

    @Test
    fun channelDisplayName_cleansResolutionTagsProperly() {
        val channel = Channel(
            id = 1L,
            playlistId = 1L,
            name = "Aaj Tak (1080p)",
            groupTitle = "News",
            streamUrl = "https://example.com/test.m3u8"
        )
        assertEquals("Aaj Tak", channel.displayName)
        assertEquals("1080p", channel.cleanResolution)
    }
}
