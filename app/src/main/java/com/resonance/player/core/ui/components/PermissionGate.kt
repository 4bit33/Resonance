package com.resonance.player.core.ui.components

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.resonance.player.R
import com.resonance.player.core.permissions.AudioPermissionManager
import com.resonance.player.core.permissions.AudioPermissionStatus
import com.resonance.player.core.permissions.MusicPermissions
import kotlinx.coroutines.launch

fun audioPermissionName(): String =
    MusicPermissions.audioPermissionForSdk(Build.VERSION.SDK_INT)

fun shouldShowAudioRationale(activity: Activity?): Boolean {
    if (activity == null) return false
    return ActivityCompat.shouldShowRequestPermissionRationale(activity, audioPermissionName())
}

fun openAppSettings(activity: Activity?) {
    if (activity == null) return
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", activity.packageName, null)
    )
    activity.startActivity(intent)
}

/**
 * Centralized audio-permission UX. Cached library content stays visible
 * underneath; the prompt card explains why access is needed (no dark
 * patterns, no re-request loops — the OS owns denial backoff).
 */
@Composable
fun AudioPermissionGate(
    manager: AudioPermissionManager,
    onPermissionGranted: () -> Unit,
    content: @Composable () -> Unit
) {
    val status by manager.status.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = context as? Activity

    LaunchedEffect(Unit) {
        manager.refresh(shouldShowAudioRationale(activity))
    }

    val requestGrant = rememberPermissionGrant(manager, onPermissionGranted)

    Column {
        when (status) {
            AudioPermissionStatus.Granted -> Unit
            AudioPermissionStatus.NotAsked,
            AudioPermissionStatus.Denied -> {
                PermissionPromptCard(
                    message = stringResource(R.string.perm_rationale),
                    actionLabel = stringResource(R.string.perm_grant),
                    onAction = requestGrant
                )
            }
            AudioPermissionStatus.PermanentlyDenied -> {
                PermissionPromptCard(
                    message = stringResource(R.string.perm_permanently_denied),
                    actionLabel = stringResource(R.string.perm_open_settings),
                    onAction = { openAppSettings(activity) }
                )
            }
        }
        content()
    }
}

@Composable
private fun PermissionPromptCard(message: String, actionLabel: String, onAction: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(message, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

/**
 * Shared permission-request launcher: marks "asked", refreshes status with
 * the current rationale signal and reports grants. Used by Library gate and
 * Settings alike so request behavior never diverges.
 */
@Composable
fun rememberPermissionGrant(
    manager: AudioPermissionManager,
    onPermissionGranted: () -> Unit
): () -> Unit {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = context as? Activity
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        scope.launch {
            manager.markAsked()
            manager.refresh(shouldShowAudioRationale(activity))
            if (granted) onPermissionGranted()
        }
    }
    return { launcher.launch(audioPermissionName()) }
}