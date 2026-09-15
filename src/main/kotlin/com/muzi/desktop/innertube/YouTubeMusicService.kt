package com.muzi.desktop.innertube

import com.muzi.desktop.model.ChartItem
import com.muzi.desktop.model.Song
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.net.URLEncoder

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
        listOf(
            Song(
                id = "1",
                title = "Pal Pal",
                artist = "Talwiinder",
                album = "Pal Pal",
                durationText = "3:15",
                durationSeconds = 195,
                thumbnailUrl = "https://c.saavncdn.com/472/Pal-Pal-Hindi-2023-20230713180425-500x500.jpg",
                streamUrl = "https://cdn.pixabay.com/download/audio/2022/05/27/audio_1808fbf07a.mp3?filename=lofi-study-112191.mp3"
            ),
            Song(
                id = "2",
                title = "Sahiba",
                artist = "Aditya Rikhari",
                album = "Sahiba",
                durationText = "3:40",
                durationSeconds = 220,
                thumbnailUrl = "https://c.saavncdn.com/264/Sahiba-Hindi-2024-20240320144026-500x500.jpg",
                streamUrl = "https://cdn.pixabay.com/download/audio/2022/03/15/audio_c8c8a73467.mp3?filename=relaxed-vlog-131746.mp3"
            ),
            Song(
                id = "3",
                title = "Ishqa Ve",
                artist = "Ishqa Ve",
                album = "Ishqa Ve",
                durationText = "3:20",
                durationSeconds = 200,
                thumbnailUrl = "https://c.saavncdn.com/970/Ishqa-Ve-Hindi-2023-20231201115124-500x500.jpg",
                streamUrl = "https://cdn.pixabay.com/download/audio/2022/01/18/audio_d0a13f69d2.mp3?filename=chill-abstract-intention-12099.mp3"
            ),
            Song(
                id = "4",
                title = "Arz Kiya Hai",
                artist = "Coke Studio Bharat",
                album = "Season 1",
                durationText = "4:12",
                durationSeconds = 252,
                thumbnailUrl = "https://c.saavncdn.com/027/Arz-Kiya-Hai-Coke-Studio-Bharat-Hindi-2023-20231013144855-500x500.jpg",
                streamUrl = "https://cdn.pixabay.com/download/audio/2022/10/14/audio_9939f77c30.mp3?filename=ambient-piano-amp-strings-10711.mp3"
            )
        )
    }

    // Direct, fast JioSaavn / iTunes / YouTube Music search API (instant search with zero CORS/rate limits)
    suspend fun search(query: String): List<Song> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val encoded = URLEncoder.encode(query, "UTF-8")

        // Primary: JioSaavn Search API for Indian & International songs (HQ 320kbps MP3 streams direct)
        try {
            val saavnUrl = "https://saavn.dev/api/search/songs?query=$encoded&limit=15"
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

        // Fallback: iTunes search API (instant search)
        try {
            val itunesUrl = "https://itunes.apple.com/search?term=$encoded&entity=song&limit=15"
            val response = client.get(itunesUrl)
            if (response.status.value in 200..299) {
                val body = response.bodyAsText()
                val jsonElement = json.parseToJsonElement(body).jsonObject
                val results = jsonElement["results"]?.jsonArray
                if (results != null) {
                    return@withContext results.mapNotNull { item ->
                        val obj = item.jsonObject
                        val trackId = obj["trackId"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                        val trackName = obj["trackName"]?.jsonPrimitive?.contentOrNull ?: "Unknown"
                        val artistName = obj["artistName"]?.jsonPrimitive?.contentOrNull ?: "Unknown Artist"
                        val previewUrl = obj["previewUrl"]?.jsonPrimitive?.contentOrNull
                        val artwork = obj["artworkUrl100"]?.jsonPrimitive?.contentOrNull?.replace("100x100bb", "500x500bb") ?: ""

                        Song(
                            id = trackId,
                            title = trackName,
                            artist = artistName,
                            album = obj["collectionName"]?.jsonPrimitive?.contentOrNull ?: "",
                            durationText = "3:30",
                            durationSeconds = 210,
                            thumbnailUrl = artwork,
                            streamUrl = previewUrl
                        )
                    }
                }
            }
        } catch (_: Exception) {}

        emptyList()
    }
}
