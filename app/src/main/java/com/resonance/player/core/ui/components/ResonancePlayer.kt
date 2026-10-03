package com.resonance.player.core.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlin.math.abs
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.resonance.player.core.model.PlaybackSnapshot
import com.resonance.player.core.ui.theme.ResonanceTheme

/**
 * Pure rule: the mini player exists only while a track is loaded.
 * Tested — the shell must not invent visibility state.
 */
fun shouldShowMiniPlayer(snapshot: PlaybackSnapshot): Boolean = snapshot.song != null

/**
 * Mini player: a floating card tinted with the artwork's colors. Tap or drag
 * up opens Now Playing; drag sideways and the card follows the finger, then
 * skips to the next/previous track (or springs back if released early).
 * Receives snapshot values as params, never touches playback state.
 */
@Composable
fun ResonanceMiniPlayer(
    title: String,
    artist: String,
    artwork: @Composable () -> Unit,
    isPlaying: Boolean,
    progress: Float,
    playDescription: String,
    pauseDescription: String,
    nextDescription: String?,
    onToggle: () -> Unit,
    onOpenPlayer: () -> Unit,
    onNext: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onPrevious: (() -> Unit)? = null,
    containerColor: Color = ResonanceTheme.colors.surfaceHigh,
    accent: Color = ResonanceTheme.colors.accent,
    /** Vertical drag drives the Now Playing expansion (delta px, up is negative). */
    onExpandDrag: (Float) -> Unit = {},
    onExpandDragStopped: suspend (velocity: Float) -> Unit = {},
    /** Where the cover thumbnail is on screen, so Now Playing can grow it into the big cover. */
    onArtworkBounds: (Rect) -> Unit = {}
) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    val spacing = ResonanceTheme.spacing
    val dimensions = ResonanceTheme.dimensions
    val motion = ResonanceTheme.motion
    val haptics = LocalHapticFeedback.current
    val next by rememberUpdatedState(onNext)
    val previous by rememberUpdatedState(onPrevious)

    var widthPx by remember { mutableFloatStateOf(1f) }
    var dragX by remember { mutableFloatStateOf(0f) }
    val expandDrag by rememberUpdatedState(onExpandDrag)
    val horizontal = rememberDraggableState { dragX += it }
    val vertical = rememberDraggableState { expandDrag(it) }

    val shownProgress = remember { Animatable(progress.coerceIn(0f, 1f)) }
    LaunchedEffect(progress) {
        val target = progress.coerceIn(0f, 1f)
        // Ticks arrive every 500 ms: glide between them, but jump on seeks and track changes.
        if (abs(target - shownProgress.value) > 0.05f) {
            shownProgress.snapTo(target)
        } else {
            shownProgress.animateTo(target, tween(500, easing = LinearEasing))
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(dimensions.miniPlayerHeight)
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .draggable(
                state = vertical,
                orientation = Orientation.Vertical,
                onDragStopped = { velocity -> onExpandDragStopped(velocity) }
            )
            .draggable(
                state = horizontal,
                orientation = Orientation.Horizontal,
                onDragStopped = { velocity ->
                    val goNext = next != null && (dragX < -widthPx * 0.3f || (velocity < -1200f && dragX < 0f))
                    val goPrevious = previous != null && (dragX > widthPx * 0.3f || (velocity > 1200f && dragX > 0f))
                    if (goNext || goPrevious) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val direction = if (goNext) -1f else 1f
                        animate(dragX, direction * widthPx, animationSpec = motion.spatialFast()) { v, _ -> dragX = v }
                        if (goNext) next?.invoke() else previous?.invoke()
                        dragX = -direction * widthPx * 0.6f
                    }
                    animate(dragX, 0f, animationSpec = motion.spatial()) { v, _ -> dragX = v }
                }
            )
            .clip(RoundedCornerShape(20.dp))
            .background(containerColor)
            .clickable(onClick = onOpenPlayer)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = dragX
                    alpha = 1f - (abs(dragX) / widthPx).coerceIn(0f, 0.8f)
                }
                .padding(start = 10.dp, end = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .onGloballyPositioned { onArtworkBounds(it.boundsInRoot()) }
                    .clip(RoundedCornerShape(12.dp))
            ) {
                artwork()
            }
            Spacer(Modifier.width(spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = typography.titleMd,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    artist,
                    style = typography.bodySm,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            MiniPlayButton(
                playing = isPlaying,
                accent = accent,
                description = if (isPlaying) pauseDescription else playDescription,
                onClick = onToggle
            )
            if (onNext != null) {
                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(spacing.touchMin)
                ) {
                    Icon(
                        Icons.Rounded.SkipNext,
                        contentDescription = nextDescription,
                        tint = colors.textPrimary
                    )
                }
            }
        }
        Canvas(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(3.dp)
        ) {
            drawRect(colors.textPrimary.copy(alpha = 0.10f))
            drawRect(accent, size = Size(size.width * shownProgress.value, size.height))
        }
    }
}

/** 40dp accent button: circle when paused, rounded square while playing. */
@Composable
private fun MiniPlayButton(playing: Boolean, accent: Color, description: String, onClick: () -> Unit) {
    val colors = ResonanceTheme.colors
    val motion = ResonanceTheme.motion
    val morph = animateFloatAsState(if (playing) 1f else 0f, motion.spatial(), label = "mini-play-morph")
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClickLabel = description,
                onClick = onClick
            )
            .semantics { contentDescription = description }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .graphicsLayer {
                    shape = MorphShape(PlayShapes.playPause, morph.value)
                    clip = true
                }
                .background(accent)
        ) {
            Crossfade(targetState = playing, animationSpec = motion.duration(150), label = "mini-play-icon") { p ->
                Icon(
                    if (p) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = colors.onAccent,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Up-Next queue peek card: accent eyebrow, next title/artist, duration,
 * drag affordance. Tap navigates to the full queue.
 */
@Composable
fun ResonanceQueuePeek(
    nextTitle: String,
    nextArtist: String,
    nextDuration: String,
    nextArtwork: @Composable () -> Unit,
    dragDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    eyebrow: String = "UP NEXT IN QUEUE",
    dragIcon: (@Composable () -> Unit)? = null
) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    val spacing = ResonanceTheme.spacing
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(ResonanceTheme.radii.card)
            .background(colors.surfaceHigh)
            .clickable(onClick = onClick)
            .padding(spacing.md)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(ResonanceTheme.radii.control)
        ) {
            nextArtwork()
        }
        Spacer(Modifier.width(spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                eyebrow,
                style = typography.labelSm,
                color = colors.accent
            )
            Text(
                nextTitle,
                style = typography.titleMd,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                nextArtist,
                style = typography.bodySm,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(nextDuration, style = typography.monoMetric, color = colors.textSecondary)
        Spacer(Modifier.width(spacing.sm))
        if (dragIcon != null) {
            dragIcon()
        } else {
            Spacer(Modifier.size(20.dp))
        }
    }
}
