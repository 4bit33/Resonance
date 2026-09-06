package com.resonance.player.playback

import androidx.media3.common.PlaybackException
import com.resonance.player.core.common.AppError

/**
 * Pure mapping from Media3 player error codes to the app [AppError] model.
 * Takes primitive codes (not the exception) so it stays JVM-testable.
 *
 * Queue-coherence policy lives in RealPlaybackController: a failing item
 * surfaces here as state, playback does NOT auto-skip (runaway skipping
 * through a broken queue would be worse), and saved library data is never
 * touched — the library subsystem owns the database.
 */
object PlayerErrorMapper {

    fun map(errorCode: Int, mediaId: String?, mimeType: String?): AppError {
        val name = PlaybackException.getErrorCodeName(errorCode)
        return when (errorCode) {
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
                AppError.MissingFile(mediaId ?: "unknown item")
            PlaybackException.ERROR_CODE_IO_NO_PERMISSION ->
                AppError.PermissionDenied
            PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE,
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
            PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED,
            PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE ->
                AppError.PlaybackFailed("Could not read this file ($name)")
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED,
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
            PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED ->
                AppError.UnsupportedFormat(mimeType)
            PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED,
            PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED,
            PlaybackException.ERROR_CODE_AUDIO_TRACK_OFFLOAD_WRITE_FAILED,
            PlaybackException.ERROR_CODE_AUDIO_TRACK_OFFLOAD_INIT_FAILED ->
                AppError.PlaybackFailed("Audio output failed ($name)")
            PlaybackException.ERROR_CODE_TIMEOUT ->
                AppError.PlaybackFailed("Playback timed out ($name)")
            PlaybackException.ERROR_CODE_DRM_UNSPECIFIED,
            PlaybackException.ERROR_CODE_DRM_SCHEME_UNSUPPORTED,
            PlaybackException.ERROR_CODE_DRM_PROVISIONING_FAILED,
            PlaybackException.ERROR_CODE_DRM_CONTENT_ERROR,
            PlaybackException.ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED,
            PlaybackException.ERROR_CODE_DRM_DISALLOWED_OPERATION,
            PlaybackException.ERROR_CODE_DRM_SYSTEM_ERROR,
            PlaybackException.ERROR_CODE_DRM_DEVICE_REVOKED,
            PlaybackException.ERROR_CODE_DRM_LICENSE_EXPIRED ->
                AppError.PlaybackFailed("Protected content is not supported")
            else -> AppError.Unknown("Playback error ($name)")
        }
    }
}
