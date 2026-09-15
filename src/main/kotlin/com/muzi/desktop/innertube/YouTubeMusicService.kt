package com.muzi.desktop.innertube

import com.music.innertube.NewPipeDownloaderImpl
import com.music.innertube.NewPipeUtils
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
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

    // Exact Playback Stream Resolution as Android Muzi App using NewPipeExtractor
    suspend fun resolveStreamUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        ensureNewPipe()
        try {
            val streamInfo = StreamInfo.getInfo(
                NewPipe.getService(0),
                "https://www.youtube.com/watch?v=$videoId"
            )
            val audioStreams = streamInfo.audioStreams
            val bestAudio = audioStreams.maxByOrNull { it.averageBitrate } ?: audioStreams.firstOrNull()
            if (bestAudio != null && !bestAudio.content.isNullOrBlank()) {
                return@withContext bestAudio.content
            }
        } catch (_: Exception) {}

        try {
            val playerResponse = YouTube.player(videoId = videoId, client = com.music.innertube.models.YouTubeClient.WEB_REMIX).getOrNull()
            val format = playerResponse?.streamingData?.adaptiveFormats?.firstOrNull {
                it.mimeType.startsWith("audio/")
            } ?: playerResponse?.streamingData?.formats?.firstOrNull()

            if (format != null) {
                val downloader = NewPipeDownloaderImpl(YouTube.proxy, YouTube.proxyAuth)
                val utils = NewPipeUtils(downloader)
                return@withContext utils.getStreamUrl(format, videoId) ?: format.url
            }
        } catch (_: Exception) {}

        null
    }
}
