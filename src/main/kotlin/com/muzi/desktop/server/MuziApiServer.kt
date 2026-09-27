package com.muzi.desktop.server

import com.muzi.desktop.innertube.YouTubeMusicService
import com.muzi.desktop.lyrics.LyricsProvider
import com.muzi.desktop.model.Song
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

object MuziApiServer {
    private var server: HttpServer? = null
    private val songIdMap = ConcurrentHashMap<Long, Song>()
    private val videoIdMap = ConcurrentHashMap<String, Long>()

    @JvmStatic
    fun main(args: Array<String>) {
        start(30488)
        println("[MuziApiServer] Server running on http://127.0.0.1:30488 - Press Enter to stop")
        Thread.currentThread().join()
    }

    fun start(port: Int = 30488) {
        if (server != null) return

        var activePort = port
        var created = false
        for (i in 0..10) {
            try {
                server = HttpServer.create(InetSocketAddress("127.0.0.1", activePort), 0)
                created = true
                break
            } catch (e: Exception) {
                println("[MuziApiServer] Port $activePort busy, trying ${activePort + 1}")
                activePort++
            }
        }

        if (!created || server == null) {
            println("[MuziApiServer] Failed to bind to any port!")
            return
        }

        val s = server!!
        s.executor = Executors.newFixedThreadPool(16)
        s.createContext("/", ApiHandler())
        s.start()
        println("[MuziApiServer] Started successfully on http://127.0.0.1:$activePort")
    }

    fun stop() {
        server?.stop(0)
        server = null
        println("[MuziApiServer] Stopped.")
    }

    private fun registerSong(song: Song): Long {
        val existing = videoIdMap[song.id]
        if (existing != null) {
            songIdMap[existing] = song
            return existing
        }
        val id = Math.abs(song.id.hashCode().toLong()) + 1000000L
        songIdMap[id] = song
        videoIdMap[song.id] = id
        return id
    }

    private class ApiHandler : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            try {
                val method = exchange.requestMethod.uppercase()
                val path = exchange.requestURI.path

                // Handle CORS Preflight
                exchange.responseHeaders.set("Access-Control-Allow-Origin", "*")
                exchange.responseHeaders.set("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
                exchange.responseHeaders.set("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With, Cookie")
                exchange.responseHeaders.set("Access-Control-Allow-Credentials", "true")

                if (method == "OPTIONS") {
                    exchange.sendResponseHeaders(204, -1)
                    exchange.close()
                    return
                }

                val queryParams = parseQueryParams(exchange.requestURI.rawQuery ?: "")
                println("[MuziApiServer] $method $path params=$queryParams")

                val jsonResponse: String = when {
                    path.endsWith("/cloudsearch") || path.endsWith("/search") -> handleSearch(queryParams)
                    path.endsWith("/search/suggest") -> handleSearchSuggest(queryParams)
                    path.endsWith("/search/hot/detail") -> handleSearchHot()
                    path.endsWith("/search/default") -> handleSearchDefault()
                    path.endsWith("/song/url/v1") || path.endsWith("/song/url") -> handleSongUrl(queryParams)
                    path.endsWith("/song/detail") -> handleSongDetail(queryParams)
                    path.endsWith("/lyric/new") || path.endsWith("/lyric") -> handleLyric(queryParams)
                    path.endsWith("/banner") -> handleBanner()
                    path.endsWith("/personalized/newsong") -> handlePersonalizedNewSong()
                    path.endsWith("/personalized") -> handlePersonalizedPlaylists()
                    path.endsWith("/top/artists") -> handleTopArtists()
                    path.endsWith("/toplist/artist") -> handleToplistOfArtists()
                    path.endsWith("/toplist") -> handleTopList()
                    path.endsWith("/album/new") -> handleNewAlbums()
                    path.endsWith("/recommend/songs") -> handleRecommendSongs()
                    path.endsWith("/personal_fm") -> handlePersonalFM()
                    path.endsWith("/login/status") || path.endsWith("/user/account") -> handleLoginStatus()
                    path.endsWith("/user/subcount") -> "{\"code\":200,\"subPlaylistCount\":0}"
                    path.endsWith("/user/playlist") -> "{\"code\":200,\"playlist\":[],\"more\":false}"
                    else -> "{\"code\":200,\"message\":\"OK\",\"data\":{}}"
                }

                val bytes = jsonResponse.toByteArray(StandardCharsets.UTF_8)
                exchange.responseHeaders.set("Content-Type", "application/json; charset=utf-8")
                exchange.sendResponseHeaders(200, bytes.size.toLong())
                exchange.responseBody.write(bytes)
                exchange.responseBody.close()
            } catch (e: Exception) {
                println("[MuziApiServer] Error handling request: ${e.message}")
                try {
                    val err = "{\"code\":500,\"message\":\"${e.message}\"}".toByteArray(StandardCharsets.UTF_8)
                    exchange.sendResponseHeaders(500, err.size.toLong())
                    exchange.responseBody.write(err)
                    exchange.responseBody.close()
                } catch (_: Exception) {}
            }
        }

