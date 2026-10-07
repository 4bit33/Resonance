package com.resonance.player.data.media

import android.os.SystemClock
import android.util.Log
import com.resonance.player.core.common.AppDispatchers
import com.resonance.player.core.common.AppError
import com.resonance.player.core.common.Result
import com.resonance.player.core.database.ResonanceDatabase
import com.resonance.player.core.database.dao.ScanFingerprint
import com.resonance.player.core.database.entity.SongEntity
import com.resonance.player.core.media.AudioCandidate
import com.resonance.player.core.media.AudioScanner
import com.resonance.player.core.media.MediaMetadataExtractor
import com.resonance.player.core.media.ScanReport
import com.resonance.player.core.media.ScanState
import com.resonance.player.core.model.SourceKind
import com.resonance.player.data.local.LibraryPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit

/**
 * Incremental scanner over the user-added sources (single-flight,
 * cancellable). Runs only when asked: right after a source is added or
 * removed, and from the "Refresh library" actions.
 *
 * Flow: list every source with a live grant -> diff against stored
 * fingerprints ([SourceReconciler]) -> chunked deletes -> bounded metadata and
 * artwork extraction for new/changed files only -> batched Room upserts (play
 * stats and first-seen date preserved) -> artwork prune -> report. A corrupt
 * or unreadable file counts as [ScanReport.failed] and never aborts the run;
 * a source that is unavailable or only partly listed never deletes its songs.
 */
