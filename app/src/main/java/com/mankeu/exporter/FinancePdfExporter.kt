package com.mankeu.exporter

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.mankeu.data.FinancialEventEntity
import com.mankeu.data.ObligationBalanceRow
import com.mankeu.domain.BookMode
import com.mankeu.domain.FinancialEventType
import java.io.ByteArrayOutputStream
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Offline, deterministic PDF intended for sharing and human review.
 * The workbook remains the machine-readable exchange format; this document is a report view.
 */
class FinancePdfExporter {
    fun export(bundle: FinanceExportBundle): ByteArray {
        val document = PdfDocument()
        val report = PdfReport(document)
        try {
            report.title("Mankeu", "Laporan keuangan UMKM")
            report.keyValue("Usaha", bundle.business.name)
            report.keyValue("Proyek / pekerjaan", bundle.project.name)
            report.keyValue("Mata uang", bundle.business.currency)
            report.keyValue("Dibuat", formatInstant(bundle.generatedAtEpochMillis))
            report.paragraph(
                "Laporan ini dibuat sepenuhnya dari data lokal dan posting debit/kredit aplikasi. " +
                    "Tidak ada analisis AI atau pengiriman data ke layanan jaringan.",
            )

            bundle.books.sortedBy { it.book.mode }.forEach { book ->
                val mode = BookMode.valueOf(book.book.mode)
                report.section(if (mode == BookMode.PROJECTION) "Proyeksi" else "Realisasi", followingHeight = 190f)
                report.subsection("Ringkasan")
                report.keyValue("Kas", money(book.summary.cashBalanceRupiah))
                report.keyValue("Pendapatan", money(book.summary.operatingIncomeRupiah))
                report.keyValue("Beban", money(book.summary.operatingExpenseRupiah))
                report.keyValue("Laba / rugi", money(book.summary.profitRupiah))
                report.keyValue("Arus kas operasi", money(book.summary.operatingCashFlowRupiah))
                report.keyValue("Arus kas investasi", money(book.summary.investingCashFlowRupiah))
                report.keyValue("Arus kas pendanaan", money(book.summary.financingCashFlowRupiah))
                report.keyValue("Piutang tersisa", money(book.summary.receivableOutstandingRupiah))
                report.keyValue("Utang tersisa", money(book.summary.payableOutstandingRupiah))

                report.subsection("Anggaran (${book.budgets.count { !it.archived }} item aktif)")
                val categoryNames = bundle.categories.associate { it.id to it.name }
                book.budgets.filterNot { it.archived }.forEach { item ->
                    val quantity = listOfNotNull(item.quantityText, item.unit).joinToString(" ").ifBlank { null }
                    val details = listOfNotNull(categoryNames[item.categoryId], quantity).joinToString(" - ")
                    report.listItem(item.name, details, money(item.plannedTotalRupiah))
                }
                if (book.budgets.none { !it.archived }) report.muted("Belum ada item anggaran aktif.")

                report.subsection("Transaksi (${book.events.size})")
                book.events.sortedWith(compareBy<FinancialEventEntity> { it.dateEpochDay }.thenBy { it.createdAtEpochMillis }).forEach { event ->
                    val details = buildList {
                        add(LocalDate.ofEpochDay(event.dateEpochDay).format(DATE))
                        add(eventTypeLabel(event.type))
                        event.counterparty?.takeIf(String::isNotBlank)?.let(::add)
                        if (event.reversedByEventId != null) add("DIBALIK")
                        if (event.reversesEventId != null) add("PEMBALIKAN")
                    }.joinToString(" - ")
                    report.listItem(event.description, details, money(event.amountRupiah))
                }
                if (book.events.isEmpty()) report.muted("Belum ada transaksi.")

                report.subsection("Piutang dan utang")
                book.obligations.filterNot { it.reversed }.forEach { obligation ->
                    report.obligation(obligation)
                }
                if (book.obligations.none { !it.reversed }) report.muted("Belum ada piutang atau utang.")

                book.assumptions?.let { assumptions ->
                    report.subsection("Asumsi")
                    report.keyValue("Saldo awal", money(assumptions.openingCashRupiah))
                    report.keyValue("Modal sendiri", money(assumptions.ownerCapitalRupiah))
                    report.keyValue("Rencana pinjaman", money(assumptions.loanAmountRupiah))
                    report.keyValue("Bunga", "${assumptions.interestRateBasisPoints / 100.0}%")
                    report.keyValue("Biaya pembiayaan lain", money(assumptions.otherFinancingCostRupiah))
                    report.keyValue("Jumlah output", assumptions.outputQuantityText?.let { "$it ${assumptions.outputUnit.orEmpty()}" } ?: "Belum diisi")
                    report.keyValue("Harga jual per unit", assumptions.outputPriceRupiah?.let(::money) ?: "Belum diisi")
                }
            }

            report.section("Jejak data")
            report.keyValue("Batch impor", bundle.importBatches.size.toString())
            report.keyValue("Notifikasi impor", bundle.notices.size.toString())
            report.keyValue("Rekaman provenance", bundle.provenance.size.toString())
            report.paragraph(
                "Untuk audit rinci, pemindahan data, dan impor ulang tanpa kehilangan makna, gunakan ekspor Excel. " +
                    "Untuk pemulihan seluruh aplikasi, gunakan cadangan .aabk terenkripsi.",
            )
            report.finish()
            return ByteArrayOutputStream().use { output ->
                document.writeTo(output)
                output.toByteArray()
            }
        } finally {
            document.close()
        }
    }

