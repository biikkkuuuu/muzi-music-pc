package com.muzi.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.muzi.desktop.ui.theme.MuziBlueContainer
import com.muzi.desktop.ui.theme.TextPrimary
import com.muzi.desktop.ui.theme.TextSecondary

enum class ScreenTab {
    HOME, SEARCH, LIBRARY, SETTINGS, PLAYER, PLAYLIST_DETAIL, ARTIST_DETAIL, HISTORY, STATS
}

@Composable
fun MuziNavigationRail(
    currentTab: ScreenTab,
    onTabSelected: (ScreenTab) -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(36.dp))
                .background(Color(0xFF222328))
                .padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Home Tab
            NavPillButton(
                icon = Icons.Default.Home,
                isSelected = currentTab == ScreenTab.HOME,
                onClick = { onTabSelected(ScreenTab.HOME) }
            )

            // Search Tab
            NavPillButton(
                icon = Icons.Default.Search,
                isSelected = currentTab == ScreenTab.SEARCH,
                onClick = { onTabSelected(ScreenTab.SEARCH) }
            )

            // Mic / Voice Action (Opens Search)
            NavPillButton(
                icon = Icons.Default.Mic,
                isSelected = false,
                onClick = { onTabSelected(ScreenTab.SEARCH) }
            )

            // Library Tab
            NavPillButton(
                icon = Icons.Default.LibraryMusic,
                isSelected = currentTab == ScreenTab.LIBRARY,
                onClick = { onTabSelected(ScreenTab.LIBRARY) }
            )

            // More Options (...) Button
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MuziBlueContainer)
                    .clickable(onClick = onMoreClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MoreHoriz,
                    contentDescription = "More",
                    tint = TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun NavPillButton(
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(if (isSelected) MuziBlueContainer else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) TextPrimary else TextSecondary,
            modifier = Modifier.size(24.dp)
        )
    }
}
