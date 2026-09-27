package com.muzi.desktop

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
                    // Escape collapses player
                    keyEvent.key == Key.Escape && currentTab == ScreenTab.PLAYER -> {
                        currentTab = previousTab
                        true
                    }
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
                    // Volume up: Up arrow
                    keyEvent.key == Key.DirectionUp && currentTab == ScreenTab.PLAYER -> {
                        DesktopAudioPlayer.adjustVolume(0.05f)
                        true
                    }
                    // Volume down: Down arrow
                    keyEvent.key == Key.DirectionDown && currentTab == ScreenTab.PLAYER -> {
                        DesktopAudioPlayer.adjustVolume(-0.05f)
                        true
                    }
                    // Toggle Mute: M key
                    keyEvent.key == Key.M && currentTab != ScreenTab.SEARCH -> {
                        DesktopAudioPlayer.toggleMute()
                        true
                    }
                    // Toggle Like: L key
                    keyEvent.key == Key.L && currentTab != ScreenTab.SEARCH -> {
                        DesktopAudioPlayer.toggleLikeCurrentSong()
                        true
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
                // Base Content Layout (Always active beneath the player)
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left Navigation Rail (Muzi Android style side-nav)
                    MuziNavigationRail(
                        currentTab = if (currentTab == ScreenTab.PLAYER) previousTab else currentTab,
                        onTabSelected = { 
                            previousTab = currentTab
                            currentTab = it 
                        }
                    )

                    // Main Screen Area with smooth animated transitions
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        var showQuickMenu by remember { mutableStateOf(false) }

                        if (showQuickMenu) {
                            com.muzi.desktop.ui.components.QuickMenuDialog(
                                onDismiss = { showQuickMenu = false },
                                onSettingsClick = {
                                    previousTab = currentTab
                                    currentTab = ScreenTab.SETTINGS
                                }
                            )
                        }

                        AnimatedContent(
                            targetState = if (currentTab == ScreenTab.PLAYER) previousTab else currentTab,
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.98f))
                                    .togetherWith(fadeOut(animationSpec = tween(160)))
                            },
                            label = "MainContentTransition"
                        ) { tab ->
                            when (tab) {
                                ScreenTab.HOME -> HomeScreen(
                                    onSongClick = { _ ->
                                        previousTab = ScreenTab.HOME
                                        currentTab = ScreenTab.PLAYER
                                    },
                                    onSettingsClick = {
                                        previousTab = currentTab
                                        currentTab = ScreenTab.SETTINGS
                                    }
                                )
                                ScreenTab.SEARCH -> SearchScreen(
                                    onSongClick = { _ ->
                                        previousTab = ScreenTab.SEARCH
                                        currentTab = ScreenTab.PLAYER
                                    }
                                )
                                ScreenTab.LIBRARY -> LibraryScreen(
                                    onSongClick = { _ ->
                                        previousTab = ScreenTab.LIBRARY
                                        currentTab = ScreenTab.PLAYER
                                    },
                                    onSettingsClick = {
                                        previousTab = currentTab
                                        currentTab = ScreenTab.SETTINGS
                                    }
                                )
                                ScreenTab.SETTINGS -> SettingsScreen(
                                    onBackClick = {
                                        currentTab = previousTab
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                                else -> {}
                            }
                        }

                        // Floating Mini Player & Floating Bottom Navigation Bar (Image 1 & 4 Screenshot Parity)
                        if (currentTab != ScreenTab.PLAYER) {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (currentSong != null) {
                                    MiniPlayerBar(
                                        song = currentSong,
                                        isPlaying = isPlaying,
                                        onTogglePlayPause = { DesktopAudioPlayer.togglePlayPause() },
                                        onClick = { 
                                            previousTab = currentTab
                                            currentTab = ScreenTab.PLAYER 
                                        }
                                    )
                                }

                                com.muzi.desktop.ui.components.FloatingBottomNavBar(
                                    currentTab = if (currentTab == ScreenTab.PLAYER) previousTab else currentTab,
                                    onTabSelected = {
                                        previousTab = currentTab
                                        currentTab = it
                                    },
                                    onMoreClick = {
                                        showQuickMenu = true
                                    }
                                )
                            }
                        }
                    }
                }

                // Smooth Sliding Fullscreen Player Sheet (Exact Muzi Android Expansion Transition)
                AnimatedVisibility(
                    visible = currentTab == ScreenTab.PLAYER,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(250)),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(180)),
                    modifier = Modifier.fillMaxSize()
                ) {
                    PlayerScreen(
                        onBackClick = { currentTab = previousTab },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
