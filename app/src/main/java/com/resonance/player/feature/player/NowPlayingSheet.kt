package com.resonance.player.feature.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.resonance.player.core.ui.components.ArtworkImage
import com.resonance.player.core.ui.theme.ArtworkPalette
import kotlin.math.roundToInt

/**
 * Now Playing growing out of the mini player ("container transform").
 *
 * [progress] runs 0 (mini player) .. 1 (full screen) and may come straight
 * from a finger. The container's rounded rect goes from [miniBounds] to the
 * whole screen, the cover flies from [miniArtBounds] to [bigArtBounds], and
 * the player fades in over the second part. Everything that moves reads
 * [progress] in the draw/layer phase only, so following a finger never
 * recomposes the player.
 */
@Composable
fun NowPlayingSheet(
    progress: () -> Float,
    miniBounds: Rect?,
    miniArtBounds: Rect?,
    bigArtBounds: Rect?,
    artworkUri: String?,
    palette: ArtworkPalette,
    /** The big cover is drawn smaller while paused (0.86); the flight lands on that size. */
    coverScale: Float = 1f,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val miniCorner = with(density) { 20.dp.toPx() }
    val miniArtCorner = with(density) { 12.dp.toPx() }
    val bigArtCorner = with(density) { 28.dp.toPx() }

    Box(
        Modifier
            .fillMaxSize()
            // Fully collapsed: placed off-screen, so it neither draws nor takes touches
            // (it stays composed to make opening smooth).
            .offset { IntOffset(0, if (progress() <= 0f) 1_000_000 else 0) }
            // Dim the app behind as the player rises.
            .drawBehind { drawRect(Color.Black.copy(alpha = 0.45f * progress().coerceIn(0f, 1f))) }
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .drawWithContent {
                    val p = progress().coerceIn(0f, 1f)
                    val full = Rect(0f, 0f, size.width, size.height)
                    val from = miniBounds ?: Rect(0f, size.height - 64.dp.toPx(), size.width, size.height)
                    val rect = lerp(from, full, p)
                    val corner = lerp(miniCorner, 0f, p)
                    val path = Path().apply { addRoundRect(RoundRect(rect, CornerRadius(corner))) }
                    clipPath(path) {
                        // Starts as the mini player's own color, so the bar seems to grow.
                        val background = lerp(palette.surface, palette.background, (p * 2f).coerceAtMost(1f))
                        drawRect(background.copy(alpha = (p * 8f).coerceAtMost(1f)))
                        this@drawWithContent.drawContent()
                    }
                }
        ) {
            // The player appears over the second half of the motion, rising slightly.
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val p = progress().coerceIn(0f, 1f)
                        alpha = ((p - 0.45f) / 0.55f).coerceIn(0f, 1f)
                        translationY = (1f - p) * size.height * 0.08f
                    }
            ) {
                content()
            }
        }
        // The flying cover: drawn at the big cover's size and transformed down to the thumbnail.
        val target = bigArtBounds?.let { b ->
            val c = b.center
            val half = b.width * coverScale / 2f
            Rect(c.x - half, c.y - half, c.x + half, c.y + half)
        }
        val start = miniArtBounds
        if (target != null && start != null && target.width > 0f) {
            Box(
                Modifier
                    .offset { IntOffset(target.left.roundToInt(), target.top.roundToInt()) }
                    .size(with(density) { target.width.toDp() }, with(density) { target.height.toDp() })
                    .graphicsLayer {
                        val p = progress().coerceIn(0f, 1f)
                        val current = lerp(start, target, p)
                        val scale = current.width / target.width
                        transformOrigin = TransformOrigin(0f, 0f)
                        scaleX = scale
                        scaleY = scale
                        translationX = current.left - target.left
                        translationY = current.top - target.top
                        shape = RoundedCornerShape(lerp(miniArtCorner, bigArtCorner, p) / scale.coerceAtLeast(0.01f))
                        clip = true
                        // Only while in between: at rest the real covers (mini / big) are shown.
                        alpha = if (p > 0.001f && p < 0.999f) 1f else 0f
                    }
            ) {
                ArtworkImage(artworkUri = artworkUri, contentDescription = null, modifier = Modifier.fillMaxSize())
            }
        }
    }
}
