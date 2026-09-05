package com.mankeu.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/**
 * The whole colour system, generated from one seed.
 *
 * `MankeuSeedBlue` is the brand blue seed (0xFF0061A4).
 * Every token below was produced from it by the Material 3 algorithm — Material Color Utilities,
 * `SchemeTonalSpot`, 2021 spec, contrast level 0 — rather than picked by eye.
 *
 * These are constants, not a runtime generator. The seed never changes at runtime, so generating
 * the palettes on every launch would cost startup time to produce the same numbers; keeping them as
 * literals also means every value is reviewable in a diff. Regenerate with
 * `docs/evidence/theme/generate_palette.py` if the seed ever moves, and re-run the contrast audit in
 * the same file rather than adjusting a token by hand.
 *
 * Every pair the app actually draws was measured with the WCAG 2.1 relative-luminance formula:
 * all passing (4.5:1 for text, 3:1 for control boundaries and chart fills).
 */
val MankeuSeedBlue = Color(0xFF0061A4)

private val MankeuLightColors = lightColorScheme(
    primary = Color(0xFF36618E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF1A4975),
    inversePrimary = Color(0xFFA0CAFD),
    secondary = Color(0xFF535F70),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD7E3F8),
    onSecondaryContainer = Color(0xFF3B4858),
    tertiary = Color(0xFF6B5778),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF3DAFF),
    onTertiaryContainer = Color(0xFF523F5F),
    background = Color(0xFFF8F9FF),
    onBackground = Color(0xFF191C20),
    surface = Color(0xFFF8F9FF),
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFDFE2EB),
    onSurfaceVariant = Color(0xFF43474E),
    surfaceTint = Color(0xFF36618E),
    inverseSurface = Color(0xFF2E3135),
    inverseOnSurface = Color(0xFFEFF0F7),
    surfaceBright = Color(0xFFF8F9FF),
    surfaceDim = Color(0xFFD8DAE0),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF2F3FA),
    surfaceContainer = Color(0xFFECEEF4),
    surfaceContainerHigh = Color(0xFFE6E8EE),
    surfaceContainerHighest = Color(0xFFE1E2E8),
    outline = Color(0xFF73777F),
    outlineVariant = Color(0xFFC3C6CF),
    scrim = Color(0xFF000000),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    primaryFixed = Color(0xFFD1E4FF),
    primaryFixedDim = Color(0xFFA0CAFD),
    onPrimaryFixed = Color(0xFF001D36),
    onPrimaryFixedVariant = Color(0xFF1A4975),
    secondaryFixed = Color(0xFFD7E3F8),
    secondaryFixedDim = Color(0xFFBBC7DB),
    onSecondaryFixed = Color(0xFF101C2B),
    onSecondaryFixedVariant = Color(0xFF3B4858),
    tertiaryFixed = Color(0xFFF3DAFF),
    tertiaryFixedDim = Color(0xFFD7BEE4),
    onTertiaryFixed = Color(0xFF251431),
    onTertiaryFixedVariant = Color(0xFF523F5F),
)

private val MankeuDarkColors = darkColorScheme(
    primary = Color(0xFFA0CAFD),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF1A4975),
    onPrimaryContainer = Color(0xFFD1E4FF),
    inversePrimary = Color(0xFF36618E),
    secondary = Color(0xFFBBC7DB),
    onSecondary = Color(0xFF253140),
    secondaryContainer = Color(0xFF3B4858),
    onSecondaryContainer = Color(0xFFD7E3F8),
    tertiary = Color(0xFFD7BEE4),
    onTertiary = Color(0xFF3B2948),
    tertiaryContainer = Color(0xFF523F5F),
    onTertiaryContainer = Color(0xFFF3DAFF),
    background = Color(0xFF111418),
    onBackground = Color(0xFFE1E2E8),
    surface = Color(0xFF111418),
    onSurface = Color(0xFFE1E2E8),
    surfaceVariant = Color(0xFF43474E),
    onSurfaceVariant = Color(0xFFC3C6CF),
    surfaceTint = Color(0xFFA0CAFD),
    inverseSurface = Color(0xFFE1E2E8),
    inverseOnSurface = Color(0xFF2E3135),
    surfaceBright = Color(0xFF36393E),
    surfaceDim = Color(0xFF111418),
    surfaceContainerLowest = Color(0xFF0B0E13),
    surfaceContainerLow = Color(0xFF191C20),
    surfaceContainer = Color(0xFF1D2024),
    surfaceContainerHigh = Color(0xFF272A2F),
    surfaceContainerHighest = Color(0xFF32353A),
    outline = Color(0xFF8D9199),
    outlineVariant = Color(0xFF43474E),
    scrim = Color(0xFF000000),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    primaryFixed = Color(0xFFD1E4FF),
    primaryFixedDim = Color(0xFFA0CAFD),
    onPrimaryFixed = Color(0xFF001D36),
    onPrimaryFixedVariant = Color(0xFF1A4975),
    secondaryFixed = Color(0xFFD7E3F8),
    secondaryFixedDim = Color(0xFFBBC7DB),
    onSecondaryFixed = Color(0xFF101C2B),
    onSecondaryFixedVariant = Color(0xFF3B4858),
    tertiaryFixed = Color(0xFFF3DAFF),
    tertiaryFixedDim = Color(0xFFD7BEE4),
    onTertiaryFixed = Color(0xFF251431),
    onTertiaryFixedVariant = Color(0xFF523F5F),
)

