package com.resonance.player.domain.importer

/** One link the user asked to import (a song, album or playlist). [title] = the last file fetched, once known. */
data class ImportJob(val id: Long, val url: String, val status: ImportStatus, val title: String? = null)

sealed interface ImportStatus {
    data object Queued : ImportStatus

    /** [item]/[total] are known for playlists; [title] is the file being fetched; [percent] counts while [stage] is DOWNLOADING. */
    data class Running(
        val percent: Float,
        val item: Int?,
        val total: Int?,
        val title: String?,
        val stage: ImportStage = ImportStage.PREPARING
    ) : ImportStatus

    /** [songs] = 0 means everything was already imported before. [warning] = some items failed. */
    data class Done(val songs: Int, val warning: String?) : ImportStatus

    data class Failed(val message: String) : ImportStatus

    data object Canceled : ImportStatus
}

/** What a running import is busy with, so a long wait explains itself. In the order they happen. */
enum class ImportStage { PREPARING, UPDATING, FINDING, DOWNLOADING, CONVERTING, SAVING }

val ImportStatus.isActive: Boolean get() = this is ImportStatus.Queued || this is ImportStatus.Running
