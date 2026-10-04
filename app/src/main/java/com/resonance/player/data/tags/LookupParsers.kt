package com.resonance.player.data.tags

import com.resonance.player.domain.tags.TagCandidate
import com.resonance.player.domain.tags.yearOf
import org.json.JSONObject

/** JSON -> candidates for MusicBrainz and Deezer. No I/O, unit-tested with sample responses. */
object LookupParsers {

    /**
     * AcoustID /v2/lookup with meta=recordings: MusicBrainz recording ids of
     * results scoring at least [minScore], best score first, without repeats.
     */
    fun acoustIdRecordings(json: String, minScore: Double = 0.5): List<String> {
        val root = JSONObject(json)
        if (root.optString("status") != "ok") {
            error(root.optJSONObject("error")?.optString("message") ?: "AcoustID error")
        }
        val results = root.optJSONArray("results") ?: return emptyList()
        val scored = (0 until results.length()).map { results.getJSONObject(it) }
            .filter { it.optDouble("score", 0.0) >= minScore }
            .sortedByDescending { it.optDouble("score", 0.0) }
        val ids = LinkedHashSet<String>()
        scored.forEach { r ->
            val recordings = r.optJSONArray("recordings") ?: return@forEach
            for (k in 0 until recordings.length()) recordings.getJSONObject(k).optString("id").takeIf { it.isNotBlank() }?.let(ids::add)
        }
        return ids.toList()
    }

    /**
     * MusicBrainz /ws/2/recording search. For each recording the release
     * picked is the earliest official one (else the earliest of any kind);
     * its track number comes from its medium, its cover from the Cover Art
     * Archive, the genre from the recording's most-voted tag.
     */
    fun musicBrainz(json: String): List<TagCandidate> {
        val recordings = JSONObject(json).optJSONArray("recordings") ?: return emptyList()
        return (0 until recordings.length()).mapNotNull { i ->
            val r = recordings.getJSONObject(i)
            val title = r.optString("title").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val credits = r.optJSONArray("artist-credit")
            val artist = buildString {
                if (credits != null) {
                    for (k in 0 until credits.length()) {
                        val c = credits.getJSONObject(k)
                        append(c.optString("name"))
                        append(c.optString("joinphrase"))
                    }
                }
            }.trim()
            val releases = r.optJSONArray("releases")
            val release = releases?.let { arr ->
                val all = (0 until arr.length()).map { arr.getJSONObject(it) }
                val official = all.filter { it.optString("status").equals("Official", true) }
                (official.ifEmpty { all }).minByOrNull { it.optString("date").ifBlank { "9999" } }
            }
            val track = release?.optJSONArray("media")?.optJSONObject(0)?.optJSONArray("track")?.optJSONObject(0)
            val releaseId = release?.optString("id")?.takeIf { it.isNotBlank() }
            val tags = r.optJSONArray("tags")
            val genre = tags?.let { arr ->
                (0 until arr.length()).map { arr.getJSONObject(it) }
                    .maxByOrNull { it.optInt("count") }
                    ?.optString("name")?.takeIf { it.isNotBlank() }
                    ?.replaceFirstChar { it.uppercase() }
            }
            val releaseArtist = release?.optJSONArray("artist-credit")?.optJSONObject(0)?.optString("name")
            TagCandidate(
                source = TagCandidate.Source.MUSICBRAINZ,
                id = r.optString("id"),
                title = title,
                artist = artist,
                album = release?.optString("title")?.takeIf { it.isNotBlank() },
                albumArtist = releaseArtist?.takeIf { it.isNotBlank() && it != artist },
                year = yearOf(release?.optString("date")),
                trackNumber = track?.optString("number")?.toIntOrNull(),
                genre = genre,
                durationMs = r.optLong("length").takeIf { it > 0L },
                coverUrl = releaseId?.let { "https://coverartarchive.org/release/$it/front-500" },
                thumbUrl = releaseId?.let { "https://coverartarchive.org/release/$it/front-250" }
            )
        }
    }

    /** Deezer /search. Year, track number and genre come later from [deezerTrack] / [deezerAlbumGenre]. */
    fun deezerSearch(json: String): List<TagCandidate> {
        val data = JSONObject(json).optJSONArray("data") ?: return emptyList()
        return (0 until data.length()).mapNotNull { i ->
            val t = data.getJSONObject(i)
            val title = t.optString("title").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val album = t.optJSONObject("album")
            TagCandidate(
                source = TagCandidate.Source.DEEZER,
                id = t.optLong("id").toString(),
                title = title,
                artist = t.optJSONObject("artist")?.optString("name").orEmpty(),
                album = album?.optString("title")?.takeIf { it.isNotBlank() },
                albumArtist = null,
                year = null,
                trackNumber = null,
                genre = null,
                durationMs = t.optLong("duration").takeIf { it > 0L }?.times(1000L),
                coverUrl = album?.optString("cover_xl")?.takeIf { it.isNotBlank() },
                thumbUrl = album?.optString("cover_medium")?.takeIf { it.isNotBlank() }
            )
        }
    }

    /** Deezer /track/{id}: track position, release date and album id. */
    fun deezerTrack(json: String, base: TagCandidate): Pair<TagCandidate, Long?> {
        val t = JSONObject(json)
        val album = t.optJSONObject("album")
        val albumArtist = t.optJSONArray("contributors")?.optJSONObject(0)?.optString("name")
        return base.copy(
            trackNumber = t.optInt("track_position").takeIf { it > 0 } ?: base.trackNumber,
            year = yearOf(t.optString("release_date")) ?: yearOf(album?.optString("release_date")) ?: base.year,
            albumArtist = base.albumArtist ?: albumArtist?.takeIf { it.isNotBlank() && it != base.artist }
        ) to album?.optLong("id")?.takeIf { it > 0L }
    }

    /** Deezer /album/{id}: the first genre name, if any. */
    fun deezerAlbumGenre(json: String): String? =
        JSONObject(json).optJSONObject("genres")?.optJSONArray("data")?.optJSONObject(0)?.optString("name")?.takeIf { it.isNotBlank() }
}
