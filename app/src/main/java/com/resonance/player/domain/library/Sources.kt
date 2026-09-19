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

    /** Drops the source, its songs and its read grant. The file itself is never touched. */
    suspend fun removeSource(id: Long)
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

/** Removes a source, then rescans so files it shared with another source come back under that one. */
class RemoveSourceUseCase(
    private val sources: SourceRepository,
    private val library: MusicRepository
) {
    suspend operator fun invoke(id: Long) {
        sources.removeSource(id)
        library.scanAndImport()
    }
}
