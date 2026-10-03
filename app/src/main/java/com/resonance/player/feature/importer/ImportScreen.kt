package com.resonance.player.feature.importer

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonance.player.R
import com.resonance.player.app.AppContainer
import com.resonance.player.core.ui.components.ResonanceTopBar
import com.resonance.player.core.ui.components.pressClickable
import com.resonance.player.core.ui.theme.ResonanceTheme
import com.resonance.player.domain.importer.ImportJob
import com.resonance.player.domain.importer.ImportStatus
import com.resonance.player.domain.importer.extractImportUrl
import com.resonance.player.importer.ImportManager
import kotlinx.coroutines.launch

private fun folderName(context: Context, treeUri: String?): String? =
    treeUri?.let { runCatching { DocumentFile.fromTreeUri(context, Uri.parse(it))?.name }.getOrNull() }

/**
 * Import: paste or share a link, pick where music is saved (once), watch the
 * queue. Everything runs in [ImportManager]; this screen only shows it.
 */
@Composable
fun ImportScreen(container: AppContainer, onBack: () -> Unit) {
    val manager = container.importManager
    val jobs by manager.jobs.collectAsStateWithLifecycle()
    val pendingShare by manager.pendingShare.collectAsStateWithLifecycle()
    val treeUri by container.settingsRepository.importTreeUri.collectAsStateWithLifecycle(initialValue = null)
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    var text by remember { mutableStateOf("") }

    LaunchedEffect(pendingShare) {
        pendingShare?.let {
            text = it
            manager.pendingShare.value = null
        }
    }
    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) scope.launch { container.setImportFolder(uri.toString()) }
    }
    val url = extractImportUrl(text)
    val canDownload = url != null && treeUri != null
    fun submit() {
        if (!canDownload || url == null) return
        manager.enqueue(url)
        text = ""
    }

    Column(Modifier.fillMaxSize()) {
        ResonanceTopBar(title = stringResource(R.string.import_title), onBack = onBack)
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Text(stringResource(R.string.import_sources), style = typography.bodyMd, color = colors.textSecondary)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text(stringResource(R.string.import_hint)) },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { clipboard.getText()?.text?.let { text = it } }) {
                            Icon(Icons.Rounded.ContentPaste, contentDescription = stringResource(R.string.import_paste))
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { submit() }),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.outlineStrong,
                        cursorColor = colors.accent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(CircleShape)
                        .background(if (canDownload) colors.accent else colors.surfaceHighest)
                        .pressClickable(onClick = ::submit),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null, tint = if (canDownload) colors.onAccent else colors.textMuted)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.import_download),
                        style = typography.labelLg,
                        color = if (canDownload) colors.onAccent else colors.textMuted
                    )
                }
            }
            item {
                FolderCard(name = folderName(context, treeUri), onPick = { pickFolder.launch(null) })
            }
            item {
                Text(stringResource(R.string.import_note), style = typography.bodySm, color = colors.textSecondary)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.import_share_hint), style = typography.bodySm, color = colors.textMuted)
            }
            if (jobs.isNotEmpty()) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = manager::clearFinished) { Text(stringResource(R.string.import_clear)) }
                    }
                }
            }
            items(jobs.reversed(), key = { it.id }) { job ->
                JobCard(
                    job = job,
                    onCancel = { manager.cancel(job.id) },
                    onRetry = { manager.enqueue(job.url) },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}

@Composable
private fun FolderCard(name: String?, onPick: () -> Unit) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surfaceContainer)
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)
    ) {
        Icon(Icons.Rounded.Folder, contentDescription = null, tint = colors.accent)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.import_folder), style = typography.labelSm, color = colors.textSecondary)
            Text(
                name ?: stringResource(R.string.import_folder_needed),
                style = if (name != null) typography.titleMd else typography.bodySm,
                color = if (name != null) colors.textPrimary else colors.textSecondary
            )
        }
        TextButton(onClick = onPick) {
            Text(stringResource(if (name == null) R.string.import_folder_pick else R.string.import_folder_change))
        }
    }
}

@Composable
private fun JobCard(job: ImportJob, onCancel: () -> Unit, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    val status = job.status
    val running = status as? ImportStatus.Running
    val title = job.title ?: Uri.parse(job.url).let { (it.host ?: "") + (it.path ?: "") }
    val (line, lineColor) = when (status) {
        ImportStatus.Queued -> stringResource(R.string.import_queued) to colors.textSecondary
        is ImportStatus.Running -> (
            if (status.item != null && status.total != null) {
                stringResource(R.string.import_item_of, status.item, status.total) + " · ${status.percent.toInt()}%"
            } else if (status.title == null) {
                stringResource(R.string.import_starting)
            } else {
                "${status.percent.toInt()}%"
            }
            ) to colors.textSecondary
        is ImportStatus.Done -> (
            if (status.songs == 0) {
                stringResource(R.string.import_done_none)
            } else {
                pluralStringResource(R.plurals.import_done_count, status.songs, status.songs)
            } + (status.warning?.let { " · " + stringResource(R.string.import_warning) } ?: "")
            ) to colors.accent
        is ImportStatus.Failed -> stringResource(
            R.string.import_failed,
            if (status.message == ImportManager.NO_FOLDER) stringResource(R.string.import_failed_folder) else status.message
        ) to colors.error
        ImportStatus.Canceled -> stringResource(R.string.import_canceled) to colors.textMuted
    }
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surfaceContainer)
            .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusIcon(status)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = typography.titleMd, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(line, style = typography.bodySm, color = lineColor, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            when (status) {
                ImportStatus.Queued, is ImportStatus.Running -> IconButton(onClick = onCancel) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.cd_cancel), tint = colors.textSecondary)
                }
                is ImportStatus.Failed, ImportStatus.Canceled -> IconButton(onClick = onRetry) {
                    Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.action_retry), tint = colors.textSecondary)
                }
                else -> Unit
            }
        }
        if (running != null) {
            val progress by animateFloatAsState(running.percent / 100f, ResonanceTheme.motion.effects(), label = "import-progress")
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .padding(end = 12.dp)
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(colors.textPrimary.copy(alpha = 0.1f))
            ) {
                Box(Modifier.fillMaxWidth(progress).height(4.dp).background(colors.accent))
            }
        }
    }
}

@Composable
private fun StatusIcon(status: ImportStatus) {
    val colors = ResonanceTheme.colors
    val (icon, tint) = when (status) {
        is ImportStatus.Done -> Icons.Rounded.CheckCircle to colors.accent
        is ImportStatus.Failed -> Icons.Rounded.ErrorOutline to colors.error
        else -> Icons.Rounded.Download to if (status is ImportStatus.Running) colors.accent else colors.textMuted
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.14f))
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
    }
}
