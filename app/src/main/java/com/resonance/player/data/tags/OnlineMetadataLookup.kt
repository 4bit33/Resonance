package com.resonance.player.data.tags

import com.resonance.player.core.common.AppError
import com.resonance.player.core.common.Result
import com.resonance.player.domain.tags.MetadataLookup
import com.resonance.player.domain.tags.TagCandidate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * MusicBrainz + Deezer, asked in parallel; only when the user taps search
 * (ADR-013). MusicBrainz wants an identifying User-Agent and at most one
 * request per second; requests to it are spaced accordingly. Deezer needs no
 * key. Any source failing just drops out; both failing is an error.
 */
class OnlineMetadataLookup(
    private val userAgent: String,
    private val acoustIdKey: String
) : MetadataLookup {

    private val musicBrainzGate = Mutex()
    private var lastMusicBrainzAt = 0L

    override suspend fun search(title: String, artist: String?): Result<List<TagCandidate>> = coroutineScope {
        val mb = async { runCatching { searchMusicBrainz(title, artist) } }
        val dz = async { runCatching { searchDeezer(title, artist) } }
        val a = mb.await()
        val b = dz.await()
        if (a.isFailure && b.isFailure) {
            Result.Failure(AppError.Unknown(a.exceptionOrNull()?.message ?: "Lookup failed"))
        } else {
            Result.Success(a.getOrDefault(emptyList()) + b.getOrDefault(emptyList()))
        }
    }

    override suspend fun details(candidate: TagCandidate): TagCandidate {
        if (candidate.source != TagCandidate.Source.DEEZER) return candidate
        return runCatching {
            val (withTrack, albumId) = LookupParsers.deezerTrack(get("https://api.deezer.com/track/${candidate.id}"), candidate)
            val genre = albumId?.let { runCatching { LookupParsers.deezerAlbumGenre(get("https://api.deezer.com/album/$it")) }.getOrNull() }
            withTrack.copy(genre = genre ?: withTrack.genre)
        }.getOrDefault(candidate)
    }

    override suspend fun identify(fingerprint: String, durationSec: Int): Result<List<TagCandidate>> {
        if (acoustIdKey.isBlank()) return Result.Failure(AppError.FeatureUnavailable("AcoustID"))
        return try {
            val body = "client=" + encode(acoustIdKey) + "&meta=recordings&duration=" + durationSec + "&fingerprint=" + encode(fingerprint)
            val ids = LookupParsers.acoustIdRecordings(post("https://api.acoustid.org/v2/lookup", body)).take(3)
            if (ids.isEmpty()) return Result.Success(emptyList())
            val query = ids.joinToString(" OR ") { "rid:$it" }
            val json = musicBrainz("https://musicbrainz.org/ws/2/recording?fmt=json&limit=8&query=" + encode(query))
            // Keep AcoustID's order (best match first).
            val found = LookupParsers.musicBrainz(json)
            Result.Success(found.sortedBy { c -> ids.indexOf(c.id).let { if (it < 0) Int.MAX_VALUE else it } })
        } catch (e: Exception) {
            Result.Failure(AppError.Unknown(e.message ?: "AcoustID lookup failed"))
        }
    }

    private suspend fun searchMusicBrainz(title: String, artist: String?): List<TagCandidate> {
        val query = buildString {
            append("recording:\"").append(title.replace("\"", "")).append('"')
            if (!artist.isNullOrBlank()) append(" AND artist:\"").append(artist.replace("\"", "")).append('"')
        }
        val url = "https://musicbrainz.org/ws/2/recording?fmt=json&limit=8&query=" + encode(query)
        return LookupParsers.musicBrainz(musicBrainz(url))
    }

    /** One request to MusicBrainz, spaced at least 1.1 s from the previous one. */
    private suspend fun musicBrainz(url: String): String =
        musicBrainzGate.withLock {
            val wait = 1_100L - (System.currentTimeMillis() - lastMusicBrainzAt)
            if (wait > 0) delay(wait)
            try {
                get(url)
            } finally {
                lastMusicBrainzAt = System.currentTimeMillis()
            }
        }

    private suspend fun searchDeezer(title: String, artist: String?): List<TagCandidate> {
        // Plain "artist title": Deezer's advanced artist:"" track:"" syntax misses many tracks.
        val q = listOfNotNull(artist?.takeIf { it.isNotBlank() }, title).joinToString(" ")
        return LookupParsers.deezerSearch(get("https://api.deezer.com/search?limit=8&q=" + encode(q)))
    }

    private suspend fun post(url: String, body: String): String = withContext(Dispatchers.IO) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 15_000
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("User-Agent", userAgent)
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        }
        try {
            connection.outputStream.use { it.write(body.toByteArray()) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            // AcoustID explains errors (e.g. a bad key) in a JSON body; let the parser report it.
            if (code !in 200..299 && !text.startsWith("{")) throw IllegalStateException("HTTP $code from ${URL(url).host}")
            text
        } finally {
            connection.disconnect()
        }
    }

    private fun encode(s: String): String = URLEncoder.encode(s, "UTF-8")

    private suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 15_000
            setRequestProperty("User-Agent", userAgent)
            setRequestProperty("Accept", "application/json")
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) throw IllegalStateException("HTTP $code from ${URL(url).host}")
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
