package com.resonance.player.core.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Injectable dispatchers. Production uses [DefaultAppDispatchers]; tests inject
 * a test-dispatcher implementation. Database and filesystem work must always
 * be confined to [io] — never the main thread.
 */
interface AppDispatchers {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
}

class DefaultAppDispatchers : AppDispatchers {
    override val main: CoroutineDispatcher = Dispatchers.Main
    override val io: CoroutineDispatcher = Dispatchers.IO
    override val default: CoroutineDispatcher = Dispatchers.Default
}
