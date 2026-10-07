package com.resonance.player.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.resonance.player.R

/**
 * Type scale on Manrope (bundled variable font, SIL OFL, Latin + Cyrillic;
 * res/font/manrope_variable.ttf). Bundled, never fetched: the app stays
 * offline. Letter-spacing in sp.
 *
 * Durations/metrics use [monoMetric] with tabular figures ("tnum") so live
 * values (scrubber, badges, counters) never jitter.
 */
@OptIn(ExperimentalTextApi::class)
private fun manrope(weight: Int) = Font(
    R.font.manrope_variable,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight))
)

private val AppFont = FontFamily(
    manrope(400),
    manrope(500),
    manrope(600),
    manrope(700),
    manrope(800)
)
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
    /** M3-mapped scale so stock M3 components inherit the same metrics. */
    val material: Typography
)

fun appTypography(): ResonanceTypography {
    val displayLg = TextStyle(
        fontFamily = AppFont, fontWeight = FontWeight.ExtraBold,
        fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = (-0.72).sp
    )
    val displayLgMobile = TextStyle(
        fontFamily = AppFont, fontWeight = FontWeight.ExtraBold,
        fontSize = 30.sp, lineHeight = 38.sp, letterSpacing = (-0.6).sp
    )
    val headlineLg = TextStyle(
        fontFamily = AppFont, fontWeight = FontWeight.ExtraBold,
        fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.4).sp
    )
    val headlineMd = TextStyle(
        fontFamily = AppFont, fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp, lineHeight = 28.sp, letterSpacing = (-0.2).sp
    )
    val titleMd = TextStyle(
        fontFamily = AppFont, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.sp
    )
    val bodyLg = TextStyle(
        fontFamily = AppFont, fontWeight = FontWeight.Normal,
        fontSize = 15.sp, lineHeight = 22.sp, letterSpacing = 0.sp
    )
    val bodyMd = TextStyle(
        fontFamily = AppFont, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.sp
    )
    val bodySm = TextStyle(
        fontFamily = AppFont, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.12.sp
    )
    val labelLg = TextStyle(
        fontFamily = AppFont, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 0.28.sp
    )
    val labelMd = TextStyle(
        fontFamily = AppFont, fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.36.sp
    )
    val labelSm = TextStyle(
        fontFamily = AppFont, fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.44.sp
    )
    val monoMetric = TextStyle(
        fontFamily = AppFont, fontWeight = FontWeight.Medium,
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
