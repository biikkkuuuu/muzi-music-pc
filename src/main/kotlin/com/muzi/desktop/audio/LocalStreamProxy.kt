package com.muzi.desktop.audio

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object LocalStreamProxy {
    private var server: HttpServer? = null
    const val PORT = 28888
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    @Synchronized
    fun start() {
        if (server != null) return
        try {
            val s = HttpServer.create(InetSocketAddress("127.0.0.1", PORT), 0)
            s.createContext("/stream", StreamHandler())
            s.executor = Executors.newCachedThreadPool()
            s.start()
            server = s
            println("[LocalStreamProxy] Started on http://127.0.0.1:$PORT/stream")
        } catch (e: Exception) {
            println("[LocalStreamProxy] Error starting server: ${e.message}")
        }
    }

    fun getProxiedUrl(targetUrl: String): String {
        start()
        val encoded = URLEncoder.encode(targetUrl, "UTF-8")
        return "http://127.0.0.1:$PORT/stream?url=$encoded"
    }

    private class StreamHandler : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            try {
                val rawQuery = exchange.requestURI.rawQuery ?: ""
                val rawUrl = if (rawQuery.startsWith("url=")) rawQuery.substring(4) else rawQuery.substringAfter("&url=")
                if (rawUrl.isBlank()) {
                    println("[LocalStreamProxy] Empty url param")
                    exchange.sendResponseHeaders(400, 0)
                    exchange.close()
                    return
                }

                val targetUrl = URLDecoder.decode(rawUrl, "UTF-8")
                println("[LocalStreamProxy] Incoming request for: ${targetUrl.take(60)}...")

                val requestBuilder = Request.Builder()
                    .url(targetUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36")
                    .header("Origin", "https://www.youtube.com")
                    .header("Referer", "https://www.youtube.com/")

                val range = exchange.requestHeaders.getFirst("Range")
                if (!range.isNullOrBlank()) {
                    requestBuilder.header("Range", range)
                    println("[LocalStreamProxy] Forwarding Range: $range")
                }

                val response = client.newCall(requestBuilder.build()).execute()
                val body = response.body

                println("[LocalStreamProxy] Upstream response code: ${response.code}")

                if (!response.isSuccessful || body == null) {
                    exchange.sendResponseHeaders(response.code, 0)
                    exchange.close()
                    response.close()
                    return
                }

                val contentType = response.header("Content-Type", "video/mp4") ?: "video/mp4"
                exchange.responseHeaders.set("Content-Type", contentType)
                val contentLength = body.contentLength()
                val contentRange = response.header("Content-Range")
                if (!contentRange.isNullOrBlank()) {
                    exchange.responseHeaders.set("Content-Range", contentRange)
                }
                exchange.responseHeaders.set("Accept-Ranges", "bytes")

                if (response.code == 206) {
                    exchange.sendResponseHeaders(206, if (contentLength > 0) contentLength else 0)
                } else {
                    exchange.sendResponseHeaders(200, if (contentLength > 0) contentLength else 0)
                }

                val outStream = exchange.responseBody
                val inStream = body.byteStream()
                val buffer = ByteArray(32768)
                var read = inStream.read(buffer)
                var total = 0L
                while (read != -1) {
                    outStream.write(buffer, 0, read)
                    total += read
                    read = inStream.read(buffer)
                }
                outStream.flush()
                outStream.close()
                inStream.close()
                response.close()
                println("[LocalStreamProxy] Completed transfer: $total bytes")
            } catch (e: Exception) {
                println("[LocalStreamProxy] Transfer error: ${e.message}")
            } finally {
                try { exchange.close() } catch (_: Exception) {}
            }
        }
    }
}
