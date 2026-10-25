package com.mankeu.presentation

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mankeu.R
import com.mankeu.auth.AuthUser
import com.mankeu.auth.FirebaseAuthManager
import com.mankeu.backup.EncryptedBackupManager
import com.mankeu.backup.cloud.GoogleDriveBackupClient
import com.mankeu.data.AppDatabase
import com.mankeu.data.FinanceRepository
import com.mankeu.data.WorkspaceSelection
import com.mankeu.importer.FinanceWorkbookImporter
import com.mankeu.presentation.screens.SettingsScreen
import com.mankeu.presentation.guide.Tour
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Locale

/** Native UI fixtures use synthetic local data and restore every device preference afterwards. */
@RunWith(AndroidJUnit4::class)
class SettingsRedesignTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun dirtyProfileBackKeepsDiscardsOrSavesAllSixFields() = withApp { fixture ->
        openSettings()
        compose.onNodeWithTag("profile-name").assertDoesNotExist()
        capture("home-light")
        compose.onNodeWithTag("settings-profile").performClick()
        replaceField("name", "Perubahan sementara")
        toolbarBack(fixture)
        compose.onNodeWithText(fixture.text(R.string.settings_unsaved_title)).assertIsDisplayed()
        capture("profile-dirty-dialog")
        compose.onNodeWithText(fixture.text(R.string.settings_keep_editing)).performClick()
        compose.onNodeWithTag("profile-name").assertTextContains("Perubahan sementara")
        systemBack()
        compose.onNodeWithText(fixture.text(R.string.settings_discard)).performClick()
        compose.onNodeWithTag("settings-home").assertIsDisplayed()
        compose.onNodeWithTag("settings-profile").performClick()
        compose.onNodeWithTag("profile-name").assertTextContains("Pemilik Contoh")

