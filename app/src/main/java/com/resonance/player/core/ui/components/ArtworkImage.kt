package com.resonance.player.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade

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
        val context = LocalContext.current
        // A short crossfade so covers fade in instead of popping (cached covers still show at once).
        val request = remember(artworkUri) {
            ImageRequest.Builder(context).data(artworkUri).crossfade(180).build()
        }
        AsyncImage(
            model = request,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop,
            onError = { failed = true }
        )
    }
}
