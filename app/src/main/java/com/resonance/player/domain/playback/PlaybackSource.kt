package com.resonance.player.domain.playback

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Where the current queue came from, so Now Playing can say "Playlist · Night drive" instead of "queue". */
data class PlaybackSource(val kind: Kind, val name: String? = null) {
    enum class Kind { LIBRARY, ALBUM, ARTIST, GENRE, FOLDER, PLAYLIST, FAVORITES, SEARCH, MOST_PLAYED, SHUFFLE_ALL }
}

/** In-memory: after a restart the queue comes back without its source (the header then shows the album). */
class PlaybackSourceStore {
    private val mutable = MutableStateFlow<PlaybackSource?>(null)
    val current: StateFlow<PlaybackSource?> = mutable.asStateFlow()

    fun set(source: PlaybackSource?) {
        mutable.value = source
    }
}
