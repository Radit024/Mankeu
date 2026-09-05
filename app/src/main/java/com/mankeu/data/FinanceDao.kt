package com.mankeu.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class ObligationBalanceRow(
    val id: String,
    val bookId: String,
    val kind: String,
    val sourceEventId: String,
    val counterparty: String,
    val description: String,
    val issueDateEpochDay: Long,
    val dueDateEpochDay: Long?,
    val originalAmountRupiah: Long,
    val reversed: Boolean,
    val settledAmountRupiah: Long,
)

data class PostingWithEventRow(
    val eventId: String,
    val cashFlowRole: String,
    val account: String,
    val side: String,
    val amountRupiah: Long,
)

@Dao
interface FinanceDao {
    @Query("SELECT * FROM businesses WHERE archived = 0 ORDER BY createdAtEpochMillis")
    fun observeBusinesses(): Flow<List<BusinessEntity>>

    @Query("SELECT * FROM businesses WHERE archived = 0 ORDER BY createdAtEpochMillis")
    suspend fun getBusinesses(): List<BusinessEntity>

    @Query("SELECT * FROM businesses WHERE id = :id LIMIT 1")
    suspend fun getBusiness(id: String): BusinessEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertBusiness(business: BusinessEntity)

    @Update
    suspend fun updateBusiness(business: BusinessEntity)

    @Query("SELECT COUNT(*) FROM financial_events WHERE projectId = :projectId")
    suspend fun countEventsForProject(projectId: String): Int

    @Query("SELECT COUNT(*) FROM budget_items WHERE bookId IN (SELECT id FROM books WHERE projectId = :projectId)")
    suspend fun countBudgetItemsForProject(projectId: String): Int

    /** Books, budget items, events and journals hang off these by CASCADE. */
    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProject(id: String)

    @Query("DELETE FROM businesses WHERE id = :id")
    suspend fun deleteBusiness(id: String)

    @Query("UPDATE businesses SET archived = 1, updatedAtEpochMillis = :updatedAt WHERE id = :id")
    suspend fun archiveBusiness(id: String, updatedAt: Long)

    /** Archiving a business takes its fields with it, so neither can be reached without the other. */
    @Query("UPDATE projects SET archived = 1, updatedAtEpochMillis = :updatedAt WHERE businessId = :businessId")
    suspend fun archiveProjectsOfBusiness(businessId: String, updatedAt: Long)

    @Query("UPDATE projects SET archived = 1, updatedAtEpochMillis = :updatedAt WHERE id = :id")
    suspend fun archiveProject(id: String, updatedAt: Long)

