package com.mankeu.presentation

import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
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
import com.mankeu.data.EventDraft
import com.mankeu.data.FinanceRepository
import com.mankeu.data.WorkspaceSelection
import com.mankeu.domain.CategoryRole
import com.mankeu.domain.FinancialEventType
import com.mankeu.importer.FinanceWorkbookImporter
import com.mankeu.importer.ImportTransaction
import com.mankeu.importer.WorkbookImportPreview
import com.mankeu.presentation.screens.HomeScreen
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

/** Reconciles cash versus after-depreciation profit, then distinguishes all records from this month. */
@RunWith(AndroidJUnit4::class)
class CardPeriodSemanticsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun homeAllRecordsAndCurrentMonthRemainExplicitAcrossTwoMonths() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        val store = ViewModelStore()
        var showHome by mutableStateOf(true)
        try {
            val repository = FinanceRepository(database)
            val today = LocalDate.now()
            val month = YearMonth.from(today)
            val selection = runBlocking {
                val business = repository.createBusiness("Kedai Kopi Contoh")
                val unit = repository.createProject(business, "Gerai Contoh")
                repository.commitImport(
                    business, unit,
                    WorkbookImportPreview(
                        sourceName = "card-period-synthetic.xlsx", sourceSha256 = "card-period-synthetic",
                        transactions = listOf(
                            ImportTransaction("T!A2", month.atDay(1), "Modal awal", "Setoran Modal Pemilik", incoming = true, amountRupiah = 15_000_000),
                            ImportTransaction("T!A3", month.atDay(1), "Mesin espresso", "Beli Peralatan & Aset", incoming = false, amountRupiah = 7_200_000, usefulLifeMonths = 36),
                            ImportTransaction("T!A4", month.atDay(1), "Sewa lapak", "Sewa Tempat", incoming = false, amountRupiah = 1_000_000),
                            ImportTransaction("T!A5", month.atDay(1), "Belanja bahan", "Bahan Baku & Stok", incoming = false, amountRupiah = 2_350_000),
                            ImportTransaction("T!A6", today, "Jual kopi susu", "Penjualan & Jasa", incoming = true, amountRupiah = 4_600_000),
                            ImportTransaction("T!A7", today, "Jual es teh", "Penjualan & Jasa", incoming = true, amountRupiah = 900_000),
                            ImportTransaction("T!A8", today, "Gaji pembantu", "Gaji & Upah", incoming = false, amountRupiah = 800_000),
                        ),
                    ),
                )
                WorkspaceSelection(business, unit)
            }
            val viewModel = AppViewModel(
                repository, FinanceWorkbookImporter(), EncryptedBackupManager(database, File(context.cacheDir, "card-period-synthetic.aabk")),
                FirebaseAuthManager().apply { setCustomUser(null) }, GoogleDriveBackupClient(), rememberedSelection = selection,
            )
            store.put("card-period", viewModel)
            val configuration = Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag("id")) }
            val localized = context.createConfigurationContext(configuration)
            compose.setContent {
                val host = LocalContext.current
                val wrapped = remember(host) { object : ContextWrapper(host) { override fun getResources(): Resources = localized.resources } }
                val state by viewModel.uiState.collectAsState()
                CompositionLocalProvider(LocalContext provides wrapped, LocalConfiguration provides configuration) {
                    MankeuTheme(darkTheme = false, motion = MankeuMotion.Off) {
                        if (showHome) HomeScreen(state, onNavigate = {}, onReverse = { _, _ -> }, onOpenData = {})
                    }
                }
            }
            compose.waitUntil(15_000) {
                val state = viewModel.uiState.value
                state.selectedProjectId == selection.projectId && state.events.size == 7 && state.actualMonth != null && state.summary.cashBalanceRupiah == 9_150_000L
            }
            compose.runOnIdle {
                val state = viewModel.uiState.value
                assertEquals(9_150_000L, state.summary.cashBalanceRupiah)
                assertEquals(5_500_000L, state.summary.operatingIncomeRupiah)
                assertEquals(4_150_000L, state.summary.operatingExpenseRupiah)
                assertEquals(1_350_000L, state.summary.profitRupiah)
                assertEquals(month, state.actualMonth!!.month)
                assertEquals(4_350_000L, state.actualMonth!!.totalCostRupiah)
                assertEquals(200_000L, state.actualMonth!!.totalCostRupiah - state.summary.operatingExpenseRupiah)
                assertEquals(1_150_000L, state.actualMonth!!.profitRupiah)
            }
            val profitTitle = localized.getString(R.string.home_cash_profit)
            val allRecords = localized.getString(R.string.home_period_all_records)
            compose.onNodeWithText(profitTitle).performScrollTo().assertIsDisplayed()
            compose.onNode(hasText(allRecords) and hasAnySibling(hasText(profitTitle))).assertIsDisplayed()
            compose.onNode(hasText(1_350_000L.toRupiah()) and hasAnySibling(hasText(profitTitle))).performScrollTo().assertIsDisplayed()

            val state = viewModel.uiState.value
            val incomeCategory = state.categories.first { CategoryRole.parse(it.role) == CategoryRole.REVENUE }
            runBlocking {
                repository.recordEvent(
                    EventDraft(
                        businessId = selection.businessId!!, projectId = selection.projectId!!,
                        bookId = requireNotNull(state.selectedBookId), type = FinancialEventType.CASH_INCOME,
                        amountRupiah = 1_000_000, date = month.minusMonths(1).atDay(15),
                        description = "Penjualan bulan sebelumnya", category = incomeCategory.name, categoryId = incomeCategory.id,
                    ),
                )
            }
            compose.waitUntil(15_000) { viewModel.uiState.value.summary.operatingIncomeRupiah == 6_500_000L && viewModel.uiState.value.realizationMonths == 2 }
            compose.runOnIdle {
                val updated = viewModel.uiState.value
                assertEquals(10_150_000L, updated.summary.cashBalanceRupiah)
                assertEquals(2_350_000L, updated.summary.profitRupiah)
                assertEquals(5_500_000L, updated.actualMonth!!.revenueRupiah)
                assertEquals(1_150_000L, updated.actualMonth!!.profitRupiah)
            }
            compose.onNodeWithText(profitTitle).performScrollTo().assertIsDisplayed()
            compose.onNode(hasText(allRecords) and hasAnySibling(hasText(profitTitle))).assertIsDisplayed()
            compose.onNode(hasText(2_350_000L.toRupiah()) and hasAnySibling(hasText(profitTitle))).performScrollTo().assertIsDisplayed()
        } finally {
            compose.runOnIdle { showHome = false }
            compose.waitForIdle()
            compose.runOnIdle { store.clear() }
            database.close()
        }
    }
}
