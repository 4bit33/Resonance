package com.resonance.player.core.ui.components

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

/** Pieces that fly from the mini player to their place in Now Playing when it opens. */
enum class PlayerAnchor { Art, Title, Artist, Play, Next }

/** Where each [PlayerAnchor] sits in the mini player and in Now Playing (root coordinates). */
@Stable
class PlayerAnchors {
    val mini = mutableStateMapOf<PlayerAnchor, Rect>()
    val big = mutableStateMapOf<PlayerAnchor, Rect>()
}

/** Reports this element's bounds in root coordinates as [anchor]. */
fun Modifier.playerAnchor(anchor: PlayerAnchor, report: (PlayerAnchor, Rect) -> Unit): Modifier =
    onGloballyPositioned { report(anchor, it.boundsInRoot()) }
