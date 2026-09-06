package com.resonance.player.core.common

/**
 * Single application-wide error model. Every failure that can reach the UI
 * must be expressed as one of these cases so screens can render deterministic
 * empty / error states (see Phase 1 spec: error handling).
 */
sealed interface AppError {
    /** File indexed in the database no longer exists on storage. */
    data class MissingFile(val path: String) : AppError

    /** Audio / storage / notification permission was denied or revoked. */
    data object PermissionDenied : AppError

    /** Container or codec the device cannot decode. */
    data class UnsupportedFormat(val mimeType: String?) : AppError

    /** ExoPlayer / MediaSession reported a playback failure. */
    data class PlaybackFailed(val reason: String?) : AppError

    /** Room / DataStore failure. */
    data class DatabaseError(val reason: String?) : AppError

    /** Tags are absent or unparseable; file itself is playable. */
    data class InvalidMetadata(val reason: String?) : AppError

    /** Embedded artwork blob cannot be decoded. */
    data object CorruptedArtwork : AppError

    /** Library query succeeded but contains zero songs. */
    data object EmptyLibrary : AppError

    /**
     * Honest stand-in for functionality scheduled for a later phase
     * (e.g. playback engine in Phase 2). Production stubs must return this
     * instead of pretending to work.
     */
    data class FeatureUnavailable(val feature: String) : AppError

    data class Unknown(val reason: String? = null) : AppError
}

/** Stable, localizable-later human message. Pure function, unit-tested. */
fun AppError.userMessage(): String = when (this) {
    is AppError.MissingFile -> "This file is no longer on your device."
    AppError.PermissionDenied -> "Audio access was denied. Grant access in Settings to scan your music."
    is AppError.UnsupportedFormat ->
        if (mimeType.isNullOrBlank()) "This audio format is not supported."
        else "This audio format is not supported ($mimeType)."
    is AppError.PlaybackFailed ->
        if (reason.isNullOrBlank()) "Playback failed." else "Playback failed: $reason"
    is AppError.DatabaseError -> "The music library database reported an error."
    is AppError.InvalidMetadata -> "Some tags could not be read; the file can still play."
    AppError.CorruptedArtwork -> "The embedded artwork could not be decoded."
    AppError.EmptyLibrary -> "Your library is empty."
    is AppError.FeatureUnavailable -> "$feature is not available yet."
    is AppError.Unknown ->
        if (reason.isNullOrBlank()) "Something went wrong." else reason
}
