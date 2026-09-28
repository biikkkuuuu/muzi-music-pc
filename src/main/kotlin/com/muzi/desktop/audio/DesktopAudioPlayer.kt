package com.muzi.desktop.audio

import androidx.compose.ui.graphics.Color
import com.muzi.desktop.data.LibraryManager
import com.muzi.desktop.innertube.YouTubeMusicService
import com.muzi.desktop.model.Song
import com.muzi.desktop.ui.theme.DynamicColorExtractor
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
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import javax.sound.sampled.AudioFormat
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
    private val _dynamicThemeColor = MutableStateFlow(Color(0xFF3B82F6))
    val dynamicThemeColor = _dynamicThemeColor.asStateFlow()

    private val _ambientPalette = MutableStateFlow<List<Color>>(listOf(Color(0xFF1E3A8A), Color(0xFF2563EB), Color(0xFF3B82F6)))
    val ambientPalette = _ambientPalette.asStateFlow()

    private val _sleepTimerRemainingMillis = MutableStateFlow<Long?>(null)
    val sleepTimerRemainingMillis = _sleepTimerRemainingMillis.asStateFlow()
    private var sleepTimerJob: Job? = null

    // PC Native Audio via Lavaplayer (Exact 44.1kHz Natural Speed & Zero-Lag Seek)
    private val outputFormat = StandardAudioDataFormats.COMMON_PCM_S16_BE
    private val playerManager: DefaultAudioPlayerManager = DefaultAudioPlayerManager().apply {
        configuration.outputFormat = outputFormat
        registerSourceManager(LocalAudioSourceManager())
        registerSourceManager(HttpAudioSourceManager())
    }

    private val audioPlayer: AudioPlayer = playerManager.createPlayer()
    private var audioLine: SourceDataLine? = null

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
        val format = AudioFormat(44100f, 16, 2, true, true)
        try {
            val lineInfo = DataLine.Info(SourceDataLine::class.java, format)
            val line = AudioSystem.getLine(lineInfo) as SourceDataLine
            line.open(format, 1024 * 16)
            line.start()
            audioLine = line
            println("[DesktopAudioPlayer] Windows PC audio line opened: 44.1kHz Stereo")
        } catch (e: Exception) {
            println("[DesktopAudioPlayer] Error opening audio line: ${e.message}")
        }

        scope.launch(Dispatchers.IO) {
            val frameBuffer = ByteBuffer.allocate(outputFormat.maximumChunkSize())
            val frame = MutableAudioFrame()
            frame.setBuffer(frameBuffer)

            while (isActive) {
                if (_isPlaying.value && !audioPlayer.isPaused) {
                    if (audioPlayer.provide(frame)) {
                        val bytes = frameBuffer.array()
                        val len = frame.dataLength
                        audioLine?.write(bytes, 0, len)
                    } else {
                        delay(2)
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
                delay(150)
            }
        }
    }

    private var currentPlayJob: Job? = null

    fun playSong(song: Song, newQueue: List<Song>? = null) {
        _currentSong.value = song
        _currentPositionMillis.value = 0L
        _durationMillis.value = if (song.durationSeconds > 0) song.durationSeconds * 1000L else 220000L
        _isPlaying.value = true
        _isBuffering.value = true

        LibraryManager.addToHistory(song)

        scope.launch {
            val palette = DynamicColorExtractor.extractPaletteFromUrl(song.thumbnailUrl)
            _ambientPalette.value = palette
            _dynamicThemeColor.value = palette.firstOrNull() ?: DynamicColorExtractor.DefaultMuziColor
        }

        // Manage Queue
        if (newQueue != null && newQueue.isNotEmpty()) {
            _queue.value = newQueue
            val foundIdx = newQueue.indexOfFirst { it.id == song.id }
            _queueIndex.value = if (foundIdx >= 0) foundIdx else 0
        } else {
            val existing = _queue.value
            val existingIdx = existing.indexOfFirst { it.id == song.id }
            if (existingIdx != -1) {
                _queueIndex.value = existingIdx
            } else {
                _queue.value = listOf(song)
                _queueIndex.value = 0
            }
        }

        // Automatically fetch and append radio queue in the background if queue has few remaining songs
        scope.launch {
            val q = _queue.value
            val currentIdx = _queueIndex.value
            if (q.size - currentIdx <= 3) {
                val radio = YouTubeMusicService.fetchRadioQueue(song.id)
                if (radio.isNotEmpty()) {
                    val currentQ = _queue.value
                    val freshSongs = radio.filter { r -> currentQ.none { it.id == r.id } }
                    if (freshSongs.isNotEmpty()) {
                        _queue.value = currentQ + freshSongs
                        println("[DesktopAudioPlayer] Radio queue enriched with ${freshSongs.size} songs. Total queue: ${_queue.value.size}")
                    }
                }
            }
        }

        // Cancel previous loading or playback download job immediately
        currentPlayJob?.cancel()
        audioPlayer.stopTrack()
        audioLine?.flush()

        currentPlayJob = scope.launch {
            try {
                val audioFile = AudioCacheManager.getAudioFile(song.id) {
                    song.streamUrl ?: YouTubeMusicService.resolveStreamUrl(song.id)
                }

                if (!isActive) return@launch

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
            } catch (e: CancellationException) {
                println("[DesktopAudioPlayer] Playback task cancelled for: ${song.title}")
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
        audioLine?.flush()
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
            _queueIndex.value = nextIndex
            playSong(q[nextIndex], q)
        } else if (_repeatMode.value == RepeatMode.ALL && q.isNotEmpty()) {
            _queueIndex.value = 0
            playSong(q[0], q)
        } else {
            // Queue ended! Fetch radio queue to continue seamless playback
            scope.launch {
                val current = _currentSong.value ?: return@launch
                val radio = YouTubeMusicService.fetchRadioQueue(current.id)
                if (radio.isNotEmpty()) {
                    val extended = q + radio.filter { r -> q.none { it.id == r.id } }
                    _queue.value = extended
                    val next = q.size
                    if (next in extended.indices) {
                        _queueIndex.value = next
                        playSong(extended[next], extended)
                    }
                }
            }
        }
    }

    fun playPrevious() {
        if (_currentPositionMillis.value > 3000L) {
            seekTo(0)
            return
        }

        val prevIndex = _queueIndex.value - 1
        val q = _queue.value
        if (prevIndex in q.indices) {
            _queueIndex.value = prevIndex
            playSong(q[prevIndex], q)
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
        audioLine?.flush()
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

    fun startSleepTimer(minutes: Int) {
        cancelSleepTimer()
        val totalMs = minutes * 60 * 1000L
        _sleepTimerRemainingMillis.value = totalMs
        sleepTimerJob = scope.launch {
            var remaining = totalMs
            while (remaining > 0 && isActive) {
                delay(1000L)
                remaining -= 1000L
                _sleepTimerRemainingMillis.value = remaining
            }
            if (isActive) {
                pause()
                _sleepTimerRemainingMillis.value = null
            }
        }
    }

    fun startSleepTimerEndOfTrack() {
        cancelSleepTimer()
        val remaining = (_durationMillis.value - _currentPositionMillis.value).coerceAtLeast(1000L)
        _sleepTimerRemainingMillis.value = remaining
        sleepTimerJob = scope.launch {
            var rem = remaining
            while (rem > 0 && isActive) {
                delay(1000L)
                rem = (_durationMillis.value - _currentPositionMillis.value).coerceAtLeast(0L)
                _sleepTimerRemainingMillis.value = rem
                if (rem <= 1000L) break
            }
            if (isActive) {
                pause()
                _sleepTimerRemainingMillis.value = null
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerRemainingMillis.value = null
    }
}
