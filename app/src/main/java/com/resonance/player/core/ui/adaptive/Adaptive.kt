package com.resonance.player.core.ui.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Minimal width-size buckets (same breakpoints as Material WindowSizeClass).
 * Implemented locally to avoid an extra dependency in the foundation phase;
 * the UI phase may swap in the official adaptive libraries.
 */
enum class WindowWidthSize { COMPACT, MEDIUM, EXPANDED }

fun windowWidthSizeFor(widthDp: Dp): WindowWidthSize = when {
    widthDp < 600.dp -> WindowWidthSize.COMPACT
    widthDp < 840.dp -> WindowWidthSize.MEDIUM
    else -> WindowWidthSize.EXPANDED
}

@Composable
fun rememberWindowWidthSize(): WindowWidthSize =
    windowWidthSizeFor(LocalConfiguration.current.screenWidthDp.dp)
