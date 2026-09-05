package com.arinahub.arinaagri.calculator.importer

import com.arinahub.arinaagri.calculator.data.BookEntity
import com.arinahub.arinaagri.calculator.data.BudgetItemEntity
import com.arinahub.arinaagri.calculator.data.BusinessEntity
import com.arinahub.arinaagri.calculator.data.CategoryEntity
import com.arinahub.arinaagri.calculator.data.FinancialEventEntity
import com.arinahub.arinaagri.calculator.data.ProjectEntity
import com.arinahub.arinaagri.calculator.domain.BookMode
import com.arinahub.arinaagri.calculator.domain.CategoryRole
import com.arinahub.arinaagri.calculator.domain.FinancialEventType
import com.arinahub.arinaagri.calculator.exporter.FinanceExportBook
import com.arinahub.arinaagri.calculator.exporter.FinanceExportBundle
import com.arinahub.arinaagri.calculator.exporter.FinanceWorkbookExporter
import com.arinahub.arinaagri.calculator.domain.FinancialSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * The export must be readable back in without losing a book or a plan.
 *
 * Round 3 shipped an exporter that writes `Anggaran`/`Transaksi` for both books and an importer
 * that read only `Transaksi Realisasi`, so a round trip silently dropped the entire Proyeksi book
 * and every RAB line. This test drives the *real* exporter, so it also fails if either side's sheet
 * names or column headers drift apart again.
 */
class CanonicalRoundTripTest {

    @Test
    fun `export round-trips both books and their budget lines`() {
        val bytes = FinanceWorkbookExporter().export(bundle())

        val preview = FinanceWorkbookImporter().preview("arina-agri-2026-08-28.xlsx", bytes)

        assertTrue(preview.errors.joinToString(), preview.canCommit)

        val byMode = preview.candidates.groupBy { it.bookMode }
        assertEquals(
            "projection transactions must survive the round trip",
            listOf("Rencana beli pupuk" to 1_200_000L),
            byMode.getValue(BookMode.PROJECTION).map { it.description to it.amountRupiah },
        )
        assertEquals(
            listOf("Beli pupuk NPK" to 1_150_000L, "Jual gabah" to 4_000_000L),
            byMode.getValue(BookMode.REALIZATION).map { it.description to it.amountRupiah }.sortedBy { it.first },
        )

        val budgetsByMode = preview.budgetCandidates.groupBy { it.bookMode }
        assertEquals(
            listOf("Pupuk NPK" to 1_200_000L),
            budgetsByMode.getValue(BookMode.PROJECTION).map { it.name to it.plannedTotalRupiah },
        )
        assertEquals("Pupuk", budgetsByMode.getValue(BookMode.PROJECTION).single().categoryName)
        assertEquals("kg", budgetsByMode.getValue(BookMode.PROJECTION).single().unit)
        assertEquals(1, budgetsByMode.getValue(BookMode.REALIZATION).size)
    }

    @Test
    fun `archived budget lines and reversed events are not resurrected`() {
        val bytes = FinanceWorkbookExporter().export(
            bundle(
                extraProjectionBudget = budgetItem(
                    id = "budget-old",
                    bookId = "book-projection",
                    name = "Rencana lama",
                    total = 999_000,
                    archived = true,
                ),
                extraRealizationEvent = event(
                    id = "event-reversed",
                    bookId = "book-realization",
                    type = FinancialEventType.CASH_EXPENSE,
                    amount = 50_000,
                    description = "Sudah dibatalkan",
                    status = "REVERSED",
                    reversedByEventId = "event-reversal",
                ),
            ),
        )

        val preview = FinanceWorkbookImporter().preview("arina-agri.xlsx", bytes)

        assertTrue(preview.budgetCandidates.none { it.name == "Rencana lama" })
        assertTrue(preview.candidates.none { it.description == "Sudah dibatalkan" })
        assertTrue(preview.warnings.any { "pembalikan" in it })
    }

