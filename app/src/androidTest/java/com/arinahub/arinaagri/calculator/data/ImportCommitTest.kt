package com.arinahub.arinaagri.calculator.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arinahub.arinaagri.calculator.domain.BookMode
import com.arinahub.arinaagri.calculator.domain.FinancialEventType
import com.arinahub.arinaagri.calculator.importer.ImportBudgetCandidate
import com.arinahub.arinaagri.calculator.importer.ImportCandidate
import com.arinahub.arinaagri.calculator.importer.ImportClassification
import com.arinahub.arinaagri.calculator.importer.WorkbookImportPreview
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Instrumented because the commit path is Room, and money writes do not get to be verified by
 * inspection. Covers what [com.arinahub.arinaagri.calculator.importer.CanonicalRoundTripTest] cannot
 * reach: that a preview carrying rows for *both* books lands each row in the right book, and that
 * reversing the batch undoes all of it.
 */
@RunWith(AndroidJUnit4::class)
class ImportCommitTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: FinanceRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = FinanceRepository(database)
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun commitImportLandsEachRowInItsOwnBookAndReversesCleanly() = runBlocking {
        val businessId = repository.createBusiness("Tani Padi Pak Ade")
        val projectId = repository.createProject(businessId, "Sawah Blok A MT1")
        val books = database.financeDao().getBooks(projectId).associateBy { BookMode.valueOf(it.mode) }

        val committed = repository.commitImport(businessId, projectId, preview())

        assertEquals(4, committed)

        val projectionBook = books.getValue(BookMode.PROJECTION)
        val realizationBook = books.getValue(BookMode.REALIZATION)
        assertEquals(
            listOf("Rencana beli pupuk"),
            database.financeDao().getEventsForBook(projectionBook.id).map { it.description },
        )
        assertEquals(
            listOf("Beli pupuk NPK"),
            database.financeDao().getEventsForBook(realizationBook.id).map { it.description },
        )
        assertEquals(
            listOf("Pupuk NPK" to 1_200_000L),
            database.financeDao().getBudgetItems(projectionBook.id).map { it.name to it.plannedTotalRupiah },
        )
        assertEquals(
            listOf("Pupuk NPK terpakai" to 1_150_000L),
            database.financeDao().getBudgetItems(realizationBook.id).map { it.name to it.plannedTotalRupiah },
        )

        // A category named only in the workbook is created rather than refusing the row.
        assertTrue(database.financeDao().getCategories(businessId).any { it.name == "Pupuk Kandang" })

        val batchId = database.financeDao().getImportBatches(projectId).single().id
        val reversedRows = repository.reverseImportBatch(batchId)

        assertEquals(4, reversedRows)
        assertTrue(database.financeDao().getBudgetItems(projectionBook.id).isEmpty())
        assertTrue(database.financeDao().getBudgetItems(realizationBook.id).isEmpty())
        assertEquals(0, database.financeDao().getActiveEventsForImportBatch(batchId).size)
    }

    private fun preview() = WorkbookImportPreview(
        sourceName = "arina-agri-2026-08-28.xlsx",
        sourceSha256 = "sha-for-test",
        recognizedSheets = listOf("Metadata", "Transaksi Proyeksi", "Transaksi Realisasi"),
        candidates = listOf(
            candidate(BookMode.PROJECTION, "Rencana beli pupuk", 1_200_000),
            candidate(BookMode.REALIZATION, "Beli pupuk NPK", 1_150_000),
        ),
        warnings = emptyList(),
        errors = emptyList(),
        budgetCandidates = listOf(
            budget(BookMode.PROJECTION, "Pupuk NPK", 1_200_000, category = "Pupuk Kandang"),
            budget(BookMode.REALIZATION, "Pupuk NPK terpakai", 1_150_000),
        ),
    )

    private fun candidate(mode: BookMode, description: String, amount: Long) = ImportCandidate(
        type = FinancialEventType.CASH_EXPENSE,
        amountRupiah = amount,
        date = LocalDate.of(2026, 8, 15),
        description = description,
        category = "Pupuk",
        classification = ImportClassification.DIRECT,
        sourceEvidence = listOf("Transaksi ${mode.name}!A2"),
        rationale = "Uji impor",
        bookMode = mode,
    )

    private fun budget(mode: BookMode, name: String, total: Long, category: String = "Pupuk") =
        ImportBudgetCandidate(
            bookMode = mode,
            categoryName = category,
            name = name,
            plannedTotalRupiah = total,
            quantityText = "10",
            unit = "kg",
            unitPriceRupiah = total / 10,
        )
}
