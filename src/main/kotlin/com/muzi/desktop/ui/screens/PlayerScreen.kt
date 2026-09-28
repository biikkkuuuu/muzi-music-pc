package com.muzi.desktop.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.muzi.desktop.audio.DesktopAudioPlayer
import com.muzi.desktop.audio.RepeatMode
import com.muzi.desktop.data.LibraryManager
import com.muzi.desktop.lyrics.LyricsProvider
import com.muzi.desktop.model.LyricsLine
import com.muzi.desktop.ui.components.AddToPlaylistDialog
import com.muzi.desktop.ui.components.AmbientGlowBackground
import com.muzi.desktop.ui.components.SongOptionsDialog
import com.muzi.desktop.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 100% Google Material 3 Desktop Player Screen for Muzi Music.
 * Pixel-accurate to Screenshots/Desktop-2.png
 */
@Composable
fun PlayerScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentSong by DesktopAudioPlayer.currentSong.collectAsState()
    val queue by DesktopAudioPlayer.queue.collectAsState()
    val queueIndex by DesktopAudioPlayer.queueIndex.collectAsState()
    val isPlaying by DesktopAudioPlayer.isPlaying.collectAsState()
    val isBuffering by DesktopAudioPlayer.isBuffering.collectAsState()
    val positionMillis by DesktopAudioPlayer.currentPositionMillis.collectAsState()
    val durationMillis by DesktopAudioPlayer.durationMillis.collectAsState()
    val volume by DesktopAudioPlayer.volume.collectAsState()
    val isShuffle by DesktopAudioPlayer.isShuffle.collectAsState()
    val repeatMode by DesktopAudioPlayer.repeatMode.collectAsState()
    val dynamicThemeColor by DesktopAudioPlayer.dynamicThemeColor.collectAsState()
    val ambientPalette by DesktopAudioPlayer.ambientPalette.collectAsState()
    val sleepTimerRemaining by DesktopAudioPlayer.sleepTimerRemainingMillis.collectAsState()

    val animatedAccent by animateColorAsState(dynamicThemeColor, animationSpec = tween(900))
    val isLiked = currentSong?.let { LibraryManager.isLiked(it.id) } ?: false

    var showQueueDrawer by remember { mutableStateOf(false) }
    var showOptionsDialog by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showEqDialog by remember { mutableStateOf(false) }
    var lyrics by remember { mutableStateOf<List<LyricsLine>>(emptyList()) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    val clipboardManager = LocalClipboardManager.current
    val lyricsListState = rememberLazyListState()
    val queueListState = rememberLazyListState()

    // Fetch synced lyrics
    LaunchedEffect(currentSong) {
        currentSong?.let { song ->
            lyrics = LyricsProvider.getLyrics(song.title, song.artist)
        }
    }

    // Active lyrics line calculation
    val activeLyricIndex = remember(positionMillis, lyrics) {
        if (lyrics.isEmpty()) -1
        else lyrics.indexOfLast { it.timeMillis <= positionMillis }.coerceAtLeast(0)
    }

    // Auto-scroll lyrics smoothly
    LaunchedEffect(activeLyricIndex) {
        if (lyrics.isNotEmpty() && activeLyricIndex in lyrics.indices) {
            lyricsListState.animateScrollToItem(
                index = (activeLyricIndex - 2).coerceAtLeast(0),
                scrollOffset = -80
            )
        }
    }

    // Toast feedback helper
    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(2000)
            toastMessage = null
        }
    }

    // Dialogs
    if (showOptionsDialog && currentSong != null) {
        SongOptionsDialog(
            song = currentSong!!,
            onDismiss = { showOptionsDialog = false },
            onAddToPlaylistClick = {
                showOptionsDialog = false
                showAddToPlaylistDialog = true
            }
        )
    }

    if (showAddToPlaylistDialog && currentSong != null) {
        AddToPlaylistDialog(
            song = currentSong!!,
            onDismiss = { showAddToPlaylistDialog = false }
        )
    }

    if (showSleepTimerDialog) {
        SleepTimerBottomSheet(
            activeRemainingMillis = sleepTimerRemaining,
            onSelectMinutes = { mins ->
                DesktopAudioPlayer.startSleepTimer(mins)
                showSleepTimerDialog = false
                toastMessage = "Sleep timer set for $mins minutes"
            },
            onSelectEndOfTrack = {
                DesktopAudioPlayer.startSleepTimerEndOfTrack()
                showSleepTimerDialog = false
                toastMessage = "Sleep timer set to end of current track"
            },
            onCancelTimer = {
                DesktopAudioPlayer.cancelSleepTimer()
                showSleepTimerDialog = false
                toastMessage = "Sleep timer cancelled"
            },
            onDismiss = { showSleepTimerDialog = false },
            accentColor = animatedAccent
        )
    }

    if (showEqDialog) {
        EqualizerDialog(
            onDismiss = { showEqDialog = false },
            accentColor = animatedAccent
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Multi-point dynamic ambient mesh glow (Material 3 Dynamic Theming)
        AmbientGlowBackground(
            colors = ambientPalette,
            modifier = Modifier.fillMaxSize()
        )

        // Contrast scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.45f),
                            Color.Black.copy(alpha = 0.30f),
                            Color.Black.copy(alpha = 0.65f)
                        )
                    )
                )
        )

        // Main Layout (Desktop 2-Column Parity from Screenshots/Desktop-2.png)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp, vertical = 24.dp)
        ) {
            // TOP BAR: Collapse Chevron (Left) + Queue Toggle (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse Player",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showEqDialog = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = "Equalizer",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = { showSleepTimerDialog = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Bedtime,
                            contentDescription = "Sleep Timer",
                            tint = if (sleepTimerRemaining != null) animatedAccent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = { showQueueDrawer = !showQueueDrawer },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.QueueMusic,
                            contentDescription = "Queue",
                            tint = if (showQueueDrawer) animatedAccent else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2-COLUMN SPLIT: LEFT = ARTWORK & CONTROLS, RIGHT = SYNCED KARAOKE LYRICS
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(56.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LEFT COLUMN: Big Album Art + Song Metadata + Seek Slider + Control Buttons
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Big Album Artwork (Desktop-2.png)
                    Box(
                        modifier = Modifier
                            .size(380.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (currentSong?.thumbnailUrl?.isNotEmpty() == true) {
                            AsyncImage(
                                model = currentSong?.thumbnailUrl,
                                contentDescription = currentSong?.title,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(96.dp)
                            )
                        }

                        if (isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                color = animatedAccent,
                                strokeWidth = 3.dp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Song Title & Artist + Heart & More Options (Desktop-2.png)
                    Row(
                        modifier = Modifier.width(380.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = currentSong?.title ?: "No Track Playing",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp)
                                    .clickable {
                                        currentSong?.let {
                                            clipboardManager.setText(AnnotatedString(it.title))
                                            toastMessage = "Copied title"
                                        }
                                    }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = currentSong?.artist ?: "Unknown Artist",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 15.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.clickable {
                                    currentSong?.let {
                                        clipboardManager.setText(AnnotatedString(it.artist))
                                        toastMessage = "Copied artist"
                                    }
                                }
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { DesktopAudioPlayer.toggleLikeCurrentSong() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Like",
                                    tint = if (isLiked) animatedAccent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            IconButton(
                                onClick = { showOptionsDialog = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.MoreHoriz,
                                    contentDescription = "More Options",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Android Animated Squiggly / Wavy Seekbar (sc_3.png & Desktop-2.png)
                    Column(modifier = Modifier.width(380.dp)) {
                        val progress = if (durationMillis > 0) {
                            (positionMillis.toFloat() / durationMillis.toFloat()).coerceIn(0f, 1f)
                        } else 0f

                        com.muzi.desktop.ui.components.SquigglySlider(
                            value = progress,
                            onValueChange = { newProg ->
                                val targetMs = (newProg * durationMillis).toLong()
                                DesktopAudioPlayer.seekTo(targetMs)
                            },
                            isPlaying = isPlaying,
                            colors = SliderDefaults.colors(
                                activeTrackColor = Color.White,
                                inactiveTrackColor = Color.White.copy(alpha = 0.25f),
                                thumbColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth().height(40.dp)
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatTime(positionMillis),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                            )
                            Text(
                                text = formatTime(durationMillis),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Material 3 Playback Controls (Shuffle, Prev, Big White Play/Pause Circle, Next, Repeat)
                    Row(
                        modifier = Modifier.width(380.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Shuffle
                        IconButton(
                            onClick = { DesktopAudioPlayer.toggleShuffle() },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.Shuffle,
                                contentDescription = "Shuffle",
                                tint = if (isShuffle) animatedAccent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Skip Previous
                        IconButton(
                            onClick = { DesktopAudioPlayer.playPrevious() },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Default.SkipPrevious,
                                contentDescription = "Previous",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        // Big Central Play/Pause Button (Solid Pure White Circle with Black Icon)
                        val playInteraction = remember { MutableInteractionSource() }
                        val isPlayPressed by playInteraction.collectIsPressedAsState()
                        val playScale by animateFloatAsState(
                            targetValue = if (isPlayPressed) 0.90f else 1f,
                            animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f)
                        )

                        FilledIconButton(
                            onClick = { DesktopAudioPlayer.togglePlayPause() },
                            shape = CircleShape,
                            interactionSource = playInteraction,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            ),
                            modifier = Modifier
                                .size(64.dp)
                                .graphicsLayer { scaleX = playScale; scaleY = playScale }
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        // Skip Next
                        IconButton(
                            onClick = { DesktopAudioPlayer.playNext() },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Default.SkipNext,
                                contentDescription = "Next",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        // Repeat Mode (OFF -> ALL -> ONE)
                        IconButton(
                            onClick = { DesktopAudioPlayer.toggleRepeat() },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = when (repeatMode) {
                                    RepeatMode.ONE -> Icons.Default.RepeatOne
                                    else -> Icons.Default.Repeat
                                },
                                contentDescription = "Repeat",
                                tint = if (repeatMode != RepeatMode.OFF) animatedAccent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // RIGHT COLUMN: Apple Music / Material 3 Interactive Synced Karaoke Lyrics (Desktop-2.png)
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    if (showQueueDrawer) {
                        // Queue Drawer on the right side
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.Black.copy(alpha = 0.40f))
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Up Next (${queue.size})",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                TextButton(onClick = { showQueueDrawer = false }) {
                                    Text("Lyrics", color = animatedAccent, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            LazyColumn(
                                state = queueListState,
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(queue) { index, item ->
                                    val isCurrent = index == queueIndex
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isCurrent) animatedAccent.copy(alpha = 0.20f) else Color(0x11FFFFFF))
                                            .clickable { DesktopAudioPlayer.playSongAt(index) }
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncImage(
                                            model = item.thumbnailUrl,
                                            contentDescription = item.title,
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.title,
                                                color = if (isCurrent) animatedAccent else Color.White,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = item.artist,
                                                color = Color.White.copy(alpha = 0.6f),
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        if (isCurrent) {
                                            Icon(
                                                Icons.Default.GraphicEq,
                                                contentDescription = "Playing",
                                                tint = animatedAccent,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Symmetrical Center Synced Lyrics View (Desktop-2.png)
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // Top 🎵 Music Note Icon (Desktop-2.png)
                            Icon(
                                Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.40f),
                                modifier = Modifier.size(24.dp).padding(bottom = 8.dp)
                            )

                            com.muzi.desktop.ui.components.KaraokeLyricsView(
                                lyrics = lyrics,
                                positionMillis = positionMillis,
                                isPlaying = isPlaying,
                                accentColor = animatedAccent,
                                listState = lyricsListState,
                                onSeekTo = { DesktopAudioPlayer.seekTo(it) },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }

        // Floating Toast Feedback Banner
        if (toastMessage != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xE61E1E1E))
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    text = toastMessage!!,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                )
            }
        }
    }
}

/**
 * Material 3 Sleep Timer Dialog.
 */
@Composable
private fun SleepTimerBottomSheet(
    activeRemainingMillis: Long?,
    onSelectMinutes: (Int) -> Unit,
    onSelectEndOfTrack: () -> Unit,
    onCancelTimer: () -> Unit,
    onDismiss: () -> Unit,
    accentColor: Color
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bedtime, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Sleep Timer", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (activeRemainingMillis != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Active: ${formatTime(activeRemainingMillis)} remaining",
                                color = accentColor,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            TextButton(onClick = onCancelTimer) {
                                Text("Turn Off", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                val options = listOf(5, 10, 15, 30, 45, 60)
                options.forEach { mins ->
                    SleepTimerOptionItem(
                        title = "$mins minutes",
                        onClick = { onSelectMinutes(mins) }
                    )
                }

                SleepTimerOptionItem(
                    title = "End of current track",
                    onClick = onSelectEndOfTrack
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
        }
    )
}

@Composable
private fun SleepTimerOptionItem(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
    }
}

/**
 * Material 3 Equalizer Modal Dialog.
 */
@Composable
private fun EqualizerDialog(
    onDismiss: () -> Unit,
    accentColor: Color
) {
    var isEnabled by remember { mutableStateOf(true) }
    var selectedPreset by remember { mutableStateOf("Rock") }
    var bassBoost by remember { mutableFloatStateOf(0.70f) }
    var surround3d by remember { mutableFloatStateOf(0.40f) }
    var band60 by remember { mutableFloatStateOf(4f) }
    var band230 by remember { mutableFloatStateOf(2f) }
    var band910 by remember { mutableFloatStateOf(-1f) }
    var band3k by remember { mutableFloatStateOf(3f) }
    var band14k by remember { mutableFloatStateOf(5f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Equalizer", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                }
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { isEnabled = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accentColor)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Presets Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Flat", "Rock", "Pop", "Jazz", "Electronic").forEach { preset ->
                        val isSelected = selectedPreset == preset
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) accentColor else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { selectedPreset = preset }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = preset,
                                color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                // 5-Band Sliders Display
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val bands = listOf(
                        Triple("60Hz", band60) { v: Float -> band60 = v },
                        Triple("230Hz", band230) { v: Float -> band230 = v },
                        Triple("910Hz", band910) { v: Float -> band910 = v },
                        Triple("3.6kHz", band3k) { v: Float -> band3k = v },
                        Triple("14kHz", band14k) { v: Float -> band14k = v }
                    )

                    bands.forEach { (label, value, onValChange) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxHeight().weight(1f)
                        ) {
                            Text(
                                text = "${value.toInt()}dB",
                                color = accentColor,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Slider(
                                value = (value + 10f) / 20f,
                                onValueChange = { onValChange(it * 20f - 10f) },
                                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = accentColor),
                                modifier = Modifier.height(60.dp).graphicsLayer { rotationZ = 270f }
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                // Bass Boost & 3D Surround Sliders
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Bass Boost", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                        Slider(
                            value = bassBoost,
                            onValueChange = { bassBoost = it },
                            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = accentColor)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("3D Surround", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                        Slider(
                            value = surround3d,
                            onValueChange = { surround3d = it },
                            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = accentColor)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black)
            ) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        }
    )
}

private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
