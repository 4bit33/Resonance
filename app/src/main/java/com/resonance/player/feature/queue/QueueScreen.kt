package com.resonance.player.feature.queue

import sh.calvin.reorderable.rememberReorderableLazyListState
import sh.calvin.reorderable.ReorderableItem
import com.resonance.player.core.ui.components.ResonanceSectionHeader
import com.resonance.player.core.ui.components.EqualizerBars
import com.resonance.player.core.model.QueueItem
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.resonance.player.R
import com.resonance.player.core.common.formatDurationMs
import com.resonance.player.core.model.ShuffleMode
import com.resonance.player.core.ui.components.ArtworkImage
import com.resonance.player.core.ui.components.ReorderableLazyColumn
import com.resonance.player.core.ui.components.ResonanceCompactSongRow
import com.resonance.player.core.ui.components.ResonanceDialog
import com.resonance.player.core.ui.components.ResonanceEmptyState
import com.resonance.player.core.ui.components.ResonanceTopBar
import com.resonance.player.core.ui.theme.ResonanceTheme

/**
 * Up next: the playing song on top, then only what comes after it. Drag a
 * row (long-press or the handle) to reorder, the cross removes it, the top
 * bar clears the queue. Moves are saved when the finger lifts.
 */
@Composable
fun QueueScreen(viewModel: QueueViewModel, onBack: () -> Unit) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    var confirmClear by remember { mutableStateOf(false) }
    val current = snapshot.queue.getOrNull(snapshot.queueIndex)
    val base = snapshot.queueIndex + 1
    val upcoming = snapshot.queue.drop(base)
    var order by remember { mutableStateOf(upcoming) }
    var dragFrom by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(upcoming) { if (dragFrom == null) order = upcoming }
    val listState = rememberLazyListState()
    val reorder = rememberReorderableLazyListState(listState) { from, to ->
        val a = order.indexOfFirst { it.queueId == from.key }
        val b = order.indexOfFirst { it.queueId == to.key }
        if (a >= 0 && b >= 0) {
            order = order.toMutableList().apply { add(b, removeAt(a)) }
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
    fun commit(item: QueueItem) {
        val start = dragFrom
        val end = order.indexOfFirst { it.queueId == item.queueId }
        dragFrom = null
        if (start != null && end >= 0 && start != end) viewModel.move(base + start, base + end)
    }

    Column(Modifier.fillMaxSize()) {
        ResonanceTopBar(
            title = stringResource(R.string.nav_queue),
            onBack = onBack,
            trailing = {
                if (upcoming.isNotEmpty()) {
                    TextButton(onClick = { confirmClear = true }) { Text(stringResource(R.string.queue_clear)) }
                }
            }
        )
        if (snapshot.queue.isEmpty()) {
            ResonanceEmptyState(
                title = stringResource(R.string.nav_queue),
                body = stringResource(R.string.queue_empty),
                icon = Icons.AutoMirrored.Rounded.QueueMusic
            )
            return@Column
        }
        LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 24.dp), modifier = Modifier.fillMaxSize()) {
            if (current != null) {
                item(key = "now") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(colors.accent.copy(alpha = 0.12f))
                            .padding(12.dp)
                    ) {
                        Box(Modifier.size(56.dp).clip(RoundedCornerShape(12.dp))) {
                            ArtworkImage(artworkUri = current.song.artworkUri, contentDescription = null, modifier = Modifier.fillMaxSize())
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.nav_player).uppercase(), style = typography.labelSm, color = colors.accent)
                            Text(current.song.title, style = typography.titleMd, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(current.song.artistName, style = typography.bodySm, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        EqualizerBars(isPlaying = snapshot.isPlaying, modifier = Modifier.size(20.dp))
                    }
                }
            }
            if (upcoming.isNotEmpty()) {
                item(key = "next-header") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ResonanceSectionHeader(
                            title = stringResource(R.string.queue_up_next_count, upcoming.size),
                            modifier = Modifier.weight(1f)
                        )
                        if (snapshot.shuffle == ShuffleMode.ON) {
                            Icon(
                                Icons.Rounded.Shuffle,
                                contentDescription = stringResource(R.string.cd_shuffle),
                                tint = colors.accent,
                                modifier = Modifier.padding(end = 20.dp).size(20.dp)
                            )
                        }
                    }
                }
            }
            itemsIndexed(order, key = { _, item -> item.queueId }) { index, item ->
                ReorderableItem(reorder, key = item.queueId) { dragging ->
                    val lift by animateDpAsState(if (dragging) 10.dp else 0.dp, ResonanceTheme.motion.spatialFast(), label = "q-lift")
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(lift, RoundedCornerShape(14.dp))
                            .background(if (dragging) colors.surfaceHigh else colors.background)
                            .longPressDraggableHandle(
                                onDragStarted = {
                                    dragFrom = index
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDragStopped = { commit(item) }
                            )
                            .clickable { viewModel.playAt(base + index) }
                            .padding(start = 20.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)
                    ) {
                        Box(Modifier.size(48.dp).clip(RoundedCornerShape(10.dp))) {
                            ArtworkImage(artworkUri = item.song.artworkUri, contentDescription = null, modifier = Modifier.fillMaxSize())
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.song.title, style = typography.titleMd, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(item.song.artistName, style = typography.bodySm, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = { viewModel.remove(base + index) }) {
                            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.queue_remove), tint = colors.textSecondary)
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
                                        dragFrom = index
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onDragStopped = { commit(item) }
                                )
                        )
                    }
                }
            }
        }
    }
    if (confirmClear) {
        ResonanceDialog(
            title = stringResource(R.string.queue_clear_title),
            text = stringResource(R.string.queue_clear_body),
            confirmLabel = stringResource(R.string.queue_clear),
            onConfirm = {
                viewModel.clear()
                confirmClear = false
            },
            onDismiss = { confirmClear = false },
            dismissLabel = stringResource(R.string.action_dismiss)
        )
    }
}

