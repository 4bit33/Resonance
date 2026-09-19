package com.resonance.player.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One thing the user added to the library: a folder ("TREE", listed
 * recursively) or a single song ("FILE"). [uri] is the persisted Storage
 * Access Framework URI, unique so re-adding is idempotent. Songs point back
 * through `songs.sourceId` with ON DELETE CASCADE: removing a source drops
 * its songs.
 */
@Entity(tableName = "sources", indices = [Index(value = ["uri"], unique = true)])
data class SourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val kind: String,
    val uri: String,
    val displayName: String,
    val addedAtEpochSec: Long,
    val lastScannedAtEpochSec: Long? = null
)
