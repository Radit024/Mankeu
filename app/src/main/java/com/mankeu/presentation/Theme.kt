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
    surfaceDim = Color(0xFFEBEBE6),
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
    primary = Color(0xFF8ED8AE),
    onPrimary = Color(0xFF10251B),
    primaryContainer = Color(0xFF1B3A2A),
    onPrimaryContainer = Color(0xFFD8F3DC),
    inversePrimary = Color(0xFF2D6A4F),
    secondary = Color(0xFFF0B49B),
    onSecondary = Color(0xFF3B2118),
    secondaryContainer = Color(0xFF36251F),
    onSecondaryContainer = Color(0xFFFADBCB),
    tertiary = Color(0xFF9AD9D2),
    onTertiary = Color(0xFF103632),
    tertiaryContainer = Color(0xFF19312F),
    onTertiaryContainer = Color(0xFFD1F4EF),
    background = Color(0xFF101613),
    onBackground = Color(0xFFF1F5F2),
    surface = Color(0xFF101613),
    onSurface = Color(0xFFF1F5F2),
    surfaceVariant = Color(0xFF29382E),
    onSurfaceVariant = Color(0xFFAAB8AF),
    surfaceTint = Color(0xFF8ED8AE),
    inverseSurface = Color(0xFFE8EFEA),
    inverseOnSurface = Color(0xFF18221B),
    surfaceBright = Color(0xFF29382E),
    surfaceDim = Color(0xFF101613),
    surfaceContainerLowest = Color(0xFF0B100D),
    surfaceContainerLow = Color(0xFF19211C),
    surfaceContainer = Color(0xFF1D2720),
    surfaceContainerHigh = Color(0xFF243027),
    surfaceContainerHighest = Color(0xFF29382E),
    outline = Color(0xFF799382),
    outlineVariant = Color(0xFF36483C),
    scrim = Color(0xFF000000),
    error = Color(0xFFF2A99D),
    onError = Color(0xFF3D1713),
    errorContainer = Color(0xFF3D2825),
    onErrorContainer = Color(0xFFFFDAD4),
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

enum class SemanticIntent {
    PRIMARY,
    SUCCESS,
    WARNING,
    DANGER,
    INFO,
    NEUTRAL,
}

data class MankeuSemanticColors(
    val actionContainer: Color,
    val onActionContainer: Color,
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val danger: Color,
    val onDanger: Color,
    val dangerContainer: Color,
    val onDangerContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val info: Color,
    val onInfo: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
    val chart: List<Color>,
    val isDark: Boolean = false,
) {
    fun softContainer(intent: SemanticIntent): Color = when (intent) {
        SemanticIntent.PRIMARY -> successContainer
        SemanticIntent.SUCCESS -> successContainer
        SemanticIntent.WARNING -> warningContainer
        SemanticIntent.DANGER -> dangerContainer
        SemanticIntent.INFO -> infoContainer
        SemanticIntent.NEUTRAL -> if (isDark) Color(0xFF243027) else Color(0xFFEBEBE6)
    }

    fun softContent(intent: SemanticIntent): Color = when (intent) {
        SemanticIntent.PRIMARY -> onSuccessContainer
        SemanticIntent.SUCCESS -> if (isDark) success else Color(0xFF2D6A4F)
        SemanticIntent.WARNING -> if (isDark) warning else onWarningContainer
        SemanticIntent.DANGER -> danger
        SemanticIntent.INFO -> info
        SemanticIntent.NEUTRAL -> if (isDark) Color(0xFFAAB8AF) else Color(0xFF6B6866)
    }

    // Decorative borders only. Interactive controls use Material outline at full contrast.
    fun softBorder(intent: SemanticIntent): Color = when (intent) {
        SemanticIntent.PRIMARY -> success.copy(alpha = 0.35f)
        SemanticIntent.SUCCESS -> (if (isDark) success else Color(0xFF52B788)).copy(alpha = 0.38f)
        SemanticIntent.WARNING -> (if (isDark) warning else Color(0xFFF4E285)).copy(alpha = 0.45f)
        SemanticIntent.DANGER -> (if (isDark) danger else Color(0xFFE07A5F)).copy(alpha = 0.40f)
        SemanticIntent.INFO -> (if (isDark) info else Color(0xFF74A57F)).copy(alpha = 0.40f)
        SemanticIntent.NEUTRAL -> if (isDark) Color(0xFF36483C) else Color(0xFF827D78).copy(alpha = 0.30f)
    }

    /** Category hues are stable across sorting; they do not communicate warning or loss. */
    fun resolveAgricultureCategoryColor(category: String): Color {
        val key = category.trim().lowercase(java.util.Locale.ROOT)
        val index = when {
            key.contains("pestisida") || key.contains("pesticide") -> 3
            key.contains("pupuk") || key.contains("fertilizer") -> 0
            key.contains("tenaga") || key.contains("labor") -> 2
            key.contains("irigasi") || key.contains("irrigation") -> 4
            key.contains("alat") || key.contains("tools") || key.contains("mesin") || key.contains("sewa") -> 5
            else -> Math.floorMod(key.hashCode(), chart.size)
        }
        return chart[index]
    }
}

