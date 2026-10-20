package com.mankeu.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mankeu.domain.BookMode
import com.mankeu.domain.CategoryRole
import com.mankeu.domain.FinancialEventType
import com.mankeu.importer.ImportBepParameters
import com.mankeu.importer.ImportBudgetLine
import com.mankeu.importer.ImportStockItem
import com.mankeu.importer.ImportTransaction
import com.mankeu.importer.WorkbookImportPreview
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.YearMonth

/**
 * The template commit path (DEC-012) against a real Room database: rows land in the right book with
 * the type their category implies, unknown categories become custom ones, the batch reverses
 * cleanly, and an identical file may be imported again after a warning.
 */
@RunWith(AndroidJUnit4::class)
class ImportCommitTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: FinanceRepository
    private lateinit var businessId: String
    private lateinit var projectId: String
    private val dao get() = database.financeDao()

    @Before
    fun setUp() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        repository = FinanceRepository(database)
        businessId = repository.createBusiness("Kedai Kopi")
        projectId = repository.createProject(businessId, "Gerai Utama")
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun templateRowsLandInTheirBooksWithCategoryDrivenTypesAndReverseCleanly() = runBlocking {
        val review = repository.reviewImport(businessId, preview())
        assertEquals(listOf("Sewa Gerobak" to CategoryRole.EXPENSE), review.newCategories)
        assertEquals(emptyList<String>(), review.conflicts)
        assertFalse(review.previouslyImported)

        assertEquals(5, repository.commitImport(businessId, projectId, preview()))

        val books = dao.getBooks(projectId).associateBy { BookMode.valueOf(it.mode) }
        val actual = dao.getEventsForBook(books.getValue(BookMode.REALIZATION).id).associate { it.description to it.type }
        assertEquals(
            mapOf(
                "Setoran modal" to FinancialEventType.OWNER_CAPITAL.name,
                "Jual kopi" to FinancialEventType.CASH_INCOME.name,
                "Sewa gerobak Agustus" to FinancialEventType.CASH_EXPENSE.name,
            ),
            actual,
        )
        val plan = books.getValue(BookMode.PROJECTION).id
        assertEquals(listOf("Biji kopi" to 1_200_000L), dao.getBudgetItems(plan).map { it.name to it.plannedTotalRupiah })
        assertEquals(2_000_000L, dao.getAssumptions(plan)?.fixedCostRupiah)
        assertEquals(listOf("Gula"), database.stockDao().getStockItems(businessId).map { it.name })
        val summary = repository.accountingSummary(books.getValue(BookMode.REALIZATION).id).first()
        // Sale 4.5 jt minus rent 0.5 jt; the 5 jt capital moves cash, not profit.
        assertEquals(4_000_000L, summary.profitRupiah)
        assertEquals(9_000_000L, summary.cashBalanceRupiah)

        repository.reverseImportBatch(dao.getImportBatches(projectId).single().id)

        assertEquals(0L, repository.accountingSummary(books.getValue(BookMode.REALIZATION).id).first().cashBalanceRupiah)
        assertTrue(dao.getBudgetItems(plan).all { it.archived })
        assertTrue(database.stockDao().getStockItems(businessId).isEmpty())
    }

    @Test
    fun aRowWhoseDirectionDoesNotFitItsCategoryIsRefused() = runBlocking {
        val clash = WorkbookImportPreview(
            "salah.xlsx",
            "sha-clash",
            transactions = listOf(ImportTransaction("Transaksi!A2", DAY, "Gaji masuk?", "Gaji & Upah", incoming = true, amountRupiah = 100_000)),
        )

        assertEquals(listOf("Transaksi!A2: kategori \"Gaji & Upah\" bukan untuk Pemasukan."), repository.reviewImport(businessId, clash).conflicts)
        assertTrue(runCatching { repository.commitImport(businessId, projectId, clash) }.isFailure)
        assertTrue(dao.getImportBatches(projectId).isEmpty())
    }

    @Test
    fun anIdenticalFileCanBeImportedAgainAfterAWarning() = runBlocking {
        repository.commitImport(businessId, projectId, preview())

        assertTrue(repository.reviewImport(businessId, preview()).previouslyImported)
        repository.commitImport(businessId, projectId, preview())

        val actual = dao.getBooks(projectId).first { it.mode == BookMode.REALIZATION.name }.id
        assertEquals(18_000_000L, repository.accountingSummary(actual).first().cashBalanceRupiah)
    }

    private fun preview() = WorkbookImportPreview(
        sourceName = "kedai.xlsx",
        sourceSha256 = "sha-kedai",
        transactions = listOf(
            ImportTransaction("Transaksi!A2", DAY, "Setoran modal", "Setoran Modal Pemilik", incoming = true, amountRupiah = 5_000_000),
            ImportTransaction("Transaksi!A3", DAY, "Jual kopi", "Penjualan & Jasa", incoming = true, amountRupiah = 4_500_000),
            ImportTransaction("Transaksi!A4", DAY, "Sewa gerobak Agustus", "Sewa Gerobak", incoming = false, amountRupiah = 500_000),
        ),
        budgetLines = listOf(ImportBudgetLine("Rencana Anggaran!A2", YearMonth.of(2026, 8), "Bahan Baku & Stok", "Biji kopi", "10", "kg", 120_000, 1_200_000)),
        bepParameters = ImportBepParameters(unit = "cup", priceRupiah = 10_000, variableCostRupiah = 5_000, fixedCostRupiah = 2_000_000),
        stockItems = listOf(ImportStockItem("Stok Barang!A2", "Gula", "kg", 15_000, "5", "1")),
    )

    private companion object {
        val DAY: LocalDate = LocalDate.of(2026, 8, 15)
    }
}
