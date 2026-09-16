package com.resonance.player.core.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.resonance.player.core.ui.theme.ResonanceTheme

/** Compact note-glyph fallback for missing artwork (dense rows and grids alike). */
@Composable
fun NoteFallback(
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp
) {
    val colors = ResonanceTheme.colors
    Box(
        modifier = modifier
            .clip(ResonanceTheme.radii.control)
            .background(colors.surfaceContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Filled.MusicNote,
            contentDescription = null,
            tint = colors.textMuted,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Playing-state indicator: three bars animating ONLY while [isPlaying].
 * This is a state animation, not audio analysis — frequency data is never
 * claimed (STEP 8/14).
 */
@Composable
fun EqualizerBars(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barWidth: Dp = 3.dp
) {
    val colors = ResonanceTheme.colors
    if (isPlaying) {
        val transition = rememberInfiniteTransition(label = "eq")
        val a by transition.animateFloat(
            0.35f, 1f,
            infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Reverse),
            label = "eqA"
        )
        val b by transition.animateFloat(
            1f, 0.4f,
            infiniteRepeatable(tween(400, easing = LinearEasing), RepeatMode.Reverse),
            label = "eqB"
        )
        val c by transition.animateFloat(
            0.5f, 0.9f,
            infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse),
            label = "eqC"
        )
        Row(
            modifier,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EqBar(a, barWidth, colors.accent)
            EqBar(b, barWidth, colors.accent)
            EqBar(c, barWidth, colors.accent)
        }
    } else {
        Row(
            modifier,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EqBar(0.5f, barWidth, colors.textMuted)
            EqBar(0.8f, barWidth, colors.textMuted)
            EqBar(0.45f, barWidth, colors.textMuted)
        }
    }
}

@Composable
private fun EqBar(
    fraction: Float,
    barWidth: Dp,
    color: androidx.compose.ui.graphics.Color
) {
    Box(
        modifier = Modifier.size(width = barWidth, height = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = barWidth, height = (16.dp * fraction).coerceAtLeast(3.dp))
                .clip(RoundedCornerShape(50))
                .background(color)
        )
    }
}
