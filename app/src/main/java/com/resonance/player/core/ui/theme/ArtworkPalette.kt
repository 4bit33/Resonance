package com.resonance.player.core.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.compose.animation.animateColorAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Colors taken from a song's artwork, already tuned for the dark player:
 * [accent] is bright enough for dark text on it, [glow] is the vivid tone
 * behind the cover, [background] is a deep tint of the cover's mood.
 */
@Immutable
data class ArtworkPalette(
    val accent: Color,
    val glow: Color,
    val background: Color,
    val surface: Color
) {
    companion object {
        /** Neutral palette built from the user's accent (no artwork / artwork colors off). */
        fun fromAccent(accent: Color): ArtworkPalette = fromSeed(accent.toArgb())

        internal fun fromSeed(argb: Int): ArtworkPalette {
            val hsl = FloatArray(3)
            ColorUtils.colorToHSL(argb, hsl)
            val hue = hsl[0]
            // Greyscale covers have no meaningful hue: keep them neutral instead of tinting red.
            val sat = hsl[1]
            val grey = sat < 0.12f
            val accent = ColorUtils.HSLToColor(floatArrayOf(hue, if (grey) 0f else sat.coerceIn(0.55f, 0.9f), 0.76f))
            val glow = ColorUtils.HSLToColor(floatArrayOf(hue, if (grey) 0f else sat.coerceIn(0.6f, 1f), 0.55f))
            val background = ColorUtils.HSLToColor(floatArrayOf(hue, if (grey) 0f else 0.45f, 0.075f))
            val surface = ColorUtils.HSLToColor(floatArrayOf(hue, if (grey) 0f else 0.32f, 0.14f))
            return ArtworkPalette(Color(accent), Color(glow), Color(background), Color(surface))
        }
    }
}

private val paletteCache = LruCache<String, ArtworkPalette>(64)

/** Decodes a small copy of the artwork and picks the most characteristic color. */
private suspend fun extractPalette(context: Context, uri: String): ArtworkPalette? =
    withContext(Dispatchers.IO) {
        paletteCache.get(uri)?.let { return@withContext it }
        val bitmap = runCatching { decodeSmall(context, Uri.parse(uri)) }.getOrNull() ?: return@withContext null
        val palette = Palette.from(bitmap).maximumColorCount(16).generate()
        bitmap.recycle()
        val swatch = palette.vibrantSwatch
            ?: palette.lightVibrantSwatch
            ?: palette.dominantSwatch
            ?: palette.mutedSwatch
            ?: return@withContext null
        // A vibrant swatch that covers almost nothing of the cover misrepresents it.
        val dominant = palette.dominantSwatch
        val seed = if (dominant != null && swatch.population * 6 < dominant.population && saturation(dominant.rgb) > 0.2f) {
            dominant.rgb
        } else {
            swatch.rgb
        }
        ArtworkPalette.fromSeed(seed).also { paletteCache.put(uri, it) }
    }

private fun saturation(argb: Int): Float {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(argb, hsl)
    return hsl[1]
}

private fun decodeSmall(context: Context, uri: Uri): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= 96 && bounds.outHeight / (sample * 2) >= 96) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
}

/**
 * The palette for [artworkUri], animated: when the song changes, every color
 * flows to the new cover's colors instead of jumping.
 */
@Composable
fun rememberArtworkPalette(artworkUri: String?, enabled: Boolean = true): ArtworkPalette {
    val context = LocalContext.current
    val fallback = ArtworkPalette.fromAccent(ResonanceTheme.colors.accent)
    val target by produceState(fallback, artworkUri, enabled, fallback) {
        value = if (enabled && !artworkUri.isNullOrBlank()) {
            extractPalette(context.applicationContext, artworkUri) ?: fallback
        } else {
            fallback
        }
    }
    val motion = ResonanceTheme.motion
    val accent by animateColorAsState(target.accent, motion.colorChange(), label = "palette-accent")
    val glow by animateColorAsState(target.glow, motion.colorChange(), label = "palette-glow")
    val background by animateColorAsState(target.background, motion.colorChange(), label = "palette-bg")
    val surface by animateColorAsState(target.surface, motion.colorChange(), label = "palette-surface")
    return ArtworkPalette(accent, glow, background, surface)
}
