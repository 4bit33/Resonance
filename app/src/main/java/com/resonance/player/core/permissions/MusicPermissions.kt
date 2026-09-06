package com.resonance.player.core.permissions

/**
 * Platform permission branches, isolated here so no other layer checks
 * Build.VERSION directly (ADR-002). Uses raw SDK ints + permission-name
 * literals so the pure mapping stays JVM-testable without Robolectric.
 */
object MusicPermissions {
    const val READ_MEDIA_AUDIO = "android.permission.READ_MEDIA_AUDIO"
    const val READ_EXTERNAL_STORAGE = "android.permission.READ_EXTERNAL_STORAGE"
    const val POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS"

    /** Granular media permission (API 33+) vs legacy storage (<= 32). */
    fun audioPermissionForSdk(sdkInt: Int): String =
        if (sdkInt >= 33) READ_MEDIA_AUDIO else READ_EXTERNAL_STORAGE

    /** Runtime notification permission only exists on API 33+. */
    fun needsNotificationPermission(sdkInt: Int): Boolean = sdkInt >= 33

    /** Foreground-service mediaPlayback type only exists on API 29+. */
    fun supportsMediaPlaybackServiceType(sdkInt: Int): Boolean = sdkInt >= 29
}
