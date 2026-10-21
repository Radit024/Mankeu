package com.mankeu.presentation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/**
 * Arina brand roles mapped from font-dan-color-palette.md (DEC-019).
 *
 * Supersedes the generated blue seed scheme. Brand fills and surfaces follow the supplied
 * reference. Text on a light surface uses dark green / terracotta instead of the raw success
 * and error main colors, which fail 4.5:1 on white. See scripts/verify_visual_system.py.
 */
internal val MankeuLightColors = lightColorScheme(
    primary = Color(0xFF2D6A4F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD8F3DC),
    onPrimaryContainer = Color(0xFF1B4332),
    inversePrimary = Color(0xFF52B788),
    secondary = Color(0xFFE07A5F),
    onSecondary = Color(0xFF2C2A29),
    secondaryContainer = Color(0xFFFCDACF),
    onSecondaryContainer = Color(0xFF713A28),
    tertiary = Color(0xFF4B7A56),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE1EFE4),
    onTertiaryContainer = Color(0xFF284B30),
    background = Color(0xFFFAFAF8),
    onBackground = Color(0xFF2C2A29),
    surface = Color(0xFFFAFAF8),
    onSurface = Color(0xFF2C2A29),
    surfaceVariant = Color(0xFFEBEBE6),
    onSurfaceVariant = Color(0xFF6B6866),
    surfaceTint = Color(0xFF2D6A4F),
    inverseSurface = Color(0xFF2C2A29),
    inverseOnSurface = Color(0xFFFAFAF8),
    surfaceBright = Color(0xFFFAFAF8),
    surfaceDim = Color(0xFFE6E6DF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFF3F3ED),
    surfaceContainerHigh = Color(0xFFEFEFE9),
    surfaceContainerHighest = Color(0xFFEBEBE6),
    outline = Color(0xFF827D78),
    outlineVariant = Color(0xFFEBEBE6),
    scrim = Color(0xFF000000),
    error = Color(0xFF9A442D),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFCDACF),
    onErrorContainer = Color(0xFF713A28),
    primaryFixed = Color(0xFFD8F3DC),
    primaryFixedDim = Color(0xFF52B788),
    onPrimaryFixed = Color(0xFF10251B),
    onPrimaryFixedVariant = Color(0xFF1B4332),
    secondaryFixed = Color(0xFFFCDACF),
    secondaryFixedDim = Color(0xFFD4836A),
    onSecondaryFixed = Color(0xFF2C2A29),
    onSecondaryFixedVariant = Color(0xFF713A28),
    tertiaryFixed = Color(0xFFE1EFE4),
    tertiaryFixedDim = Color(0xFF7DB88A),
    onTertiaryFixed = Color(0xFF142A19),
    onTertiaryFixedVariant = Color(0xFF284B30),
)

