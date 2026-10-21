package com.mankeu.presentation

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
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
import com.mankeu.importer.FinanceWorkbookImporter
import com.mankeu.presentation.screens.WelcomeScreen
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class WelcomeScreenTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext.withAppLanguage()
    private fun text(id: Int) = context.getString(id)

    private fun setContent(content: @Composable () -> Unit) {
        val localized = context
        compose.setContent {
            val host = LocalContext.current
            val wrapped = remember(host) {
                object : ContextWrapper(host) {
                    override fun getResources(): Resources = localized.resources
                }
            }
            CompositionLocalProvider(LocalContext provides wrapped, LocalConfiguration provides localized.resources.configuration) {
                content()
            }
        }
    }

    @Test fun bothChoicesAreVisibleAndGoogleDoesNotCompleteOfflineChoice() {
        var google = 0
        var offline = 0
        setContent { MankeuTheme(darkTheme = false) { WelcomeScreen(false, { google++ }, { offline++ }) } }
        compose.onNodeWithText(text(R.string.welcome_title)).assertIsDisplayed()
        capture("light")
        compose.onNodeWithText(text(R.string.welcome_google)).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, google); assertEquals(0, offline) }
        compose.onNodeWithText(text(R.string.welcome_offline)).assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, offline) }
    }

    @Test fun busyGoogleKeepsOfflineEntryEnabledInDarkTheme() {
        var offline = false
        setContent { MankeuTheme(darkTheme = true) { WelcomeScreen(true, { fail("Duplicate sign-in") }, { offline = true }) } }
        compose.onNodeWithText(text(R.string.welcome_signing_in)).performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText(text(R.string.welcome_offline)).performScrollTo().assertIsEnabled()
        capture("dark-busy")
        compose.onNodeWithText(text(R.string.welcome_offline)).performClick()
        compose.runOnIdle { assertTrue(offline) }
    }

    @Test fun largeTextKeepsBothChoicesReachable() {
        setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                MankeuTheme(darkTheme = false) { WelcomeScreen(false, {}, {}) }
            }
        }
        compose.onNodeWithText(text(R.string.welcome_google)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(text(R.string.welcome_offline)).performScrollTo().assertIsDisplayed()
        capture("font200")
    }

    @Test fun offlineChoiceReachesRealHomeAndSurvivesCompositionRestart() = withIsolatedApp { vm, _ ->
        var generation by mutableIntStateOf(0)
        setContent { key(generation) { MankeuApp(vm) } }
        compose.waitUntil(15_000) { !vm.uiState.value.isLoading }
        compose.onNodeWithText(text(R.string.welcome_offline)).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.nav_home)).assertIsDisplayed()
        compose.runOnIdle {
            assertTrue(AppPreferencesState(context).welcomeComplete)
            assertTrue(vm.uiState.value.businesses.isEmpty())
            assertNull(vm.authUser.value)
            generation++
        }
        compose.onNodeWithText(text(R.string.nav_home)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.welcome_offline)).assertDoesNotExist()
    }

    @Test fun failedSignInStaysOnWelcomeAndSuccessfulAuthEntersHome() = withIsolatedApp { vm, _ ->
        setContent { MankeuApp(vm) }
        compose.waitUntil(15_000) { !vm.uiState.value.isLoading }
        compose.onNodeWithText(text(R.string.welcome_google)).performScrollTo().assertIsDisplayed()
        compose.runOnIdle { vm.message.value = UiText.Res(R.string.welcome_network_error, isError = true) }
        compose.onNodeWithText(text(R.string.welcome_network_error)).assertIsDisplayed()
        compose.runOnIdle { assertFalse(AppPreferencesState(context).welcomeComplete) }
        compose.onNodeWithText(text(R.string.welcome_offline)).assertIsEnabled()
        compose.runOnIdle { vm.setAuthenticatedUser(AuthUser("welcome-test", null, "Demo", null)) }
        compose.onNodeWithText(text(R.string.nav_home)).assertIsDisplayed()
        compose.runOnIdle { assertTrue(AppPreferencesState(context).welcomeComplete) }
    }

    @Test fun existingBusinessBypassesWelcomeWithoutAuthentication() = withIsolatedApp { vm, repository ->
        runBlocking { repository.createBusiness("Usaha contoh") }
        setContent { MankeuApp(vm) }
        compose.waitUntil(15_000) { !vm.uiState.value.isLoading && vm.uiState.value.businesses.isNotEmpty() }
        compose.onNodeWithText(text(R.string.nav_home)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.welcome_google)).assertDoesNotExist()
        compose.runOnIdle { assertNull(vm.authUser.value) }
    }

    private fun withIsolatedApp(block: (AppViewModel, FinanceRepository) -> Unit) {
        val prefs = context.getSharedPreferences("mankeu_ui_prefs", Context.MODE_PRIVATE)
        val saved = prefs.all.toMap()
        prefs.edit().remove("welcome_complete").putBoolean("tour_offers_enabled", false).commit()
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        val store = ViewModelStore()
        val auth = FirebaseAuthManager()
        val savedUser = auth.currentUser.value
        val repository = FinanceRepository(database)
        val vm = AppViewModel(repository, FinanceWorkbookImporter(), EncryptedBackupManager(database, File(context.cacheDir, "welcome-test.aabk")), auth, GoogleDriveBackupClient())
        store.put("welcome-test", vm)
        auth.setCustomUser(null)
        try {
            block(vm, repository)
        } finally {
            compose.runOnIdle { store.clear(); auth.setCustomUser(savedUser) }
            database.close()
            prefs.edit().clear().apply {
                saved.forEach { (key, value) ->
                    when (value) {
                        is String -> putString(key, value)
                        is Boolean -> putBoolean(key, value)
                        is Int -> putInt(key, value)
                        is Long -> putLong(key, value)
                        is Float -> putFloat(key, value)
                        is Set<*> -> putStringSet(key, value.filterIsInstance<String>().toSet())
                    }
                }
            }.commit()
        }
    }

    private fun capture(label: String) {
        val prefix = InstrumentationRegistry.getArguments().getString("welcomeCapture", "phone")
        val out = File(context.filesDir, "welcome-ui").apply { mkdirs() }
        compose.onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
            File(out, "$prefix-$label.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
