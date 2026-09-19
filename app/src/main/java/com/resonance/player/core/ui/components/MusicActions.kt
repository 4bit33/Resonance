package com.resonance.player.core.ui.components

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * How screens ask for music to be added. The system pickers live once in the
 * app shell (one launcher, one result handler); screens only trigger them.
 * [addFolder] takes an optional folder URI to open the picker at, used to
 * re-add a folder whose access was lost.
 */
class MusicActions(
    val addFolder: (initialUri: String?) -> Unit,
    val addSongs: () -> Unit
)

val LocalMusicActions = staticCompositionLocalOf { MusicActions(addFolder = {}, addSongs = {}) }
