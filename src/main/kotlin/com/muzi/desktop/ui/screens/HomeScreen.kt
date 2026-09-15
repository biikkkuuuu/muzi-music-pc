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
    var charts by remember { mutableStateOf<List<ChartItem>>(emptyList()) }
    var quickPicks by remember { mutableStateOf<List<Song>>(emptyList()) }
    var trendingSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var selectedChip by remember { mutableStateOf("Feel good") }
    var isLoadingFeed by remember { mutableStateOf(false) }

    val moodChips = listOf("Feel good", "Work out", "Romance", "Party", "Energise", "Relax", "Commute", "Sad", "Focus", "Sleep")

    LaunchedEffect(Unit) {
        charts = YouTubeMusicService.getBrowseCharts()
        trendingSongs = YouTubeMusicService.search("Top Trending Songs")
    }

    LaunchedEffect(selectedChip) {
        isLoadingFeed = true
        quickPicks = YouTubeMusicService.search("$selectedChip Songs")
        isLoadingFeed = false
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(horizontal = 36.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        // App Header & Mood Chips (Desktop-1.png)
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
                Spacer(modifier = Modifier.height(16.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(moodChips) { chip ->
                        val isSelected = chip == selectedChip
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) Color(0xFFE50914) else ChipBackground)
                                .clickable { selectedChip = chip }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = chip,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        // Browse Charts Section (Desktop-1.png)
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
                                    // Load songs for this chart
                                    selectedChip = chart.title
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

        // Quick Picks Section (4-column grid layout as seen in Desktop-1.png)
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedChip == "Feel good") "Quick picks" else "$selectedChip Picks",
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = {
                            if (quickPicks.isNotEmpty()) {
                                DesktopAudioPlayer.playSong(quickPicks.first(), quickPicks)
                                onSongClick(quickPicks.first())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF262626)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Play all", color = TextPrimary, fontSize = 13.sp)
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))

                if (isLoadingFeed) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
                    }
                } else {
                    val chunked = quickPicks.take(16).chunked(4)
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
                                                DesktopAudioPlayer.playSong(song, quickPicks)
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
            }
        }

        // Trending Now Section
        if (trendingSongs.isNotEmpty()) {
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Trending Now",
                            color = TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Button(
                            onClick = {
                                if (trendingSongs.isNotEmpty()) {
                                    DesktopAudioPlayer.playSong(trendingSongs.first(), trendingSongs)
                                    onSongClick(trendingSongs.first())
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF262626)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Play all", color = TextPrimary, fontSize = 13.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(trendingSongs) { song ->
                            Column(
                                modifier = Modifier
                                    .width(140.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        DesktopAudioPlayer.playSong(song, trendingSongs)
                                        onSongClick(song)
                                    }
                            ) {
                                AsyncImage(
                                    model = song.thumbnailUrl,
                                    contentDescription = song.title,
                                    modifier = Modifier
                                        .size(140.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = song.title,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
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
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}
