package com.muzi.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.layout.ContentScale
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

/**
 * Replicating Android Muzi's OnlinePlaylistScreen.kt & AlbumScreen.kt
 * Dedicated detail screen for playlists and albums with full tracklist and playback controls.
 */
@Composable
fun PlaylistDetailScreen(
    playlistId: String?,
    albumId: String?,
    fallbackTitle: String = "Playlist",
    fallbackThumbnail: String? = null,
    onBackClick: () -> Unit,
    onSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf(fallbackTitle) }
    var author by remember { mutableStateOf("") }
    var thumbnailUrl by remember { mutableStateOf(fallbackThumbnail) }
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var selectedSongForOptions by remember { mutableStateOf<Song?>(null) }
    var selectedSongForPlaylist by remember { mutableStateOf<Song?>(null) }

    val currentPlayingSong by DesktopAudioPlayer.currentSong.collectAsState()
    val isPlaying by DesktopAudioPlayer.isPlaying.collectAsState()

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

    LaunchedEffect(playlistId, albumId) {
        isLoading = true
        if (!playlistId.isNullOrBlank()) {
            val res = YouTubeMusicService.getPlaylist(playlistId)
            if (res != null) {
                title = res.title
                author = res.author
                thumbnailUrl = res.thumbnailUrl ?: fallbackThumbnail
                songs = res.songs
            }
        } else if (!albumId.isNullOrBlank()) {
            val res = YouTubeMusicService.getAlbum(albumId)
            if (res != null) {
                title = res.title
                author = res.artist
                thumbnailUrl = res.thumbnailUrl ?: fallbackThumbnail
                songs = res.songs
            }
        }
        isLoading = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(horizontal = 40.dp, vertical = 24.dp)
    ) {
        // Back Navigation
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = if (albumId != null) "Album" else "Playlist",
                color = TextSecondary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MuziAccent, strokeWidth = 3.dp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Hero Header Card
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = thumbnailUrl,
                            contentDescription = title,
                            modifier = Modifier
                                .size(180.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(SurfaceDark),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.width(28.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                color = TextPrimary,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = author.ifBlank { "YouTube Music" },
                                color = TextSecondary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${songs.size} tracks",
                                color = Color(0xFF71717A),
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(20.dp))

                            // Play & Shuffle Action Buttons
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                Button(
                                    onClick = {
                                        if (songs.isNotEmpty()) {
                                            DesktopAudioPlayer.playSong(songs.first(), songs)
                                            onSongClick(songs.first())
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MuziAccent),
                                    shape = RoundedCornerShape(24.dp),
                                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Play", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        if (songs.isNotEmpty()) {
                                            val shuffled = songs.shuffled()
                                            DesktopAudioPlayer.playSong(shuffled.first(), shuffled)
                                            onSongClick(shuffled.first())
                                        }
                                    },
                                    shape = RoundedCornerShape(24.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                                ) {
                                    Icon(Icons.Default.Shuffle, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Shuffle", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }

                // Tracklist Header
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tracks",
                        color = TextPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Songs Table
                itemsIndexed(songs) { index, song ->
                    val isCurrent = currentPlayingSong?.id == song.id
                    val isLiked = LibraryManager.isLiked(song.id)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isCurrent) MuziBlueContainer.copy(alpha = 0.35f) else Color.Transparent)
                            .clickable {
                                DesktopAudioPlayer.playSong(song, songs)
                                onSongClick(song)
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Track Number or Playing Indicator
                        Box(
                            modifier = Modifier.width(36.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (isCurrent && isPlaying) {
                                Icon(Icons.Default.GraphicEq, null, tint = MuziAccent, modifier = Modifier.size(20.dp))
                            } else {
                                Text(
                                    text = "%02d".format(index + 1),
                                    color = if (isCurrent) MuziAccent else TextSecondary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Thumbnail
                        AsyncImage(
                            model = song.thumbnailUrl,
                            contentDescription = song.title,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        // Title & Artist
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
                                text = song.artist,
                                color = TextSecondary,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Duration
                        Text(
                            text = song.durationText,
                            color = TextSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        // Like Heart
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

                        // 3-dots Context Menu
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
