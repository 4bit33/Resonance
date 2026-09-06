package com.resonance.player.playback

import androidx.media3.common.Player

/**
 * Playback-completion semantics for history bookkeeping (pure, tested):
 * only a natural auto-transition means the track was heard through;
 * user skips, seeks, removals and internal jumps do not.
 */
fun transitionCompleted(@Player.DiscontinuityReason reason: Int): Boolean =
    reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION
