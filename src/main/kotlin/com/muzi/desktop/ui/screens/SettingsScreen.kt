package com.muzi.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muzi.desktop.ui.theme.*
import java.awt.Desktop
import java.io.File
import java.net.URI

enum class SettingsSubpage {
    MAIN, APPEARANCE, PLAYER_AUDIO, CONTENT, DISCORD, PRIVACY, STORAGE, BACKUP, LISTEN_TOGETHER, ABOUT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var currentSubpage by remember { mutableStateOf(SettingsSubpage.MAIN) }
    var searchQuery by remember { mutableStateOf("") }

    // 1. Appearance State
    var dynamicTheme by remember { mutableStateOf(true) }
    var amoledDark by remember { mutableStateOf(true) }
    var appleMusicPlayer by remember { mutableStateOf(false) }
    var playerBackgroundStyle by remember { mutableStateOf("Live Mesh Glow") }
    var miniPlayerBackgroundStyle by remember { mutableStateOf("Follow Theme") }
    var playerButtonsStyle by remember { mutableStateOf("Default") }
    var sliderStyle by remember { mutableStateOf("Wavy (Squiggly)") }
    var cropAlbumArt by remember { mutableStateOf(false) }
    var showCodecOnPlayer by remember { mutableStateOf(false) }
    var swipeThumbnail by remember { mutableStateOf(true) }
    var rotatingThumbnail by remember { mutableStateOf(false) }
    var lyricsPosition by remember { mutableStateOf("Center") }
    var lyricsAnimationStyle by remember { mutableStateOf("Apple Music Glow") }
    var lyricsGlowEffect by remember { mutableStateOf(true) }

    // 2. Player & Audio State
    var audioQualityHigh by remember { mutableStateOf(true) }
    var audioQualityMode by remember { mutableStateOf("High (320 kbps Opus)") }
    var autoRadioEnabled by remember { mutableStateOf(true) }
    var gaplessPlayback by remember { mutableStateOf(true) }
    var crossfadeEnabled by remember { mutableStateOf(false) }
    var crossfadeDuration by remember { mutableFloatStateOf(5f) }
    var audioNormalization by remember { mutableStateOf(true) }
    var skipSilence by remember { mutableStateOf(false) }
    var persistentQueue by remember { mutableStateOf(true) }
    var autoSkipOnError by remember { mutableStateOf(true) }

    // 3. Content State
    var contentCountry by remember { mutableStateOf("IN (India)") }
    var contentLanguage by remember { mutableStateOf("English") }
    var hideExplicit by remember { mutableStateOf(false) }
    var hideVideoSongs by remember { mutableStateOf(false) }
    var hideShorts by remember { mutableStateOf(true) }
    var sponsorBlock by remember { mutableStateOf(true) }
    var proxyEnabled by remember { mutableStateOf(false) }
    var proxyUrl by remember { mutableStateOf("") }

    // 4. Discord Rich Presence State
    var discordRpcEnabled by remember { mutableStateOf(true) }
    var discordStatus by remember { mutableStateOf("Listening to Music") }
    var discordLargeImage by remember { mutableStateOf("Thumbnail") }
    var discordShowButtons by remember { mutableStateOf(true) }

    // 5. Privacy State
    var pauseSearchHistory by remember { mutableStateOf(false) }
    var pauseListenHistory by remember { mutableStateOf(false) }

    // 6. Storage State
    val cacheDir = remember { File(System.getProperty("user.home"), ".muzi/cache/audio") }
    var cacheSizeBytes by remember {
        mutableLongStateOf(cacheDir.listFiles()?.sumOf { it.length() } ?: 0L)
    }
    var clearCacheSuccess by remember { mutableStateOf(false) }

    // Dialog State Pickers
    var showBackgroundPicker by remember { mutableStateOf(false) }
    var showSliderPicker by remember { mutableStateOf(false) }
    var showLyricsAnimPicker by remember { mutableStateOf(false) }
    var showQualityPicker by remember { mutableStateOf(false) }
    var showCountryPicker by remember { mutableStateOf(false) }

