package com.resonance.player.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/**
 * Cached-artwork image with a deterministic local fallback. Coil loads the
 * file:// URI lazily with downsampling + memory cache (never full-res, never
 * a Room BLOB); missing/corrupt art shows the fallback box. No network is
 * involved — only app-cache files.
 */
@Composable
fun ArtworkImage(
    artworkUri: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    fallbackSize: Dp = 24.dp
) {
    var failed by remember(artworkUri) { mutableStateOf(false) }
    if (artworkUri.isNullOrBlank() || failed) {
        ArtworkFallback(modifier, fallbackSize)
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

@Composable
fun ArtworkFallback(modifier: Modifier = Modifier, iconSize: Dp = 24.dp) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Filled.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(iconSize)
        )
    }
}
