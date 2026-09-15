package com.muzi.desktop.audio

import com.muzi.desktop.model.Song
import javazoom.jl.player.Player
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.URL

object DesktopAudioPlayer {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _currentPositionMillis = MutableStateFlow(0L)
    val currentPositionMillis = _currentPositionMillis.asStateFlow()

    private val _durationMillis = MutableStateFlow(195000L)
    val durationMillis = _durationMillis.asStateFlow()

    private val _volume = MutableStateFlow(0.85f)
    val volume = _volume.asStateFlow()

    private var progressJob: Job? = null
    private var playbackJob: Job? = null
    private var player: Player? = null

    init {
        _currentSong.value = Song(
            id = "1",
            title = "Pal Pal",
            artist = "Talwiinder",
            album = "Pal Pal",
            durationText = "3:15",
            durationSeconds = 195,
            thumbnailUrl = "https://c.saavncdn.com/472/Pal-Pal-Hindi-2023-20230713180425-500x500.jpg",
            streamUrl = "https://cdn.pixabay.com/download/audio/2022/05/27/audio_1808fbf07a.mp3?filename=lofi-study-112191.mp3"
        )
    }

    fun playSong(song: Song) {
        _currentSong.value = song
        _currentPositionMillis.value = 0L
        _durationMillis.value = if (song.durationSeconds > 0) song.durationSeconds * 1000L else 195000L
        _isPlaying.value = true
        startProgressTicker()
        startPlayback(song.streamUrl)
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
        _currentSong.value?.let { startPlayback(it.streamUrl) }
    }

    fun pause() {
        _isPlaying.value = false
        progressJob?.cancel()
        stopPlayback()
    }

    fun seekTo(positionMillis: Long) {
        _currentPositionMillis.value = positionMillis.coerceIn(0L, _durationMillis.value)
    }

    fun setVolume(newVolume: Float) {
        _volume.value = newVolume.coerceIn(0f, 1f)
    }

    private fun startPlayback(streamUrl: String?) {
        if (streamUrl == null) return
        stopPlayback()

        playbackJob = scope.launch(Dispatchers.IO) {
            try {
                val url = URL(streamUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                conn.connectTimeout = 10000
                conn.readTimeout = 15000
                conn.connect()

                val bufferedStream = BufferedInputStream(conn.inputStream)
                val newPlayer = Player(bufferedStream)
                player = newPlayer

                newPlayer.play()
            } catch (_: Exception) {
                // stream closed or error handled gracefully
            }
        }
    }

    private fun stopPlayback() {
        playbackJob?.cancel()
        try {
            player?.close()
        } catch (_: Exception) {}
        player = null
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