        private fun parseQueryParams(raw: String): Map<String, String> {
            val map = mutableMapOf<String, String>()
            if (raw.isBlank()) return map
            for (part in raw.split("&")) {
                val kv = part.split("=", limit = 2)
                if (kv.isNotEmpty()) {
                    val key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8.name())
                    val value = if (kv.size > 1) URLDecoder.decode(kv[1], StandardCharsets.UTF_8.name()) else ""
                    map[key] = value
                }
            }
            return map
        }

        private fun handleSearch(params: Map<String, String>): String = runBlocking {
            val keywords = params["keywords"] ?: params["keyword"] ?: ""
            val songs = if (keywords.isNotBlank()) YouTubeMusicService.search(keywords) else emptyList()

            buildJsonObject {
                put("code", 200)
                put("result", buildJsonObject {
                    put("songCount", songs.size)
                    val songsArr = buildJsonArray {
                        for (s in songs) {
                            val numId = registerSong(s)
                            add(buildJsonObject {
                                put("id", numId)
                                put("name", s.title)
                                put("dt", s.durationSeconds * 1000)
                                put("mv", 0)
                                put("fee", 0)
                                put("videoId", s.id)
                                put("ar", buildJsonArray {
                                    add(buildJsonObject {
                                        put("id", 1)
                                        put("name", s.artist)
                                    })
                                })
                                put("artists", buildJsonArray {
                                    add(buildJsonObject {
                                        put("id", 1)
                                        put("name", s.artist)
                                    })
                                })
                                put("al", buildJsonObject {
                                    put("id", 1)
                                    put("name", s.album)
                                    put("picUrl", s.thumbnailUrl)
                                })
                                put("album", buildJsonObject {
                                    put("id", 1)
                                    put("name", s.album)
                                    put("picUrl", s.thumbnailUrl)
                                })
                            })
                        }
                    }
                    put("songs", songsArr)
                    put("song", buildJsonObject {
                        put("songs", songsArr)
                    })
                })
            }.toString()
        }

        private fun handleSearchSuggest(params: Map<String, String>): String = runBlocking {
            val keywords = params["keywords"] ?: ""
            val suggestions = if (keywords.isNotBlank()) YouTubeMusicService.getSearchSuggestions(keywords) else emptyList()

            buildJsonObject {
                put("code", 200)
                put("result", buildJsonObject {
                    put("songs", buildJsonArray {
                        for (s in suggestions) {
                            add(buildJsonObject { put("name", s) })
                        }
                    })
                })
            }.toString()
        }

        private fun handleSearchHot(): String {
            val list = listOf("Blinding Lights", "Fakira", "Kesariya", "Arijit Singh", "Trending Hits", "Diljit Dosanjh", "Top Global 50", "Shape of You")
            return buildJsonObject {
                put("code", 200)
                put("data", buildJsonArray {
                    list.forEachIndexed { i, word ->
                        add(buildJsonObject {
                            put("searchWord", word)
                            put("score", (10 - i) * 10000)
                        })
                    }
                })
            }.toString()
        }

        private fun handleSearchDefault(): String {
            return buildJsonObject {
                put("code", 200)
                put("data", buildJsonObject {
                    put("showKeyword", "Blinding Lights")
                    put("realkeyword", "Blinding Lights")
                })
            }.toString()
        }

        private fun handleSongUrl(params: Map<String, String>): String = runBlocking {
            val idStr = params["id"] ?: "0"
            val numId = idStr.toLongOrNull() ?: 0L
            val song = songIdMap[numId]
            val videoId = song?.id ?: idStr

            println("[MuziApiServer] Resolving stream for videoId=$videoId (id=$idStr)")
            val streamUrl = YouTubeMusicService.resolveStreamUrl(videoId) ?: ""

            buildJsonObject {
                put("code", 200)
                put("data", buildJsonArray {
                    add(buildJsonObject {
                        put("id", numId)
                        put("url", streamUrl)
                        put("br", 256000)
                        put("size", 0)
                        put("md5", "")
                        put("code", 200)
                        put("type", "m4a")
                        put("fee", 0)
                    })
                })
            }.toString()
        }

        private fun handleSongDetail(params: Map<String, String>): String {
            val ids = params["ids"]?.split(",") ?: emptyList()
            return buildJsonObject {
                put("code", 200)
                put("songs", buildJsonArray {
                    for (idStr in ids) {
                        val numId = idStr.toLongOrNull() ?: continue
                        val s = songIdMap[numId]
                        if (s != null) {
                            add(buildJsonObject {
                                put("id", numId)
                                put("name", s.title)
                                put("dt", s.durationSeconds * 1000)
                                put("al", buildJsonObject {
                                    put("id", 1)
                                    put("name", s.album)
                                    put("picUrl", s.thumbnailUrl)
                                })
                                put("ar", buildJsonArray {
                                    add(buildJsonObject {
                                        put("id", 1)
                                        put("name", s.artist)
                                    })
                                })
                            })
                        }
                    }
                })
            }.toString()
        }

        private fun handleLyric(params: Map<String, String>): String = runBlocking {
            val idStr = params["id"] ?: "0"
            val numId = idStr.toLongOrNull() ?: 0L
            val song = songIdMap[numId]

            val lrcBuilder = StringBuilder()
            if (song != null) {
                val lines = LyricsProvider.getLyrics(song.title, song.artist)
                for (l in lines) {
                    val m = l.timeMillis / 60000
                    val s = (l.timeMillis % 60000) / 1000
                    val ms = (l.timeMillis % 1000) / 10
                    lrcBuilder.append(String.format("[%02d:%02d.%02d]%s\n", m, s, ms, l.text))
                }
            }

            val lrcText = if (lrcBuilder.isNotEmpty()) lrcBuilder.toString() else "[00:00.00]Instrumental\n"

            buildJsonObject {
                put("code", 200)
                put("lrc", buildJsonObject {
                    put("lyric", lrcText)
                })
            }.toString()
        }

        private fun handleBanner(): String {
            return buildJsonObject {
                put("code", 200)
                put("banners", buildJsonArray {
                    add(buildJsonObject {
                        put("imageUrl", "https://i.ytimg.com/vi/ewfdRy5jfF8/hqdefault.jpg")
                        put("typeTitle", "Exclusive")
                        put("targetId", 100001)
                        put("targetType", 1)
                        put("titleColor", "red")
                    })
                    add(buildJsonObject {
                        put("imageUrl", "https://i.ytimg.com/vi/kJQP7kiw5Fk/hqdefault.jpg")
                        put("typeTitle", "Trending")
                        put("targetId", 100002)
                        put("targetType", 1)
                        put("titleColor", "red")
                    })
                    add(buildJsonObject {
                        put("imageUrl", "https://i.ytimg.com/vi/JGwWNGJdvx8/hqdefault.jpg")
                        put("typeTitle", "Hot Hits")
                        put("targetId", 100003)
                        put("targetType", 1)
                        put("titleColor", "red")
                    })
                })
            }.toString()
        }

        private fun handlePersonalizedNewSong(): String = runBlocking {
            val quickPicks = YouTubeMusicService.getQuickPicks()

            buildJsonObject {
                put("code", 200)
                put("result", buildJsonArray {
                    for (s in quickPicks.take(12)) {
                        val numId = registerSong(s)
                        add(buildJsonObject {
                            put("id", numId)
                            put("name", s.title)
                            put("picUrl", s.thumbnailUrl)
                            put("song", buildJsonObject {
                                put("duration", s.durationSeconds * 1000)
                                put("artists", buildJsonArray {
                                    add(buildJsonObject { put("name", s.artist) })
                                })
                                put("album", buildJsonObject { put("name", s.album) })
                            })
                        })
                    }
                })
            }.toString()
        }

        private fun handlePersonalizedPlaylists(): String {
            return buildJsonObject {
                put("code", 200)
                put("result", buildJsonArray {
                    add(buildJsonObject {
                        put("id", 2001)
                        put("name", "Top Global 50")
                        put("picUrl", "https://charts-static.billboard.com/img/1886/11/bruno-mars-e5t-180x180.jpg")
                        put("playCount", 1890000)
                        put("trackCount", 50)
                    })
                    add(buildJsonObject {
                        put("id", 2002)
                        put("name", "Bollywood Romantic Melodies")
                        put("picUrl", "https://i.ytimg.com/vi/ewfdRy5jfF8/hqdefault.jpg")
                        put("playCount", 2340000)
                        put("trackCount", 40)
                    })
                    add(buildJsonObject {
                        put("id", 2003)
                        put("name", "Punjabi Chartbusters")
                        put("picUrl", "https://i.ytimg.com/vi/kJQP7kiw5Fk/hqdefault.jpg")
                        put("playCount", 980000)
                        put("trackCount", 35)
                    })
                    add(buildJsonObject {
                        put("id", 2004)
                        put("name", "Chill and Focus Beats")
                        put("picUrl", "https://i.ytimg.com/vi/JGwWNGJdvx8/hqdefault.jpg")
                        put("playCount", 670000)
                        put("trackCount", 60)
                    })
                })
            }.toString()
        }

        private fun handleTopArtists(): String {
            val artists = listOf(
                "The Weeknd", "Arijit Singh", "Bruno Mars", "Diljit Dosanjh", "Taylor Swift", "Ed Sheeran"
            )
            return buildJsonObject {
                put("code", 200)
                put("artists", buildJsonArray {
                    artists.forEachIndexed { i, name ->
                        add(buildJsonObject {
                            put("id", 100 + i)
                            put("name", name)
                            put("picUrl", "https://charts-static.billboard.com/img/1886/11/bruno-mars-e5t-180x180.jpg")
                        })
                    }
                })
            }.toString()
        }

        private fun handleTopList(): String {
            val chartItems = listOf(
                Pair(19723756L, "Billboard Hot 100"),
                Pair(180106L, "UK Official Singles Chart"),
                Pair(60198L, "Global Viral 50"),
                Pair(3812895L, "Top Hits Today"),
                Pair(60131L, "Trending Music")
            )
            return buildJsonObject {
                put("code", 200)
                put("list", buildJsonArray {
                    chartItems.forEach { (id, name) ->
                        add(buildJsonObject {
                            put("id", id)
                            put("name", name)
                            put("coverImgUrl", "https://i.ytimg.com/vi/4NRXx6U8ABQ/hqdefault.jpg")
                            put("updateFrequency", "Daily")
                        })
                    }
                })
            }.toString()
        }

        private fun handleLoginStatus(): String {
            return buildJsonObject {
                put("code", 200)
                put("profile", buildJsonObject {
                    put("userId", 1)
                    put("nickname", "Muzi User")
                    put("avatarUrl", "https://i.ytimg.com/vi/ewfdRy5jfF8/hqdefault.jpg")
                })
            }.toString()
        }
    
        private fun handleNewAlbums(): String {
            return buildJsonObject {
                put("code", 200)
                put("albums", buildJsonArray {
                    add(buildJsonObject {
                        put("id", 5001)
                        put("name", "Hurry Up Tomorrow")
                        put("picUrl", "https://i.ytimg.com/vi/4NRXx6U8ABQ/hqdefault.jpg")
                        put("artist", buildJsonObject { put("name", "The Weeknd") })
                    })
                    add(buildJsonObject {
                        put("id", 5002)
                        put("name", "Die With A Smile")
                        put("picUrl", "https://i.ytimg.com/vi/ewfdRy5jfF8/hqdefault.jpg")
                        put("artist", buildJsonObject { put("name", "Lady Gaga & Bruno Mars") })
                    })
                    add(buildJsonObject {
                        put("id", 5003)
                        put("name", "Aashiqui 2")
                        put("picUrl", "https://i.ytimg.com/vi/kJQP7kiw5Fk/hqdefault.jpg")
                        put("artist", buildJsonObject { put("name", "Arijit Singh") })
                    })
                })
            }.toString()
        }

        private fun handleToplistOfArtists(): String {
            val artists = listOf(
                "The Weeknd", "Arijit Singh", "Bruno Mars", "Diljit Dosanjh", "Taylor Swift", "Ed Sheeran"
            )
            return buildJsonObject {
                put("code", 200)
                put("list", buildJsonObject {
                    put("artists", buildJsonArray {
                        artists.forEachIndexed { i, name ->
                            add(buildJsonObject {
                                put("id", 100 + i)
                                put("name", name)
                                put("picUrl", "https://charts-static.billboard.com/img/1886/11/bruno-mars-e5t-180x180.jpg")
                            })
                        }
                    })
                })
            }.toString()
        }

        private fun handleRecommendSongs(): String {
            return buildJsonObject {
                put("code", 200)
                put("data", buildJsonObject {
                    put("dailySongs", buildJsonArray {
                        add(buildJsonObject {
                            put("id", 219200207)
                            put("name", "Blinding Lights")
                            put("dt", 200040)
                            put("ar", buildJsonArray { add(buildJsonObject { put("id", 101); put("name", "The Weeknd") }) })
                            put("al", buildJsonObject { put("id", 201); put("name", "After Hours"); put("picUrl", "https://i.ytimg.com/vi/4NRXx6U8ABQ/hqdefault.jpg") })
                        })
                        add(buildJsonObject {
                            put("id", 219200208)
                            put("name", "Die With A Smile")
                            put("dt", 251000)
                            put("ar", buildJsonArray { add(buildJsonObject { put("id", 102); put("name", "Lady Gaga & Bruno Mars") }) })
                            put("al", buildJsonObject { put("id", 202); put("name", "Die With A Smile"); put("picUrl", "https://i.ytimg.com/vi/ewfdRy5jfF8/hqdefault.jpg") })
                        })
                    })
                })
            }.toString()
        }

        private fun handlePersonalFM(): String {
            return buildJsonObject {
                put("code", 200)
                put("data", buildJsonArray {
                    add(buildJsonObject {
                        put("id", 219200207)
                        put("name", "Blinding Lights")
                        put("dt", 200040)
                        put("ar", buildJsonArray { add(buildJsonObject { put("id", 101); put("name", "The Weeknd") }) })
                        put("al", buildJsonObject { put("id", 201); put("name", "After Hours"); put("picUrl", "https://i.ytimg.com/vi/4NRXx6U8ABQ/hqdefault.jpg") })
                    })
                })
            }.toString()
        }

    }
}
