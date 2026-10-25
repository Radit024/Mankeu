package com.mankeu.presentation

import com.mankeu.presentation.components.TourTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SettingsNavigationTest {
    @Test fun homeClosesAndDirtyProfileNeedsReview() {
        assertEquals(SettingsBackAction.CLOSE, settingsBackAction(SettingsPage.HOME, false))
        assertEquals(SettingsBackAction.CLOSE, settingsBackAction(SettingsPage.HOME, true))
        assertEquals(SettingsBackAction.CONFIRM_DISCARD, settingsBackAction(SettingsPage.PROFILE, true))
        assertEquals(SettingsBackAction.HOME, settingsBackAction(SettingsPage.PROFILE, false))
    }

    @Test fun everyOtherDetailReturnsToOverview() {
        SettingsPage.entries.filter { it !in listOf(SettingsPage.HOME, SettingsPage.PROFILE) }.forEach {
            assertEquals("Clean $it", SettingsBackAction.HOME, settingsBackAction(it, false))
            assertEquals("Preserved draft on $it", SettingsBackAction.HOME, settingsBackAction(it, true))
        }
    }

    @Test fun toursNavigateToTheirActualSettingsOwner() {
        assertEquals(SettingsPage.APPEARANCE, settingsPageForTour(TourTarget.SETTINGS_DISPLAY))
        assertEquals(SettingsPage.GUIDES, settingsPageForTour(TourTarget.SETTINGS_GUIDES))
        assertEquals(SettingsPage.ACCOUNT, settingsPageForTour(TourTarget.SETTINGS_DRIVE))
        TourTarget.entries.filter { it !in setOf(TourTarget.SETTINGS_DISPLAY, TourTarget.SETTINGS_GUIDES, TourTarget.SETTINGS_DRIVE) }.forEach {
            assertNull("Non Settings target $it", settingsPageForTour(it))
        }
    }

    @Test fun eachProfileFieldParticipatesInUnsavedChanges() {
        val baseline = ProfileDraft("Pemilik", "628123456789", "@contoh", "Kota", "Kopi", "1 Ha")
        listOf(
            baseline.copy(name = "Nama baru"), baseline.copy(whatsapp = ""),
            baseline.copy(telegram = ""), baseline.copy(location = ""),
            baseline.copy(commodity = ""), baseline.copy(landArea = ""),
        ).forEach { assertNotEquals(baseline, it) }
        assertEquals(baseline, baseline.copy())
    }

    @Test fun saveNormalizesContactsAndKeepsOptionalFieldsOptional() {
        assertEquals(
            ProfileDraft("Pemilik", "628123456789", "@contoh", "Kota", "Kopi", "1 Ha"),
            ProfileDraft(" Pemilik ", "0812-3456-789", "@Contoh", " Kota ", " Kopi ", " 1 Ha ").normalized(),
        )
        assertEquals(ProfileDraft(), ProfileDraft(" ", " ", " ", " ", " ", " ").normalized())
    }
}
