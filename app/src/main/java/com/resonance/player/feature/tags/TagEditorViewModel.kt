package com.resonance.player.feature.tags

import com.resonance.player.data.fingerprint.AudioFingerprinter
import com.resonance.player.core.common.AppError
import com.resonance.player.BuildConfig
import android.util.Log
import android.net.Uri
import com.resonance.player.domain.tags.merge
import com.resonance.player.domain.tags.isPlausible
import com.resonance.player.domain.tags.TagSnapshot
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resonance.player.core.common.Result
import com.resonance.player.core.common.userMessage
import com.resonance.player.core.model.Song
import com.resonance.player.domain.library.GetSongUseCase
import com.resonance.player.domain.library.RescanLibraryUseCase
import com.resonance.player.domain.tags.MetadataLookup
import com.resonance.player.domain.tags.SongTags
import com.resonance.player.domain.tags.TagCandidate
import com.resonance.player.domain.tags.TagRepository
import com.resonance.player.domain.tags.rankCandidates
import com.resonance.player.domain.tags.searchTerms
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TagEditorState(
    val song: Song? = null,
    val tags: SongTags = SongTags("", "", "", "", "", "", ""),
    /** What the cover box shows: a picked picture or a chosen match's cover, else the song's own. */
    val coverPreview: String? = null,
    /** The picture to save as the cover, or null to keep the current one. */
    val coverSource: String? = null,
    val searching: Boolean = false,
    /** Fingerprinting + AcoustID in progress. */
    val recognizing: Boolean = false,
    /** AcoustID knew nothing about this recording. */
    val notRecognized: Boolean = false,
    /** This build has no AcoustID key, so recognising by sound is off. */
    val recognitionOff: Boolean = false,
    /** null = not searched yet. */
    val results: List<TagCandidate>? = null,
    val applied: TagCandidate? = null,
    /** Which sources the applied tags came from, e.g. "MusicBrainz + Deezer"; set once applied and saved. */
    val appliedFrom: String? = null,
    /** The search found nothing safe to apply by itself; the user picks from the list. */
    val noSafeMatch: Boolean = false,
    /** An automatic / one-tap fill can be undone. */
    val canUndo: Boolean = false,
    val busy: Boolean = false,
    val error: String? = null
)

