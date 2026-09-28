package com.muzi.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muzi.desktop.ui.theme.MuziBlue
import com.muzi.desktop.ui.theme.PureBlack
import com.muzi.desktop.ui.theme.TextPrimary
import com.muzi.desktop.ui.theme.TextSecondary

data class EQProfile(
    val id: String,
    val name: String,
    val bands: List<Float>,
    val isCustom: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqScreen(
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var enabled by remember { mutableStateOf(false) }
    var bassBoost by remember { mutableFloatStateOf(0f) }
    var virtualizer by remember { mutableFloatStateOf(0f) }

    val defaultProfiles = remember {
        listOf(
            EQProfile("flat", "Flat", listOf(0f, 0f, 0f, 0f, 0f)),
            EQProfile("bass_boost", "Bass Booster", listOf(5f, 3f, 0f, -1f, -2f)),
            EQProfile("rock", "Rock", listOf(4f, 2f, -1f, 2f, 4f)),
            EQProfile("pop", "Pop", listOf(-1f, 2f, 4f, 2f, -1f)),
            EQProfile("vocal", "Vocal Booster", listOf(-2f, -1f, 3f, 4f, 2f)),
            EQProfile("classical", "Classical", listOf(4f, 3f, -1f, 2f, 3f)),
            EQProfile("jazz", "Jazz", listOf(3f, 2f, -2f, 2f, 3f)),
            EQProfile("electronic", "Electronic", listOf(4f, 2f, 0f, 2f, 3f))
        )
    }

    var selectedProfileId by remember { mutableStateOf("flat") }
    var bandValues by remember {
        mutableStateOf(listOf(0f, 0f, 0f, 0f, 0f))
    }

    val bandFrequencies = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(horizontal = 36.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Top Header
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
                    text = "Equalizer",
                    color = TextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = enabled,
                    onCheckedChange = { enabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MuziBlue
                    )
                )
            }
        }

        // 5-Band Slider Graphic EQ
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Text(
                        text = "Graphic Equalizer (5-Band)",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        bandFrequencies.forEachIndexed { index, freq ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxHeight().weight(1f)
                            ) {
                                Text(
                                    text = "${bandValues.getOrElse(index) { 0f }.toInt()} dB",
                                    color = if (enabled) MuziBlue else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Slider(
                                    value = bandValues.getOrElse(index) { 0f },
                                    onValueChange = { newVal ->
                                        if (enabled) {
                                            val list = bandValues.toMutableList()
                                            list[index] = newVal
                                            bandValues = list
                                            selectedProfileId = "custom"
                                        }
                                    },
                                    valueRange = -10f..10f,
                                    enabled = enabled,
                                    modifier = Modifier.height(100.dp),
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.White,
                                        activeTrackColor = MuziBlue,
                                        inactiveTrackColor = Color(0x33FFFFFF)
                                    )
                                )
                                Text(
                                    text = freq,
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bass Boost & Virtualizer Sliders
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Audio Enhancements",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Bass Boost
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Bass Boost", color = TextPrimary, fontSize = 14.sp)
                            Text("${(bassBoost * 100).toInt()}%", color = MuziBlue, fontSize = 14.sp)
                        }
                        Slider(
                            value = bassBoost,
                            onValueChange = { if (enabled) bassBoost = it },
                            enabled = enabled,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = MuziBlue,
                                inactiveTrackColor = Color(0x33FFFFFF)
                            )
                        )
                    }

                    // Virtualizer 3D Surround
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("3D Surround Virtualizer", color = TextPrimary, fontSize = 14.sp)
                            Text("${(virtualizer * 100).toInt()}%", color = MuziBlue, fontSize = 14.sp)
                        }
                        Slider(
                            value = virtualizer,
                            onValueChange = { if (enabled) virtualizer = it },
                            enabled = enabled,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = MuziBlue,
                                inactiveTrackColor = Color(0x33FFFFFF)
                            )
                        )
                    }
                }
            }
        }

        // Preset Profiles List
        item {
            Text(
                text = "Preset Profiles",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        items(defaultProfiles) { profile ->
            val isSelected = selectedProfileId == profile.id
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSelected) MuziBlue.copy(alpha = 0.15f) else Color(0xFF1E1E24))
                    .clickable {
                        selectedProfileId = profile.id
                        bandValues = profile.bands
                    }
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = isSelected,
                        onClick = {
                            selectedProfileId = profile.id
                            bandValues = profile.bands
                        },
                        colors = RadioButtonDefaults.colors(selectedColor = MuziBlue)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = profile.name,
                        color = if (isSelected) MuziBlue else TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
                Text(
                    text = "${profile.bands.size} Bands",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}
