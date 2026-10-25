package com.mankeu.presentation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class PaletteBehaviorTest {
    @Test
    fun sortingAmountsDoesNotReassignCategoryColors() {
        for (colors in listOf(MankeuLightSemanticColors, MankeuDarkSemanticColors)) {
            val categories = listOf("Pengemasan", "Transportasi", "Administrasi", "Internet")
            val before = categories.associateWith(colors::resolveAgricultureCategoryColor)
            val after = categories.reversed().associateWith(colors::resolveAgricultureCategoryColor)
            assertEquals(before, after)
            assertTrue(before.values.all { it in colors.chart })
        }
    }

    @Test
    fun categoryIdentityUsesRootLocaleAndTrimmedName() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            for (colors in listOf(MankeuLightSemanticColors, MankeuDarkSemanticColors)) {
                assertEquals(
                    colors.resolveAgricultureCategoryColor("internet"),
                    colors.resolveAgricultureCategoryColor("  INTERNET  "),
                )
            }
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun categoriesKeepHueMeaningAcrossThemesWithoutImplyingDanger() {
        val meanings = mapOf("Pupuk" to 0, "Tenaga kerja" to 2, "Pestisida" to 3, "Irigasi" to 4, "Sewa alat" to 5)
        for (colors in listOf(MankeuLightSemanticColors, MankeuDarkSemanticColors)) {
            assertEquals(6, colors.chart.size)
            meanings.forEach { (category, index) ->
                assertEquals(colors.chart[index], colors.resolveAgricultureCategoryColor(category))
            }
            assertFalse(colors.resolveAgricultureCategoryColor("Pestisida") == colors.danger)
            assertFalse(colors.resolveAgricultureCategoryColor("Sewa alat") == colors.warning)
        }
    }

    @Test
    fun filledActionAndReadableInkMeetTextContrast() {
        for ((material, semantic) in listOf(MankeuLightColors to MankeuLightSemanticColors, MankeuDarkColors to MankeuDarkSemanticColors)) {
            assertTrue(contrast(semantic.onActionContainer, semantic.actionContainer) >= 4.5f)
            listOf(material.surface, material.surfaceContainerLow, material.surfaceContainerHigh, material.surfaceContainerHighest).forEach {
                assertTrue(contrast(material.primary, it) >= 4.5f)
            }
            AccentFamily.entries.forEach {
                val accent = accentFamilyColors(it, semantic.isDark)
                assertTrue(contrast(accent.icon, accent.container) >= 4.5f)
            }
        }
    }

    private fun contrast(first: Color, second: Color): Float {
        val a = first.luminance()
        val b = second.luminance()
        return (maxOf(a, b) + 0.05f) / (minOf(a, b) + 0.05f)
    }
}
