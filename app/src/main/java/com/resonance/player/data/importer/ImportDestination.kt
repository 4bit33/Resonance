package com.resonance.player.data.importer

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.resonance.player.domain.importer.importedAudioExtensions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Moves finished downloads from the app's private staging folder into the
 * music folder the user picked (a tree with a persisted write grant), keeping
 * the Artist/Album structure. Files are created as octet-stream so providers
 * keep the exact name and extension; the scanner reads the real type later.
 */
class ImportDestination(private val context: Context) {

    /** Returns how many files were moved. An existing file with the same name is replaced. */
    suspend fun moveAll(stagingDir: File, treeUri: String): Int = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, Uri.parse(treeUri)) ?: return@withContext 0
        var moved = 0
        stagingDir.walkTopDown()
            .filter { it.isFile && it.extension.lowercase() in importedAudioExtensions }
            .forEach { file ->
                val parts = file.relativeTo(stagingDir).path.split(File.separatorChar)
                var dir = root
                for (name in parts.dropLast(1)) {
                    dir = dir.findFile(name)?.takeIf { it.isDirectory } ?: dir.createDirectory(name) ?: return@forEach
                }
                dir.findFile(file.name)?.delete()
                val target = dir.createFile("application/octet-stream", file.name) ?: return@forEach
                val copied = runCatching {
                    context.contentResolver.openOutputStream(target.uri)?.use { out ->
                        file.inputStream().use { it.copyTo(out) }
                    } != null
                }.getOrDefault(false)
                if (copied) {
                    file.delete()
                    moved++
                } else {
                    target.delete()
                }
            }
        stagingDir.deleteRecursively()
        moved
    }
}
