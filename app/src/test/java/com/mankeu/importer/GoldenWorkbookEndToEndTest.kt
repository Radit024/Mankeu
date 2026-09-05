package com.mankeu.importer

import com.mankeu.data.BookEntity
import com.mankeu.data.BudgetItemEntity
import com.mankeu.data.BusinessEntity
import com.mankeu.data.FinancialEventEntity
import com.mankeu.data.ProjectEntity
import com.mankeu.domain.BookMode
import com.mankeu.domain.CategoryRole
import com.mankeu.data.CategoryEntity
import com.mankeu.domain.FinancialEventType
import com.mankeu.domain.LedgerPolicy
import com.mankeu.domain.MoneyEvent
import com.mankeu.domain.PostedRecord
import com.mankeu.domain.PostingReportCalculator
import com.mankeu.exporter.FinanceExportBook
import com.mankeu.exporter.FinanceExportBundle
import com.mankeu.exporter.FinanceWorkbookExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.UUID

/**
 * The whole point of the product, as one test: can the owner's spreadsheet be replaced by this app?
 *
 * `GoldenWorkbookImportTest` stops at the import *preview* — it proves the five sheets are read and
 * that Sewa Lahan is recovered. This carries the same workbook the rest of the way:
 *
 *   real .xlsx -> import preview -> money events -> journal postings -> the reports the app shows
 *              -> export back to .xlsx -> import that export again
 *
 * The figures asserted below are the workbook's own, from `docs/REFERENCE_WORKBOOK_CONTRACT.md`:
 * income Rp45,500,000 and expense Rp22,159,000 (the latter only reconciles once the Rp7,000,000
 * land rent, absent from the daily sheet, is recovered from the other four sheets).
 *
 * The workbook is `references/CATATAN KEUANGAN PADI 1 Ha ADE.xlsx`, byte-identical to fixture
 * WB-BASE-001 (same SHA-256). It is restricted material and is deliberately NOT copied into this
 * repository — the test reads it from the sibling fixture repository and skips when absent.
 */
class GoldenWorkbookEndToEndTest {

    @Test
    fun `the owner's own workbook becomes the app's reports and survives a round trip`() {
        val workbook = fixtureRoot().resolve("sources/verified/WB-BASE-001/workbook.xlsx")
        assumeTrue("Sibling fixture repository is required for this integration test", workbook.isFile)

        // 1. Import the real workbook.
        val preview = FinanceWorkbookImporter().preview(workbook.name, workbook.readBytes())
        assertTrue(preview.errors.joinToString(), preview.canCommit)
        assertEquals(45_500_000L, preview.totalIncomeRupiah)
        assertEquals(22_159_000L, preview.totalExpenseRupiah)

        // 2. Turn the candidates into the events the repository would commit, then into postings.
        //    This is the same journalling path `insertEventWithJournal` uses, so the summary below
        //    is the one the Laporan screen renders.
        val events = preview.candidates.map { candidate ->
            MoneyEvent(
                id = UUID.randomUUID().toString(),
                businessId = BUSINESS_ID,
                projectId = PROJECT_ID,
                bookId = REALIZATION_BOOK_ID,
                type = candidate.type,
                amountRupiah = candidate.amountRupiah,
                date = candidate.date,
                description = candidate.description,
                category = candidate.category,
                counterparty = candidate.counterparty,
                dueDate = candidate.dueDate,
                reference = candidate.reference,
            )
        }
        val records = events.flatMap { event ->
            LedgerPolicy.journalFor(event).requireBalanced().postings.map { posting ->
                PostedRecord(event.id, event.cashFlowRole, posting.account, posting.side, posting.amountRupiah)
            }
        }

        val summary = PostingReportCalculator.calculate(records)

        // 3. The reports have to agree with the spreadsheet they replace.
        assertEquals(45_500_000L, summary.operatingIncomeRupiah)
        assertEquals(22_159_000L, summary.operatingExpenseRupiah)
        assertEquals(23_341_000L, summary.profitRupiah)
        assertEquals(
            "cash on hand must equal what the workbook's own income and spending leave behind",
            45_500_000L - 22_159_000L,
            summary.cashBalanceRupiah,
        )

        // 4. Export those records back out, and read the export in again. Nothing may be lost:
        //    a season brought in from Excel has to be able to leave as Excel.
        val exported = FinanceWorkbookExporter().export(bundleOf(events))
        val reimported = FinanceWorkbookImporter().preview("mankeu-export.xlsx", exported)

        assertTrue(reimported.errors.joinToString(), reimported.canCommit)
        assertEquals(
            "every event must survive export and re-import",
            events.size,
            reimported.candidates.size,
        )
        assertEquals(summary.operatingIncomeRupiah, reimported.totalIncomeRupiah)
        assertEquals(summary.operatingExpenseRupiah, reimported.totalExpenseRupiah)
        assertTrue(
            "the re-import must land in the realization book",
            reimported.candidates.all { it.bookMode == BookMode.REALIZATION },
        )
    }

    private fun bundleOf(events: List<MoneyEvent>) = FinanceExportBundle(
        business = BusinessEntity(BUSINESS_ID, "Tani Padi 1 Ha", createdAtEpochMillis = 0),
        project = ProjectEntity(PROJECT_ID, BUSINESS_ID, "Padi 1 Ha", isGeneral = false, createdAtEpochMillis = 0),
        categories = listOf(
            CategoryEntity(
                id = "category-import",
                businessId = BUSINESS_ID,
                role = CategoryRole.EXPENSE.name,
                name = "Impor",
                normalizedName = "impor",
                createdAtEpochMillis = 0,
            ),
        ),
        books = listOf(
            FinanceExportBook(
                book = BookEntity(REALIZATION_BOOK_ID, PROJECT_ID, BookMode.REALIZATION.name, createdAtEpochMillis = 0),
                budgets = emptyList<BudgetItemEntity>(),
                events = events.map(::toEntity),
                obligations = emptyList(),
                summary = PostingReportCalculator.calculate(emptyList()),
                assumptions = null,
            ),
        ),
        importBatches = emptyList(),
        notices = emptyList(),
        provenance = emptyList(),
    )

    private fun toEntity(event: MoneyEvent) = FinancialEventEntity(
        id = event.id,
        businessId = event.businessId,
        projectId = event.projectId,
        bookId = event.bookId,
        type = event.type.name,
        amountRupiah = event.amountRupiah,
        dateEpochDay = event.date.toEpochDay(),
        description = event.description,
        category = event.category,
        counterparty = event.counterparty,
        reference = event.reference,
        dueDateEpochDay = event.dueDate?.toEpochDay(),
        cashFlowRole = event.cashFlowRole.name,
        createdAtEpochMillis = 0,
    )

    private fun fixtureRoot(): File {
        val appDirectory = File(requireNotNull(System.getProperty("user.dir")))
        val arinaDirectory = appDirectory.parentFile?.parentFile ?: appDirectory
        return arinaDirectory.resolve("arina-finance-workbook-fixtures")
    }

    private companion object {
        const val BUSINESS_ID = "business-golden"
        const val PROJECT_ID = "project-golden"
        const val REALIZATION_BOOK_ID = "book-realization"
    }
}
