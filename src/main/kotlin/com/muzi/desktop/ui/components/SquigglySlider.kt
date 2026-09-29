package com.muzi.desktop.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * 100% Android Muzi / Material 3 Straight Line Seekbar with Rounded Pill Thumb.
 * Ultra smooth drag, tap-to-seek, and crystal clean rendering (Desktop-2.png parity).
 */
@Composable
fun SquigglySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(),
    isPlaying: Boolean = true,
) {
    val primaryColor = colors.activeTrackColor
    val inactiveColor = colors.inactiveTrackColor

    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableFloatStateOf(value) }

    val currentValue = if (isDragging) dragPosition else value
    val duration = valueRange.endInclusive - valueRange.start
    val position = currentValue - valueRange.start

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .then(
                if (enabled) {
                    Modifier
                        .pointerInput(valueRange) {
                            detectTapGestures { offset ->
                                val newPosition = (offset.x / size.width) * duration
                                val mappedValue = valueRange.start + newPosition.coerceIn(0f, duration)
                                onValueChange(mappedValue)
                                onValueChangeFinished?.invoke()
                            }
                        }
                        .pointerInput(valueRange) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    isDragging = true
                                    val newPosition = (offset.x / size.width) * duration
                                    dragPosition = valueRange.start + newPosition.coerceIn(0f, duration)
                                    onValueChange(dragPosition)
                                },
                                onDragEnd = {
                                    isDragging = false
                                    onValueChangeFinished?.invoke()
                                },
                                onDragCancel = {
                                    isDragging = false
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val newPosition = (change.position.x / size.width) * duration
                                    dragPosition = valueRange.start + newPosition.coerceIn(0f, duration)
                                    onValueChange(dragPosition)
                                }
                            )
                        }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
        ) {
            val strokeWidth = 3.5.dp.toPx()
            val progress = if (duration > 0f) (position / duration).coerceIn(0f, 1f) else 0f
            val totalWidth = size.width
            val totalProgressPx = (totalWidth * progress).coerceIn(0f, totalWidth)
            val centerY = size.height / 2f

            val disabledAlpha = 0.25f
            val inactiveTrackColor = if (inactiveColor != Color.Unspecified) inactiveColor else primaryColor.copy(alpha = disabledAlpha)

            // Inactive track (background line)
            drawLine(
                color = inactiveTrackColor,
                start = Offset(0f, centerY),
                end = Offset(totalWidth, centerY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            // Active track (played progress line)
            if (totalProgressPx > 0f) {
                drawLine(
                    color = primaryColor,
                    start = Offset(0f, centerY),
                    end = Offset(totalProgressPx, centerY),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }

            // Signature Android / Material 3 Pill Thumb
            val thumbHalfHeight = 7.dp.toPx()
            val thumbWidth = 4.5.dp.toPx()

            drawLine(
                color = primaryColor,
                start = Offset(totalProgressPx, centerY - thumbHalfHeight),
                end = Offset(totalProgressPx, centerY + thumbHalfHeight),
                strokeWidth = thumbWidth,
                cap = StrokeCap.Round
            )
        }
    }
}
