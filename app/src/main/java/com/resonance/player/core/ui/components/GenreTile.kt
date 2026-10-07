package com.resonance.player.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.resonance.player.R
import com.resonance.player.core.model.Genre
import com.resonance.player.core.ui.theme.ResonanceTheme
import com.resonance.player.core.ui.theme.rememberArtworkPalette

/** Genre tile: tinted with a cover from the genre, that cover peeking in at the corner. */
@Composable
fun GenreTile(genre: Genre, height: Dp, modifier: Modifier, onClick: () -> Unit) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    val palette = rememberArtworkPalette(genre.artUri, ResonanceTheme.look.artworkColors)
    Box(
        modifier
            .height(height)
            .clip(RoundedCornerShape(18.dp))
            .background(palette.surface)
            .pressClickable(onClickLabel = genre.name, onClick = onClick)
    ) {
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 18.dp, y = 18.dp)
                .size(height + 4.dp)
                .graphicsLayer { rotationZ = 18f }
                .clip(RoundedCornerShape(14.dp))
        ) {
            ArtworkImage(artworkUri = genre.artUri, contentDescription = null, modifier = Modifier.fillMaxSize())
        }
        Column(Modifier.padding(12.dp).fillMaxWidth(0.7f)) {
            Text(genre.name, style = typography.titleMd, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                pluralStringResource(R.plurals.music_song_count, genre.songCount, genre.songCount),
                style = typography.bodySm,
                color = colors.textSecondary
            )
        }
    }
}

