package com.resonance.player.data.importer

import android.content.Context
import com.resonance.player.domain.importer.ytDlpArguments
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * yt-dlp + ffmpeg on the device (youtubedl-android). Unpacking the bundled
 * Python takes a few seconds the first time, so it happens on the first
 * import, never at app start. yt-dlp updates itself once per app run, because
 * YouTube changes often break older versions; an update that fails (offline,
 * GitHub down) is not an error, the bundled version still runs.
 */
class YtDlpEngine(private val context: Context) {
    private val readyLock = Mutex()
    private var ready = false
    private var updateTried = false

    private suspend fun ensureReady() = readyLock.withLock {
        if (ready) return@withLock
        withContext(Dispatchers.IO) {
            YoutubeDL.getInstance().init(context)
            FFmpeg.getInstance().init(context)
        }
        ready = true
    }

    private suspend fun updateOnce() {
        if (updateTried) return
        updateTried = true
        withContext(Dispatchers.IO) {
            runCatching { YoutubeDL.getInstance().updateYoutubeDL(context, YoutubeDL.UpdateChannel._STABLE) }
        }
    }

    /**
     * Downloads [url] (a song, album or playlist) into [stagingDir]. [onProgress]
     * gets the percent of the current file and each output line. Returns
     * yt-dlp's error text on failure, null on success.
     */
    suspend fun download(
        url: String,
        stagingDir: File,
        archive: File,
        processId: String,
        onProgress: (percent: Float, line: String) -> Unit
    ): String? {
        ensureReady()
        updateOnce()
        return withContext(Dispatchers.IO) {
            stagingDir.mkdirs()
            val request = YoutubeDLRequest(url).addCommands(ytDlpArguments(url, stagingDir.absolutePath, archive.absolutePath))
            try {
                val response = YoutubeDL.getInstance().execute(request, processId) { percent, _, line ->
                    onProgress(percent, line)
                }
                if (response.exitCode == 0) null else response.err.lines().lastOrNull { it.isNotBlank() } ?: "yt-dlp failed"
            } catch (e: YoutubeDL.CanceledException) {
                CANCELED
            } catch (e: Exception) {
                e.message?.lines()?.lastOrNull { it.isNotBlank() } ?: e.toString()
            }
        }
    }

    fun cancel(processId: String) {
        runCatching { YoutubeDL.getInstance().destroyProcessById(processId) }
    }

    companion object {
        const val CANCELED = "canceled"
    }
}
