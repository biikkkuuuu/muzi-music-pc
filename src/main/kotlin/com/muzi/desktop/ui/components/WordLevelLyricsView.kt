package com.muzi.desktop.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muzi.desktop.model.LyricsLine
import kotlinx.coroutines.isActive
import kotlin.math.max

data class WordTimestamp(
    val text: String,
    val startMs: Long,
    val endMs: Long
)

data class ActiveWordSegment(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val progress: Float
)

/**
 * Android Apple Music / Spotify Style Word-by-Word Karaoke Traveling Lyrics View.
 * Renders traveling ambient bloom glow and feathered gradient sweep across each word.
 */
@Composable
fun KaraokeLyricsView(
    lyrics: List<LyricsLine>,
    positionMillis: Long,
    isPlaying: Boolean,
    accentColor: Color,
    listState: LazyListState,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (lyrics.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No synced lyrics found",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
            )
        }
        return
    }

    val activeIndex = remember(positionMillis, lyrics) {
        lyrics.indexOfLast { it.timeMillis <= positionMillis }.coerceAtLeast(0)
    }

    // Auto scroll centering
    LaunchedEffect(activeIndex) {
        if (activeIndex in lyrics.indices) {
            listState.animateScrollToItem(
                index = max(0, activeIndex - 2),
                scrollOffset = -80
            )
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = 40.dp, horizontal = 24.dp)
    ) {
        itemsIndexed(lyrics) { index, line ->
            val isActive = index == activeIndex
            val nextLineTime = lyrics.getOrNull(index + 1)?.timeMillis ?: (line.timeMillis + 4500L)
            val lineDuration = (nextLineTime - line.timeMillis).coerceIn(1500L, 8000L)

            val words = remember(line.text, line.timeMillis, lineDuration) {
                computeWordTimestamps(line.text, line.timeMillis, lineDuration)
            }

            val scale by animateFloatAsState(
                targetValue = if (isActive) 1.10f else 0.94f,
                animationSpec = tween(durationMillis = 350)
            )
            val alpha by animateFloatAsState(
                targetValue = if (isActive) 1.0f else 0.35f,
                animationSpec = tween(durationMillis = 350)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSeekTo(line.timeMillis) }
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = line.text,
                    style = TextStyle(
                        fontSize = if (isActive) 26.sp else 22.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.40f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Word Level Canvas with Traveling Glow Bloom & Smooth Leading Edge Feather.
 */
@Composable
private fun WordLevelCanvasLyrics(
    mainText: String,
    words: List<WordTimestamp>,
    currentPositionMs: Long,
    isPlaying: Boolean,
    accentColor: Color
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    var smoothPosition by remember { mutableLongStateOf(currentPositionMs) }

    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        var lastTime = withFrameMillis { it }
        while (isActive) {
            withFrameMillis { now ->
                val delta = now - lastTime
                lastTime = now
                smoothPosition += delta
            }
        }
    }

    LaunchedEffect(currentPositionMs) {
        smoothPosition = currentPositionMs
    }

    val lyricStyle = TextStyle(
        fontSize = 28.sp,
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.Center,
        color = Color.White
    )

    val glowStyle = lyricStyle.copy(
        shadow = Shadow(
            color = accentColor.copy(alpha = 0.85f),
            blurRadius = 32f,
            offset = Offset.Zero
        )
    )

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val maxWidthPx = constraints.maxWidth

        val layoutResult = remember(mainText, maxWidthPx, lyricStyle) {
            textMeasurer.measure(
                text = mainText,
                style = lyricStyle,
                constraints = Constraints(minWidth = maxWidthPx, maxWidth = maxWidthPx),
                softWrap = true
            )
        }

        val glowLayoutResult = remember(mainText, maxWidthPx, glowStyle) {
            textMeasurer.measure(
                text = mainText,
                style = glowStyle,
                constraints = Constraints(minWidth = maxWidthPx, maxWidth = maxWidthPx),
                softWrap = true
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { layoutResult.size.height.toDp() })
                .graphicsLayer(clip = false)
        ) {
            if (mainText.isEmpty()) return@Canvas

            // 1. Draw inactive background dim text
            drawText(layoutResult, color = Color.White.copy(alpha = 0.25f))

            val completedPath = Path()
            var hasCompleted = false
            val activeSegments = mutableListOf<ActiveWordSegment>()

            val wordIdxMap = IntArray(mainText.length) { -1 }
            var searchOffset = 0
            words.forEachIndexed { idx, w ->
                val pos = mainText.indexOf(w.text, searchOffset)
                if (pos != -1) {
                    for (i in pos until (pos + w.text.length)) {
                        wordIdxMap[i] = idx
                    }
                    searchOffset = pos + w.text.length
                }
            }

            for (idx in words.indices) {
                val word = words[idx]
                if (smoothPosition < word.startMs) continue

                val progress = if (smoothPosition >= word.endMs) 1f
                else ((smoothPosition - word.startMs).toFloat() / (word.endMs - word.startMs).coerceAtLeast(1L)).coerceIn(0f, 1f)

                var firstChar = -1
                var lastChar = -1
                for (i in wordIdxMap.indices) {
                    if (wordIdxMap[i] == idx) {
                        if (firstChar == -1) firstChar = i
                        lastChar = i
                    }
                }

                if (firstChar != -1 && lastChar != -1) {
                    var lineStartLeft = Float.MAX_VALUE
                    var lineStartTop = Float.MAX_VALUE
                    var lineEndRight = Float.MIN_VALUE
                    var lineEndBottom = Float.MIN_VALUE

                    for (i in firstChar..lastChar) {
                        val bounds = layoutResult.getBoundingBox(i)
                        if (bounds.left < lineStartLeft) lineStartLeft = bounds.left
                        if (bounds.top < lineStartTop) lineStartTop = bounds.top
                        if (bounds.right > lineEndRight) lineEndRight = bounds.right
                        if (bounds.bottom > lineEndBottom) lineEndBottom = bounds.bottom
                    }

                    if (progress >= 1f) {
                        completedPath.addRect(Rect(lineStartLeft, lineStartTop, lineEndRight, lineEndBottom))
                        hasCompleted = true
                    } else if (progress > 0f) {
                        activeSegments.add(
                            ActiveWordSegment(lineStartLeft, lineStartTop, lineEndRight, lineEndBottom, progress)
                        )
                    }
                }
            }

            // 2. Draw fully completed words in solid white + glow
            if (hasCompleted) {
                clipPath(completedPath) {
                    drawText(glowLayoutResult, color = Color.White)
                    drawText(layoutResult, color = Color.White)
                }
            }

            // 3. Draw active word with real-time traveling bloom glow & leading edge gradient
            activeSegments.forEach { seg ->
                val segWidth = (seg.right - seg.left).coerceAtLeast(1f)
                val fillWidth = segWidth * seg.progress
                val headX = seg.left + fillWidth
                val centerY = (seg.top + seg.bottom) / 2f
                val glowRadius = (seg.bottom - seg.top) * 1.8f

                // Ambient traveling radial bloom (Apple Music Karaoke glow)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.50f),
                            accentColor.copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        center = Offset(headX, centerY),
                        radius = glowRadius
                    ),
                    radius = glowRadius,
                    center = Offset(headX, centerY)
                )

                // Glowing shadow text for the singing portion
                clipRect(
                    left = seg.left - 4f,
                    top = seg.top - 4f,
                    right = headX + 4f,
                    bottom = seg.bottom + 4f
                ) {
                    drawText(glowLayoutResult, color = Color.White)
                }

                // Feathered traveling gradient brush
                val featherPx = 16f
                val stop0 = ((headX - featherPx - seg.left) / segWidth).coerceIn(0f, 1f)
                val stop1 = ((headX + 2f - seg.left) / segWidth).coerceIn(0f, 1f)

                val brush = Brush.horizontalGradient(
                    colorStops = arrayOf(
                        0f to Color.White,
                        stop0 to Color.White,
                        stop1 to Color.White.copy(alpha = 0.25f),
                        1f to Color.White.copy(alpha = 0.25f)
                    ),
                    startX = seg.left,
                    endX = seg.right
                )

                clipRect(left = seg.left, top = seg.top, right = seg.right, bottom = seg.bottom) {
                    drawText(layoutResult, brush = brush)
                }
            }
        }
    }
}

/**
 * Splits line into timed word segments with syllable-weighted durations.
 */
private fun computeWordTimestamps(lineText: String, lineStartMs: Long, lineDurationMs: Long): List<WordTimestamp> {
    val words = lineText.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (words.isEmpty()) return emptyList()

    val totalChars = words.sumOf { it.length }.coerceAtLeast(1)
    val result = mutableListOf<WordTimestamp>()
    var currentStart = lineStartMs

    words.forEach { word ->
        val wordDuration = ((word.length.toFloat() / totalChars) * lineDurationMs).toLong().coerceAtLeast(180L)
        val wordEnd = currentStart + wordDuration
        result.add(WordTimestamp(word, currentStart, wordEnd))
        currentStart = wordEnd
    }

    return result
}
