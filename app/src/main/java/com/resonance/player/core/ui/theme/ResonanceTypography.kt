package com.resonance.player.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Stitch type scale (DESIGN.md). Stitch specifies Inter; no font is bundled
 * (runtime fetching would violate offline-first), so the system sans-serif
 * is used with identical metrics until Inter OFL files are added
 * (see ARCHITECTURE.md font decision). Letter-spacing converted em -> sp.
 *
 * Durations/metrics use [monoMetric] with tabular figures ("tnum") so live
 * values (scrubber, badges, counters) never jitter.
 */
private val StitchFont = FontFamily.Default
private const val TNUM = "tnum"

@Immutable
data class ResonanceTypography(
    val displayLg: TextStyle,
    val displayLgMobile: TextStyle,
    val headlineLg: TextStyle,
    val headlineMd: TextStyle,
    val titleMd: TextStyle,
    val bodyLg: TextStyle,
    val bodyMd: TextStyle,
    val bodySm: TextStyle,
    val labelLg: TextStyle,
    val labelMd: TextStyle,
    val labelSm: TextStyle,
    val monoMetric: TextStyle,
    /** M3-mapped scale so stock M3 components inherit Stitch metrics. */
    val material: Typography
)

fun stitchTypography(): ResonanceTypography {
    val displayLg = TextStyle(
        fontFamily = StitchFont, fontWeight = FontWeight.Bold,
        fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = (-0.72).sp
    )
    val displayLgMobile = TextStyle(
        fontFamily = StitchFont, fontWeight = FontWeight.Bold,
        fontSize = 30.sp, lineHeight = 38.sp, letterSpacing = (-0.6).sp
    )
    val headlineLg = TextStyle(
        fontFamily = StitchFont, fontWeight = FontWeight.Bold,
        fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = (-0.24).sp
    )
    val headlineMd = TextStyle(
        fontFamily = StitchFont, fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp, lineHeight = 28.sp, letterSpacing = (-0.2).sp
    )
    val titleMd = TextStyle(
        fontFamily = StitchFont, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.sp
    )
    val bodyLg = TextStyle(
        fontFamily = StitchFont, fontWeight = FontWeight.Normal,
        fontSize = 15.sp, lineHeight = 22.sp, letterSpacing = 0.sp
    )
    val bodyMd = TextStyle(
        fontFamily = StitchFont, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.sp
    )
    val bodySm = TextStyle(
        fontFamily = StitchFont, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.12.sp
    )
    val labelLg = TextStyle(
        fontFamily = StitchFont, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 0.28.sp
    )
    val labelMd = TextStyle(
        fontFamily = StitchFont, fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.36.sp
    )
    val labelSm = TextStyle(
        fontFamily = StitchFont, fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.44.sp
    )
    val monoMetric = TextStyle(
        fontFamily = StitchFont, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.6.sp,
        fontFeatureSettings = TNUM
    )
    return ResonanceTypography(
        displayLg = displayLg,
        displayLgMobile = displayLgMobile,
        headlineLg = headlineLg,
        headlineMd = headlineMd,
        titleMd = titleMd,
        bodyLg = bodyLg,
        bodyMd = bodyMd,
        bodySm = bodySm,
        labelLg = labelLg,
        labelMd = labelMd,
        labelSm = labelSm,
        monoMetric = monoMetric,
        material = Typography(
            displayLarge = displayLg,
            displayMedium = displayLgMobile,
            displaySmall = headlineLg,
            headlineLarge = headlineLg,
            headlineMedium = headlineMd,
            headlineSmall = headlineMd,
            titleLarge = titleMd,
            titleMedium = titleMd,
            titleSmall = labelLg,
            bodyLarge = bodyLg,
            bodyMedium = bodyMd,
            bodySmall = bodySm,
            labelLarge = labelLg,
            labelMedium = labelMd,
            labelSmall = labelSm
        )
    )
}
