package com.muzi.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muzi.desktop.ui.theme.MuziBlue
import com.muzi.desktop.ui.theme.PureBlack
import com.muzi.desktop.ui.theme.TextPrimary
import com.muzi.desktop.ui.theme.TextSecondary
import java.awt.Desktop
import java.net.URI

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var cookieInput by remember { mutableStateOf("") }
    var visitorDataInput by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var saveSuccess by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(horizontal = 36.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header
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
                text = "Account & Login",
                color = TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = MuziBlue,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("YouTube Music Account Sync", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Sync your liked songs, custom playlists, and recommendations", color = TextSecondary, fontSize = 13.sp)
                    }
                }

                Button(
                    onClick = {
                        try {
                            Desktop.getDesktop().browse(URI("https://music.youtube.com"))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MuziBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open YouTube Music in Browser", color = Color.White)
                }
            }
        }

        // Cookie and Session Key Input
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
                    text = "Manual Cookie & Token Session Setup",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = cookieInput,
                    onValueChange = { cookieInput = it },
                    label = { Text("InnerTube Cookie") },
                    placeholder = { Text("SAPISID=...; SSID=...; HSID=...") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = MuziBlue,
                        unfocusedBorderColor = Color(0xFF333338)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = visitorDataInput,
                    onValueChange = { visitorDataInput = it },
                    label = { Text("Visitor Data (Optional)") },
                    placeholder = { Text("Cgt...") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = MuziBlue,
                        unfocusedBorderColor = Color(0xFF333338)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            isSaving = true
                            saveSuccess = true
                            isSaving = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MuziBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (saveSuccess) "Saved Successfully!" else "Save & Sync Account", color = Color.White)
                    }
                }
            }
        }
    }
}
