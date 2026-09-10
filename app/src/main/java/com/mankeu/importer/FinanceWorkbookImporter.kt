package com.mankeu.importer

import com.mankeu.domain.BookMode
import com.mankeu.domain.CategoryRole
import com.mankeu.domain.FinancialEventType
import java.security.MessageDigest
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

class FinanceWorkbookImporter(
    private val reader: XlsxWorkbookReader = XlsxWorkbookReader(),
    private val matcher: SemanticMatcher = FuzzySemanticMatcher,
) {
    fun preview(sourceName: String, bytes: ByteArray): WorkbookImportPreview {
        val hash = bytes.sha256()
        val workbook = runCatching { reader.read(bytes.inputStream()) }.getOrElse { error ->
            val detail = error.message?.take(180)?.let { " Detail teknis: $it" }.orEmpty()
            return WorkbookImportPreview(
                sourceName,
                hash,
                emptyList(),
                emptyList(),
                emptyList(),
                listOf("Berkas tidak dapat dibaca sebagai workbook .xlsx yang valid.$detail"),
            )
        }
        parseCanonicalExport(workbook)?.let { parsed ->
            return WorkbookImportPreview(
                sourceName = sourceName,
                sourceSha256 = hash,
                recognizedSheets = parsed.recognizedSheets,
                candidates = parsed.candidates,
                warnings = parsed.warnings,
                errors = if (parsed.candidates.isEmpty() && parsed.budgets.isEmpty()) {
                    listOf("Ekspor Mankeu tidak memuat transaksi aktif atau item anggaran yang dapat diimpor.")
                } else {
                    emptyList()
                },
                budgetCandidates = parsed.budgets,
                carriesBookMode = true,
            )
        }
        val roles = recognizeRoles(workbook)
        val missing = REQUIRED_ROLES.filterNot(roles::containsKey)
        if (missing.isNotEmpty()) {
            val year = inferYear(workbook)
            val genericWarnings = mutableListOf<String>()
            val generic = workbook.sheets.flatMap { parseDailyLedger(it, year, genericWarnings) }
            val genericBudgets = workbook.sheets
                .filter { sheet ->
                    val norm = FuzzySemanticMatcher.normalized(sheet.name)
                    "rab" in norm || "anggaran" in norm || "budget" in norm
                }
                .flatMap { parseGenericBudget(it) }
            val genericAssumptions = workbook.sheets
                .firstOrNull { sheet ->
                    val norm = FuzzySemanticMatcher.normalized(sheet.name)
                    "parameter" in norm || "asumsi" in norm || "assumption" in norm
                }
                ?.let(::parseAssumptions)
            if (generic.isNotEmpty() || genericBudgets.isNotEmpty() || genericAssumptions != null) {
                val recognized = workbook.sheets.filter { sheet ->
                    parseDailyLedger(sheet, year).isNotEmpty() ||
                        ((FuzzySemanticMatcher.normalized(sheet.name).let { "rab" in it || "anggaran" in it || "budget" in it }) &&
                            parseGenericBudget(sheet).isNotEmpty())
                }.map { it.name }
                val warning = if (genericBudgets.isEmpty()) {
                    "Workbook tabel tunggal dikenali melalui judul kolom. Tidak ada enrichment lintas lembar; tinjau seluruh pratinjau sebelum menyetujui."
                } else {
                    "Workbook tabel dikenali melalui judul kolom. Tinjau seluruh pratinjau sebelum menyetujui."
                }
                val hasMultipleModes = generic.any { it.bookMode == BookMode.PROJECTION } && generic.any { it.bookMode == BookMode.REALIZATION }
                return WorkbookImportPreview(
                    sourceName = sourceName,
                    sourceSha256 = hash,
                    recognizedSheets = recognized,
                    candidates = generic,
                    warnings = listOf(warning) + genericWarnings.distinct(),
                    errors = emptyList(),
                    budgetCandidates = genericBudgets,
                    carriesBookMode = hasMultipleModes,
                    assumptions = genericAssumptions,
                )
            }
            return WorkbookImportPreview(
                sourceName,
                hash,
                roles.values.map { it.name },
                emptyList(),
                emptyList(),
                listOf("Lembar keuangan belum lengkap: ${missing.joinToString()} tidak dikenali."),
            )
        }

        val year = inferYear(workbook)
        val dailySheet = roles.getValue(Role.DAILY)
        val ledgerWarnings = mutableListOf<String>()
        val direct = parseDailyLedger(dailySheet, year, ledgerWarnings)
        if (direct.isEmpty()) {
            return WorkbookImportPreview(
                sourceName,
                hash,
                roles.values.map { it.name },
                emptyList(),
                emptyList(),
                listOf("Tidak ada transaksi harian dengan kolom tanggal, uraian, pengeluaran, dan pemasukan yang dapat dibaca."),
            )
        }

        val rabSheet = roles[Role.BUDGET]
        val rabBudgets = rabSheet?.let { parseRabBudget(it) } ?: emptyList()
        val assumptions = roles[Role.PARAMETERS]?.let(::parseAssumptions)

        val enrichments = if (rabSheet != null) {
            findDeterministicEnrichments(
                rab = rabSheet,
                profitLoss = roles.getValue(Role.PROFIT_LOSS),
                cashFlow = roles.getValue(Role.CASH_FLOW),
                direct = direct,
                year = year,
            )
        } else {
            emptyList()
        }
        val warnings = buildList {
            addAll(ledgerWarnings)
            if (assumptions != null) {
                add("${assumptions.fieldCount} parameter perencanaan dibaca dari lembar parameter dan akan diisikan ke Asumsi buku Proyeksi.")
            }
            if (enrichments.isNotEmpty()) {
                add("${enrichments.size} catatan dipulihkan dari bukti konsisten lintas lembar dan harus ditinjau melalui notifikasi.")
            }
        }
        return WorkbookImportPreview(
            sourceName = sourceName,
            sourceSha256 = hash,
            recognizedSheets = roles.values.map { it.name },
            candidates = direct + enrichments,
            warnings = warnings,
            errors = emptyList(),
            budgetCandidates = rabBudgets,
            // A five-view workbook that carries a RAB *does* say which book each row belongs to:
            // the RAB sheet is the plan and the daily ledger is what actually happened. Reporting
            // otherwise put a target-book picker in front of the user, and `commitImport` applies
            // that choice to every row — so choosing "Realisasi", the obvious answer for a book of
            // real transactions, dropped the whole RAB into the realization book and left the plan
            // empty. With no RAB there is genuinely nothing to distinguish, and the picker stays.
            carriesBookMode = rabBudgets.isNotEmpty(),
            assumptions = assumptions,
        )
    }

    /**
     * Reads Mankeu's own export back in.
     *
     * Round 4 fix: this used to read only the `Transaksi Realisasi` sheet, so exporting a project
     * and importing it again silently dropped the entire Proyeksi book and every RAB line — the
     * exporter writes `Anggaran`/`Transaksi` for *both* books. Both are now read, and each row
     * carries the mode it came from so the commit lands in the right book.
     */
    private fun parseCanonicalExport(workbook: WorkbookSnapshot): CanonicalParse? {
        val metadata = workbook.sheets.firstOrNull { FuzzySemanticMatcher.normalized(it.name) == "metadata" } ?: return null
        val formatMarker = metadata.rows.any { row ->
            row.cells.any { it.display == "MANKEU_FINANCE_XLSX" || it.display == "UMKM_MANKEU_FINANCE_XLSX" || it.display == "ARINA_AGRI_FINANCE_XLSX" }
        }
        if (!formatMarker) return null

        val recognized = mutableListOf(metadata.name)
        val warnings = mutableListOf<String>()
        val candidates = mutableListOf<ImportCandidate>()
        val budgets = mutableListOf<ImportBudgetCandidate>()

        BookMode.entries.forEach { mode ->
            val label = mode.sheetLabel().lowercase()
            workbook.sheets.firstOrNull { "transaksi" in it.name.lowercase() && label in it.name.lowercase() }?.let { sheet ->
                recognized += sheet.name
                candidates += parseCanonicalTransactions(sheet, mode, warnings)
            }
            workbook.sheets.firstOrNull { "anggaran" in it.name.lowercase() && label in it.name.lowercase() }?.let { sheet ->
                recognized += sheet.name
                budgets += parseCanonicalBudget(sheet, mode)
            }
        }

        if (candidates.isEmpty() && budgets.isEmpty()) {
            warnings += "Lembar Transaksi/Anggaran ekspor Mankeu tidak ditemukan atau tidak memuat baris aktif."
        }
        return CanonicalParse(recognized, candidates, budgets, warnings)
    }

    private fun parseCanonicalTransactions(
        sheet: WorkbookSheet,
        mode: BookMode,
        warnings: MutableList<String>,
    ): List<ImportCandidate> {
        val header = sheet.rows.firstOrNull { row ->
            val values = row.cells.map { canonicalKey(it.display) }.toSet()
            listOf("tanggal", "jenis", "keterangan", "nominal").all(values::contains)
        } ?: run {
            warnings += "Judul kolom transaksi pada ${sheet.name} tidak dikenali; lembar dilewati."
            return emptyList()
        }
        val columns = header.cells.associate { canonicalKey(it.display) to it.columnIndex }
        fun column(name: String): Int? = columns[name]
        val dateColumn = column("tanggal") ?: return emptyList()
        val typeColumn = column("jenis") ?: return emptyList()
        val descriptionColumn = column("keterangan") ?: return emptyList()
        val amountColumn = column("nominal") ?: return emptyList()
        var skippedHistory = 0
        var skippedInvalid = 0
        val candidates = sheet.rows.filter { it.index > header.index }.mapNotNull { row ->
            val status = column("status")?.let(row::cellAt)?.display.orEmpty()
            val reversedBy = column("dibalik oleh id")?.let(row::cellAt)?.display.orEmpty()
            val type = runCatching { FinancialEventType.valueOf(row.cellAt(typeColumn)?.display.orEmpty()) }.getOrNull()
            if (type == FinancialEventType.REVERSAL || status == "REVERSED" || reversedBy.isNotBlank()) {
                skippedHistory += 1
                return@mapNotNull null
            }
            val amount = row.cellAt(amountColumn)?.moneyValue()
            val date = runCatching { LocalDate.parse(row.cellAt(dateColumn)?.display.orEmpty()) }.getOrNull()
            val description = row.cellAt(descriptionColumn)?.display?.trim().orEmpty()
            if (type == null || amount == null || amount <= 0 || date == null || description.isBlank()) {
                skippedInvalid += 1
                return@mapNotNull null
            }
            val counterparty = column("pihak")?.let(row::cellAt)?.display?.trim()?.ifBlank { null }
            if (type in setOf(FinancialEventType.CREDIT_SALE, FinancialEventType.CREDIT_PURCHASE_EXPENSE, FinancialEventType.CREDIT_PURCHASE_ASSET) && counterparty == null) {
                skippedInvalid += 1
                return@mapNotNull null
            }
            ImportCandidate(
                type = type,
                amountRupiah = amount,
                date = date,
                description = description,
                category = column("kategori")?.let(row::cellAt)?.display?.trim()?.ifBlank { "Impor Mankeu" } ?: "Impor Mankeu",
                classification = ImportClassification.DIRECT,
                sourceEvidence = listOf("${sheet.name}!A${row.index}"),
                rationale = "Catatan dibaca dari format ekspor kanonik Mankeu.",
                counterparty = counterparty,
                dueDate = column("jatuh tempo")?.let(row::cellAt)?.display?.takeIf(String::isNotBlank)?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
                reference = column("referensi")?.let(row::cellAt)?.display?.trim()?.ifBlank { null },
                sourceRecordId = column("id")?.let(row::cellAt)?.display?.trim()?.ifBlank { null },
                sourceObligationId = column("id kewajiban")?.let(row::cellAt)?.display?.trim()?.ifBlank { null },
                bookMode = mode,
            )
        }
        if (skippedHistory > 0) warnings += "$skippedHistory baris riwayat pembalikan pada ${sheet.name} dilewati; hanya keadaan aktif yang diimpor."
        if (skippedInvalid > 0) warnings += "$skippedInvalid baris tidak lengkap/tidak dikenali pada ${sheet.name} dilewati."
        return candidates
    }

    private fun parseCanonicalBudget(sheet: WorkbookSheet, mode: BookMode): List<ImportBudgetCandidate> {
        val header = sheet.rows.firstOrNull { row ->
            val values = row.cells.map { canonicalKey(it.display) }.toSet()
            listOf("kategori", "item", "total rencana").all(values::contains)
        } ?: return emptyList()
        val columns = header.cells.associate { canonicalKey(it.display) to it.columnIndex }
        fun column(name: String): Int? = columns[name]
        val categoryColumn = column("kategori") ?: return emptyList()
        val nameColumn = column("item") ?: return emptyList()
        val totalColumn = column("total rencana") ?: return emptyList()
        return sheet.rows.filter { it.index > header.index }.mapNotNull { row ->
            // Archived lines are history, not plan. Importing them would resurrect items the owner
            // deliberately retired, which is the same class of mistake as re-importing a reversal.
            if (column("status")?.let(row::cellAt)?.display == "DIARSIPKAN") return@mapNotNull null
            val name = row.cellAt(nameColumn)?.display?.trim().orEmpty()
            val total = row.cellAt(totalColumn)?.moneyValue()
            if (name.isBlank() || total == null || total <= 0) return@mapNotNull null
            ImportBudgetCandidate(
                bookMode = mode,
                categoryName = row.cellAt(categoryColumn)?.display?.trim()?.ifBlank { null } ?: "Impor Mankeu",
                name = name,
                plannedTotalRupiah = total,
                quantityText = column("jumlah")?.let(row::cellAt)?.display?.trim()?.ifBlank { null },
                unit = column("satuan")?.let(row::cellAt)?.display?.trim()?.ifBlank { null },
                unitPriceRupiah = column("harga satuan")?.let(row::cellAt)?.moneyValue(),
                plannedDate = column("tanggal")?.let(row::cellAt)?.display?.takeIf(String::isNotBlank)
                    ?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
                sourceEvidence = listOf("${sheet.name}!A${row.index}"),
            )
        }
    }

    private fun BookMode.sheetLabel(): String = when (this) {
        BookMode.PROJECTION -> "Proyeksi"
        BookMode.REALIZATION -> "Realisasi"
    }

    private fun parseRabBudget(sheet: WorkbookSheet): List<ImportBudgetCandidate> {
        val summaryPattern = Regex("total|subtotal|grand total|keuntungan|laba bersih|margin|hpp|bep|bagi hasil|ratio", RegexOption.IGNORE_CASE)

        val canonicalCandidates = parseCanonicalBudget(sheet, BookMode.PROJECTION)
        if (canonicalCandidates.isNotEmpty()) return canonicalCandidates

        val header = sheet.rows.firstOrNull { row ->
            val displays = row.cells.map { it.display.lowercase().trim() }
            displays.any { "uraian" in it || "item" in it || "deskripsi" in it || "keterangan" in it } &&
                displays.any { "volume" in it || "jumlah" in it || "harga" in it || "total" in it || "biaya" in it || "satuan" in it || "mt" in it }
        }

        val headerRowIndex = header?.index ?: 0
        var nameCol: Int = 1
        var volumeCol: Int? = null
        var unitCol: Int? = null
        var unitPriceCol: Int? = null
        var totalCol: Int? = null
        // Optional. A dated RAB is what lets the plan book produce a cash-flow schedule, and
        // therefore a real working-capital figure instead of Rp0 — see BudgetPlanCalculator.
        var dateCol: Int? = null

        if (header != null) {
            header.cells.forEach { cell ->
                val text = cell.display.lowercase().trim()
                when {
                    "uraian" in text || "item" in text || "deskripsi" in text || "keterangan" in text -> nameCol = cell.columnIndex
                    "volume" in text || ("jumlah" in text && "total" !in text) || "qty" in text -> volumeCol = cell.columnIndex
                    "harga" in text -> unitPriceCol = cell.columnIndex
                    "satuan" in text && unitCol == null -> unitCol = cell.columnIndex
                    // Before "total"/"rencana", or "Bulan Rencana" would be taken for a money column.
                    "bulan" in text || "tanggal" in text || "periode" in text -> dateCol = cell.columnIndex
                    "total" in text || "biaya" in text || "mt" in text || "rencana" in text -> totalCol = cell.columnIndex
                }
            }
        }

        var currentCategory = "Anggaran"
        var currentRole = CategoryRole.EXPENSE
        val result = mutableListOf<ImportBudgetCandidate>()

        for (row in sheet.rows) {
            if (row.index <= headerRowIndex) continue
            val allCells = row.cells.filter { it.display.isNotBlank() }
            if (allCells.isEmpty()) continue

            val nameCell = row.cellAt(nameCol) ?: row.cells.firstOrNull { it.columnIndex in 1..3 && it.display.isNotBlank() }
            val name = nameCell?.display?.trim().orEmpty()
            if (name.isBlank()) continue

            val firstCell = row.cellAt(0)
            val firstCellText = firstCell?.display?.trim().orEmpty()
            val numericValues = row.cells.filter { it.columnIndex >= 2 }.mapNotNull { it.moneyValue() }

            if (summaryPattern.containsMatchIn(name) || name.equals("total", ignoreCase = true) || name.startsWith("total ", ignoreCase = true)) {
                continue
            }

            // Section marker row e.g. "A. BIAYA VARIABEL", "I. SAPRODI", "ESTIMASI PENDAPATAN"
            if (numericValues.isEmpty() || (firstCellText.isNotBlank() && firstCell?.number == null && numericValues.size <= 1 && totalCol?.let { row.cellAt(it)?.moneyValue() } == null)) {
                val cleanCat = name.replace(Regex("^[A-Z0-9IVXLCDM]+\\.\\s*"), "").trim()
                if (cleanCat.isNotBlank() && !summaryPattern.containsMatchIn(cleanCat)) {
                    currentCategory = cleanCat
                    currentRole = when {
                        Regex("pendapatan|penerimaan|penjualan|omzet", RegexOption.IGNORE_CASE).containsMatchIn(cleanCat) ->
                            CategoryRole.REVENUE
                        // Equipment is bought once and shows up in investing cash flow, not in the
                        // cost of a cycle. A budget that files it as an operating expense reports a
                        // loss the business did not make.
                        Regex("peralatan|perlengkapan|mesin|aset|inventaris|kendaraan|alat", RegexOption.IGNORE_CASE).containsMatchIn(cleanCat) ->
                            CategoryRole.ASSET
                        else -> CategoryRole.EXPENSE
                    }
                }
                continue
            }

            val total = totalCol?.let { row.cellAt(it)?.moneyValue() }
                ?: row.cells.filter { it.columnIndex >= 4 }.mapNotNull { it.moneyValue() }.lastOrNull()
                ?: run {
                    val v = volumeCol?.let { row.cellAt(it)?.number }
                    val p = unitPriceCol?.let { row.cellAt(it)?.moneyValue() }
                    if (v != null && p != null && v > 0 && p > 0) (v * p).toLong() else null
                }

            if (total == null || total <= 0) continue

            // quantityValue(), not display: a numeric cell renders as "6000.0" through `display`,
            // and the app then showed a plan line as "6000.0 pcs". parseGenericBudget already
            // formatted it correctly; this path did not.
            val volume = volumeCol?.let { row.cellAt(it)?.quantityValue() }?.takeIf { it.isNotBlank() && it != "-" }
            val unit = unitCol?.let { row.cellAt(it)?.display?.trim() }?.takeIf { it.isNotBlank() && it != "-" }
            val unitPrice = unitPriceCol?.let { row.cellAt(it)?.moneyValue() }?.takeIf { it > 0 }

            val itemCategory = if (currentCategory != "Anggaran") {
                currentCategory
            } else {
                CategoryClassifier.classify(name, currentRole) ?: "Anggaran"
            }

            result += ImportBudgetCandidate(
                bookMode = BookMode.PROJECTION,
                categoryName = itemCategory,
                categoryRole = currentRole,
                name = name,
                plannedTotalRupiah = total,
                quantityText = volume,
                unit = unit,
                unitPriceRupiah = unitPrice,
                plannedDate = dateCol?.let { row.cellAt(it) }?.let(::parsePlannedDate),
                sourceEvidence = listOf("${sheet.name}!A${row.index}"),
            )
        }

        return result
    }

    /**
     * A plan line's period. Accepts a full ISO date, an Excel serial date, or a bare month
     * (`2026-02`), which is how a budget is usually phased — the day within the month is not
     * something a plan claims to know, so it is normalised to the first.
     */
    private fun parsePlannedDate(cell: WorkbookCell): LocalDate? {
        val raw = cell.display.trim()
        if (raw.isBlank()) return null
        runCatching { LocalDate.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE) }.getOrNull()?.let { return it }
        Regex("""^(20\d{2})-(\d{1,2})$""").find(raw)?.let { match ->
            val (year, month) = match.destructured
            return runCatching { LocalDate.of(year.toInt(), month.toInt(), 1) }.getOrNull()
        }
        cell.number?.let { serial ->
            if (serial in 1.0..100_000.0) return LocalDate.of(1899, 12, 30).plusDays(serial.toLong())
        }
        return null
    }

    private fun recognizeRoles(workbook: WorkbookSnapshot): Map<Role, WorkbookSheet> {
        val result = linkedMapOf<Role, WorkbookSheet>()
        workbook.sheets.forEach { sheet ->
            val normalized = FuzzySemanticMatcher.normalized(sheet.name)
            val role = when {
                listOf("pasca pembiayaan", "post financing", "setelah pembiayaan").any { it in sheet.name.lowercase() } -> Role.POST_FINANCING
                // Tested before BUDGET so an "Asumsi dan Target Usaha" sheet is read as parameters
                // rather than as a second budget. Neither word appears in the four sheet roles the
                // five-view contract already names.
                "parameter" in normalized || "asumsi" in normalized || "assumption" in normalized -> Role.PARAMETERS
                "rab" in normalized || "anggaran" in normalized || "budget" in normalized -> Role.BUDGET
                "transaksi" in normalized || "ledger" in normalized || "harian" in normalized -> Role.DAILY
                "laba" in normalized || "rugi" in normalized || "profit" in normalized -> Role.PROFIT_LOSS
                "arus kas" in sheet.name.lowercase() || "cash flow" in sheet.name.lowercase() -> Role.CASH_FLOW
                else -> null
            }
            if (role != null && role !in result) result[role] = sheet
        }
        return result
    }

    private fun parseDailyLedger(
        sheet: WorkbookSheet,
        year: Int,
        warnings: MutableList<String> = mutableListOf(),
    ): List<ImportCandidate> {
        val header = sheet.rows.firstOrNull { row ->
            val values = row.cells.associate { it.columnIndex to it.display.lowercase() }
            values.values.any { "tanggal" in it || "date" in it } &&
                values.values.any { "uraian" in it || "deskripsi" in it || "description" in it || "keterangan" in it || "transaksi" in it }
        } ?: return emptyList()
        val columns = header.cells.associate { cell -> cell.columnIndex to cell.display.lowercase() }
        val dateColumn = columns.firstKeyContaining("tanggal", "date") ?: return emptyList()
        val descriptionColumn = columns.firstKeyContaining("uraian", "deskripsi", "description", "keterangan", "transaksi") ?: return emptyList()
        val categoryColumn = columns.firstKeyContaining("kategori", "category")
        val expenseColumn = columns.firstKeyContaining("pengeluaran", "uang keluar", "expense", "debit", "keluar") ?: return emptyList()
        val incomeColumn = columns.firstKeyContaining("pemasukan", "uang masuk", "income", "kredit", "masuk", "penerimaan") ?: return emptyList()

        // Optional columns. A ledger with none of them behaves exactly as before — two money
        // columns, every row a plain cash movement. A ledger that declares a `Jenis` gets the
        // full event vocabulary instead; see [TransactionKindLabels] for why that matters.
        val kindColumn = columns.firstKeyContaining("jenis", "tipe")
        val amountColumn = columns.firstKeyContaining("nominal", "nilai transaksi")
        val counterpartyColumn = columns.firstKeyContaining("pihak", "pelanggan", "pemasok")
        val dueColumn = columns.firstKeyContaining("jatuh tempo", "due")
        val obligationColumn = columns.firstKeyContaining("kewajiban", "obligation")
        val unitPriceColumn = columns.firstKeyContaining("harga")
        val unitColumn = columns.entries
            .firstOrNull { it.key != unitPriceColumn && ("satuan" in it.value || "unit" in it.value) }?.key
        val quantityColumn = columns.firstKeyContaining("volume", "kuantitas", "qty")

        val isProjection = "proyeksi" in sheet.name.lowercase() || "rencana" in sheet.name.lowercase() || "plan" in sheet.name.lowercase()
        val targetBookMode = if (isProjection) BookMode.PROJECTION else BookMode.REALIZATION
        var skippedAmount = 0
        var skippedCounterparty = 0
        var skippedObligation = 0

        return sheet.rows.asSequence()
            .filter { it.index > header.index }
            .flatMap { row ->
                val description = row.cellAt(descriptionColumn)?.display?.trim().orEmpty()
                if (description.isBlank()) return@flatMap emptySequence()
                val dateCell = row.cellAt(dateColumn)
                val (date, inferred) = parseDate(dateCell, year)
                val expense = row.cellAt(expenseColumn)?.moneyValue()
                val income = row.cellAt(incomeColumn)?.moneyValue()
                val explicitCategory = categoryColumn?.let { row.cellAt(it)?.display?.trim() }?.ifBlank { null }

                val declaredKind = kindColumn?.let { row.cellAt(it)?.display?.trim() }?.ifBlank { null }
                val declaredType = TransactionKindLabels.parse(declaredKind)
                if (declaredType != null) {
                    val amount = amountColumn?.let { row.cellAt(it)?.moneyValue() }
                        ?: income?.takeIf { it > 0 }
                        ?: expense
                    if (amount == null || amount <= 0) {
                        skippedAmount += 1
                        return@flatMap emptySequence()
                    }
                    val counterparty = counterpartyColumn?.let { row.cellAt(it)?.display?.trim() }?.ifBlank { null }
                    if (declaredType in CREDIT_TYPES && counterparty == null) {
                        skippedCounterparty += 1
                        return@flatMap emptySequence()
                    }
                    val obligationReference = obligationColumn?.let { row.cellAt(it)?.display?.trim() }?.ifBlank { null }
                    if (declaredType in SETTLEMENT_TYPES && obligationReference == null) {
                        // Without it the commit cannot tell which receivable or payable the money
                        // settles; guessing would post the cash against the wrong counterparty.
                        skippedObligation += 1
                        return@flatMap emptySequence()
                    }
                    val incoming = declaredType in TransactionKindLabels.incoming
                    val role = if (incoming) CategoryRole.REVENUE else CategoryRole.EXPENSE
                    return@flatMap sequenceOf(
                        ImportCandidate(
                            type = declaredType,
                            amountRupiah = amount,
                            date = date,
                            description = description,
                            category = explicitCategory
                                ?: CategoryClassifier.classify(description, role)
                                ?: if (incoming) "Pemasukan impor" else "Pengeluaran impor",
                            classification = ImportClassification.DIRECT,
                            sourceEvidence = listOf("${sheet.name}!A${row.index}"),
                            rationale = "Baris menyatakan jenisnya sendiri (\"$declaredKind\") pada buku transaksi harian.",
                            inferredFields = if (inferred) listOf("year-or-day") else emptyList(),
                            counterparty = counterparty,
                            dueDate = dueColumn?.let { row.cellAt(it)?.display?.trim() }
                                ?.takeIf(String::isNotBlank)
                                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
                            sourceObligationId = obligationReference,
                            bookMode = targetBookMode,
                            quantityText = quantityColumn?.let { row.cellAt(it)?.quantityValue() },
                            unit = unitColumn?.let { row.cellAt(it)?.display?.trim() }?.ifBlank { null },
                            unitPriceRupiah = unitPriceColumn?.let { row.cellAt(it)?.moneyValue() }?.takeIf { it > 0 },
                        ),
                    )
                }
                if (declaredKind != null) {
                    warnings += "Jenis transaksi \"$declaredKind\" pada ${sheet.name} tidak dikenali; baris dibaca sebagai uang masuk atau uang keluar biasa."
                }
                sequence {
                    if (expense != null && expense > 0) yield(
                        ImportCandidate(
                            FinancialEventType.CASH_EXPENSE,
                            expense,
                            date,
                            description,
                            explicitCategory ?: CategoryClassifier.classify(description, CategoryRole.EXPENSE) ?: "Pengeluaran impor",
                            ImportClassification.DIRECT,
                            listOf("${sheet.name}!${row.cellAt(expenseColumn)?.reference}"),
                            categorisedRationale(description, CategoryRole.EXPENSE),
                            if (inferred) listOf("year-or-day") else emptyList(),
                            bookMode = targetBookMode,
                        ),
                    )
                    if (income != null && income > 0) yield(
                        ImportCandidate(
                            FinancialEventType.CASH_INCOME,
                            income,
                            date,
                            description,
                            explicitCategory ?: CategoryClassifier.classify(description, CategoryRole.REVENUE) ?: "Pemasukan impor",
                            ImportClassification.DIRECT,
                            listOf("${sheet.name}!${row.cellAt(incomeColumn)?.reference}"),
                            categorisedRationale(description, CategoryRole.REVENUE),
                            if (inferred) listOf("year-or-day") else emptyList(),
                            bookMode = targetBookMode,
                        ),
                    )
                }
            }
            .toList()
            .also {
                if (skippedAmount > 0) warnings += "$skippedAmount baris pada ${sheet.name} dilewati karena tidak memuat nominal."
                if (skippedCounterparty > 0) warnings += "$skippedCounterparty baris tempo pada ${sheet.name} dilewati karena tidak memuat nama pihak."
                if (skippedObligation > 0) warnings += "$skippedObligation baris pelunasan pada ${sheet.name} dilewati karena tidak menyebut nomor kewajiban asalnya."
            }
    }

    /**
     * Reads a label/value parameter sheet into [ImportedAssumptions].
     *
     * A parameter sheet is a two-column list in every workbook that has one — a label on the left,
     * a figure beside it, sometimes a unit after that — so the parse is by label keyword rather
     * than by column position, which no two workbooks agree on. Rows whose label matches nothing
     * are ignored rather than guessed at.
     *
     * Keyword order is load-bearing. "Modal Kas Awal" contains both "kas awal" and "modal", and it
     * means opening cash, not owner capital; testing opening cash first is what keeps the owner's
     * equity out of a cash line.
     */
    private fun parseAssumptions(sheet: WorkbookSheet): ImportedAssumptions? {
        var openingCash: Long? = null
        var ownerCapital: Long? = null
        var loan: Long? = null
        var interestBps: Int? = null
        var otherCost: Long? = null
        var quantity: String? = null
        var unit: String? = null
        var price: Long? = null
        val evidence = mutableListOf<String>()

        sheet.rows.forEach { row ->
            val labelCell = row.cells.firstOrNull { !it.text.isNullOrBlank() } ?: return@forEach
            val label = labelCell.text.orEmpty().lowercase().trim()
            // The first cell to the right of the label that holds a figure, and the one after it,
            // which is where a unit ("botol", "Rp", "bps", "%") sits when the sheet has that column.
            val valueCell = row.cells
                .filter { it.columnIndex > labelCell.columnIndex }
                .firstOrNull { it.number != null || it.text?.trim()?.toLongOrNull() != null }
                ?: return@forEach
            val amount = valueCell.moneyValue() ?: return@forEach
            val unitText = row.cells
                .filter { it.columnIndex > valueCell.columnIndex }
                .firstOrNull { !it.text.isNullOrBlank() }
                ?.text?.trim()
                .orEmpty()
            fun record() = evidence.add("${sheet.name}!${valueCell.reference}")

            when {
                label.containsAny("kas awal", "saldo kas", "opening cash") -> {
                    openingCash = amount; record()
                }
                label.containsAny("modal sendiri", "modal awal", "modal pemilik", "own capital", "owner capital") -> {
                    ownerCapital = amount; record()
                }
                // Before the loan branch: "Suku bunga pinjaman" contains "pinjaman", and reading
                // it as the loan amount would turn a 12% rate into a Rp1.200 principal.
                label.containsAny("suku bunga", "bunga pinjaman", "interest") -> {
                    // Written either as basis points (1200) or as a percentage (12 or 12,5).
                    // The unit column says which; without one, anything under 100 is a percentage,
                    // because a 0,99% loan is possible and a 99-basis-point column heading is not.
                    val asPercent = unitText.contains("%") ||
                        (!unitText.contains("bps", ignoreCase = true) && amount in 0..100)
                    interestBps = if (asPercent) (amount * 100).toInt() else amount.toInt()
                    record()
                }
                label.containsAny("pinjaman", "kredit modal", "loan") -> {
                    loan = amount; record()
                }
                label.containsAny("biaya pembiayaan", "biaya administrasi pinjaman", "provisi", "financing cost") -> {
                    otherCost = amount; record()
                }
                label.containsAny("harga jual", "selling price", "harga satuan produk") -> {
                    price = amount; record()
                }
                label.containsAny("kapasitas produksi", "target volume", "volume penjualan", "target penjualan", "target produksi", "output quantity") -> {
                    quantity = valueCell.quantityValue()
                    unit = unitText.takeIf { it.isNotBlank() && !it.equals("rp", ignoreCase = true) }
                    record()
                }
            }
        }

        val result = ImportedAssumptions(
            openingCashRupiah = openingCash,
            ownerCapitalRupiah = ownerCapital,
            loanAmountRupiah = loan,
            interestRateBasisPoints = interestBps,
            otherFinancingCostRupiah = otherCost,
            outputQuantityText = quantity,
            outputUnit = unit,
            outputPriceRupiah = price,
            sourceEvidence = evidence,
        )
        return result.takeUnless { it.isEmpty }
    }

    private fun String.containsAny(vararg needles: String): Boolean = needles.any { it in this }

    private fun parseGenericBudget(sheet: WorkbookSheet): List<ImportBudgetCandidate> {
        val header = sheet.rows.firstOrNull { row ->
            val values = row.cells.map { canonicalKey(it.display) }
            values.any { "kategori" in it } &&
                values.any { "item" in it || "kebutuhan" in it || "nama" in it || "uraian" in it } &&
                values.any { "total" in it || "rencana" in it || "anggaran" in it || "biaya" in it || "jumlah" in it }
        } ?: return emptyList()
        val columns = header.cells.associate { canonicalKey(it.display) to it.columnIndex }
        fun colContaining(vararg keys: String): Int? = columns.entries.firstOrNull { entry -> keys.any { it in entry.key } }?.value
        val categoryColumn = colContaining("kategori")
        val nameColumn = colContaining("item", "kebutuhan", "nama", "uraian") ?: return emptyList()
        val totalColumn = colContaining("total", "rencana", "anggaran", "biaya") ?: return emptyList()
        val qtyColumn = columns.entries.firstOrNull { it.value != totalColumn && listOf("jumlah", "qty", "volume", "kuantitas").any { k -> k in it.key } }?.value
        val unitColumn = colContaining("satuan", "unit")
        val priceColumn = colContaining("harga", "price", "tarif")

        return sheet.rows.filter { it.index > header.index }.mapNotNull { row ->
            val name = row.cellAt(nameColumn)?.display?.trim().orEmpty()
            val total = row.cellAt(totalColumn)?.moneyValue()
            if (name.isBlank() || total == null || total <= 0) return@mapNotNull null
            val rawQty = qtyColumn?.let { row.cellAt(it) }
            val formattedQty = when {
                rawQty?.number != null -> {
                    val num = rawQty.number
                    if (num % 1.0 == 0.0) num.toLong().toString() else num.toString()
                }
                else -> rawQty?.display?.trim()?.ifBlank { null }
            }
            ImportBudgetCandidate(
                bookMode = BookMode.PROJECTION,
                categoryName = categoryColumn?.let { row.cellAt(it)?.display?.trim() }?.ifBlank { null } ?: "RAB Impor",
                name = name,
                plannedTotalRupiah = total,
                quantityText = formattedQty,
                unit = unitColumn?.let { row.cellAt(it)?.display?.trim() }?.ifBlank { null },
                unitPriceRupiah = priceColumn?.let { row.cellAt(it)?.moneyValue() },
                plannedDate = null,
                sourceEvidence = listOf("${sheet.name}!A${row.index}"),
            )
        }
    }

    private fun findDeterministicEnrichments(
        rab: WorkbookSheet,
        profitLoss: WorkbookSheet,
        cashFlow: WorkbookSheet,
        direct: List<ImportCandidate>,
        year: Int,
    ): List<ImportCandidate> {
        val rabItems = rab.rows.mapNotNull { row ->
            val first = row.cellAt(0)?.number ?: return@mapNotNull null
            if (abs(first - first.roundToLong()) > 0.001) return@mapNotNull null
            val label = row.cells.firstOrNull { it.columnIndex in 1..3 && !it.text.isNullOrBlank() } ?: return@mapNotNull null
            val amount = row.cells.filter { it.columnIndex >= 4 }.mapNotNull { cell -> cell.moneyValue()?.let { cell to it } }.lastOrNull()
                ?: return@mapNotNull null
            if (amount.second <= 0) return@mapNotNull null
            Evidence(label.text!!.trim(), amount.second, "${rab.name}!${label.reference}", amount.first.reference, row)
        }

        return rabItems.mapNotNull { item ->
            val relatedDirect = direct.filter { event ->
                event.type == FinancialEventType.CASH_EXPENSE &&
                    aggregationCompatible(item.label, event.description) &&
                    matcher.similarity(item.label, event.description) > 0.0
            }
            val represented = relatedDirect.any {
                it.amountRupiah == item.amount && matcher.similarity(item.label, it.description) >= 0.43
            } ||
                relatedDirect.sumOf { it.amountRupiah } == item.amount
            if (represented) return@mapNotNull null

            val support = mutableListOf("${item.labelRef}/${item.amountRef}")
            val reportMatch = findEvidence(profitLoss, item)
            val cashMatch = findEvidence(cashFlow, item)
            if (reportMatch != null) support += reportMatch
            if (cashMatch != null) support += cashMatch
            if (support.size < 3) return@mapNotNull null

            val month = cashMatch?.let { source -> inferMonthFromRow(cashFlow, source.substringAfter('!').substringBefore('/')) }
                ?: Month.JANUARY
            ImportCandidate(
                type = FinancialEventType.CASH_EXPENSE,
                amountRupiah = item.amount,
                date = LocalDate.of(year, month, 1),
                description = item.label,
                category = CategoryClassifier.classify(item.label, CategoryRole.EXPENSE) ?: "Pengeluaran dipulihkan",
                classification = ImportClassification.ENRICHED_DETERMINISTIC,
                sourceEvidence = support,
                rationale = "Catatan harian tidak memuat transaksi ini, tetapi RAB, laporan laba rugi, dan arus kas menunjukkan uraian dan jumlah yang konsisten.",
                inferredFields = listOf("exact-day"),
            )
        }
    }

    /**
     * The preview line for a directly-read row, naming the category the row was placed in.
     *
     * Categorisation is the one thing the importer decides that the workbook does not state
     * outright, so it is said out loud in the preview rather than discovered later in a report.
     */
    private fun categorisedRationale(description: String, role: CategoryRole): String {
        val category = CategoryClassifier.classify(description, role)
        val base = "Nilai tercatat langsung pada buku transaksi harian."
        return if (category == null) base else "$base Diletakkan pada kategori \"$category\" dari uraiannya."
    }

    private fun findEvidence(sheet: WorkbookSheet, expected: Evidence): String? {
        return sheet.rows.asSequence().mapNotNull { row ->
            val labels = row.cells.filter { !it.text.isNullOrBlank() }
            val amounts = row.cells.mapNotNull { cell -> cell.moneyValue()?.let { cell to it } }
            labels.flatMap { label ->
                amounts.map { amount -> Triple(label, amount, matcher.similarity(expected.label, label.text.orEmpty())) }
            }.filter { (_, amount, score) -> amount.second == expected.amount && score >= 0.55 }
                .maxByOrNull { it.third }
                ?.let { (label, amount, _) -> "${sheet.name}!${label.reference}/${amount.first.reference}" }
        }.firstOrNull()
    }

    private fun inferMonthFromRow(sheet: WorkbookSheet, labelReference: String): Month {
        val rowIndex = labelReference.dropWhile(Char::isLetter).toIntOrNull() ?: return Month.JANUARY
        val row = sheet.rows.firstOrNull { it.index == rowIndex } ?: return Month.JANUARY
        val amountCell = row.cells.firstOrNull { (it.moneyValue() ?: 0) > 0 } ?: return Month.JANUARY
        val headerText = sheet.rows.asSequence()
            .filter { it.index < rowIndex }
            .mapNotNull { it.cellAt(amountCell.columnIndex)?.text }
            .lastOrNull()
            .orEmpty()
        return parseMonth(headerText) ?: Month.JANUARY
    }

    private fun inferYear(workbook: WorkbookSnapshot): Int {
        val regex = Regex("\\b(20\\d{2})\\b")
        return workbook.sheets.asSequence().flatMap { it.rows.asSequence() }.flatMap { it.cells.asSequence() }
            .mapNotNull { regex.find(it.display)?.groupValues?.get(1)?.toIntOrNull() }
            .firstOrNull() ?: LocalDate.now().year
    }

    private fun parseDate(cell: WorkbookCell?, year: Int): Pair<LocalDate, Boolean> {
        val number = cell?.number
        if (number != null && number in 1.0..100_000.0) {
            return LocalDate.of(1899, 12, 30).plusDays(number.toLong()) to false
        }
        val raw = cell?.display?.trim().orEmpty()
        runCatching { LocalDate.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE) }.getOrNull()?.let { return it to false }
        val parts = raw.lowercase(Locale.forLanguageTag("id-ID")).replace('.', ' ').split(Regex("\\s+")).filter(String::isNotBlank)
        val day = parts.firstOrNull()?.toIntOrNull()
        val month = parts.drop(1).firstNotNullOfOrNull(::parseMonth)
        return if (day != null && month != null) LocalDate.of(year, month, day) to true
        else LocalDate.of(year, 1, 1) to true
    }

    private fun parseMonth(value: String): Month? = when (value.lowercase().take(3)) {
        "jan" -> Month.JANUARY
        "feb" -> Month.FEBRUARY
        "mar" -> Month.MARCH
        "apr" -> Month.APRIL
        "mei", "may" -> Month.MAY
        "jun" -> Month.JUNE
        "jul" -> Month.JULY
        "agu", "ags", "aug" -> Month.AUGUST
        "sep" -> Month.SEPTEMBER
        "okt", "oct" -> Month.OCTOBER
        "nov" -> Month.NOVEMBER
        "des", "dec" -> Month.DECEMBER
        else -> null
    }

    /**
     * A volume as the app stores it: a plain decimal string, no thousands separators and no
     * trailing `.0` on a whole number. `2000.0` on screen for two thousand bottles reads as a
     * different figure than the workbook shows.
     */
    private fun WorkbookCell.quantityValue(): String? {
        number?.let { value ->
            if (!value.isFinite()) return null
            return if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
        }
        return text?.trim()?.ifBlank { null }
    }

    private fun WorkbookCell.moneyValue(): Long? {
        number?.let { if (it.isFinite() && abs(it - it.roundToLong()) < 0.001) return it.roundToLong() }
        val cleaned = text?.replace(Regex("[^0-9-]"), "").orEmpty()
        return cleaned.takeIf { it.isNotBlank() && it != "-" }?.toLongOrNull()
    }

    private fun Map<Int, String>.firstKeyContaining(vararg aliases: String): Int? = entries
        .firstOrNull { (_, value) -> aliases.any { alias -> alias in value } }
        ?.key

    private fun canonicalKey(value: String): String = value.lowercase().trim().replace(Regex("\\s+"), " ")

    private fun aggregationCompatible(budgetLabel: String, ledgerDescription: String): Boolean {
        val budgetTokens = FuzzySemanticMatcher.normalized(budgetLabel).split(' ').toSet()
        val ledgerTokens = FuzzySemanticMatcher.normalized(ledgerDescription).split(' ').toSet()
        return !(("tenaga" in ledgerTokens) && ("tenaga" !in budgetTokens))
    }

    private fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(this)
        .joinToString("") { "%02x".format(it) }

    private data class Evidence(
        val label: String,
        val amount: Long,
        val labelRef: String,
        val amountRef: String,
        val row: WorkbookRow,
    )

    private data class CanonicalParse(
        val recognizedSheets: List<String>,
        val candidates: List<ImportCandidate>,
        val budgets: List<ImportBudgetCandidate>,
        val warnings: List<String>,
    )

    private enum class Role { PARAMETERS, BUDGET, DAILY, PROFIT_LOSS, CASH_FLOW, POST_FINANCING }

    private companion object {
        val REQUIRED_ROLES = setOf(Role.DAILY, Role.PROFIT_LOSS, Role.CASH_FLOW)
        val CREDIT_TYPES = setOf(
            FinancialEventType.CREDIT_SALE,
            FinancialEventType.CREDIT_PURCHASE_EXPENSE,
            FinancialEventType.CREDIT_PURCHASE_ASSET,
        )
        val SETTLEMENT_TYPES = setOf(
            FinancialEventType.RECEIVABLE_COLLECTION,
            FinancialEventType.PAYABLE_SETTLEMENT,
        )
    }
}
