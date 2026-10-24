package com.mankeu.presentation

import androidx.compose.ui.graphics.Color

enum class MetricAccent { CASH, PROFIT, INCOME, EXPENSE }

data class MetricAccentColors(val container: Color, val icon: Color)

fun metricAccentColors(accent: MetricAccent, dark: Boolean): MetricAccentColors =
    when (accent) {
        MetricAccent.CASH -> if (dark) MetricAccentColors(Color(0xFF1E2C3D), Color(0xFFA9CCF4)) else MetricAccentColors(Color(0xFFEDF4FF), Color(0xFF315D90))
        MetricAccent.PROFIT -> if (dark) MetricAccentColors(Color(0xFF2A2438), Color(0xFFD0BDF4)) else MetricAccentColors(Color(0xFFF2EEFF), Color(0xFF66508D))
        MetricAccent.INCOME -> if (dark) MetricAccentColors(Color(0xFF1C2E24), Color(0xFFA9D8BC)) else MetricAccentColors(Color(0xFFEAF6EF), Color(0xFF2D6A4F))
        MetricAccent.EXPENSE -> if (dark) MetricAccentColors(Color(0xFF2E281F), Color(0xFFDEC9A3)) else MetricAccentColors(Color(0xFFF5F0E6), Color(0xFF76603F))
    }
