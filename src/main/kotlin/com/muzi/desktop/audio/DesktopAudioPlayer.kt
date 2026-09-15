package com.muzi.desktop.audio

import com.muzi.desktop.model.Song
import javafx.application.Platform
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.util.Duration
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

    private val _durationMillis = MutableStateFlow(220000L)
    val durationMillis = _durationMillis.asStateFlow()

    private val _volume = MutableStateFlow(0.85f)
    val volume = _volume.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var isFxInitialized = false

    init {
        try {
            Platform.startup {}
            isFxInitialized = true
        } catch (_: Exception) {
            isFxInitialized = true
        }

        _currentSong.value = Song(
            id = "1",
            title = "Sahiba",
            artist = "Aditya Rikhari",
            album = "Sahiba",
            durationText = "3:40",
            durationSeconds = 220,
            thumbnailUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music113/v4/b2/9f/45/b29f4582-a1a2-ec02-ee7d-21bef3346547/8718857677529.png/500x500bb.jpg",
            streamUrl = "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview221/v4/2e/43/de/2e43de6d-8347-233c-c55b-5e63180f453c/mzaf_11408243760072457560.plus.aac.p.m4a"
        )
    }

    fun playSong(song: Song) {
        _currentSong.value = song
        _currentPositionMillis.value = 0L
        _durationMillis.value = if (song.durationSeconds > 0) song.durationSeconds * 1000L else 220000L
        _isPlaying.value = true

        val url = song.streamUrl
        if (url != null) {
            startFxPlayback(url)
        }
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
        Platform.runLater {
            mediaPlayer?.play()
        }
    }

    fun pause() {
        _isPlaying.value = false
        Platform.runLater {
            mediaPlayer?.pause()
        }
    }

    fun seekTo(positionMillis: Long) {
        _currentPositionMillis.value = positionMillis
        Platform.runLater {
            mediaPlayer?.seek(Duration.millis(positionMillis.toDouble()))
        }
    }

    fun setVolume(newVolume: Float) {
        _volume.value = newVolume.coerceIn(0f, 1f)
        Platform.runLater {
            mediaPlayer?.volume = _volume.value.toDouble()
        }
    }

    private fun startFxPlayback(streamUrl: String) {
        Platform.runLater {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.dispose()

                val media = Media(streamUrl)
                val player = MediaPlayer(media)
                mediaPlayer = player

                player.volume = _volume.value.toDouble()

                player.currentTimeProperty().addListener { _, _, newTime ->
                    _currentPositionMillis.value = newTime.toMillis().toLong()
                }

                player.totalDurationProperty().addListener { _, _, newDuration ->
                    if (!newDuration.isUnknown) {
                        _durationMillis.value = newDuration.toMillis().toLong()
                    }
                }

                player.setOnEndOfMedia {
                    _isPlaying.value = false
                    _currentPositionMillis.value = 0L
                }

                player.play()
                _isPlaying.value = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
