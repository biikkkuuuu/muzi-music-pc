package com.muzi.desktop.audio

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

object AudioCacheManager {
    private val cacheDir = File(System.getProperty("user.home"), ".muzi/cache/audio").apply {
        if (!exists()) mkdirs()
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun getAudioFile(songId: String, streamUrlProvider: suspend () -> String?): File? = withContext(Dispatchers.IO) {
        val targetFile = File(cacheDir, "$songId.mp4")
        if (targetFile.exists() && targetFile.length() > 50_000) {
            println("[AudioCacheManager] Cache hit for $songId (${targetFile.length()} bytes)")
            return@withContext targetFile
        }

        val url = streamUrlProvider() ?: return@withContext null
        val tempFile = File(cacheDir, "$songId.tmp")

        try {
            println("[AudioCacheManager] Downloading audio for $songId from YouTube...")
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36")
                .header("Origin", "https://www.youtube.com")
                .header("Referer", "https://www.youtube.com/")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    println("[AudioCacheManager] Failed to download audio: ${response.code}")
                    return@withContext null
                }
                val body = response.body ?: return@withContext null
                body.byteStream().use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }

            if (tempFile.exists() && tempFile.length() > 50_000) {
                if (targetFile.exists()) targetFile.delete()
                tempFile.renameTo(targetFile)
                println("[AudioCacheManager] Successfully cached $songId (${targetFile.length()} bytes)")
                return@withContext targetFile
            }
        } catch (e: Exception) {
            println("[AudioCacheManager] Error downloading audio: ${e.message}")
            if (tempFile.exists()) tempFile.delete()
        }

        return@withContext null
    }
}
