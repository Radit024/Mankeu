package com.mankeu.importer

import com.mankeu.data.BookEntity
import com.mankeu.data.BudgetItemEntity
import com.mankeu.data.BusinessEntity
import com.mankeu.data.CategoryEntity
import com.mankeu.data.FinancialEventEntity
import com.mankeu.data.ProjectEntity
import com.mankeu.data.ScenarioAssumptionEntity
import com.mankeu.domain.BookMode
import com.mankeu.domain.CategoryRole
import com.mankeu.domain.FinancialEventType
import com.mankeu.domain.FinancialSummary
import com.mankeu.domain.StockItemEntity
import com.mankeu.exporter.FinanceExportBook
import com.mankeu.exporter.FinanceExportBundle
import com.mankeu.exporter.FinanceWorkbookExporter
import com.mankeu.exporter.WorkbookTable
import com.mankeu.exporter.XlsxWorkbookWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/**
 * The single Mankeu template (DEC-012): the exporter writes it, the importer reads exactly it, and
 * anything else is rejected with the precise problem. These drive the real exporter, so a drift in
 * sheet names or headers on either side fails here.
 */
class TemplateImportTest {
    private val importer = FinanceWorkbookImporter()

    @Test
    fun `an export imports back without loss`() {
        val preview = importer.preview("ekspor.xlsx", FinanceWorkbookExporter().export(bundle()))

        assertTrue(preview.errors.joinToString(), preview.canCommit)
        assertEquals(
            listOf(
                ImportTransaction("Transaksi!A2", DAY, "Setoran modal awal", "Setoran Modal Pemilik", incoming = true, amountRupiah = 5_000_000),
                ImportTransaction("Transaksi!A3", DAY, "Beli kopi", "Bahan Baku & Stok", incoming = false, amountRupiah = 1_150_000),
                ImportTransaction("Transaksi!A4", DAY, "Jual kopi", "Penjualan & Jasa", incoming = true, amountRupiah = 4_000_000),
            ),
            preview.transactions,
        )
        assertEquals(
            listOf(ImportBudgetLine("Rencana Anggaran!A2", YearMonth.of(2026, 8), "Bahan Baku & Stok", "Biji kopi", "10", "kg", 120_000, 1_200_000)),
            preview.budgetLines,
        )
        assertEquals(ImportBepParameters("cup", 10_000, 5_000, 2_000_000, "700"), preview.bepParameters)
        assertEquals(listOf(ImportStockItem("Stok Barang!A2", "Gula", "kg", 15_000, "2.5", "1")), preview.stockItems)
    }

    @Test
    fun `reversed rows and archived plan lines are not exported`() {
        val preview = importer.preview("ekspor.xlsx", FinanceWorkbookExporter().export(bundle()))

        assertTrue(preview.transactions.none { it.description == "Salah catat" })
        assertTrue(preview.budgetLines.none { it.name == "Rencana lama" })
    }

    @Test
    fun `the blank template imports as nothing, without errors`() {
        val preview = importer.preview("template.xlsx", FinanceWorkbookExporter().template(listOf("Penjualan & Jasa" to CategoryRole.REVENUE)))

        assertEquals(emptyList<String>(), preview.errors)
        assertFalse(preview.canCommit)
    }

    @Test
    fun `a workbook that is not the template is rejected with the reason`() {
        val bytes = XlsxWorkbookWriter().write(listOf(WorkbookTable("Buku Kas", listOf("Tanggal", "Jumlah"), listOf(listOf("2026-08-01", 5_000L)))))

        val preview = importer.preview("lain.xlsx", bytes)

        assertFalse(preview.canCommit)
        assertTrue(preview.errors.first().startsWith("File ini bukan template Mankeu"))
        assertTrue(preview.errors.any { it == "Lembar \"Transaksi\" tidak ada." })
    }

    @Test
    fun `a renamed column is named exactly`() {
        val bytes = template(transactions = WorkbookTable("Transaksi", listOf("Tanggal", "Uraian", "Kategori", "Pemasukan (Rp)", "Pengeluaran (Rp)"), emptyList()))

        assertEquals(listOf("Lembar \"Transaksi\" kolom B1 harus \"Keterangan\", tertulis \"Uraian\"."), importer.preview("x.xlsx", bytes).errors)
    }

    @Test
    fun `invalid cells are reported by sheet and cell`() {
        val bytes = template(
            transactions = WorkbookTable(
                "Transaksi",
                MankeuTemplate.TRANSACTION_HEADERS,
                listOf(
                    listOf("2026-08-01", "Dua kolom", "Penjualan", 10_000L, 2_000L),
                    listOf("1/8/2026", "Tanggal salah", "Penjualan", 10_000L, null),
                    listOf("2026-08-02", "Rp teks", "Penjualan", "Rp 25.000", null),
                ),
            ),
        )

        val preview = importer.preview("x.xlsx", bytes)

        assertEquals(
            listOf(
                "Transaksi!D2: isi tepat satu kolom, Pemasukan atau Pengeluaran.",
                "Transaksi!A3: tanggal harus berformat YYYY-MM-DD, tertulis \"1/8/2026\".",
            ),
            preview.errors,
        )
        assertEquals(25_000L, preview.transactions.single().amountRupiah)
    }

