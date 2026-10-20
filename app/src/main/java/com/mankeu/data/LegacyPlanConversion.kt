package com.mankeu.data

import androidx.sqlite.db.SupportSQLiteDatabase
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

/**
 * Schema 9 (DEC-014): the four-number BEP calculator becomes a business plan. Shared by
 * `MIGRATION_8_9` and by restoring a backup older than schema 9, so old parameters are never lost.
 *
 * For a Proyeksi book's old `scenario_assumptions` row:
 * - price/unit/target → product "Produk utama"; variable cost per unit → its one recipe line;
 * - fixed cost → a monthly FIXED plan line; owner capital → a capital plan line;
 * - opening cash is **not** turned into a transaction (that would invent a record); the notice
 *   names it so the owner can record it as Setoran Modal if it is real.
 * Realisasi rows held per-book copies of the same calculator; their values are only listed.
 */
object LegacyPlanConversion {
    /** Default cost nature for the basic and common legacy expense categories, by normalized name. */
    fun applyDefaultCostNatures(db: SupportSQLiteDatabase) {
        val variable = listOf("bahan baku stok", "bahan baku dan stok", "bahan baku", "kemasan dan label", "kemasan", "transportasi", "transport dan pengiriman")
        val fixed = listOf(
            "gaji upah", "gaji dan upah", "sewa tempat", "listrik air internet", "listrik air dan internet",
            "biaya lain lain", "perizinan dan administrasi", "pemasaran dan promosi", "biaya tetap",
        )
        fun mark(nature: String, names: List<String>) = db.execSQL(
            "UPDATE categories SET costNature = '$nature' WHERE role = 'EXPENSE' AND costNature IS NULL AND normalizedName IN (${names.joinToString { "'$it'" }})",
        )
        mark("VARIABLE", variable)
        mark("FIXED", fixed)
    }

    /** @param rows old `scenario_assumptions` rows as column → value. */
    fun convertAssumptions(db: SupportSQLiteDatabase, rows: List<Map<String, Any?>>, source: String) {
        val lines = mutableListOf<String>()
        val now = System.currentTimeMillis()
        rows.forEach { row ->
            val bookId = row["bookId"] as? String ?: return@forEach
            val (mode, projectId, businessId) = db.query(
                "SELECT b.mode, b.projectId, p.businessId FROM books b JOIN projects p ON p.id = b.projectId WHERE b.id = ?",
                arrayOf(bookId),
            ).use { c -> if (c.moveToFirst()) Triple(c.getString(0), c.getString(1), c.getString(2)) else return@forEach }
            val price = (row["outputPriceRupiah"] as? Number)?.toLong()
            val variable = (row["variableCostPerUnitRupiah"] as? Number)?.toLong()
            val fixed = (row["fixedCostRupiah"] as? Number)?.toLong()
            val capital = (row["ownerCapitalRupiah"] as? Number)?.toLong() ?: 0
            val opening = (row["openingCashRupiah"] as? Number)?.toLong() ?: 0
            val unit = (row["outputUnit"] as? String)?.ifBlank { null } ?: "unit"
            val target = (row["outputQuantityText"] as? String)?.ifBlank { null }

            if (mode != "PROJECTION") {
                if (price != null || variable != null || fixed != null || capital > 0 || opening > 0) {
                    lines += "• Buku Realisasi berisi parameter lama (harga ${rp(price)}, biaya variabel ${rp(variable)}, biaya tetap ${rp(fixed)}). " +
                        "BEP Realisasi kini dihitung dari transaksi dan resep produk, jadi nilai ini tidak dipakai lagi."
                }
                return@forEach
            }
            if (price != null) {
                val productId = UUID.randomUUID().toString()
                db.execSQL(
                    "INSERT INTO products(id, projectId, name, unit, priceRupiah, targetPerMonthText, sortOrder, archived, createdAtEpochMillis, updatedAtEpochMillis) VALUES (?, ?, 'Produk utama', ?, ?, ?, 0, 0, ?, ?)",
                    arrayOf<Any?>(productId, projectId, unit, price, target, now, now),
                )
                if (variable != null && variable > 0) {
                    db.execSQL(
                        "INSERT INTO product_cost_components(id, productId, name, amountRupiah, yieldText, sortOrder) VALUES (?, ?, 'Biaya variabel (dari kalkulator lama)', ?, '1', 0)",
                        arrayOf<Any?>(UUID.randomUUID().toString(), productId, variable),
                    )
                }
                lines += "• Produk \"Produk utama\": harga ${rp(price)}/$unit, biaya variabel ${rp(variable)}/$unit, target ${target ?: "-"}/bulan."
            }
            if (fixed != null && fixed > 0) {
                val categoryId = category(db, businessId, "EXPENSE", "Biaya Tetap", "FIXED", now)
                planLine(db, bookId, categoryId, "Biaya tetap (dari kalkulator lama)", fixed, "MONTHLY", "FIXED", now)
                lines += "• Biaya tetap bulanan ${rp(fixed)} menjadi baris rencana berulang."
            }
            if (capital > 0) {
                val categoryId = category(db, businessId, "CAPITAL_IN", "Setoran Modal Pemilik", null, now)
                planLine(db, bookId, categoryId, "Setoran modal (dari kalkulator lama)", capital, "ONCE", null, now)
                lines += "• Rencana modal sendiri ${rp(capital)} menjadi baris Setoran Modal."
            }
            if (opening > 0) {
                lines += "• Saldo kas awal ${rp(opening)} tidak lagi diisi di kalkulator. Bila uang itu memang ada, catat sebagai Uang masuk berkategori Setoran Modal Pemilik."
            }
        }
        if (lines.isEmpty()) return
        db.execSQL(
            "INSERT INTO import_notices(id, batchId, severity, title, explanation, amountRupiah, sourceEvidence, reviewed, action, createdAtEpochMillis) VALUES (?, NULL, 'SYSTEM', ?, ?, NULL, ?, 0, NULL, ?)",
            arrayOf<Any?>(
                UUID.randomUUID().toString(),
                "Parameter BEP lama dipindahkan ke Rencana Usaha",
                "Kalkulator BEP kini dihitung dari produk, resep, dan rencana biaya.\n" + lines.joinToString("\n"),
                source,
                now,
            ),
        )
    }

