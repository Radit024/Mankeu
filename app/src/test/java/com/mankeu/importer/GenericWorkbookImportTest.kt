package com.mankeu.importer

import com.mankeu.domain.FinancialEventType
import com.mankeu.exporter.WorkbookTable
import com.mankeu.exporter.XlsxWorkbookWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GenericWorkbookImportTest {
    @Test
    fun `canonical Mankeu export preserves active credit and settlement linkage`() {
        val headers = listOf(
            "ID", "Tanggal", "Jenis", "Keterangan", "Kategori", "Nominal", "Arus Kas", "Pihak",
            "Jatuh Tempo", "Referensi", "Jumlah", "Satuan", "Harga Satuan", "ID Anggaran",
            "ID Kewajiban", "Status", "Membalik ID", "Dibalik Oleh ID", "Import Batch",
        )
        val bytes = XlsxWorkbookWriter().write(
            listOf(
                WorkbookTable("Metadata", listOf("Kunci", "Nilai"), listOf(listOf("Format", "MANKEU_FINANCE_XLSX"))),
                WorkbookTable(
                    "Transaksi Realisasi",
                    headers,
                    listOf(
                        row(headers, "sale", "2026-08-01", "CREDIT_SALE", "Jasa panen", 500_000, "Pak Budi", "obligation-1", "POSTED"),
                        row(headers, "pay", "2026-08-05", "RECEIVABLE_COLLECTION", "Terima sebagian", 200_000, "Pak Budi", "obligation-1", "POSTED"),
                        row(headers, "old", "2026-08-06", "CASH_EXPENSE", "Catatan dibalik", 50_000, null, null, "REVERSED", reversedBy = "reversal"),
                        row(headers, "reversal", "2026-08-06", "REVERSAL", "Pembalikan", 50_000, null, null, "POSTED"),
                    ),
                ),
            ),
        )

        val preview = FinanceWorkbookImporter().preview("arina-export.xlsx", bytes)

        assertTrue(preview.errors.joinToString(), preview.canCommit)
        assertEquals(2, preview.candidates.size)
        assertEquals(listOf(FinancialEventType.CREDIT_SALE, FinancialEventType.RECEIVABLE_COLLECTION), preview.candidates.map { it.type })
        assertEquals(listOf("obligation-1", "obligation-1"), preview.candidates.map { it.sourceObligationId })
        assertTrue(preview.warnings.any { "pembalikan" in it })
    }

    @Test
    fun `single daily table imports without requiring the five-view baseline`() {
        val bytes = XlsxWorkbookWriter().write(
            listOf(
                WorkbookTable(
                    "Catatan Bengkel",
                    listOf("Tanggal", "Uraian", "Pengeluaran", "Pemasukan"),
                    listOf(
                        listOf("2026-08-10", "Servis traktor", null, 800_000),
                        listOf("2026-08-11", "Beli oli", 120_000, null),
                    ),
                ),
            ),
        )

        val preview = FinanceWorkbookImporter().preview("bengkel.xlsx", bytes)

        assertTrue(preview.errors.joinToString(), preview.canCommit)
        assertEquals(2, preview.candidates.size)
        assertEquals(800_000, preview.totalIncomeRupiah)
        assertEquals(120_000, preview.totalExpenseRupiah)
        assertTrue(preview.warnings.single().contains("tabel tunggal"))
    }

    private fun row(
        headers: List<String>,
        id: String,
        date: String,
        type: String,
        description: String,
        amount: Long,
        counterparty: String?,
        obligationId: String?,
        status: String,
        reversedBy: String? = null,
    ): List<Any?> = headers.map { header ->
        when (header) {
            "ID" -> id
            "Tanggal" -> date
            "Jenis" -> type
            "Keterangan" -> description
            "Kategori" -> "Jasa"
            "Nominal" -> amount
            "Arus Kas" -> "OPERATING"
            "Pihak" -> counterparty
            "Jatuh Tempo" -> "2026-08-31"
            "ID Kewajiban" -> obligationId
            "Status" -> status
            "Dibalik Oleh ID" -> reversedBy
            else -> null
        }
    }
}
