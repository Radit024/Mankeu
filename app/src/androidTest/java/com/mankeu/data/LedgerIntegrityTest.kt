package com.mankeu.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mankeu.backup.BackupTable
import com.mankeu.backup.BackupValue
import com.mankeu.backup.EncryptedBackupManager
import com.mankeu.backup.PasswordBackupCipher
import com.mankeu.backup.PortableBackupCodec
import com.mankeu.domain.BookMode
import com.mankeu.domain.CashFlowRole
import com.mankeu.domain.CategoryRole
import com.mankeu.domain.FinancialEventType
import com.mankeu.domain.FinancialEventType.CASH_EXPENSE
import com.mankeu.domain.FinancialEventType.CASH_INCOME
import com.mankeu.domain.FinancialEventType.CREDIT_SALE
import com.mankeu.domain.FinancialEventType.RECEIVABLE_COLLECTION
import com.mankeu.domain.LedgerAccount
import com.mankeu.domain.ObligationBalance
import com.mankeu.domain.ObligationKind
import com.mankeu.domain.PostedRecord
import com.mankeu.domain.PostingReportCalculator
import com.mankeu.domain.PostingSide
import com.mankeu.domain.RecordStatus
import com.mankeu.importer.ImportCandidate
import com.mankeu.importer.ImportClassification
import com.mankeu.importer.WorkbookImportPreview
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate

/**
 * One regression per ledger-integrity defect fixed in D-076..D-079. Each asserts the balances a
 * report would show afterwards, not just that a call returned: every one of these bugs "succeeded"
 * and left the books wrong.
 */
