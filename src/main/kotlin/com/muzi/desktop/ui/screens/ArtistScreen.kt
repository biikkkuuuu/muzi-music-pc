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
 * Replicating Android Muzi's ArtistScreen.kt
 * Dedicated artist profile with circular banner, subscriber stats, popular songs, and discography.
 */
@Composable
fun ArtistScreen(
    artistId: String,
    fallbackName: String = "Artist",
    fallbackThumbnail: String? = null,
    onBackClick: () -> Unit,
    onSongClick: (Song) -> Unit,
    onAlbumClick: (albumId: String, title: String, thumbnail: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var artistDetails by remember { mutableStateOf<YouTubeMusicService.ArtistDetails?>(null) }
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

    LaunchedEffect(artistId) {
        isLoading = true
        artistDetails = YouTubeMusicService.getArtist(artistId)
        isLoading = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(horizontal = 40.dp, vertical = 24.dp)
    ) {
        // Back Button
        IconButton(
            onClick = onBackClick,
            modifier = Modifier.size(40.dp).padding(bottom = 8.dp)
        ) {
            Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MuziAccent, strokeWidth = 3.dp)
            }
        } else {
            val artist = artistDetails
            val name = artist?.name ?: fallbackName
            val thumb = artist?.thumbnailUrl ?: fallbackThumbnail
            val topSongs = artist?.topSongs ?: emptyList()
            val albums = artist?.albums ?: emptyList()

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Artist Hero Profile
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = thumb,
                            contentDescription = name,
                            modifier = Modifier
                                .size(160.dp)
                                .clip(CircleShape)
                                .background(SurfaceDark),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.width(32.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = name,
                                color = TextPrimary,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            val stats = listOfNotNull(
                                artist?.subscriberCount,
                                artist?.monthlyListeners
                            ).joinToString(" • ")

                            if (stats.isNotBlank()) {
                                Text(
                                    text = stats,
                                    color = TextSecondary,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(18.dp))
                            } else {
                                Spacer(modifier = Modifier.height(14.dp))
                            }

                            // Actions
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                Button(
                                    onClick = {
                                        if (topSongs.isNotEmpty()) {
                                            DesktopAudioPlayer.playSong(topSongs.first(), topSongs)
                                            onSongClick(topSongs.first())
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
                                        if (topSongs.isNotEmpty()) {
                                            val shuffled = topSongs.shuffled()
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

                // Top Songs Section
                if (topSongs.isNotEmpty()) {
                    item {
                        Text(
                            text = "Popular Songs",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    itemsIndexed(topSongs.take(8)) { index, song ->
                        val isCurrent = currentPlayingSong?.id == song.id
                        val isLiked = LibraryManager.isLiked(song.id)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isCurrent) MuziBlueContainer.copy(alpha = 0.35f) else Color.Transparent)
                                .clickable {
                                    DesktopAudioPlayer.playSong(song, topSongs)
                                    onSongClick(song)
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "%02d".format(index + 1),
                                color = if (isCurrent) MuziAccent else TextSecondary,
                                fontSize = 14.sp,
                                modifier = Modifier.width(32.dp)
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
                                Text(
                                    text = song.album,
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Text(
                                text = song.durationText,
                                color = TextSecondary,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

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

                // Discography / Albums Section
                if (albums.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Albums & Singles",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            items(albums) { album ->
                                Column(
                                    modifier = Modifier
                                        .width(140.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            onAlbumClick(album.id, album.title, album.thumbnailUrl)
                                        }
                                ) {
                                    AsyncImage(
                                        model = album.thumbnailUrl,
                                        contentDescription = album.title,
                                        modifier = Modifier.size(140.dp).clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = album.title,
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    album.year?.let {
                                        Text(text = it, color = TextSecondary, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
