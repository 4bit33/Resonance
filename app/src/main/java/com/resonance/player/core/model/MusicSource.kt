package com.resonance.player.core.model

/** A folder is listed recursively; a file is one individually added song. */
enum class SourceKind { TREE, FILE }

/**
 * A folder or single song the user added to the library. [accessOk] is false
 * when the persisted read grant is gone (the songs are kept; re-adding the
 * same folder repairs it).
 */
data class MusicSource(
    val id: Long,
    val kind: SourceKind,
    val uri: String,
    val displayName: String,
    val songCount: Int,
    val accessOk: Boolean
)
