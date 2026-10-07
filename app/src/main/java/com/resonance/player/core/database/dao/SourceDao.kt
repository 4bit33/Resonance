package com.resonance.player.core.database.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.resonance.player.core.database.entity.SourceEntity
import kotlinx.coroutines.flow.Flow

/** A source plus how many songs currently belong to it (Settings row). */
data class SourceWithCount(
    @Embedded val source: SourceEntity,
    val songCount: Int
)

@Dao
interface SourceDao {
    @Query(
        "SELECT s.*, (SELECT COUNT(*) FROM songs WHERE sourceId = s.id) AS songCount " +
            "FROM sources s ORDER BY s.id"
    )
    fun observeAll(): Flow<List<SourceWithCount>>

    @Query("SELECT * FROM sources ORDER BY id")
    suspend fun getAll(): List<SourceEntity>

    @Query("SELECT * FROM sources WHERE id = :id")
    suspend fun getById(id: Long): SourceEntity?

    /**
     * IGNORE, never REPLACE: REPLACE deletes the parent row first and the FK
     * cascade would wipe every song of the source. Returns -1 when the uri
     * already exists.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(source: SourceEntity): Long

    @Query("DELETE FROM sources WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE sources SET lastScannedAtEpochSec = :sec WHERE id IN (:ids)")
    suspend fun markScanned(ids: List<Long>, sec: Long)
}
