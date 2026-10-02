package com.resonance.player.core.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable

/**
 * One motion vocabulary for the whole app, so every animation feels related.
 *
 * - spatial: things that move or resize (sheets, artwork, the play button
 *   shape). Critically damped: arrives fast, never wobbles.
 * - expressive: the few places where a small overshoot means something
 *   (a press releasing, the play button morph).
 * - effects: color, alpha. No overshoot ever (a color must not "bounce").
 *
 * [speed] comes from the user's setting: 1 = default, 2 = twice as fast,
 * 0 = animations off (everything snaps).
 */
@Immutable
data class ResonanceMotion(val speed: Float = 1f) {
    val enabled: Boolean get() = speed > 0f

    /** Stiffness scales with speed squared: a spring's duration goes as 1/sqrt(stiffness). */
    private fun stiffness(base: Float): Float = base * speed * speed

    fun <T> spatial(): FiniteAnimationSpec<T> =
        if (enabled) spring(Spring.DampingRatioNoBouncy, stiffness(Spring.StiffnessMediumLow)) else snap()

    fun <T> spatialFast(): FiniteAnimationSpec<T> =
        if (enabled) spring(Spring.DampingRatioNoBouncy, stiffness(Spring.StiffnessMedium)) else snap()

    fun <T> expressive(): FiniteAnimationSpec<T> =
        if (enabled) spring(0.72f, stiffness(Spring.StiffnessMediumLow)) else snap()

    fun <T> effects(): FiniteAnimationSpec<T> =
        if (enabled) spring(Spring.DampingRatioNoBouncy, stiffness(Spring.StiffnessLow)) else snap()

    /** Slow color handover when the artwork (and so the whole palette) changes. */
    fun <T> colorChange(): FiniteAnimationSpec<T> = duration(900)

    fun <T> duration(millis: Int): FiniteAnimationSpec<T> =
        if (enabled) tween((millis / speed).toInt(), easing = FastOutSlowInEasing) else snap()

    fun millis(base: Int): Int = if (enabled) (base / speed).toInt() else 0
}
