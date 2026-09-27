package com.muzi.desktop.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin

/**
 * Android Muzi Signature Ambient Mesh Glow Background
 * Replicating com.biikkkuuuu.muzi.ui.screens.ambient.AmbientGlowBackground:
 * Multi-point rotating oscillating radial gradient mesh reacting to album artwork palette.
 */
@Composable
fun AmbientGlowBackground(
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    val activeColors = if (colors.isNotEmpty()) colors else listOf(
        Color(0xFF1E3A8A),
        Color(0xFF2563EB),
        Color(0xFF3B82F6),
        Color(0xFF1D4ED8)
    )

    AnimatedContent(
        targetState = activeColors,
        transitionSpec = {
            fadeIn(tween(1000)) togetherWith fadeOut(tween(1000))
        },
        label = "AmbientGlowTransition",
        modifier = modifier
    ) { palette ->
        val infiniteTransition = rememberInfiniteTransition(label = "GlowRotation")

        val progress by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(24000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "meshProgress"
        )

        fun rotatedColorAt(index: Int): Color {
            val size = palette.size
            val idx = index.toFloat() + progress * size
            val a = floor(idx).toInt() % size
            val b = (a + 1) % size
            val frac = idx - floor(idx)
            return lerp(
                palette.getOrElse(a) { Color(0xFF1E3A8A) },
                palette.getOrElse(b) { Color(0xFF2563EB) },
                frac
            )
        }

        fun oscillate(min: Float, max: Float, phase: Float, speed: Float = 1f): Float {
            val v = sin(2f * PI.toFloat() * (progress * speed + phase))
            return min + (max - min) * ((v + 1f) * 0.5f)
        }

        val c1 = rotatedColorAt(0)
        val c2 = rotatedColorAt(1)
        val c3 = rotatedColorAt(2)
        val c4 = rotatedColorAt(3)
        val c5 = rotatedColorAt(4)
        val c6 = rotatedColorAt(5)

        val o1x = oscillate(0.05f, 0.95f, 0.00f, 1.0f)
        val o1y = oscillate(0.05f, 0.55f, 0.08f, 1.0f)
        val r1 = oscillate(0.7f, 1.4f, 0.12f, 1.0f)

        val o2x = oscillate(0.95f, 0.05f, 0.20f, 1.0f)
        val o2y = oscillate(0.45f, 0.95f, 0.25f, 1.0f)
        val r2 = oscillate(0.6f, 1.3f, 0.18f, 1.0f)

        val o3x = oscillate(0.20f, 0.80f, 0.33f, 1.0f)
        val o3y = oscillate(0.80f, 0.20f, 0.36f, 1.0f)
        val r3 = oscillate(0.5f, 1.2f, 0.29f, 1.0f)

        val o4x = oscillate(0.30f, 0.70f, 0.44f, 1.0f)
        val o4y = oscillate(0.20f, 0.80f, 0.41f, 1.0f)
        val r4 = oscillate(0.8f, 1.5f, 0.47f, 1.0f)

        val o5x = oscillate(0.40f, 0.60f, 0.55f, 1.0f)
        val o5y = oscillate(0.05f, 0.95f, 0.51f, 1.0f)
        val r5 = oscillate(0.6f, 1.3f, 0.58f, 1.0f)

        val o6x = oscillate(0.05f, 0.95f, 0.66f, 1.0f)
        val o6y = oscillate(0.50f, 0.70f, 0.62f, 1.0f)
        val r6 = oscillate(0.7f, 1.6f, 0.69f, 1.0f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .drawWithCache {
                    val w = size.width
                    val h = size.height

                    val b1 = Brush.radialGradient(
                        colors = listOf(c1.copy(alpha = 0.55f), c1.copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(w * o1x, h * o1y),
                        radius = (w.coerceAtLeast(h)) * r1
                    )
                    val b2 = Brush.radialGradient(
                        colors = listOf(c2.copy(alpha = 0.45f), c2.copy(alpha = 0.18f), Color.Transparent),
                        center = Offset(w * o2x, h * o2y),
                        radius = (w.coerceAtLeast(h)) * r2
                    )
                    val b3 = Brush.radialGradient(
                        colors = listOf(c3.copy(alpha = 0.40f), c3.copy(alpha = 0.15f), Color.Transparent),
                        center = Offset(w * o3x, h * o3y),
                        radius = (w.coerceAtLeast(h)) * r3
                    )
                    val b4 = Brush.radialGradient(
                        colors = listOf(c4.copy(alpha = 0.35f), c4.copy(alpha = 0.12f), Color.Transparent),
                        center = Offset(w * o4x, h * o4y),
                        radius = (w.coerceAtLeast(h)) * r4
                    )
                    val b5 = Brush.radialGradient(
                        colors = listOf(c5.copy(alpha = 0.30f), c5.copy(alpha = 0.10f), Color.Transparent),
                        center = Offset(w * o5x, h * o5y),
                        radius = (w.coerceAtLeast(h)) * r5
                    )
                    val b6 = Brush.radialGradient(
                        colors = listOf(c6.copy(alpha = 0.25f), Color.Transparent),
                        center = Offset(w * o6x, h * o6y),
                        radius = (w.coerceAtLeast(h)) * r6
                    )

                    onDrawBehind {
                        drawRect(Color(0xFF040406))
                        drawRect(b1)
                        drawRect(b2)
                        drawRect(b3)
                        drawRect(b4)
                        drawRect(b5)
                        drawRect(b6)
                    }
                }
        )
    }
}
