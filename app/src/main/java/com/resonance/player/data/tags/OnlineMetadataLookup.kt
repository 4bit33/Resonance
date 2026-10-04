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
class OnlineMetadataLookup(private val userAgent: String) : MetadataLookup {

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

    private suspend fun searchMusicBrainz(title: String, artist: String?): List<TagCandidate> {
        val query = buildString {
            append("recording:\"").append(title.replace("\"", "")).append('"')
            if (!artist.isNullOrBlank()) append(" AND artist:\"").append(artist.replace("\"", "")).append('"')
        }
        val url = "https://musicbrainz.org/ws/2/recording?fmt=json&limit=8&query=" + encode(query)
        val json = musicBrainzGate.withLock {
            val wait = 1_100L - (System.currentTimeMillis() - lastMusicBrainzAt)
            if (wait > 0) delay(wait)
            try {
                get(url)
            } finally {
                lastMusicBrainzAt = System.currentTimeMillis()
            }
        }
        return LookupParsers.musicBrainz(json)
    }

    private suspend fun searchDeezer(title: String, artist: String?): List<TagCandidate> {
        // Plain "artist title": Deezer's advanced artist:"" track:"" syntax misses many tracks.
        val q = listOfNotNull(artist?.takeIf { it.isNotBlank() }, title).joinToString(" ")
        return LookupParsers.deezerSearch(get("https://api.deezer.com/search?limit=8&q=" + encode(q)))
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
