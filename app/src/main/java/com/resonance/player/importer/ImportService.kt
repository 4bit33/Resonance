package com.resonance.player.importer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.resonance.player.MainActivity
import com.resonance.player.R
import com.resonance.player.app.ResonanceApp
import com.resonance.player.domain.importer.ImportJob
import com.resonance.player.domain.importer.ImportStatus
import com.resonance.player.domain.importer.isActive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Keeps imports running when the app is in the background, with a progress
 * notification. Holds no logic: it mirrors [ImportManager.jobs] and stops
 * itself once nothing is queued or running.
 */
class ImportService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var collecting = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureChannel()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            build(emptyList()),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0
        )
        if (!collecting) {
            collecting = true
            val manager = (application as ResonanceApp).container.importManager
            scope.launch {
                manager.jobs.collect { jobs ->
                    if (jobs.none { it.status.isActive }) {
                        ServiceCompat.stopForeground(this@ImportService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    } else {
                        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, build(jobs))
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, getString(R.string.import_channel), NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun build(jobs: List<ImportJob>): Notification {
        val running = jobs.firstNotNullOfOrNull { it.status as? ImportStatus.Running }
        val queued = jobs.count { it.status == ImportStatus.Queued }
        val title = running?.title ?: getString(R.string.import_title)
        val text = buildString {
            if (running?.item != null && running.total != null) append(getString(R.string.import_item_of, running.item, running.total))
            if (queued > 0) {
                if (isNotEmpty()) append(" · ")
                append(getString(R.string.import_queued_count, queued))
            }
        }
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text.ifEmpty { null })
            .setProgress(100, running?.percent?.toInt() ?: 0, running == null)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(open)
            .build()
    }

    private companion object {
        const val CHANNEL_ID = "import"
        const val NOTIFICATION_ID = 7301
    }
}
