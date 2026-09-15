package com.muzi.desktop.model

import kotlinx.serialization.Serializable

@Serializable
data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val durationText: String = "",
    val durationSeconds: Long = 0,
    val thumbnailUrl: String = "",
    val streamUrl: String? = null
)

@Serializable
data class ChartItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String
)

@Serializable
data class LyricsLine(
    val timeMillis: Long,
    val text: String
)
