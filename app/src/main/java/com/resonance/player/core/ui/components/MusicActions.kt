package com.resonance.player.core.ui.components

import androidx.compose.runtime.staticCompositionLocalOf
import com.resonance.player.core.model.Song

/**
 * How screens ask for music to be added or removed. The system pickers live
 * once in the app shell (one launcher, one result handler); screens only
 * trigger them. [addFolder] takes an optional folder URI to open the picker
 * at, used to re-add a folder whose access was lost. [removeSong] takes one
 * song out of the library (never deletes the file). [editGenre] opens the
 * genre dialog for some songs; [onSaved] gets the new genre (null = cleared).
 */
class MusicActions(
    val addFolder: (initialUri: String?) -> Unit,
    val addSongs: () -> Unit,
    val removeSong: (Song) -> Unit,
    val editGenre: (songs: List<Song>, onSaved: (String?) -> Unit) -> Unit = { _, _ -> }
)

val LocalMusicActions = staticCompositionLocalOf {
    MusicActions(addFolder = {}, addSongs = {}, removeSong = {})
}
