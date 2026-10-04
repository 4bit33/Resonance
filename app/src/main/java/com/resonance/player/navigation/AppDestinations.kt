package com.resonance.player.navigation

import com.resonance.player.domain.library.CollectionRef

/**
 * Route architecture: tabs are Home / Library / Playlists. Settings (gear on
 * Home), Search, Now Playing, Queue and the Home editor are global routes
 * (no tab slot), reached by a push and left with back.
 */
sealed class AppDestination(val route: String) {
    data object Home : AppDestination("home")
    data object Library : AppDestination("library?tab={tab}") {
        const val ARG_TAB = "tab"
        fun routeFor(tab: Int) = "library?tab=$tab"
    }
    data object Search : AppDestination("search")
    data object Settings : AppDestination("settings")
    data object HomeEditor : AppDestination("home/edit")
    data object Import : AppDestination("import")
    data object Tags : AppDestination("tags/{songId}") {
        const val ARG_SONG_ID = "songId"
        fun routeFor(songId: Long) = "tags/$songId"
    }

    /** An album, artist, genre or folder page. Values are URI-encoded by the caller-facing builder. */
    data object Collection : AppDestination("collection/{kind}?key={key}&extra={extra}") {
        const val ARG_KIND = "kind"
        const val ARG_KEY = "key"
        const val ARG_EXTRA = "extra"

        fun routeFor(ref: CollectionRef, encode: (String) -> String): String {
            val (kind, key, extra) = when (ref) {
                is CollectionRef.Album -> Triple("album", ref.name, ref.albumArtist)
                is CollectionRef.Artist -> Triple("artist", ref.name, null)
                is CollectionRef.Genre -> Triple("genre", ref.name, null)
                is CollectionRef.Folder -> Triple("folder", ref.path, ref.name)
            }
            return "collection/$kind?key=${encode(key)}" + (extra?.let { "&extra=${encode(it)}" } ?: "")
        }

        /** Inverse of [routeFor] on the already-decoded arguments; null for an unknown kind. */
        fun refFor(kind: String?, key: String?, extra: String?): CollectionRef? = when (kind) {
            "album" -> key?.let { CollectionRef.Album(it, extra) }
            "artist" -> key?.let { CollectionRef.Artist(it) }
            "genre" -> key?.let { CollectionRef.Genre(it) }
            "folder" -> CollectionRef.Folder(key.orEmpty(), extra.orEmpty())
            else -> null
        }
    }
    data object Favorites : AppDestination("favorites")
    data object Player : AppDestination("player/{songId}") {
        const val ARG_SONG_ID = "songId"
        fun routeFor(songId: Long) = "player/$songId"
    }
    data object Queue : AppDestination("queue")
    data object Playlists : AppDestination("playlists")
    data object PlaylistDetail : AppDestination("playlists/{playlistId}") {
        const val ARG_PLAYLIST_ID = "playlistId"
        fun routeFor(playlistId: Long) = "playlists/$playlistId"
    }

    companion object {
        /**
         * The tab destinations, in dock order. Lazy on purpose:
         * eager initialization here would read nested `object` instances from
         * the outer class static initializer — a JLS 12.4.2 recursive-init
         * cycle that can surface null elements at runtime.
         */
        val tabs: List<AppDestination> by lazy { listOf(Home, Library, Playlists) }

        /**
         * Maps a current nav route to its tab, or null for global routes
         * (search / player / queue). Pure and unit-tested.
         */
        fun tabForRoute(route: String?): AppDestination? {
            if (route == null) return null
            val base = route.substringBefore("?").substringBefore("/")
            return tabs.firstOrNull { it.route.substringBefore("?") == base }
        }
    }
}