    @Test
    fun `an excel date cell is read as a date`() {
        val serial = LocalDate.of(2026, 8, 15).toEpochDay() - LocalDate.of(1899, 12, 30).toEpochDay()
        val bytes = template(
            transactions = WorkbookTable("Transaksi", MankeuTemplate.TRANSACTION_HEADERS, listOf(listOf(serial, "Jual", "Penjualan", 5_000L, null))),
        )

        assertEquals(LocalDate.of(2026, 8, 15), importer.preview("x.xlsx", bytes).transactions.single().date)
    }

    // --- fixtures ---------------------------------------------------------------------------

    /** A minimal valid template whose Transaksi sheet is [transactions]. */
    private fun template(transactions: WorkbookTable): ByteArray = XlsxWorkbookWriter().write(
        listOf(
            WorkbookTable(MankeuTemplate.GUIDE, MankeuTemplate.GUIDE_HEADERS, listOf(listOf("Format", MankeuTemplate.MARKER))),
            transactions,
            WorkbookTable(MankeuTemplate.BUDGET, MankeuTemplate.BUDGET_HEADERS, emptyList()),
            WorkbookTable(MankeuTemplate.BEP, MankeuTemplate.BEP_HEADERS, emptyList()),
            WorkbookTable(MankeuTemplate.STOCK, MankeuTemplate.STOCK_HEADERS, emptyList()),
        ),
    )

    private fun bundle() = FinanceExportBundle(
        business = BusinessEntity(id = "b", name = "Kedai Kopi Berkah", createdAtEpochMillis = 0),
        project = ProjectEntity(id = "p", businessId = "b", name = "Gerai Utama", isGeneral = false, createdAtEpochMillis = 0),
        categories = listOf(
            category("c-bahan", "Bahan Baku & Stok", CategoryRole.EXPENSE),
            category("c-jual", "Penjualan & Jasa", CategoryRole.REVENUE),
            category("c-modal", "Setoran Modal Pemilik", CategoryRole.CAPITAL_IN),
        ),
        books = listOf(
            FinanceExportBook(
                book = BookEntity(id = "plan", projectId = "p", mode = BookMode.PROJECTION.name, createdAtEpochMillis = 0),
                budgets = listOf(budget("Biji kopi", 1_200_000), budget("Rencana lama", 999_000, archived = true)),
                events = emptyList(),
                summary = EMPTY,
                assumptions = ScenarioAssumptionEntity(
                    id = "a",
                    bookId = "plan",
                    outputQuantityText = "700",
                    outputUnit = "cup",
                    outputPriceRupiah = 10_000,
                    fixedCostRupiah = 2_000_000,
                    variableCostPerUnitRupiah = 5_000,
                    updatedAtEpochMillis = 0,
                ),
            ),
            FinanceExportBook(
                book = BookEntity(id = "actual", projectId = "p", mode = BookMode.REALIZATION.name, createdAtEpochMillis = 0),
                budgets = emptyList(),
                events = listOf(
                    event("e1", FinancialEventType.OWNER_CAPITAL, 5_000_000, "Setoran modal awal", "Setoran Modal Pemilik", createdAt = 1),
                    event("e2", FinancialEventType.CASH_EXPENSE, 1_150_000, "Beli kopi", "Bahan Baku & Stok", createdAt = 2),
                    event("e3", FinancialEventType.CASH_INCOME, 4_000_000, "Jual kopi", "Penjualan & Jasa", createdAt = 3),
                    event("e4", FinancialEventType.CASH_EXPENSE, 50_000, "Salah catat", "Bahan Baku & Stok", createdAt = 4, reversedBy = "e5"),
                    event("e5", FinancialEventType.REVERSAL, 50_000, "Pembalikan", "Bahan Baku & Stok", createdAt = 5),
                ),
                summary = EMPTY,
                assumptions = null,
            ),
        ),
        importBatches = emptyList(),
        notices = emptyList(),
        provenance = emptyList(),
        stockItems = listOf(
            StockItemEntity(id = "s", businessId = "b", name = "Gula", unit = "kg", basePriceRupiah = 15_000, currentStockText = "2.5", minAlertStockText = "1", createdAtEpochMillis = 0),
        ),
    )

    private fun category(id: String, name: String, role: CategoryRole) =
        CategoryEntity(id = id, businessId = "b", role = role.name, name = name, normalizedName = name.lowercase(), createdAtEpochMillis = 0)

    private fun budget(name: String, total: Long, archived: Boolean = false) = BudgetItemEntity(
        id = name,
        bookId = "plan",
        categoryId = "c-bahan",
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
        type: FinancialEventType,
        amount: Long,
        description: String,
        category: String,
        createdAt: Long,
        reversedBy: String? = null,
    ) = FinancialEventEntity(
        id = id,
        businessId = "b",
        projectId = "p",
        bookId = "actual",
        type = type.name,
        amountRupiah = amount,
        dateEpochDay = DAY.toEpochDay(),
        description = description,
        category = category,
        cashFlowRole = "OPERATING",
        status = if (reversedBy != null) "REVERSED" else "POSTED",
        reversedByEventId = reversedBy,
        createdAtEpochMillis = createdAt,
    )

    private companion object {
        val DAY: LocalDate = LocalDate.of(2026, 8, 15)
        val EMPTY = FinancialSummary(0, 0, 0, 0, 0, 0)
    }
}
