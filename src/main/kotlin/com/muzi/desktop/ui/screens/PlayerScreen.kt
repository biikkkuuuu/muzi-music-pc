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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.muzi.desktop.audio.DesktopAudioPlayer
import com.muzi.desktop.lyrics.LyricsProvider
import com.muzi.desktop.model.LyricsLine
import com.muzi.desktop.ui.theme.*

@Composable
fun PlayerScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentSong by DesktopAudioPlayer.currentSong.collectAsState()
    val isPlaying by DesktopAudioPlayer.isPlaying.collectAsState()
    val positionMillis by DesktopAudioPlayer.currentPositionMillis.collectAsState()
    val durationMillis by DesktopAudioPlayer.durationMillis.collectAsState()

    var lyrics by remember { mutableStateOf<List<LyricsLine>>(emptyList()) }
    val listState = rememberLazyListState()

    LaunchedEffect(currentSong) {
        currentSong?.let { song ->
            lyrics = LyricsProvider.getLyrics(song.title, song.artist)
        }
    }

    // Active lyric index
    val activeIndex = remember(positionMillis, lyrics) {
        lyrics.indexOfLast { it.timeMillis <= positionMillis }.coerceAtLeast(0)
    }

    LaunchedEffect(activeIndex) {
        if (lyrics.isNotEmpty() && activeIndex in lyrics.indices) {
            listState.animateScrollToItem(
                (activeIndex - 2).coerceAtLeast(0)
            )
        }
    }

    // Background gradient sampled from album color (SecondaryRed / PrimaryRed as in Desktop-2.png)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(SecondaryRed, Color(0xFF200505), PureBlack),
                    radius = 1200f
                )
            )
            .padding(horizontal = 48.dp, vertical = 28.dp)
    ) {
        // Back / Collapse button
        IconButton(
            onClick = onBackClick,
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Icon(Icons.Default.KeyboardArrowDown, "Close", tint = Color.White, modifier = Modifier.size(32.dp))
        }

        // Split Layout (Left: Artwork & Controls, Right: Synced Lyrics)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Column (Artwork + Controls)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large Artwork
                AsyncImage(
                    model = currentSong?.thumbnailUrl,
                    contentDescription = currentSong?.title,
                    modifier = Modifier
                        .size(360.dp)
                        .clip(RoundedCornerShape(16.dp))
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Track Title & Artist
                Row(
                    modifier = Modifier.width(360.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentSong?.title ?: "No Track",
                            color = TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = currentSong?.artist ?: "Unknown Artist",
                            color = TextSecondary,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.FavoriteBorder, "Favorite", tint = Color.White)
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.MoreVert, "Options", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Progress Bar
                val progress = if (durationMillis > 0) positionMillis.toFloat() / durationMillis.toFloat() else 0f
                Slider(
                    value = progress.coerceIn(0f, 1f),
                    onValueChange = { newProgress ->
                        DesktopAudioPlayer.seekTo((newProgress * durationMillis).toLong())
                    },
                    modifier = Modifier.width(360.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color(0x44FFFFFF)
                    )
                )

                Row(
                    modifier = Modifier.width(360.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatTime(positionMillis), color = TextSecondary, fontSize = 12.sp)
                    Text(formatTime(durationMillis), color = TextSecondary, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Controls: Shuffle, Prev, Play/Pause, Next, Repeat
                Row(
                    modifier = Modifier.width(360.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Shuffle, "Shuffle", tint = Color.White)
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.SkipPrevious, "Previous", tint = Color.White, modifier = Modifier.size(28.dp))
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
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Repeat, "Repeat", tint = Color.White)
                    }
                }
            }

            // Right Column (Synchronized Lyrics as shown in Desktop-2.png)
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.MusicNote, "Music", tint = Color(0x66FFFFFF), modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.height(24.dp))

                LazyColumn(
                    state = listState,
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
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
