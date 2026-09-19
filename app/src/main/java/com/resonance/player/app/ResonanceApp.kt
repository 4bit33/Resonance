package com.resonance.player.app

import android.app.Application

/** Application owns the [AppContainer]; nothing else constructs dependencies. */
class ResonanceApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
