package com.mankeu.presentation

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import androidx.compose.runtime.remember
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mankeu.R
import com.mankeu.auth.FirebaseAuthManager
import com.mankeu.backup.EncryptedBackupManager
import com.mankeu.backup.cloud.GoogleDriveBackupClient
import com.mankeu.data.AppDatabase
import com.mankeu.data.FinanceRepository
import com.mankeu.data.WorkspaceSelection
import com.mankeu.domain.CostNature
import com.mankeu.domain.Recurrence
import com.mankeu.importer.FinanceWorkbookImporter
import com.mankeu.importer.ImportBudgetLine
import com.mankeu.importer.ImportComponent
import com.mankeu.importer.ImportProduct
import com.mankeu.importer.ImportStockItem
import com.mankeu.importer.ImportTransaction
import com.mankeu.importer.WorkbookImportPreview
import com.mankeu.presentation.guide.HelpArticle
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

/**
 * Makes the Pusat Bantuan screenshots (owner decision Q12): fictional demo data, Indonesian, light
 * theme, each place highlighted the way "Tunjukkan di aplikasi" highlights it, without the card.
 *
 * Not a check: it only runs when `scripts/capture_help_screenshots.ps1` passes
 * `-e helpScreenshots true`, writes `files/help/help_*.webp` in the app's storage, and restores the
 * owner's display preferences afterwards.
 */