    private fun category(db: SupportSQLiteDatabase, businessId: String, role: String, name: String, nature: String?, now: Long): String {
        val normalized = normalize(name)
        db.query("SELECT id FROM categories WHERE businessId = ? AND role = ? AND active = 1 ORDER BY normalizedName = ? DESC LIMIT 1", arrayOf(businessId, role, normalized))
            .use { c -> if (c.moveToFirst()) return c.getString(0) }
        val id = UUID.randomUUID().toString()
        db.execSQL(
            "INSERT OR IGNORE INTO categories(id, businessId, role, name, normalizedName, aliases, templateOrigin, active, createdAtEpochMillis, costNature) VALUES (?, ?, ?, ?, ?, '', 'DEFAULT', 1, ?, ?)",
            arrayOf<Any?>(id, businessId, role, name, normalized, now, nature),
        )
        // An archived row with the same key wins the unique index; revive it instead.
        db.execSQL("UPDATE categories SET active = 1 WHERE businessId = ? AND role = ? AND normalizedName = ?", arrayOf(businessId, role, normalized))
        return db.query("SELECT id FROM categories WHERE businessId = ? AND role = ? AND normalizedName = ?", arrayOf(businessId, role, normalized))
            .use { c -> c.moveToFirst(); c.getString(0) }
    }

    private fun planLine(db: SupportSQLiteDatabase, bookId: String, categoryId: String, name: String, amount: Long, recurrence: String, nature: String?, now: Long) {
        val sortOrder = db.query("SELECT COALESCE(MAX(sortOrder), 0) + 1 FROM budget_items WHERE bookId = ?", arrayOf(bookId)).use { c -> c.moveToFirst(); c.getInt(0) }
        db.execSQL(
            "INSERT INTO budget_items(id, bookId, categoryId, name, normalizedName, plannedTotalRupiah, aliases, amountSource, formulaDifferenceRupiah, sortOrder, archived, createdAtEpochMillis, updatedAtEpochMillis, costNature, recurrence) " +
                "VALUES (?, ?, ?, ?, ?, ?, '', 'USER_TOTAL', 0, ?, 0, ?, ?, ?, ?)",
            arrayOf<Any?>(UUID.randomUUID().toString(), bookId, categoryId, name, normalize(name), amount, sortOrder, now, now, nature, recurrence),
        )
    }

    /** Same normalization the repository uses for category and plan names. */
    fun normalize(value: String): String = value.lowercase(Locale.forLanguageTag("id-ID")).replace(Regex("[^a-z0-9]+"), " ").trim()

    private fun rp(value: Long?) = value?.let { "Rp" + NumberFormat.getIntegerInstance(Locale.forLanguageTag("id-ID")).format(it) } ?: "-"
}
