package com.resonance.player.core.media

/**
 * Canonical display fallbacks for missing tags. Centralized so Library,
 * Search, Player and notifications never render "null"/""/"undefined".
 * These are display values only — embedded metadata is never modified.
 */
object UnknownMetadata {
    const val TITLE = "Unknown Title"
    const val ARTIST = "Unknown Artist"
    const val ALBUM = "Unknown Album"
    const val GENRE = "Unknown Genre"
    const val VARIOUS_ARTISTS = "Various Artists"
}
