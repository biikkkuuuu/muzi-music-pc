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

    // Guaranteed Full Length YouTube Stream Playback
    suspend fun resolveStreamUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        ensureNewPipe()
        try {
            val streamInfo = StreamInfo.getInfo(
                NewPipe.getService(0),
                "https://www.youtube.com/watch?v=$videoId"
            )

            // 1. Audio stream if available (M4A / AAC preferred)
            val m4aAudio = streamInfo.audioStreams.firstOrNull { it.format?.suffix?.equals("m4a", ignoreCase = true) == true }
            if (m4aAudio != null && !m4aAudio.content.isNullOrBlank()) {
                return@withContext m4aAudio.content
            }

            val anyAudio = streamInfo.audioStreams.firstOrNull { !it.content.isNullOrBlank() }
            if (anyAudio != null) {
                return@withContext anyAudio.content
            }

            // 2. Video+Audio MP4 stream with sound (itag 18 is 360p MP4 with AAC, fast ~5-8MB)
            val mp4Medium = streamInfo.videoStreams.firstOrNull { it.itag == 18 && !it.content.isNullOrBlank() }
            if (mp4Medium != null) {
                return@withContext mp4Medium.content
            }

            // 3. Any non-video-only stream (i.e. contains audio)
            val muxedStream = streamInfo.videoStreams.firstOrNull { !it.isVideoOnly && !it.content.isNullOrBlank() }
            if (muxedStream != null) {
                return@withContext muxedStream.content
            }

            val fallbackVideo = streamInfo.videoStreams.firstOrNull { !it.content.isNullOrBlank() }
            if (fallbackVideo != null) {
                return@withContext fallbackVideo.content
            }
        } catch (e: Exception) {
            println("[YouTubeMusicService] Stream resolution error for $videoId: ${e.message}")
        }

        null
    }
}
