package com.mankeu.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Schema migrations against real old databases.
 *
 * Schema 7 → 8 on a database that holds both cash and legacy non-cash rows: cash survives intact,
 * every credit/loan row goes with its reversal, journal and postings, a notice says so, category
 * roles are remapped, and sync change capture is live afterwards (DEC-011, DEC-013).
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test
    fun migrate7To8PurgesNonCashKeepsCashAndStartsChangeCapture() {
        helper.createDatabase(DB, 7).apply {
            execSQL("INSERT INTO businesses VALUES ('b', 'Warung', 'IDR', 0, 0, 0)")
            execSQL("INSERT INTO projects(id, businessId, name, isGeneral, archived, createdAtEpochMillis, description, updatedAtEpochMillis) VALUES ('p', 'b', 'Utama', 0, 0, 0, '', 0)")
            execSQL("INSERT INTO books(id, projectId, mode, createdAtEpochMillis, updatedAtEpochMillis) VALUES ('k', 'p', 'REALIZATION', 0, 0)")
            category("c-modal", "EQUITY", "Modal Sendiri", "modal sendiri")
            category("c-pinjam", "LIABILITY", "Pinjaman Usaha", "pinjaman usaha")
            category("c-bunga", "EXPENSE", "Biaya Bunga Pinjaman", "biaya bunga pinjaman")
            event("cash", "CASH_INCOME", "OPERATING", 1_000_000, posts = listOf("CASH" to "DEBIT", "REVENUE" to "CREDIT"))
            event("credit", "CREDIT_SALE", "NON_CASH", 500_000, posts = listOf("ACCOUNTS_RECEIVABLE" to "DEBIT", "REVENUE" to "CREDIT"))
            event("credit-rev", "REVERSAL", "NON_CASH", 500_000, reverses = "credit", posts = listOf("ACCOUNTS_RECEIVABLE" to "CREDIT", "REVENUE" to "DEBIT"))
            event("loan", "LOAN_RECEIVED", "FINANCING", 2_000_000, posts = listOf("CASH" to "DEBIT", "LOAN_PAYABLE" to "CREDIT"))
            execSQL("INSERT INTO scenario_assumptions(id, bookId, openingCashRupiah, ownerCapitalRupiah, otherFinancingCostRupiah, updatedAtEpochMillis) VALUES ('a', 'k', 0, 0, 400000, 0)")
            close()
        }

        val db = helper.runMigrationsAndValidate(DB, 8, true, AppDatabase.MIGRATION_7_8)

        assertEquals(listOf("cash"), db.strings("SELECT id FROM financial_events"))
        assertEquals(listOf("journal-cash", "journal-cash"), db.strings("SELECT journalId FROM postings ORDER BY id"))
        assertEquals(1, db.count("SELECT COUNT(*) FROM import_notices WHERE batchId IS NULL AND severity = 'SYSTEM'"))
        assertEquals(listOf("CAPITAL_IN"), db.strings("SELECT role FROM categories WHERE id = 'c-modal'"))
        assertEquals(listOf("0"), db.strings("SELECT active FROM categories WHERE id IN ('c-pinjam')"))
        assertEquals(listOf("0"), db.strings("SELECT active FROM categories WHERE id IN ('c-bunga')"))
        assertNotNull(db.strings("SELECT value FROM sync_meta WHERE `key` = 'device_id'").singleOrNull())
        assertEquals(listOf("1"), db.strings("SELECT value FROM sync_meta WHERE `key` = 'requires_full_snapshot'"))

        // Change capture is live: any write now lands in the outbox.
        db.execSQL("UPDATE businesses SET name = 'Warung Baru' WHERE id = 'b'")
        assertEquals(listOf("businesses|b|UPSERT"), db.strings("SELECT tableName || '|' || rowId || '|' || op FROM sync_outbox"))
        assertTrue(db.strings("PRAGMA foreign_key_check").isEmpty())
    }

    /**
     * Schema 8 → 9 (DEC-014): the old four-number calculator of a Proyeksi book becomes a product
     * with a recipe line, a monthly fixed plan line and a capital line; opening cash is only named
     * in the notice; basic expense categories get their default cost nature; the new rows are in
     * the sync outbox.
     */
    @Test
    fun migrate8To9TurnsTheOldCalculatorIntoABusinessPlan() {
        helper.createDatabase(DB, 8).apply {
            execSQL("INSERT INTO businesses VALUES ('b', 'Kedai', 'IDR', 0, 0, 0)")
            execSQL("INSERT INTO projects(id, businessId, name, isGeneral, archived, createdAtEpochMillis, description, updatedAtEpochMillis) VALUES ('p', 'b', 'Utama', 0, 0, 0, '', 0)")
            execSQL("INSERT INTO books(id, projectId, mode, createdAtEpochMillis, updatedAtEpochMillis) VALUES ('plan', 'p', 'PROJECTION', 0, 0)")
            category("c-bahan", "EXPENSE", "Bahan Baku & Stok", "bahan baku stok")
            category("c-gaji", "EXPENSE", "Gaji & Upah", "gaji upah")
            execSQL(
                "INSERT INTO scenario_assumptions(id, bookId, openingCashRupiah, ownerCapitalRupiah, outputQuantityText, outputUnit, outputPriceRupiah, fixedCostRupiah, variableCostPerUnitRupiah, updatedAtEpochMillis) " +
                    "VALUES ('a', 'plan', 1000000, 5000000, '700', 'cup', 10000, 2000000, 5000, 0)",
            )
            execSQL("DELETE FROM sync_outbox")
            close()
        }

        val db = helper.runMigrationsAndValidate(DB, 9, true, AppDatabase.MIGRATION_8_9)

        assertEquals(listOf("Produk utama|cup|10000|700"), db.strings("SELECT name || '|' || unit || '|' || priceRupiah || '|' || targetPerMonthText FROM products"))
        assertEquals(listOf("5000|1"), db.strings("SELECT amountRupiah || '|' || yieldText FROM product_cost_components"))
        assertEquals(
            listOf("CAPITAL_IN|5000000|ONCE|", "EXPENSE|2000000|MONTHLY|FIXED"),
            db.strings(
                "SELECT c.role || '|' || i.plannedTotalRupiah || '|' || i.recurrence || '|' || COALESCE(i.costNature, '') " +
                    "FROM budget_items i JOIN categories c ON c.id = i.categoryId ORDER BY c.role",
            ),
        )
        assertEquals(listOf("VARIABLE", "FIXED"), db.strings("SELECT costNature FROM categories WHERE id IN ('c-bahan', 'c-gaji') ORDER BY id"))
        assertTrue(db.strings("SELECT explanation FROM import_notices WHERE severity = 'SYSTEM'").single().contains("Saldo kas awal Rp1.000.000"))
        assertEquals(0, db.count("SELECT COUNT(*) FROM sqlite_master WHERE name = 'scenario_assumptions'"))
        assertEquals(0, db.count("SELECT COUNT(*) FROM financial_events"))
        assertEquals(1, db.count("SELECT COUNT(*) FROM sync_outbox WHERE tableName = 'products'"))
        assertTrue(db.strings("PRAGMA foreign_key_check").isEmpty())
    }

    private fun androidx.sqlite.db.SupportSQLiteDatabase.category(id: String, role: String, name: String, normalized: String) =
        execSQL("INSERT INTO categories(id, businessId, role, name, normalizedName, aliases, active, createdAtEpochMillis) VALUES ('$id', 'b', '$role', '$name', '$normalized', '', 1, 0)")

    private fun androidx.sqlite.db.SupportSQLiteDatabase.event(
        id: String,
        type: String,
        role: String,
        amount: Long,
        reverses: String? = null,
        posts: List<Pair<String, String>>,
    ) {
        execSQL(
            "INSERT INTO financial_events(id, businessId, projectId, bookId, type, amountRupiah, dateEpochDay, description, category, cashFlowRole, status, reversesEventId, createdAtEpochMillis) " +
                "VALUES ('$id', 'b', 'p', 'k', '$type', $amount, 20000, '$id', 'x', '$role', 'POSTED', ${reverses?.let { "'$it'" } ?: "NULL"}, 0)",
        )
        execSQL("INSERT INTO journal_entries(id, sourceEventId, dateEpochDay, description) VALUES ('journal-$id', '$id', 20000, '$id')")
        posts.forEachIndexed { index, (account, side) ->
            execSQL("INSERT INTO postings(id, journalId, account, side, amountRupiah) VALUES ('$id-$index', 'journal-$id', '$account', '$side', $amount)")
        }
    }

    private fun androidx.sqlite.db.SupportSQLiteDatabase.strings(sql: String): List<String> =
        query(sql).use { c -> buildList { while (c.moveToNext()) add(c.getString(0)) } }

    private fun androidx.sqlite.db.SupportSQLiteDatabase.count(sql: String): Int = query(sql).use { it.moveToFirst(); it.getInt(0) }

    private companion object {
        const val DB = "migration-test.db"
    }
}