data class MankeuSemanticColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val danger: Color,
    val onDanger: Color,
    val dangerContainer: Color,
    val onDangerContainer: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val chart: List<Color>,
)

private val MankeuLightSemanticColors = MankeuSemanticColors(
    success = Color(0xFF006B5A),
    onSuccess = Color(0xFFFFFFFF),
    successContainer = Color(0xFF80F7DB),
    onSuccessContainer = Color(0xFF00201A),
    danger = Color(0xFF9A442D),
    onDanger = Color(0xFFFFFFFF),
    dangerContainer = Color(0xFFFFDBD2),
    onDangerContainer = Color(0xFF3C0800),
    warningContainer = Color(0xFFE1E991),
    onWarningContainer = Color(0xFF1A1D00),
    chart = listOf(
        Color(0xFF006D43),
        Color(0xFFB72122),
        Color(0xFF005FAD),
        Color(0xFF6D43C6),
        Color(0xFF00677F),
        Color(0xFFB4047E),
    ),
)

private val MankeuDarkSemanticColors = MankeuSemanticColors(
    success = Color(0xFF62DABF),
    onSuccess = Color(0xFF00382E),
    successContainer = Color(0xFF005143),
    onSuccessContainer = Color(0xFF80F7DB),
    danger = Color(0xFFFFB4A1),
    onDanger = Color(0xFF5D1805),
    dangerContainer = Color(0xFF7C2E19),
    onDangerContainer = Color(0xFFFFDBD2),
    warningContainer = Color(0xFF444B01),
    onWarningContainer = Color(0xFFE1E991),
    chart = listOf(
        Color(0xFF68CC94),
        Color(0xFFFF9F96),
        Color(0xFF8BBBFF),
        Color(0xFFC5ABFF),
        Color(0xFF6DC4E1),
        Color(0xFFFF98CE),
    ),
)

val LocalMankeuSemanticColors = androidx.compose.runtime.staticCompositionLocalOf { MankeuLightSemanticColors }

