package com.resonance.player.core.ui.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlin.math.abs

enum class SwipeDirection { Left, Right, Up, Down }

/**
 * Pure rule for a finished drag: the dominant axis wins and must travel at
 * least [thresholdPx]; anything shorter (or a perfect diagonal) is not a swipe,
 * so taps and small wobbles never trigger an action.
 */
fun swipeDirection(dx: Float, dy: Float, thresholdPx: Float): SwipeDirection? = when {
    abs(dx) > abs(dy) && abs(dx) >= thresholdPx -> if (dx < 0f) SwipeDirection.Left else SwipeDirection.Right
    abs(dy) > abs(dx) && abs(dy) >= thresholdPx -> if (dy < 0f) SwipeDirection.Up else SwipeDirection.Down
    else -> null
}

/**
 * Threshold swipes (no finger-tracking animation). With no vertical handler
 * only horizontal drags are claimed, so a vertical scroll or pull on the same
 * element keeps working (Now Playing artwork).
 */
fun Modifier.onSwipe(
    onLeft: (() -> Unit)? = null,
    onRight: (() -> Unit)? = null,
    onUp: (() -> Unit)? = null
): Modifier = composed {
    // Read through updated state: the handlers change on recomposition and a restarted
    // detector would cancel a drag in progress (the mini player recomposes every tick).
    val left by rememberUpdatedState(onLeft)
    val right by rememberUpdatedState(onRight)
    val up by rememberUpdatedState(onUp)
    val horizontalOnly = onUp == null
    pointerInput(horizontalOnly) {
        val threshold = SWIPE_DISTANCE.toPx()
        var dx = 0f
        var dy = 0f
        val finish = {
            when (swipeDirection(dx, dy, threshold)) {
                SwipeDirection.Left -> left?.invoke()
                SwipeDirection.Right -> right?.invoke()
                SwipeDirection.Up -> up?.invoke()
                else -> Unit
            }
        }
        if (horizontalOnly) {
            detectHorizontalDragGestures(
                onDragStart = { dx = 0f },
                onDragEnd = { finish() },
                onHorizontalDrag = { _, amount -> dx += amount }
            )
        } else {
            detectDragGestures(
                onDragStart = { dx = 0f; dy = 0f },
                onDragEnd = { finish() },
                onDrag = { _, amount -> dx += amount.x; dy += amount.y }
            )
        }
    }
}

private val SWIPE_DISTANCE = 64.dp

/**
 * Pure rule for "pull down to close": a long enough pull, or a short pull with
 * a quick flick. Velocity alone never closes (a fast scroll back to the top of
 * a long screen must not dismiss it).
 */
fun shouldDismiss(pulledPx: Float, velocityY: Float, pullPx: Float, flickPx: Float, flickVelocity: Float): Boolean =
    pulledPx >= pullPx || (pulledPx >= flickPx && velocityY >= flickVelocity)

/**
 * Nested-scroll connection that turns leftover downward drag (the child cannot
 * scroll up any further, or does not scroll at all) into a close gesture. It
 * consumes nothing, so the child's own scrolling is untouched.
 */
class PullDownToDismiss(
    private val pullPx: Float,
    private val flickPx: Float,
    private val flickVelocity: Float,
    private val onDismiss: () -> Unit
) : NestedScrollConnection {
    private var pulled = 0f

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (source == NestedScrollSource.UserInput) pulled = (pulled + available.y).coerceAtLeast(0f)
        return Offset.Zero
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        val dismiss = shouldDismiss(pulled, available.y, pullPx, flickPx, flickVelocity)
        pulled = 0f
        if (dismiss) onDismiss()
        return Velocity.Zero
    }
}
