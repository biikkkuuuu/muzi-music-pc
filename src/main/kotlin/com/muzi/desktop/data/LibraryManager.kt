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

@Serializable
data class LibraryData(
    val likedSongs: List<Song> = emptyList(),
    val historySongs: List<Song> = emptyList()
)

object LibraryManager {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val storageFile = File(System.getProperty("user.home"), ".muzi/library.json")
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    private val _likedSongs = MutableStateFlow<List<Song>>(emptyList())
    val likedSongs = _likedSongs.asStateFlow()

    private val _historySongs = MutableStateFlow<List<Song>>(emptyList())
    val historySongs = _historySongs.asStateFlow()

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
                    historySongs = _historySongs.value
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
}
