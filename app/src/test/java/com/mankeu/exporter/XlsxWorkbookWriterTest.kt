package com.mankeu.exporter

import com.mankeu.importer.XlsxWorkbookReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class XlsxWorkbookWriterTest {
    @Test
    fun `writer produces readable multi-sheet xlsx with exact integer rupiah`() {
        val bytes = XlsxWorkbookWriter().write(
            listOf(
                WorkbookTable("Metadata", listOf("Kunci", "Nilai"), listOf(listOf("Format", "MANKEU"))),
                WorkbookTable(
                    "Transaksi Realisasi",
                    listOf("Tanggal", "Keterangan", "Nominal"),
                    listOf(listOf("2026-08-15", "Jasa traktor & panen", 7_000_000L)),
                    currencyColumns = setOf(2),
                ),
            ),
        )

        assertTrue(bytes.copyOfRange(0, 2).contentEquals(byteArrayOf('P'.code.toByte(), 'K'.code.toByte())))
        val snapshot = XlsxWorkbookReader().read(bytes.inputStream())
        assertEquals(listOf("Metadata", "Transaksi Realisasi"), snapshot.sheets.map { it.name })
        val row = snapshot.sheets[1].rows[1]
        assertEquals("2026-08-15", row.cellAt(0)?.text)
        assertEquals("Jasa traktor & panen", row.cellAt(1)?.text)
        assertEquals(7_000_000.0, row.cellAt(2)?.number)
    }

    @Test
    fun `duplicate and invalid sheet names are normalized uniquely`() {
        val bytes = XlsxWorkbookWriter().write(
            listOf(
                WorkbookTable("A/B", listOf("x"), emptyList()),
                WorkbookTable("A:B", listOf("x"), emptyList()),
            ),
        )

        val names = XlsxWorkbookReader().read(bytes.inputStream()).sheets.map { it.name }
        assertEquals(2, names.distinct().size)
        assertTrue(names.all { it.length <= 31 && '/' !in it && ':' !in it })
    }
}
