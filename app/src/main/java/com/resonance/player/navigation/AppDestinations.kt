package com.resonance.player.navigation

/** Type-safe-ish route table for the foundation shell (Stitch map arrives later). */
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
}
