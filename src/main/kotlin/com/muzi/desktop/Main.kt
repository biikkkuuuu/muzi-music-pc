package com.muzi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
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
import com.muzi.desktop.ui.screens.SettingsScreen
import com.muzi.desktop.ui.theme.MuziTheme
import com.muzi.desktop.ui.theme.PureBlack

fun main() = application {
    val windowState = rememberWindowState(width = 1200.dp, height = 800.dp)

    var currentTab by remember { mutableStateOf(ScreenTab.HOME) }
    var previousTab by remember { mutableStateOf(ScreenTab.HOME) }

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "Muzi Music",
        onKeyEvent = { keyEvent ->
            if (keyEvent.type == KeyEventType.KeyDown) {
                when {
                    // Global play/pause toggle with Space (when not on Search screen typing)
                    keyEvent.key == Key.Spacebar && currentTab != ScreenTab.SEARCH -> {
                        DesktopAudioPlayer.togglePlayPause()
                        true
                    }
                    // Next song: Ctrl + Right Arrow
                    keyEvent.isCtrlPressed && keyEvent.key == Key.DirectionRight -> {
                        DesktopAudioPlayer.playNext()
                        true
                    }
                    // Previous song: Ctrl + Left Arrow
                    keyEvent.isCtrlPressed && keyEvent.key == Key.DirectionLeft -> {
                        DesktopAudioPlayer.playPrevious()
                        true
                    }
                    // Seek forward 5s: Right arrow (when not in search box)
                    !keyEvent.isCtrlPressed && keyEvent.key == Key.DirectionRight && currentTab != ScreenTab.SEARCH -> {
                        DesktopAudioPlayer.seekRelative(5000L)
                        true
                    }
                    // Seek backward 5s: Left arrow (when not in search box)
                    !keyEvent.isCtrlPressed && keyEvent.key == Key.DirectionLeft && currentTab != ScreenTab.SEARCH -> {
                        DesktopAudioPlayer.seekRelative(-5000L)
                        true
                    }
                    // Volume Up: Up arrow (when not in search)
                    keyEvent.key == Key.DirectionUp && currentTab != ScreenTab.SEARCH -> {
                        DesktopAudioPlayer.adjustVolume(0.05f)
                        true
                    }
                    // Volume Down: Down arrow (when not in search)
                    keyEvent.key == Key.DirectionDown && currentTab != ScreenTab.SEARCH -> {
                        DesktopAudioPlayer.adjustVolume(-0.05f)
                        true
                    }
                    // Mute / Unmute: M key
                    keyEvent.key == Key.M && currentTab != ScreenTab.SEARCH -> {
                        DesktopAudioPlayer.toggleMute()
                        true
                    }
                    // Like / Unlike: L key
                    keyEvent.key == Key.L && currentTab != ScreenTab.SEARCH -> {
                        DesktopAudioPlayer.toggleLikeCurrentSong()
                        true
                    }
                    // Escape: Close full Player screen back to main
                    keyEvent.key == Key.Escape -> {
                        if (currentTab == ScreenTab.PLAYER) {
                            currentTab = previousTab
                            true
                        } else false
                    }
                    else -> false
                }
            } else {
                false
            }
        }
    ) {
        MuziTheme {
            val currentSong by DesktopAudioPlayer.currentSong.collectAsState()
            val isPlaying by DesktopAudioPlayer.isPlaying.collectAsState()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PureBlack)
            ) {
                if (currentTab == ScreenTab.PLAYER) {
                    PlayerScreen(
                        onBackClick = { currentTab = previousTab },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Left Navigation Rail (Desktop-1,3,4)
                        MuziNavigationRail(
                            currentTab = currentTab,
                            onTabSelected = { 
                                previousTab = currentTab
                                currentTab = it 
                            }
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
                                        previousTab = ScreenTab.HOME
                                        currentTab = ScreenTab.PLAYER
                                    }
                                )
                                ScreenTab.SEARCH -> SearchScreen(
                                    onSongClick = { song ->
                                        DesktopAudioPlayer.playSong(song)
                                        previousTab = ScreenTab.SEARCH
                                        currentTab = ScreenTab.PLAYER
                                    }
                                )
                                ScreenTab.LIBRARY -> LibraryScreen(
                                    onSongClick = { song ->
                                        DesktopAudioPlayer.playSong(song)
                                        previousTab = ScreenTab.LIBRARY
                                        currentTab = ScreenTab.PLAYER
                                    }
                                )
                                ScreenTab.SETTINGS -> SettingsScreen(
                                    modifier = Modifier.fillMaxSize()
                                )
                                else -> {}
                            }

                            // Floating Mini Player (At Bottom of main area)
                            MiniPlayerBar(
                                song = currentSong,
                                isPlaying = isPlaying,
                                onTogglePlayPause = { DesktopAudioPlayer.togglePlayPause() },
                                onClick = { 
                                    previousTab = currentTab
                                    currentTab = ScreenTab.PLAYER 
                                },
                                modifier = Modifier.align(Alignment.BottomCenter)
                            )
                        }
                    }
                }
            }
        }
    }
}
