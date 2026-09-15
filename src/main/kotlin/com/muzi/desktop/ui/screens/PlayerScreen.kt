package com.muzi.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.muzi.desktop.audio.DesktopAudioPlayer
import com.muzi.desktop.audio.RepeatMode
import com.muzi.desktop.data.LibraryManager
import com.muzi.desktop.lyrics.LyricsProvider
import com.muzi.desktop.model.LyricsLine
import com.muzi.desktop.ui.theme.*

enum class PlayerSideTab {
    LYRICS, QUEUE, DETAILS
}

@Composable
fun PlayerScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentSong by DesktopAudioPlayer.currentSong.collectAsState()
    val queue by DesktopAudioPlayer.queue.collectAsState()
    val queueIndex by DesktopAudioPlayer.queueIndex.collectAsState()
    val isPlaying by DesktopAudioPlayer.isPlaying.collectAsState()
    val isBuffering by DesktopAudioPlayer.isBuffering.collectAsState()
    val positionMillis by DesktopAudioPlayer.currentPositionMillis.collectAsState()
    val durationMillis by DesktopAudioPlayer.durationMillis.collectAsState()
    val volume by DesktopAudioPlayer.volume.collectAsState()
    val isShuffle by DesktopAudioPlayer.isShuffle.collectAsState()
    val repeatMode by DesktopAudioPlayer.repeatMode.collectAsState()
    val likedSongs by LibraryManager.likedSongs.collectAsState()

    val isLiked = currentSong?.let { LibraryManager.isLiked(it.id) } ?: false

    var sideTab by remember { mutableStateOf(PlayerSideTab.LYRICS) }
    var lyrics by remember { mutableStateOf<List<LyricsLine>>(emptyList()) }
    val lyricsListState = rememberLazyListState()
    val queueListState = rememberLazyListState()

    LaunchedEffect(currentSong) {
        currentSong?.let { song ->
            lyrics = LyricsProvider.getLyrics(song.title, song.artist)
        }
    }

    // Auto scroll lyrics
    val activeIndex = remember(positionMillis, lyrics) {
        lyrics.indexOfLast { it.timeMillis <= positionMillis }.coerceAtLeast(0)
    }

    LaunchedEffect(activeIndex) {
        if (lyrics.isNotEmpty() && activeIndex in lyrics.indices) {
            lyricsListState.animateScrollToItem((activeIndex - 2).coerceAtLeast(0))
        }
    }

    // Auto scroll queue to current song
    LaunchedEffect(queueIndex) {
        if (queue.isNotEmpty() && queueIndex in queue.indices) {
            queueListState.animateScrollToItem((queueIndex - 1).coerceAtLeast(0))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(28.dp)
    ) {
        // Back / Collapse Button (Top Left)
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(44.dp)
        ) {
            Icon(Icons.Default.KeyboardArrowDown, "Minimize", tint = Color.White, modifier = Modifier.size(32.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(48.dp)
        ) {
            // Left Column (Artwork + Song Info + Controls + Volume)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Album Art with clean rounded corners
                Box(
                    modifier = Modifier
                        .size(350.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceDark)
                ) {
                    if (currentSong?.thumbnailUrl?.isNotEmpty() == true) {
                        AsyncImage(
                            model = currentSong?.thumbnailUrl,
                            contentDescription = currentSong?.title,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier
                                .size(96.dp)
                                .align(Alignment.Center)
                        )
                    }

                    if (isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(48.dp)
                                .align(Alignment.Center),
                            color = MuziAccent,
                            strokeWidth = 3.dp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Song Title + Like Heart + Artist
                Row(
                    modifier = Modifier.width(360.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentSong?.title ?: "Select a song",
                            color = TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentSong?.artist ?: "Muzi Music",
                            color = TextSecondary,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Like / Heart Icon (Muzi Coral Red when liked)
                    IconButton(
                        onClick = { currentSong?.let { LibraryManager.toggleLike(it) } }
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (isLiked) MuziAccent else Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Seek Progress Bar (Signature Muzi Accent)
                val progress = if (durationMillis > 0) positionMillis.toFloat() / durationMillis.toFloat() else 0f
                Slider(
                    value = progress.coerceIn(0f, 1f),
                    onValueChange = { newProgress ->
                        DesktopAudioPlayer.seekTo((newProgress * durationMillis).toLong())
                    },
                    modifier = Modifier.width(360.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = MuziAccent,
                        inactiveTrackColor = Color(0xFF262626)
                    )
                )

                // Timestamps (Current vs Duration)
                Row(
                    modifier = Modifier.width(360.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = formatTime(positionMillis), color = TextSecondary, fontSize = 12.sp)
                    Text(text = formatTime(durationMillis), color = TextSecondary, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Controls: Shuffle, Prev, Play/Pause, Next, Repeat
                Row(
                    modifier = Modifier.width(360.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { DesktopAudioPlayer.toggleShuffle() }) {
                        Icon(
                            Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (isShuffle) MuziAccent else Color(0x66FFFFFF)
                        )
                    }
                    IconButton(onClick = { DesktopAudioPlayer.playPrevious() }) {
                        Icon(Icons.Default.SkipPrevious, "Previous", tint = Color.White, modifier = Modifier.size(30.dp))
                    }
                    IconButton(
                        onClick = { DesktopAudioPlayer.togglePlayPause() },
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(MuziAccent)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    IconButton(onClick = { DesktopAudioPlayer.playNext() }) {
                        Icon(Icons.Default.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(30.dp))
                    }
                    IconButton(onClick = { DesktopAudioPlayer.toggleRepeat() }) {
                        Icon(
                            imageVector = when (repeatMode) {
                                RepeatMode.ONE -> Icons.Default.RepeatOne
                                else -> Icons.Default.Repeat
                            },
                            contentDescription = "Repeat",
                            tint = if (repeatMode != RepeatMode.OFF) MuziAccent else Color(0x66FFFFFF)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Volume Slider (Muzi Desktop essential control)
                Row(
                    modifier = Modifier.width(340.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (volume > 0.5f) Icons.Default.VolumeUp else if (volume > 0f) Icons.Default.VolumeDown else Icons.Default.VolumeMute,
                        contentDescription = "Volume",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Slider(
                        value = volume,
                        onValueChange = { DesktopAudioPlayer.setVolume(it) },
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color(0xFF262626)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${(volume * 100).toInt()}%",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.width(32.dp)
                    )
                }
            }

            // Right Column (Side Tab: Lyrics vs Up Next vs Details)
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight()
            ) {
                // Tab Switcher (Lyrics / Up Next / Song Details)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TabPill(
                        title = "Lyrics",
                        isSelected = sideTab == PlayerSideTab.LYRICS,
                        onClick = { sideTab = PlayerSideTab.LYRICS }
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    TabPill(
                        title = "Up Next (${queue.size})",
                        isSelected = sideTab == PlayerSideTab.QUEUE,
                        onClick = { sideTab = PlayerSideTab.QUEUE }
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    TabPill(
                        title = "Details",
                        isSelected = sideTab == PlayerSideTab.DETAILS,
                        onClick = { sideTab = PlayerSideTab.DETAILS }
                    )
                }

                when (sideTab) {
                    PlayerSideTab.LYRICS -> {
                        // Lyrics View
                        if (lyrics.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No lyrics available for this song", color = Color(0x44FFFFFF), fontSize = 16.sp)
                            }
                        } else {
                            LazyColumn(
                                state = lyricsListState,
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                itemsIndexed(lyrics) { index, line ->
                                    val isActive = index == activeIndex
                                    Text(
                                        text = line.text,
                                        color = if (isActive) Color.White else Color(0xFF555555),
                                        fontSize = if (isActive) 26.sp else 19.sp,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { DesktopAudioPlayer.seekTo(line.timeMillis) }
                                            .padding(horizontal = 16.dp)
                                    )
                                }
                            }
                        }
                    }
                    PlayerSideTab.QUEUE -> {
                        // Up Next / Queue View (Exact Muzi Android Queue Behavior)
                        LazyColumn(
                            state = queueListState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            itemsIndexed(queue) { index, item ->
                                val isCurrent = index == queueIndex
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isCurrent) MuziAccent.copy(alpha = 0.16f) else Color.Transparent)
                                        .clickable { DesktopAudioPlayer.playSongAt(index) }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = item.thumbnailUrl,
                                        contentDescription = item.title,
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            color = if (isCurrent) MuziAccent else TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = item.artist,
                                            color = TextSecondary,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (isCurrent) {
                                        Icon(
                                            Icons.Default.GraphicEq,
                                            contentDescription = "Playing",
                                            tint = MuziAccent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    PlayerSideTab.DETAILS -> {
                        // Audio Details (Ported from Android Muzi Technical details)
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            DetailCard(title = "Title", value = currentSong?.title ?: "-")
                            DetailCard(title = "Artist", value = currentSong?.artist ?: "-")
                            DetailCard(title = "YouTube Video ID", value = currentSong?.id ?: "-")
                            DetailCard(title = "Duration", value = formatTime(durationMillis))
                            DetailCard(title = "Audio Engine", value = "JavaFX Native MediaPlayer + InnerTube Stream")
                            DetailCard(title = "Audio Quality", value = "256 kbps AAC / M4A Native Stream")
                            DetailCard(title = "Theme", value = "Muzi AMOLED Black (#000000) & Muzi Coral (#ED5564)")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabPill(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) MuziAccent else Color(0xFF1E1E1E))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            color = if (isSelected) Color.White else TextSecondary,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun DetailCard(title: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceElevated)
            .padding(14.dp)
    ) {
        Text(text = title, color = TextSecondary, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
