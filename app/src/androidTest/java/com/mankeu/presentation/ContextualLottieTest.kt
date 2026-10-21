package com.mankeu.presentation

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.activity.ComponentActivity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.mankeu.R
import com.mankeu.auth.AuthUser
import com.mankeu.auth.FirebaseAuthManager
import com.mankeu.backup.EncryptedBackupManager
import com.mankeu.backup.cloud.GoogleDriveBackupClient
import com.mankeu.data.AppDatabase
import com.mankeu.data.FinanceRepository
import com.mankeu.data.WorkspaceSelection
import com.mankeu.domain.BookMode
import com.mankeu.importer.FinanceWorkbookImporter
import com.mankeu.presentation.components.AnimationLicenseButton
import com.mankeu.presentation.components.StateArtwork
import com.mankeu.presentation.components.StateIllustration
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.After
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ContextualLottieTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private var originalConfiguration: Configuration? = null
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext.withAppLanguage()
    private fun text(id: Int) = context.getString(id)
    private val output get() = File(context.filesDir, "lottie-proof").apply { mkdirs() }

    @Test fun allLightAndDarkAssetsRenderMeaningfulHeldFramesAndRealMotion() {
        val board = Bitmap.createBitmap(512, StateArtwork.entries.size * 256, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(board)
        StateArtwork.entries.forEachIndexed { row, artwork ->
            listOf(artwork.resource, artwork.darkResource).forEachIndexed { column, resource ->
                val parsed = LottieCompositionFactory.fromRawResSync(context, resource)
                assertNotNull("${artwork.name}: ${parsed.exception}", parsed.value)
                val composition = parsed.value!!
                val drawable = LottieDrawable().apply { setComposition(composition); setBounds(0, 0, 256, 256) }
                val image = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
                drawable.progress = artwork.stillProgress
                drawable.draw(Canvas(image))
                val pixels = IntArray(256 * 256)
                image.getPixels(pixels, 0, 256, 0, 0, 256, 256)
                val colored = pixels.count { pixel ->
                    AndroidColor.alpha(pixel) > 80 &&
                        maxOf(AndroidColor.red(pixel), AndroidColor.green(pixel), AndroidColor.blue(pixel)) -
                        minOf(AndroidColor.red(pixel), AndroidColor.green(pixel), AndroidColor.blue(pixel)) > 20
                }
                assertTrue("${artwork.name} variant $column needs meaningful visible colour: $colored", colored > 100)
                canvas.save()
                canvas.translate(column * 256f, row * 256f)
                canvas.clipRect(0f, 0f, 256f, 256f)
                canvas.drawColor(if (column == 0) AndroidColor.rgb(250,250,248) else AndroidColor.rgb(30,38,32))
                canvas.drawBitmap(image, 0f, 0f, null)
                canvas.restore()
                // A nonzero timeline alone is insufficient: at least two rendered frames must differ.
                val frames = listOf(0.15f, 0.45f, 0.75f).map { progress ->
                    val frame = Bitmap.createBitmap(128,128,Bitmap.Config.ARGB_8888)
                    drawable.setBounds(0,0,128,128)
                    drawable.progress = progress
                    drawable.draw(Canvas(frame))
                    IntArray(128*128).also { frame.getPixels(it,0,128,0,0,128,128); frame.recycle() }.contentHashCode()
                }
                assertTrue("${artwork.name} must actually animate", frames.distinct().size > 1)
                image.recycle()
            }
        }
        save(board, "all-artwork-light-dark")
    }

    @Test fun reducedAndOffArtworkDoNotAdvanceWithTheClock() {
        var mode by mutableStateOf(MankeuMotion.Reduced)
        var loaded = false
        // Let asynchronous composition loading settle before freezing the test clock.
        setContent {
            val result = rememberLottieComposition(LottieCompositionSpec.RawRes(StateArtwork.BUSINESS_PLAN.resource))
            val ready = result.value != null
            SideEffect { loaded = ready }
            MankeuTheme(darkTheme = false, motion = mode) { StateIllustration(StateArtwork.BUSINESS_PLAN, Modifier.size(160.dp)) }
        }
        // Parsing is asynchronous; wait for the composition cache populated by the native loader.
        assertNotNull(LottieCompositionFactory.fromRawResSync(context, StateArtwork.BUSINESS_PLAN.resource).value)
        compose.waitUntil(15_000) { loaded }
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
        val before = compose.onRoot().captureToImage().asAndroidBitmap()
        compose.mainClock.advanceTimeBy(2000)
        val reduced = compose.onRoot().captureToImage().asAndroidBitmap()
        assertTrue("Reduced must remain still", before.sameAs(reduced))
        compose.runOnIdle { mode = MankeuMotion.Off }
        compose.mainClock.advanceTimeBy(500)
        val offBefore = compose.onRoot().captureToImage().asAndroidBitmap()
        compose.mainClock.advanceTimeBy(2000)
        assertTrue("Off must remain still", offBefore.sameAs(compose.onRoot().captureToImage().asAndroidBitmap()))
    }

    @Test fun animationLicenseAndAllCreatorCreditsAreAvailableOffline() {
        setContent { MankeuTheme { AnimationLicenseButton() } }
        compose.onNodeWithText(text(R.string.lottie_license_title)).performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithText("Md. Imam Hossain", substring = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("FL 9.13.21", substring = true).assertExists()
        listOf("Zakaria Harry Utomo", "Jeffrey Christopher", "Kohinoor Film", "MD Abdur Rahim").forEach {
            compose.onNodeWithText(it, substring = true).assertExists()
        }
        compose.onNodeWithText(text(R.string.action_close)).performClick()
    }

    @Test fun realNavigationUsesEachArtworkOnlyInItsRelevantContext() {
        val prefs = context.getSharedPreferences("mankeu_ui_prefs", Context.MODE_PRIVATE)
        val saved = prefs.all.toMap()
        val theme = InstrumentationRegistry.getArguments().getString("lottieTheme", "light")
        prefs.edit().putBoolean("welcome_complete",true).putBoolean("tour_offers_enabled",false)
            .putString("motion_mode", MotionMode.OFF.name).putString("theme_mode",if (theme=="dark") ThemeMode.DARK.name else ThemeMode.LIGHT.name).commit()
        val db = Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).allowMainThreadQueries().build()
        val repository = FinanceRepository(db)
        val selection = runBlocking { val b=repository.createBusiness("Usaha Contoh"); WorkspaceSelection(b,repository.createProject(b,"Unit Contoh")) }
        val auth = FirebaseAuthManager()
        val originalUser = auth.currentUser.value
        auth.setCustomUser(AuthUser("lottie-test", "demo@example.invalid", "Demo", null))
        val vm = AppViewModel(repository,FinanceWorkbookImporter(),EncryptedBackupManager(db,File(context.cacheDir,"lottie-test.aabk")),auth,GoogleDriveBackupClient(),rememberedSelection=selection)
        val store = ViewModelStore().apply { put("lottie",vm) }
        var mounted by mutableStateOf(true)
        try {
            setContent { if(mounted) MankeuApp(vm) }
            compose.waitUntil(15_000) { !vm.uiState.value.isLoading && vm.uiState.value.selectedProjectId==selection.projectId }
            compose.onNodeWithText(text(R.string.nav_reports)).performClick()
            compose.onNodeWithTag("artwork-REPORTS").assertIsDisplayed()
            capture("$theme-reports", StateArtwork.REPORTS)
            compose.onNodeWithText(text(R.string.nav_stock)).performClick()
            compose.onNodeWithTag("artwork-STOCK").assertIsDisplayed()
            compose.onNodeWithText(text(R.string.stock_add_item)).assertIsEnabled()
            capture("$theme-stock", StateArtwork.STOCK)
            compose.onNodeWithText(text(R.string.scope_mode_plan)).performClick()
            compose.onNodeWithText(text(R.string.nav_entry)).performClick()
            compose.onNodeWithTag("artwork-BUSINESS_PLAN").assertIsDisplayed()
            capture("$theme-plan", StateArtwork.BUSINESS_PLAN)
            compose.onNodeWithContentDescription(text(R.string.data_title)).performClick()
            compose.onNodeWithTag("artwork-IMPORT").assertIsDisplayed()
            compose.onNodeWithText(text(R.string.import_pick)).assertIsEnabled()
            compose.onNodeWithTag("artwork-CLOUD_BACKUP").assertDoesNotExist()
            capture("$theme-import", StateArtwork.IMPORT)
            compose.runOnIdle { vm.setAuthenticatedUser(AuthUser("lottie-test", "demo@example.invalid", "Demo", null)) }
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(text(R.string.data_export_backup),substring=true))
            compose.onNodeWithText(text(R.string.data_export_backup),substring=true).performClick()
            compose.onNodeWithTag("artwork-CLOUD_BACKUP").assertDoesNotExist()
            compose.onNodeWithText(text(R.string.backup_create_cta)).performClick()
            compose.onNodeWithTag("artwork-CLOUD_BACKUP").assertDoesNotExist()
            compose.onNodeWithText(text(R.string.action_back_step)).performClick()
            compose.onNodeWithText(text(R.string.data_export_drive_backup)).performClick()
            compose.onNodeWithTag("artwork-CLOUD_BACKUP").assertIsDisplayed()
            capture("$theme-cloud", StateArtwork.CLOUD_BACKUP)
            compose.runOnIdle { assertTrue(vm.uiState.value.events.isEmpty()); assertTrue(vm.uiState.value.stockItems.isEmpty()) }
        } finally {
            compose.runOnIdle { mounted=false }
            compose.waitForIdle()
            compose.runOnIdle { store.clear(); auth.setCustomUser(originalUser) }
            db.close()
            prefs.edit().clear().apply {
                saved.forEach { (key,value) -> when(value) {
                    is String -> putString(key,value); is Boolean -> putBoolean(key,value); is Int -> putInt(key,value)
                    is Long -> putLong(key,value); is Float -> putFloat(key,value)
                    is Set<*> -> putStringSet(key,value.filterIsInstance<String>().toSet())
                } }
            }.commit()
        }
    }

    private fun setContent(content: @Composable () -> Unit) {
        val localized=context
        // Match MainActivity's localized host resources, including new dialog windows. A wrapper
        // LocalContext alone localizes the screen but leaves dialog buttons in the device locale.
        compose.runOnUiThread {
            val resources = compose.activity.resources
            if (originalConfiguration == null) originalConfiguration = Configuration(resources.configuration)
            @Suppress("DEPRECATION")
            resources.updateConfiguration(localized.resources.configuration, resources.displayMetrics)
        }
        compose.setContent(content)
    }
    @After fun restoreHostConfiguration() {
        originalConfiguration?.let { configuration -> compose.runOnUiThread {
            val resources = compose.activity.resources
            @Suppress("DEPRECATION")
            resources.updateConfiguration(configuration, resources.displayMetrics)
        } }
    }
    private fun capture(name:String, artwork:StateArtwork) {
        val root = isRoot() and hasAnyDescendant(hasTestTag("artwork-${artwork.name}"))
        compose.onNode(root).assertExists()
        // PixelCopy-based Compose capture can sample the Activity underneath a separate modal
        // window. UiAutomation captures the composed display, including the actual backup sheet.
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        automation.waitForIdle(300, 5000)
        save(requireNotNull(automation.takeScreenshot()),name)
    }
    private fun save(bitmap:Bitmap,name:String) { File(output,"$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) } }
}
