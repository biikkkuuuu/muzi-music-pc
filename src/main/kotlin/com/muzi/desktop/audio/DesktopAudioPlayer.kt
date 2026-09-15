package com.muzi.desktop.audio

import androidx.compose.ui.graphics.Color
import com.muzi.desktop.data.LibraryManager
import com.muzi.desktop.innertube.YouTubeMusicService
import com.muzi.desktop.model.Song
import com.muzi.desktop.ui.theme.DynamicColorExtractor
import javafx.application.Platform
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.util.Duration
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

enum class RepeatMode {
    OFF, ALL, ONE
}

object DesktopAudioPlayer {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong = _currentSong.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue = _queue.asStateFlow()

    private val _queueIndex = MutableStateFlow(0)
    val queueIndex = _queueIndex.asStateFlow()

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

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle = _isShuffle.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode = _repeatMode.asStateFlow()

    // Dynamic color extracted from album art (Android Muzi signature feature)
    private val _dynamicThemeColor = MutableStateFlow(Color(0xFFE50914))
    val dynamicThemeColor = _dynamicThemeColor.asStateFlow()

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

    fun playSong(song: Song, newQueue: List<Song>? = null) {
        _currentSong.value = song
        _currentPositionMillis.value = 0L
        _durationMillis.value = if (song.durationSeconds > 0) song.durationSeconds * 1000L else 220000L
        _isPlaying.value = true
        _isBuffering.value = true

        LibraryManager.addToHistory(song)

        // Dynamically extract album art color
        scope.launch {
            val color = DynamicColorExtractor.extractFromUrl(song.thumbnailUrl)
            _dynamicThemeColor.value = color
        }

        if (newQueue != null && newQueue.isNotEmpty()) {
            _queue.value = newQueue
            _queueIndex.value = newQueue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        } else {
            val existing = _queue.value
            val existingIdx = existing.indexOfFirst { it.id == song.id }
            if (existingIdx != -1) {
                _queueIndex.value = existingIdx
            } else {
                _queue.value = listOf(song)
                _queueIndex.value = 0
                // Fetch endless radio queue in background (Muzi Android behavior)
                scope.launch {
                    val radioSongs = YouTubeMusicService.fetchRadioQueue(song.id)
                    if (radioSongs.isNotEmpty()) {
                        val merged = listOf(song) + radioSongs.filter { it.id != song.id }
                        _queue.value = merged
                    }
                }
            }
        }

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

    fun playSongAt(index: Int) {
        val q = _queue.value
        if (index in q.indices) {
            _queueIndex.value = index
            playSong(q[index], q)
        }
    }

    fun playNext() {
        val q = _queue.value
        if (q.isEmpty()) return

        if (_repeatMode.value == RepeatMode.ONE) {
            seekTo(0)
            play()
            return
        }

        val nextIndex = if (_isShuffle.value) {
            q.indices.random()
        } else {
            _queueIndex.value + 1
        }

        if (nextIndex in q.indices) {
            playSongAt(nextIndex)
        } else if (_repeatMode.value == RepeatMode.ALL && q.isNotEmpty()) {
            playSongAt(0)
        } else {
            _isPlaying.value = false
        }
    }

    fun playPrevious() {
        if (_currentPositionMillis.value > 3000L) {
            seekTo(0)
            return
        }

        val prevIndex = _queueIndex.value - 1
        if (prevIndex >= 0) {
            playSongAt(prevIndex)
        } else {
            seekTo(0)
        }
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
    }

    fun toggleRepeat() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
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

                // Autoplay next song from radio queue when current song finishes!
                player.setOnEndOfMedia {
                    playNext()
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
