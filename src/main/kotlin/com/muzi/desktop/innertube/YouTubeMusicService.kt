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
                title = "Sahiba",
                artist = "Aditya Rikhari",
                album = "Sahiba",
                durationText = "3:40",
                durationSeconds = 220,
                thumbnailUrl = "https://c.saavncdn.com/264/Sahiba-Hindi-2024-20240320144026-500x500.jpg",
                streamUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Kangaroo_MusiQue_-_The_Neverending_Story.mp3"
            ),
            Song(
                id = "2",
                title = "Pal Pal",
                artist = "Talwiinder",
                album = "Pal Pal",
                durationText = "3:15",
                durationSeconds = 195,
                thumbnailUrl = "https://c.saavncdn.com/472/Pal-Pal-Hindi-2023-20230713180425-500x500.jpg",
                streamUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__nbsp_.mp3"
            ),
            Song(
                id = "3",
                title = "Ishqa Ve",
                artist = "Ishqa Ve",
                album = "Ishqa Ve",
                durationText = "3:20",
                durationSeconds = 200,
                thumbnailUrl = "https://c.saavncdn.com/970/Ishqa-Ve-Hindi-2023-20231201115124-500x500.jpg",
                streamUrl = "https://commondatastorage.googleapis.com/codeskulptor-assets/Epoq-Lepidoptera.ogg"
            ),
            Song(
                id = "4",
                title = "Arz Kiya Hai",
                artist = "Coke Studio Bharat",
                album = "Season 1",
                durationText = "4:12",
                durationSeconds = 252,
                thumbnailUrl = "https://c.saavncdn.com/027/Arz-Kiya-Hai-Coke-Studio-Bharat-Hindi-2023-20231013144855-500x500.jpg",
                streamUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Kangaroo_MusiQue_-_The_Neverending_Story.mp3"
            )
        )
    }

    suspend fun search(query: String): List<Song> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        listOf(
            Song(
                id = "s1",
                title = query,
                artist = "Top Artist",
                album = "Latest Single",
                durationText = "3:30",
                durationSeconds = 210,
                thumbnailUrl = "https://c.saavncdn.com/264/Sahiba-Hindi-2024-20240320144026-500x500.jpg",
                streamUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Kangaroo_MusiQue_-_The_Neverending_Story.mp3"
            )
        )
    }
}
