package com.resonance.player.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage

/**
 * Cached-artwork image with a deterministic local fallback. Coil loads the
 * file:// URI lazily with downsampling + memory cache (never full-res, never
 * a Room BLOB); missing/corrupt art shows a plain note glyph — never an
 * error state. No network is involved — only app-cache files.
 */
@Composable
fun ArtworkImage(
    artworkUri: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    var failed by remember(artworkUri) { mutableStateOf(false) }
    if (artworkUri.isNullOrBlank() || failed) {
        NoteFallback(modifier)
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
