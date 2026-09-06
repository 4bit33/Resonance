package com.resonance.player.core.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Stitch 4px-grid spacing scale (DESIGN.md). Screens use these + semantic paddings, never raw dp. */
@Immutable
data class ResonanceSpacing(
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 20.dp,
    val xxl: Dp = 24.dp,
    val xxxl: Dp = 32.dp,
    val xxxxl: Dp = 40.dp,
    val xxxxxl: Dp = 48.dp,
    val giant: Dp = 64.dp,
    /** Primary horizontal screen gutter (24dp on foldable/tablet outer gutters). */
    val screenGutter: Dp = 16.dp,
    val screenGutterWide: Dp = 24.dp,
    val sheetGutter: Dp = 20.dp,
    val touchMin: Dp = 48.dp,
    val sectionSpacing: Dp = 24.dp,
    val cardPadding: Dp = 16.dp,
    val rowSpacing: Dp = 4.dp
)

/** Stitch functional radius hierarchy (DESIGN.md). Curvature follows scale + interaction category. */
@Immutable
data class ResonanceRadii(
    /** Controls, rows, artwork thumbnails, chips, inputs. */
    val control: RoundedCornerShape = RoundedCornerShape(8.dp),
    /** Cards, grid tiles, mini player, sheet content. */
    val card: RoundedCornerShape = RoundedCornerShape(16.dp),
    /** Modal sheets/overlays: top edge only. */
    val sheetTop: RoundedCornerShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    /** Circular controls, play buttons, thumbs, avatars. */
    val full: RoundedCornerShape = CircleShape,
    val controlDp: Dp = 8.dp,
    val cardDp: Dp = 16.dp
)

/** Fixed Stitch component metrics (dock, mini player, insets, rows, art). */
@Immutable
data class ResonanceDimensions(
    val navigationDockHeight: Dp = 68.dp,
    val miniPlayerHeight: Dp = 64.dp,
    /** 68 + 64 + 16: content bottom inset under dock + mini player. */
    val contentBottomInset: Dp = 148.dp,
    val topBarHeight: Dp = 56.dp,
    val songRowHeight: Dp = 64.dp,
    val compactRowHeight: Dp = 48.dp,
    val songArtwork: Dp = 48.dp,
    val miniPlayerArtwork: Dp = 40.dp,
    val primaryPlayButton: Dp = 64.dp,
    val albumCardWidth: Dp = 160.dp,
    val scrubTrack: Dp = 4.dp,
    val scrubTrackTouched: Dp = 6.dp,
    val scrubThumb: Dp = 14.dp,
    val miniProgress: Dp = 2.dp,
    val navIcon: Dp = 24.dp
)
