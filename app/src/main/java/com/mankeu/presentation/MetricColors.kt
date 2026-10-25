package com.mankeu.presentation

import androidx.compose.ui.graphics.Color

enum class MetricAccent { CASH, PROFIT, INCOME, EXPENSE }

enum class AccentFamily { MINT, TERRACOTTA, BLUE, LAVENDER, SAND, TEAL }

data class MetricAccentColors(val container: Color, val icon: Color)

fun metricAccentColors(accent: MetricAccent, dark: Boolean): MetricAccentColors =
    accentFamilyColors(
        when (accent) {
            MetricAccent.CASH -> AccentFamily.BLUE
            MetricAccent.PROFIT -> AccentFamily.LAVENDER
            MetricAccent.INCOME -> AccentFamily.MINT
            MetricAccent.EXPENSE -> AccentFamily.SAND
        },
        dark,
    )

fun accentFamilyColors(family: AccentFamily, dark: Boolean): MetricAccentColors = when (family) {
    AccentFamily.MINT -> if (dark) MetricAccentColors(Color(0xFF1C2E24), Color(0xFFA9D8BC))
        else MetricAccentColors(Color(0xFFEAF6EF), Color(0xFF2D6A4F))
    AccentFamily.TERRACOTTA -> if (dark) MetricAccentColors(Color(0xFF36251F), Color(0xFFF0B49B))
        else MetricAccentColors(Color(0xFFFCDACF), Color(0xFF713A28))
    AccentFamily.BLUE -> if (dark) MetricAccentColors(Color(0xFF1A2735), Color(0xFFA8CBE8))
        else MetricAccentColors(Color(0xFFEDF4FF), Color(0xFF315D90))
    AccentFamily.LAVENDER -> if (dark) MetricAccentColors(Color(0xFF2B2437), Color(0xFFC7B6E8))
        else MetricAccentColors(Color(0xFFF2EEFF), Color(0xFF66508D))
    AccentFamily.SAND -> if (dark) MetricAccentColors(Color(0xFF302A20), Color(0xFFDEC9A3))
        else MetricAccentColors(Color(0xFFF5F0E6), Color(0xFF76603F))
    AccentFamily.TEAL -> if (dark) MetricAccentColors(Color(0xFF19312F), Color(0xFF9AD9D2))
        else MetricAccentColors(Color(0xFFE6F3F1), Color(0xFF326B68))
}
