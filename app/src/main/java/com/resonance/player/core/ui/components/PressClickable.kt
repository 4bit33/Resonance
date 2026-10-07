package com.resonance.player.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ripple
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import com.resonance.player.core.ui.theme.ResonanceTheme

/**
 * Clickable that squeezes to [pressedScale] under the finger and springs back
 * on release, with the app's expressive motion. For cards and tiles.
 */
fun Modifier.pressClickable(
    pressedScale: Float = 0.96f,
    role: Role = Role.Button,
    onClickLabel: String? = null,
    onClick: () -> Unit
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) pressedScale else 1f,
        ResonanceTheme.motion.expressive(),
        label = "press-scale"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interaction,
            indication = ripple(),
            role = role,
            onClickLabel = onClickLabel,
            onClick = onClick
        )
}
