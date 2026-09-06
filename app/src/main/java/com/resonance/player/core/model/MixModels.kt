package com.resonance.player.core.model

/**
 * Smart Mix domain models (engine lands in a later phase).
 * Everything is computed LOCALLY from library data, tags and playback
 * history. No network, no account, no profiling leaves the device.
 */
data class MixPreset(
    val id: String,
    val name: String,
    val seedGenre: String? = null,
    val seedArtist: String? = null,
    val targetSize: Int = 25,
    val avoidRecentlyPlayed: Boolean = true,
    val preferFavorites: Boolean = true
) {
    init {
        require(id.isNotBlank()) { "MixPreset id must not be blank" }
        require(name.isNotBlank()) { "MixPreset name must not be blank" }
        require(targetSize in 1..500) { "targetSize must be within 1..500" }
    }
}

data class MixConfiguration(
    val preset: MixPreset,
    val crossfadeMs: Long = 0L,
    val allowRepeats: Boolean = false
)

data class TrackScore(
    val songId: Long,
    val score: Double,
    val reasons: List<String> = emptyList()
)

data class TransitionPlan(
    val fromSongId: Long,
    val toSongId: Long,
    val crossfadeMs: Long = 0L,
    val gapless: Boolean = true
)
