package com.muzi.desktop.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.muzi.desktop.audio.DesktopAudioPlayer
import com.muzi.desktop.data.LibraryManager
import com.muzi.desktop.innertube.YouTubeMusicService
import com.muzi.desktop.model.Song
import com.muzi.desktop.ui.components.AddToPlaylistDialog
import com.muzi.desktop.ui.components.SongOptionsDialog
import com.muzi.desktop.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SearchScreen(
    onSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Song>>(emptyList()) }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedFilter by remember { mutableStateOf("All") }
    var isSearching by remember { mutableStateOf(false) }

    var selectedSongForOptions by remember { mutableStateOf<Song?>(null) }
    var selectedSongForPlaylist by remember { mutableStateOf<Song?>(null) }

    val searchHistory by LibraryManager.searchHistory.collectAsState()
    val filterTabs = listOf("All", "Songs", "Videos", "Albums", "Artists", "Playlists")

    // Fetch live search suggestions as user types
    LaunchedEffect(query) {
        if (query.length >= 2) {
            delay(180)
            suggestions = YouTubeMusicService.getSearchSuggestions(query)
        } else {
            suggestions = emptyList()
        }
    }

    // Execute search when query or filter changes
    LaunchedEffect(query, selectedFilter) {
        if (query.isNotBlank()) {
            isSearching = true
            LibraryManager.addSearchQuery(query)
            val fullQuery = if (selectedFilter == "All") query else "$query $selectedFilter"
            results = YouTubeMusicService.search(fullQuery)
            isSearching = false
        } else {
            results = emptyList()
        }
    }

    // Song options dialog
    selectedSongForOptions?.let { song ->
        SongOptionsDialog(
            song = song,
            onDismiss = { selectedSongForOptions = null },
            onAddToPlaylistClick = { selectedSongForPlaylist = song }
        )
    }

    // Add to playlist dialog
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
            .padding(horizontal = 48.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Search Pill Input (Muzi Android style)
        Row(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF242424))
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, "Search", tint = TextSecondary, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(14.dp))
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                cursorBrush = SolidColor(Color.White),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { query = "" },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Close, "Clear", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Category Filter Chips
        LazyRow(
            modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filterTabs) { filter ->
                val isSelected = selectedFilter == filter
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) MuziAccent else Color(0xFF1E1E1E))
                        .clickable { selectedFilter = filter }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = filter,
                        color = if (isSelected) Color.White else TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Content Area
        if (query.isEmpty()) {
            // Search History
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
            ) {
                if (searchHistory.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Searches",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Clear All",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.clickable { LibraryManager.clearSearchHistory() }
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(searchHistory) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { query = item }
                                    .padding(vertical = 10.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.History, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(text = item, color = TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ArrowOutward, null, tint = Color(0x44FFFFFF), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.MusicNote, null, tint = Color(0x33FFFFFF), modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Search songs, artists, albums, or playlists", color = TextSecondary, fontSize = 15.sp)
                        }
                    }
                }
            }
        } else if (isSearching) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MuziAccent, strokeWidth = 3.dp)
            }
        } else {
            // Search Results Feed with Top Result Card
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top Result Hero Card (if available)
                if (results.isNotEmpty() && selectedFilter == "All") {
                    val topSong = results.first()
                    item {
                        Text(
                            text = "Top Result",
                            color = TextPrimary,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0x16FFFFFF))
                                .clickable {
                                    DesktopAudioPlayer.playSong(topSong, results)
                                    onSongClick(topSong)
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = topSong.thumbnailUrl,
                                contentDescription = topSong.title,
                                modifier = Modifier
                                    .size(92.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            Spacer(modifier = Modifier.width(20.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = topSong.title,
                                    color = TextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${topSong.artist} • Song",
                                    color = TextSecondary,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(
                                        onClick = {
                                            DesktopAudioPlayer.playSong(topSong, results)
                                            onSongClick(topSong)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MuziAccent),
                                        shape = RoundedCornerShape(18.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Play", fontSize = 13.sp)
                                    }
                                }
                            }
                            IconButton(onClick = { selectedSongForOptions = topSong }) {
                                Icon(Icons.Default.MoreVert, "Options", tint = TextSecondary)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Songs",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }

                // Songs List
                items(if (selectedFilter == "All" && results.size > 1) results.drop(1) else results) { song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                DesktopAudioPlayer.playSong(song, results)
                                onSongClick(song)
                            }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = song.thumbnailUrl,
                            contentDescription = song.title,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${song.artist} • ${song.durationText}",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { selectedSongForOptions = song }) {
                            Icon(Icons.Default.MoreVert, "Options", tint = TextSecondary, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}
