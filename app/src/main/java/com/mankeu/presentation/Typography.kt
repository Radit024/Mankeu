package com.mankeu.presentation

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mankeu.R

/** Local resources keep the owner's brand typography available on the very first offline launch. */
val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
)

val Sora = FontFamily(
    Font(R.font.sora_semibold, FontWeight.SemiBold),
    Font(R.font.sora_bold, FontWeight.Bold),
)

private fun heading(size: Int, lineHeight: Int, weight: FontWeight = FontWeight.SemiBold) = TextStyle(
    fontFamily = Sora,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = 0.sp,
    fontFeatureSettings = "tnum",
)

private fun body(size: Int, lineHeight: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = PlusJakartaSans,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = 0.sp,
    fontFeatureSettings = "tnum",
)

/** Material roles, rather than per-screen font overrides, own the heading/body split. */
val MankeuTypography = Typography(
    displayLarge = heading(48, 56, FontWeight.Bold),
    displayMedium = heading(40, 48, FontWeight.Bold),
    displaySmall = heading(36, 44, FontWeight.Bold),
    headlineLarge = heading(32, 40, FontWeight.Bold),
    headlineMedium = heading(28, 36),
    headlineSmall = heading(24, 32),
    titleLarge = heading(22, 30),
    titleMedium = heading(16, 24),
    titleSmall = heading(14, 20),
    bodyLarge = body(16, 24),
    bodyMedium = body(14, 22),
    bodySmall = body(12, 18),
    labelLarge = body(14, 20, FontWeight.SemiBold),
    labelMedium = body(12, 16, FontWeight.Medium),
    labelSmall = body(11, 16, FontWeight.Medium),
)
