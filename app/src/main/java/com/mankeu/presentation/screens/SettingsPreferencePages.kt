package com.mankeu.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.mankeu.R
import com.mankeu.presentation.*
import com.mankeu.presentation.components.TourTarget
import com.mankeu.presentation.components.primaryActionButtonColors
import com.mankeu.presentation.components.tourTarget

@Composable
internal fun SettingsPreferencePage(
    page: SettingsPage,
    themeMode: ThemeMode, onThemeModeChange: (ThemeMode) -> Unit,
    language: AppLanguage, onLanguageChange: (AppLanguage) -> Unit,
    motionMode: MotionMode, onMotionModeChange: (MotionMode) -> Unit,
    hapticsEnabled: Boolean, onHapticsChange: (Boolean) -> Unit,
    soundEnabled: Boolean, onSoundChange: (Boolean) -> Unit,
    tourOffersEnabled: Boolean, onTourOffersChange: (Boolean) -> Unit,
    onOpenHelpCenter: () -> Unit, onResetTours: () -> Unit,
) {
    when (page) {
        SettingsPage.APPEARANCE -> SettingCard(stringResource(R.string.settings_display), Modifier.tourTarget(TourTarget.SETTINGS_DISPLAY)) {
            Column(Modifier.selectableGroup()) {
                ThemeMode.entries.forEach { option ->
                    SettingsChoiceRow(stringResource(option.labelRes), themeMode == option, { if (option != themeMode) onThemeModeChange(option) }, "theme-${option.name}") {
                        ThemePreview(option)
                    }
                }
            }
        }
        SettingsPage.LANGUAGE -> SettingCard(stringResource(R.string.settings_language)) {
            Column(Modifier.selectableGroup()) {
                AppLanguage.entries.forEach { option ->
                    SettingsChoiceRow(option.label, language == option, { if (option != language) onLanguageChange(option) }, "language-${option.name}")
                }
            }
        }
        SettingsPage.MOTION -> SettingCard(stringResource(R.string.settings_motion)) {
            Text(stringResource(R.string.settings_motion_help), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(Modifier.selectableGroup()) {
                MotionMode.entries.forEach { option ->
                    SettingsChoiceRow(stringResource(option.labelRes), motionMode == option, { if (option != motionMode) onMotionModeChange(option) }, "motion-${option.name}")
                }
            }
        }
        SettingsPage.FEEDBACK -> SettingCard(stringResource(R.string.settings_feedback)) {
            ToggleRow(stringResource(R.string.settings_haptics), stringResource(R.string.settings_haptics_help), hapticsEnabled, onHapticsChange)
            ToggleRow(stringResource(R.string.settings_sound), stringResource(R.string.settings_sound_help), soundEnabled, onSoundChange)
        }
        SettingsPage.GUIDES -> SettingCard(stringResource(R.string.settings_tour_title), Modifier.tourTarget(TourTarget.SETTINGS_GUIDES)) {
            Text(stringResource(R.string.settings_tour_desc), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onOpenHelpCenter, colors = primaryActionButtonColors(), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.help_open_center))
            }
            ToggleRow(stringResource(R.string.settings_tour_offers), stringResource(R.string.settings_guides_summary), tourOffersEnabled, onTourOffersChange)
            TextButton(onClick = onResetTours, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.settings_tour_reset))
            }
            Text(stringResource(R.string.settings_tour_reset_help), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        else -> Unit
    }
}

@Composable
private fun SettingsChoiceRow(label: String, selected: Boolean, onClick: () -> Unit, tag: String, preview: (@Composable () -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).testTag(tag).selectable(selected, role = Role.RadioButton, onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label)
            preview?.invoke()
        }
    }
}

/** Static swatches explain light/dark without running a second theme or decorative animation. */
@Composable
private fun ThemePreview(mode: ThemeMode) {
    val colors = when (mode) {
        ThemeMode.LIGHT -> listOf(Color(0xFFFAFAF8), Color(0xFFD8F3DC), Color(0xFF2D6A4F))
        ThemeMode.DARK -> listOf(Color(0xFF101613), Color(0xFF243027), Color(0xFF8ED8AE))
        ThemeMode.SYSTEM -> listOf(Color(0xFFFAFAF8), Color(0xFF101613), Color(0xFF52B788))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        colors.forEach { color -> Box(Modifier.size(width = 36.dp, height = 18.dp).background(color, RoundedCornerShape(4.dp))) }
    }
}
