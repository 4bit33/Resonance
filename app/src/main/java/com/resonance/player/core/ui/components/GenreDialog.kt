package com.resonance.player.core.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.resonance.player.R
import com.resonance.player.core.model.Genre
import com.resonance.player.core.ui.theme.ResonanceTheme

/**
 * Set the genre of one or more songs. Existing genres are offered as chips
 * (filtered by what is typed) so the library does not end up with "Rock",
 * "rock" and "Рок" side by side. Saving a blank field clears the genre.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun GenreDialog(
    songCount: Int,
    current: String?,
    genres: List<Genre>,
    onSave: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    var text by remember { mutableStateOf(current.orEmpty()) }
    val query = text.trim()
    // Until the user types something new, offer every genre (the field starts with the current one).
    val filtering = query.isNotEmpty() && !query.equals(current?.trim(), ignoreCase = true)
    val suggestions = genres
        .filter { !filtering || it.name.contains(query, ignoreCase = true) }
        .filter { !it.name.equals(query, ignoreCase = false) }
        .sortedByDescending { it.songCount }
        .take(16)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceHigh,
        title = { Text(stringResource(R.string.genre_edit_title), style = typography.headlineMd, color = colors.textPrimary) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    pluralStringResource(R.plurals.music_song_count, songCount, songCount),
                    style = typography.bodySm,
                    color = colors.textSecondary
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.genre_edit_hint)) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onSave(query) }),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.outlineStrong,
                        cursorColor = colors.accent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (suggestions.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 220.dp)
                    ) {
                        suggestions.forEach { genre ->
                            ResonanceChip(label = genre.name, onClick = { text = genre.name }, count = genre.songCount.toString())
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(query) }) { Text(stringResource(R.string.genre_edit_save), color = colors.accent) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_dismiss), color = colors.textSecondary) }
        }
    )
}
