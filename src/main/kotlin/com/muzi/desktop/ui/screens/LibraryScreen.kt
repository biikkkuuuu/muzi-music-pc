package com.muzi.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.muzi.desktop.audio.DesktopAudioPlayer
import com.muzi.desktop.data.LibraryManager
import com.muzi.desktop.data.UserPlaylist
import com.muzi.desktop.model.Song
import com.muzi.desktop.ui.theme.*

enum class LibrarySubView {
    MAIN, FAVOURITES, DOWNLOADS, HISTORY, PLAYLIST_DETAILS
}

@Composable
fun LibraryScreen(
    onSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val likedSongs by LibraryManager.likedSongs.collectAsState()
    val historySongs by LibraryManager.historySongs.collectAsState()
    val playlists by LibraryManager.playlists.collectAsState()
    val downloadedSongs = remember(likedSongs, historySongs) { LibraryManager.getDownloadedSongs() }

    var subView by remember { mutableStateOf(LibrarySubView.MAIN) }
    var selectedPlaylist by remember { mutableStateOf<UserPlaylist?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Playlist", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("Playlist Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MuziAccent,
                        unfocusedBorderColor = Color(0x66FFFFFF)
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            LibraryManager.createPlaylist(newPlaylistName)
                            newPlaylistName = ""
                            showCreateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MuziAccent)
                ) {
                    Text("Create", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = Color(0xFF1E1E1E)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(horizontal = 48.dp, vertical = 24.dp)
    ) {
        if (subView == LibrarySubView.MAIN) {
            // Header: Saved (Desktop-4.png)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Library",
                    color = TextPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.Add, "New Playlist", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Big Cards: Favourites, Downloads, History (Desktop-4.png)
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                LibraryCard(
                    icon = Icons.Default.Favorite,
                    title = "Favourites",
                    subtitle = "${likedSongs.size} Songs",
                    onClick = { subView = LibrarySubView.FAVOURITES }
                )
                LibraryCard(
                    icon = Icons.Default.Download,
                    title = "Downloads",
                    subtitle = "${downloadedSongs.size} Offline Songs",
                    onClick = { subView = LibrarySubView.DOWNLOADS }
                )
                LibraryCard(
                    icon = Icons.Default.History,
                    title = "History",
                    subtitle = "${historySongs.size} Songs",
                    onClick = { subView = LibrarySubView.HISTORY }
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // User Custom Playlists Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your Playlists (${playlists.size})",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (playlists.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No custom playlists yet. Tap '+' to create one!", color = TextSecondary, fontSize = 14.sp)
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(playlists) { pl ->
                        Column(
                            modifier = Modifier
                                .width(140.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedPlaylist = pl
                                    subView = LibrarySubView.PLAYLIST_DETAILS
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(140.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF222222)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.QueueMusic, null, tint = Color.White, modifier = Modifier.size(36.dp))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = pl.title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text(text = "${pl.songs.size} songs", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
        } else {
            // Sub-view: Favourites / Downloads / History / Playlist Details
            val (title, songs) = when (subView) {
                LibrarySubView.FAVOURITES -> "Favourites" to likedSongs
                LibrarySubView.DOWNLOADS -> "Downloads (Offline)" to downloadedSongs
                LibrarySubView.HISTORY -> "History" to historySongs
                LibrarySubView.PLAYLIST_DETAILS -> (selectedPlaylist?.title ?: "Playlist") to (selectedPlaylist?.songs ?: emptyList())
                else -> "" to emptyList()
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { subView = LibrarySubView.MAIN }) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (songs.isNotEmpty()) {
                    Button(
                        onClick = {
                            DesktopAudioPlayer.playSong(songs.first(), songs)
                            onSongClick(songs.first())
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF262626)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Play all", color = TextPrimary, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (songs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No songs here yet", color = TextSecondary, fontSize = 16.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(songs) { song ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    DesktopAudioPlayer.playSong(song, songs)
                                    onSongClick(song)
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = song.thumbnailUrl,
                                contentDescription = song.title,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(6.dp))
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
                                Text(
                                    text = "${song.artist} • ${song.durationText}",
                                    color = TextSecondary,
                                    fontSize = 13.sp,
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
}

@Composable
private fun LibraryCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(180.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF181818)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = subtitle,
            color = TextSecondary,
            fontSize = 13.sp
        )
    }
}