/** Edit one song's tags by hand, or fill them from an online match, then save as overrides. */
class TagEditorViewModel(
    private val songId: Long,
    private val getSong: GetSongUseCase,
    private val lookup: MetadataLookup,
    private val repository: TagRepository,
    private val rescan: RescanLibraryUseCase,
    private val libraryEdits: MutableSharedFlow<Unit>,
    private val fingerprinter: AudioFingerprinter
) : ViewModel() {
    private val mutable = MutableStateFlow(TagEditorState())
    val state: StateFlow<TagEditorState> = mutable.asStateFlow()

    /** The fixes before the first automatic / one-tap change on this screen, for Undo. */
    private var undoSnapshot: TagSnapshot? = null

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        run {
            val song = (getSong(songId) as? Result.Success)?.value ?: return
            mutable.update {
                it.copy(
                    song = song,
                    tags = SongTags(
                        title = song.title,
                        artist = song.artistName,
                        album = song.albumName,
                        albumArtist = song.albumArtist.orEmpty(),
                        year = song.year?.toString().orEmpty(),
                        trackNumber = song.trackNumber?.toString().orEmpty(),
                        genre = song.genreName.orEmpty()
                    )
                )
            }
        }
    }

    fun edit(transform: (SongTags) -> SongTags) {
        mutable.update { it.copy(tags = transform(it.tags), error = null) }
    }

    fun pickCover(uri: String) {
        mutable.update { it.copy(coverPreview = uri, coverSource = uri) }
    }

    /**
     * Searches both sources with the current title / artist, then applies and
     * saves at once the best plausible match of each source merged together
     * (the richer one leads, the other fills its gaps, Deezer's big cover
     * wins). With nothing plausible, the list is shown to pick from.
     */
    fun search() {
        val current = mutable.value
        val (artist, title) = searchTerms(current.tags.title, current.tags.artist)
        if (title.isBlank()) return
        mutable.update { it.copy(searching = true, error = null, noSafeMatch = false) }
        viewModelScope.launch {
            when (val result = lookup.search(title, artist)) {
                is Result.Success -> {
                    val durationMs = current.song?.durationMs ?: 0L
                    val ranked = rankCandidates(result.value, durationMs)
                    mutable.update { it.copy(searching = false, results = ranked) }
                    val plausible = ranked.filter { isPlausible(it, title, durationMs) }
                    val mb = plausible.firstOrNull { it.source == TagCandidate.Source.MUSICBRAINZ }
                    val dz = plausible.firstOrNull { it.source == TagCandidate.Source.DEEZER }?.let { lookup.details(it) }
                    val merged = merge(mb, dz)
                    if (merged == null) {
                        mutable.update { it.copy(noSafeMatch = ranked.isNotEmpty()) }
                    } else {
                        applyAndSave(merged, listOfNotNull(mb, dz).joinToString(" + ") { sourceName(it.source) }, highlight = mb ?: dz)
                    }
                }
                is Result.Failure -> mutable.update { it.copy(searching = false, error = result.error.userMessage()) }
                Result.Loading -> Unit
            }
        }
    }

    /**
     * Recognises the song by its sound (Chromaprint + AcoustID), for files whose
     * tags say nothing useful. The recognised MusicBrainz recording leads; a
     * Deezer match for it fills gaps and brings the big cover. Saved at once.
     */
    fun recognize() {
        val song = mutable.value.song ?: return
        mutable.update { it.copy(recognizing = true, error = null, notRecognized = false, noSafeMatch = false) }
        viewModelScope.launch {
            val print = fingerprinter.fingerprint(Uri.parse(song.contentUri)).getOrElse { e ->
                mutable.update { it.copy(recognizing = false, error = e.message) }
                return@launch
            }
            if (BuildConfig.DEBUG) Log.d("CrateFingerprint", "${song.title}: ${print.durationSec}s ${print.fingerprint}")
            when (val found = lookup.identify(print.fingerprint, print.durationSec)) {
                is Result.Failure -> mutable.update {
                    if (found.error is AppError.FeatureUnavailable) it.copy(recognizing = false, recognitionOff = true)
                    else it.copy(recognizing = false, error = found.error.userMessage())
                }
                is Result.Success -> {
                    val best = found.value.firstOrNull()
                    if (best == null) {
                        mutable.update { it.copy(recognizing = false, notRecognized = true) }
                        return@launch
                    }
                    val durationMs = song.durationMs
                    val more = (lookup.search(best.title, best.artist) as? Result.Success)?.value.orEmpty()
                    val dz = rankCandidates(more, durationMs)
                        .firstOrNull { it.source == TagCandidate.Source.DEEZER && isPlausible(it, best.title, durationMs) }
                        ?.let { lookup.details(it) }
                    mutable.update { it.copy(recognizing = false, results = found.value + rankCandidates(more.filter { c -> c.id != best.id }, durationMs)) }
                    applyAndSave(merge(best, dz)!!, listOfNotNull("AcoustID", dz?.let { "Deezer" }).joinToString(" + "), highlight = best)
                }
                Result.Loading -> Unit
            }
        }
    }

    /** Uses one match from the list: fills the fields and saves straight away (Undo is offered). */
    fun apply(candidate: TagCandidate) {
        viewModelScope.launch {
            mutable.update { it.copy(busy = true) }
            applyAndSave(lookup.details(candidate), sourceName(candidate.source), highlight = candidate)
        }
    }

    private suspend fun applyAndSave(c: TagCandidate, from: String, highlight: TagCandidate?) {
        if (undoSnapshot == null) undoSnapshot = repository.snapshot(songId)
        val s = mutable.value
        val tags = s.tags.copy(
            title = c.title,
            artist = c.artist.ifBlank { s.tags.artist },
            album = c.album ?: s.tags.album,
            albumArtist = c.albumArtist ?: s.tags.albumArtist,
            year = c.year?.toString() ?: s.tags.year,
            trackNumber = c.trackNumber?.toString() ?: s.tags.trackNumber,
            genre = c.genre ?: s.tags.genre
        )
        mutable.update { it.copy(busy = true, tags = tags, applied = highlight, coverPreview = c.coverUrl ?: it.coverPreview) }
        when (val result = repository.save(songId, tags, c.coverUrl)) {
            is Result.Success -> {
                libraryEdits.tryEmit(Unit)
                mutable.update { it.copy(busy = false, appliedFrom = from, canUndo = true, coverSource = null) }
            }
            is Result.Failure -> mutable.update { it.copy(busy = false, error = result.error.userMessage()) }
            Result.Loading -> Unit
        }
    }

    /** Back to how the song was before the automatic / one-tap fill on this screen. */
    fun undo() {
        val snapshot = undoSnapshot ?: return
        mutable.update { it.copy(busy = true) }
        viewModelScope.launch {
            repository.restore(songId, snapshot)
            rescan()
            libraryEdits.tryEmit(Unit)
            undoSnapshot = null
            mutable.update { it.copy(busy = false, canUndo = false, appliedFrom = null, applied = null, coverPreview = null, coverSource = null) }
            load()
        }
    }

    private fun sourceName(source: TagCandidate.Source): String =
        if (source == TagCandidate.Source.MUSICBRAINZ) "MusicBrainz" else "Deezer"

    fun save(onDone: () -> Unit) {
        val s = mutable.value
        if (s.song == null || s.busy) return
        mutable.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            when (val result = repository.save(songId, s.tags, s.coverSource)) {
                is Result.Success -> {
                    libraryEdits.tryEmit(Unit)
                    mutable.update { it.copy(busy = false) }
                    onDone()
                }
                is Result.Failure -> mutable.update { it.copy(busy = false, error = result.error.userMessage()) }
                Result.Loading -> Unit
            }
        }
    }

    /** Back to what the file says: drop the fixes and re-read the file. */
    fun reset(onDone: () -> Unit) {
        mutable.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            repository.reset(songId)
            rescan()
            libraryEdits.tryEmit(Unit)
            mutable.update { it.copy(busy = false) }
            onDone()
        }
    }
}