internal val MankeuDarkColors = darkColorScheme(
    primary = Color(0xFF52B788),
    onPrimary = Color(0xFF1E2620),
    primaryContainer = Color(0xFF1B4332),
    onPrimaryContainer = Color(0xFFD8F3DC),
    inversePrimary = Color(0xFF2D6A4F),
    secondary = Color(0xFFD4836A),
    onSecondary = Color(0xFF1E2620),
    secondaryContainer = Color(0xFF713A28),
    onSecondaryContainer = Color(0xFFFCDACF),
    tertiary = Color(0xFF7DB88A),
    onTertiary = Color(0xFF1E2E22),
    tertiaryContainer = Color(0xFF284B30),
    onTertiaryContainer = Color(0xFFE1EFE4),
    background = Color(0xFF1E2620),
    onBackground = Color(0xFFDDE8DF),
    surface = Color(0xFF1E2620),
    onSurface = Color(0xFFDDE8DF),
    surfaceVariant = Color(0xFF2B322C),
    onSurfaceVariant = Color(0xFF8BA08E),
    surfaceTint = Color(0xFF52B788),
    inverseSurface = Color(0xFFDDE8DF),
    inverseOnSurface = Color(0xFF2C2A29),
    surfaceBright = Color(0xFF2B322C),
    surfaceDim = Color(0xFF1E2620),
    surfaceContainerLowest = Color(0xFF171E19),
    surfaceContainerLow = Color(0xFF262D28),
    surfaceContainer = Color(0xFF28302A),
    surfaceContainerHigh = Color(0xFF2A312B),
    surfaceContainerHighest = Color(0xFF2B322C),
    outline = Color(0xFF8BA08E),
    outlineVariant = Color(0xFF2E3830),
    scrim = Color(0xFF000000),
    error = Color(0xFFD4836A),
    onError = Color(0xFF1E2620),
    errorContainer = Color(0xFF3A2018),
    onErrorContainer = Color(0xFFFCDACF),
    primaryFixed = Color(0xFFD8F3DC),
    primaryFixedDim = Color(0xFF52B788),
    onPrimaryFixed = Color(0xFF10251B),
    onPrimaryFixedVariant = Color(0xFF1B4332),
    secondaryFixed = Color(0xFFFCDACF),
    secondaryFixedDim = Color(0xFFD4836A),
    onSecondaryFixed = Color(0xFF2C2A29),
    onSecondaryFixedVariant = Color(0xFF713A28),
    tertiaryFixed = Color(0xFFE1EFE4),
    tertiaryFixedDim = Color(0xFF7DB88A),
    onTertiaryFixed = Color(0xFF142A19),
    onTertiaryFixedVariant = Color(0xFF284B30),
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

internal val MankeuLightSemanticColors = MankeuSemanticColors(
    success = Color(0xFF2D6A4F),
    onSuccess = Color(0xFFFFFFFF),
    successContainer = Color(0xFFD8F3DC),
    onSuccessContainer = Color(0xFF1B4332),
    danger = Color(0xFF9A442D),
    onDanger = Color(0xFFFFFFFF),
    dangerContainer = Color(0xFFFCDACF),
    onDangerContainer = Color(0xFF713A28),
    warningContainer = Color(0xFFFFF7D6),
    onWarningContainer = Color(0xFF59460D),
    chart = listOf(
        Color(0xFF2D6A4F),
        Color(0xFF9A442D),
        Color(0xFF375D81),
        Color(0xFF705580),
        Color(0xFF326B68),
        Color(0xFF86583A),
    ),
)

internal val MankeuDarkSemanticColors = MankeuSemanticColors(
    success = Color(0xFF68C99A),
    onSuccess = Color(0xFF1E2620),
    successContainer = Color(0xFF2A3E2F),
    onSuccessContainer = Color(0xFFD8F3DC),
    danger = Color(0xFFD4836A),
    onDanger = Color(0xFF1E2620),
    dangerContainer = Color(0xFF3A2018),
    onDangerContainer = Color(0xFFFCDACF),
    warningContainer = Color(0xFF35300A),
    onWarningContainer = Color(0xFFFFF7D6),
    chart = listOf(
        Color(0xFF68C99A),
        Color(0xFFD4836A),
        Color(0xFFA0C4DF),
        Color(0xFFCBB3DF),
        Color(0xFF87C9BD),
        Color(0xFFDDB78D),
    ),
)

val LocalMankeuSemanticColors = androidx.compose.runtime.staticCompositionLocalOf { MankeuLightSemanticColors }

private val MankeuShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

data class MankeuMotion(
    val enabled: Boolean,
    val fastMillis: Int,
    val mediumMillis: Int,
) {
    /** Decorative playback and spatial travel only run at the full motion level. */
    val expressive: Boolean get() = enabled && this == Full

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
fun MankeuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    motion: MankeuMotion = MankeuMotion.Full,
    content: @Composable () -> Unit,
) {
    // Tokens are passed straight through.
    //
    // These used to be interpolated: 40 `ColorScheme` roles plus 11 semantic ones, each wrapped in
    // its own `animateColorAsState`, so ~51 animation state objects were created and read on the
    // very first composition and re-read on every recomposition of the whole tree — all to
    // cross-fade a theme switch most owners press once, if ever. The cost was paid on every launch
    // by everyone; the benefit went to a rare gesture. `Crossfade` on the tree is not the
    // alternative either (see D-017: it discards scroll position, half-filled forms and open
    // dialogs). A theme change is now an instant swap, which is what the platform itself does.
    val scheme = if (darkTheme) MankeuDarkColors else MankeuLightColors
    val semanticColors = if (darkTheme) MankeuDarkSemanticColors else MankeuLightSemanticColors
    androidx.compose.runtime.CompositionLocalProvider(
        LocalMankeuSemanticColors provides semanticColors,
        LocalMotion provides motion,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            shapes = MankeuShapes,
            typography = MankeuTypography,
            content = content,
        )
    }
}
