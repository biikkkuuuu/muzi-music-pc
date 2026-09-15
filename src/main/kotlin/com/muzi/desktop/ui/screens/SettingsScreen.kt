package com.muzi.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muzi.desktop.ui.theme.*
import java.io.File

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    val cacheDir = remember { File(System.getProperty("user.home"), ".muzi/cache/audio") }
    var cacheSizeBytes by remember {
        mutableLongStateOf(
            cacheDir.listFiles()?.sumOf { it.length() } ?: 0L
        )
    }
    var selectedQuality by remember { mutableStateOf("High (128-256 kbps AAC)") }
    var selectedPreset by remember { mutableStateOf("Dynamic (Album Art)") }
    var clearCacheSuccess by remember { mutableStateOf(false) }

    val qualities = listOf("Standard (128 kbps)", "High (128-256 kbps AAC)", "Ultra (Lossless Op)")
    val presets = listOf("Dynamic (Album Art)", "Pure AMOLED Black", "Deep Obsidian")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(horizontal = 48.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        item {
            Text(
                text = "Settings",
                color = TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Section: Storage & Cache
        item {
            SettingsCard(title = "Storage & Offline Cache", icon = Icons.Default.Storage) {
                val mb = "%.1f MB".format(cacheSizeBytes / (1024.0 * 1024.0))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Cached Audio Files", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Songs stored locally for instant offline playback ($mb)", color = TextSecondary, fontSize = 13.sp)
                    }
                    Button(
                        onClick = {
                            cacheDir.listFiles()?.forEach { it.delete() }
                            cacheSizeBytes = cacheDir.listFiles()?.sumOf { it.length() } ?: 0L
                            clearCacheSuccess = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (clearCacheSuccess) "Cleared!" else "Clear Cache", color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }

        // Section: Audio Quality
        item {
            SettingsCard(title = "Audio Quality & Streaming", icon = Icons.Default.Equalizer) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Streaming Audio Format", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("Higher bitrate ensures crystal-clear highs and deep bass", color = TextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        qualities.forEach { q ->
                            val isSelected = q == selectedQuality
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSelected) Color(0xFFE50914) else Color(0xFF222222))
                                    .clickable { selectedQuality = q }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = q,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Appearance & Theme
        item {
            SettingsCard(title = "Appearance & Aesthetics", icon = Icons.Default.Palette) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Theme Style", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("Song-adaptive ambient gradient vs Pure AMOLED black", color = TextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        presets.forEach { p ->
                            val isSelected = p == selectedPreset
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSelected) Color(0xFFE50914) else Color(0xFF222222))
                                    .clickable { selectedPreset = p }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = p,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Keyboard Shortcuts Cheat Sheet
        item {
            SettingsCard(title = "Desktop Keyboard Shortcuts", icon = Icons.Default.Keyboard) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ShortcutRow("Spacebar", "Play / Pause playback")
                    ShortcutRow("Left / Right Arrow", "Seek 5 seconds backward / forward")
                    ShortcutRow("Ctrl + Left / Right", "Play previous / next song")
                    ShortcutRow("Up / Down Arrow", "Volume Up / Down (5% step)")
                    ShortcutRow("L", "Like (??) current song into Favourites")
                    ShortcutRow("M", "Mute / Unmute audio")
                }
            }
        }

        // Section: About
        item {
            SettingsCard(title = "About Muzi Desktop", icon = Icons.Default.Info) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Muzi Music for Windows (PC)", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Version 1.2.0 • Standalone Desktop Edition", color = TextSecondary, fontSize = 13.sp)
                    Text("Engine: Native YouTube Music / InnerTube Stream Pipeline", color = TextSecondary, fontSize = 13.sp)
                    Text("Created for Vikash Rana • GitHub: biikkkuuuu/muzi-music-pc", color = TextSecondary, fontSize = 13.sp)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF141414))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = title, tint = Color(0xFFE50914), modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = title, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
        content()
    }
}

@Composable
private fun ShortcutRow(key: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF262626))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(text = key, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(text = description, color = TextSecondary, fontSize = 13.sp)
    }
}
