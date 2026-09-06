package com.resonance.player.data.media

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.resonance.player.core.common.AppDispatchers
import com.resonance.player.core.common.AppError
import com.resonance.player.core.common.Result
import com.resonance.player.core.database.ResonanceDatabase
import com.resonance.player.core.database.dao.ScanFingerprint
import com.resonance.player.core.database.entity.SongEntity
import com.resonance.player.core.media.AudioScanner
import com.resonance.player.core.media.MediaItemCandidate
import com.resonance.player.core.media.ScanReport
import com.resonance.player.core.media.ScanState
import com.resonance.player.core.media.SongMetadata
import com.resonance.player.core.permissions.MusicPermissions
import com.resonance.player.data.local.LibraryPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit

/**
 * Real incremental MediaStore scanner (single-flight, cancellable).
 *
 * Flow: permission -> count -> stream candidates -> diff against stored
 * fingerprints -> bounded metadata/artwork extraction for new/changed only
 * -> batched Room upserts (stats preserved) -> chunked deletes -> artwork
 * prune -> report. Corrupt/inaccessible single files increment [failed] and
 * never abort the run. Startup behavior: fast cached UI first, this runs in
 * the background on app scope (see ResonanceApp).
 */
class MediaStoreLibraryScanner(
    private val appContext: Context,
    private val dispatchers: AppDispatchers,
    private val scope: CoroutineScope,
    private val database: ResonanceDatabase,
    private val dataSource: MediaStoreAudioDataSource,
    private val extractor: AndroidMetadataExtractor,
    private val artworkExtractor: ArtworkExtractor,
    private val artworkStore: ArtworkStore,
    private val prefs: LibraryPreferences,
    private val clockSec: () -> Long = { System.currentTimeMillis() / 1000L }
) : AudioScanner {

    private val mutable = MutableStateFlow<ScanState>(ScanState.Idle)
    override val state: StateFlow<ScanState> = mutable.asStateFlow()

    private val mutex = Mutex()
    private var current: Job? = null

    override suspend fun scanLibrary(): Result<ScanReport> {
        val job = mutex.withLock {
            val running = current?.takeIf { it.isActive }
            if (running != null) {
                return@withLock running
            }
            scope.launchScan().also { current = it }
        }
        return try {
            job.join()
            lastResult ?: Result.Failure(AppError.ScanFailed("scan produced no result"))
        } catch (e: CancellationException) {
            throw e
        }
    }

    override fun cancel() {
        current?.cancel()
    }

    @Volatile
    private var lastResult: Result<ScanReport>? = null

    private fun CoroutineScope.launchScan(): Job = launch(dispatchers.io) {
        val startedMs = android.os.SystemClock.elapsedRealtime()
        mutable.value = ScanState.CheckingPermission
        try {
            if (!hasAudioPermission()) {
                mutable.value = ScanState.PermissionRequired
                lastResult = Result.Failure(AppError.PermissionDenied)
                return@launch
            }
            val total = try {
                dataSource.count()
            } catch (e: SecurityException) {
                mutable.value = ScanState.PermissionRequired
                lastResult = Result.Failure(AppError.PermissionDenied)
                return@launch
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                mutable.value = ScanState.Failed(AppError.MediaStoreUnavailable(e.message))
                lastResult = Result.Failure(AppError.MediaStoreUnavailable(e.message))
                return@launch
            }
            mutable.value = ScanState.Scanning(0, total, 0, 0)

            val stored = try {
                database.songDao().getFingerprints()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                mutable.value = ScanState.Failed(AppError.DatabaseError(e.message))
                lastResult = Result.Failure(AppError.DatabaseError(e.message))
                return@launch
            }
            val storedByKey = stored.associateBy { it.mediaStoreId to it.volumeName }

            val seen = LinkedHashMap<Pair<Long, String>, MediaItemCandidate>(total.coerceAtLeast(16))
            var skipped = 0
            try {
                dataSource.streamCandidates().collect { row ->
                    when (row) {
                        is ScanRow.Row -> {
                            val c = row.candidate
                            seen[c.mediaStoreId to c.volumeName] = c
                        }
                        is ScanRow.Skipped -> skipped++
                    }
                    if ((seen.size + skipped) % PROGRESS_EVERY == 0) {
                        val s = mutable.value
                        if (s is ScanState.Scanning) {
                            mutable.value = s.copy(processed = seen.size + skipped)
                        }
                    }
                }
            } catch (e: SecurityException) {
                mutable.value = ScanState.PermissionRequired
                lastResult = Result.Failure(AppError.PermissionDenied)
                return@launch
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                mutable.value = ScanState.Failed(AppError.MediaStoreUnavailable(e.message))
                lastResult = Result.Failure(AppError.MediaStoreUnavailable(e.message))
                return@launch
            }

            val basePlan = Reconciler.plan(
                stored.map {
                    StoredFingerprint(
                        it.id, it.mediaStoreId, it.volumeName,
                        it.dateModifiedEpochSec, it.fileSizeBytes,
                        it.playCount, it.lastPlayedEpochSec
                    )
                },
                seen
            )
            // Unchanged rows whose cached artwork file vanished are
            // re-imported so the image can be re-extracted (cheap exists
            // checks; extraction itself stays bounded with the rest).
            val artMissing = stored.mapNotNull { fingerprint ->
                val key = fingerprint.artworkKey ?: return@mapNotNull null
                if (artworkStore.uriFor(key) != null) return@mapNotNull null
                seen[fingerprint.mediaStoreId to fingerprint.volumeName]
            }
            val plan = if (artMissing.isEmpty()) {
                basePlan
            } else {
                val merged = (basePlan.toImport + artMissing)
                    .distinctBy { it.mediaStoreId to it.volumeName }
                basePlan.copy(toImport = merged)
            }

            var removed = 0
            if (plan.toDelete.isNotEmpty()) {
                try {
                    plan.toDelete.chunked(DB_CHUNK).forEach { chunk ->
                        database.songDao().deleteByIds(chunk)
                        removed += chunk.size
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    mutable.value = ScanState.Failed(AppError.DatabaseError(e.message))
                    lastResult = Result.Failure(AppError.DatabaseError(e.message))
                    return@launch
                }
            }

            var added = 0
            var updated = 0
            var failed = skipped
            var imported = 0
            val storedKeys = storedByKey.keys
            val nowSec = clockSec()
            // Total covers discovery (seen.size) plus the import backlog so
            // progress keeps advancing through metadata extraction.
            val workTotal = seen.size + plan.toImport.size
            val semaphore = Semaphore(EXTRACTION_PARALLELISM)
            try {
                plan.toImport.chunked(DB_BATCH).forEach { batch ->
                    val entities = batch.map { candidate ->
                        async {
                            semaphore.withPermit { importCandidate(candidate, storedByKey, nowSec) }
                        }
                    }.awaitAll()
                    val ready = entities.filterNotNull()
                    failed += entities.size - ready.size
                    if (ready.isNotEmpty()) {
                        database.songDao().upsertBatch(ready)
                        var batchAdded = 0
                        for (entity in ready) {
                            if (storedKeys.contains(entity.mediaStoreId to entity.volumeName)) {
                                updated++
                            } else {
                                batchAdded++
                            }
                        }
                        added += batchAdded
                    }
                    imported += batch.size
                    mutable.value = ScanState.Scanning(seen.size + imported, workTotal, added, updated)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                mutable.value = ScanState.Failed(AppError.DatabaseError(e.message))
                lastResult = Result.Failure(AppError.DatabaseError(e.message))
                return@launch
            }

            try {
                val referenced = database.songDao().getReferencedArtworkKeys().toSet()
                val pruned = artworkStore.prune(referenced)
                if (pruned > 0) Log.i(TAG, "Pruned $pruned orphaned artwork files")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w(TAG, "Artwork prune failed", e)
            }

            try {
                prefs.setLastScan(nowSec)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w(TAG, "Could not persist last scan time", e)
            }

            val report = ScanReport(added, updated, removed, failed, seen.size)
            val elapsed = android.os.SystemClock.elapsedRealtime() - startedMs
            Log.i(
                TAG,
                "Scan done in ${elapsed}ms: total=${seen.size} added=$added " +
                    "updated=$updated removed=$removed failed=$failed"
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
        candidate: MediaItemCandidate,
        storedByKey: Map<Pair<Long, String>, ScanFingerprint>,
        nowSec: Long
    ): SongEntity? {
        return try {
            val extracted: SongMetadata? = when (val result = extractor.extract(candidate.contentUri)) {
                is Result.Success -> result.value
                is Result.Failure -> {
                    if (result.error is AppError.MissingFile ||
                        result.error is AppError.PermissionDenied
                    ) {
                        return null
                    }
                    null
                }
                Result.Loading -> null
            }
            val normalized = MetadataNormalizer.normalize(candidate, extracted)
            val artRef = try {
                val bytes = artworkExtractor.extractArtwork(candidate.contentUri)
                if (bytes != null) artworkStore.store(bytes) else null
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w(TAG, "Artwork failed for ${candidate.contentUri}", e)
                null
            }
            val existing = storedByKey[candidate.mediaStoreId to candidate.volumeName]
            SongEntity(
                id = candidate.mediaStoreId,
                mediaStoreId = candidate.mediaStoreId,
                volumeName = candidate.volumeName,
                title = normalized.title,
                artistName = normalized.artistName,
                albumName = normalized.albumName,
                albumArtist = normalized.albumArtist,
                albumId = null,
                artistId = null,
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
                dateAddedEpochSec = candidate.dateAddedSec,
                dateModifiedEpochSec = candidate.dateModifiedSec,
                lastScannedAtSec = nowSec,
                artworkKey = artRef?.key,
                artworkUri = artRef?.uri,
                playCount = existing?.playCount ?: 0L,
                lastPlayedEpochSec = existing?.lastPlayedEpochSec,
                bpm = null,
                musicalKey = null
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Import failed for ${candidate.contentUri}", e)
            null
        }
    }

    private fun hasAudioPermission(): Boolean {
        val permission = MusicPermissions.audioPermissionForSdk(
            android.os.Build.VERSION.SDK_INT
        )
        return ContextCompat.checkSelfPermission(appContext, permission) ==
            PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val TAG = "LibraryScanner"
        private const val DB_BATCH = 100
        private const val DB_CHUNK = 500
        private const val PROGRESS_EVERY = 200
        private const val EXTRACTION_PARALLELISM = 4
    }
}
