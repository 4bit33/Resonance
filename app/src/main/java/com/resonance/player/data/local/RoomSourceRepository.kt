package com.resonance.player.data.local

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import com.resonance.player.core.common.AppDispatchers
import com.resonance.player.core.database.ResonanceDatabase
import com.resonance.player.core.database.entity.SourceEntity
import com.resonance.player.core.media.AudioScanner
import com.resonance.player.core.media.uniqueName
import com.resonance.player.core.model.MusicSource
import com.resonance.player.core.model.SourceKind
import com.resonance.player.data.media.SafAudioDataSource
import com.resonance.player.domain.library.SourceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Room + Storage Access Framework implementation. The only class that takes
 * or releases persistable grants. Both writes stop a running scan first: the
 * scanner is single-flight, so a scan in progress would swallow the
 * follow-up scan request, and importing songs of a source that was just
 * removed would violate the songs -> sources foreign key.
 */
class RoomSourceRepository(
    private val resolver: ContentResolver,
    private val database: ResonanceDatabase,
    private val scanner: AudioScanner,
    private val dataSource: SafAudioDataSource,
    private val dispatchers: AppDispatchers,
    private val clockSec: () -> Long = { System.currentTimeMillis() / 1000L }
) : SourceRepository {

    override fun observeSources(): Flow<List<MusicSource>> =
        database.sourceDao().observeAll().map { rows ->
            val grants = dataSource.readGrants()
            rows.map {
                MusicSource(
                    id = it.source.id,
                    kind = SourceKind.valueOf(it.source.kind),
                    uri = it.source.uri,
                    displayName = it.source.displayName,
                    songCount = it.songCount,
                    accessOk = it.source.uri in grants
                )
            }
        }.flowOn(dispatchers.io)

    override suspend fun addSources(kind: SourceKind, uris: List<String>) {
        withContext(dispatchers.io) {
            scanner.cancel()
            val dao = database.sourceDao()
            val taken = dao.getAll().mapTo(HashSet()) { it.displayName }
            for (uri in uris) {
                try {
                    resolver.takePersistableUriPermission(Uri.parse(uri), Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (e: SecurityException) {
                    // The provider offers no lasting grant: it would be unreadable after a restart.
                    continue
                }
                val name = uniqueName(
                    dataSource.displayNameOf(uri, tree = kind == SourceKind.TREE) ?: DEFAULT_NAME,
                    taken
                )
                val source = SourceEntity(kind = kind.name, uri = uri, displayName = name, addedAtEpochSec = clockSec())
                if (dao.insert(source) != -1L) taken += name
            }
        }
    }

    override suspend fun removeSource(id: Long) {
        withContext(dispatchers.io) {
            scanner.cancel()
            val dao = database.sourceDao()
            val source = dao.getById(id) ?: return@withContext
            dao.delete(id)
            try {
                resolver.releasePersistableUriPermission(Uri.parse(source.uri), Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: SecurityException) {
                // No grant left to release (it was already lost).
            }
        }
    }

    private companion object {
        const val DEFAULT_NAME = "Music"
    }
}
