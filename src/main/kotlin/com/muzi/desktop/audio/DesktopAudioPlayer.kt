package com.muzi.desktop.audio

import com.muzi.desktop.model.Song
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object DesktopAudioPlayer {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _currentPositionMillis = MutableStateFlow(0L)
    val currentPositionMillis = _currentPositionMillis.asStateFlow()

    private val _durationMillis = MutableStateFlow(220000L) // default ~3:40
    val durationMillis = _durationMillis.asStateFlow()

    private val _volume = MutableStateFlow(0.85f)
    val volume = _volume.asStateFlow()

    private var progressJob: Job? = null

    init {
        // Set initial default song matching Desktop-2.png
        _currentSong.value = Song(
            id = "1",
            title = "Sahiba",
            artist = "Aditya Rikhari",
            album = "Sahiba",
            durationText = "6:20",
            durationSeconds = 380,
            thumbnailUrl = "https://c.saavncdn.com/264/Sahiba-Hindi-2024-20240320144026-500x500.jpg"
        )
        _durationMillis.value = 380000L
    }

    fun playSong(song: Song) {
        _currentSong.value = song
        _currentPositionMillis.value = 0L
        _durationMillis.value = if (song.durationSeconds > 0) song.durationSeconds * 1000L else 220000L
        _isPlaying.value = true
        startProgressTicker()
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            play()
        }
    }

    fun play() {
        _isPlaying.value = true
        startProgressTicker()
    }

    fun pause() {
        _isPlaying.value = false
        progressJob?.cancel()
    }

    fun seekTo(positionMillis: Long) {
        _currentPositionMillis.value = positionMillis.coerceIn(0L, _durationMillis.value)
    }

    fun setVolume(newVolume: Float) {
        _volume.value = newVolume.coerceIn(0f, 1f)
    }

    private fun startProgressTicker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (_isPlaying.value) {
                delay(250)
                if (_currentPositionMillis.value < _durationMillis.value) {
                    _currentPositionMillis.value += 250
                } else {
                    _isPlaying.value = false
                    break
                }
            }
        }
    }
}