    private class PdfReport(private val document: PdfDocument) {
        private val body = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = CHARCOAL
            textSize = 10f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        private val bold = Paint(body).apply { typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) }
        private val mutedPaint = Paint(body).apply { color = MUTED }
        private var page: PdfDocument.Page? = null
        private var pageNumber = 0
        private var y = TOP

        fun title(product: String, title: String) {
            ensurePage(120f)
            val canvas = requireNotNull(page).canvas
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 112f, Paint().apply { color = BLUE })
            canvas.drawText(product, LEFT, 49f, Paint(bold).apply { color = Color.WHITE; textSize = 20f })
            canvas.drawText(title, LEFT, 78f, Paint(body).apply { color = PALE; textSize = 13f })
            y = 140f
        }

        fun section(text: String, followingHeight: Float = 0f) {
            ensurePage(50f + followingHeight)
            y += 10f
            requireNotNull(page).canvas.drawText(text, LEFT, y, Paint(bold).apply { color = BLUE; textSize = 17f })
            y += 18f
            requireNotNull(page).canvas.drawRect(LEFT, y, PAGE_WIDTH - RIGHT, y + 1.5f, Paint().apply { color = PALE_BLUE })
            y += 17f
        }

        fun subsection(text: String) {
            ensurePage(34f)
            y += 5f
            requireNotNull(page).canvas.drawText(text, LEFT, y, Paint(bold).apply { textSize = 12f })
            y += 17f
        }

        fun keyValue(label: String, value: String) {
            ensurePage(20f)
            val canvas = requireNotNull(page).canvas
            canvas.drawText(label, LEFT, y, body)
            val valuePaint = Paint(bold).apply { textAlign = Paint.Align.RIGHT }
            canvas.drawText(value, PAGE_WIDTH - RIGHT, y, valuePaint)
            y += 17f
        }

        fun listItem(primary: String, details: String?, amount: String) {
            val primaryLines = wrap(primary, bold, CONTENT_WIDTH - 110f)
            val detailsLines = details?.let { wrap(it, mutedPaint, CONTENT_WIDTH - 110f) }.orEmpty()
            val height = 11f + primaryLines.size * 13f + detailsLines.size * 12f
            ensurePage(height)
            val canvas = requireNotNull(page).canvas
            primaryLines.forEach { line -> canvas.drawText(line, LEFT, y, bold); y += 13f }
            detailsLines.forEach { line -> canvas.drawText(line, LEFT, y, mutedPaint); y += 12f }
            val amountPaint = Paint(bold).apply { textAlign = Paint.Align.RIGHT }
            canvas.drawText(amount, PAGE_WIDTH - RIGHT, y - detailsLines.size * 12f - 2f, amountPaint)
            canvas.drawRect(LEFT, y + 2f, PAGE_WIDTH - RIGHT, y + 2.6f, Paint().apply { color = DIVIDER })
            y += 11f
        }

        fun obligation(value: ObligationBalanceRow) {
            val remaining = (value.originalAmountRupiah - value.settledAmountRupiah).coerceAtLeast(0)
            val due = value.dueDateEpochDay?.let { "jatuh tempo ${LocalDate.ofEpochDay(it).format(DATE)}" } ?: "tanpa jatuh tempo"
            listItem(
                "${if (value.kind == "RECEIVABLE") "Piutang" else "Utang"} - ${value.counterparty}",
                "${value.description} - $due - dibayar ${money(value.settledAmountRupiah)}",
                money(remaining),
            )
        }

        fun paragraph(text: String) {
            val lines = wrap(text, body, CONTENT_WIDTH)
            ensurePage(lines.size * 14f + 13f)
            y += 5f
            lines.forEach { line -> requireNotNull(page).canvas.drawText(line, LEFT, y, body); y += 14f }
            y += 8f
        }

        fun muted(text: String) {
            ensurePage(20f)
            requireNotNull(page).canvas.drawText(text, LEFT, y, mutedPaint)
            y += 18f
        }

        fun finish() {
            finishPage()
        }

        private fun ensurePage(requiredHeight: Float) {
            if (page == null || y + requiredHeight > PAGE_HEIGHT - BOTTOM) {
                finishPage()
                pageNumber += 1
                page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                y = TOP
                val canvas = requireNotNull(page).canvas
                canvas.drawText("Mankeu", LEFT, 26f, Paint(bold).apply { color = BLUE; textSize = 9f })
            }
        }

        private fun finishPage() {
            val current = page ?: return
            val footer = Paint(mutedPaint).apply { textAlign = Paint.Align.RIGHT; textSize = 8f }
            current.canvas.drawText("Halaman $pageNumber | laporan lokal", PAGE_WIDTH - RIGHT, PAGE_HEIGHT - 20f, footer)
            document.finishPage(current)
            page = null
        }

        private fun wrap(text: String, paint: Paint, width: Float): List<String> {
            if (text.isBlank()) return listOf("")
            val lines = mutableListOf<String>()
            var current = ""
            text.split(Regex("\\s+")).forEach { word ->
                val candidate = if (current.isEmpty()) word else "$current $word"
                if (paint.measureText(candidate) <= width || current.isEmpty()) current = candidate
                else {
                    lines += current
                    current = word
                }
            }
            if (current.isNotEmpty()) lines += current
            return lines
        }
    }

    companion object {
        private const val PAGE_WIDTH = 595
        private const val PAGE_HEIGHT = 842
        private const val LEFT = 38f
        private const val RIGHT = 38f
        private const val TOP = 42f
        private const val BOTTOM = 42f
        private const val CONTENT_WIDTH = PAGE_WIDTH - LEFT - RIGHT
        private const val BLUE = 0xFF0061A4.toInt()
        private const val PALE_BLUE = 0xFFD1E4FF.toInt()
        private const val PALE = 0xFFF8F9FF.toInt()
        private const val CHARCOAL = 0xFF2C2A29.toInt()
        private const val MUTED = 0xFF686462.toInt()
        private const val DIVIDER = 0xFFE6E2DC.toInt()
        private val INDONESIAN = Locale.forLanguageTag("id-ID")
        private val DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM uuuu", INDONESIAN)

        private fun money(value: Long): String = "Rp${NumberFormat.getIntegerInstance(INDONESIAN).format(value)}"

        private fun formatInstant(epochMillis: Long): String = Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("dd MMMM uuuu HH:mm", INDONESIAN))

        private fun eventTypeLabel(raw: String): String = runCatching { FinancialEventType.valueOf(raw) }.getOrNull()?.let { type ->
            when (type) {
                FinancialEventType.CASH_INCOME -> "Pendapatan tunai"
                FinancialEventType.CASH_EXPENSE -> "Pengeluaran tunai"
                FinancialEventType.CREDIT_SALE -> "Penjualan kredit"
                FinancialEventType.CREDIT_PURCHASE_EXPENSE -> "Pembelian kredit - biaya"
                FinancialEventType.CREDIT_PURCHASE_ASSET -> "Pembelian kredit - aset"
                FinancialEventType.RECEIVABLE_COLLECTION -> "Penerimaan piutang"
                FinancialEventType.PAYABLE_SETTLEMENT -> "Pembayaran utang"
                FinancialEventType.OWNER_CAPITAL -> "Modal pemilik"
                FinancialEventType.OWNER_DRAWING -> "Pengambilan pemilik"
                FinancialEventType.LOAN_RECEIVED -> "Pinjaman diterima"
                FinancialEventType.LOAN_PRINCIPAL_REPAID -> "Pembayaran pokok pinjaman"
                FinancialEventType.LOAN_INTEREST_PAID -> "Pembayaran bunga pinjaman"
                FinancialEventType.ASSET_PURCHASE_CASH -> "Pembelian aset tunai"
                FinancialEventType.REVERSAL -> "Pembalikan"
            }
        } ?: raw.replace('_', ' ').lowercase().replaceFirstChar(Char::uppercase)
    }
}