    if (showBackgroundPicker) {
        AlertDialog(
            onDismissRequest = { showBackgroundPicker = false },
            title = { Text("Player Background Style", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Default (Follow Theme)", "Dynamic Gradient", "Blur Glass", "Glow Animated", "Live Mesh Glow", "Apple Music Canvas").forEach { style ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    playerBackgroundStyle = style
                                    showBackgroundPicker = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = playerBackgroundStyle == style,
                                onClick = {
                                    playerBackgroundStyle = style
                                    showBackgroundPicker = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = MuziBlue)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(style, color = TextPrimary, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showBackgroundPicker = false }) { Text("Close", color = MuziBlue) } },
            containerColor = Color(0xFF1E1E22)
        )
    }

    if (showQualityPicker) {
        AlertDialog(
            onDismissRequest = { showQualityPicker = false },
            title = { Text("Audio Quality Preset", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Low (128 kbps)", "Medium (256 kbps)", "High (320 kbps Opus)", "Lossless / FLAC (44.1 kHz)").forEach { q ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    audioQualityMode = q
                                    showQualityPicker = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = audioQualityMode == q,
                                onClick = {
                                    audioQualityMode = q
                                    showQualityPicker = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = MuziBlue)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(q, color = TextPrimary, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showQualityPicker = false }) { Text("Close", color = MuziBlue) } },
            containerColor = Color(0xFF1E1E22)
        )
    }

