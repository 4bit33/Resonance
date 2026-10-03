package com.resonance.player.importer

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.resonance.player.core.model.SourceKind
import com.resonance.player.data.importer.ImportDestination
import com.resonance.player.data.importer.YtDlpEngine
import com.resonance.player.domain.importer.ImportJob
import com.resonance.player.domain.importer.ImportStatus
import com.resonance.player.domain.importer.parseCurrentTitle
import com.resonance.player.domain.importer.parsePlaylistItem
import com.resonance.player.domain.library.AddSourcesUseCase
import com.resonance.player.domain.settings.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicLong

/**
 * The import queue: one download at a time, in order. Each job downloads into
 * a private staging folder, moves the files into the user's music folder,
 * then registers that folder as a library source and rescans, so the songs
 * appear in the library by themselves. [ImportService] keeps the process
 * alive (foreground notification) while anything is queued or running.
 */
class ImportManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val engine: YtDlpEngine,
    private val destination: ImportDestination,
    private val settings: UserPreferencesRepository,
    private val addSources: AddSourcesUseCase
) {
    private val jobsMutable = MutableStateFlow<List<ImportJob>>(emptyList())
    val jobs: StateFlow<List<ImportJob>> = jobsMutable.asStateFlow()

    /** Text shared to the app ("Share > Crate"), waiting for the import screen to pick it up. */
    val pendingShare = MutableStateFlow<String?>(null)

    private val nextId = AtomicLong(1)
    private val queue = Channel<Long>(Channel.UNLIMITED)
    private val archive = File(context.filesDir, "import_archive.txt")

    init {
        scope.launch { for (id in queue) run(id) }
    }

    fun enqueue(url: String) {
        val id = nextId.getAndIncrement()
        jobsMutable.update { it + ImportJob(id, url, ImportStatus.Queued) }
        queue.trySend(id)
        ContextCompat.startForegroundService(context, Intent(context, ImportService::class.java))
    }

    fun cancel(id: Long) {
        val job = jobsMutable.value.firstOrNull { it.id == id } ?: return
        when (job.status) {
            is ImportStatus.Running -> engine.cancel(processId(id))
            ImportStatus.Queued -> setStatus(id, ImportStatus.Canceled)
            else -> Unit
        }
    }

    fun clearFinished() {
        jobsMutable.update { jobs -> jobs.filter { it.status is ImportStatus.Queued || it.status is ImportStatus.Running } }
    }

    private fun setStatus(id: Long, status: ImportStatus) {
        jobsMutable.update { jobs ->
            jobs.map {
                if (it.id != id) return@map it
                it.copy(status = status, title = (status as? ImportStatus.Running)?.title ?: it.title)
            }
        }
    }

    private fun processId(id: Long) = "import-$id"

    private suspend fun run(id: Long) {
        val job = jobsMutable.value.firstOrNull { it.id == id } ?: return
        if (job.status != ImportStatus.Queued) return
        val tree = settings.importTreeUri.first()
        if (tree == null) {
            setStatus(id, ImportStatus.Failed(NO_FOLDER))
            return
        }
        var running = ImportStatus.Running(0f, null, null, null)
        setStatus(id, running)
        val staging = File(context.cacheDir, "import/$id")
        val error = engine.download(job.url, staging, archive, processId(id)) { percent, line ->
            val item = parsePlaylistItem(line)
            val title = parseCurrentTitle(line)
            val next = running.copy(
                percent = percent.coerceIn(0f, 100f),
                item = item?.first ?: running.item,
                total = item?.second ?: running.total,
                title = title ?: running.title
            )
            if (next != running) {
                running = next
                setStatus(id, next)
            }
        }
        val moved = runCatching { destination.moveAll(staging, tree) }.getOrDefault(0)
        if (moved > 0) runCatching { addSources(SourceKind.TREE, listOf(tree)) }
        setStatus(
            id,
            when {
                error == YtDlpEngine.CANCELED -> ImportStatus.Canceled
                error != null && moved == 0 -> ImportStatus.Failed(error)
                else -> ImportStatus.Done(moved, error)
            }
        )
    }

    companion object {
        /** Failure text the UI replaces with a localized "pick a folder first". */
        const val NO_FOLDER = "no-folder"
    }
}