@RunWith(AndroidJUnit4::class)
class LedgerIntegrityTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: FinanceRepository
    private lateinit var businessId: String
    private lateinit var projectId: String
    private lateinit var bookId: String
    private val dao get() = database.financeDao()

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = FinanceRepository(database)
        runBlocking {
            businessId = repository.createBusiness("Warung Uji")
            projectId = repository.createProject(businessId, "Siklus Uji")
            bookId = dao.getBooks(projectId).first { it.mode == BookMode.REALIZATION.name }.id
        }
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun deletingCreditSaleWithStandingPaymentIsRefused() = runBlocking {
        val sale = record(CREDIT_SALE, 500_000, counterparty = "Bu Ani")
        repository.settleObligation(obligationOf(sale), 200_000, DAY, "")

        assertRefused { repository.deleteEvent(sale) }
        assertEquals(200_000, summary().cashBalanceRupiah)
        assertEquals(300_000, summary().receivableOutstandingRupiah)
    }

    @Test
    fun deletingReversedEventTakesItsReversalWithIt() = runBlocking {
        val income = record(CASH_INCOME, 100_000)
        repository.reverseEvent(income, DAY, "Salah catat")

        repository.deleteEvent(income)

        assertEquals(0, dao.getEventsForBook(bookId).size)
        assertEquals(0, summary().cashBalanceRupiah)
    }

    @Test
    fun deletingReversalRestoresCreditSaleAndItsReceivable() = runBlocking {
        val sale = record(CREDIT_SALE, 500_000, counterparty = "Bu Ani")
        val reversal = repository.reverseEvent(sale, DAY, "Batal")
        assertEquals(0, summary().receivableOutstandingRupiah)

        repository.deleteEvent(reversal)

        assertEquals(RecordStatus.POSTED.name, dao.getEvent(sale)!!.status)
        assertEquals(500_000, summary().operatingIncomeRupiah)
        assertEquals(500_000, summary().receivableOutstandingRupiah)
    }

    @Test
    fun restoringPaymentThatNoLongerFitsIsRefused() = runBlocking {
        val obligation = obligationOf(record(CREDIT_SALE, 500_000, counterparty = "Pak Budi"))
        val firstPayment = repository.settleObligation(obligation, 500_000, DAY, "")
        val firstPaymentReversal = repository.reverseEvent(firstPayment, DAY, "Salah")
        repository.settleObligation(obligation, 500_000, DAY, "")

        assertRefused { repository.deleteEvent(firstPaymentReversal) }
        assertEquals(500_000, summary().cashBalanceRupiah)
        assertEquals(0, summary().receivableOutstandingRupiah)
    }

    @Test
    fun reversingAReversalIsRefused() = runBlocking {
        val reversal = repository.reverseEvent(record(CASH_INCOME, 100_000), DAY, "Salah")

        assertRefused { repository.reverseEvent(reversal, DAY, "") }
        assertEquals(0, summary().cashBalanceRupiah)
    }

    @Test
    fun paymentsAndReversalsCannotBeRecordedAsPlainTransactions() = runBlocking {
        assertRefused { record(RECEIVABLE_COLLECTION, 10_000) }
        assertRefused { record(FinancialEventType.REVERSAL, 10_000) }
        assertEquals(0, dao.getEventsForBook(bookId).size)
    }

    @Test
    fun paymentDatedBeforeItsSaleIsRefused() = runBlocking {
        val obligation = obligationOf(record(CREDIT_SALE, 300_000, counterparty = "Bu Sri"))

        assertRefused { repository.settleObligation(obligation, 100_000, DAY.minusDays(1), "") }
        assertEquals(300_000, summary().receivableOutstandingRupiah)
    }

    @Test
    fun archivedCategoryNameCanBeAddedAgain() = runBlocking {
        val first = repository.createCategory(businessId, "Bensin Motor", CategoryRole.EXPENSE)
        repository.archiveCategory(first)

        assertEquals(first, repository.createCategory(businessId, "bensin motor", CategoryRole.EXPENSE))
        assertTrue(dao.getCategory(first)!!.active)
        assertRefused { repository.createCategory(businessId, "BENSIN MOTOR", CategoryRole.EXPENSE) }
    }

    @Test
    fun sameWorkbookCanBeImportedAgainOnlyAfterItsBatchIsReversed() = runBlocking {
        val workbook = preview("sha-again", listOf(candidate(CASH_INCOME, 90_000)))
        repository.commitImport(businessId, projectId, workbook)
        assertRefused { repository.commitImport(businessId, projectId, workbook) }

        repository.reverseImportBatch(dao.getImportBatches(projectId).single().id)
        repository.commitImport(businessId, projectId, workbook)

        assertEquals(90_000, summary().cashBalanceRupiah)
    }

    @Test
    fun batchReversalUndoesPaymentsBeforeTheirCreditSale() = runBlocking {
        repository.commitImport(
            businessId,
            projectId,
            preview(
                "sha-credit",
                listOf(
                    candidate(CREDIT_SALE, 400_000, counterparty = "Toko Maju", sourceObligationId = "o-1"),
                    candidate(RECEIVABLE_COLLECTION, 150_000, counterparty = "Toko Maju", sourceObligationId = "o-1"),
                ),
            ),
        )

        repository.reverseImportBatch(dao.getImportBatches(projectId).single().id)

        assertEquals(0, summary().cashBalanceRupiah)
        assertEquals(0, summary().operatingIncomeRupiah)
        assertEquals(0, summary().receivableOutstandingRupiah)
    }

    @Test
    fun backupTakenOnSchemaThreeRestoresIntoCurrentSchema() = runBlocking {
        record(CASH_INCOME, 750_000)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = EncryptedBackupManager(database, File(context.cacheDir, "ledger-integrity-recovery.aabk"))
        val password = "sandi-uji-123".toCharArray()
        val current = PortableBackupCodec.decode(PasswordBackupCipher.decrypt(manager.create(password), password))
        // Shaped the way v0.0.4 wrote it: schema 3, a `units` table, `categories.defaultUnitId`.
        val schemaThree = current.copy(
            schemaVersion = 3,
            tables = current.tables.map { table ->
                if (table.name != "categories") table
                else table.copy(columns = table.columns + "defaultUnitId", rows = table.rows.map { it + BackupValue.Null })
            } + BackupTable("units", listOf("id", "businessId", "name"), emptyList()),
        )
        record(CASH_EXPENSE, 50_000)

        val prepared = manager.prepareRestore(
            PasswordBackupCipher.encrypt(PortableBackupCodec.encode(schemaThree), password),
            password,
        )
        manager.restore(prepared, password)

        assertEquals(750_000, summary().cashBalanceRupiah)
    }

    private suspend fun record(type: FinancialEventType, amount: Long, counterparty: String? = null): String =
        repository.recordEvent(
            EventDraft(
                businessId = businessId,
                projectId = projectId,
                bookId = bookId,
                type = type,
                amountRupiah = amount,
                date = DAY,
                description = "Uji $type",
                category = "",
                counterparty = counterparty,
            ),
        )

    private suspend fun obligationOf(sourceEventId: String): String =
        dao.getObligationBalances(bookId).single { it.sourceEventId == sourceEventId }.id

    private suspend fun summary() = PostingReportCalculator.calculate(
        dao.getPostingRows(bookId).map {
            PostedRecord(
                it.eventId,
                CashFlowRole.valueOf(it.cashFlowRole),
                LedgerAccount.valueOf(it.account),
                PostingSide.valueOf(it.side),
                it.amountRupiah,
            )
        },
        dao.getObligationBalances(bookId).map {
            ObligationBalance(it.id, ObligationKind.valueOf(it.kind), it.originalAmountRupiah, it.settledAmountRupiah, null, it.reversed)
        },
    )

    /** Business rules reject with `require`, which rolls the whole transaction back. */
    private suspend fun assertRefused(block: suspend () -> Unit) {
        val failure = runCatching { block() }.exceptionOrNull()
        assertTrue("Expected a rule refusal, got $failure", failure is IllegalArgumentException)
    }

    private fun preview(sha: String, candidates: List<ImportCandidate>) = WorkbookImportPreview(
        sourceName = "uji.xlsx",
        sourceSha256 = sha,
        recognizedSheets = listOf("Transaksi"),
        candidates = candidates,
        warnings = emptyList(),
        errors = emptyList(),
    )

    private fun candidate(
        type: FinancialEventType,
        amount: Long,
        counterparty: String? = null,
        sourceObligationId: String? = null,
    ) = ImportCandidate(
        type = type,
        amountRupiah = amount,
        date = DAY,
        description = "Impor $type",
        category = "Penjualan",
        classification = ImportClassification.DIRECT,
        sourceEvidence = listOf("Transaksi!A2"),
        rationale = "Uji",
        counterparty = counterparty,
        sourceObligationId = sourceObligationId,
    )

    private companion object {
        val DAY: LocalDate = LocalDate.of(2026, 9, 1)
    }
}
