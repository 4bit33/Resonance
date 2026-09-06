package com.resonance.player.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/**
 * Cached-artwork image with a deterministic local fallback. Coil loads the
 * file:// URI lazily with downsampling + memory cache (never full-res, never
 * a Room BLOB); missing/corrupt art shows the Stitch etched fallback — never
 * an error state. No network is involved — only app-cache files.
 */
@Composable
fun ArtworkImage(
    artworkUri: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    fallbackSize: Dp = 24.dp,
    fallbackStyle: ArtworkFallbackStyle = ArtworkFallbackStyle.Etched
) {
    var failed by remember(artworkUri) { mutableStateOf(false) }
    if (artworkUri.isNullOrBlank() || failed) {
        when (fallbackStyle) {
            ArtworkFallbackStyle.Etched -> WaveformFallback(modifier, fallbackSize)
            ArtworkFallbackStyle.Note -> NoteFallback(modifier, fallbackSize)
        }
    } else {
        AsyncImage(
            model = artworkUri,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop,
            onError = { failed = true }
        )
    }
}

/**
 * Legacy fallback entry (MusicNote box). Kept for source compatibility;
 * new code should use [ArtworkImage], which defaults to the Stitch etched
 * fallback.
 */
@Composable
fun ArtworkFallback(modifier: Modifier = Modifier, iconSize: Dp = 24.dp) {
    NoteFallback(modifier, iconSize)
}