/**
 * Atmospheric weather gradient tokens matching Arina webapp (lib/weatherTokens.ts).
 */
object ArinaWeatherTokens {
    fun gradientStops(condition: String, isDark: Boolean): List<Color> {
        val norm = condition.lowercase()
        return if (!isDark) {
            when {
                norm.contains("cerah") || norm.contains("sunny") ->
                    listOf(Color(0xFFE07A5F), Color(0xFFD98C4A), Color(0xFFF4E285))
                norm.contains("awan") || norm.contains("mendung") || norm.contains("cloud") ->
                    listOf(Color(0xFF4B7A56), Color(0xFF6B6866), Color(0xFF9A978C))
                else ->
                    listOf(Color(0xFF1B4332), Color(0xFF2D6A4F), Color(0xFF74A57F))
            }
        } else {
            when {
                norm.contains("cerah") || norm.contains("sunny") ->
                    listOf(Color(0xFFB05A42), Color(0xFFF0B49B), Color(0xFFE9CF7A))
                norm.contains("awan") || norm.contains("mendung") || norm.contains("cloud") ->
                    listOf(Color(0xFF101613), Color(0xFF243027), Color(0xFF799382))
                else ->
                    listOf(Color(0xFF0B100D), Color(0xFF1B3A2A), Color(0xFF357956))
            }
        }
    }
}

internal val MankeuLightSemanticColors = MankeuSemanticColors(
    actionContainer = Color(0xFF2D6A4F),
    onActionContainer = Color(0xFFFFFFFF),
    success = Color(0xFF2D6A4F),
    onSuccess = Color(0xFFFFFFFF),
    successContainer = Color(0xFFD8F3DC),
    onSuccessContainer = Color(0xFF1B4332),
    danger = Color(0xFF9A442D),
    onDanger = Color(0xFFFFFFFF),
    dangerContainer = Color(0xFFFCDACF),
    onDangerContainer = Color(0xFF713A28),
    warning = Color(0xFF59460D),
    onWarning = Color(0xFFFFFFFF),
    warningContainer = Color(0xFFFFF7D6),
    onWarningContainer = Color(0xFF59460D),
    info = Color(0xFF4B7A56),
    onInfo = Color(0xFFFFFFFF),
    infoContainer = Color(0xFFE1EFE4),
    onInfoContainer = Color(0xFF284B30),
    chart = listOf(
        Color(0xFF2D6A4F),
        Color(0xFF9A442D),
        Color(0xFF375D81),
        Color(0xFF705580),
        Color(0xFF326B68),
        Color(0xFF86583A),
    ),
    isDark = false,
)

internal val MankeuDarkSemanticColors = MankeuSemanticColors(
    actionContainer = Color(0xFF357956),
    onActionContainer = Color(0xFFFFFFFF),
    success = Color(0xFF8ED8AE),
    onSuccess = Color(0xFF10251B),
    successContainer = Color(0xFF1B3A2A),
    onSuccessContainer = Color(0xFFD8F3DC),
    danger = Color(0xFFF2A99D),
    onDanger = Color(0xFF3D1713),
    dangerContainer = Color(0xFF3D2825),
    onDangerContainer = Color(0xFFFFDAD4),
    warning = Color(0xFFE9CF7A),
    onWarning = Color(0xFF30260C),
    warningContainer = Color(0xFF39321D),
    onWarningContainer = Color(0xFFFFF0B8),
    info = Color(0xFFA8CBE8),
    onInfo = Color(0xFF112A3A),
    infoContainer = Color(0xFF22313C),
    onInfoContainer = Color(0xFFD5EAFB),
    chart = listOf(
        Color(0xFF8ED8AE),
        Color(0xFFF0B49B),
        Color(0xFFA8CBE8),
        Color(0xFFC7B6E8),
        Color(0xFF9AD9D2),
        Color(0xFFDEC9A3),
    ),
    isDark = true,
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
