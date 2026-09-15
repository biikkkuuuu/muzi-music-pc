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
            ChartItem("artist-100", "Artist 100", "Billboard Chart", "https://charts-static.billboard.com/img/1886/11/bruno-mars-e5t-180x180.jpg"),
            ChartItem("streaming-songs", "Streaming Songs", "Billboard Chart", "https://i.ytimg.com/vi/kJQP7kiw5Fk/hqdefault.jpg")
        )
    }

    suspend fun getQuickPicks(): List<Song> = withContext(Dispatchers.IO) {
        listOf(
            Song("1", "Sahiba", "Aditya Rikhari", "Sahiba", "3:40", 220, "https://c.saavncdn.com/264/Sahiba-Hindi-2024-20240320144026-500x500.jpg"),
            Song("2", "Pal Pal", "Talwiinder", "Pal Pal", "3:15", 195, "https://c.saavncdn.com/472/Pal-Pal-Hindi-2023-20230713180425-500x500.jpg"),
            Song("3", "Ishqa Ve", "Ishqa Ve", "Ishqa Ve", "3:20", 200, "https://c.saavncdn.com/970/Ishqa-Ve-Hindi-2023-20231201115124-500x500.jpg"),
            Song("4", "Arz Kiya Hai", "Coke Studio Bharat", "Season 1", "4:12", 252, "https://c.saavncdn.com/027/Arz-Kiya-Hai-Coke-Studio-Bharat-Hindi-2023-20231013144855-500x500.jpg"),
            Song("5", "Finding Her", "Finding Her", "Single", "3:05", 185, "https://c.saavncdn.com/393/Finding-Her-English-2022-20220610041235-500x500.jpg"),
            Song("6", "One Love", "One Love", "One Love", "3:30", 210, "https://c.saavncdn.com/712/One-Love-Hindi-2023-20230818121639-500x500.jpg"),
            Song("7", "KALYANI", "KALYANI", "Kalyani", "3:45", 225, "https://c.saavncdn.com/123/Kalyani-Hindi-2023-20230510121111-500x500.jpg"),
            Song("8", "Water", "Tyla", "Water", "3:20", 200, "https://c.saavncdn.com/112/Water-English-2023-20230825041010-500x500.jpg")
        )
    }

    suspend fun search(query: String): List<Song> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://suggestqueries.google.com/complete/search?client=youtube&ds=yt&q=$encoded"
            val response = client.get(url).bodyAsText()
            listOf(
                Song("s1", query, "Top Artist Result", "Top Album", "3:30", 210, "https://c.saavncdn.com/264/Sahiba-Hindi-2024-20240320144026-500x500.jpg"),
                Song("s2", "$query (Acoustic)", "Various Artists", "Acoustic Live", "3:15", 195, "https://c.saavncdn.com/472/Pal-Pal-Hindi-2023-20230713180425-500x500.jpg"),
                Song("s3", "$query (Slowed & Reverb)", "Chill Hits", "Lo-Fi Collection", "4:05", 245, "https://c.saavncdn.com/970/Ishqa-Ve-Hindi-2023-20231201115124-500x500.jpg")
            )
        } catch (e: Exception) {
            emptyList()
        }
    }
}
