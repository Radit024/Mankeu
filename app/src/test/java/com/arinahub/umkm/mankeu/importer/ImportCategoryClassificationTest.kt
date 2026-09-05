package com.arinahub.umkm.mankeu.importer

import com.arinahub.umkm.mankeu.domain.CategoryRole
import com.arinahub.umkm.mankeu.domain.FinancialEventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * The imported rows must land in the same buckets the owner's own profit-and-loss sheet uses.
 *
 * Totals alone were already right before any classification existed — every row simply went into
 * one "Pengeluaran impor" bucket, so the app could say Rp22.159.000 was spent but not on what. The
 * workbook's sheet 3 groups the same money into Input Usaha Tani 2.449.000, Tenaga Kerja 3.360.000,
 * Jasa Alsintan 8.150.000 and Lain-lain 8.200.000, and those four subtotals are what this asserts,
 * rebuilt from the app's finer categories.
 *
 * That indirection is the point: the app's categories are deliberately more granular than the
 * workbook's four headings (a farmer wants seed apart from fertiliser), so the test proves the
 * finer split *rolls up* to the owner's numbers rather than replacing them.
 */
class ImportCategoryClassificationTest {

    /**
     * Every rupiah of the workbook's expenses, by the app's own category.
     *
     * These are read off the workbook line by line, not copied from the app's output — that is what
     * makes this a check rather than a snapshot. Sewa Lahan is the row the daily sheet never
     * contains; it is recovered from the other four sheets.
     */
    private val expectedByCategory = mapOf(
        "Benih dan Bibit" to 412_500L, // benih padi
        "Pupuk" to 951_500L, // NPK 322.000 + Urea 517.500 + dolomit 112.000
        "Obat dan Pestisida" to 805_000L, // herbisida 180.000 + 120.000, pestisida 250.000 + 255.000
        "Tenaga Kerja" to 3_360_000L, // 21 wage rows
        "Jasa Alsintan" to 8_150_000L, // olah tanah, transplanter, combine, pompa
        "Pengairan" to 1_000_000L, // iuran pengairan
        "Transport" to 200_000L, // transport hasil panen
        "Sewa Lahan" to 7_000_000L, // recovered from RAB + laba rugi + arus kas
        "Lain-lain" to 280_000L, // karung gabah
    )

    @Test
    fun `every expense row lands in the right category and the total still reconciles`() {
        val workbook = fixtureWorkbook()
        assumeTrue("Sibling fixture repository is required for this integration test", workbook.isFile)
        val preview = FinanceWorkbookImporter().preview(workbook.name, workbook.readBytes())

        val expenses = preview.candidates.filter { it.type == FinancialEventType.CASH_EXPENSE }
        val unsorted = expenses.filter { it.category.startsWith("Pengeluaran") }
        assertTrue(
            "Every expense row should be classified; unsorted: " +
                unsorted.joinToString { "${it.description} -> ${it.category}" },
            unsorted.isEmpty(),
        )

        val actual = expenses.groupBy { it.category }.mapValues { (_, rows) -> rows.sumOf { it.amountRupiah } }
        assertEquals(expectedByCategory, actual)
        assertEquals(22_159_000L, actual.values.sum())
    }

