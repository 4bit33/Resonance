package com.resonance.player.data.media

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import com.resonance.player.core.media.AudioCandidate
import com.resonance.player.core.media.DOCUMENT_DIR_MIME
import com.resonance.player.core.media.DocRow
import com.resonance.player.core.media.audioCandidateOf
import com.resonance.player.core.media.isAudioDoc
import com.resonance.player.core.media.joinRelPath
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * How completely one source could be listed. Drives which stored rows a scan
 * may delete (see [SourceReconciler]): only COMPLETE sources can prove a file
 * is gone.
 */
enum class WalkStatus { COMPLETE, INCOMPLETE, UNAVAILABLE }

/**
 * Lists the audio files of user-added sources through the Storage Access
 * Framework. The only class that talks DocumentsContract/ContentResolver for
 * discovery.
 *
 * Trees are walked with ONE child query per directory (DocumentFile issues a
 * query per property and is far slower). Cancellation is checked per
 * directory and per row; the cursor closes on cancel.
 */
class SafAudioDataSource(private val resolver: ContentResolver) {

    /** URIs the app still holds a persisted READ grant for. */
    fun readGrants(): Set<String> =
        resolver.persistedUriPermissions.filter { it.isReadPermission }.mapTo(HashSet()) { it.uri.toString() }

    /** Display name of a picked folder ([tree]) or file, or null when it cannot be read. */
    suspend fun displayNameOf(uriString: String, tree: Boolean): String? {
        val uri = Uri.parse(uriString)
        val docUri = try {
            if (tree) {
                DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))
            } else {
                uri
            }
        } catch (e: IllegalArgumentException) {
            return null
        }
        return query(docUri)?.use { c -> if (c.moveToFirst()) c.getString(1) else null }
    }

    suspend fun walkTree(
        sourceId: Long,
        treeUriString: String,
        rootName: String,
        onCandidate: (AudioCandidate) -> Unit
    ): WalkStatus {
        val treeUri = Uri.parse(treeUriString)
        val authority = treeUri.authority ?: return WalkStatus.UNAVAILABLE
        val rootId = try {
            DocumentsContract.getTreeDocumentId(treeUri)
        } catch (e: IllegalArgumentException) {
            return WalkStatus.UNAVAILABLE
        }
        var status = WalkStatus.COMPLETE
        val visited = HashSet<String>()
        val stack = ArrayDeque<Pair<String, String>>()
        stack.addLast(rootId to joinRelPath(null, rootName))
        while (stack.isNotEmpty()) {
            currentCoroutineContext().ensureActive()
            val (dirId, relPath) = stack.removeLast()
            if (!visited.add(dirId)) continue
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, dirId)
            val cursor = query(childrenUri)
            if (cursor == null) {
                if (dirId == rootId) return WalkStatus.UNAVAILABLE
                status = WalkStatus.INCOMPLETE
                continue
            }
            try {
                cursor.use { c ->
                    while (c.moveToNext()) {
                        currentCoroutineContext().ensureActive()
                        val row = c.toDocRow()
                        if (row.mimeType == DOCUMENT_DIR_MIME) {
                            stack.addLast(
                                row.documentId to joinRelPath(relPath, row.name ?: row.documentId)
                            )
                        } else if (isAudioDoc(row.name, row.mimeType)) {
                            val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, row.documentId)
                            onCandidate(audioCandidateOf(row, authority, sourceId, docUri.toString(), relPath))
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                status = WalkStatus.INCOMPLETE
            }
        }
        return status
    }

    /**
     * A single individually added file. An empty result means the file is gone
     * (COMPLETE, no candidate). The user picked it explicitly, so it is trusted
     * as audio: the name/type filter only guards folder listings (a picked
     * `.trashed-*` file must not vanish silently).
     */
    suspend fun walkFile(
        sourceId: Long,
        documentUriString: String,
        onCandidate: (AudioCandidate) -> Unit
    ): WalkStatus {
        val uri = Uri.parse(documentUriString)
        val authority = uri.authority ?: return WalkStatus.UNAVAILABLE
        val cursor = query(uri) ?: return WalkStatus.UNAVAILABLE
        cursor.use { c ->
            if (c.moveToFirst()) {
                onCandidate(audioCandidateOf(c.toDocRow(), authority, sourceId, documentUriString, null))
            }
        }
        return WalkStatus.COMPLETE
    }

    /** Null when the provider is unreachable, the grant is gone or the URI is stale. */
    private suspend fun query(uri: Uri): Cursor? = try {
        resolver.query(uri, COLUMNS, null, null, null)
    } catch (e: Exception) {
        currentCoroutineContext().ensureActive()
        null
    }

    private fun Cursor.toDocRow() = DocRow(
        documentId = getString(0),
        name = getString(1),
        mimeType = getString(2),
        lastModifiedMs = if (isNull(3)) null else getLong(3),
        sizeBytes = if (isNull(4)) null else getLong(4)
    )

    private companion object {
        val COLUMNS = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_SIZE
        )
    }
}