        replaceField("name", " Pemilik Baru ")
        replaceField("commodity", " Kopi ")
        replaceField("landArea", " 2 Ha ")
        replaceField("whatsapp", "0812-3456-789")
        replaceField("telegram", "@Contoh")
        replaceField("location", " Kota Contoh ")
        systemBack()
        compose.onNodeWithText(fixture.text(R.string.action_save)).performClick()
        compose.onNodeWithTag("settings-home").assertIsDisplayed()
        assertEquals("Pemilik Baru", fixture.preferences.getString("account_name", null))
        assertEquals("628123456789", fixture.preferences.getString("whatsapp_phone", null))
        assertEquals("@contoh", fixture.preferences.getString("telegram_contact", null))
        assertEquals("Kota Contoh", fixture.preferences.getString("profile_location", null))
        assertEquals("Kopi", fixture.preferences.getString("profile_commodity", null))
        assertEquals("2 Ha", fixture.preferences.getString("profile_land_area", null))
        compose.onNodeWithTag("settings-profile").performClick()
        compose.onNodeWithTag("profile-name").assertTextContains("Pemilik Baru")
        toolbarBack(fixture)
        compose.onNodeWithTag("settings-home").assertIsDisplayed()
        toolbarBack(fixture)
        compose.onNodeWithTag("settings-home").assertDoesNotExist()
        compose.onNodeWithText("Usaha Contoh").assertIsDisplayed()
    }

    @Test fun savedStateRestoresEditorAndAuthChangesDoNotOverwriteItsDraft() {
        val restoration = StateRestorationTester(compose)
        withApp(restoration) { fixture ->
            openSettings()
            compose.onNodeWithTag("settings-profile").performClick()
            replaceField("name", "Draft lokal belum disimpan")
            replaceField("commodity", "Produk lokal")
            replaceField("landArea", "3 Ha")
            replaceField("whatsapp", "081299988877")
            replaceField("telegram", "@draft_lokal")
            replaceField("location", "Alamat sementara")
            compose.runOnIdle {
                fixture.viewModel.setAuthenticatedUser(AuthUser("synthetic-settings-user", "owner@example.invalid", "Nama Google Berbeda", null))
            }
            compose.onNodeWithTag("profile-name").assertTextContains("Draft lokal belum disimpan")
            restoration.emulateSavedInstanceStateRestore()
            compose.onNodeWithTag("profile-name").assertTextContains("Draft lokal belum disimpan")
            compose.onNodeWithTag("profile-commodity").assertTextContains("Produk lokal")
            compose.onNodeWithTag("profile-landArea").assertTextContains("3 Ha")
            compose.onNodeWithTag("profile-whatsapp").assertTextContains("081299988877")
            compose.onNodeWithTag("profile-telegram").assertTextContains("@draft_lokal")
            compose.onNodeWithTag("profile-location").assertTextContains("Alamat sementara")
            assertEquals("Pemilik Contoh", fixture.preferences.getString("account_name", null))
            systemBack()
            compose.onNodeWithText(fixture.text(R.string.settings_unsaved_title)).assertIsDisplayed()
            // Dialog Back dismisses only the modal, retaining the editor.
            systemBack()
            compose.onNodeWithTag("profile-name").assertTextContains("Draft lokal belum disimpan")
            toolbarBack(fixture)
            compose.onNodeWithText(fixture.text(R.string.settings_discard)).performClick()
            compose.onNodeWithTag("settings-home").assertIsDisplayed()
        }
    }

    @Test fun helpReturnsToGuidesAndOfflinePreferencesRemainAvailable() = withApp { fixture ->
        openSettings()
        openHomeRow("settings-motion")
        compose.onNodeWithTag("motion-REDUCED").performClick().assertIsSelected()
        assertEquals(MotionMode.REDUCED.name, fixture.preferences.getString("motion_mode", null))
        toolbarBack(fixture)
        openHomeRow("settings-guides")
        capture("guides")
        compose.onNodeWithText(fixture.text(R.string.help_open_center)).performClick()
        compose.onNodeWithTag("settings-detail").assertDoesNotExist()
        toolbarBack(fixture)
        compose.onNodeWithText(fixture.text(R.string.settings_tour_reset)).assertIsDisplayed()
        systemBack()
        compose.onNodeWithTag("settings-home").assertIsDisplayed()
        openHomeRow("settings-account")
        compose.onNodeWithText(fixture.text(R.string.account_signed_out_status)).assertIsDisplayed()
        capture("account-offline")
        toolbarBack(fixture)
        openHomeRow("settings-about")
        compose.onNodeWithText(fixture.text(R.string.about_version, com.mankeu.BuildConfig.VERSION_NAME)).assertIsDisplayed()
        compose.onNodeWithText(fixture.text(R.string.lottie_license_title)).performScrollTo().performClick()
        compose.onNode(isDialog()).assertIsDisplayed()
        capture("about-license")
    }

    @Test fun hubRowsExposeCurrentValuesAndNavigateExactlyOnce() {
        val requested = mutableListOf<SettingsPage>()
        var helpRequests = 0
        compose.setContent {
            MankeuTheme(motion = MankeuMotion.Off) {
                FixtureScreen(page = SettingsPage.HOME, onOpenPage = requested::add, onHelp = { helpRequests++ })
            }
        }
        compose.onNodeWithText("Pemilik Contoh").assertIsDisplayed()
        compose.onNodeWithTag("profile-name").assertDoesNotExist()
        compose.onNodeWithTag("settings-appearance").assertTextContains(compose.activity.getString(ThemeMode.LIGHT.labelRes))
        compose.onNodeWithTag("settings-language").assertTextContains(AppLanguage.INDONESIAN.label)
        compose.onNodeWithTag("settings-motion").assertTextContains(compose.activity.getString(MotionMode.OFF.labelRes))
        val routes = listOf(
            "settings-profile" to SettingsPage.PROFILE,
            "settings-appearance" to SettingsPage.APPEARANCE,
            "settings-language" to SettingsPage.LANGUAGE,
            "settings-motion" to SettingsPage.MOTION,
            "settings-feedback" to SettingsPage.FEEDBACK,
            "settings-account" to SettingsPage.ACCOUNT,
            "settings-guides" to SettingsPage.GUIDES,
            "settings-about" to SettingsPage.ABOUT,
        )
        routes.forEach { (tag, page) ->
            compose.onNodeWithTag("settings-home").performScrollToNode(hasTestTag(tag))
            compose.onNodeWithTag(tag).performClick()
            assertEquals(page, requested.last())
        }
        assertEquals(routes.size, requested.size)
        compose.onNodeWithTag("settings-home").performScrollToNode(hasTestTag("settings-help"))
        compose.onNodeWithTag("settings-help").performClick()
        assertEquals(1, helpRequests)
    }

    @Test fun radioAndFeedbackRowsHaveOneActionAndImmediateSelection() {
        var page by mutableStateOf(SettingsPage.APPEARANCE)
        var theme by mutableStateOf(ThemeMode.LIGHT)
        var language by mutableStateOf(AppLanguage.INDONESIAN)
        var motion by mutableStateOf(MotionMode.OFF)
        var haptics by mutableStateOf(true)
        var sounds by mutableStateOf(false)
        var themeCalls = 0
        var languageCalls = 0
        var motionCalls = 0
        var hapticCalls = 0
        var soundCalls = 0
        compose.setContent {
            MankeuTheme(darkTheme = theme == ThemeMode.DARK, motion = MankeuMotion.Off) {
                FixtureScreen(
                    page = page,
                    theme = theme, onTheme = { theme = it; themeCalls++ },
                    language = language, onLanguage = { language = it; languageCalls++ },
                    motion = motion, onMotion = { motion = it; motionCalls++ },
                    haptics = haptics, onHaptics = { haptics = it; hapticCalls++ },
                    sound = sounds, onSound = { sounds = it; soundCalls++ },
                )
            }
        }
        compose.onNodeWithTag("theme-LIGHT").assertIsSelected().performClick()
        assertEquals(0, themeCalls)
        compose.onNodeWithTag("theme-DARK").performClick().assertIsSelected()
        assertEquals(1, themeCalls)
        capture("appearance-dark")
        compose.runOnIdle { page = SettingsPage.LANGUAGE }
        compose.onNodeWithTag("language-ENGLISH").performClick().assertIsSelected()
        assertEquals(1, languageCalls)
        compose.runOnIdle { page = SettingsPage.MOTION }
        compose.onNodeWithTag("motion-FULL").performClick().assertIsSelected()
        assertEquals(1, motionCalls)
        compose.runOnIdle { page = SettingsPage.FEEDBACK }
        compose.onAllNodes(isToggleable())[0].performClick().assertIsOff()
        assertEquals(1, hapticCalls)
        compose.onAllNodes(isToggleable())[1].performClick().assertIsOn()
        assertEquals(1, soundCalls)
        assertFalse(haptics)
    }

    @Test fun everySettingsPageRendersInBothThemesAndAtTwoHundredPercentText() {
        var page by mutableStateOf(SettingsPage.HOME)
        var dark by mutableStateOf(false)
        var textScale by mutableStateOf(1f)
        var renderLanguage by mutableStateOf(AppLanguage.INDONESIAN)
        compose.setContent {
            val host = LocalContext.current
            val hostConfiguration = LocalConfiguration.current
            val density = LocalDensity.current
            // Isolated rendered screen only; no dialog or Activity callback is invoked here.
            // Root workflow tests above keep the real Activity context for D-017.
            val configuration = remember(hostConfiguration, renderLanguage) {
                Configuration(hostConfiguration).apply { setLocale(Locale.forLanguageTag(renderLanguage.tag)) }
            }
            val localized = remember(host, configuration) { host.createConfigurationContext(configuration) }
            CompositionLocalProvider(
                LocalContext provides localized,
                LocalConfiguration provides configuration,
                LocalDensity provides Density(density.density, textScale),
            ) {
                MankeuTheme(darkTheme = dark, motion = MankeuMotion.Off) {
                    FixtureScreen(page, theme = if (dark) ThemeMode.DARK else ThemeMode.LIGHT, language = renderLanguage)
                }
            }
        }
        for (locale in AppLanguage.entries) {
            for (night in listOf(false, true)) {
                for (scale in listOf(1f, 2f)) {
                    compose.runOnIdle { renderLanguage = locale; dark = night; textScale = scale }
                    for (target in SettingsPage.entries) {
                        compose.runOnIdle { page = target }
                        val list = if (target == SettingsPage.HOME) "settings-home" else "settings-detail"
                        compose.onNodeWithTag(list).assertIsDisplayed().performScrollToIndex(0)
                        when (target) {
                            SettingsPage.PROFILE -> compose.onNodeWithTag("profile-location").performScrollTo().assertIsDisplayed()
                            SettingsPage.APPEARANCE -> compose.onNodeWithTag(if (night) "theme-DARK" else "theme-LIGHT").performScrollTo().assertIsSelected()
                            SettingsPage.LANGUAGE -> compose.onNodeWithTag("language-${locale.name}").performScrollTo().assertIsSelected()
                            SettingsPage.MOTION -> compose.onNodeWithTag("motion-OFF").performScrollTo().assertIsSelected()
                            else -> Unit
                        }
                        compose.onNodeWithTag(list).performScrollToIndex(0)
                        capture("${locale.tag}-${if (night) "dark" else "light"}-font${(scale * 100).toInt()}-${target.name.lowercase(Locale.ROOT)}")
                    }
                }
            }
        }
    }

    @Test fun settingsTourFindsAllThreeDetailPagesAtTwoHundredPercentText() = withApp(fontScale = 2f) { fixture ->
        openSettings()
        compose.onNodeWithContentDescription(fixture.text(R.string.tour_replay)).performClick()
        compose.onNodeWithText(fixture.text(R.string.help_play_tour, Tour.SETTINGS.steps.size)).performClick()
        val pages = listOf(SettingsPage.GUIDES, SettingsPage.APPEARANCE, SettingsPage.ACCOUNT)
        pages.forEachIndexed { index, page ->
            val counter = fixture.text(R.string.tour_step_counter, index + 1, 3)
            compose.waitUntil(15_000) { compose.onAllNodesWithText(counter).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText(counter).assertIsDisplayed()
            compose.onAllNodesWithText(fixture.text(Tour.SETTINGS.steps[index].titleRes)).onLast().assertIsDisplayed()
            compose.onNodeWithTag("settings-detail").assertIsDisplayed()
            when (page) {
                SettingsPage.GUIDES -> compose.onNodeWithText(fixture.text(R.string.settings_tour_reset)).assertExists()
                SettingsPage.APPEARANCE -> compose.onNodeWithTag("theme-LIGHT").assertIsSelected()
                SettingsPage.ACCOUNT -> compose.onNodeWithText(fixture.text(R.string.account_signed_out_status)).assertExists()
                else -> error("Unexpected tour page")
            }
            capture("tour-font200-${page.name.lowercase(Locale.ROOT)}")
            compose.onNodeWithText(fixture.text(if (index == 2) R.string.tour_btn_finish else R.string.tour_btn_next)).performClick()
        }
        compose.onNodeWithText(fixture.text(R.string.tour_step_counter, 3, 3)).assertDoesNotExist()
        compose.onNodeWithText(fixture.text(R.string.account_signed_out_status)).assertIsDisplayed()
    }

    @Test fun realActivityRecreationPreservesPreferencePageAndDirtyProfile() = withApp { fixture ->
        openSettings()
        openHomeRow("settings-appearance")
        val beforeTheme = compose.activity
        compose.onNodeWithTag("theme-DARK").performClick()
        compose.waitUntil(15_000) { compose.activity !== beforeTheme }
        fixture.reinstallContent()
        compose.onNodeWithTag("theme-DARK").assertIsSelected()
        assertEquals(ThemeMode.DARK.name, fixture.preferences.getString("theme_mode", null))
        capture("activity-recreated-appearance-dark")

        toolbarBack(fixture)
        openHomeRow("settings-language")
        val beforeLanguage = compose.activity
        compose.onNodeWithTag("language-INDONESIAN").performClick()
        compose.waitUntil(15_000) { compose.activity !== beforeLanguage }
        fixture.reinstallContent()
        compose.onNodeWithTag("language-INDONESIAN").assertIsSelected()
        assertEquals("id", fixture.preferences.getString("language", null))
        // ComponentActivity has no MainActivity.attachBaseContext language override. This proves
        // the production callback and saved UI session, not the translated-resource application.
        toolbarBack(fixture)
        openHomeRow("settings-profile")
        replaceField("name", "Draft setelah rotasi")
        replaceField("commodity", "Produk rotasi")
        replaceField("landArea", "4 Ha")
        replaceField("whatsapp", "081211122233")
        replaceField("telegram", "@rotasi")
        replaceField("location", "Lokasi rotasi")
        compose.activityRule.scenario.recreate()
        fixture.reinstallContent()
        compose.onNodeWithTag("profile-name").assertTextContains("Draft setelah rotasi")
        compose.onNodeWithTag("profile-commodity").assertTextContains("Produk rotasi")
        compose.onNodeWithTag("profile-landArea").assertTextContains("4 Ha")
        compose.onNodeWithTag("profile-whatsapp").assertTextContains("081211122233")
        compose.onNodeWithTag("profile-telegram").assertTextContains("@rotasi")
        compose.onNodeWithTag("profile-location").assertTextContains("Lokasi rotasi")
        assertEquals("Pemilik Contoh", fixture.preferences.getString("account_name", null))
        toolbarBack(fixture)
        compose.onNodeWithText(fixture.text(R.string.settings_unsaved_title)).assertIsDisplayed()
        compose.onNodeWithText(fixture.text(R.string.settings_keep_editing)).performClick()
        capture("activity-recreated-profile-draft")
    }

    @Composable
    private fun FixtureScreen(
        page: SettingsPage,
        onOpenPage: (SettingsPage) -> Unit = {}, onHelp: () -> Unit = {},
        theme: ThemeMode = ThemeMode.LIGHT, onTheme: (ThemeMode) -> Unit = {},
        language: AppLanguage = AppLanguage.INDONESIAN, onLanguage: (AppLanguage) -> Unit = {},
        motion: MotionMode = MotionMode.OFF, onMotion: (MotionMode) -> Unit = {},
        haptics: Boolean = true, onHaptics: (Boolean) -> Unit = {},
        sound: Boolean = false, onSound: (Boolean) -> Unit = {},
    ) {
        SettingsScreen(
            themeMode = theme, onThemeModeChange = onTheme, motionMode = motion, onMotionModeChange = onMotion,
            hapticsEnabled = haptics, onHapticsChange = onHaptics, soundEnabled = sound, onSoundChange = onSound,
            language = language, onLanguageChange = onLanguage, accountName = "Pemilik Contoh", authUser = null,
            onSignInGoogle = {}, onSignOut = {}, onOpenHelpCenter = onHelp,
            tourOffersEnabled = false, onTourOffersChange = {}, onResetTours = {},
            page = page, onOpenPage = onOpenPage, draft = ProfileDraft(name = "Pemilik Contoh"), onDraftChange = {}, onSaveDraft = {},
        )
    }

    private class Fixture(
        val preferences: SharedPreferences,
        val resources: () -> Context,
        val viewModel: AppViewModel,
        val reinstallContent: () -> Unit,
    ) {
        fun text(resource: Int, vararg args: Any): String = resources().getString(resource, *args)
    }

    private fun withApp(restoration: StateRestorationTester? = null, fontScale: Float = 1f, block: (Fixture) -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = context.getSharedPreferences("mankeu_ui_prefs", Context.MODE_PRIVATE)
        val saved = preferences.all.toMap()
        preferences.edit().putString("theme_mode", ThemeMode.LIGHT.name).putString("motion_mode", MotionMode.OFF.name)
            .putString("language", "en").putBoolean("tour_offers_enabled", false).putBoolean("welcome_complete", true)
            .putString("account_name", "Pemilik Contoh").putString("whatsapp_phone", "").putString("telegram_contact", "")
            .putString("profile_location", "").putString("profile_commodity", "").putString("profile_land_area", "").commit()
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        val store = ViewModelStore()
        var showApp by mutableStateOf(true)
        try {
            val repository = FinanceRepository(database)
            val selection = runBlocking {
                val business = repository.createBusiness("Usaha Contoh")
                WorkspaceSelection(business, repository.createProject(business, "Unit Contoh"))
            }
            val auth = FirebaseAuthManager().apply { setCustomUser(null) }
            val viewModel = AppViewModel(repository, FinanceWorkbookImporter(), EncryptedBackupManager(database, File(context.cacheDir, "settings-test.aabk")), auth, GoogleDriveBackupClient(), rememberedSelection = selection)
            store.put("settings-test", viewModel)
            // Keep the real Activity context. Dialog subcompositions inherit its resources;
            // overriding only screen locals would test a locale mismatch that the app avoids
            // through MainActivity.attachBaseContext and Activity.recreate (D-017).
            val content: @Composable () -> Unit = {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                    if (showApp) MankeuApp(viewModel)
                }
            }
            if (restoration == null) compose.setContent(content) else restoration.setContent(content)
            compose.waitUntil(15_000) { !viewModel.uiState.value.isLoading && viewModel.uiState.value.selectedProjectId == selection.projectId }
            compose.waitForIdle()
            block(Fixture(preferences, { compose.activity }, viewModel) {
                compose.activityRule.scenario.onActivity { it.setContent(content = content) }
                compose.waitForIdle()
            })
        } finally {
            compose.runOnIdle { showApp = false }
            compose.waitForIdle()
            compose.runOnIdle { store.clear() }
            database.close()
            preferences.edit().clear().apply {
                saved.forEach { (key, value) ->
                    when (value) {
                        is Boolean -> putBoolean(key, value)
                        is Int -> putInt(key, value)
                        is Long -> putLong(key, value)
                        is Float -> putFloat(key, value)
                        is String -> putString(key, value)
                        is Set<*> -> putStringSet(key, value.filterIsInstance<String>().toSet())
                    }
                }
            }.commit()
        }
    }

    private fun openSettings() {
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.settings_title)).performClick()
        compose.onNodeWithTag("settings-home").assertIsDisplayed()
    }

    private fun openHomeRow(tag: String) {
        compose.onNodeWithTag("settings-home").performScrollToNode(hasTestTag(tag))
        compose.onNodeWithTag(tag).performClick()
    }

    private fun replaceField(field: String, value: String) {
        compose.onNodeWithTag("profile-$field").performScrollTo().performTextReplacement(value)
    }

    private fun toolbarBack(fixture: Fixture) {
        compose.onNodeWithContentDescription(fixture.text(R.string.action_back)).performClick()
    }

    private fun systemBack() {
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
    }

    private fun capture(name: String) {
        if (InstrumentationRegistry.getArguments().getString("uiSettingsScreenshots") != "true") return
        compose.waitForIdle()
        val target = if (compose.onAllNodes(isDialog()).fetchSemanticsNodes().isNotEmpty()) compose.onNode(isDialog()) else compose.onRoot()
        val bitmap = target.captureToImage().asAndroidBitmap()
        val directory = File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir, "ui-colors-settings").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
