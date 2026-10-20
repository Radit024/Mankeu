package com.mankeu.importer

import com.mankeu.domain.TransactionCalculation
import com.mankeu.domain.sha256Hex
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeParseException

/**
 * Reads Mankeu's own template ([MankeuTemplate]) and nothing else (DEC-012).
 *
 * A file without the marker, or with a missing/renamed sheet or column, is rejected with the exact
 * list of problems. Cells are read literally: no guessing of columns, years, months or categories.
 * Every error names its sheet and cell so the owner can fix the file.
 */
class FinanceWorkbookImporter(
    private val reader: XlsxWorkbookReader = XlsxWorkbookReader(),
) {
    fun preview(sourceName: String, bytes: ByteArray): WorkbookImportPreview {
        val hash = bytes.sha256Hex()
        val workbook = runCatching { reader.read(bytes.inputStream()) }.getOrElse {
            return WorkbookImportPreview(sourceName, hash, errors = listOf("Berkas ini bukan file Excel (.xlsx) yang bisa dibaca."))
        }
        val sheets = workbook.sheets.associateBy { it.name.trim() }

        val structure = buildList {
            val guide = sheets[MankeuTemplate.GUIDE]
            val marked = guide?.rows?.any { row -> row.cells.any { it.display.trim() == MankeuTemplate.MARKER } } == true
            if (!marked) add("File ini bukan template Mankeu (penanda ${MankeuTemplate.MARKER} di lembar \"${MankeuTemplate.GUIDE}\" tidak ditemukan).")
            MankeuTemplate.REQUIRED.forEach { (name, headers) ->
                val sheet = sheets[name]
                if (sheet == null) {
                    add("Lembar \"$name\" tidak ada.")
                } else {
                    val actual = sheet.rows.firstOrNull { it.index == 1 }
                    headers.forEachIndexed { column, expected ->
                        val found = actual?.cellAt(column)?.display?.trim().orEmpty()
                        if (found != expected) add("Lembar \"$name\" kolom ${columnLetter(column)}1 harus \"$expected\", tertulis \"$found\".")
                    }
                }
            }
        }
        if (structure.isNotEmpty()) return WorkbookImportPreview(sourceName, hash, errors = structure)

        val errors = mutableListOf<String>()
        return WorkbookImportPreview(
            sourceName = sourceName,
            sourceSha256 = hash,
            transactions = dataRows(sheets.getValue(MankeuTemplate.TRANSACTIONS)).mapNotNull { readTransaction(it, errors) },
            budgetLines = dataRows(sheets.getValue(MankeuTemplate.BUDGET)).mapNotNull { readBudget(it, errors) },
            bepParameters = readBep(sheets.getValue(MankeuTemplate.BEP), errors),
            stockItems = dataRows(sheets.getValue(MankeuTemplate.STOCK)).mapNotNull { readStock(it, errors) },
            errors = errors,
        )
    }

    private fun readTransaction(row: Row, errors: MutableList<String>): ImportTransaction? {
        val date = row.date(0, errors) ?: return null
        val description = row.requiredText(1, "Keterangan", errors) ?: return null
        val category = row.requiredText(2, "Kategori", errors) ?: return null
        val income = row.money(3, errors)
        val expense = row.money(4, errors)
        if ((income == null) == (expense == null)) {
            errors += "${row.ref(3)}: isi tepat satu kolom, Pemasukan atau Pengeluaran."
            return null
        }
        return ImportTransaction(row.ref(0), date, description, category, incoming = income != null, amountRupiah = income ?: expense!!)
    }

    private fun readBudget(row: Row, errors: MutableList<String>): ImportBudgetLine? {
        val monthText = row.text(0)
        val month = monthText?.let {
            runCatching { YearMonth.parse(it) }.getOrElse { errors += "${row.ref(0)}: bulan harus berformat YYYY-MM, tertulis \"$monthText\"."; return null }
        }
        val category = row.requiredText(1, "Kategori", errors) ?: return null
        val name = row.requiredText(2, "Item", errors) ?: return null
        val quantity = row.text(3)
        if (quantity != null && TransactionCalculation.parseQuantity(quantity) == null) {
            errors += "${row.ref(3)}: jumlah harus angka lebih dari nol, tertulis \"$quantity\"."
            return null
        }
        val price = row.money(5, errors)
        val total = row.money(6, errors)
            ?: quantity?.let(TransactionCalculation::parseQuantity)?.let { q -> price?.let { TransactionCalculation.computeTotalRupiah(q, it) } }
        if (total == null || total <= 0) {
            errors += "${row.ref(6)}: isi Total, atau Jumlah dan Harga Satuan."
            return null
        }
        return ImportBudgetLine(row.ref(0), month, category, name, quantity, row.text(4), price, total)
    }

    private fun readBep(sheet: WorkbookSheet, errors: MutableList<String>): ImportBepParameters? {
        val values = dataRows(sheet).associate { (it.text(0) ?: "") to it }
        values.keys.filter { it.isNotBlank() && it !in MankeuTemplate.BEP_KEYS }.forEach {
            errors += "Lembar \"${MankeuTemplate.BEP}\": parameter \"$it\" tidak dikenal."
        }
        val parameters = ImportBepParameters(
            unit = values[MankeuTemplate.BEP_UNIT]?.text(1),
            priceRupiah = values[MankeuTemplate.BEP_PRICE]?.money(1, errors),
            variableCostRupiah = values[MankeuTemplate.BEP_VARIABLE]?.money(1, errors),
            fixedCostRupiah = values[MankeuTemplate.BEP_FIXED]?.money(1, errors),
            targetQuantityText = values[MankeuTemplate.BEP_TARGET]?.let { row ->
                row.text(1)?.also { if (TransactionCalculation.parseQuantity(it) == null) errors += "${row.ref(1)}: target harus angka lebih dari nol." }
            },
        )
        return parameters.takeUnless { it.isEmpty }
    }

    private fun readStock(row: Row, errors: MutableList<String>): ImportStockItem? {
        val name = row.requiredText(0, "Nama", errors) ?: return null
        val unit = row.requiredText(1, "Satuan", errors) ?: return null
        val price = row.money(2, errors) ?: 0
        val quantity = row.quantity(3, errors) ?: return null
        val minimum = row.quantity(4, errors) ?: return null
        return ImportStockItem(row.ref(0), name, unit, price, quantity, minimum)
    }

    // ---- cell reading -----------------------------------------------------------------------

    private class Row(val sheet: String, val source: WorkbookRow) {
        fun ref(column: Int) = "$sheet!${columnLetter(column)}${source.index}"
        fun cell(column: Int) = source.cellAt(column)
        fun text(column: Int): String? = cell(column)?.let { it.text ?: it.number?.let(::plainNumber) }?.trim()?.ifBlank { null }
    }

    private fun dataRows(sheet: WorkbookSheet) = sheet.rows
        .filter { it.index > 1 && it.cells.any { cell -> cell.display.isNotBlank() } }
        .map { Row(sheet.name.trim(), it) }

    private fun Row.requiredText(column: Int, label: String, errors: MutableList<String>): String? =
        text(column) ?: null.also { errors += "${ref(column)}: $label wajib diisi." }

    /** Whole rupiah. A number cell, or text like "150000", "150.000" or "Rp 150.000". Blank is null. */
    private fun Row.money(column: Int, errors: MutableList<String>): Long? {
        val cell = cell(column) ?: return null
        cell.number?.let { number ->
            if (number < 0 || number != Math.floor(number)) {
                errors += "${ref(column)}: nominal harus bilangan bulat rupiah tanpa minus."
                return null
            }
            return number.toLong().takeIf { it > 0 }
        }
        val raw = cell.text?.trim()?.ifBlank { null } ?: return null
        val digits = raw.removePrefix("Rp").replace(".", "").replace(" ", "")
        return digits.toLongOrNull()?.takeIf { it >= 0 && digits.all(Char::isDigit) }?.takeIf { it > 0 }
            ?: null.also { if (digits != "0") errors += "${ref(column)}: \"$raw\" bukan nominal rupiah yang valid." }
    }

    /** A stock count; zero allowed, decimals with comma or dot. */
    private fun Row.quantity(column: Int, errors: MutableList<String>): String? {
        val raw = text(column) ?: return "0"
        val value = raw.replace(',', '.').toBigDecimalOrNull()
        if (value == null || value.signum() < 0) {
            errors += "${ref(column)}: \"$raw\" bukan jumlah yang valid."
            return null
        }
        return value.stripTrailingZeros().toPlainString()
    }

    /** ISO `YYYY-MM-DD` text, or an Excel date cell (serial day number). */
    private fun Row.date(column: Int, errors: MutableList<String>): LocalDate? {
        val cell = cell(column)
        cell?.number?.let { return EXCEL_EPOCH.plusDays(it.toLong()) }
        val raw = cell?.text?.trim()?.ifBlank { null } ?: return null.also { errors += "${ref(column)}: Tanggal wajib diisi." }
        return try {
            LocalDate.parse(raw)
        } catch (_: DateTimeParseException) {
            errors += "${ref(column)}: tanggal harus berformat YYYY-MM-DD, tertulis \"$raw\"."
            null
        }
    }

    private companion object {
        val EXCEL_EPOCH: LocalDate = LocalDate.of(1899, 12, 30)

        fun columnLetter(index: Int): String = ('A' + index).toString()

        fun plainNumber(value: Double): String =
            if (value == Math.floor(value)) value.toLong().toString() else value.toBigDecimal().stripTrailingZeros().toPlainString()
    }
}
