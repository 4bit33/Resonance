package com.resonance.player.app

import android.app.Application

/** Application owns the [AppContainer]; nothing else constructs dependencies. */
class ResonanceApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Cached library renders first; the incremental scan reconciles
        // in the background on the application scope (never blocking UI).
        container.onAppStarted()
    }
}
