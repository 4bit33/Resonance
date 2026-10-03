package com.resonance.player.core.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable

/**
 * One motion vocabulary for the whole app, on the Material 3 Expressive
 * motion scheme (spring values from material3's ExpressiveMotionTokens; the
 * scheme itself is not public in material3 1.4).
 *
 * - spatial: things that move or resize. A small overshoot, as in M3 Expressive.
 * - spatialFast / expressive: small, quick moves (press feedback, toggles, morphs).
 * - settle: a move that must not overshoot, e.g. Now Playing reaching the screen edge.
 * - effects: color and alpha. Never overshoots.
 *
 * [speed] comes from the user's setting: 1 = default, 2 = twice as fast,
 * 0 = animations off (everything snaps).
 */
@Immutable
data class ResonanceMotion(val speed: Float = 1f) {
    val enabled: Boolean get() = speed > 0f

    /** Stiffness scales with speed squared: a spring's duration goes as 1/sqrt(stiffness). */
    private fun stiffness(base: Float): Float = base * speed * speed

    private fun <T> springOrSnap(damping: Float, stiffness: Float): FiniteAnimationSpec<T> =
        if (enabled) spring(damping, stiffness(stiffness)) else snap()

    fun <T> spatial(): FiniteAnimationSpec<T> = springOrSnap(0.8f, 380f)

    fun <T> spatialFast(): FiniteAnimationSpec<T> = springOrSnap(0.6f, 800f)

    fun <T> spatialSlow(): FiniteAnimationSpec<T> = springOrSnap(0.8f, 200f)

    fun <T> expressive(): FiniteAnimationSpec<T> = springOrSnap(0.6f, 800f)

    fun <T> settle(): FiniteAnimationSpec<T> = springOrSnap(1f, 380f)

    fun <T> effects(): FiniteAnimationSpec<T> = springOrSnap(1f, 1600f)

    fun <T> effectsFast(): FiniteAnimationSpec<T> = springOrSnap(1f, 3800f)

    /** Slow color handover when the artwork (and so the whole palette) changes. */
    fun <T> colorChange(): FiniteAnimationSpec<T> = duration(900)

    fun <T> duration(millis: Int): FiniteAnimationSpec<T> =
        if (enabled) tween((millis / speed).toInt(), easing = FastOutSlowInEasing) else snap()

    fun millis(base: Int): Int = if (enabled) (base / speed).toInt() else 0
}
