package com.resonance.player.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.resonance.player.core.database.entity.ExcludedSongEntity

@Dao
interface ExcludedSongDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(song: ExcludedSongEntity)

    @Query("SELECT id FROM excluded_songs")
    suspend fun getIds(): List<Long>
}
