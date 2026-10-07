package com.resonance.player.domain.library

/** Something in the library that opens as a list of songs: an album, artist, genre or folder. */
sealed interface CollectionRef {
    data class Album(val name: String, val albumArtist: String?) : CollectionRef
    data class Artist(val name: String) : CollectionRef
    data class Genre(val name: String) : CollectionRef
    /** [path] mirrors [com.resonance.player.core.model.MusicFolder.path]: "" is the root folder. */
    data class Folder(val path: String, val name: String) : CollectionRef
}