@RunWith(AndroidJUnit4::class)
class HelpScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    /**
     * Actual native navigation smoke/capture, enabled explicitly with `-e brandScreenshots true`.
     * `brandTheme` is light/dark and `brandLabel` identifies phone/tablet captures. The fixture uses
     * an in-memory database; neither the installed app's financial data nor its display preferences
     * are kept changed by this test. Images are unhighlighted, full-resolution (up to 1440px wide).
     */
    @Test
    fun captureBrandScreenshotsThroughRealNavigation() {
        val arguments = InstrumentationRegistry.getArguments()
        assumeTrue(arguments.getString("brandScreenshots") == "true")
        val theme = arguments.getString("brandTheme", "light")
        require(theme == "light" || theme == "dark") { "brandTheme must be light or dark" }
        val label = arguments.getString("brandLabel", "phone")
            .filter { it.isLetterOrDigit() || it == '-' }.take(32).ifBlank { "phone" }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("mankeu_ui_prefs", Context.MODE_PRIVATE)
        val saved = prefs.all.toMap()
        prefs.edit()
            .putString("theme_mode", if (theme == "dark") ThemeMode.DARK.name else ThemeMode.LIGHT.name)
            .putString("motion_mode", MotionMode.OFF.name)
            .putString("language", "id")
            .putBoolean("tour_offers_enabled", false).commit()
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        val viewModels = ViewModelStore()
        var showApp by mutableStateOf(true)
        try {
            val repository = FinanceRepository(database)
            val selection = runBlocking { seed(repository) }
            val viewModel = AppViewModel(
                repository, FinanceWorkbookImporter(), EncryptedBackupManager(database, File(context.cacheDir, "brand-shot.aabk")),
                FirebaseAuthManager(), GoogleDriveBackupClient(), rememberedSelection = selection,
            )
            viewModels.put("brand-shot", viewModel)
            val indonesian = Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag("id")) }
            val localized = context.createConfigurationContext(indonesian)
            compose.setContent {
                val host = LocalContext.current
                val wrapped = remember(host) { object : ContextWrapper(host) { override fun getResources(): Resources = localized.resources } }
                CompositionLocalProvider(LocalContext provides wrapped, LocalConfiguration provides indonesian) {
                    if (showApp) MankeuApp(viewModel)
                }
            }
            compose.waitUntil(timeoutMillis = 15_000) {
                val state = viewModel.uiState.value
                !state.isLoading && state.selectedProjectId == selection.projectId &&
                    state.events.size == 7 && state.stockItems.size == 2
            }
            compose.waitForIdle()
            val out = File(context.filesDir, "brand-ui").apply { mkdirs() }
            val screens = listOf(
                Triple(R.string.nav_home, R.string.home_balance_actual, "home"),
                Triple(R.string.nav_entry, R.string.entry_title, "entry"),
                Triple(R.string.nav_stock, R.string.stock_title, "stock"),
                Triple(R.string.nav_reports, R.string.reports_title, "reports"),
            )
            screens.forEach { (navigationLabel, screenLabel, destination) ->
                // The same label selects a bottom NavigationBarItem or an expanded NavigationRailItem.
                compose.onNodeWithText(localized.getString(navigationLabel)).performClick()
                compose.mainClock.advanceTimeBy(1000)
                compose.waitForIdle()
                compose.onNodeWithText("Kedai Kopi Senja").assertIsDisplayed()
                compose.onNodeWithText("Gerai Utama").assertIsDisplayed()
                compose.onNodeWithText(localized.getString(screenLabel)).assertIsDisplayed()
                val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
                save(bitmap, File(out, "$label-$theme-$destination.webp"), bitmap.width.coerceAtMost(1440))
            }
        } finally {
            // Unsubscribe Compose, then cancel the ViewModel's Room collectors before closing Room.
            compose.runOnIdle { showApp = false }
            compose.waitForIdle()
            compose.runOnIdle { viewModels.clear() }
            database.close()
            prefs.edit().clear().apply {
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

    @Test
    fun captureHelpScreenshots() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("helpScreenshots") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("mankeu_ui_prefs", Context.MODE_PRIVATE)
        val saved = prefs.all.toMap()
        prefs.edit().putString("theme_mode", ThemeMode.LIGHT.name).putString("motion_mode", MotionMode.OFF.name)
            .putBoolean("tour_offers_enabled", false).commit()
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        try {
            val repository = FinanceRepository(database)
            val selection = runBlocking { seed(repository) }
            val viewModel = AppViewModel(
                repository, FinanceWorkbookImporter(), EncryptedBackupManager(database, File(context.cacheDir, "help-shot.aabk")),
                FirebaseAuthManager(), GoogleDriveBackupClient(), rememberedSelection = selection,
            )
            val indonesian = Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag("id")) }
            val localized = context.createConfigurationContext(indonesian)
            var current by mutableStateOf<HelpArticle?>(null)
            compose.setContent {
                // Wraps the host Activity (file launchers look it up) and only swaps in Indonesian resources.
                val host = LocalContext.current
                val wrapped = remember(host) { object : ContextWrapper(host) { override fun getResources(): Resources = localized.resources } }
                CompositionLocalProvider(LocalContext provides wrapped, LocalConfiguration provides indonesian) {
                    MankeuApp(viewModel, captureShow = current)
                }
            }
            compose.waitForIdle()
            val out = File(context.filesDir, "help").apply { deleteRecursively(); mkdirs() }
            HelpArticle.entries.distinctBy { it.show.imageName }.forEach { article ->
                compose.runOnIdle { current = article }
                // The page transition, the show delay and the spotlight's scroll-to-target run on
                // the test's virtual clock: advance it, sleeping would not.
                repeat(30) { compose.mainClock.advanceTimeBy(100) }
                compose.waitForIdle()
                save(compose.onRoot().captureToImage().asAndroidBitmap(), File(out, article.show.imageName + ".webp"))
            }
        } finally {
            database.close()
            prefs.edit().clear().apply {
                saved.forEach { (key, value) ->
                    when (value) {
                        is Boolean -> putBoolean(key, value)
                        is Int -> putInt(key, value)
                        is Long -> putLong(key, value)
                        is Float -> putFloat(key, value)
                        is String -> putString(key, value)
                    }
                }
            }.commit()
        }
    }

    /** A coffee stall with two products, a plan, this month's sales, stock and one import batch. */
    private suspend fun seed(repository: FinanceRepository): WorkspaceSelection {
        val business = repository.createBusiness("Kedai Kopi Senja")
        val unit = repository.createProject(business, "Gerai Utama")
        val today = LocalDate.now()
        val month = YearMonth.from(today)
        repository.commitImport(
            business, unit,
            WorkbookImportPreview(
                sourceName = "kedai-kopi-senja.xlsx",
                sourceSha256 = "help-screenshots",
                transactions = listOf(
                    ImportTransaction("T!A2", month.atDay(1), "Modal awal", "Setoran Modal Pemilik", incoming = true, amountRupiah = 15_000_000),
                    ImportTransaction("T!A3", month.atDay(1), "Mesin espresso", "Beli Peralatan & Aset", incoming = false, amountRupiah = 7_200_000, usefulLifeMonths = 36),
                    ImportTransaction("T!A4", month.atDay(1), "Sewa lapak", "Sewa Tempat", incoming = false, amountRupiah = 1_000_000),
                    ImportTransaction("T!A5", month.atDay(1), "Belanja susu, kopi, cup", "Bahan Baku & Stok", incoming = false, amountRupiah = 2_350_000),
                    ImportTransaction("T!A6", today, "Jual kopi susu", "Penjualan & Jasa", incoming = true, amountRupiah = 4_600_000, productName = "Kopi Susu", quantityText = "460"),
                    ImportTransaction("T!A7", today, "Jual es teh", "Penjualan & Jasa", incoming = true, amountRupiah = 900_000, productName = "Es Teh", quantityText = "180"),
                    ImportTransaction("T!A8", today, "Gaji pembantu", "Gaji & Upah", incoming = false, amountRupiah = 800_000),
                ),
                budgetLines = listOf(
                    ImportBudgetLine("R!A2", null, "Sewa Tempat", "Sewa lapak", null, null, null, 1_000_000, Recurrence.MONTHLY, CostNature.FIXED),
                    ImportBudgetLine("R!A3", null, "Gaji & Upah", "Gaji pembantu", null, null, null, 800_000, Recurrence.MONTHLY, CostNature.FIXED),
                    ImportBudgetLine("R!A4", null, "Listrik, Air & Internet", "Listrik dan air", null, null, null, 200_000, Recurrence.MONTHLY, CostNature.FIXED),
                    ImportBudgetLine("R!A5", month, "Beli Peralatan & Aset", "Mesin espresso", null, null, null, 7_200_000, Recurrence.ONCE, usefulLifeMonths = 36),
                    ImportBudgetLine("R!A6", month, "Setoran Modal Pemilik", "Modal awal", null, null, null, 15_000_000),
                ),
                products = listOf(
                    ImportProduct("P!A2", "Kopi Susu", "cup", 10_000, "700", listOf(ImportComponent("Susu", 18_000, "6"), ImportComponent("Kopi dan cup", 2_000, "1"))),
                    ImportProduct("P!A3", "Es Teh", "gelas", 5_000, "400", listOf(ImportComponent("Teh, gula, es", 1_500, "1"))),
                ),
                stockItems = listOf(
                    ImportStockItem("S!A2", "Susu UHT", "liter", 18_000, "24", "6"),
                    ImportStockItem("S!A3", "Biji kopi", "kg", 120_000, "3", "1"),
                ),
                planPeriod = month to month.plusMonths(11),
            ),
        )
        return WorkspaceSelection(business, unit)
    }

    private fun save(bitmap: Bitmap, file: File, width: Int = 540) {
        val scaled = Bitmap.createScaledBitmap(bitmap, width, bitmap.height * width / bitmap.width, true)
        @Suppress("DEPRECATION")
        val format = if (Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP
        file.outputStream().use { scaled.compress(format, 82, it) }
    }
}
