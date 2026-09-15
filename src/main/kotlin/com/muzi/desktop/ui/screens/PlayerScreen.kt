package com.muzi.desktop.ui.screens

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.graphics.Brush
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
    LYRICS, QUEUE
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
    val isShuffle by DesktopAudioPlayer.isShuffle.collectAsState()
    val repeatMode by DesktopAudioPlayer.repeatMode.collectAsState()
    val likedSongs by LibraryManager.likedSongs.collectAsState()
    val dynamicThemeColor by DesktopAudioPlayer.dynamicThemeColor.collectAsState()

    val animatedBgColor by animateColorAsState(
        targetValue = dynamicThemeColor,
        animationSpec = tween(700),
        label = "PlayerDynamicBg"
    )

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
            .background(
                Brush.verticalGradient(
                    listOf(
                        animatedBgColor.copy(alpha = 0.55f),
                        animatedBgColor.copy(alpha = 0.15f),
                        PureBlack,
                        PureBlack
                    )
                )
            )
            .padding(28.dp)
    ) {
        // Back Button (Top Left)
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
                .padding(top = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(48.dp)
        ) {
            // Left Column (Artwork + Song Info + Controls)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Album Art (Desktop-2.png)
                Box(
                    modifier = Modifier
                        .size(360.dp)
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
                            color = Color.White,
                            strokeWidth = 3.dp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

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

                    // Like / Heart Icon
                    IconButton(
                        onClick = { currentSong?.let { LibraryManager.toggleLike(it) } }
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (isLiked) Color(0xFFFF3366) else Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Seek Progress Bar (with dynamic accent color)
                val progress = if (durationMillis > 0) positionMillis.toFloat() / durationMillis.toFloat() else 0f
                Slider(
                    value = progress.coerceIn(0f, 1f),
                    onValueChange = { newProgress ->
                        DesktopAudioPlayer.seekTo((newProgress * durationMillis).toLong())
                    },
                    modifier = Modifier.width(360.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = animatedBgColor,
                        inactiveTrackColor = Color(0x44FFFFFF)
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

                Spacer(modifier = Modifier.height(20.dp))

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
                            tint = if (isShuffle) Color.White else Color(0x44FFFFFF)
                        )
                    }
                    IconButton(onClick = { DesktopAudioPlayer.playPrevious() }) {
                        Icon(Icons.Default.SkipPrevious, "Previous", tint = Color.White, modifier = Modifier.size(30.dp))
                    }
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable { DesktopAudioPlayer.togglePlayPause() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.Black,
                            modifier = Modifier.size(32.dp)
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
                            tint = if (repeatMode != RepeatMode.OFF) Color.White else Color(0x44FFFFFF)
                        )
                    }
                }
            }

            // Right Column (Side Tab: Lyrics vs Up Next / Queue)
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight()
            ) {
                // Tab Switcher (Lyrics / Up Next)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Lyrics",
                        color = if (sideTab == PlayerSideTab.LYRICS) Color.White else Color(0x55FFFFFF),
                        fontSize = 18.sp,
                        fontWeight = if (sideTab == PlayerSideTab.LYRICS) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { sideTab = PlayerSideTab.LYRICS }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Up Next (${queue.size})",
                        color = if (sideTab == PlayerSideTab.QUEUE) Color.White else Color(0x55FFFFFF),
                        fontSize = 18.sp,
                        fontWeight = if (sideTab == PlayerSideTab.QUEUE) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { sideTab = PlayerSideTab.QUEUE }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                if (sideTab == PlayerSideTab.LYRICS) {
                    // Lyrics View (Desktop-2.png)
                    if (lyrics.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No lyrics available", color = Color(0x44FFFFFF), fontSize = 16.sp)
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
                                    color = if (isActive) Color.White else Color(0x55FFFFFF),
                                    fontSize = if (isActive) 26.sp else 20.sp,
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
                } else {
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
                                    .background(if (isCurrent) animatedBgColor.copy(alpha = 0.25f) else Color.Transparent)
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
                                        color = if (isCurrent) Color.White else TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
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
                                        Icons.Default.VolumeUp,
                                        contentDescription = "Playing",
                                        tint = animatedBgColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Text(
                                        text = item.durationText,
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
