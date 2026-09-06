package com.resonance.player.navigation

/**
 * Stitch route architecture: tabs are Home / Library / Playlists / Settings.
 * Search, Now Playing and Queue are GLOBAL routes (no tab slot); Search stays
 * fully functional and is reached via top-bar actions + back navigation until
 * the global search entry lands on every header.
 */
sealed class AppDestination(val route: String) {
    data object Home : AppDestination("home")
    data object Library : AppDestination("library")
    data object Search : AppDestination("search")
    data object Settings : AppDestination("settings")
    data object Player : AppDestination("player/{songId}") {
        const val ARG_SONG_ID = "songId"
        fun routeFor(songId: Long) = "player/$songId"
    }
    data object Queue : AppDestination("queue")
    data object Playlists : AppDestination("playlists")

    companion object {
        /**
         * The four Stitch tab destinations, in dock order. Lazy on purpose:
         * eager initialization here would read nested `object` instances from
         * the outer class static initializer — a JLS 12.4.2 recursive-init
         * cycle that can surface null elements at runtime.
         */
        val tabs: List<AppDestination> by lazy { listOf(Home, Library, Playlists, Settings) }

        /**
         * Maps a current nav route to its tab, or null for global routes
         * (search / player / queue). Pure and unit-tested.
         */
        fun tabForRoute(route: String?): AppDestination? {
            if (route == null) return null
            val base = route.substringBefore("/")
            return tabs.firstOrNull { it.route == base }
        }
    }
}
