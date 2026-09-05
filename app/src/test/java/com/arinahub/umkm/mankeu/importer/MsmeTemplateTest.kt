package com.arinahub.umkm.mankeu.importer

import com.arinahub.umkm.mankeu.domain.FinancialEventType
import com.arinahub.umkm.mankeu.exporter.MsmeTemplateExporter
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
            "Expected RAB & Anggaran in recognized sheets",
            preview.recognizedSheets.any { "anggaran" in it.lowercase() || "rab" in it.lowercase() },
        )

        // Verify daily cash transactions
        assertEquals(7, preview.candidates.size)
        val incomeCandidates = preview.candidates.filter { it.type == FinancialEventType.CASH_INCOME }
        val expenseCandidates = preview.candidates.filter { it.type == FinancialEventType.CASH_EXPENSE }

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

        // Verify budget lines
        assertEquals(5, preview.budgetCandidates.size)
        val flour = preview.budgetCandidates.find { it.name == "Tepung dan Bumbu" }
        assertNotNull(flour)
        assertEquals(1_500_000L, flour!!.plannedTotalRupiah)
        assertEquals("Bahan Baku", flour.categoryName)
        assertEquals("100", flour.quantityText)
        assertEquals("kg", flour.unit)
        assertEquals(15_000L, flour.unitPriceRupiah)
    }
}
