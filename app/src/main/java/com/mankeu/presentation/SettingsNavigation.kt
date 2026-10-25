package com.mankeu.presentation

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import com.mankeu.R
import com.mankeu.domain.ProfileContactUtils
import com.mankeu.presentation.components.TourTarget

enum class SettingsPage { HOME, PROFILE, APPEARANCE, LANGUAGE, MOTION, FEEDBACK, ACCOUNT, GUIDES, ABOUT }
enum class SettingsBackAction { CLOSE, HOME, CONFIRM_DISCARD }

fun settingsTitleRes(page: SettingsPage): Int = when (page) {
    SettingsPage.HOME -> R.string.settings_title
    SettingsPage.PROFILE -> R.string.settings_tab_profile
    SettingsPage.APPEARANCE -> R.string.settings_display
    SettingsPage.LANGUAGE -> R.string.settings_language
    SettingsPage.MOTION -> R.string.settings_motion
    SettingsPage.FEEDBACK -> R.string.settings_feedback
    SettingsPage.ACCOUNT -> R.string.settings_optional_account
    SettingsPage.GUIDES -> R.string.settings_tour_title
    SettingsPage.ABOUT -> R.string.settings_about_entry
}

fun settingsBackAction(page: SettingsPage, dirty: Boolean): SettingsBackAction = when {
    page == SettingsPage.HOME -> SettingsBackAction.CLOSE
    page == SettingsPage.PROFILE && dirty -> SettingsBackAction.CONFIRM_DISCARD
    else -> SettingsBackAction.HOME
}

fun settingsPageForTour(target: TourTarget): SettingsPage? = when (target) {
    TourTarget.SETTINGS_DISPLAY -> SettingsPage.APPEARANCE
    TourTarget.SETTINGS_GUIDES -> SettingsPage.GUIDES
    TourTarget.SETTINGS_DRIVE -> SettingsPage.ACCOUNT
    else -> null
}

/** Editor snapshot; authentication never owns or rewrites local profile fields. */
data class ProfileDraft(
    val name: String = "",
    val whatsapp: String = "",
    val telegram: String = "",
    val location: String = "",
    val commodity: String = "",
    val landArea: String = "",
) {
    fun normalized(): ProfileDraft = copy(
        name = name.trim(),
        whatsapp = ProfileContactUtils.normalizeWhatsAppPhone(whatsapp),
        telegram = ProfileContactUtils.normalizeTelegramContact(telegram).displayValue,
        location = location.trim(), commodity = commodity.trim(), landArea = landArea.trim(),
    )

    companion object {
        val StateSaver: Saver<ProfileDraft, Any> = listSaver(
            save = { listOf(it.name, it.whatsapp, it.telegram, it.location, it.commodity, it.landArea) },
            restore = { ProfileDraft(it[0], it[1], it[2], it[3], it[4], it[5]) },
        )
    }
}
