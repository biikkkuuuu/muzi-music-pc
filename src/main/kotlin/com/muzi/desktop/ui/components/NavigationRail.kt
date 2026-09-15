package com.muzi.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.muzi.desktop.ui.theme.PureBlack

enum class ScreenTab {
    HOME, SEARCH, LIBRARY, SETTINGS, PLAYER
}

@Composable
fun MuziNavigationRail(
    currentTab: ScreenTab,
    onTabSelected: (ScreenTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(72.dp)
            .fillMaxHeight()
            .background(PureBlack)
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically)
    ) {
        NavIconItem(
            icon = Icons.Default.Home,
            contentDescription = "Home",
            isSelected = currentTab == ScreenTab.HOME,
            onClick = { onTabSelected(ScreenTab.HOME) }
        )
        NavIconItem(
            icon = Icons.Default.Search,
            contentDescription = "Search",
            isSelected = currentTab == ScreenTab.SEARCH,
            onClick = { onTabSelected(ScreenTab.SEARCH) }
        )
        NavIconItem(
            icon = Icons.Default.QueueMusic,
            contentDescription = "Library",
            isSelected = currentTab == ScreenTab.LIBRARY,
            onClick = { onTabSelected(ScreenTab.LIBRARY) }
        )
        NavIconItem(
            icon = Icons.Default.Settings,
            contentDescription = "Settings",
            isSelected = currentTab == ScreenTab.SETTINGS,
            onClick = { onTabSelected(ScreenTab.SETTINGS) }
        )
    }
}

@Composable
private fun NavIconItem(
    icon: ImageVector,
    contentDescription: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (isSelected) Color.White else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isSelected) Color.Black else Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}