class SafLibraryScanner(
    private val dispatchers: AppDispatchers,
    private val scope: CoroutineScope,
    private val database: ResonanceDatabase,
    private val dataSource: SafAudioDataSource,
    private val extractor: MediaMetadataExtractor,
    private val artworkExtractor: ArtworkExtractor,
    private val artworkStore: ArtworkStore,
    private val prefs: LibraryPreferences,
    private val clockSec: () -> Long = { System.currentTimeMillis() / 1000L }
) : AudioScanner {

    private val mutable = MutableStateFlow<ScanState>(ScanState.Idle)
    override val state: StateFlow<ScanState> = mutable.asStateFlow()

    private val mutex = Mutex()
    private var current: Job? = null

    @Volatile
    private var lastResult: Result<ScanReport>? = null

    override suspend fun scanLibrary(): Result<ScanReport> {
        val job = mutex.withLock {
            current?.takeIf { it.isActive } ?: scope.launchScan().also { current = it }
        }
        job.join()
        return lastResult ?: Result.Failure(AppError.ScanFailed("scan produced no result"))
    }

    override fun cancel() {
        current?.cancel()
    }

    private sealed interface Import {
        data class Ok(val entity: SongEntity) : Import
        data object Filtered : Import
        data object Failed : Import
    }

    private fun CoroutineScope.launchScan(): Job = launch(dispatchers.io) {
        val startedMs = SystemClock.elapsedRealtime()
        mutable.value = ScanState.Scanning(0, 0, 0, 0)
        try {
            val sources = database.sourceDao().getAll()
            val grants = dataSource.readGrants()

            // Sources go in id order and putIfAbsent keeps the first, so a file
            // reachable through two sources belongs to the older one.
            val seen = LinkedHashMap<Long, AudioCandidate>()
            val complete = HashSet<Long>()
            val onCandidate: (AudioCandidate) -> Unit = { c ->
                seen.putIfAbsent(c.id, c)
                if (seen.size % PROGRESS_EVERY == 0) mutable.value = ScanState.Scanning(seen.size, 0, 0, 0)
            }
            for (source in sources) {
                if (source.uri !in grants) continue
                val status = when (SourceKind.valueOf(source.kind)) {
                    SourceKind.TREE -> dataSource.walkTree(source.id, source.uri, source.displayName, onCandidate)
                    SourceKind.FILE -> dataSource.walkFile(source.id, source.uri, onCandidate)
                }
                if (status == WalkStatus.COMPLETE) complete += source.id
            }

            val stored = database.songDao().getFingerprints()
            val storedById = stored.associateBy { it.id }
            val ignoreShort = prefs.ignoreShortFiles.first()
            val plan = SourceReconciler.plan(
                stored.map {
                    StoredSong(it.id, it.sourceId, it.dateModifiedEpochSec, it.fileSizeBytes, it.durationMs)
                },
                seen,
                complete,
                ignoreShort
            )
            // Unchanged rows whose cached artwork file vanished are re-imported
            // so the image can be re-extracted (cheap exists checks).
            val artMissing = stored.mapNotNull { f ->
                f.artworkKey?.takeIf { artworkStore.uriFor(it) == null }?.let { seen[f.id] }
            }
            val toImport = (plan.toImport + artMissing).distinctBy { it.id }

            var removed = 0
            plan.toDelete.chunked(DB_CHUNK).forEach { chunk ->
                database.songDao().deleteByIds(chunk)
                removed += chunk.size
            }

            var added = 0
            var updated = 0
            var failed = 0
            var done = 0
            val nowSec = clockSec()
            val semaphore = Semaphore(EXTRACTION_PARALLELISM)
            if (toImport.isNotEmpty()) mutable.value = ScanState.Scanning(0, toImport.size, 0, 0)
            toImport.chunked(DB_BATCH).forEach { batch ->
                val results = batch.map { candidate ->
                    async { semaphore.withPermit { importCandidate(candidate, storedById[candidate.id], nowSec, ignoreShort) } }
                }.awaitAll()
                val ready = results.filterIsInstance<Import.Ok>().map { it.entity }
                failed += results.count { it is Import.Failed }
                if (ready.isNotEmpty()) {
                    database.songDao().upsertBatch(ready)
                    val fresh = ready.count { it.id !in storedById }
                    added += fresh
                    updated += ready.size - fresh
                }
                done += batch.size
                mutable.value = ScanState.Scanning(done, toImport.size, added, updated)
            }

            try {
                val pruned = artworkStore.prune(database.songDao().getReferencedArtworkKeys().toSet())
                if (pruned > 0) Log.i(TAG, "Pruned $pruned orphaned artwork files")
                if (complete.isNotEmpty()) database.sourceDao().markScanned(complete.toList(), nowSec)
                prefs.setLastScan(nowSec)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w(TAG, "Post-scan housekeeping failed", e)
            }

            val report = ScanReport(added, updated, removed, failed, seen.size)
            Log.i(
                TAG,
                "Scan done in ${SystemClock.elapsedRealtime() - startedMs}ms: files=${seen.size} " +
                    "added=$added updated=$updated removed=$removed failed=$failed"
            )
            mutable.value = ScanState.Completed(report)
            lastResult = Result.Success(report)
        } catch (e: CancellationException) {
            Log.i(TAG, "Scan cancelled")
            mutable.value = ScanState.Cancelled
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Scan failed", e)
            mutable.value = ScanState.Failed(AppError.ScanFailed(e.message))
            lastResult = Result.Failure(AppError.ScanFailed(e.message))
        } finally {
            mutex.withLock { if (current?.isCompleted != false) current = null }
        }
    }

    private suspend fun importCandidate(
        candidate: AudioCandidate,
        existing: ScanFingerprint?,
        nowSec: Long,
        ignoreShort: Boolean
    ): Import = try {
        val extracted = when (val result = extractor.extract(candidate.contentUri)) {
            is Result.Success -> result.value
            is Result.Failure -> {
                if (result.error is AppError.MissingFile || result.error is AppError.PermissionDenied) {
                    return Import.Failed
                }
                null
            }
            Result.Loading -> null
        }
        val normalized = MetadataNormalizer.normalize(candidate, extracted)
        // ponytail: a filtered short file is never stored, so it is re-extracted on every
        // refresh; persist a "filtered" marker if that ever gets slow.
        if (!SourceReconciler.passesDurationFilter(normalized.durationMs, ignoreShort)) {
            Import.Filtered
        } else {
            val artRef = try {
                artworkExtractor.extractArtwork(candidate.contentUri)?.let { artworkStore.store(it) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w(TAG, "Artwork failed for ${candidate.contentUri}", e)
                null
            }
            Import.Ok(
                SongEntity(
                    id = candidate.id,
                    sourceId = candidate.sourceId,
                    title = normalized.title,
                    artistName = normalized.artistName,
                    albumName = normalized.albumName,
                    albumArtist = normalized.albumArtist,
                    genreName = normalized.genreName,
                    trackNumber = normalized.trackNumber,
                    totalTracks = normalized.totalTracks,
                    discNumber = normalized.discNumber,
                    totalDiscs = normalized.totalDiscs,
                    year = normalized.year,
                    durationMs = normalized.durationMs,
                    path = normalized.displayPath,
                    contentUri = candidate.contentUri,
                    relativePath = candidate.relativePath,
                    mimeType = normalized.mimeType,
                    bitrate = normalized.bitrate,
                    sampleRate = normalized.sampleRate,
                    fileSizeBytes = candidate.sizeBytes,
                    // First seen: min(now, mtime), kept across re-imports.
                    dateAddedEpochSec = existing?.dateAddedEpochSec
                        ?: minOf(nowSec, candidate.dateModifiedSec.takeIf { it > 0L } ?: nowSec),
                    dateModifiedEpochSec = candidate.dateModifiedSec,
                    lastScannedAtSec = nowSec,
                    artworkKey = artRef?.key,
                    artworkUri = artRef?.uri,
                    playCount = existing?.playCount ?: 0L,
                    lastPlayedEpochSec = existing?.lastPlayedEpochSec
                )
            )
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "Import failed for ${candidate.contentUri}", e)
        Import.Failed
    }

    private companion object {
        const val TAG = "LibraryScanner"
        const val DB_BATCH = 100
        const val DB_CHUNK = 500
        const val PROGRESS_EVERY = 200
        const val EXTRACTION_PARALLELISM = 4
    }
}