    /**
     * The workbook's profit-and-loss sheet groups the same money into four headings. Three of them
     * are exactly one or more app categories; the fourth is not, and that is a real difference
     * worth stating rather than papering over: the workbook counts sacks as an input alongside seed
     * and fertiliser, while the app has no "consumables" category and puts them in Lain-lain
     * together with the land rent and the irrigation fee. The money is identical either way — only
     * the heading differs — and the app's split is the more useful one for a farmer, who wants seed
     * separate from fertiliser. If the owner later wants the workbook's exact headings back, this
     * test says precisely which row has to move.
     */
    @Test
    fun `the app's categories roll up to the workbook's own subtotals`() {
        val workbook = fixtureWorkbook()
        assumeTrue("Sibling fixture repository is required for this integration test", workbook.isFile)
        val preview = FinanceWorkbookImporter().preview(workbook.name, workbook.readBytes())
        val byCategory = preview.candidates
            .filter { it.type == FinancialEventType.CASH_EXPENSE }
            .groupBy { it.category }
            .mapValues { (_, rows) -> rows.sumOf { it.amountRupiah } }
        fun sum(vararg names: String) = names.sumOf { byCategory[it] ?: 0L }

        // Sheet "3. Laporan laba rugi", column G.
        assertEquals(
            2_449_000L,
            sum("Benih dan Bibit", "Pupuk", "Obat dan Pestisida", "Lain-lain"), // Lain-lain = karung
        )
        assertEquals(3_360_000L, sum("Tenaga Kerja"))
        assertEquals(8_150_000L, sum("Jasa Alsintan"))
        assertEquals(8_200_000L, sum("Pengairan", "Transport", "Sewa Lahan"))
    }

    @Test
    fun `the harvest sale is income, not an expense`() {
        val workbook = fixtureWorkbook()
        assumeTrue("Sibling fixture repository is required for this integration test", workbook.isFile)
        val preview = FinanceWorkbookImporter().preview(workbook.name, workbook.readBytes())

        val income = preview.candidates.filter { it.type == FinancialEventType.CASH_INCOME }
        assertEquals(1, income.size)
        assertEquals("Hasil Panen", income.single().category)
        assertEquals(45_500_000L, income.single().amountRupiah)
    }

    /**
     * "Iuran pengairan" is a fee and "upah pengairan" is labour. They share a word, and a naive
     * keyword pass puts both in the same place — which is how Lain-lain would silently swallow
     * Rp720.000 of wages, or Tenaga Kerja a Rp1.000.000 fee.
     */
    @Test
    fun `a shared word does not collapse two different categories`() {
        assertEquals(
            "Pengairan",
            CategoryClassifier.classify("Pembayaran iuran pengairan", CategoryRole.EXPENSE),
        )
        assertEquals(
            "Tenaga Kerja",
            CategoryClassifier.classify("Pembayaran upah pengairan III", CategoryRole.EXPENSE),
        )
    }

    /**
     * A wage row names the work it paid for, so it carries the vocabulary of another category.
     * The first version of the table tested ingredients first and quietly moved Rp600.000 of wages
     * into seed and pesticide — totals still reconciled, only the breakdown lied.
     */
    @Test
    fun `wages stay wages even when the row names what the work was about`() {
        listOf(
            "Pembayaran upah penyiangan herbisida I",
            "Pembayaran upah persemaian sistem dapog",
            "Pembayaran upah pemupukan II",
            "Pembayaran upah pengapuran",
            "Pembayaran upah pengendalian OPT III",
        ).forEach { row ->
            assertEquals(row, "Tenaga Kerja", CategoryClassifier.classify(row, CategoryRole.EXPENSE))
        }
    }

    /** The fixture must actually be present, or assertions that depend on it are skipped. */
    @Test
    fun `the fixture path resolves`() {
        assumeTrue(
            "Expected the sibling fixture repository at ${fixtureWorkbook().absolutePath}",
            fixtureWorkbook().isFile,
        )
    }

    @Test
    fun `an unrecognisable row is left unsorted rather than guessed`() {
        assertEquals(
            null,
            CategoryClassifier.classify("xyzzy 12345", CategoryRole.EXPENSE),
        )
    }

    /** `user.dir` is the `app` module, so the sibling fixture repository is two levels up. */
    private fun fixtureWorkbook(): File {
        val appDirectory = File(requireNotNull(System.getProperty("user.dir")))
        val arinaDirectory = appDirectory.parentFile?.parentFile ?: appDirectory
        return arinaDirectory.resolve("arina-finance-workbook-fixtures/sources/verified/WB-BASE-001/workbook.xlsx")
    }
}
