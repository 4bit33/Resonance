package com.resonance.player.feature.playlists

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonance.player.R
import com.resonance.player.core.common.formatDurationMs
import com.resonance.player.core.model.Playlist
import com.resonance.player.core.model.Song
import com.resonance.player.core.ui.components.ArtworkImage
import com.resonance.player.core.ui.components.ResonanceDialog
import com.resonance.player.core.ui.components.ResonanceEmptyState
import com.resonance.player.core.ui.components.SongPickerSheet
import com.resonance.player.core.ui.components.pressClickable
import com.resonance.player.core.ui.theme.ResonanceTheme
import com.resonance.player.feature.home.PlaylistCover
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * A playlist. Normally just for listening: cover with the name beside it,
 * Play / Shuffle, the songs. The pencil switches to editing, and only then
 * can songs be dragged (long-press or the handle), removed, the cover and
 * the name changed, or the playlist deleted.
 */
@Composable
fun PlaylistDetailScreen(
    viewModel: PlaylistDetailViewModel,
    playlist: Playlist?,
    onBack: () -> Unit,
    onSongClick: (Long) -> Unit,
    onDeleted: () -> Unit
) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showAddSongs by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val pickCover = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.setCover(uri.toString())
    }
    BackHandler(enabled = editing) { editing = false }

    // Local copy so a drag moves rows instantly; the new order is saved when the finger lifts.
    var order by remember { mutableStateOf(songs) }
    var dragStart by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(songs) { if (dragStart == null) order = songs }
    val listState = rememberLazyListState()
    val reorder = rememberReorderableLazyListState(listState) { from, to ->
        val fromIndex = order.indexOfFirst { it.id == from.key }
        val toIndex = order.indexOfFirst { it.id == to.key }
        if (fromIndex >= 0 && toIndex >= 0) {
            order = order.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp, top = 4.dp)) {
            IconButton(onClick = { if (editing) editing = false else onBack() }) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.cd_back), tint = colors.textPrimary)
            }
            Spacer(Modifier.weight(1f))
            if (playlist != null) {
                TextButton(onClick = { editing = !editing }) {
                    Icon(if (editing) Icons.Rounded.Check else Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(if (editing) R.string.action_done else R.string.playlist_edit))
                }
            }
        }
        LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 24.dp), modifier = Modifier.fillMaxSize()) {
            item(key = "header") {
                Header(
                    playlist = playlist,
                    songs = order,
                    editing = editing,
                    onPickCover = { pickCover.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    onRemoveCover = { viewModel.setCover(null) },
                    onRename = { showRename = true },
                    onPlay = { viewModel.playAll(playlist?.name, shuffled = false) },
                    onShuffle = { viewModel.playAll(playlist?.name, shuffled = true) }
                )
            }
            if (editing) {
                item(key = "add") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressClickable(pressedScale = 0.98f) { showAddSongs = true }
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(colors.accent.copy(alpha = 0.16f))
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, tint = colors.accent)
                        }
                        Spacer(Modifier.width(14.dp))
                        Text(stringResource(R.string.playlist_add_songs), style = typography.titleMd, color = colors.textPrimary)
                    }
                }
            }
            if (order.isEmpty() && !editing) {
                item(key = "empty") {
                    ResonanceEmptyState(
                        title = stringResource(R.string.playlist_detail_empty_title),
                        body = stringResource(R.string.playlist_detail_empty_body)
                    )
                }
            }
            itemsIndexed(order, key = { _, song -> song.id }) { index, song ->
                ReorderableItem(reorder, key = song.id, enabled = editing) { dragging ->
                    val lift by animateDpAsState(if (dragging) 10.dp else 0.dp, ResonanceTheme.motion.spatialFast(), label = "drag-lift")
                    val scale by animateFloatAsState(if (dragging) 1.03f else 1f, ResonanceTheme.motion.expressive(), label = "drag-scale")
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .shadow(lift, RoundedCornerShape(14.dp))
                            .background(if (dragging) colors.surfaceHigh else colors.background)
                            .then(
                                if (editing) {
                                    Modifier.longPressDraggableHandle(
                                        onDragStarted = {
                                            dragStart = index
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                        onDragStopped = { commitMove(song, dragStart, order, viewModel) { dragStart = null } }
                                    )
                                } else {
                                    Modifier.pressClickable(pressedScale = 0.98f, onClickLabel = song.title) {
                                        viewModel.playFrom(index, playlist?.name)
                                        onSongClick(song.id)
                                    }
                                }
                            )
                            .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
                            .animateContentSize()
                    ) {
                        Box(Modifier.size(48.dp).clip(RoundedCornerShape(10.dp))) {
                            ArtworkImage(artworkUri = song.artworkUri, contentDescription = null, modifier = Modifier.fillMaxSize())
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(song.title, style = typography.titleMd, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(song.artistName, style = typography.bodySm, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        AnimatedVisibility(visible = !editing, enter = fadeIn(), exit = fadeOut()) {
                            Text(
                                formatDurationMs(song.durationMs),
                                style = typography.monoMetric,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                        AnimatedVisibility(visible = editing, enter = fadeIn() + expandHorizontally(), exit = fadeOut() + shrinkHorizontally()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { viewModel.remove(song.id) }) {
                                    Icon(Icons.Rounded.RemoveCircleOutline, contentDescription = stringResource(R.string.queue_remove), tint = colors.error)
                                }
                                Icon(
                                    Icons.Rounded.DragIndicator,
                                    contentDescription = stringResource(R.string.queue_drag),
                                    tint = colors.textSecondary,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .padding(12.dp)
                                        .draggableHandle(
                                            onDragStarted = {
                                                dragStart = index
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            },
                                            onDragStopped = { commitMove(song, dragStart, order, viewModel) { dragStart = null } }
                                        )
                                )
                            }
                        }
                    }
                }
            }
            if (editing) {
                item(key = "delete") {
                    TextButton(onClick = { showDelete = true }, modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp)) {
                        Text(stringResource(R.string.playlist_delete_action), color = colors.error)
                    }
                }
            }
            if (error != null) {
                item(key = "error") {
                    Text(error ?: "", style = typography.bodyMd, color = colors.error, modifier = Modifier.padding(20.dp))
                }
            }
        }
    }

    if (showRename && playlist != null) {
        PlaylistNameDialog(
            title = stringResource(R.string.playlist_rename),
            confirmLabel = stringResource(R.string.playlist_rename),
            initial = playlist.name,
            onConfirm = {
                viewModel.rename(it)
                showRename = false
            },
            onDismiss = { showRename = false }
        )
    }
    if (showDelete) {
        ResonanceDialog(
            title = stringResource(R.string.playlist_delete_title),
            text = stringResource(R.string.playlist_delete_body),
            confirmLabel = stringResource(R.string.playlist_delete),
            onConfirm = { viewModel.delete(onDeleted) },
            onDismiss = { showDelete = false },
            dismissLabel = stringResource(R.string.action_dismiss)
        )
    }
    if (showAddSongs && playlist != null) {
        val allSongs by viewModel.allSongs.collectAsStateWithLifecycle()
        SongPickerSheet(
            title = stringResource(R.string.playlist_add_songs_title, playlist.name),
            songs = allSongs,
            onConfirm = { ids -> viewModel.addSongs(ids) { showAddSongs = false } },
            onDismiss = { showAddSongs = false }
        )
    }
}

