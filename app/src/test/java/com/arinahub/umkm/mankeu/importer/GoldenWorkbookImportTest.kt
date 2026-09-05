package com.arinahub.umkm.mankeu.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

class GoldenWorkbookImportTest {
    @Test
    fun `verified facilitator workbook reconciles land rent transparently`() {
        val workbook = fixtureRoot().resolve("sources/verified/WB-BASE-001/workbook.xlsx")
        assumeTrue("Sibling fixture repository is required for this integration test", workbook.isFile)

        val preview = FinanceWorkbookImporter().preview(workbook.name, workbook.readBytes())

        assertTrue(preview.errors.joinToString(), preview.canCommit)
        assertEquals(5, preview.recognizedSheets.size)
        assertEquals(45_500_000L, preview.totalIncomeRupiah)
        assertEquals(
            preview.candidates.filter { it.classification == ImportClassification.ENRICHED_DETERMINISTIC }
                .joinToString { "${it.description}:${it.amountRupiah}" },
            22_159_000L,
            preview.totalExpenseRupiah,
        )
        assertEquals(1, preview.enrichmentCount)
        val landRent = preview.candidates.single { it.classification == ImportClassification.ENRICHED_DETERMINISTIC }
        assertTrue(landRent.description.contains("sewa", ignoreCase = true))
        assertEquals(7_000_000L, landRent.amountRupiah)
        assertEquals(listOf("exact-day"), landRent.inferredFields)
        assertTrue(landRent.sourceEvidence.size >= 3)
    }

    @Test
    fun `synthetic enrichment fixture uses the same five-view contract`() {
        val workbook = fixtureRoot().resolve("fixtures/enrichment/WB-ENR-001/workbook.xlsx")
        assumeTrue("Sibling fixture repository is required for this integration test", workbook.isFile)

        val preview = FinanceWorkbookImporter().preview(workbook.name, workbook.readBytes())

        assertTrue(preview.errors.joinToString(), preview.canCommit)
        assertEquals(45_500_000L, preview.totalIncomeRupiah)
        assertEquals(22_159_000L, preview.totalExpenseRupiah)
        assertEquals(1, preview.enrichmentCount)
    }

    private fun fixtureRoot(): File {
        val appDirectory = File(requireNotNull(System.getProperty("user.dir")))
        val arinaDirectory = appDirectory.parentFile?.parentFile ?: appDirectory
        return arinaDirectory.resolve("arina-finance-workbook-fixtures")
    }
}
