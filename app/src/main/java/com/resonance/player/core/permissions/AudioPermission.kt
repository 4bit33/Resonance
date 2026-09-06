package com.resonance.player.core.permissions

/**
 * Audio-permission states surfaced to UI. [NotAsked] vs [Denied] is tracked
 * via a persisted "asked before" flag; [PermanentlyDenied] means the OS will
 * no longer show the dialog (open Settings instead).
 */
sealed interface AudioPermissionStatus {
    data object Granted : AudioPermissionStatus
    data object NotAsked : AudioPermissionStatus
    data object Denied : AudioPermissionStatus
    data object PermanentlyDenied : AudioPermissionStatus
}

/** Pure derivation of [AudioPermissionStatus] from OS signals. JVM-testable. */
fun permissionStatusFor(
    granted: Boolean,
    askedBefore: Boolean,
    shouldShowRationale: Boolean
): AudioPermissionStatus = when {
    granted -> AudioPermissionStatus.Granted
    !askedBefore -> AudioPermissionStatus.NotAsked
    shouldShowRationale -> AudioPermissionStatus.Denied
    else -> AudioPermissionStatus.PermanentlyDenied
}

/**
 * Abstraction over the Activity permission request. Implemented once in the
 * UI layer; ViewModels observe status through [AudioPermissionManager].
 */
interface AudioPermissionManager {
    val status: kotlinx.coroutines.flow.StateFlow<AudioPermissionStatus>
    suspend fun refresh(shouldShowRationale: Boolean)
    suspend fun markAsked()
}