    if (showCountryPicker) {
        AlertDialog(
            onDismissRequest = { showCountryPicker = false },
            title = { Text("Content Region & Country", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("IN (India)", "US (United States)", "GB (United Kingdom)", "JP (Japan)", "KR (South Korea)", "BR (Brazil)", "Global / System").forEach { c ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    contentCountry = c
                                    showCountryPicker = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = contentCountry == c,
                                onClick = {
                                    contentCountry = c
                                    showCountryPicker = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = MuziBlue)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(c, color = TextPrimary, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showCountryPicker = false }) { Text("Close", color = MuziBlue) } },
            containerColor = Color(0xFF1E1E22)
        )
    }

    if (showSliderPicker) {
        AlertDialog(
            onDismissRequest = { showSliderPicker = false },
            title = { Text("Player Slider Style", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Default Bar", "Wavy (Squiggly)", "Slim Track", "Waveform").forEach { style ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    sliderStyle = style
                                    showSliderPicker = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = sliderStyle == style,
                                onClick = {
                                    sliderStyle = style
                                    showSliderPicker = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = MuziBlue)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(style, color = TextPrimary, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showSliderPicker = false }) { Text("Close", color = MuziBlue) } },
            containerColor = Color(0xFF1E1E22)
        )
    }

    if (showLyricsAnimPicker) {
        AlertDialog(
            onDismissRequest = { showLyricsAnimPicker = false },
            title = { Text("Lyrics Animation Style", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Fade", "Glow", "Slide Up", "Karaoke Fluid", "Apple Music Glow", "Metro Lyrics").forEach { anim ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    lyricsAnimationStyle = anim
                                    showLyricsAnimPicker = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = lyricsAnimationStyle == anim,
                                onClick = {
                                    lyricsAnimationStyle = anim
                                    showLyricsAnimPicker = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = MuziBlue)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(anim, color = TextPrimary, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showLyricsAnimPicker = false }) { Text("Close", color = MuziBlue) } },
            containerColor = Color(0xFF1E1E22)
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(horizontal = 36.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Header
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = {
                    if (currentSubpage != SettingsSubpage.MAIN) {
                        currentSubpage = SettingsSubpage.MAIN
                    } else {
                        onBackClick()
                    }
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = when (currentSubpage) {
                        SettingsSubpage.MAIN -> "Settings"
                        SettingsSubpage.APPEARANCE -> "Appearance"
                        SettingsSubpage.PLAYER_AUDIO -> "Player & Audio"
                        SettingsSubpage.CONTENT -> "Content & Country"
                        SettingsSubpage.DISCORD -> "Discord Rich Presence"
                        SettingsSubpage.PRIVACY -> "Privacy & History"
                        SettingsSubpage.STORAGE -> "Storage & Cache"
                        SettingsSubpage.BACKUP -> "Backup and Restore"
                        SettingsSubpage.LISTEN_TOGETHER -> "Listen Together"
                        SettingsSubpage.ABOUT -> "About Muzi Music"
                    },
                    color = TextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        when (currentSubpage) {
            SettingsSubpage.MAIN -> {
                // Search Pill
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        placeholder = { Text("Search settings...", color = Color(0xFF888888), fontSize = 15.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, null, tint = Color(0xFF888888), modifier = Modifier.size(20.dp))
                        },
                        shape = RoundedCornerShape(28.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = Color(0xFF16161A),
                            unfocusedContainerColor = Color(0xFF16161A),
                            focusedBorderColor = MuziBlue,
                            unfocusedBorderColor = Color(0xFF2C2C30)
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Main Hub Sections
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF232328))
                    ) {
                        SettingsCategoryItem(
                            icon = Icons.Default.Palette,
                            title = "Appearance",
                            subtitle = "Themes, colors, player UI, and synchronized lyrics",
                            onClick = { currentSubpage = SettingsSubpage.APPEARANCE }
                        )
                        SettingsDivider()

                        SettingsCategoryItem(
                            icon = Icons.Default.PlayArrow,
                            title = "Player and audio",
                            subtitle = "Playback, audio quality, crossfade, and equalizer",
                            onClick = { currentSubpage = SettingsSubpage.PLAYER_AUDIO }
                        )
                        SettingsDivider()

                        SettingsCategoryItem(
                            icon = Icons.Default.Language,
                            title = "Content and country",
                            subtitle = "Region, languages, proxy, and content filters",
                            onClick = { currentSubpage = SettingsSubpage.CONTENT }
                        )
                        SettingsDivider()

                        SettingsCategoryItem(
                            icon = Icons.Default.ChatBubble,
                            title = "Discord Rich Presence",
                            subtitle = "Show current playing track live on your Discord profile",
                            onClick = { currentSubpage = SettingsSubpage.DISCORD }
                        )
                        SettingsDivider()

                        SettingsCategoryItem(
                            icon = Icons.Default.Group,
                            title = "Listen Together",
                            subtitle = "Sync playback with friends in real-time rooms",
                            onClick = { currentSubpage = SettingsSubpage.LISTEN_TOGETHER }
                        )
                        SettingsDivider()

                        SettingsCategoryItem(
                            icon = Icons.Default.Shield,
                            title = "Privacy",
                            subtitle = "Listening history, search logging, and tracking",
                            onClick = { currentSubpage = SettingsSubpage.PRIVACY }
                        )
                        SettingsDivider()

                        SettingsCategoryItem(
                            icon = Icons.Default.Storage,
                            title = "Storage & cache",
                            subtitle = "Cache size, download locations, and offline data",
                            onClick = { currentSubpage = SettingsSubpage.STORAGE }
                        )
                        SettingsDivider()

                        SettingsCategoryItem(
                            icon = Icons.Default.CloudSync,
                            title = "Backup and restore",
                            subtitle = "Export / Import playlists, settings, and favorites JSON",
                            onClick = { currentSubpage = SettingsSubpage.BACKUP }
                        )
                        SettingsDivider()

                        SettingsCategoryItem(
                            icon = Icons.Default.Info,
                            title = "About",
                            subtitle = "Version 1.4.1, GitHub, Telegram, Developer contacts",
                            onClick = { currentSubpage = SettingsSubpage.ABOUT }
                        )
                    }
                }
            }

            SettingsSubpage.APPEARANCE -> {
                item {
                    SettingsSectionHeader(title = "THEME & COLORS")
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF232328))
                    ) {
                        SettingsToggleItem(
                            icon = Icons.Default.ColorLens,
                            title = "Enable dynamic theme",
                            subtitle = "Extract primary accent palette from album art",
                            checked = dynamicTheme,
                            onCheckedChange = { dynamicTheme = it }
                        )
                        SettingsDivider()
                        SettingsToggleItem(
                            icon = Icons.Default.DarkMode,
                            title = "Pure Black AMOLED mode",
                            subtitle = "Use 100% pure black backgrounds for high contrast",
                            checked = amoledDark,
                            onCheckedChange = { amoledDark = it }
                        )
                    }
                }

                item {
                    SettingsSectionHeader(title = "NOW PLAYING PLAYER")
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF232328))
                    ) {
                        SettingsToggleItem(
                            icon = Icons.Default.MusicNote,
                            title = "Apple Music inspired design",
                            subtitle = "Use Apple Music style player layout & controls",
                            checked = appleMusicPlayer,
                            onCheckedChange = { appleMusicPlayer = it }
                        )
                        SettingsDivider()

                        SettingsClickableItem(
                            icon = Icons.Default.Gradient,
                            title = "Player background style",
                            subtitle = playerBackgroundStyle,
                            onClick = { showBackgroundPicker = true }
                        )
                        SettingsDivider()

                        SettingsClickableItem(
                            icon = Icons.Default.LinearScale,
                            title = "Player slider style",
                            subtitle = sliderStyle,
                            onClick = { showSliderPicker = true }
                        )
                        SettingsDivider()

                        SettingsToggleItem(
                            icon = Icons.Default.Crop,
                            title = "Crop album art",
                            subtitle = "Fill container without border gaps",
                            checked = cropAlbumArt,
                            onCheckedChange = { cropAlbumArt = it }
                        )
                        SettingsDivider()

                        SettingsToggleItem(
                            icon = Icons.Default.Info,
                            title = "Show codec on player",
                            subtitle = "Display audio bitrate, codec (Opus/AAC), and kHz",
                            checked = showCodecOnPlayer,
                            onCheckedChange = { showCodecOnPlayer = it }
                        )
                        SettingsDivider()

                        SettingsToggleItem(
                            icon = Icons.Default.Swipe,
                            title = "Enable swipe gesture on thumbnail",
                            subtitle = "Swipe thumbnail left or right to skip tracks",
                            checked = swipeThumbnail,
                            onCheckedChange = { swipeThumbnail = it }
                        )
                        SettingsDivider()

                        SettingsToggleItem(
                            icon = Icons.AutoMirrored.Filled.RotateRight,
                            title = "Rotating thumbnail animation",
                            subtitle = "Continuous smooth rotation when music plays",
                            checked = rotatingThumbnail,
                            onCheckedChange = { rotatingThumbnail = it }
                        )
                    }
                }

                item {
                    SettingsSectionHeader(title = "SYNCHRONIZED LYRICS")
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF232328))
                    ) {
                        SettingsClickableItem(
                            icon = Icons.AutoMirrored.Filled.FormatAlignLeft,
                            title = "Lyrics text position",
                            subtitle = lyricsPosition,
                            onClick = {
                                lyricsPosition = when (lyricsPosition) {
                                    "Left" -> "Center"
                                    "Center" -> "Right"
                                    else -> "Left"
                                }
                            }
                        )
                        SettingsDivider()

                        SettingsClickableItem(
                            icon = Icons.Default.Animation,
                            title = "Lyrics animation style",
                            subtitle = lyricsAnimationStyle,
                            onClick = { showLyricsAnimPicker = true }
                        )
                        SettingsDivider()

                        SettingsToggleItem(
                            icon = Icons.Default.AutoAwesome,
                            title = "Active line glow effect",
                            subtitle = "Dynamic color glow on currently sung lyric line",
                            checked = lyricsGlowEffect,
                            onCheckedChange = { lyricsGlowEffect = it }
                        )
                    }
                }
            }

            SettingsSubpage.PLAYER_AUDIO -> {
                item {
                    SettingsSectionHeader(title = "AUDIO & STREAMING QUALITY")
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF232328))
                    ) {
                        SettingsClickableItem(
                            icon = Icons.Default.HighQuality,
                            title = "Audio Quality",
                            subtitle = audioQualityMode,
                            onClick = { showQualityPicker = true }
                        )
                        SettingsDivider()

                        SettingsToggleItem(
                            icon = Icons.Default.Radio,
                            title = "Endless Auto-Radio",
                            subtitle = "Automatically load similar songs when queue ends",
                            checked = autoRadioEnabled,
                            onCheckedChange = { autoRadioEnabled = it }
                        )
                        SettingsDivider()

                        SettingsToggleItem(
                            icon = Icons.Default.Audiotrack,
                            title = "Gapless Playback",
                            subtitle = "Seamless track transition without silent pauses",
                            checked = gaplessPlayback,
                            onCheckedChange = { gaplessPlayback = it }
                        )
                        SettingsDivider()

                        SettingsToggleItem(
                            icon = Icons.Default.VolumeUp,
                            title = "Volume Normalization (ReplayGain)",
                            subtitle = "Equalize loudness across different songs automatically",
                            checked = audioNormalization,
                            onCheckedChange = { audioNormalization = it }
                        )
                        SettingsDivider()

                        SettingsToggleItem(
                            icon = Icons.AutoMirrored.Filled.VolumeMute,
                            title = "Skip Silence",
                            subtitle = "Automatically skip silent leading & trailing intros",
                            checked = skipSilence,
                            onCheckedChange = { skipSilence = it }
                        )
                        SettingsDivider()

                        SettingsToggleItem(
                            icon = Icons.Default.QueueMusic,
                            title = "Persistent Queue",
                            subtitle = "Save and restore current queue across app restarts",
                            checked = persistentQueue,
                            onCheckedChange = { persistentQueue = it }
                        )
                    }
                }
            }

            SettingsSubpage.CONTENT -> {
                item {
                    SettingsSectionHeader(title = "REGION & LOCALIZATION")
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF232328))
                    ) {
                        SettingsClickableItem(
                            icon = Icons.Default.Public,
                            title = "Content Country & Region",
                            subtitle = contentCountry,
                            onClick = { showCountryPicker = true }
                        )
                        SettingsDivider()

                        SettingsToggleItem(
                            icon = Icons.Default.Explicit,
                            title = "Hide Explicit Songs",
                            subtitle = "Filter out songs marked with 18+ explicit badge",
                            checked = hideExplicit,
                            onCheckedChange = { hideExplicit = it }
                        )
                        SettingsDivider()

                        SettingsToggleItem(
                            icon = Icons.Default.VideocamOff,
                            title = "Hide YouTube Shorts & Videos",
                            subtitle = "Only stream pure audio tracks and official album versions",
                            checked = hideShorts,
                            onCheckedChange = { hideShorts = it }
                        )
                        SettingsDivider()

                        SettingsToggleItem(
                            icon = Icons.Default.Block,
                            title = "SponsorBlock Integration",
                            subtitle = "Skip non-music sponsor intros and outros",
                            checked = sponsorBlock,
                            onCheckedChange = { sponsorBlock = it }
                        )
                    }
                }
            }

            SettingsSubpage.DISCORD -> {
                item {
                    SettingsSectionHeader(title = "DISCORD RICH PRESENCE")
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF232328))
                    ) {
                        SettingsToggleItem(
                            icon = Icons.Default.ChatBubble,
                            title = "Enable Discord RPC",
                            subtitle = "Broadcast currently playing song to your Discord profile",
                            checked = discordRpcEnabled,
                            onCheckedChange = { discordRpcEnabled = it }
                        )
                        SettingsDivider()

                        SettingsToggleItem(
                            icon = Icons.Default.OpenInNew,
                            title = "Show 'Play on Muzi' Buttons",
                            subtitle = "Include interactive song link buttons on Discord",
                            checked = discordShowButtons,
                            onCheckedChange = { discordShowButtons = it }
                        )
                    }
                }
            }

            SettingsSubpage.PRIVACY -> {
                item {
                    SettingsSectionHeader(title = "HISTORY & DATA PRIVACY")
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF232328))
                    ) {
                        SettingsToggleItem(
                            icon = Icons.Default.History,
                            title = "Pause Listening History",
                            subtitle = "Stop recording played songs into recent history",
                            checked = pauseListenHistory,
                            onCheckedChange = { pauseListenHistory = it }
                        )
                        SettingsDivider()

                        SettingsToggleItem(
                            icon = Icons.Default.SearchOff,
                            title = "Pause Search History",
                            subtitle = "Stop saving search queries to suggestion chips",
                            checked = pauseSearchHistory,
                            onCheckedChange = { pauseSearchHistory = it }
                        )
                    }
                }
            }

            SettingsSubpage.STORAGE -> {
                item {
                    val mb = "%.1f MB".format(cacheSizeBytes / (1024.0 * 1024.0))
                    SettingsSectionHeader(title = "LOCAL CACHE & DISK STORAGE")
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF232328))
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Cached Audio Storage: $mb", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text("Offline cached music is stored in your user directory for instant latency-free playback without re-downloading.", color = TextSecondary, fontSize = 13.sp)

                        Button(
                            onClick = {
                                cacheDir.listFiles()?.forEach { it.delete() }
                                cacheSizeBytes = cacheDir.listFiles()?.sumOf { it.length() } ?: 0L
                                clearCacheSuccess = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MuziBlue),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (clearCacheSuccess) "Cache Cleared!" else "Clear Audio Cache", color = Color.White)
                        }
                    }
                }
            }

            SettingsSubpage.BACKUP -> {
                item {
                    var backupSuccess by remember { mutableStateOf(false) }
                    SettingsSectionHeader(title = "DATA BACKUP & RESTORE")
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF232328))
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Export & Import Muzi Data", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text("Backup all your playlists, liked songs, listening stats, and custom equalizer settings into a portable JSON file.", color = TextSecondary, fontSize = 13.sp)

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = { backupSuccess = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MuziBlue),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.FileDownload, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (backupSuccess) "Exported to ~/.muzi/backup.json" else "Export Backup", color = Color.White)
                            }
                        }
                    }
                }
            }

            SettingsSubpage.LISTEN_TOGETHER -> {
                item {
                    SettingsSectionHeader(title = "LISTEN TOGETHER SYNC")
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF232328))
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text("Real-Time Playback Rooms", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text("Host or join rooms with friends to sync playback, queue, and tracks live.", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            }

            SettingsSubpage.ABOUT -> {
                item {
                    SettingsSectionHeader(title = "ABOUT MUZI MUSIC")
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF232328))
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text("Muzi Music PC (v1.4.1)", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Created with Android Muzi UI & Behavior Parity.", color = TextSecondary, fontSize = 13.sp)

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = {
                                    try { Desktop.getDesktop().browse(URI("https://github.com/biikkkuuuu/muzi-music")) } catch (e: Exception) { e.printStackTrace() }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2D35)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("GitHub Repository", color = Color.White)
                            }

                            Button(
                                onClick = {
                                    try { Desktop.getDesktop().browse(URI("https://t.me/biikkkuuuuu")) } catch (e: Exception) { e.printStackTrace() }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MuziBlue),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Telegram (@biikkkuuuuu)", color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Bottom space so floating mini-player doesn't cut off scrolling
        item {
            Spacer(modifier = Modifier.height(130.dp))
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        color = MuziBlue,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(start = 6.dp, top = 6.dp, bottom = 2.dp)
    )
}

@Composable
private fun SettingsCategoryItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MuziBlueContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MuziBlue,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 13.sp
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MuziBlue,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, color = TextSecondary, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = MuziBlue)
        )
    }
}

@Composable
private fun SettingsClickableItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MuziBlue,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, color = MuziBlue, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0x1AFFFFFF))
    )
}
