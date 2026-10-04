package com.resonance.player.feature.tags

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
    /** null = not searched yet. */
    val results: List<TagCandidate>? = null,
    val applied: TagCandidate? = null,
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
    private val libraryEdits: MutableSharedFlow<Unit>
) : ViewModel() {
    private val mutable = MutableStateFlow(TagEditorState())
    val state: StateFlow<TagEditorState> = mutable.asStateFlow()

    init {
        viewModelScope.launch {
            val song = (getSong(songId) as? Result.Success)?.value ?: return@launch
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

    /** Searches with the title / artist currently in the fields (cleaned of video-site noise). */
    fun search() {
        val current = mutable.value
        val (artist, title) = searchTerms(current.tags.title, current.tags.artist)
        if (title.isBlank()) return
        mutable.update { it.copy(searching = true, error = null) }
        viewModelScope.launch {
            when (val result = lookup.search(title, artist)) {
                is Result.Success -> mutable.update {
                    it.copy(searching = false, results = rankCandidates(result.value, current.song?.durationMs ?: 0L))
                }
                is Result.Failure -> mutable.update { it.copy(searching = false, error = result.error.userMessage()) }
                Result.Loading -> Unit
            }
        }
    }

    /** Fills the fields from a match (only what the match knows) and offers its cover. */
    fun apply(candidate: TagCandidate) {
        viewModelScope.launch {
            mutable.update { it.copy(busy = true) }
            val c = lookup.details(candidate)
            mutable.update { s ->
                s.copy(
                    busy = false,
                    applied = candidate,
                    tags = s.tags.copy(
                        title = c.title,
                        artist = c.artist.ifBlank { s.tags.artist },
                        album = c.album ?: s.tags.album,
                        albumArtist = c.albumArtist ?: s.tags.albumArtist,
                        year = c.year?.toString() ?: s.tags.year,
                        trackNumber = c.trackNumber?.toString() ?: s.tags.trackNumber,
                        genre = c.genre ?: s.tags.genre
                    ),
                    coverPreview = c.coverUrl ?: s.coverPreview,
                    coverSource = c.coverUrl ?: s.coverSource
                )
            }
        }
    }

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
