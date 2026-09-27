package com.muzi.desktop.lyrics

import com.muzi.desktop.model.LyricsLine
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.net.URLEncoder

object LyricsProvider {
    private val client = HttpClient(OkHttp)
    private val json = Json { ignoreUnknownKeys = true }
    private val cache = mutableMapOf<String, List<LyricsLine>>()

    suspend fun getLyrics(trackName: String, artistName: String): List<LyricsLine> = withContext(Dispatchers.IO) {
        val cacheKey = "$trackName-$artistName".lowercase().trim()
        cache[cacheKey]?.let { return@withContext it }

        val cleanTrack = cleanTrackName(trackName)
        val cleanArtist = cleanArtistName(artistName)

        // 1. Direct LRCLIB match
        try {
            val encodedTrack = URLEncoder.encode(cleanTrack, "UTF-8")
            val encodedArtist = URLEncoder.encode(cleanArtist, "UTF-8")
            val url = "https://lrclib.net/api/get?track_name=$encodedTrack&artist_name=$encodedArtist"
            val response = client.get(url)
            if (response.status.value in 200..299) {
                val body = response.bodyAsText()
                val jsonObject = json.parseToJsonElement(body).jsonObject
                val syncedLyrics = jsonObject["syncedLyrics"]?.jsonPrimitive?.contentOrNull
                if (!syncedLyrics.isNullOrBlank()) {
                    val parsed = parseLrc(syncedLyrics)
                    if (parsed.isNotEmpty()) {
                        cache[cacheKey] = parsed
                        return@withContext parsed
                    }
                }
            }
        } catch (_: Exception) {}

        // 2. LRCLIB Search Fallback
        try {
            val searchQuery = URLEncoder.encode("$cleanTrack $cleanArtist".trim(), "UTF-8")
            val searchUrl = "https://lrclib.net/api/search?q=$searchQuery"
            val response = client.get(searchUrl)
            if (response.status.value in 200..299) {
                val body = response.bodyAsText()
                val jsonArray = json.parseToJsonElement(body).jsonArray
                for (elem in jsonArray) {
                    val obj = elem.jsonObject
                    val syncedLyrics = obj["syncedLyrics"]?.jsonPrimitive?.contentOrNull
                    if (!syncedLyrics.isNullOrBlank()) {
                        val parsed = parseLrc(syncedLyrics)
                        if (parsed.isNotEmpty()) {
                            cache[cacheKey] = parsed
                            return@withContext parsed
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        emptyList()
    }

    private fun cleanTrackName(name: String): String {
        return name
            .replace(Regex("""(?i)\(official\s*(music\s*)?video\)"""), "")
            .replace(Regex("""(?i)\[official\s*(music\s*)?video\]"""), "")
            .replace(Regex("""(?i)\(lyric\s*video\)"""), "")
            .replace(Regex("""(?i)\[lyric\s*video\]"""), "")
            .replace(Regex("""(?i)\(audio\)"""), "")
            .replace(Regex("""(?i)\[audio\]"""), "")
            .replace(Regex("""(?i)\(from\s*".*?"\)"""), "")
            .replace(Regex("""(?i)\(from\s*'.*?'\)"""), "")
            .replace(Regex("""(?i)ft\..*"""), "")
            .replace(Regex("""(?i)feat\..*"""), "")
            .trim()
    }

    private fun cleanArtistName(name: String): String {
        return name.split(",", "&", "feat.", "ft.", "•").firstOrNull()?.trim() ?: name.trim()
    }

    private fun parseLrc(lrc: String): List<LyricsLine> {
        val lines = mutableListOf<LyricsLine>()
        val regex = Regex("""\[(\d+):(\d+)(?:\.(\d+))?](.*)""")
        lrc.lines().forEach { line ->
            val match = regex.find(line)
            if (match != null) {
                val min = match.groupValues[1].toLongOrNull() ?: 0L
                val sec = match.groupValues[2].toLongOrNull() ?: 0L
                val rawMillis = match.groupValues[3]
                val millis = when (rawMillis.length) {
                    1 -> rawMillis.toLong() * 100
                    2 -> rawMillis.toLong() * 10
                    3 -> rawMillis.toLong()
                    else -> rawMillis.take(3).toLongOrNull() ?: 0L
                }
                val totalMillis = (min * 60 + sec) * 1000 + millis
                val text = match.groupValues[4].trim()
                if (text.isNotEmpty()) {
                    lines.add(LyricsLine(totalMillis, text))
                }
            }
        }
        return lines.sortedBy { it.timeMillis }
    }
}
