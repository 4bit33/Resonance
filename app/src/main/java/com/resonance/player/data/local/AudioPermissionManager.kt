package com.resonance.player.data.local

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.resonance.player.core.media.ScanState
import com.resonance.player.core.permissions.AudioPermissionManager
import com.resonance.player.core.permissions.AudioPermissionStatus
import com.resonance.player.core.permissions.MusicPermissions
import com.resonance.player.core.permissions.permissionStatusFor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

/** Single centralized permission holder (no scattered SDK checks in UI). */
class AppAudioPermissionManager(
    private val appContext: Context,
    private val prefs: LibraryPreferences
) : AudioPermissionManager {

    private val mutable = MutableStateFlow<AudioPermissionStatus>(AudioPermissionStatus.NotAsked)
    override val status: StateFlow<AudioPermissionStatus> = mutable.asStateFlow()

    override suspend fun refresh(shouldShowRationale: Boolean) {
        val granted = ContextCompat.checkSelfPermission(
            appContext, MusicPermissions.audioPermissionForSdk(currentSdk())
        ) == PackageManager.PERMISSION_GRANTED
        val asked = try {
            prefs.permissionAsked.first()
        } catch (e: Exception) {
            false
        }
        mutable.value = permissionStatusFor(granted, asked, shouldShowRationale)
    }

    override suspend fun markAsked() {
        try {
            prefs.setPermissionAsked()
        } catch (e: Exception) {
            // Best-effort flag; permission flow works without it.
        }
        refresh(shouldShowRationale = false)
    }

    private fun currentSdk(): Int = android.os.Build.VERSION.SDK_INT
}

