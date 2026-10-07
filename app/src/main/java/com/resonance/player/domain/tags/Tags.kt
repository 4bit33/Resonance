package com.resonance.player.domain.tags

import com.resonance.player.core.common.Result

/** The editable tags of one song. Blank text = "no value". */
data class SongTags(
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String,
    val year: String,
    val trackNumber: String,
    val genre: String
)

/** One match from an online database. [coverUrl] is a full-size picture; [thumbUrl] a small one for lists. */
data class TagCandidate(
    val source: Source,
    val id: String,
    val title: String,
    val artist: String,
    val album: String?,
    val albumArtist: String?,
    val year: Int?,
    val trackNumber: Int?,
    val genre: String?,
    val durationMs: Long?,
    val coverUrl: String?,
    val thumbUrl: String?
) {
    enum class Source { MUSICBRAINZ, DEEZER }
}

/** Online lookup (network only when the user asks, ADR-013). */
interface MetadataLookup {
    suspend fun search(title: String, artist: String?): Result<List<TagCandidate>>

    /** Fills what a search result leaves out (e.g. year / track / genre for Deezer). */
    suspend fun details(candidate: TagCandidate): TagCandidate

    /**
     * Recognises a song by its sound: an AcoustID lookup of a Chromaprint
     * [fingerprint], then the matching MusicBrainz recordings, best first.
     */
    suspend fun identify(fingerprint: String, durationSec: Int): Result<List<TagCandidate>>
}

/** Where fixed tags are kept (overrides laid over the file's tags; files are not rewritten). */
interface TagRepository {
    /** Saves [tags] for the song; [coverSource] is a picture to use as the cover (http URL or content/file URI), or null to keep it. */
    suspend fun save(songId: Long, tags: SongTags, coverSource: String?): Result<Unit>

    /** Drops every fix for the song and re-reads its file. */
    suspend fun reset(songId: Long): Result<Unit>

    /** The song's fixes right now (to undo a change later). */
    suspend fun snapshot(songId: Long): TagSnapshot

    /** Puts the fixes back as they were in [snapshot]; the caller rescans so dropped fixes show the file's tags again. */
    suspend fun restore(songId: Long, snapshot: TagSnapshot): Result<Unit>
}

private val noise = listOf(
    """\((official\s*)?(music\s*)?(video|audio|visuali[sz]er|lyric(s)?(\s*video)?)\)""",
    """\[(official\s*)?(music\s*)?(video|audio|visuali[sz]er|lyric(s)?(\s*video)?|hd|hq|4k)\]""",
    """\((hd|hq|4k|remastered(\s*\d{4})?)\)""",
    """\s+-\s+topic$"""
).map { Regex(it, RegexOption.IGNORE_CASE) }

/** Strips video-site noise from a title ("Song (Official Video) [HD]" -> "Song"). Pure. */
fun cleanTitle(raw: String): String {
    var s = raw
    noise.forEach { s = it.replace(s, "") }
    return s.replace(Regex("""\s{2,}"""), " ").trim().trim('-', '|', ' ')
}

/**
 * Best guess of (artist, title) to search with. A file-like title such as
 * "Artist - Song (Official Video)" wins over an unknown artist tag. Pure.
 */
fun searchTerms(title: String, artist: String?): Pair<String?, String> {
    val cleaned = cleanTitle(title)
    val unknownArtist = artist.isNullOrBlank() || artist.equals("unknown", true) || artist.startsWith("<unknown")
    val dash = Regex("""^(.+?)\s+[-–—]\s+(.+)$""").find(cleaned)
    if (dash != null && unknownArtist) {
        return dash.groupValues[1].trim() to dash.groupValues[2].trim()
    }
    return (if (unknownArtist) null else artist?.trim()) to cleaned
}

/**
 * Orders matches: duration close to the song's first (a different edit is
 * usually a different length), then MusicBrainz before Deezer on a tie. Pure.
 */
