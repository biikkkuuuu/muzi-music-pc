package com.muzi.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.muzi.desktop.audio.DesktopAudioPlayer
import com.muzi.desktop.data.LibraryManager
import com.muzi.desktop.model.Song
import com.muzi.desktop.ui.components.AddToPlaylistDialog
import com.muzi.desktop.ui.components.SongOptionsDialog
import com.muzi.desktop.ui.theme.*

/**
 * Replicating Android Muzi's StatsScreen.kt
 * Listening statistics: total playback time, ranked most played songs, and top artists.
 */
@Composable
fun StatsScreen(
    onBackClick: () -> Unit,
    onSongClick: (Song) -> Unit,
    onArtistClick: (name: String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val historySongs by LibraryManager.historySongs.collectAsState()
    val playCounts by LibraryManager.playCounts.collectAsState()

    var selectedPeriod by remember { mutableStateOf("All Time") }
    val periods = listOf("1 Week", "1 Month", "3 Months", "6 Months", "All Time")

    var selectedSongForOptions by remember { mutableStateOf<Song?>(null) }
    var selectedSongForPlaylist by remember { mutableStateOf<Song?>(null) }

    val currentPlayingSong by DesktopAudioPlayer.currentSong.collectAsState()

    // Aggregate statistics
    val totalPlayCount = remember(playCounts) { playCounts.values.sum() }
    val totalSeconds = remember(historySongs, playCounts) {
        historySongs.sumOf { (playCounts[it.id] ?: 1) * it.durationSeconds }
    }
    val totalHours = totalSeconds / 3600
    val totalMinutes = (totalSeconds % 3600) / 60

    val mostPlayedSongs = remember(historySongs, playCounts) {
        historySongs.distinctBy { it.id }.sortedByDescending { playCounts[it.id] ?: 1 }
    }

    val topArtists = remember(historySongs, playCounts) {
        historySongs.groupBy { it.artist }
            .mapValues { (_, songs) -> songs.sumOf { playCounts[it.id] ?: 1 } }
            .toList()
            .sortedByDescending { it.second }
    }

    selectedSongForOptions?.let { song ->
        SongOptionsDialog(
            song = song,
            onDismiss = { selectedSongForOptions = null },
            onAddToPlaylistClick = { selectedSongForPlaylist = song }
        )
    }

    selectedSongForPlaylist?.let { song ->
        AddToPlaylistDialog(
            song = song,
            onDismiss = { selectedSongForPlaylist = null }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(horizontal = 40.dp, vertical = 24.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick, modifier = Modifier.size(40.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Listening Stats",
                color = TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Period Filter Chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(periods) { period ->
                val isSelected = selectedPeriod == period
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) MuziAccent else Color(0xFF222328))
                        .clickable { selectedPeriod = period }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = period,
                        color = if (isSelected) Color.White else TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // Hero Listening Time Card
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1E1E24))
                        .padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MuziBlueContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Headphones, null, tint = MuziAccent, modifier = Modifier.size(32.dp))
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Column {
                        Text(
                            text = if (totalHours > 0) "$totalHours hrs $totalMinutes mins" else "$totalMinutes mins",
                            color = TextPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$totalPlayCount songs played in total",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Top Artists Row
            if (topArtists.isNotEmpty()) {
                item {
                    Text(
                        text = "Top Artists",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(topArtists.take(8)) { (artist, plays) ->
                            val thumb = historySongs.firstOrNull { it.artist == artist }?.thumbnailUrl
                            Column(
                                modifier = Modifier
                                    .width(100.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onArtistClick(artist) },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                AsyncImage(
                                    model = thumb,
                                    contentDescription = artist,
                                    modifier = Modifier.size(90.dp).clip(CircleShape).background(SurfaceDark),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = artist,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$plays plays",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // Most Played Songs List
            if (mostPlayedSongs.isNotEmpty()) {
                item {
                    Text(
                        text = "Most Played Songs",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                itemsIndexed(mostPlayedSongs.take(15)) { index, song ->
                    val isCurrent = currentPlayingSong?.id == song.id
                    val isLiked = LibraryManager.isLiked(song.id)
                    val plays = playCounts[song.id] ?: 1

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isCurrent) MuziBlueContainer.copy(alpha = 0.35f) else Color.Transparent)
                            .clickable {
                                DesktopAudioPlayer.playSong(song, mostPlayedSongs)
                                onSongClick(song)
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rank Badge
                        Text(
                            text = "#%d".format(index + 1),
                            color = if (index < 3) MuziAccent else TextSecondary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(36.dp)
                        )

                        AsyncImage(
                            model = song.thumbnailUrl,
                            contentDescription = song.title,
                            modifier = Modifier.size(46.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                color = if (isCurrent) MuziAccent else TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${song.artist} • $plays plays",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = { LibraryManager.toggleLike(song) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Like",
                                tint = if (isLiked) MuziAccent else Color(0x66FFFFFF),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { selectedSongForOptions = song },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.MoreVert, "Options", tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}