    @Query("SELECT * FROM projects WHERE businessId = :businessId AND archived = 0 ORDER BY isGeneral DESC, createdAtEpochMillis")
    fun observeProjects(businessId: String): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE businessId = :businessId AND archived = 0 ORDER BY isGeneral DESC, createdAtEpochMillis")
    suspend fun getProjects(businessId: String): List<ProjectEntity>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProject(id: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProject(project: ProjectEntity)

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertBooks(books: List<BookEntity>)

    @Query("SELECT * FROM books WHERE projectId = :projectId ORDER BY mode")
    fun observeBooks(projectId: String): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE projectId = :projectId ORDER BY mode")
    suspend fun getBooks(projectId: String): List<BookEntity>

    @Query("SELECT * FROM books WHERE projectId = :projectId AND mode = :mode LIMIT 1")
    suspend fun getBook(projectId: String, mode: String): BookEntity?

    @Query("SELECT * FROM books WHERE id = :id LIMIT 1")
    suspend fun getBookById(id: String): BookEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUnit(unit: UnitEntity)

    @Query("UPDATE units SET active = 0 WHERE id = :id")
    suspend fun archiveUnit(id: String)

    @Query("SELECT * FROM units WHERE businessId = :businessId AND active = 1 ORDER BY name COLLATE NOCASE")
    fun observeUnits(businessId: String): Flow<List<UnitEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCategory(category: CategoryEntity)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Query("UPDATE categories SET active = 0 WHERE id = :id")
    suspend fun archiveCategory(id: String)

    @Query("SELECT * FROM categories WHERE businessId = :businessId AND active = 1 ORDER BY role, name COLLATE NOCASE")
    fun observeCategories(businessId: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE businessId = :businessId AND active = 1 ORDER BY role, name COLLATE NOCASE")
    suspend fun getCategories(businessId: String): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getCategory(id: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertBudgetItem(item: BudgetItemEntity)

    @Update
    suspend fun updateBudgetItem(item: BudgetItemEntity)

    @Query("SELECT * FROM budget_items WHERE id = :id LIMIT 1")
    suspend fun getBudgetItem(id: String): BudgetItemEntity?

    @Query("SELECT * FROM budget_items WHERE bookId = :bookId AND archived = 0 ORDER BY sortOrder, createdAtEpochMillis")
    fun observeBudgetItems(bookId: String): Flow<List<BudgetItemEntity>>

    @Query("SELECT * FROM budget_items WHERE bookId = :bookId AND archived = 0 ORDER BY sortOrder, createdAtEpochMillis")
    suspend fun getBudgetItems(bookId: String): List<BudgetItemEntity>

    @Query("UPDATE budget_items SET archived = 1, updatedAtEpochMillis = :updatedAt WHERE id = :id")
    suspend fun archiveBudgetItem(id: String, updatedAt: Long)

    @Query("SELECT * FROM budget_items WHERE importBatchId = :batchId AND archived = 0")
    suspend fun getBudgetItemsForImportBatch(batchId: String): List<BudgetItemEntity>

    @Query("SELECT * FROM financial_events WHERE businessId = :businessId AND projectId = :projectId ORDER BY dateEpochDay DESC, createdAtEpochMillis DESC")
    fun observeEvents(businessId: String, projectId: String): Flow<List<FinancialEventEntity>>

    @Query("SELECT * FROM financial_events WHERE bookId = :bookId ORDER BY dateEpochDay DESC, createdAtEpochMillis DESC")
    fun observeEventsForBook(bookId: String): Flow<List<FinancialEventEntity>>

    @Query("SELECT * FROM financial_events WHERE bookId = :bookId ORDER BY dateEpochDay, createdAtEpochMillis")
    suspend fun getEventsForBook(bookId: String): List<FinancialEventEntity>

    @Query("SELECT * FROM financial_events WHERE id = :id")
    suspend fun getEvent(id: String): FinancialEventEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEvent(event: FinancialEventEntity)

    @Query("UPDATE financial_events SET status = 'REVERSED', reversedByEventId = :reversalEventId WHERE id = :eventId AND reversedByEventId IS NULL")
    suspend fun markEventReversed(eventId: String, reversalEventId: String): Int

    @Query("DELETE FROM financial_events WHERE id = :id")
    suspend fun deleteEvent(id: String)

    @Query("UPDATE financial_events SET status = 'POSTED', reversedByEventId = NULL WHERE reversedByEventId = :id")
    suspend fun clearReversalReference(id: String)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertJournal(entry: JournalEntryEntity)

    @Query("SELECT * FROM journal_entries WHERE sourceEventId = :eventId LIMIT 1")
    suspend fun getJournalForEvent(eventId: String): JournalEntryEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPostings(postings: List<PostingEntity>)

    @Query("SELECT * FROM postings WHERE journalId = :journalId ORDER BY id")
    suspend fun getPostings(journalId: String): List<PostingEntity>

    @Query(
        """
        SELECT e.id AS eventId, e.cashFlowRole, p.account, p.side, p.amountRupiah
        FROM financial_events e
        INNER JOIN journal_entries j ON j.sourceEventId = e.id
        INNER JOIN postings p ON p.journalId = j.id
        WHERE e.bookId = :bookId
        ORDER BY e.dateEpochDay, e.createdAtEpochMillis, p.id
        """,
    )
    fun observePostingRows(bookId: String): Flow<List<PostingWithEventRow>>

    @Query(
        """
        SELECT e.id AS eventId, e.cashFlowRole, p.account, p.side, p.amountRupiah
        FROM financial_events e
        INNER JOIN journal_entries j ON j.sourceEventId = e.id
        INNER JOIN postings p ON p.journalId = j.id
        WHERE e.bookId = :bookId
        ORDER BY e.dateEpochDay, e.createdAtEpochMillis, p.id
        """,
    )
    suspend fun getPostingRows(bookId: String): List<PostingWithEventRow>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertObligation(obligation: ObligationEntity)

    @Query("SELECT * FROM obligations WHERE id = :id")
    suspend fun getObligation(id: String): ObligationEntity?

    @Query(
        """
        SELECT o.id, o.bookId, o.kind, o.sourceEventId, o.counterparty, o.description,
               o.issueDateEpochDay, o.dueDateEpochDay, o.originalAmountRupiah, o.reversed,
               COALESCE(SUM(CASE WHEN se.reversedByEventId IS NULL THEN s.amountRupiah ELSE 0 END), 0) AS settledAmountRupiah
        FROM obligations o
        LEFT JOIN settlements s ON s.obligationId = o.id
        LEFT JOIN financial_events se ON se.id = s.eventId
        WHERE o.bookId = :bookId
        GROUP BY o.id
        ORDER BY o.reversed ASC, o.dueDateEpochDay IS NULL, o.dueDateEpochDay, o.createdAtEpochMillis DESC
        """,
    )
    fun observeObligationBalances(bookId: String): Flow<List<ObligationBalanceRow>>

    @Query(
        """
        SELECT o.id, o.bookId, o.kind, o.sourceEventId, o.counterparty, o.description,
               o.issueDateEpochDay, o.dueDateEpochDay, o.originalAmountRupiah, o.reversed,
               COALESCE(SUM(CASE WHEN se.reversedByEventId IS NULL THEN s.amountRupiah ELSE 0 END), 0) AS settledAmountRupiah
        FROM obligations o
        LEFT JOIN settlements s ON s.obligationId = o.id
        LEFT JOIN financial_events se ON se.id = s.eventId
        WHERE o.bookId = :bookId
        GROUP BY o.id
        ORDER BY o.reversed ASC, o.dueDateEpochDay IS NULL, o.dueDateEpochDay, o.createdAtEpochMillis DESC
        """,
    )
    suspend fun getObligationBalances(bookId: String): List<ObligationBalanceRow>

    @Query(
        """
        SELECT o.id, o.bookId, o.kind, o.sourceEventId, o.counterparty, o.description,
               o.issueDateEpochDay, o.dueDateEpochDay, o.originalAmountRupiah, o.reversed,
               COALESCE(SUM(CASE WHEN se.reversedByEventId IS NULL THEN s.amountRupiah ELSE 0 END), 0) AS settledAmountRupiah
        FROM obligations o
        LEFT JOIN settlements s ON s.obligationId = o.id
        LEFT JOIN financial_events se ON se.id = s.eventId
        WHERE o.id = :id
        GROUP BY o.id
        LIMIT 1
        """,
    )
    suspend fun getObligationBalance(id: String): ObligationBalanceRow?

    @Query("UPDATE obligations SET reversed = 1 WHERE sourceEventId = :sourceEventId")
    suspend fun markObligationReversedBySource(sourceEventId: String)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSettlement(settlement: SettlementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAssumptions(assumptions: ScenarioAssumptionEntity)

    @Query("SELECT * FROM scenario_assumptions WHERE bookId = :bookId LIMIT 1")
    fun observeAssumptions(bookId: String): Flow<ScenarioAssumptionEntity?>

    @Query("SELECT * FROM scenario_assumptions WHERE bookId = :bookId LIMIT 1")
    suspend fun getAssumptions(bookId: String): ScenarioAssumptionEntity?

    @Query("SELECT * FROM import_batches WHERE projectId = :projectId ORDER BY committedAtEpochMillis")
    suspend fun getImportBatches(projectId: String): List<ImportBatchEntity>

    @Query("SELECT * FROM import_batches WHERE projectId = :projectId ORDER BY committedAtEpochMillis DESC")
    fun observeImportBatches(projectId: String): Flow<List<ImportBatchEntity>>

    @Query("SELECT * FROM financial_events WHERE importBatchId = :batchId AND status = 'POSTED' AND reversedByEventId IS NULL ORDER BY dateEpochDay DESC, createdAtEpochMillis DESC")
    suspend fun getActiveEventsForImportBatch(batchId: String): List<FinancialEventEntity>

    @Query("SELECT n.* FROM import_notices n INNER JOIN import_batches b ON b.id = n.batchId WHERE b.projectId = :projectId ORDER BY n.createdAtEpochMillis")
    suspend fun getImportNotices(projectId: String): List<ImportNoticeEntity>

    @Query("SELECT p.* FROM provenance_records p INNER JOIN import_batches b ON b.id = p.batchId WHERE b.projectId = :projectId ORDER BY p.batchId, p.id")
    suspend fun getProvenance(projectId: String): List<ProvenanceEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertImportBatch(batch: ImportBatchEntity)

    @Query("UPDATE import_batches SET state = 'REVERSED', reversedAtEpochMillis = :reversedAt WHERE id = :batchId AND state = 'COMMITTED'")
    suspend fun markImportBatchReversed(batchId: String, reversedAt: Long): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertNotice(notice: ImportNoticeEntity)

    @Query("UPDATE import_notices SET reviewed = 1 WHERE id = :id")
    suspend fun markNoticeReviewed(id: String)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProvenance(provenance: ProvenanceEntity)

    @Query("SELECT * FROM import_notices ORDER BY reviewed ASC, createdAtEpochMillis DESC")
    fun observeNotices(): Flow<List<ImportNoticeEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM import_batches WHERE sourceSha256 = :sourceSha256 AND state = 'COMMITTED')")
    suspend fun hasImported(sourceSha256: String): Boolean
}
