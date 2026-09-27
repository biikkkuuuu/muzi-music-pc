package com.muzi.desktop.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
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
    val dynamicThemeColor by DesktopAudioPlayer.dynamicThemeColor.collectAsState()

    val animatedAccent by animateColorAsState(dynamicThemeColor, animationSpec = tween(900))

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

    // Auto scroll lyrics with smooth Apple Music / Spotify style centering
    val activeIndex = remember(positionMillis, lyrics) {
        if (lyrics.isEmpty()) -1
        else lyrics.indexOfLast { it.timeMillis <= positionMillis }.coerceAtLeast(0)
    }

    LaunchedEffect(activeIndex) {
        if (lyrics.isNotEmpty() && activeIndex in lyrics.indices) {
            lyricsListState.animateScrollToItem(
                index = (activeIndex - 1).coerceAtLeast(0),
                scrollOffset = -140
            )
        }
    }

    // Auto scroll queue to current song
    LaunchedEffect(queueIndex) {
        if (queue.isNotEmpty() && queueIndex in queue.indices) {
            queueListState.animateScrollToItem((queueIndex - 1).coerceAtLeast(0))
        }
    }

    // Dynamic Ambient Mesh Gradient Background (Signature Muzi Android Feature)
    val ambientBrush = Brush.radialGradient(
        colors = listOf(
            animatedAccent.copy(alpha = 0.40f),
            animatedAccent.copy(alpha = 0.15f),
            Color(0xFF0A0A0A),
            Color.Black
        ),
        center = Offset(200f, 300f),
        radius = 1200f
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .background(ambientBrush)
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
                // Album Art with clean rounded corners & glowing ambient drop
                Box(
                    modifier = Modifier
                        .size(350.dp)
                        .clip(RoundedCornerShape(22.dp))
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
                            color = animatedAccent,
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
                            text = currentSong?.artist ?: "Unknown Artist",
                            color = TextSecondary,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = { DesktopAudioPlayer.toggleLikeCurrentSong() },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (isLiked) animatedAccent else TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Progress Bar + Time Stamps
                Column(modifier = Modifier.width(360.dp)) {
                    val progress = if (durationMillis > 0) {
                        (positionMillis.toFloat() / durationMillis.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    Slider(
                        value = progress,
                        onValueChange = { newProgress ->
                            DesktopAudioPlayer.seekTo((newProgress * durationMillis).toLong())
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = animatedAccent,
                            inactiveTrackColor = Color(0x33FFFFFF)
                        ),
                        modifier = Modifier.fillMaxWidth().height(18.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(positionMillis),
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = formatTime(durationMillis),
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Playback Controls (Shuffle, Previous, Play/Pause, Next, Repeat)
                Row(
                    modifier = Modifier.width(360.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { DesktopAudioPlayer.toggleShuffle() }) {
                        Icon(
                            Icons.Default.Shuffle,
                            "Shuffle",
                            tint = if (isShuffle) animatedAccent else TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(onClick = { DesktopAudioPlayer.playPrevious() }) {
                        Icon(Icons.Default.SkipPrevious, "Previous", tint = Color.White, modifier = Modifier.size(32.dp))
                    }

                    IconButton(
                        onClick = { DesktopAudioPlayer.togglePlayPause() },
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(animatedAccent)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    IconButton(onClick = { DesktopAudioPlayer.playNext() }) {
                        Icon(Icons.Default.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(32.dp))
                    }

                    IconButton(onClick = { DesktopAudioPlayer.toggleRepeat() }) {
                        Icon(
                            imageVector = when (repeatMode) {
                                RepeatMode.ONE -> Icons.Default.RepeatOne
                                else -> Icons.Default.Repeat
                            },
                            contentDescription = "Repeat",
                            tint = if (repeatMode != RepeatMode.OFF) animatedAccent else TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Volume Slider with Mute Toggle
                Row(
                    modifier = Modifier.width(360.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = { DesktopAudioPlayer.toggleMute() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = when {
                                volume == 0f -> Icons.Default.VolumeOff
                                volume < 0.5f -> Icons.Default.VolumeDown
                                else -> Icons.Default.VolumeUp
                            },
                            contentDescription = "Volume",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Slider(
                        value = volume,
                        onValueChange = { DesktopAudioPlayer.setVolume(it) },
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color(0x33FFFFFF)
                        ),
                        modifier = Modifier.weight(1f).height(16.dp)
                    )
                }
            }

            // Right Column (Lyrics / Queue / Details Tabs)
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
                        accentColor = animatedAccent,
                        onClick = { sideTab = PlayerSideTab.LYRICS }
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    TabPill(
                        title = "Up Next (${queue.size})",
                        isSelected = sideTab == PlayerSideTab.QUEUE,
                        accentColor = animatedAccent,
                        onClick = { sideTab = PlayerSideTab.QUEUE }
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    TabPill(
                        title = "Details",
                        isSelected = sideTab == PlayerSideTab.DETAILS,
                        accentColor = animatedAccent,
                        onClick = { sideTab = PlayerSideTab.DETAILS }
                    )
                }

                when (sideTab) {
                    PlayerSideTab.LYRICS -> {
                        // Apple Music / Spotify Style Synchronized Karaoke Lyrics
                        if (lyrics.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.MicNone,
                                        contentDescription = null,
                                        tint = Color(0x44FFFFFF),
                                        modifier = Modifier.size(56.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("No synced lyrics found for this song", color = Color(0x66FFFFFF), fontSize = 16.sp)
                                }
                            }
                        } else {
                            LazyColumn(
                                state = lyricsListState,
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(22.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                itemsIndexed(lyrics) { index, line ->
                                    val isActive = index == activeIndex

                                    val scale by animateFloatAsState(
                                        targetValue = if (isActive) 1.12f else 0.95f,
                                        animationSpec = tween(durationMillis = 350)
                                    )
                                    val alpha by animateFloatAsState(
                                        targetValue = if (isActive) 1.0f else 0.36f,
                                        animationSpec = tween(durationMillis = 350)
                                    )

                                    val textShadow = if (isActive) {
                                        Shadow(
                                            color = animatedAccent.copy(alpha = 0.70f),
                                            offset = Offset.Zero,
                                            blurRadius = 24f
                                        )
                                    } else null

                                    Text(
                                        text = line.text,
                                        color = if (isActive) Color.White else Color(0xFF888888),
                                        fontSize = if (isActive) 27.sp else 20.sp,
                                        fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                                        textAlign = TextAlign.Center,
                                        style = LocalTextStyle.current.copy(shadow = textShadow),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .graphicsLayer {
                                                scaleX = scale
                                                scaleY = scale
                                                this.alpha = alpha
                                            }
                                            .clickable { DesktopAudioPlayer.seekTo(line.timeMillis) }
                                            .padding(horizontal = 20.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                    PlayerSideTab.QUEUE -> {
                        // Up Next / Queue View
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
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isCurrent) animatedAccent.copy(alpha = 0.20f) else Color(0x11FFFFFF))
                                        .clickable { DesktopAudioPlayer.playSongAt(index) }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = item.thumbnailUrl,
                                        contentDescription = item.title,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            color = if (isCurrent) animatedAccent else TextPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = item.artist,
                                            color = TextSecondary,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (isCurrent) {
                                        Icon(
                                            Icons.Default.GraphicEq,
                                            contentDescription = "Playing",
                                            tint = animatedAccent,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    PlayerSideTab.DETAILS -> {
                        // Audio Details
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            DetailCard(title = "Song Title", value = currentSong?.title ?: "-")
                            DetailCard(title = "Primary Artist", value = currentSong?.artist ?: "-")
                            DetailCard(title = "Album", value = currentSong?.album ?: "Single")
                            DetailCard(title = "YouTube Video ID", value = currentSong?.id ?: "-")
                            DetailCard(title = "Duration", value = formatTime(durationMillis))
                            DetailCard(title = "Audio Engine", value = "Lavaplayer PC Engine (44.1 kHz, 16-bit PCM, Pure Windows Sound Line)")
                            DetailCard(title = "Stream Format", value = "GoogleVideo Direct Stream (AAC / itag 18 & 140)")
                            DetailCard(title = "Dynamic Ambient Color", value = "#%06X".format(animatedAccent.value.toLong() and 0xFFFFFF))
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
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) accentColor else Color(0xFF1E1E1E))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp)
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
            .background(Color(0x1AFFFFFF))
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
