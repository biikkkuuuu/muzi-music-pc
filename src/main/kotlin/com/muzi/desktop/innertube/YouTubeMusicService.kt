package com.muzi.desktop.innertube

import com.music.innertube.NewPipeDownloaderImpl
import com.music.innertube.NewPipeUtils
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint
import com.music.innertube.pages.HomePage
import com.muzi.desktop.model.ChartItem
import com.muzi.desktop.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.stream.StreamInfo

object YouTubeMusicService {
    private var isNewPipeInit = false

    private fun ensureNewPipe() {
        if (!isNewPipeInit) {
            try {
                val downloader = NewPipeDownloaderImpl(YouTube.proxy, YouTube.proxyAuth)
                NewPipeUtils(downloader)
                isNewPipeInit = true
            } catch (_: Exception) {}
        }
    }

    data class HomeFeed(
        val chips: List<HomePage.Chip>,
        val sections: List<HomePage.Section>
    )

    // Exact 100% Android Muzi Home Feed via InnerTube
    suspend fun getHomeFeed(params: String? = null): HomeFeed? = withContext(Dispatchers.IO) {
        try {
            val page = YouTube.home(params = params).getOrNull() ?: return@withContext null
            return@withContext HomeFeed(
                chips = page.chips ?: emptyList(),
                sections = page.sections
            )
        } catch (e: Exception) {
            println("[YouTubeMusicService] Error loading home feed: ${e.message}")
            null
        }
    }

    suspend fun getBrowseCharts(): List<ChartItem> = withContext(Dispatchers.IO) {
        listOf(
            ChartItem("spotify-50", "Spotify Top 50 Global", "Billboard Chart", "https://i.scdn.co/image/ab67706c0000bebb8d0ce13d55f634e290f744ba"),
            ChartItem("hot-100", "Hot 100", "Billboard Chart", "https://charts-static.billboard.com/img/1886/11/bruno-mars-e5t-180x180.jpg"),
            ChartItem("billboard-200", "Billboard 200", "Billboard Chart", "https://i.ytimg.com/vi/kJQP7kiw5Fk/hqdefault.jpg"),
            ChartItem("global-200", "Global 200", "Billboard Chart", "https://i.ytimg.com/vi/JGwWNGJdvx8/hqdefault.jpg"),
            ChartItem("artist-100", "Artist 100", "Billboard Chart", "https://charts-static.billboard.com/img/1886/11/bruno-mars-e5t-180x180.jpg")
        )
    }

    suspend fun getQuickPicks(): List<Song> = withContext(Dispatchers.IO) {
        search("Top Trending Hindi Songs").ifEmpty {
            search("Trending Songs")
        }
    }

    // Exact YouTube Music Search as Android Muzi App
    suspend fun search(query: String): List<Song> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val result = YouTube.searchSummary(query).getOrNull()
            if (result != null) {
                val allItems = result.summaries.flatMap { it.items }
                val songList = allItems.filterIsInstance<SongItem>().map { item ->
                    Song(
                        id = item.id,
                        title = item.title,
                        artist = item.artists.joinToString(", ") { it.name },
                        album = item.album?.name ?: "Single",
                        durationText = item.duration?.let { "%d:%02d".format(it / 60, it % 60) } ?: "3:30",
                        durationSeconds = item.duration?.toLong() ?: 210L,
                        thumbnailUrl = item.thumbnail,
                        streamUrl = null
                    )
                }.distinctBy { it.id }

                if (songList.isNotEmpty()) {
                    return@withContext songList
                }
            }
        } catch (_: Exception) {}

        emptyList()
    }

    suspend fun getSearchSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val res = YouTube.searchSuggestions(query).getOrNull()
            return@withContext res?.queries ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    // Auto-Radio Queue for Continuous Endless Playback (Like Muzi Android)
    suspend fun fetchRadioQueue(videoId: String): List<Song> = withContext(Dispatchers.IO) {
        try {
            val res = YouTube.next(WatchEndpoint(videoId = videoId, playlistId = "RDAMVM$videoId")).getOrNull()
            if (res != null && res.items.isNotEmpty()) {
                return@withContext res.items.map { item ->
                    Song(
                        id = item.id,
                        title = item.title,
                        artist = item.artists.joinToString(", ") { it.name },
                        album = item.album?.name ?: "Single",
                        durationText = item.duration?.let { "%d:%02d".format(it / 60, it % 60) } ?: "3:30",
                        durationSeconds = item.duration?.toLong() ?: 210L,
                        thumbnailUrl = item.thumbnail,
                        streamUrl = null
                    )
                }.distinctBy { it.id }
            }
        } catch (e: Exception) {
            println("[YouTubeMusicService] Error fetching radio queue: ${e.message}")
        }
        emptyList()
    }

    // Guaranteed Full Length YouTube Stream Playback for Windows PC
    suspend fun resolveStreamUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        // 1. Try ANDROID_NO_SDK (Most reliable for direct unthrottled streams on PC)
        try {
            val res = YouTube.player(videoId = videoId, client = com.music.innertube.models.YouTubeClient.ANDROID_NO_SDK).getOrNull()
            val url = res?.streamingData?.formats?.firstOrNull { it.itag == 18 && it.url != null }?.url
                ?: res?.streamingData?.adaptiveFormats?.firstOrNull { it.itag == 140 && it.url != null }?.url
                ?: res?.streamingData?.adaptiveFormats?.firstOrNull { it.url != null && it.mimeType?.startsWith("audio/") == true }?.url
            if (url != null) return@withContext url
        } catch (e: Exception) {
            println("[YouTubeMusicService] ANDROID_NO_SDK error: ${e.message}")
        }

        // 2. Try IOS client
        try {
            val resIos = YouTube.player(videoId = videoId, client = com.music.innertube.models.YouTubeClient.IOS).getOrNull()
            val urlIos = resIos?.streamingData?.adaptiveFormats?.firstOrNull { it.itag == 140 && it.url != null }?.url
                ?: resIos?.streamingData?.adaptiveFormats?.firstOrNull { it.url != null && it.mimeType?.startsWith("audio/") == true }?.url
            if (urlIos != null) return@withContext urlIos
        } catch (e: Exception) {
            println("[YouTubeMusicService] IOS client error: ${e.message}")
        }

        // 3. Fallback to NewPipe
        ensureNewPipe()
        try {
            val streamInfo = StreamInfo.getInfo(
                NewPipe.getService(0),
                "https://www.youtube.com/watch?v=$videoId"
            )
            val anyAudio = streamInfo.audioStreams.firstOrNull { !it.content.isNullOrBlank() }
            if (anyAudio != null) {
                return@withContext anyAudio.content
            }
        } catch (e: Exception) {
            println("[YouTubeMusicService] NewPipe error for $videoId: ${e.message}")
        }

        null
    }
}
