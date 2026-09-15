package com.muzi.desktop.data

import com.muzi.desktop.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

@Serializable
data class UserPlaylist(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val songs: List<Song> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class LibraryData(
    val likedSongs: List<Song> = emptyList(),
    val historySongs: List<Song> = emptyList(),
    val searchHistory: List<String> = emptyList(),
    val playlists: List<UserPlaylist> = emptyList()
)

object LibraryManager {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val storageFile = File(System.getProperty("user.home"), ".muzi/library.json")
    private val cacheDir = File(System.getProperty("user.home"), ".muzi/cache/audio")
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    private val _likedSongs = MutableStateFlow<List<Song>>(emptyList())
    val likedSongs = _likedSongs.asStateFlow()

    private val _historySongs = MutableStateFlow<List<Song>>(emptyList())
    val historySongs = _historySongs.asStateFlow()

    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory = _searchHistory.asStateFlow()

    private val _playlists = MutableStateFlow<List<UserPlaylist>>(emptyList())
    val playlists = _playlists.asStateFlow()

    init {
        load()
    }

    private fun load() {
        try {
            if (storageFile.exists()) {
                val content = storageFile.readText()
                val data = json.decodeFromString<LibraryData>(content)
                _likedSongs.value = data.likedSongs
                _historySongs.value = data.historySongs
                _searchHistory.value = data.searchHistory
                _playlists.value = data.playlists
            }
        } catch (e: Exception) {
            println("[LibraryManager] Error loading library: ${e.message}")
        }
    }

    private fun save() {
        scope.launch {
            try {
                storageFile.parentFile?.mkdirs()
                val data = LibraryData(
                    likedSongs = _likedSongs.value,
                    historySongs = _historySongs.value,
                    searchHistory = _searchHistory.value,
                    playlists = _playlists.value
                )
                storageFile.writeText(json.encodeToString(data))
            } catch (e: Exception) {
                println("[LibraryManager] Error saving library: ${e.message}")
            }
        }
    }

    fun isLiked(songId: String): Boolean {
        return _likedSongs.value.any { it.id == songId }
    }

    fun toggleLike(song: Song) {
        val current = _likedSongs.value.toMutableList()
        val index = current.indexOfFirst { it.id == song.id }
        if (index != -1) {
            current.removeAt(index)
        } else {
            current.add(0, song)
        }
        _likedSongs.value = current
        save()
    }

    fun addToHistory(song: Song) {
        val current = _historySongs.value.toMutableList()
        current.removeAll { it.id == song.id }
        current.add(0, song)
        if (current.size > 100) {
            _historySongs.value = current.take(100)
        } else {
            _historySongs.value = current
        }
        save()
    }

    fun addSearchQuery(query: String) {
        if (query.isBlank()) return
        val current = _searchHistory.value.toMutableList()
        current.removeAll { it.equals(query, ignoreCase = true) }
        current.add(0, query.trim())
        if (current.size > 20) {
            _searchHistory.value = current.take(20)
        } else {
            _searchHistory.value = current
        }
        save()
    }

    fun clearSearchHistory() {
        _searchHistory.value = emptyList()
        save()
    }

    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        val current = _playlists.value.toMutableList()
        current.add(0, UserPlaylist(title = name))
        _playlists.value = current
        save()
    }

    fun deletePlaylist(id: String) {
        val current = _playlists.value.toMutableList()
        current.removeAll { it.id == id }
        _playlists.value = current
        save()
    }

    fun addSongToPlaylist(playlistId: String, song: Song) {
        val current = _playlists.value.toMutableList()
        val index = current.indexOfFirst { it.id == playlistId }
        if (index != -1) {
            val pl = current[index]
            val updatedSongs = pl.songs.toMutableList()
            if (updatedSongs.none { it.id == song.id }) {
                updatedSongs.add(song)
                current[index] = pl.copy(songs = updatedSongs)
                _playlists.value = current
                save()
            }
        }
    }

    // Get offline downloaded songs from disk
    fun getDownloadedSongs(): List<Song> {
        if (!cacheDir.exists()) return emptyList()
        val cachedFiles = cacheDir.listFiles { f -> f.extension == "mp4" && f.length() > 50_000 } ?: return emptyList()
        val cachedIds = cachedFiles.map { it.nameWithoutExtension }.toSet()

        // Match against known songs from history or liked
        val known = (_likedSongs.value + _historySongs.value).distinctBy { it.id }
        return known.filter { it.id in cachedIds }
    }
}
