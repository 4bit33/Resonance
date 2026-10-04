package com.resonance.player.feature.tags

import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.resonance.player.R
import com.resonance.player.core.common.formatDurationMs
import com.resonance.player.core.ui.components.ArtworkImage
import com.resonance.player.core.ui.components.ResonanceTopBar
import com.resonance.player.core.ui.components.pressClickable
import com.resonance.player.core.ui.theme.ResonanceTheme
import com.resonance.player.domain.tags.TagCandidate
import kotlin.math.abs

/**
 * Fix a song's tags: edit the fields, or search MusicBrainz + Deezer with the
 * current title / artist and tap a match to fill them in (with its cover).
 * Saving keeps the fixes in the app; the file itself is not changed.
 */
@Composable
fun TagEditorScreen(viewModel: TagEditorViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    val pickCover = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.pickCover(uri.toString())
    }
    Column(Modifier.fillMaxSize()) {
        ResonanceTopBar(
            title = stringResource(R.string.tags_title),
            onBack = onBack,
            trailing = {
                TextButton(onClick = { viewModel.save(onBack) }, enabled = state.song != null && !state.busy) {
                    Text(stringResource(R.string.genre_edit_save))
                }
            }
        )
        LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item(key = "cover") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(112.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .pressClickable(onClickLabel = stringResource(R.string.tags_cover_pick)) {
                                pickCover.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                    ) {
                        val preview = state.coverPreview
                        if (preview != null && preview.startsWith("http")) {
                            AsyncImage(model = preview, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        } else {
                            ArtworkImage(artworkUri = preview ?: state.song?.artworkUri, contentDescription = null, modifier = Modifier.fillMaxSize())
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp)
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(colors.background.copy(alpha = 0.7f))
                        ) {
                            Icon(Icons.Rounded.Image, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(state.song?.title.orEmpty(), style = typography.titleMd, color = colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(state.song?.path.orEmpty(), style = typography.bodySm, color = colors.textMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(10.dp))
                        FilledTonalButton(onClick = viewModel::search, enabled = !state.searching && state.tags.title.isNotBlank()) {
                            if (state.searching) {
                                CircularProgressIndicator(modifier = Modifier.size(ButtonDefaults.IconSize), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                            }
                            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                            Text(stringResource(R.string.tags_search))
                        }
                        Spacer(Modifier.height(6.dp))
                        FilledTonalButton(onClick = viewModel::recognize, enabled = !state.recognizing && state.song != null) {
                            if (state.recognizing) {
                                CircularProgressIndicator(modifier = Modifier.size(ButtonDefaults.IconSize), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Rounded.GraphicEq, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                            }
                            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                            Text(stringResource(R.string.tags_recognize))
                        }
                    }
                }
            }
            state.appliedFrom?.let { from ->
                item(key = "applied") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.accent.copy(alpha = 0.14f))
                            .padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = colors.accent)
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.tags_applied, from), style = typography.bodyMd, color = colors.textPrimary, modifier = Modifier.weight(1f))
                        if (state.canUndo) {
                            TextButton(onClick = viewModel::undo, enabled = !state.busy) { Text(stringResource(R.string.tags_undo)) }
                        }
                    }
                }
            }
            if (state.recognitionOff || state.notRecognized) {
                item(key = "recognize-info") {
                    Text(
                        stringResource(if (state.recognitionOff) R.string.tags_recognize_off else R.string.tags_not_recognized),
                        style = typography.bodyMd,
                        color = colors.textSecondary
                    )
                }
            }
            if (state.noSafeMatch) {
                item(key = "no-safe") {
                    Text(stringResource(R.string.tags_pick_one), style = typography.bodyMd, color = colors.textSecondary)
                }
            }
            state.error?.let { error ->
                item(key = "error") { Text(error, style = typography.bodyMd, color = colors.error) }
            }
            state.results?.let { results ->
                item(key = "results-title") {
                    Text(
                        if (results.isEmpty()) stringResource(R.string.tags_no_matches) else stringResource(R.string.tags_other_matches),
                        style = typography.labelLg,
                        color = colors.accent
                    )
                }
                items(results.take(12), key = { it.source.name + it.id }) { candidate ->
                    CandidateRow(candidate, state.song?.durationMs ?: 0L, selected = candidate == state.applied) { viewModel.apply(candidate) }
                }
            }
            item(key = "fields") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Field(stringResource(R.string.tags_field_title), state.tags.title) { v -> viewModel.edit { it.copy(title = v) } }
                    Field(stringResource(R.string.tags_field_artist), state.tags.artist) { v -> viewModel.edit { it.copy(artist = v) } }
                    Field(stringResource(R.string.tags_field_album), state.tags.album) { v -> viewModel.edit { it.copy(album = v) } }
                    Field(stringResource(R.string.tags_field_album_artist), state.tags.albumArtist) { v -> viewModel.edit { it.copy(albumArtist = v) } }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Field(stringResource(R.string.tags_field_year), state.tags.year, number = true, modifier = Modifier.weight(1f)) { v ->
                            viewModel.edit { it.copy(year = v.filter(Char::isDigit).take(4)) }
                        }
                        Field(stringResource(R.string.tags_field_track), state.tags.trackNumber, number = true, modifier = Modifier.weight(1f)) { v ->
                            viewModel.edit { it.copy(trackNumber = v.filter(Char::isDigit).take(3)) }
                        }
                    }
                    Field(stringResource(R.string.genre_edit_title), state.tags.genre) { v -> viewModel.edit { it.copy(genre = v) } }
                }
            }
            item(key = "note") {
                Text(stringResource(R.string.tags_note), style = typography.bodySm, color = colors.textSecondary)
                TextButton(onClick = { viewModel.reset(onBack) }, enabled = !state.busy, contentPadding = PaddingValues(0.dp)) {
                    Text(stringResource(R.string.tags_reset), color = colors.error)
                }
            }
        }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    number: Boolean = false,
    onChange: (String) -> Unit
) {
    val colors = ResonanceTheme.colors
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = if (number) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.accent,
            unfocusedBorderColor = colors.outlineStrong,
            focusedLabelColor = colors.accent,
            cursorColor = colors.accent
        ),
        modifier = modifier
    )
}

/** A match: cover, title, artist, album · year, where it is from and how close its length is. */
@Composable
private fun CandidateRow(candidate: TagCandidate, songDurationMs: Long, selected: Boolean, onClick: () -> Unit) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) colors.accent.copy(alpha = 0.14f) else colors.surfaceContainer)
            .then(if (selected) Modifier.border(1.dp, colors.accent, RoundedCornerShape(16.dp)) else Modifier)
            .pressClickable(pressedScale = 0.98f, onClickLabel = candidate.title, onClick = onClick)
            .padding(10.dp)
    ) {
        Box(Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)).background(colors.surfaceHigh)) {
            if (candidate.thumbUrl != null) {
                AsyncImage(model = candidate.thumbUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(candidate.title, style = typography.titleMd, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(candidate.artist, style = typography.bodySm, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(candidate.album, candidate.year?.toString()).joinToString(" · "),
                style = typography.bodySm,
                color = colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                if (candidate.source == TagCandidate.Source.MUSICBRAINZ) "MusicBrainz" else "Deezer",
                style = typography.labelSm,
                color = colors.accent
            )
            candidate.durationMs?.let { ms ->
                val close = songDurationMs > 0L && abs(ms - songDurationMs) <= 3_000L
                Text(
                    formatDurationMs(ms),
                    style = typography.bodySm,
                    color = if (close) colors.textPrimary else colors.textMuted
                )
            }
        }
    }
}
