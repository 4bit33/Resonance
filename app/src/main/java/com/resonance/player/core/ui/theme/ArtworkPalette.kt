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
            val accent = ColorUtils.HSLToColor(floatArrayOf(hue, if (grey) 0f else sat.coerceIn(0.7f, 1f), 0.68f))
            val glow = ColorUtils.HSLToColor(floatArrayOf(hue, if (grey) 0f else sat.coerceIn(0.7f, 1f), 0.55f))
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
        // clearFilters: Palette's default filter throws away orange and brown (hue 10-37) as
        // "skin tones", which turned warm covers red or pink.
        val palette = Palette.from(bitmap).maximumColorCount(24).clearFilters().generate()
        bitmap.recycle()
        val seed = pickSeed(palette.swatches.map { it.rgb to it.population }) ?: return@withContext null
        ArtworkPalette.fromSeed(seed).also { paletteCache.put(uri, it) }
    }

/**
 * The color that characterizes a cover: the one that fills the most of it,
 * weighted towards saturated colors, ignoring near-black and near-white. A
 * vivid speck does not beat a large muted area, and a grey cover stays grey.
 */
internal fun pickSeed(swatches: List<Pair<Int, Int>>): Int? =
    swatches.maxByOrNull { (rgb, population) -> colorfulness(rgb) * population }?.first

private fun colorfulness(rgb: Int): Float {
    val r = (rgb shr 16 and 0xFF) / 255f
    val g = (rgb shr 8 and 0xFF) / 255f
    val b = (rgb and 0xFF) / 255f
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val lightness = (max + min) / 2f
    val saturation = if (max == min) 0f else (max - min) / (1f - kotlin.math.abs(2f * lightness - 1f))
    val usable = if (lightness < 0.12f || lightness > 0.92f) 0.05f else 1f
    return (0.15f + saturation) * (0.15f + saturation) * usable
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
 *
 * A cover seen before starts on its cached colors right away, and an unseen
 * one starts from the last palette shown anywhere, so a screen that opens
 * (Now Playing, a page, the mini player coming back) never flashes the plain
 * accent before turning into the artwork's colors.
 */
@Composable
fun rememberArtworkPalette(artworkUri: String?, enabled: Boolean = true): ArtworkPalette {
    val context = LocalContext.current
    val fallback = ArtworkPalette.fromAccent(ResonanceTheme.colors.accent)
    val known = if (enabled && !artworkUri.isNullOrBlank()) paletteCache.get(artworkUri) else null
    val initial = known ?: if (enabled) lastShownPalette ?: fallback else fallback
    val target by produceState(initial, artworkUri, enabled, fallback) {
        value = if (enabled && !artworkUri.isNullOrBlank()) {
            extractPalette(context.applicationContext, artworkUri) ?: fallback
        } else {
            fallback
        }
        if (enabled) lastShownPalette = value
    }
    val motion = ResonanceTheme.motion
    val accent by animateColorAsState(target.accent, motion.colorChange(), label = "palette-accent")
    val glow by animateColorAsState(target.glow, motion.colorChange(), label = "palette-glow")
    val background by animateColorAsState(target.background, motion.colorChange(), label = "palette-bg")
    val surface by animateColorAsState(target.surface, motion.colorChange(), label = "palette-surface")
    return ArtworkPalette(accent, glow, background, surface)
}

/** The most recent artwork palette any screen settled on (process-wide, main thread). */
private var lastShownPalette: ArtworkPalette? = null
