package com.resonance.player.data.fingerprint

/**
 * Chromaprint (vendored native library, LGPL-2.1+): turns 16-bit PCM into the
 * compressed fingerprint AcoustID looks up. One instance per song; not thread-safe.
 */
class Chromaprint(sampleRate: Int, channels: Int) : AutoCloseable {
    private var handle: Long = nativeStart(sampleRate, channels)

    init {
        check(handle != 0L) { "Chromaprint could not start ($sampleRate Hz, $channels ch)" }
    }

    /** [count] interleaved samples from [samples]. */
    fun feed(samples: ShortArray, count: Int): Boolean = nativeFeed(handle, samples, count)

    /** The base64 fingerprint, or null if there was too little audio. */
    fun finish(): String? = nativeFinish(handle)

    override fun close() {
        if (handle != 0L) {
            nativeFree(handle)
            handle = 0L
        }
    }

    companion object {
        init {
            System.loadLibrary("crate_fingerprint")
        }

        @JvmStatic private external fun nativeStart(sampleRate: Int, channels: Int): Long
        @JvmStatic private external fun nativeFeed(handle: Long, samples: ShortArray, count: Int): Boolean
        @JvmStatic private external fun nativeFinish(handle: Long): String?
        @JvmStatic private external fun nativeFree(handle: Long)
    }
}
