package com.mankeu.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Shared number/date formatting for every screen. Centralized so a future currency-display
 * change (for example, a compact "Rp1,2 jt" mode) touches one file instead of six.
 */

val IndonesianLocale: Locale = Locale.forLanguageTag("id-ID")

/**
 * Money is always formatted `id-ID`, in both languages, on purpose: the amounts *are* rupiah, and
 * "Rp1.150.000" is how they are written on the receipts and in the owner's workbook. Reformatting
 * them for an English UI would only invent a second way to write the same figure.
 */
fun Long.toRupiah(): String = NumberFormat.getCurrencyInstance(IndonesianLocale).apply {
    maximumFractionDigits = 0
}.format(this)

/**
 * Dates, unlike money, do follow the chosen language — month names are words, not values.
 * [ProvideAppLanguage] keeps `Locale.getDefault()` in step with the setting, so this needs no
 * composition context and stays usable from non-composable code.
 */
fun LocalDate.toDisplayDate(): String = format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()))

/**
 * The locale to format with inside composition.
 *
 * Reading `Locale.getDefault()` from a composable is not observable — a locale change would not
 * invalidate the caller — which is exactly what lint's `NonObservableLocale` catches. Going through
 * `LocalConfiguration` means the language switch redraws the dates with it.
 */
@Composable
fun appLocale(): Locale = LocalConfiguration.current.locales[0]
