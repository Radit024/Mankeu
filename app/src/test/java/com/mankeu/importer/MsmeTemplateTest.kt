package com.mankeu.importer

import com.mankeu.domain.BookMode
import com.mankeu.domain.FinancialEventType
import com.mankeu.exporter.MsmeTemplateExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MsmeTemplateTest {

    @Test
    fun `msme template generates valid bytes and round-trips into importer preview`() {
        val exporter = MsmeTemplateExporter()
        val bytes = exporter.generateTemplate()

        assertTrue("Template byte array must not be empty", bytes.isNotEmpty())

        val importer = FinanceWorkbookImporter()
        val preview = importer.preview("template-pencatatan-umkm.xlsx", bytes)

        assertTrue(
            "Import preview errors: ${preview.errors.joinToString()}",
            preview.canCommit,
        )
        assertTrue(
            "Expected Buku Kas Harian in recognized sheets",
            preview.recognizedSheets.any { "kas harian" in it.lowercase() },
        )
        assertTrue(
            "Expected Rencana Usaha (Proyeksi) in recognized sheets",
            preview.recognizedSheets.any { "proyeksi" in it.lowercase() || "rencana" in it.lowercase() },
        )

        val realCandidates = preview.candidates.filter { it.bookMode == BookMode.REALIZATION }
        val projCandidates = preview.candidates.filter { it.bookMode == BookMode.PROJECTION }

        // Verify daily cash realization transactions
        assertEquals(7, realCandidates.size)
        val incomeCandidates = realCandidates.filter { it.type == FinancialEventType.CASH_INCOME }
        val expenseCandidates = realCandidates.filter { it.type == FinancialEventType.CASH_EXPENSE }

        assertEquals(3, incomeCandidates.size)
        assertEquals(4, expenseCandidates.size)

        val capital = incomeCandidates.find { it.description == "Setoran Modal Awal" }
        assertNotNull(capital)
        assertEquals(5_000_000L, capital!!.amountRupiah)
        assertEquals("Modal Sendiri", capital.category)

        val stock = expenseCandidates.find { it.description == "Belanja Stok Bahan Baku" }
        assertNotNull(stock)
        assertEquals(1_200_000L, stock!!.amountRupiah)
        assertEquals("Bahan Baku", stock.category)

        // Verify projection plan transactions
        assertEquals(7, projCandidates.size)
        val projIncome = projCandidates.filter { it.type == FinancialEventType.CASH_INCOME }
        val projExpense = projCandidates.filter { it.type == FinancialEventType.CASH_EXPENSE }
        assertEquals(2, projIncome.size)
        assertEquals(5, projExpense.size)

        val projFlour = projExpense.find { it.description == "Belanja Bahan Baku" }
        assertNotNull(projFlour)
        assertEquals(1_500_000L, projFlour!!.amountRupiah)
        assertEquals("Bahan Baku", projFlour.category)

        assertTrue("Template with both realization and projection carries book mode", preview.carriesBookMode)
    }
}
