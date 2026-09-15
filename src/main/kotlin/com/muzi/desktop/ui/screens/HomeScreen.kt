package com.muzi.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import com.music.innertube.pages.HomePage
import com.muzi.desktop.audio.DesktopAudioPlayer
import com.muzi.desktop.innertube.YouTubeMusicService
import com.muzi.desktop.model.ChartItem
import com.muzi.desktop.model.Song
import com.muzi.desktop.ui.theme.*

@Composable
fun HomeScreen(
    onSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    var homeFeed by remember { mutableStateOf<YouTubeMusicService.HomeFeed?>(null) }
    var charts by remember { mutableStateOf<List<ChartItem>>(emptyList()) }
    var selectedChip by remember { mutableStateOf<HomePage.Chip?>(null) }
    var isLoadingFeed by remember { mutableStateOf(true) }

    // Load Default Home Feed & Charts on launch
    LaunchedEffect(Unit) {
        charts = YouTubeMusicService.getBrowseCharts()
        homeFeed = YouTubeMusicService.getHomeFeed()
        isLoadingFeed = false
    }

    // Load Filtered Feed when mood chip changes
    LaunchedEffect(selectedChip) {
        isLoadingFeed = true
        homeFeed = YouTubeMusicService.getHomeFeed(selectedChip?.endpoint?.params)
        isLoadingFeed = false
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(horizontal = 36.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        // App Header & Real YouTube Music Mood Chips (Desktop-1.png & Muzi Android)
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Muzi",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(18.dp))

                val chips = homeFeed?.chips
                if (!chips.isNullOrEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(chips) { chip ->
                            val isSelected = chip.title == selectedChip?.title
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) Color(0xFFE50914) else ChipBackground)
                                    .clickable {
                                        selectedChip = if (isSelected) null else chip
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = chip.title,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }

        // Browse Charts Section (Desktop-1.png)
        if (selectedChip == null && charts.isNotEmpty()) {
            item {
                Column {
                    Text(
                        text = "Browse Charts",
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(charts) { chart ->
                            Column(
                                modifier = Modifier
                                    .width(150.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        // Find chip with same name or search
                                        homeFeed?.chips?.firstOrNull { it.title.contains("Top", ignoreCase = true) }?.let {
                                            selectedChip = it
                                        }
                                    }
                            ) {
                                AsyncImage(
                                    model = chart.thumbnailUrl,
                                    contentDescription = chart.title,
                                    modifier = Modifier
                                        .size(150.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = chart.title,
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = chart.subtitle,
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // Feed Loading Spinner
        if (isLoadingFeed) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
                }
            }
        } else {
            // Dynamic YouTube Music Sections (100% Exact Android Muzi Content Feed)
            val sections = homeFeed?.sections ?: emptyList()
            items(sections) { section ->
                val songItems = section.items.filterIsInstance<SongItem>().map { item ->
                    Song(
                        id = item.id,
                        title = item.title,
                        artist = item.artists?.joinToString(", ") { it.name } ?: "Album",
                        album = item.album?.name ?: "Single",
                        durationText = item.duration?.let { "%d:%02d".format(it / 60, it % 60) } ?: "3:30",
                        durationSeconds = item.duration?.toLong() ?: 210L,
                        thumbnailUrl = item.thumbnail,
                        streamUrl = null
                    )
                }

                if (songItems.isNotEmpty()) {
                    // Song Grid Section (Desktop-1.png style)
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = section.title,
                                color = TextPrimary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Button(
                                onClick = {
                                    DesktopAudioPlayer.playSong(songItems.first(), songItems)
                                    onSongClick(songItems.first())
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF262626)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Play all", color = TextPrimary, fontSize = 13.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))

                        val chunked = songItems.take(16).chunked(4)
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            chunked.forEach { rowSongs ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    rowSongs.forEach { song ->
                                        Row(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    DesktopAudioPlayer.playSong(song, songItems)
                                                    onSongClick(song)
                                                }
                                                .padding(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            AsyncImage(
                                                model = song.thumbnailUrl,
                                                contentDescription = song.title,
                                                modifier = Modifier
                                                    .size(52.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = song.title,
                                                    color = TextPrimary,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = song.artist,
                                                    color = TextSecondary,
                                                    fontSize = 12.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                    repeat(4 - rowSongs.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Playlist / Album Carousels Section
                    Column {
                        Text(
                            text = section.title,
                            color = TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(section.items) { item ->
                                val (title, subtitle, thumb) = when (item) {
                                    is PlaylistItem -> Triple(item.title, item.author?.name ?: "Playlist", item.thumbnail ?: "")
                                    is AlbumItem -> Triple(item.title, item.artists?.joinToString(", ") { it.name } ?: "Album", item.thumbnail ?: "")
                                    else -> Triple("Music", "Collection", "")
                                }
                                Column(
                                    modifier = Modifier
                                        .width(150.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            // Search songs for this item
                                            // To play album/playlist
                                        }
                                ) {
                                    AsyncImage(
                                        model = thumb,
                                        contentDescription = title,
                                        modifier = Modifier
                                            .size(150.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = title,
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = subtitle,
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

