package com.muzi.desktop.ui.utils

/**
 * Android Muzi Image Resize Utility for High-Resolution Album Covers & Thumbnails.
 * Automatically upscales YouTube/Google low-res thumbs to 544x544 or 1200x1200 HD covers.
 */
fun String.resize(
    width: Int? = null,
    height: Int? = null,
): String {
    if (width == null && height == null) return this

    if (this.contains("i.ytimg.com")) {
        val targetQuality = if ((width ?: 0) >= 600 || (height ?: 0) >= 600) "maxresdefault.jpg" else "hqdefault.jpg"
        return this.replace(
            Regex("(default|mqdefault|hqdefault|sddefault|maxresdefault)\\.jpg"),
            targetQuality
        )
    }

    if (this.contains("googleusercontent.com") && this.contains("=w")) {
        val baseUrl = this.split("=w")[0]
        val size = if ((width ?: 0) >= 600 || (height ?: 0) >= 600) 1200 else 544
        return "$baseUrl=w$size-h$size-l90-rj"
    }

    if (this.contains("yt3.ggpht.com")) {
        val baseUrl = this.split("=")[0].split("-s")[0]
        val size = if ((width ?: 0) >= 600 || (height ?: 0) >= 600) 1200 else 544
        return "$baseUrl=s$size"
    }

    "https://lh\\d\\.googleusercontent\\.com/.*".toRegex().matchEntire(this)?.let {
        val size = if ((width ?: 0) >= 600 || (height ?: 0) >= 600) 1200 else 544
        return "${this.split("=")[0]}=w$size-h$size-l90-rj"
    }

    return this
}

fun String?.toHighResThumbnail(): String {
    if (this.isNullOrBlank()) return ""
    return this.resize(1200, 1200)
}

fun String?.toMediumThumbnail(): String {
    if (this.isNullOrBlank()) return ""
    return this.resize(544, 544)
}
