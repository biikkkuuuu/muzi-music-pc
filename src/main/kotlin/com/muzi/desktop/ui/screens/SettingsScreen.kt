package com.muzi.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val cacheDir = remember { File(System.getProperty("user.home"), ".muzi/cache/audio") }
    var cacheSizeBytes by remember {
        mutableLongStateOf(cacheDir.listFiles()?.sumOf { it.length() } ?: 0L)
    }
    var clearCacheSuccess by remember { mutableStateOf(false) }

    var audioQualityHigh by remember { mutableStateOf(true) }
    var autoRadioEnabled by remember { mutableStateOf(true) }

    // Dialog for Player & Audio
    if (selectedCategory == "Player") {
        AlertDialog(
            onDismissRequest = { selectedCategory = null },
            title = { Text("Player & Audio", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("High Quality Audio", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text("Stream & cache at 320 kbps Opus/AAC", color = TextSecondary, fontSize = 12.sp)
                        }
                        Switch(
                            checked = audioQualityHigh,
                            onCheckedChange = { audioQualityHigh = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = MuziBlue)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Endless Auto-Radio", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text("Auto-enrich queue when songs are ending", color = TextSecondary, fontSize = 12.sp)
                        }
                        Switch(
                            checked = autoRadioEnabled,
                            onCheckedChange = { autoRadioEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = MuziBlue)
                        )
                    }

                    Text("Audio Output: Lavaplayer Native 44.1 kHz PCM", color = MuziBlue, fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedCategory = null }) {
                    Text("Done", color = MuziBlue)
                }
            },
            containerColor = Color(0xFF1E1E22)
        )
    }

    // Dialog for Appearance
    if (selectedCategory == "Appearance") {
        AlertDialog(
            onDismissRequest = { selectedCategory = null },
            title = { Text("Appearance", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Theme Style: AMOLED Pure Black", color = TextSecondary, fontSize = 14.sp)
                    Text("Ambient Mesh Glow: Dynamic Reactive Palette", color = TextSecondary, fontSize = 14.sp)
                    Text("Layout: Android Muzi Material 3 Design System", color = MuziBlue, fontSize = 13.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedCategory = null }) {
                    Text("Done", color = MuziBlue)
                }
            },
            containerColor = Color(0xFF1E1E22)
        )
    }

    // Dialog for Storage / Cache
    if (selectedCategory == "Storage") {
        AlertDialog(
            onDismissRequest = { selectedCategory = null },
            title = { Text("Storage & Cache", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                val mb = "%.1f MB".format(cacheSizeBytes / (1024.0 * 1024.0))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Cached Audio Files: $mb", color = TextSecondary, fontSize = 14.sp)
                    Text("Offline cached files enable instant zero-bandwidth playback.", color = TextSecondary, fontSize = 12.sp)
                    Button(
                        onClick = {
                            cacheDir.listFiles()?.forEach { it.delete() }
                            cacheSizeBytes = cacheDir.listFiles()?.sumOf { it.length() } ?: 0L
                            clearCacheSuccess = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MuziBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (clearCacheSuccess) "Cleared!" else "Clear Cache Now", color = Color.White)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedCategory = null }) {
                    Text("Close", color = MuziBlue)
                }
            },
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
        // Top Bar: Back Arrow & "Settings" (Image 2 Parity)
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Settings",
                    color = TextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Search Bar Pill (Image 2 Parity)
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                placeholder = {
                    Text("Search", color = Color(0xFF888888), fontSize = 15.sp)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color(0xFF888888),
                        modifier = Modifier.size(20.dp)
                    )
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
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Grouped Settings Cards (Image 2 Parity)
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
                    subtitle = "Themes, colors, and UI layout",
                    onClick = { selectedCategory = "Appearance" }
                )
                SettingsDivider()

                SettingsCategoryItem(
                    icon = Icons.Default.PlayArrow,
                    title = "Player and audio",
                    subtitle = "Playback, quality, and equalizer",
                    onClick = { selectedCategory = "Player" }
                )
                SettingsDivider()

                SettingsCategoryItem(
                    icon = Icons.Default.Group,
                    title = "Listen Together",
                    subtitle = "Sync playback with friends",
                    onClick = { selectedCategory = "ListenTogether" }
                )
                SettingsDivider()

                SettingsCategoryItem(
                    icon = Icons.Default.Language,
                    title = "Content",
                    subtitle = "Language, region, and providers",
                    onClick = { selectedCategory = "Content" }
                )
                SettingsDivider()

                SettingsCategoryItem(
                    icon = Icons.Default.Shield,
                    title = "Privacy",
                    subtitle = "History and tracking",
                    onClick = { selectedCategory = "Privacy" }
                )
                SettingsDivider()

                SettingsCategoryItem(
                    icon = Icons.Default.Storage,
                    title = "Storage",
                    subtitle = "Cache and downloads",
                    onClick = { selectedCategory = "Storage" }
                )
                SettingsDivider()

                SettingsCategoryItem(
                    icon = Icons.Default.CloudSync,
                    title = "Backup and restore",
                    subtitle = "Backup playlists and settings",
                    onClick = { selectedCategory = "Backup" }
                )
            }
        }
    }
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
