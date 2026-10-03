package com.resonance.player.feature.player

import com.resonance.player.core.ui.theme.ResonanceTheme
import com.resonance.player.core.ui.components.PlayerAnchors
import com.resonance.player.core.ui.components.PlayerAnchor
import com.resonance.player.core.ui.components.PlayShapes
import com.resonance.player.core.ui.components.MorphShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.Alignment
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
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

/**
 * Now Playing growing out of the mini player ("container transform").
 *
 * [progress] runs 0 (mini player) .. 1 (full screen) and may come straight
 * from a finger. The container's rounded rect goes from [miniBounds] to the
 * whole screen; the cover, title, artist, play and next buttons fly from
 * their mini player spots ([anchors].mini) to their Now Playing spots
 * ([anchors].big), the text crossfading between the small and the big type;
 * the rest of the player fades in over the second half. Everything that
 * moves reads [progress] in the draw/layer phase only.
 */
@Composable
fun NowPlayingSheet(
    progress: () -> Float,
    miniBounds: Rect?,
    anchors: PlayerAnchors,
    fallbackArt: Rect,
    artworkUri: String?,
    title: String,
    artist: String,
    playing: Boolean,
    palette: ArtworkPalette,
    /** The big cover is drawn smaller while paused (0.86); the flight lands on that size. */
    coverScale: Float = 1f,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
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

        val inFlight: (Float) -> Boolean = { p -> p > 0.001f && p < 0.999f }

        // Cover: drawn at the big cover's size and transformed down to the thumbnail.
        val bigArt = (anchors.big[PlayerAnchor.Art] ?: fallbackArt).let { b ->
            val c = b.center
            val half = b.width * coverScale / 2f
            Rect(c.x - half, c.y - half, c.x + half, c.y + half)
        }
        val miniArt = anchors.mini[PlayerAnchor.Art]
        if (miniArt != null && bigArt.width > 0f) {
            Box(
                Modifier
                    .size(with(density) { bigArt.width.toDp() }, with(density) { bigArt.height.toDp() })
                    .graphicsLayer {
                        val p = progress().coerceIn(0f, 1f)
                        val current = lerp(miniArt, bigArt, p)
                        val scale = current.width / bigArt.width
                        transformOrigin = TransformOrigin(0f, 0f)
                        scaleX = scale
                        scaleY = scale
                        translationX = current.left
                        translationY = current.top
                        shape = RoundedCornerShape(lerp(miniArtCorner, bigArtCorner, p) / scale.coerceAtLeast(0.01f))
                        clip = true
                        alpha = if (inFlight(p)) 1f else 0f
                    }
            ) {
                ArtworkImage(artworkUri = artworkUri, contentDescription = null, modifier = Modifier.fillMaxSize())
            }
        }

        // Title and artist: the small text grows out while the big text grows in, both on one path.
        TextFlight(PlayerAnchor.Title, anchors, progress, title, typography.titleMd, typography.headlineLg, colors.textPrimary)
        TextFlight(PlayerAnchor.Artist, anchors, progress, artist, typography.bodySm, typography.bodyLg, colors.textSecondary)

        // Play / pause: the big button's shape and icon, scaled down to the mini button.
        Flight(PlayerAnchor.Play, anchors, progress) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        shape = MorphShape(PlayShapes.playPause, if (playing) 1f else 0f)
                        clip = true
                    }
                    .background(palette.accent)
            ) {
                Icon(
                    if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = colors.onAccent,
                    modifier = Modifier.fillMaxSize(0.5f)
                )
            }
        }

        // Next.
        Flight(PlayerAnchor.Next, anchors, progress) {
            Icon(Icons.Rounded.SkipNext, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.fillMaxSize())
        }
    }
}

/**
 * Draws [content] at the size of the anchor's Now Playing spot and moves /
 * scales it from the mini player spot to there as [progress] goes 0 -> 1.
 * Visible only in between; at rest the real elements show.
 */
@Composable
private fun Flight(anchor: PlayerAnchor, anchors: PlayerAnchors, progress: () -> Float, content: @Composable () -> Unit) {
    val from = anchors.mini[anchor] ?: return
    val to = anchors.big[anchor] ?: return
    if (to.width <= 0f || to.height <= 0f) return
    val density = LocalDensity.current
    Box(
        Modifier
            .size(with(density) { to.width.toDp() }, with(density) { to.height.toDp() })
            .graphicsLayer {
                val p = progress().coerceIn(0f, 1f)
                val current = lerp(from, to, p)
                val scale = current.height / to.height
                transformOrigin = TransformOrigin(0f, 0f)
                scaleX = scale
                scaleY = scale
                translationX = current.left
                translationY = current.top
                alpha = if (p > 0.001f && p < 0.999f) 1f else 0f
            }
    ) {
        content()
    }
}

/** A line of text flying between its small and big spot, crossfading between the two type styles. */
@Composable
private fun TextFlight(
    anchor: PlayerAnchor,
    anchors: PlayerAnchors,
    progress: () -> Float,
    text: String,
    small: TextStyle,
    big: TextStyle,
    color: Color
) {
    val from = anchors.mini[anchor] ?: return
    val to = anchors.big[anchor] ?: return
    if (from.height <= 0f || to.height <= 0f) return
    val density = LocalDensity.current
    listOf(Triple(from, small, true), Triple(to, big, false)).forEach { (reference, style, isSmall) ->
        Text(
            text,
            style = style,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .size(with(density) { reference.width.toDp() }, with(density) { reference.height.toDp() })
                .graphicsLayer {
                    val p = progress().coerceIn(0f, 1f)
                    val current = lerp(from, to, p)
                    val scale = current.height / reference.height
                    transformOrigin = TransformOrigin(0f, 0f)
                    scaleX = scale
                    scaleY = scale
                    translationX = current.left
                    translationY = current.top
                    alpha = if (p > 0.001f && p < 0.999f) (if (isSmall) 1f - p else p) else 0f
                }
        )
    }
}