/** Saves a finished drag: from where the song started to where it ended up. */
private fun commitMove(song: Song, start: Int?, order: List<Song>, viewModel: PlaylistDetailViewModel, done: () -> Unit) {
    val end = order.indexOfFirst { it.id == song.id }
    if (start != null && end >= 0 && start != end) viewModel.move(start, end)
    done()
}

/** Cover on the left, name and actions on the right. In edit mode the cover and name are tappable. */
@Composable
private fun Header(
    playlist: Playlist?,
    songs: List<Song>,
    editing: Boolean,
    onPickCover: () -> Unit,
    onRemoveCover: () -> Unit,
    onRename: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit
) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    val coverScale by animateFloatAsState(if (editing) 0.94f else 1f, ResonanceTheme.motion.expressive(), label = "cover-edit")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 16.dp)
    ) {
        Box(
            Modifier
                .size(136.dp)
                .graphicsLayer {
                    scaleX = coverScale
                    scaleY = coverScale
                }
                .clip(RoundedCornerShape(20.dp))
                .then(if (editing) Modifier.pressClickable(onClickLabel = stringResource(R.string.playlist_cover_change), onClick = onPickCover) else Modifier)
        ) {
            PlaylistCover(playlist?.coverUri ?: songs.firstNotNullOfOrNull { it.artworkUri }, Modifier.fillMaxSize())
            val overlay by animateFloatAsState(if (editing) 1f else 0f, ResonanceTheme.motion.effects(), label = "cover-overlay")
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer { alpha = overlay }
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colors.background.copy(alpha = 0.7f))
            ) {
                Icon(Icons.Rounded.Image, contentDescription = null, tint = colors.textPrimary)
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                playlist?.name ?: stringResource(R.string.nav_playlists),
                style = typography.headlineMd,
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = if (editing) Modifier.pressClickable(onClickLabel = stringResource(R.string.playlist_rename), onClick = onRename) else Modifier
            )
            Text(
                pluralStringResource(R.plurals.music_song_count, songs.size, songs.size) + " · " +
                    formatDurationMs(songs.sumOf { it.durationMs }),
                style = typography.bodySm,
                color = colors.textSecondary
            )
            Spacer(Modifier.height(12.dp))
            if (editing) {
                Column {
                    TextButton(onClick = onRename, contentPadding = PaddingValues(horizontal = 0.dp)) {
                        Text(stringResource(R.string.playlist_rename))
                    }
                    if (playlist?.coverUri != null) {
                        TextButton(onClick = onRemoveCover, contentPadding = PaddingValues(horizontal = 0.dp)) {
                            Text(stringResource(R.string.playlist_cover_remove), color = colors.textSecondary)
                        }
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .height(44.dp)
                            .clip(CircleShape)
                            .background(if (songs.isNotEmpty()) colors.accent else colors.surfaceHighest)
                            .pressClickable(onClick = { if (songs.isNotEmpty()) onPlay() })
                            .padding(horizontal = 18.dp)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = colors.onAccent, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.cd_play), style = typography.labelLg, color = colors.onAccent)
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(colors.textPrimary.copy(alpha = 0.08f))
                            .pressClickable(onClickLabel = stringResource(R.string.cd_shuffle), onClick = { if (songs.isNotEmpty()) onShuffle() })
                    ) {
                        Icon(Icons.Rounded.Shuffle, contentDescription = stringResource(R.string.cd_shuffle), tint = colors.textPrimary, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}
