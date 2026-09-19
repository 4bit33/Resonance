package com.resonance.player.domain.library

import com.resonance.player.core.model.MusicSource
import com.resonance.player.core.model.SourceKind
import kotlinx.coroutines.flow.Flow

/**
 * The folders and single songs the user added. Android-free boundary: URIs
 * travel as strings; taking and releasing the persistable read grant is the
 * implementation's job.
 */
interface SourceRepository {
    fun observeSources(): Flow<List<MusicSource>>

    /** Idempotent: an already added uri keeps its row and just gets its grant re-taken (the repair path). */
    suspend fun addSources(kind: SourceKind, uris: List<String>)

    /** Drops the sources, their songs and their read grants. The files themselves are never touched. */
    suspend fun removeSources(ids: List<Long>)
}

class ObserveSourcesUseCase(private val repository: SourceRepository) {
    operator fun invoke(): Flow<List<MusicSource>> = repository.observeSources()
}

/** Adds folders/songs, then imports them right away (nothing else scans automatically). */
class AddSourcesUseCase(
    private val sources: SourceRepository,
    private val library: MusicRepository
) {
    suspend operator fun invoke(kind: SourceKind, uris: List<String>) {
        if (uris.isEmpty()) return
        sources.addSources(kind, uris)
        library.scanAndImport()
    }
}

/** Removes sources, then rescans so files they shared with another source come back under that one. */
class RemoveSourcesUseCase(
    private val sources: SourceRepository,
    private val library: MusicRepository
) {
    suspend operator fun invoke(ids: List<Long>) {
        if (ids.isEmpty()) return
        sources.removeSources(ids)
        library.scanAndImport()
    }
}
