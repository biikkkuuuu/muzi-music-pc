package com.muzi.desktop.ui.theme

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.image.BufferedImage
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO

object DynamicColorExtractor {
    private val cache = ConcurrentHashMap<String, Color>()
    val DefaultMuziColor = Color(0xFFE50914) // Muzi Red

    suspend fun extractFromUrl(url: String?): Color = withContext(Dispatchers.IO) {
        if (url.isNullOrBlank()) return@withContext DefaultMuziColor
        cache[url]?.let { return@withContext it }

        try {
            val img: BufferedImage = ImageIO.read(URI(url).toURL()) ?: return@withContext DefaultMuziColor
            
            // Sample a 32x32 scaled version for fast processing
            val width = img.width
            val height = img.height
            val stepX = (width / 32).coerceAtLeast(1)
            val stepY = (height / 32).coerceAtLeast(1)

            var bestColor = DefaultMuziColor
            var maxScore = -1.0f

            val hsb = FloatArray(3)

            for (y in 0 until height step stepY) {
                for (x in 0 until width step stepX) {
                    val rgb = img.getRGB(x, y)
                    val r = (rgb shr 16) and 0xFF
                    val g = (rgb shr 8) and 0xFF
                    val b = rgb and 0xFF

                    java.awt.Color.RGBtoHSB(r, g, b, hsb)
                    val sat = hsb[1]
                    val bri = hsb[2]

                    // We want vibrant, rich colors (not too dark, not washed out white/grey)
                    if (sat > 0.35f && bri > 0.3f && bri < 0.9f) {
                        val score = sat * 2.0f + bri
                        if (score > maxScore) {
                            maxScore = score
                            bestColor = Color(r, g, b)
                        }
                    }
                }
            }

            cache[url] = bestColor
            return@withContext bestColor
        } catch (e: Exception) {
            return@withContext DefaultMuziColor
        }
    }
}
