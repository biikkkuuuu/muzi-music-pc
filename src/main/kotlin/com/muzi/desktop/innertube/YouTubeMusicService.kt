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
        val searchPicks = search("Hindi Trending Songs")
        if (searchPicks.isNotEmpty()) searchPicks else listOf(
            Song(
                id = "1",
                title = "Sahiba",
                artist = "Aditya Rikhari",
                album = "Sahiba",
                durationText = "3:40",
                durationSeconds = 220,
                thumbnailUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music113/v4/b2/9f/45/b29f4582-a1a2-ec02-ee7d-21bef3346547/8718857677529.png/500x500bb.jpg",
                streamUrl = "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview221/v4/2e/43/de/2e43de6d-8347-233c-c55b-5e63180f453c/mzaf_11408243760072457560.plus.aac.p.m4a"
            )
        )
    }

    suspend fun search(query: String): List<Song> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val encoded = URLEncoder.encode(query, "UTF-8")

        try {
            val itunesUrl = "https://itunes.apple.com/search?term=$encoded&entity=song&limit=25"
            val response = client.get(itunesUrl)
            if (response.status.value in 200..299) {
                val body = response.bodyAsText()
                val jsonElement = json.parseToJsonElement(body).jsonObject
                val results = jsonElement["results"]?.jsonArray
                if (results != null && results.isNotEmpty()) {
                    return@withContext results.mapNotNull { item ->
                        val obj = item.jsonObject
                        val trackId = obj["trackId"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                        val trackName = obj["trackName"]?.jsonPrimitive?.contentOrNull ?: "Unknown"
                        val artistName = obj["artistName"]?.jsonPrimitive?.contentOrNull ?: "Unknown Artist"
                        val previewUrl = obj["previewUrl"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                        val artwork = obj["artworkUrl100"]?.jsonPrimitive?.contentOrNull?.replace("100x100bb", "500x500bb") ?: ""
                        val durationMillis = obj["trackTimeMillis"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 210000L
                        val durationSeconds = durationMillis / 1000
                        val min = durationSeconds / 60
                        val sec = durationSeconds % 60
                        val durationText = "%d:%02d".format(min, sec)

                        Song(
                            id = trackId,
                            title = trackName,
                            artist = artistName,
                            album = obj["collectionName"]?.jsonPrimitive?.contentOrNull ?: "Single",
                            durationText = durationText,
                            durationSeconds = durationSeconds,
                            thumbnailUrl = artwork,
                            streamUrl = previewUrl
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        emptyList()
    }
}
