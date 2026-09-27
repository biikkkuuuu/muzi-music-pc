package com.muzi.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import com.muzi.desktop.audio.DesktopAudioPlayer
import com.muzi.desktop.data.LibraryManager
import com.muzi.desktop.model.Song
import com.muzi.desktop.ui.theme.*

@Composable
fun SongOptionsDialog(
    song: Song,
    onDismiss: () -> Unit,
    onAddToPlaylistClick: () -> Unit
) {
    val isLiked = LibraryManager.isLiked(song.id)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            modifier = Modifier.width(380.dp).padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with Album Art & Title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = song.thumbnailUrl,
                        contentDescription = song.title,
                        modifier = Modifier.size(54.dp).clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
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

                HorizontalDivider(color = Color(0x22FFFFFF))

                // Menu Actions
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    MenuActionRow(icon = Icons.Default.PlayArrow, text = "Play Now") {
                        DesktopAudioPlayer.playSong(song)
                        onDismiss()
                    }
                    MenuActionRow(icon = Icons.Default.QueueMusic, text = "Play Next") {
                        val currentQueue = DesktopAudioPlayer.queue.value.toMutableList()
                        val nextIndex = (DesktopAudioPlayer.queueIndex.value + 1).coerceAtMost(currentQueue.size)
                        currentQueue.add(nextIndex, song)
                        onDismiss()
                    }
                    MenuActionRow(
                        icon = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        text = if (isLiked) "Remove from Liked" else "Add to Liked Songs"
                    ) {
                        LibraryManager.toggleLike(song)
                        onDismiss()
                    }
                    MenuActionRow(icon = Icons.Default.PlaylistAdd, text = "Add to Playlist...") {
                        onDismiss()
                        onAddToPlaylistClick()
                    }
                    MenuActionRow(icon = Icons.Default.Radio, text = "Start Radio from this Song") {
                        DesktopAudioPlayer.playSong(song)
                        onDismiss()
                    }
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E2E2E)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Text("Close", color = TextPrimary)
                }
            }
        }
    }
}

@Composable
fun AddToPlaylistDialog(
    song: Song,
    onDismiss: () -> Unit
) {
    val playlists by LibraryManager.playlists.collectAsState()
    var newPlaylistName by remember { mutableStateOf("") }
    var isCreatingNew by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            modifier = Modifier.width(420.dp).padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Add to Playlist",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                if (isCreatingNew) {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        placeholder = { Text("Playlist name...") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MuziAccent,
                            unfocusedBorderColor = Color(0x33FFFFFF)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { isCreatingNew = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E2E2E)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                if (newPlaylistName.isNotBlank()) {
                                    val created = LibraryManager.createPlaylist(newPlaylistName.trim())
                                    LibraryManager.addSongToPlaylist(created.id, song)
                                    onDismiss()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MuziAccent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Create & Add")
                        }
                    }
                } else {
                    Button(
                        onClick = { isCreatingNew = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MuziAccent.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, null, tint = MuziAccent, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("New Playlist", color = MuziAccent, fontWeight = FontWeight.SemiBold)
                    }

                    if (playlists.isEmpty()) {
                        Text(
                            text = "No custom playlists yet. Create one above!",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 240.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(playlists) { playlist ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0x11FFFFFF))
                                        .clickable {
                                            LibraryManager.addSongToPlaylist(playlist.id, song)
                                            onDismiss()
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.QueueMusic,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = playlist.title,
                                            color = TextPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "${playlist.songs.size} songs",
                                            color = TextSecondary,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E2E2E)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Text("Cancel", color = TextPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuActionRow(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = text, tint = TextSecondary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(text = text, color = TextPrimary, fontSize = 14.sp)
    }
}
