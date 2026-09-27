package com.muzi.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.muzi.desktop.audio.DesktopAudioPlayer
import com.muzi.desktop.data.LibraryManager
import com.muzi.desktop.data.UserPlaylist
import com.muzi.desktop.model.Song
import com.muzi.desktop.ui.components.SongOptionsDialog
import com.muzi.desktop.ui.theme.*

enum class LibraryTab {
    PLAYLISTS, SONGS, ALBUMS, ARTISTS
}

enum class LibraryTileFilter {
    ALL, LIKED, DOWNLOADED, EXPORTED, CACHED, TOP_50, LOCAL
}

@Composable
fun LibraryScreen(
    onSongClick: (Song) -> Unit,
    onPlaylistClick: (id: String, isAlbum: Boolean, title: String, thumbnail: String?) -> Unit = { _, _, _, _ -> },
    onArtistClick: (id: String, name: String, thumbnail: String?) -> Unit = { _, _, _ -> },
    onHistoryClick: () -> Unit = {},
    onStatsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val likedSongs by LibraryManager.likedSongs.collectAsState()
    val historySongs by LibraryManager.historySongs.collectAsState()
    val playlists by LibraryManager.playlists.collectAsState()
    val downloadedSongs = remember(likedSongs, historySongs) { LibraryManager.getDownloadedSongs() }
    val playCounts by LibraryManager.playCounts.collectAsState()

    val allSongs = remember(likedSongs, historySongs) {
        (likedSongs + historySongs).distinctBy { it.id }
    }
    val allAlbums = remember(likedSongs, historySongs) {
        (likedSongs + historySongs)
            .filter { it.album.isNotBlank() && it.album != "Single" }
            .groupBy { it.album }
            .map { (albumTitle, songs) ->
                val first = songs.first()
                Triple(first.id, albumTitle, first.thumbnailUrl)
            }
    }
    val allArtists = remember(likedSongs, historySongs) {
        (likedSongs + historySongs)
            .filter { it.artist.isNotBlank() }
            .groupBy { it.artist }
            .map { (artistName, songs) ->
                val first = songs.first()
                Triple(first.artist, artistName, first.thumbnailUrl)
            }
    }

    var selectedTab by remember { mutableStateOf(LibraryTab.PLAYLISTS) }
    var tileFilter by remember { mutableStateOf(LibraryTileFilter.ALL) }
    var selectedPlaylist by remember { mutableStateOf<UserPlaylist?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var selectedSongForOptions by remember { mutableStateOf<Song?>(null) }

    // Dialog for creating new playlist
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
                        focusedBorderColor = MuziBlue,
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
                    colors = ButtonDefaults.buttonColors(containerColor = MuziBlue)
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

    selectedSongForOptions?.let { song ->
        SongOptionsDialog(
            song = song,
            onDismiss = { selectedSongForOptions = null },
            onAddToPlaylistClick = {}
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 36.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Top Bar: Title "Library" & 4 Action Icons (Image 1 Parity)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Library",
                        color = TextPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(onClick = onHistoryClick) {
                            Icon(Icons.Default.History, "History", tint = TextPrimary, modifier = Modifier.size(22.dp))
                        }
                        IconButton(onClick = onStatsClick) {
                            Icon(Icons.Default.TrendingUp, "Stats", tint = TextPrimary, modifier = Modifier.size(22.dp))
                        }
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.Group, "Listen Together", tint = TextPrimary, modifier = Modifier.size(22.dp))
                        }
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Default.Settings, "Settings", tint = TextPrimary, modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }

            // Filter Tabs Row: Playlists | Songs | Albums | Artists (Image 1 Parity)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LibraryTabItem(
                        title = "Playlists",
                        isSelected = selectedTab == LibraryTab.PLAYLISTS,
                        onClick = { selectedTab = LibraryTab.PLAYLISTS; tileFilter = LibraryTileFilter.ALL }
                    )
                    LibraryTabItem(
                        title = "Songs",
                        isSelected = selectedTab == LibraryTab.SONGS,
                        onClick = { selectedTab = LibraryTab.SONGS }
                    )
                    LibraryTabItem(
                        title = "Albums",
                        isSelected = selectedTab == LibraryTab.ALBUMS,
                        onClick = { selectedTab = LibraryTab.ALBUMS }
                    )
                    LibraryTabItem(
                        title = "Artists",
                        isSelected = selectedTab == LibraryTab.ARTISTS,
                        onClick = { selectedTab = LibraryTab.ARTISTS }
                    )
                }
            }

            // Sort Button: "Date added" ^
            item {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF242426))
                        .clickable {}
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Date added",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = null,
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // 2-Column Quick Access Tiles (Image 1 Parity)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Row 1: Liked | Downloaded
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        LibraryTile(
                            icon = Icons.Default.Favorite,
                            iconTint = Color(0xFFEF4444),
                            title = "Liked",
                            badgeCount = likedSongs.size,
                            isSelected = tileFilter == LibraryTileFilter.LIKED,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                tileFilter = if (tileFilter == LibraryTileFilter.LIKED) LibraryTileFilter.ALL else LibraryTileFilter.LIKED
                            }
                        )
                        LibraryTile(
                            icon = Icons.Default.CheckCircle,
                            iconTint = TextPrimary,
                            title = "Downloaded",
                            badgeCount = downloadedSongs.size,
                            isSelected = tileFilter == LibraryTileFilter.DOWNLOADED,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                tileFilter = if (tileFilter == LibraryTileFilter.DOWNLOADED) LibraryTileFilter.ALL else LibraryTileFilter.DOWNLOADED
                            }
                        )
                    }

                    // Row 2: Exported | Cached
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        LibraryTile(
                            icon = Icons.Default.FileDownload,
                            iconTint = TextPrimary,
                            title = "Exported",
                            badgeCount = null,
                            isSelected = tileFilter == LibraryTileFilter.EXPORTED,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                tileFilter = if (tileFilter == LibraryTileFilter.EXPORTED) LibraryTileFilter.ALL else LibraryTileFilter.EXPORTED
                            }
                        )
                        LibraryTile(
                            icon = Icons.Default.Sync,
                            iconTint = TextPrimary,
                            title = "Cached",
                            badgeCount = downloadedSongs.size,
                            isSelected = tileFilter == LibraryTileFilter.CACHED,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                tileFilter = if (tileFilter == LibraryTileFilter.CACHED) LibraryTileFilter.ALL else LibraryTileFilter.CACHED
                            }
                        )
                    }

                    // Row 3: My top 50 | Local
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        LibraryTile(
                            icon = Icons.Default.TrendingUp,
                            iconTint = TextPrimary,
                            title = "My top 50",
                            badgeCount = historySongs.take(50).size,
                            isSelected = tileFilter == LibraryTileFilter.TOP_50,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                tileFilter = if (tileFilter == LibraryTileFilter.TOP_50) LibraryTileFilter.ALL else LibraryTileFilter.TOP_50
                            }
                        )
                        LibraryTile(
                            icon = Icons.Default.Folder,
                            iconTint = TextPrimary,
                            title = "Local",
                            badgeCount = null,
                            isSelected = tileFilter == LibraryTileFilter.LOCAL,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                tileFilter = if (tileFilter == LibraryTileFilter.LOCAL) LibraryTileFilter.ALL else LibraryTileFilter.LOCAL
                            }
                        )
                    }
                }
            }

            // Content Section
            if (tileFilter == LibraryTileFilter.LIKED) {
                item {
                    Text(
                        text = "Liked Songs (${likedSongs.size})",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(likedSongs) { song ->
                    LibrarySongRow(
                        song = song,
                        onClick = {
                            DesktopAudioPlayer.playSong(song, likedSongs)
                            onSongClick(song)
                        },
                        onOptionsClick = { selectedSongForOptions = song }
                    )
                }
            } else if (tileFilter == LibraryTileFilter.DOWNLOADED || tileFilter == LibraryTileFilter.CACHED) {
                item {
                    Text(
                        text = "Downloaded / Cached Songs (${downloadedSongs.size})",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(downloadedSongs) { song ->
                    LibrarySongRow(
                        song = song,
                        onClick = {
                            DesktopAudioPlayer.playSong(song, downloadedSongs)
                            onSongClick(song)
                        },
                        onOptionsClick = { selectedSongForOptions = song }
                    )
                }
            } else if (tileFilter == LibraryTileFilter.TOP_50) {
                val top50 = historySongs.sortedByDescending { playCounts[it.id] ?: 1 }.take(50)
                item {
                    Text(
                        text = "My Top 50 (${top50.size})",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(top50) { song ->
                    LibrarySongRow(
                        song = song,
                        onClick = {
                            DesktopAudioPlayer.playSong(song, top50)
                            onSongClick(song)
                        },
                        onOptionsClick = { selectedSongForOptions = song }
                    )
                }
            } else if (selectedTab == LibraryTab.SONGS) {
                item {
                    Text(
                        text = "All Songs (${allSongs.size})",
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(allSongs) { song ->
                    LibrarySongRow(
                        song = song,
                        onClick = {
                            DesktopAudioPlayer.playSong(song, allSongs)
                            onSongClick(song)
                        },
                        onOptionsClick = { selectedSongForOptions = song }
                    )
                }
            } else if (selectedTab == LibraryTab.ALBUMS) {
                item {
                    Text(
                        text = "Albums (${allAlbums.size})",
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        allAlbums.chunked(5).forEach { rowAlbums ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                rowAlbums.forEach { (id, title, thumb) ->
                                    Column(
                                        modifier = Modifier
                                            .width(140.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { onPlaylistClick(id, true, title, thumb) }
                                    ) {
                                        AsyncImage(
                                            model = thumb,
                                            contentDescription = title,
                                            modifier = Modifier.size(140.dp).clip(RoundedCornerShape(12.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = title,
                                            color = TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (selectedTab == LibraryTab.ARTISTS) {
                item {
                    Text(
                        text = "Artists (${allArtists.size})",
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        allArtists.chunked(5).forEach { rowArtists ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                rowArtists.forEach { (id, name, thumb) ->
                                    Column(
                                        modifier = Modifier
                                            .width(130.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { onArtistClick(id, name, thumb) },
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        AsyncImage(
                                            model = thumb,
                                            contentDescription = name,
                                            modifier = Modifier.size(110.dp).clip(CircleShape).background(SurfaceDark),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = name,
                                            color = TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Default: Playlists Header & List (Image 1 Parity)
                item {
                    Text(
                        text = "Playlists",
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (playlists.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No custom playlists yet", color = TextSecondary, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { showCreateDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MuziBlue),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Icon(Icons.Default.Add, null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Create Playlist")
                            }
                        }
                    }
                } else {
                    items(playlists) { playlist ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E1E22))
                                .clickable { selectedPlaylist = playlist }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF2C2D33)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.QueueMusic, null, tint = MuziBlue, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = playlist.title,
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${playlist.songs.size} songs",
                                    color = TextSecondary,
                                    fontSize = 13.sp
                                )
                            }
                            IconButton(onClick = { LibraryManager.deletePlaylist(playlist.id) }) {
                                Icon(Icons.Default.DeleteOutline, "Delete", tint = TextSecondary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            // Bottom space for floating bottom bars
            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }

        // Circular '+' Floating Action Button at bottom right (Image 1 Parity)
        FloatingActionButton(
            onClick = { showCreateDialog = true },
            containerColor = Color(0xFF2C2D35),
            contentColor = TextPrimary,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 36.dp, bottom = 120.dp)
                .size(54.dp)
        ) {
            Icon(Icons.Default.Add, "New Playlist", modifier = Modifier.size(26.dp))
        }
    }
}

@Composable
private fun LibraryTabItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Color(0xFF33353A) else Color(0xFF1E1F23))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 9.dp)
    ) {
        Text(
            text = title,
            color = if (isSelected) TextPrimary else TextSecondary,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun LibraryTile(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    badgeCount: Int?,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) MuziBlueContainer else Color(0xFF1A1B20))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        if (badgeCount != null) {
            Text(
                text = "$badgeCount",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun LibrarySongRow(
    song: Song,
    onClick: () -> Unit,
    onOptionsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = song.thumbnailUrl,
            contentDescription = song.title,
            modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${song.artist} • ${song.durationText}",
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onOptionsClick) {
            Icon(Icons.Default.MoreVert, "Options", tint = TextSecondary, modifier = Modifier.size(18.dp))
        }
    }
}