private val MankeuShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(20.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

data class MankeuMotion(
    val enabled: Boolean,
    val fastMillis: Int,
    val mediumMillis: Int,
) {
    companion object {
        val Full = MankeuMotion(enabled = true, fastMillis = 180, mediumMillis = 320)
        val Reduced = MankeuMotion(enabled = true, fastMillis = 90, mediumMillis = 140)
        val Off = MankeuMotion(enabled = false, fastMillis = 0, mediumMillis = 0)

        fun of(mode: MotionMode): MankeuMotion = when (mode) {
            MotionMode.FULL -> Full
            MotionMode.REDUCED -> Reduced
            MotionMode.OFF -> Off
        }

        fun resolve(mode: MotionMode, systemScale: Float): MankeuMotion = when {
            systemScale <= 0f -> Off
            systemScale < 1f -> of(mode).let { if (it == Full) Reduced else it }
            else -> of(mode)
        }
    }
}

val LocalMotion = androidx.compose.runtime.staticCompositionLocalOf { MankeuMotion.Full }

@Composable
fun isDarkScheme(): Boolean = MaterialTheme.colorScheme.surface.luminance() < 0.5f

@Composable
private fun animatedColorScheme(target: ColorScheme, motion: MankeuMotion): ColorScheme {
    val spec = tween<Color>(durationMillis = maxOf(motion.mediumMillis, 1), easing = FastOutSlowInEasing)

    @Composable
    fun shade(value: Color, label: String): Color = animateColorAsState(value, spec, label = label).value

    return target.copy(
        primary = shade(target.primary, "primary"),
        onPrimary = shade(target.onPrimary, "onPrimary"),
        primaryContainer = shade(target.primaryContainer, "primaryContainer"),
        onPrimaryContainer = shade(target.onPrimaryContainer, "onPrimaryContainer"),
        inversePrimary = shade(target.inversePrimary, "inversePrimary"),
        secondary = shade(target.secondary, "secondary"),
        onSecondary = shade(target.onSecondary, "onSecondary"),
        secondaryContainer = shade(target.secondaryContainer, "secondaryContainer"),
        onSecondaryContainer = shade(target.onSecondaryContainer, "onSecondaryContainer"),
        tertiary = shade(target.tertiary, "tertiary"),
        onTertiary = shade(target.onTertiary, "onTertiary"),
        tertiaryContainer = shade(target.tertiaryContainer, "tertiaryContainer"),
        onTertiaryContainer = shade(target.onTertiaryContainer, "onTertiaryContainer"),
        background = shade(target.background, "background"),
        onBackground = shade(target.onBackground, "onBackground"),
        surface = shade(target.surface, "surface"),
        onSurface = shade(target.onSurface, "onSurface"),
        surfaceVariant = shade(target.surfaceVariant, "surfaceVariant"),
        onSurfaceVariant = shade(target.onSurfaceVariant, "onSurfaceVariant"),
        surfaceTint = shade(target.surfaceTint, "surfaceTint"),
        inverseSurface = shade(target.inverseSurface, "inverseSurface"),
        inverseOnSurface = shade(target.inverseOnSurface, "inverseOnSurface"),
        error = shade(target.error, "error"),
        onError = shade(target.onError, "onError"),
        errorContainer = shade(target.errorContainer, "errorContainer"),
        onErrorContainer = shade(target.onErrorContainer, "onErrorContainer"),
        outline = shade(target.outline, "outline"),
        outlineVariant = shade(target.outlineVariant, "outlineVariant"),
        surfaceBright = shade(target.surfaceBright, "surfaceBright"),
        surfaceDim = shade(target.surfaceDim, "surfaceDim"),
        surfaceContainerLowest = shade(target.surfaceContainerLowest, "surfaceContainerLowest"),
        surfaceContainerLow = shade(target.surfaceContainerLow, "surfaceContainerLow"),
        surfaceContainer = shade(target.surfaceContainer, "surfaceContainer"),
        surfaceContainerHigh = shade(target.surfaceContainerHigh, "surfaceContainerHigh"),
        surfaceContainerHighest = shade(target.surfaceContainerHighest, "surfaceContainerHighest"),
    )
}

@Composable
private fun animatedSemanticColors(target: MankeuSemanticColors, motion: MankeuMotion): MankeuSemanticColors {
    val spec = tween<Color>(durationMillis = maxOf(motion.mediumMillis, 1), easing = FastOutSlowInEasing)

    @Composable
    fun shade(value: Color, label: String): Color = animateColorAsState(value, spec, label = label).value

    return MankeuSemanticColors(
        success = shade(target.success, "success"),
        onSuccess = shade(target.onSuccess, "onSuccess"),
        successContainer = shade(target.successContainer, "successContainer"),
        onSuccessContainer = shade(target.onSuccessContainer, "onSuccessContainer"),
        danger = shade(target.danger, "danger"),
        onDanger = shade(target.onDanger, "onDanger"),
        dangerContainer = shade(target.dangerContainer, "dangerContainer"),
        onDangerContainer = shade(target.onDangerContainer, "onDangerContainer"),
        warningContainer = shade(target.warningContainer, "warningContainer"),
        onWarningContainer = shade(target.onWarningContainer, "onWarningContainer"),
        chart = target.chart.mapIndexed { index, color -> shade(color, "chart$index") },
    )
}

@Composable
fun MankeuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    motion: MankeuMotion = MankeuMotion.Full,
    content: @Composable () -> Unit,
) {
    val targetScheme = if (darkTheme) MankeuDarkColors else MankeuLightColors
    val targetSemantic = if (darkTheme) MankeuDarkSemanticColors else MankeuLightSemanticColors
    val scheme = animatedColorScheme(targetScheme, motion)
    val semanticColors = animatedSemanticColors(targetSemantic, motion)
    androidx.compose.runtime.CompositionLocalProvider(
        LocalMankeuSemanticColors provides semanticColors,
        LocalMotion provides motion,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            shapes = MankeuShapes,
            content = content,
        )
    }
}
