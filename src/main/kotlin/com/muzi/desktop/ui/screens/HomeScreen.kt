package com.muzi.desktop.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import com.muzi.desktop.data.LibraryManager
import com.muzi.desktop.innertube.YouTubeMusicService
import com.muzi.desktop.model.ChartItem
import com.muzi.desktop.model.Song
import com.muzi.desktop.ui.components.AddToPlaylistDialog
import com.muzi.desktop.ui.components.SongOptionsDialog
import com.muzi.desktop.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onSongClick: (Song) -> Unit,
    onPlaylistClick: (id: String, isAlbum: Boolean, title: String, thumbnail: String?) -> Unit = { _, _, _, _ -> },
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var homeFeed by remember { mutableStateOf<YouTubeMusicService.HomeFeed?>(null) }
    var charts by remember { mutableStateOf<List<ChartItem>>(emptyList()) }
    var fallbackQuickPicks by remember { mutableStateOf<List<Song>>(emptyList()) }
    var selectedChip by remember { mutableStateOf<HomePage.Chip?>(null) }
    var isLoadingFeed by remember { mutableStateOf(true) }

    // Adaptive Data from local listening history
    val historySongs by LibraryManager.historySongs.collectAsState()
    val playCounts by LibraryManager.playCounts.collectAsState()

    val speedDialSongs = remember(historySongs, playCounts) {
        LibraryManager.getSpeedDialSongs(6)
    }
    val forgottenFavorites = remember(historySongs, playCounts) {
        LibraryManager.getForgottenFavorites()
    }

    var dailyDiscoverSong by remember { mutableStateOf<Song?>(null) }
    var dailyDiscoverSeed by remember { mutableStateOf<Song?>(null) }
    var similarSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var similarSeed by remember { mutableStateOf<Song?>(null) }

    val dynamicColor by DesktopAudioPlayer.dynamicThemeColor.collectAsState()
    val animatedAccent by animateColorAsState(dynamicColor, animationSpec = tween(600))

    var selectedSongForOptions by remember { mutableStateOf<Song?>(null) }
    var selectedSongForPlaylist by remember { mutableStateOf<Song?>(null) }

    val defaultMoodChips = listOf(
        "Feel good", "Relax", "Romance", "Energize", "Workout", "Commute", "Party", "Focus", "Sad", "Sleep"
    )

    // Initial Load (Fresh Install / Static Feed)
    LaunchedEffect(Unit) {
        charts = YouTubeMusicService.getBrowseCharts()
        val feed = YouTubeMusicService.getHomeFeed()
        if (feed != null && feed.sections.isNotEmpty()) {
            homeFeed = feed
        } else {
            fallbackQuickPicks = YouTubeMusicService.getQuickPicks()
        }
        isLoadingFeed = false
    }

    // Adaptive Listening Listener
    LaunchedEffect(historySongs) {
        if (historySongs.isNotEmpty()) {
            val seed = LibraryManager.getDailyDiscoverSeed()
            if (seed != null && seed.id != dailyDiscoverSeed?.id) {
                dailyDiscoverSeed = seed
                val recs = YouTubeMusicService.fetchRadioQueue(seed.id)
                dailyDiscoverSong = recs.firstOrNull { it.id != seed.id }
            }
            val recent = historySongs.firstOrNull()
            if (recent != null && recent.id != similarSeed?.id) {
                similarSeed = recent
                similarSongs = YouTubeMusicService.fetchRadioQueue(recent.id).filter { it.id != recent.id }
            }
        }
    }

    // Chip Filter Change
    LaunchedEffect(selectedChip) {
        if (selectedChip != null) {
            isLoadingFeed = true
            val filtered = YouTubeMusicService.getHomeFeed(selectedChip?.endpoint?.params)
            if (filtered != null && filtered.sections.isNotEmpty()) {
                homeFeed = filtered
            } else {
                fallbackQuickPicks = YouTubeMusicService.search("${selectedChip?.title} Songs")
            }
            isLoadingFeed = false
        }
    }

    // Dialogs
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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(horizontal = 36.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        // App Header: "Muzi Music" + 4 Action Icons (Image 4 Parity)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Muzi Music",
                    color = TextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.History, "History", tint = TextPrimary, modifier = Modifier.size(22.dp))
                    }
                    IconButton(onClick = {}) {
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

        // Mood Filter Chips Row
        item {
            val chips = homeFeed?.chips
            if (!chips.isNullOrEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(chips) { chip ->
                        val isSelected = chip.title == selectedChip?.title
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) MuziBlueContainer else ChipBackground)
                                .clickable { selectedChip = if (isSelected) null else chip }
                                .padding(horizontal = 18.dp, vertical = 9.dp)
                        ) {
                            Text(
                                text = chip.title,
                                color = if (isSelected) Color.White else TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(defaultMoodChips) { chipName ->
                        val isSelected = chipName == selectedChip?.title
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) MuziBlueContainer else ChipBackground)
                                .clickable {
                                    coroutineScope.launch {
                                        isLoadingFeed = true
                                        fallbackQuickPicks = YouTubeMusicService.search("$chipName Songs")
                                        isLoadingFeed = false
                                    }
                                }
                                .padding(horizontal = 18.dp, vertical = 9.dp)
                        ) {
                            Text(
                                text = chipName,
                                color = if (isSelected) Color.White else TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // ADAPTIVE SECTION 1: Speed Dial Grid
        // (Appears adaptively once user listens to songs)
        // ==========================================
        if (selectedChip == null && speedDialSongs.isNotEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Speed dial",
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    val chunked = speedDialSongs.chunked(3)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        chunked.forEach { rowSongs ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                rowSongs.forEach { song ->
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF1A1A1A))
                                            .clickable {
                                                DesktopAudioPlayer.playSong(song, speedDialSongs)
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
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = song.title,
                                                color = TextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = song.artist,
                                                color = TextSecondary,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        IconButton(
                                            onClick = { selectedSongForOptions = song },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MoreVert,
                                                contentDescription = "Options",
                                                tint = TextSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                                repeat(3 - rowSongs.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // ADAPTIVE SECTION 2: Daily Discover (Hero Card)
        // (Appears adaptively based on user's seed track)
        // ==========================================
        if (selectedChip == null && dailyDiscoverSong != null && dailyDiscoverSeed != null) {
            item {
                val seed = dailyDiscoverSeed!!
                val rec = dailyDiscoverSong!!
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF181818))
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = rec.thumbnailUrl,
                            contentDescription = rec.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.94f),
                                            Color.Black.copy(alpha = 0.82f),
                                            Color.Black.copy(alpha = 0.35f)
                                        )
                                    )
                                )
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "BECAUSE YOU LISTENED TO ${seed.title.uppercase()}",
                                    color = animatedAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = rec.title,
                                    color = TextPrimary,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${rec.artist} • ${rec.durationText}",
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { selectedSongForOptions = rec },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Options",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                IconButton(
                                    onClick = {
                                        DesktopAudioPlayer.playSong(rec, listOf(rec))
                                        onSongClick(rec)
                                    },
                                    modifier = Modifier
                                        .size(52.dp)
                                        .background(animatedAccent, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // ADAPTIVE SECTION 3: Keep Listening / Recently Played
        // (Appears adaptively as history accumulates)
        // ==========================================
        if (historySongs.isNotEmpty() && selectedChip == null) {
            item {
                Column {
                    Text(
                        text = "Keep listening",
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(historySongs.take(12)) { song ->
                            Column(
                                modifier = Modifier
                                    .width(136.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        DesktopAudioPlayer.playSong(song, historySongs)
                                        onSongClick(song)
                                    }
                            ) {
                                Box {
                                    AsyncImage(
                                        model = song.thumbnailUrl,
                                        contentDescription = song.title,
                                        modifier = Modifier.size(136.dp).clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    IconButton(
                                        onClick = { selectedSongForOptions = song },
                                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(28.dp)
                                    ) {
                                        Icon(Icons.Default.MoreVert, "Options", tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
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

        // ==========================================
        // ADAPTIVE SECTION 4: Similar to [Seed Track]
        // ==========================================
        if (selectedChip == null && similarSongs.isNotEmpty() && similarSeed != null) {
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Similar to",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = similarSeed!!.artist.ifBlank { similarSeed!!.title },
                                color = MuziBlue,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "More",
                            tint = MuziBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(similarSongs.take(10)) { song ->
                            Column(
                                modifier = Modifier
                                    .width(136.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        DesktopAudioPlayer.playSong(song, similarSongs)
                                        onSongClick(song)
                                    }
                            ) {
                                Box {
                                    AsyncImage(
                                        model = song.thumbnailUrl,
                                        contentDescription = song.title,
                                        modifier = Modifier.size(136.dp).clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    IconButton(
                                        onClick = { selectedSongForOptions = song },
                                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(28.dp)
                                    ) {
                                        Icon(Icons.Default.MoreVert, "Options", tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
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

        // ==========================================
        // ADAPTIVE SECTION 5: Forgotten Favorites
        // ==========================================
        if (selectedChip == null && forgottenFavorites.isNotEmpty()) {
            item {
                Column {
                    Text(
                        text = "Forgotten favorites",
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(forgottenFavorites.take(8)) { song ->
                            Column(
                                modifier = Modifier
                                    .width(136.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        DesktopAudioPlayer.playSong(song, forgottenFavorites)
                                        onSongClick(song)
                                    }
                            ) {
                                Box {
                                    AsyncImage(
                                        model = song.thumbnailUrl,
                                        contentDescription = song.title,
                                        modifier = Modifier.size(136.dp).clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    IconButton(
                                        onClick = { selectedSongForOptions = song },
                                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(28.dp)
                                    ) {
                                        Icon(Icons.Default.MoreVert, "Options", tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
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

        // ==========================================
        // Browse Charts Section
        // ==========================================
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
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(charts) { chart ->
                            Column(
                                modifier = Modifier
                                    .width(150.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        coroutineScope.launch {
                                            isLoadingFeed = true
                                            fallbackQuickPicks = YouTubeMusicService.search(chart.title)
                                            homeFeed = null
                                            isLoadingFeed = false
                                        }
                                    }
                            ) {
                                AsyncImage(
                                    model = chart.thumbnailUrl,
                                    contentDescription = chart.title,
                                    modifier = Modifier.size(150.dp).clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
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

        // ==========================================
        // InnerTube Dynamic Sections & Quick Picks
        // ==========================================
        if (isLoadingFeed) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = animatedAccent, strokeWidth = 3.dp)
                }
            }
        } else if (homeFeed != null && homeFeed!!.sections.isNotEmpty()) {
            val sections = homeFeed!!.sections
            sections.forEach { section ->
                val songItems = section.items.filterIsInstance<SongItem>().map { item ->
                    Song(
                        id = item.id,
                        title = item.title,
                        artist = item.artists.joinToString(", ") { it.name },
                        album = item.album?.name ?: "Single",
                        durationText = item.duration?.let { "%d:%02d".format(it / 60, it % 60) } ?: "3:30",
                        durationSeconds = item.duration?.toLong() ?: 210L,
                        thumbnailUrl = item.thumbnail,
                        streamUrl = null
                    )
                }

                if (songItems.isNotEmpty()) {
                    item {
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
                                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
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
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .clickable {
                                                        DesktopAudioPlayer.playSong(song, songItems)
                                                        onSongClick(song)
                                                    }
                                                    .padding(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                AsyncImage(
                                                    model = song.thumbnailUrl,
                                                    contentDescription = song.title,
                                                    modifier = Modifier.size(54.dp).clip(RoundedCornerShape(8.dp)),
                                                    contentScale = ContentScale.Crop
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
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
                                                IconButton(
                                                    onClick = { selectedSongForOptions = song },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.MoreVert, "Options", tint = TextSecondary, modifier = Modifier.size(18.dp))
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
                } else {
                    item {
                        Column {
                            Text(
                                text = section.title,
                                color = TextPrimary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                items(section.items) { item ->
                                    val (title, subtitle, thumb) = when (item) {
                                        is PlaylistItem -> Triple(item.title, item.author?.name ?: "Playlist", item.thumbnail ?: "")
                                        is AlbumItem -> Triple(item.title, item.artists?.joinToString(", ") { it.name } ?: "Album", item.thumbnail ?: "")
                                        else -> Triple("Music", "Collection", "")
                                    }
                                    Column(
                                        modifier = Modifier
                                            .width(150.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                when (item) {
                                                    is PlaylistItem -> onPlaylistClick(item.id, false, title, thumb)
                                                    is AlbumItem -> onPlaylistClick(item.id, true, title, thumb)
                                                    else -> {
                                                        coroutineScope.launch {
                                                            val searchResults = YouTubeMusicService.search(title)
                                                            if (searchResults.isNotEmpty()) {
                                                                DesktopAudioPlayer.playSong(searchResults.first(), searchResults)
                                                                onSongClick(searchResults.first())
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                    ) {
                                        AsyncImage(
                                            model = thumb,
                                            contentDescription = title,
                                            modifier = Modifier.size(150.dp).clip(RoundedCornerShape(12.dp)),
                                            contentScale = ContentScale.Crop
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
        } else if (fallbackQuickPicks.isNotEmpty()) {
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Quick picks",
                            color = TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Button(
                            onClick = {
                                DesktopAudioPlayer.playSong(fallbackQuickPicks.first(), fallbackQuickPicks)
                                onSongClick(fallbackQuickPicks.first())
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF262626)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Play all", color = TextPrimary, fontSize = 13.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    val chunked = fallbackQuickPicks.chunked(4)
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
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                DesktopAudioPlayer.playSong(song, fallbackQuickPicks)
                                                onSongClick(song)
                                            }
                                            .padding(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncImage(
                                            model = song.thumbnailUrl,
                                            contentDescription = song.title,
                                            modifier = Modifier.size(54.dp).clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
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
                                        IconButton(
                                            onClick = { selectedSongForOptions = song },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.MoreVert, "Options", tint = TextSecondary, modifier = Modifier.size(18.dp))
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
    }
}
