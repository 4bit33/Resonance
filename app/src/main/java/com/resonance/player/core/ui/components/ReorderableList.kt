package com.resonance.player.core.ui.components

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Long-press-drag reorder shared by Queue and playlist detail.
 * The dragged row follows the finger; a single move commits on release —
 * no live-displacement second queue, the engine stays authoritative.
 */
class ReorderScope internal constructor(
    private val listState: LazyListState,
    private val scope: CoroutineScope
) {
    var draggingIndex: Int? by mutableStateOf(null)
        private set
    var dragOffsetY: Float by mutableFloatStateOf(0f)
        private set

    /**
     * Drag-handle modifier for the row at [index]; commits via [onDrop]
     * with domain indices when the finger lifts over another row.
     */
    fun Modifier.dragHandle(index: Int, onDrop: (fromIndex: Int, toIndex: Int) -> Unit): Modifier =
        pointerInput(index) {
            detectDragGesturesAfterLongPress(
                onDragStart = {
                    draggingIndex = index
                    dragOffsetY = 0f
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    dragOffsetY += dragAmount.y
                    val edge = 96.dp.toPx()
                    val viewport = size.height.toFloat()
                    val fingerY = rowTop(index) + dragOffsetY
                    scope.launch {
                        if (fingerY < edge) {
                            listState.scrollBy(-(edge - fingerY))
                        } else if (fingerY > viewport - edge) {
                            listState.scrollBy(fingerY - (viewport - edge))
                        }
                    }
                },
                onDragEnd = {
                    val from = draggingIndex
                    val fingerY = rowTop(index) + dragOffsetY
                    val target = listState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                        fingerY >= item.offset && fingerY < item.offset + item.size
                    }?.index
                    draggingIndex = null
                    dragOffsetY = 0f
                    if (from != null && target != null && target != from) {
                        onDrop(from, target)
                    }
                },
                onDragCancel = {
                    draggingIndex = null
                    dragOffsetY = 0f
                }
            )
        }

    /** Follows the finger while dragging; z-raising happens on the item wrapper. */
    fun Modifier.dragged(index: Int): Modifier =
        this.graphicsLayer {
            translationY = if (draggingIndex == index) dragOffsetY else 0f
        }

    private fun rowTop(index: Int): Float =
        (listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }?.offset
            ?: 0).toFloat()
}

/**
 * LazyColumn wired for drag reorder: rows declare handles via the
 * [ReorderScope] receiver, drops commit domain indices through [onMove].
 */
@Composable
fun <T> ReorderableLazyColumn(
    items: List<T>,
    key: (T) -> Any,
    modifier: Modifier = Modifier,
    itemContent: @Composable ReorderScope.(item: T, index: Int) -> Unit
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val reorder = remember(listState, scope) { ReorderScope(listState, scope) }
    LazyColumn(state = listState, modifier = modifier) {
        itemsIndexed(items, key = { _, item -> key(item) }) { index, item ->
            // animateItem() springs siblings into place after a drop commits a
            // reorder (or after an insert/remove); zIndex here (not on the row)
            // raises the whole item above siblings while it's being dragged.
            Box(
                modifier = Modifier
                    .animateItem()
                    .zIndex(if (reorder.draggingIndex == index) 1f else 0f)
            ) {
                reorder.itemContent(item, index)
            }
        }
    }
}
