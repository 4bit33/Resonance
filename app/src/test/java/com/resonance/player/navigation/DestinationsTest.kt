package com.resonance.player.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DestinationsTest {

    @Test
    fun tabs_areHomeLibraryPlaylists() {
        assertEquals(
            listOf("home", "library", "playlists"),
            AppDestination.tabs.map { it.route.substringBefore("?") }
        )
    }

    @Test
    fun tabForRoute_mapsTabsAndGlobals() {
        assertEquals(AppDestination.Home, AppDestination.tabForRoute("home"))
        assertEquals(AppDestination.Library, AppDestination.tabForRoute("library"))
        assertEquals(AppDestination.Library, AppDestination.tabForRoute("library?tab=2"))
        assertEquals(AppDestination.Playlists, AppDestination.tabForRoute("playlists"))
        // Settings is reached from Home's gear, not a tab; the Home editor belongs to Home.
        assertNull(AppDestination.tabForRoute("settings"))
        assertEquals(AppDestination.Home, AppDestination.tabForRoute("home/edit"))
        assertNull(AppDestination.tabForRoute("search"))
        assertNull(AppDestination.tabForRoute("player/42"))
        assertNull(AppDestination.tabForRoute("queue"))
        assertNull(AppDestination.tabForRoute(null))
    }

    @Test
    fun libraryRoute_buildsWithTab() {
        assertEquals("library?tab=3", AppDestination.Library.routeFor(3))
    }

    @Test
    fun playerRoute_builds() {
        assertEquals("player/7", AppDestination.Player.routeFor(7L))
    }
}
