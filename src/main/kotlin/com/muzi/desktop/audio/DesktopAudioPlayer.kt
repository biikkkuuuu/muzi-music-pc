package com.muzi.desktop.audio

import com.muzi.desktop.model.Song
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.URL
import javax.sound.sampled.*

object DesktopAudioPlayer {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _currentPositionMillis = MutableStateFlow(0L)
    val currentPositionMillis = _currentPositionMillis.asStateFlow()

    private val _durationMillis = MutableStateFlow(195000L) // 3:15
    val durationMillis = _durationMillis.asStateFlow()

    private val _volume = MutableStateFlow(0.85f)
    val volume = _volume.asStateFlow()

    private var progressJob: Job? = null
    private var playbackJob: Job? = null
    private var currentLine: SourceDataLine? = null

    init {
        _currentSong.value = Song(
            id = "2",
            title = "Pal Pal",
            artist = "Talwiinder",
            album = "Pal Pal",
            durationText = "3:15",
            durationSeconds = 195,
            thumbnailUrl = "https://c.saavncdn.com/472/Pal-Pal-Hindi-2023-20230713180425-500x500.jpg",
            streamUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__nbsp_.mp3"
        )
    }

    fun playSong(song: Song) {
        _currentSong.value = song
        _currentPositionMillis.value = 0L
        _durationMillis.value = if (song.durationSeconds > 0) song.durationSeconds * 1000L else 195000L
        _isPlaying.value = true
        startProgressTicker()
        startAudioStream(song.streamUrl)
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
        _currentSong.value?.let { startAudioStream(it.streamUrl) }
    }

    fun pause() {
        _isPlaying.value = false
        progressJob?.cancel()
        stopAudioStream()
    }

    fun seekTo(positionMillis: Long) {
        _currentPositionMillis.value = positionMillis.coerceIn(0L, _durationMillis.value)
    }

    fun setVolume(newVolume: Float) {
        _volume.value = newVolume.coerceIn(0f, 1f)
        try {
            currentLine?.let { line ->
                if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    val gainControl = line.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
                    val range = gainControl.maximum - gainControl.minimum
                    val gain = (range * _volume.value) + gainControl.minimum
                    gainControl.value = gain.coerceIn(gainControl.minimum, gainControl.maximum)
                }
            }
        } catch (_: Exception) {}
    }

    private fun startAudioStream(streamUrl: String?) {
        if (streamUrl == null) return
        stopAudioStream()

        playbackJob = scope.launch(Dispatchers.IO) {
            try {
                val connection = URL(streamUrl).openConnection()
                connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                connection.connect()
                val inputStream = connection.getInputStream()

                val inStream = AudioSystem.getAudioInputStream(inputStream)
                val baseFormat = inStream.format
                val decodedFormat = AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    baseFormat.sampleRate,
                    16,
                    baseFormat.channels,
                    baseFormat.channels * 2,
                    baseFormat.sampleRate,
                    false
                )

                val din = AudioSystem.getAudioInputStream(decodedFormat, inStream)
                val info = DataLine.Info(SourceDataLine::class.java, decodedFormat)
                val line = AudioSystem.getLine(info) as SourceDataLine
                currentLine = line

                line.open(decodedFormat)
                line.start()

                val buffer = ByteArray(4096)
                var bytesRead = 0

                while (isActive && _isPlaying.value && din.read(buffer, 0, buffer.size).also { bytesRead = it } != -1) {
                    line.write(buffer, 0, bytesRead)
                }

                line.drain()
                line.stop()
                line.close()
            } catch (e: Exception) {
                // stream error handled gracefully
            }
        }
    }

    private fun stopAudioStream() {
        playbackJob?.cancel()
        try {
            currentLine?.stop()
            currentLine?.close()
        } catch (_: Exception) {}
        currentLine = null
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
