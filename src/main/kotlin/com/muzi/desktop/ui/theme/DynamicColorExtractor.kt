package com.muzi.desktop.ui.theme

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.image.BufferedImage
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO

object DynamicColorExtractor {
    private val colorCache = ConcurrentHashMap<String, Color>()
    private val paletteCache = ConcurrentHashMap<String, List<Color>>()
    val DefaultMuziColor = Color(0xFF3B82F6) // Muzi Electric Blue

    suspend fun extractFromUrl(url: String?): Color = withContext(Dispatchers.IO) {
        if (url.isNullOrBlank()) return@withContext DefaultMuziColor
        colorCache[url]?.let { return@withContext it }
        val palette = extractPaletteFromUrl(url)
        val best = palette.firstOrNull() ?: DefaultMuziColor
        colorCache[url] = best
        best
    }

    suspend fun extractPaletteFromUrl(url: String?): List<Color> = withContext(Dispatchers.IO) {
        if (url.isNullOrBlank()) return@withContext listOf(DefaultMuziColor, Color(0xFF1E3A8A), Color(0xFF0F172A))
        paletteCache[url]?.let { return@withContext it }

        try {
            val img: BufferedImage = ImageIO.read(URI(url).toURL()) ?: return@withContext listOf(DefaultMuziColor)
            val width = img.width
            val height = img.height
            val stepX = (width / 24).coerceAtLeast(1)
            val stepY = (height / 24).coerceAtLeast(1)

            val sampledColors = mutableListOf<Color>()
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

                    // Capture rich colors: avoid extreme blacks and washed out whites
                    if (sat > 0.25f && bri in 0.25f..0.88f) {
                        sampledColors.add(Color(r, g, b))
                    }
                }
            }

            // Cluster / deduplicate sampled colors
            val distinctColors = mutableListOf<Color>()
            for (c in sampledColors) {
                val isDistinct = distinctColors.none { existing ->
                    val dr = kotlin.math.abs(existing.red - c.red)
                    val dg = kotlin.math.abs(existing.green - c.green)
                    val db = kotlin.math.abs(existing.blue - c.blue)
                    (dr + dg + db) < 0.35f
                }
                if (isDistinct) {
                    distinctColors.add(c)
                    if (distinctColors.size >= 6) break
                }
            }

            val finalPalette = if (distinctColors.isNotEmpty()) {
                distinctColors
            } else {
                listOf(DefaultMuziColor, Color(0xFF2563EB), Color(0xFF1D4ED8))
            }

            paletteCache[url] = finalPalette
            finalPalette
        } catch (_: Exception) {
            listOf(DefaultMuziColor, Color(0xFF1E3A8A), Color(0xFF0F172A))
        }
    }
}
