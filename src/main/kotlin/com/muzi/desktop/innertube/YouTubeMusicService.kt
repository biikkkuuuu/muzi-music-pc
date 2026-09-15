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

    // Piped / Invidious instances for YouTube Music live search and direct audio streams
    private val pipedInstances = listOf(
        "https://pipedapi.kavin.rocks",
        "https://api.piped.privacydev.net",
        "https://piped-api.garudalinux.org"
    )

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
        // Fetch trending Hindi/Punjabi/Pop songs directly from live backend
        search("Top Trending Hindi Songs").ifEmpty {
            listOf(
                Song(
                    id = "2",
                    title = "Pal Pal",
                    artist = "Talwiinder",
                    album = "Pal Pal",
                    durationText = "3:15",
                    durationSeconds = 195,
                    thumbnailUrl = "https://c.saavncdn.com/472/Pal-Pal-Hindi-2023-20230713180425-500x500.jpg",
                    streamUrl = null
                ),
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
    }

    suspend fun search(query: String): List<Song> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val encoded = URLEncoder.encode(query, "UTF-8")

        for (instance in pipedInstances) {
            try {
                val url = "$instance/search?q=$encoded&filter=music_songs"
                val response = client.get(url)
                if (response.status.value in 200..299) {
                    val body = response.bodyAsText()
                    val jsonElement = json.parseToJsonElement(body)
                    val items = jsonElement.jsonObject["items"]?.jsonArray ?: continue

                    val songList = items.mapNotNull { item ->
                        val obj = item.jsonObject
                        val rawUrl = obj["url"]?.jsonPrimitive?.contentOrNull ?: ""
                        val videoId = rawUrl.removePrefix("/watch?v=")
                        if (videoId.isEmpty()) return@mapNotNull null

                        val title = obj["title"]?.jsonPrimitive?.contentOrNull ?: "Unknown"
                        val uploaderName = obj["uploaderName"]?.jsonPrimitive?.contentOrNull ?: "Unknown Artist"
                        val duration = obj["duration"]?.jsonPrimitive?.contentOrNull ?: "3:30"
                        val thumbnail = obj["thumbnail"]?.jsonPrimitive?.contentOrNull
                            ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

                        Song(
                            id = videoId,
                            title = title,
                            artist = uploaderName,
                            album = "YouTube Music",
                            durationText = duration,
                            durationSeconds = parseDurationToSeconds(duration),
                            thumbnailUrl = thumbnail,
                            streamUrl = null // Will be resolved when clicked
                        )
                    }

                    if (songList.isNotEmpty()) {
                        return@withContext songList
                    }
                }
            } catch (_: Exception) {
                // Try next instance
            }
        }

        emptyList()
    }

    suspend fun resolveStreamUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        for (instance in pipedInstances) {
            try {
                val url = "$instance/streams/$videoId"
                val response = client.get(url)
                if (response.status.value in 200..299) {
                    val body = response.bodyAsText()
                    val jsonElement = json.parseToJsonElement(body)
                    val audioStreams = jsonElement.jsonObject["audioStreams"]?.jsonArray ?: continue

                    // Find best audio stream (m4a / mp3 / webm)
                    val bestStream = audioStreams.firstOrNull {
                        val mimeType = it.jsonObject["mimeType"]?.jsonPrimitive?.contentOrNull ?: ""
                        mimeType.contains("audio/mp4") || mimeType.contains("audio/m4a")
                    } ?: audioStreams.firstOrNull()

                    val streamUrl = bestStream?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull
                    if (!streamUrl.isNullOrBlank()) {
                        return@withContext streamUrl
                    }
                }
            } catch (_: Exception) {
                // Try next instance
            }
        }
        null
    }

    private fun parseDurationToSeconds(duration: String): Long {
        return try {
            val parts = duration.split(":")
            if (parts.size == 2) {
                parts[0].toLong() * 60 + parts[1].toLong()
            } else if (parts.size == 3) {
                parts[0].toLong() * 3600 + parts[1].toLong() * 60 + parts[2].toLong()
            } else {
                210L
            }
        } catch (_: Exception) {
            210L
        }
    }
}