    // --- fixtures ---------------------------------------------------------------------------

    private fun bundle(
        extraProjectionBudget: BudgetItemEntity? = null,
        extraRealizationEvent: FinancialEventEntity? = null,
    ) = FinanceExportBundle(
        business = BusinessEntity(id = "business-1", name = "Tani Padi Pak Ade", createdAtEpochMillis = 0),
        project = ProjectEntity(id = "project-1", businessId = "business-1", name = "Sawah Blok A", isGeneral = false, createdAtEpochMillis = 0),
        categories = listOf(
            CategoryEntity(id = "cat-pupuk", businessId = "business-1", role = CategoryRole.EXPENSE.name, name = "Pupuk", normalizedName = "pupuk", createdAtEpochMillis = 0),
            CategoryEntity(id = "cat-panen", businessId = "business-1", role = CategoryRole.REVENUE.name, name = "Hasil Panen", normalizedName = "hasil panen", createdAtEpochMillis = 0),
        ),
        books = listOf(
            FinanceExportBook(
                book = BookEntity(id = "book-projection", projectId = "project-1", mode = BookMode.PROJECTION.name, createdAtEpochMillis = 0),
                budgets = listOfNotNull(
                    budgetItem("budget-1", "book-projection", "Pupuk NPK", 1_200_000),
                    extraProjectionBudget,
                ),
                events = listOf(
                    event("event-plan", "book-projection", FinancialEventType.CASH_EXPENSE, 1_200_000, "Rencana beli pupuk"),
                ),
                obligations = emptyList(),
                summary = EMPTY_SUMMARY,
                assumptions = null,
            ),
            FinanceExportBook(
                book = BookEntity(id = "book-realization", projectId = "project-1", mode = BookMode.REALIZATION.name, createdAtEpochMillis = 0),
                budgets = listOf(budgetItem("budget-2", "book-realization", "Pupuk NPK terpakai", 1_150_000)),
                events = listOfNotNull(
                    event("event-buy", "book-realization", FinancialEventType.CASH_EXPENSE, 1_150_000, "Beli pupuk NPK"),
                    event("event-sell", "book-realization", FinancialEventType.CASH_INCOME, 4_000_000, "Jual gabah", category = "Hasil Panen"),
                    extraRealizationEvent,
                ),
                obligations = emptyList(),
                summary = EMPTY_SUMMARY,
                assumptions = null,
            ),
        ),
        importBatches = emptyList(),
        notices = emptyList(),
        provenance = emptyList(),
    )

    private fun budgetItem(
        id: String,
        bookId: String,
        name: String,
        total: Long,
        archived: Boolean = false,
    ) = BudgetItemEntity(
        id = id,
        bookId = bookId,
        categoryId = "cat-pupuk",
        name = name,
        normalizedName = name.lowercase(),
        quantityText = "10",
        unit = "kg",
        unitPriceRupiah = total / 10,
        plannedTotalRupiah = total,
        plannedDateEpochDay = LocalDate.of(2026, 8, 1).toEpochDay(),
        sortOrder = 1,
        archived = archived,
        createdAtEpochMillis = 0,
    )

    private fun event(
        id: String,
        bookId: String,
        type: FinancialEventType,
        amount: Long,
        description: String,
        category: String = "Pupuk",
        status: String = "POSTED",
        reversedByEventId: String? = null,
    ) = FinancialEventEntity(
        id = id,
        businessId = "business-1",
        projectId = "project-1",
        bookId = bookId,
        type = type.name,
        amountRupiah = amount,
        dateEpochDay = LocalDate.of(2026, 8, 15).toEpochDay(),
        description = description,
        category = category,
        cashFlowRole = "OPERATING",
        status = status,
        reversedByEventId = reversedByEventId,
        createdAtEpochMillis = 0,
    )

    private companion object {
        val EMPTY_SUMMARY = FinancialSummary(0, 0, 0, 0, 0, 0)
    }
}
