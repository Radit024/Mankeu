package com.mankeu.presentation

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
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
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class UiRefinementWorkflowTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun testFullUiRefinementWorkflow() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("mankeu_ui_prefs", Context.MODE_PRIVATE)
        val saved = prefs.all.toMap()
        prefs.edit()
            .putString("theme_mode", ThemeMode.LIGHT.name)
            .putString("motion_mode", MotionMode.OFF.name)
            .putString("language", "id")
            .putBoolean("tour_offers_enabled", false)
            .commit()

        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        val viewModels = ViewModelStore()
        var showApp by mutableStateOf(true)

        try {
            val repository = FinanceRepository(database)
            val selection = runBlocking { seedFixture(repository) }
            val viewModel = AppViewModel(
                repository,
                FinanceWorkbookImporter(),
                EncryptedBackupManager(database, File(context.cacheDir, "workflow-test.aabk")),
                FirebaseAuthManager(),
                GoogleDriveBackupClient(),
                rememberedSelection = selection,
            )
            viewModels.put("workflow-test", viewModel)

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

            // 1. Verify Home screen metrics
            compose.onNodeWithText("Kedai Kopi Senja").assertIsDisplayed()
            compose.onNodeWithText("Gerai Utama").assertIsDisplayed()
            compose.onNodeWithText(localized.getString(R.string.home_cash_flow)).assertIsDisplayed()
            compose.onNodeWithText(localized.getString(R.string.home_profit)).assertIsDisplayed()
            compose.onNodeWithText(localized.getString(R.string.reports_operating_income)).assertIsDisplayed()
            compose.onNodeWithText(localized.getString(R.string.reports_operating_expense)).assertIsDisplayed()

            // 2. Navigate to Entry screen and verify ledger scanning
            compose.onNodeWithText(localized.getString(R.string.nav_entry)).performClick()
            compose.mainClock.advanceTimeBy(1000)
            compose.waitForIdle()
            compose.onNodeWithText(localized.getString(R.string.entry_title)).assertIsDisplayed()
            compose.onNodeWithText("Jual kopi susu").assertIsDisplayed()

            // 3. Switch to Proyeksi mode to verify Plan screen
            compose.onNodeWithText(localized.getString(R.string.scope_mode_plan)).performClick()
            compose.mainClock.advanceTimeBy(1000)
            compose.waitForIdle()
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Kopi Susu", substring = true))
            compose.onNodeWithText("Kopi Susu", substring = true).assertIsDisplayed()

            // Switch back to Realisasi
            compose.onNodeWithText(localized.getString(R.string.scope_mode_actual)).performClick()
            compose.mainClock.advanceTimeBy(1000)
            compose.waitForIdle()

            // 4. Navigate to Stock screen and verify stock item hierarchy
            compose.onNodeWithText(localized.getString(R.string.nav_stock)).performClick()
            compose.mainClock.advanceTimeBy(1000)
            compose.waitForIdle()
            compose.onNodeWithText(localized.getString(R.string.stock_title)).assertIsDisplayed()
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Susu UHT", substring = true))
            compose.onNodeWithText("Susu UHT", substring = true).assertIsDisplayed()

            // 5. Navigate to Reports screen and verify reports section
            compose.onNodeWithText(localized.getString(R.string.nav_reports)).performClick()
            compose.mainClock.advanceTimeBy(1000)
            compose.waitForIdle()
            compose.onNodeWithText(localized.getString(R.string.reports_title)).assertIsDisplayed()

            // 6. Open Berkas dan Data (More / Data overlay)
            compose.onNodeWithContentDescription(localized.getString(R.string.data_title)).performClick()
            compose.mainClock.advanceTimeBy(1000)
            compose.waitForIdle()
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(localized.getString(R.string.data_export_excel), substring = true))
            compose.onNodeWithText(localized.getString(R.string.data_export_excel), substring = true).assertIsDisplayed()
        } finally {
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

    private suspend fun seedFixture(repository: FinanceRepository): WorkspaceSelection {
        val business = repository.createBusiness("Kedai Kopi Senja")
        val unit = repository.createProject(business, "Gerai Utama")
        val today = LocalDate.now()
        val month = YearMonth.from(today)
        repository.commitImport(
            business, unit,
            WorkbookImportPreview(
                sourceName = "kedai-kopi-senja.xlsx",
                sourceSha256 = "workflow-test",
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
}
