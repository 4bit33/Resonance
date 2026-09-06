package com.resonance.player.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.resonance.player.MainActivity
import com.resonance.player.R

/**
 * The ONLY owner of the ExoPlayer instance and the MediaSession in the app.
 *
 * - Survives Activity/Compose destruction; playback lifecycle belongs here,
 *   never to the UI.
 * - Media3 promotes this service to the foreground automatically while
 *   playing (manifest declares foregroundServiceType="mediaPlayback") and
 *   renders the playback notification + lock-screen / Bluetooth controls
 *   from the session — no custom notification code.
 * - Audio focus is handled by ExoPlayer itself (handleAudioFocus=true):
 *   transient loss pauses, duckable loss ducks, permanent loss pauses;
 *   no blind auto-resume beyond ExoPlayer conventions.
 * - Headset/Bluetooth disconnect pauses via handleAudioBecomingNoisy.
 * - Gapless playback is ExoPlayer default behavior for compatible tracks;
 *   nothing here disables it (pauseAtEndOfMediaItems stays false).
 * - Crossfade: Media3 1.10.1 exposes NO crossfade API (verified against the
 *   shipped artifacts) — so none is faked. MixConfiguration.crossfadeMs
 *   remains the future input for a real transition stage.
 */
class PlaybackService : MediaSessionService() {

    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus= */ true
            )
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .setHandleAudioBecomingNoisy(true)
            .build()
        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        session = MediaSession.Builder(this, player)
            .setId(SESSION_ID)
            .setSessionActivity(sessionActivity)
            .build()
        val notificationProvider = DefaultMediaNotificationProvider(this)
        notificationProvider.setSmallIcon(R.drawable.ic_notification)
        setMediaNotificationProvider(notificationProvider)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = session?.player
        val playing = player != null &&
            player.playWhenReady &&
            player.playbackState != Player.STATE_IDLE &&
            player.playbackState != Player.STATE_ENDED
        if (!playing) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        session?.let {
            it.player.release()
            it.release()
        }
        session = null
        super.onDestroy()
    }

    companion object {
        const val SESSION_ID = "resonance-playback"
    }
}
