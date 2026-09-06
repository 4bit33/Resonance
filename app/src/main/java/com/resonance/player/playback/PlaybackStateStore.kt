package com.resonance.player.playback

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.resonance.player.core.model.RepeatMode
import com.resonance.player.core.model.ShuffleMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.playbackStateStore: DataStore<Preferences> by preferencesDataStore(
    name = "playback_state"
)

/**
 * Small restorable playback state: queue song ids, index, position, shuffle,
 * repeat. "Remember what was playing" — never "auto-start music".
 *
 * Position is written on pause / track change / stop / queue edits plus a
 * debounced periodic save while playing — NEVER on every progress tick, and
 * never into Room.
 */
data class RestoredPlayback(
    val songIds: List<Long>,
    val index: Int,
    val positionMs: Long,
    val shuffle: ShuffleMode,
    val repeat: RepeatMode
)

/** Pure encode/decode of [RestoredPlayback] for unit tests. */
object RestoredPlaybackCodec {
    fun encode(value: RestoredPlayback): Map<String, String> = mapOf(
        "queue" to value.songIds.joinToString(","),
        "shuffle" to value.shuffle.name,
        "repeat" to value.repeat.name
    )

    fun decode(
        queue: String?,
        index: Int,
        positionMs: Long,
        shuffleName: String?,
        repeatName: String?
    ): RestoredPlayback? {
        val ids = queue
            ?.split(",")
            ?.mapNotNull { it.toLongOrNull() }
            ?.takeIf { it.isNotEmpty() }
            ?: return null
        val shuffle = runCatching { ShuffleMode.valueOf(shuffleName ?: "") }
            .getOrDefault(ShuffleMode.OFF)
        val repeat = runCatching { RepeatMode.valueOf(repeatName ?: "") }
            .getOrDefault(RepeatMode.OFF)
        return RestoredPlayback(
            songIds = ids,
            index = index.coerceIn(0, ids.size - 1),
            positionMs = positionMs.coerceAtLeast(0L),
            shuffle = shuffle,
            repeat = repeat
        )
    }
}

class PlaybackStateStore(private val context: Context) {

    suspend fun save(value: RestoredPlayback) {
        if (value.songIds.isEmpty()) {
            clear()
            return
        }
        val fields = RestoredPlaybackCodec.encode(value)
        context.playbackStateStore.edit { prefs ->
            prefs[Keys.QUEUE] = fields.getValue("queue")
            prefs[Keys.INDEX] = value.index
            prefs[Keys.POSITION] = value.positionMs
            prefs[Keys.SHUFFLE] = fields.getValue("shuffle")
            prefs[Keys.REPEAT] = fields.getValue("repeat")
        }
    }

    suspend fun load(): RestoredPlayback? {
        val prefs = context.playbackStateStore.data
            .map { it }
            .first()
        return RestoredPlaybackCodec.decode(
            queue = prefs[Keys.QUEUE],
            index = prefs[Keys.INDEX] ?: 0,
            positionMs = prefs[Keys.POSITION] ?: 0L,
            shuffleName = prefs[Keys.SHUFFLE],
            repeatName = prefs[Keys.REPEAT]
        )
    }

    suspend fun clear() {
        context.playbackStateStore.edit { it.clear() }
    }

    private object Keys {
        val QUEUE = stringPreferencesKey("queue_ids")
        val INDEX = intPreferencesKey("queue_index")
        val POSITION = longPreferencesKey("queue_position_ms")
        val SHUFFLE = stringPreferencesKey("shuffle")
        val REPEAT = stringPreferencesKey("repeat")
    }
}
