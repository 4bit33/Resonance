package com.resonance.player.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DestinationsTest {

    @Test
    fun tabs_coverStitchDestinations() {
        assertEquals(
            listOf("home", "library", "playlists", "settings"),
            AppDestination.tabs.map { it.route }
        )
    }

    @Test
    fun tabForRoute_mapsTabsAndGlobals() {
        assertEquals(AppDestination.Home, AppDestination.tabForRoute("home"))
        assertEquals(AppDestination.Library, AppDestination.tabForRoute("library"))
        assertEquals(AppDestination.Playlists, AppDestination.tabForRoute("playlists"))
        assertEquals(AppDestination.Settings, AppDestination.tabForRoute("settings"))
        assertNull(AppDestination.tabForRoute("search"))
        assertNull(AppDestination.tabForRoute("player/42"))
        assertNull(AppDestination.tabForRoute("queue"))
        assertNull(AppDestination.tabForRoute(null))
    }

    @Test
    fun playerRoute_builds() {
        assertEquals("player/7", AppDestination.Player.routeFor(7L))
    }
}
