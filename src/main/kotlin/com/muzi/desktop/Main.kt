package com.muzi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.muzi.desktop.audio.DesktopAudioPlayer
import com.muzi.desktop.ui.components.MiniPlayerBar
import com.muzi.desktop.ui.components.MuziNavigationRail
import com.muzi.desktop.ui.components.ScreenTab
import com.muzi.desktop.ui.screens.HomeScreen
import com.muzi.desktop.ui.screens.LibraryScreen
import com.muzi.desktop.ui.screens.PlayerScreen
import com.muzi.desktop.ui.screens.SearchScreen
import com.muzi.desktop.ui.theme.MuziTheme
import com.muzi.desktop.ui.theme.PureBlack

fun main() = application {
    val windowState = rememberWindowState(width = 1200.dp, height = 800.dp)

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "Muzi"
    ) {
        MuziTheme {
            var currentTab by remember { mutableStateOf(ScreenTab.HOME) }
            val currentSong by DesktopAudioPlayer.currentSong.collectAsState()
            val isPlaying by DesktopAudioPlayer.isPlaying.collectAsState()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PureBlack)
            ) {
                if (currentTab == ScreenTab.PLAYER) {
                    PlayerScreen(
                        onBackClick = { currentTab = ScreenTab.HOME },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Left Navigation Rail (Desktop-1,3,4)
                        MuziNavigationRail(
                            currentTab = currentTab,
                            onTabSelected = { currentTab = it }
                        )

                        // Main Content Area
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            when (currentTab) {
                                ScreenTab.HOME -> HomeScreen(
                                    onSongClick = { song ->
                                        DesktopAudioPlayer.playSong(song)
                                        currentTab = ScreenTab.PLAYER
                                    }
                                )
                                ScreenTab.SEARCH -> SearchScreen(
                                    onSongClick = { song ->
                                        DesktopAudioPlayer.playSong(song)
                                        currentTab = ScreenTab.PLAYER
                                    }
                                )
                                ScreenTab.LIBRARY -> LibraryScreen(
                                    onSongClick = { song ->
                                        DesktopAudioPlayer.playSong(song)
                                        currentTab = ScreenTab.PLAYER
                                    }
                                )
                                ScreenTab.SETTINGS -> HomeScreen(
                                    onSongClick = { song ->
                                        DesktopAudioPlayer.playSong(song)
                                        currentTab = ScreenTab.PLAYER
                                    }
                                )
                                else -> {}
                            }

                            // Floating Mini Player (At Bottom of main area)
                            MiniPlayerBar(
                                song = currentSong,
                                isPlaying = isPlaying,
                                onTogglePlayPause = { DesktopAudioPlayer.togglePlayPause() },
                                onClick = { currentTab = ScreenTab.PLAYER },
                                modifier = Modifier.align(Alignment.BottomCenter)
                            )
                        }
                    }
                }
            }
        }
    }
}