fun rankCandidates(candidates: List<TagCandidate>, songDurationMs: Long): List<TagCandidate> =
    candidates.sortedWith(
        compareBy<TagCandidate> { c ->
            val d = c.durationMs ?: return@compareBy Long.MAX_VALUE / 2
            kotlin.math.abs(d - songDurationMs) / 5_000L
        }.thenBy { it.source.ordinal }
    )

/** "2019-05-01" / "2019" -> 2019. Pure. */
fun yearOf(date: String?): Int? = date?.take(4)?.toIntOrNull()?.takeIf { it in 1000..2999 }

/** How much a match knows beyond title and artist (album, year, track, genre, cover). Pure. */
fun infoScore(c: TagCandidate): Int =
    listOf(c.album, c.albumArtist, c.year, c.trackNumber, c.genre, c.coverUrl).count { it != null }

private fun normalize(s: String): String =
    cleanTitle(s).lowercase().replace(Regex("""[^\p{L}\p{N}]+"""), " ").trim()

/**
 * Whether a match is safe to apply without asking: the titles agree (one
 * contains the other once cleaned) and, when both lengths are known, they
 * differ by at most [toleranceMs]. Pure.
 */
fun isPlausible(c: TagCandidate, songTitle: String, songDurationMs: Long, toleranceMs: Long = 4_000L): Boolean {
    val a = normalize(c.title)
    val b = normalize(songTitle)
    if (a.isEmpty() || b.isEmpty()) return false
    val titlesAgree = a == b || a.contains(b) || b.contains(a)
    val d = c.durationMs
    val lengthsAgree = d == null || songDurationMs <= 0L || kotlin.math.abs(d - songDurationMs) <= toleranceMs
    return titlesAgree && lengthsAgree
}

/**
 * The automatic pick: the best plausible match of each source, then the
 * one knowing more leads and the other fills its gaps. The bigger cover
 * (Deezer's 1000 px) is preferred. Null when nothing is plausible. Pure.
 */
fun bestMerged(candidates: List<TagCandidate>, songTitle: String, songDurationMs: Long): TagCandidate? {
    val ranked = rankCandidates(candidates.filter { isPlausible(it, songTitle, songDurationMs) }, songDurationMs)
    val mb = ranked.firstOrNull { it.source == TagCandidate.Source.MUSICBRAINZ }
    val dz = ranked.firstOrNull { it.source == TagCandidate.Source.DEEZER }
    return merge(mb, dz)
}

/** [a] and [b] merged: the richer leads, the other fills what it lacks; Deezer's cover wins. Pure. */
fun merge(a: TagCandidate?, b: TagCandidate?): TagCandidate? {
    if (a == null) return b
    if (b == null) return a
    val (lead, other) = if (infoScore(a) >= infoScore(b)) a to b else b to a
    val deezerCover = listOf(a, b).firstOrNull { it.source == TagCandidate.Source.DEEZER }?.coverUrl
    return lead.copy(
        album = lead.album ?: other.album,
        albumArtist = lead.albumArtist ?: other.albumArtist,
        year = lead.year ?: other.year,
        trackNumber = lead.trackNumber ?: other.trackNumber,
        genre = lead.genre ?: other.genre,
        durationMs = lead.durationMs ?: other.durationMs,
        coverUrl = deezerCover ?: lead.coverUrl ?: other.coverUrl,
        thumbUrl = lead.thumbUrl ?: other.thumbUrl
    )
}

/** What a song's fixes looked like before a change, so an automatic fill can be undone. */
data class TagSnapshot(
    val hadOverride: Boolean,
    val title: String?,
    val artist: String?,
    val album: String?,
    val albumArtist: String?,
    val year: Int?,
    val trackNumber: Int?,
    val artworkKey: String?,
    val artworkUri: String?,
    val hadGenreOverride: Boolean,
    val genre: String?
)
