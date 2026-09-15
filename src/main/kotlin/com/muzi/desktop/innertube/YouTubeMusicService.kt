package com.muzi.desktop.innertube

import com.muzi.desktop.model.ChartItem
import com.muzi.desktop.model.Song
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

object YouTubeMusicService {
    private val client = HttpClient(OkHttp)
    private val json = Json { ignoreUnknownKeys = true }

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
        val searchPicks = search("Hindi Trending Songs")
        if (searchPicks.isNotEmpty()) searchPicks else listOf(
            Song(
                id = "1",
                title = "Sahiba",
                artist = "Aditya Rikhari",
                album = "Sahiba",
                durationText = "3:40",
                durationSeconds = 220,
                thumbnailUrl = "https://c.saavncdn.com/264/Sahiba-Hindi-2024-20240320144026-500x500.jpg",
                streamUrl = null
            )
        )
    }

    // Direct InnerTube YouTube Music Search - Exactly like Android App (WEB_REMIX Client)
    suspend fun search(query: String): List<Song> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val url = "https://music.youtube.com/youtubei/v1/search"
            val bodyPayload = """
            {
                "context": {
                    "client": {
                        "clientName": "WEB_REMIX",
                        "clientVersion": "1.20260213.01.00",
                        "hl": "en",
                        "gl": "IN"
                    }
                },
                "query": "${query.replace("\"", "\\\"")}",
                "params": "Eg-KAQwIABAAGAAgACgAMABqChAEEAMQCRAFEAo%3D"
            }
            """.trimIndent()

            val response = client.post(url) {
                contentType(ContentType.Application.Json)
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                header("Origin", "https://music.youtube.com")
                header("Referer", "https://music.youtube.com/")
                setBody(bodyPayload)
            }

            if (response.status.value in 200..299) {
                val responseText = response.bodyAsText()
                val jsonElement = json.parseToJsonElement(responseText)
                val results = parseInnerTubeSearchResults(jsonElement)
                if (results.isNotEmpty()) {
                    return@withContext results
                }
            }
        } catch (_: Exception) {}

        // Fallback to JioSaavn API for instant direct Indian/International high-speed streams
        try {
            val encoded = java.net.URLEncoder.encode(query, "UTF-8")
            val saavnUrl = "https://saavn.dev/api/search/songs?query=$encoded&limit=20"
            val response = client.get(saavnUrl)
            if (response.status.value in 200..299) {
                val body = response.bodyAsText()
                val jsonElement = json.parseToJsonElement(body).jsonObject
                val data = jsonElement["data"]?.jsonObject
                val results = data?.get("results")?.jsonArray

                if (results != null && results.isNotEmpty()) {
                    return@withContext results.mapNotNull { item ->
                        val obj = item.jsonObject
                        val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                        val name = obj["name"]?.jsonPrimitive?.contentOrNull?.replace("&quot;", "\"")?.replace("&#039;", "'") ?: "Unknown"
                        val artist = obj["artists"]?.jsonObject?.get("primary")?.jsonArray?.firstOrNull()?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull ?: "Unknown Artist"
                        val duration = obj["duration"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 210
                        val min = duration / 60
                        val sec = duration % 60
                        val durationText = "%d:%02d".format(min, sec)

                        val images = obj["image"]?.jsonArray
                        val thumb = images?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull ?: ""

                        val downloadUrls = obj["downloadUrl"]?.jsonArray
                        // High quality 320kbps MP3 direct stream
                        val stream = downloadUrls?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull

                        Song(
                            id = id,
                            title = name,
                            artist = artist,
                            album = obj["album"]?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull ?: "",
                            durationText = durationText,
                            durationSeconds = duration.toLong(),
                            thumbnailUrl = thumb,
                            streamUrl = stream
                        )
                    }
                }
            }
        } catch (_: Exception) {}

        emptyList()
    }

    private fun parseInnerTubeSearchResults(root: JsonElement): List<Song> {
        val songs = mutableListOf<Song>()
        try {
            val contents = root.jsonObject["contents"]
                ?.jsonObject?.get("tabbedSearchResultsRenderer")
                ?.jsonObject?.get("tabs")
                ?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("tabRenderer")
                ?.jsonObject?.get("content")
                ?.jsonObject?.get("sectionListRenderer")
                ?.jsonObject?.get("contents")
                ?.jsonArray ?: return emptyList()

            for (section in contents) {
                val shelf = section.jsonObject["musicShelfRenderer"]?.jsonObject ?: continue
                val items = shelf["contents"]?.jsonArray ?: continue

                for (item in items) {
                    val renderer = item.jsonObject["musicResponsiveListItemRenderer"]?.jsonObject ?: continue
                    val flexColumns = renderer["flexColumns"]?.jsonArray ?: continue

                    // Title
                    val titleRuns = flexColumns.getOrNull(0)?.jsonObject
                        ?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObject
                        ?.get("text")?.jsonObject?.get("runs")?.jsonArray

                    val title = titleRuns?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull ?: continue
                    val videoId = titleRuns.firstOrNull()?.jsonObject?.get("navigationEndpoint")?.jsonObject
                        ?.get("watchEndpoint")?.jsonObject?.get("videoId")?.jsonPrimitive?.contentOrNull
                        ?: renderer["playlistItemData"]?.jsonObject?.get("videoId")?.jsonPrimitive?.contentOrNull
                        ?: continue

                    // Artist & Duration
                    val subtitleRuns = flexColumns.getOrNull(1)?.jsonObject
                        ?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObject
                        ?.get("text")?.jsonObject?.get("runs")?.jsonArray

                    val artist = subtitleRuns?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull ?: "Unknown Artist"
                    val durationText = subtitleRuns?.lastOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull ?: "3:30"

                    // Thumbnail
                    val thumbnails = renderer["thumbnail"]?.jsonObject
                        ?.get("musicItemThumbnailOverlayRenderer")?.jsonObject
                        ?.get("thumbnail")?.jsonObject?.get("musicThumbnailRenderer")?.jsonObject
                        ?.get("thumbnail")?.jsonObject?.get("thumbnails")?.jsonArray
                        ?: renderer["thumbnail"]?.jsonObject?.get("musicThumbnailRenderer")?.jsonObject
                            ?.get("thumbnail")?.jsonObject?.get("thumbnails")?.jsonArray

                    val thumbnailUrl = thumbnails?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull
                        ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

                    songs.add(
                        Song(
                            id = videoId,
                            title = title,
                            artist = artist,
                            album = "YouTube Music",
                            durationText = durationText,
                            durationSeconds = 210,
                            thumbnailUrl = thumbnailUrl,
                            streamUrl = null
                        )
                    )
                }
            }
        } catch (_: Exception) {}
        return songs
    }
}
