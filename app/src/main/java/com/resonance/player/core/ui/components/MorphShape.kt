package com.resonance.player.core.ui.components

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath

/**
 * Shapes for the play button, built with androidx.graphics.shapes (the
 * library Material 3's MaterialShapes is made with): a circle while paused,
 * a soft nine-sided "cookie" while playing. Same vertex count on both, so
 * the morph between them stays smooth.
 */
object PlayShapes {
    private val circle = RoundedPolygon.circle(numVertices = 9).normalized()
    private val cookie = RoundedPolygon.star(
        numVerticesPerRadius = 9,
        innerRadius = 0.84f,
        rounding = CornerRounding(radius = 0.32f, smoothing = 0.6f),
        innerRounding = CornerRounding(radius = 0.32f, smoothing = 0.6f)
    ).normalized()

    /** progress 0 = circle (paused), 1 = cookie (playing). */
    val playPause = Morph(circle, cookie)
}

/** A [Morph] frozen at [progress] and turned by [rotation] degrees, as a Compose [Shape]. */
class MorphShape(
    private val morph: Morph,
    private val progress: Float,
    private val rotation: Float = 0f
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = morph.toPath(progress.coerceIn(0f, 1f))
        val matrix = android.graphics.Matrix().apply {
            postScale(size.width, size.height)
            postRotate(rotation, size.width / 2f, size.height / 2f)
        }
        path.transform(matrix)
        return Outline.Generic(path.asComposePath())
    }
}
