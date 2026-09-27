package com.muzi.desktop.audio

import androidx.compose.ui.graphics.Color
import com.muzi.desktop.data.LibraryManager
import com.muzi.desktop.innertube.YouTubeMusicService
import com.muzi.desktop.model.Song
import com.muzi.desktop.ui.theme.DynamicColorExtractor
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormatTools
import com.sedmelluq.discord.lavaplayer.format.AudioPlayerInputStream
import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.InputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine

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

    private var previousVolume = 0.85f

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle = _isShuffle.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode = _repeatMode.asStateFlow()

    // Dynamic color extracted from album art
    private val _dynamicThemeColor = MutableStateFlow(Color(0xFFE50914))
    val dynamicThemeColor = _dynamicThemeColor.asStateFlow()

    // PC Native Audio via Lavaplayer & Windows Sound Architecture
    private val outputFormat = StandardAudioDataFormats.COMMON_PCM_S16_BE
    private val playerManager: DefaultAudioPlayerManager = DefaultAudioPlayerManager().apply {
        configuration.outputFormat = outputFormat
        registerSourceManager(LocalAudioSourceManager())
        registerSourceManager(HttpAudioSourceManager())
    }

    private val audioPlayer: AudioPlayer = playerManager.createPlayer()
    private var audioLine: SourceDataLine? = null
    private var audioStream: InputStream? = null

    init {
        audioPlayer.volume = (_volume.value * 100).toInt().coerceIn(0, 100)

        audioPlayer.addListener(object : AudioEventAdapter() {
            override fun onTrackEnd(player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason) {
                if (endReason.mayStartNext) {
                    scope.launch {
                        playNext()
                    }
                }
            }

            override fun onTrackException(player: AudioPlayer, track: AudioTrack, exception: FriendlyException) {
                println("[DesktopAudioPlayer] Track exception: ${exception.message}")
                _isPlaying.value = false
                _isBuffering.value = false
            }
        })

        startHardwareAudioThread()
        startPositionWatcher()
    }

    private fun startHardwareAudioThread() {
        val javaFormat = AudioDataFormatTools.toAudioFormat(outputFormat)
        try {
            val lineInfo = DataLine.Info(SourceDataLine::class.java, javaFormat)
            val line = AudioSystem.getLine(lineInfo) as SourceDataLine
            line.open(javaFormat, 1024 * 32)
            line.start()
            audioLine = line
            println("[DesktopAudioPlayer] Windows PC audio line opened: $javaFormat")
        } catch (e: Exception) {
            println("[DesktopAudioPlayer] Error opening audio line: ${e.message}")
        }

        audioStream = AudioPlayerInputStream.createStream(audioPlayer, outputFormat, 10000L, false)

        scope.launch(Dispatchers.IO) {
            val buffer = ByteArray(1024 * 4)
            while (isActive) {
                if (_isPlaying.value && !audioPlayer.isPaused) {
                    val stream = audioStream
                    val count = stream?.read(buffer, 0, buffer.size) ?: -1
                    if (count > 0) {
                        audioLine?.write(buffer, 0, count)
                    } else {
                        delay(4)
                    }
                } else {
                    delay(20)
                }
            }
        }
    }

    private fun startPositionWatcher() {
        scope.launch(Dispatchers.Default) {
            while (isActive) {
                val track = audioPlayer.playingTrack
                if (track != null && _isPlaying.value && !audioPlayer.isPaused) {
                    _currentPositionMillis.value = track.position
                    if (track.duration > 0) {
                        _durationMillis.value = track.duration
                    }
                }
                delay(200)
            }
        }
    }

    fun playSong(song: Song, newQueue: List<Song>? = null) {
        _currentSong.value = song
        _currentPositionMillis.value = 0L
        _durationMillis.value = if (song.durationSeconds > 0) song.durationSeconds * 1000L else 220000L
        _isPlaying.value = true
        _isBuffering.value = true

        LibraryManager.addToHistory(song)

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

                val source = if (audioFile != null && audioFile.exists() && audioFile.length() > 50000) {
                    audioFile.absolutePath
                } else {
                    song.streamUrl ?: YouTubeMusicService.resolveStreamUrl(song.id)
                }

                if (source != null) {
                    println("[DesktopAudioPlayer] Starting playback for ${song.title}: $source")
                    loadAndPlay(source)
                } else {
                    println("[DesktopAudioPlayer] Failed to resolve audio for ${song.title}")
                    _isBuffering.value = false
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

    private fun loadAndPlay(source: String) {
        audioPlayer.stopTrack()
        playerManager.loadItem(source, object : AudioLoadResultHandler {
            override fun trackLoaded(track: AudioTrack) {
                println("[DesktopAudioPlayer] Track loaded: ${track.info.title} duration=${track.duration}ms")
                audioPlayer.playTrack(track)
                audioPlayer.isPaused = false
                _isPlaying.value = true
                _isBuffering.value = false
                if (track.duration > 0) {
                    _durationMillis.value = track.duration
                }
            }

            override fun playlistLoaded(playlist: AudioPlaylist) {
                val track = playlist.tracks.firstOrNull() ?: return
                audioPlayer.playTrack(track)
                audioPlayer.isPaused = false
                _isPlaying.value = true
                _isBuffering.value = false
            }

            override fun noMatches() {
                println("[DesktopAudioPlayer] No audio matches found for source")
                _isBuffering.value = false
                _isPlaying.value = false
            }

            override fun loadFailed(e: FriendlyException) {
                println("[DesktopAudioPlayer] Load failed: ${e.message}")
                _isBuffering.value = false
                _isPlaying.value = false
            }
        })
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
        audioPlayer.isPaused = false
    }

    fun pause() {
        _isPlaying.value = false
        audioPlayer.isPaused = true
    }

    fun seekTo(positionMillis: Long) {
        val target = positionMillis.coerceIn(0L, _durationMillis.value)
        _currentPositionMillis.value = target
        audioPlayer.playingTrack?.position = target
    }

    fun seekRelative(deltaMillis: Long) {
        val newPos = (_currentPositionMillis.value + deltaMillis).coerceIn(0L, _durationMillis.value)
        seekTo(newPos)
    }

    fun setVolume(newVolume: Float) {
        val clamped = newVolume.coerceIn(0f, 1f)
        _volume.value = clamped
        audioPlayer.volume = (clamped * 100).toInt()
    }

    fun adjustVolume(delta: Float) {
        setVolume(_volume.value + delta)
    }

    fun toggleMute() {
        if (_volume.value > 0f) {
            previousVolume = _volume.value
            setVolume(0f)
        } else {
            setVolume(if (previousVolume > 0f) previousVolume else 0.85f)
        }
    }

    fun toggleLikeCurrentSong() {
        _currentSong.value?.let { LibraryManager.toggleLike(it) }
    }
}
