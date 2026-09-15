package com.muzi.desktop.audio

import com.muzi.desktop.innertube.YouTubeMusicService
import com.muzi.desktop.model.Song
import javafx.application.Platform
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.util.Duration
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

object DesktopAudioPlayer {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering = _isBuffering.asStateFlow()

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
    }

    fun playSong(song: Song) {
        _currentSong.value = song
        _currentPositionMillis.value = 0L
        _durationMillis.value = if (song.durationSeconds > 0) song.durationSeconds * 1000L else 220000L
        _isPlaying.value = true
        _isBuffering.value = true

        scope.launch {
            try {
                val audioFile = AudioCacheManager.getAudioFile(song.id) {
                    song.streamUrl ?: YouTubeMusicService.resolveStreamUrl(song.id)
                }
                _isBuffering.value = false

                if (audioFile != null && audioFile.exists()) {
                    val mediaUri = audioFile.toURI().toString()
                    _currentSong.value = song.copy(streamUrl = mediaUri)
                    startFxPlayback(mediaUri)
                } else {
                    println("[DesktopAudioPlayer] Failed to obtain audio file for ${song.title}")
                    _isPlaying.value = false
                }
            } catch (e: Exception) {
                println("[DesktopAudioPlayer] Error in playSong: ${e.message}")
                e.printStackTrace()
                _isBuffering.value = false
                _isPlaying.value = false
            }
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

    private fun startFxPlayback(mediaUri: String) {
        Platform.runLater {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.dispose()

                println("[DesktopAudioPlayer] Loading media: $mediaUri")
                val media = Media(mediaUri)
                media.setOnError {
                    println("[DesktopAudioPlayer] Media error: ${media.error?.message}")
                }

                val player = MediaPlayer(media)
                mediaPlayer = player

                player.volume = _volume.value.toDouble()

                player.setOnError {
                    println("[DesktopAudioPlayer] Player error: ${player.error?.message}")
                }

                player.currentTimeProperty().addListener { _, _, newTime ->
                    _currentPositionMillis.value = newTime.toMillis().toLong()
                }

                player.totalDurationProperty().addListener { _, _, newDuration ->
                    if (!newDuration.isUnknown) {
                        _durationMillis.value = newDuration.toMillis().toLong()
                    }
                }

                player.setOnReady {
                    println("[DesktopAudioPlayer] Player is ready! Duration: ${player.totalDuration.toSeconds()}s")
                    player.play()
                    _isPlaying.value = true
                }

                player.setOnEndOfMedia {
                    _isPlaying.value = false
                    _currentPositionMillis.value = 0L
                }

                player.play()
                _isPlaying.value = true
            } catch (e: Exception) {
                println("[DesktopAudioPlayer] Error starting playback: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}
