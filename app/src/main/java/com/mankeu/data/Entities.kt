package com.mankeu.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "businesses")
data class BusinessEntity(
    @PrimaryKey val id: String,
    val name: String,
    val currency: String = "IDR",
    val archived: Boolean = false,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long = createdAtEpochMillis,
)

@Entity(
    tableName = "projects",
    foreignKeys = [ForeignKey(
        entity = BusinessEntity::class,
        parentColumns = ["id"],
        childColumns = ["businessId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("businessId")],
)
data class ProjectEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val name: String,
    val isGeneral: Boolean,
    val archived: Boolean = false,
    val createdAtEpochMillis: Long,
    val description: String = "",
    val startDateEpochDay: Long? = null,
    val endDateEpochDay: Long? = null,
    val updatedAtEpochMillis: Long = createdAtEpochMillis,
)

@Entity(
    tableName = "books",
    foreignKeys = [ForeignKey(ProjectEntity::class, ["id"], ["projectId"], ForeignKey.CASCADE)],
    indices = [Index("projectId"), Index(value = ["projectId", "mode"], unique = true)],
)
data class BookEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val mode: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long = createdAtEpochMillis,
)

@Entity(
    tableName = "categories",
    foreignKeys = [ForeignKey(BusinessEntity::class, ["id"], ["businessId"], ForeignKey.CASCADE)],
    indices = [
        Index("businessId"),
        Index(value = ["businessId", "role", "normalizedName"], unique = true),
    ],
)
data class CategoryEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val role: String,
    val name: String,
    val normalizedName: String,
    val aliases: String = "",
    val templateOrigin: String? = null,
    val active: Boolean = true,
    val createdAtEpochMillis: Long,
)

@Entity(
    tableName = "budget_items",
    foreignKeys = [
        ForeignKey(BookEntity::class, ["id"], ["bookId"], ForeignKey.CASCADE),
        ForeignKey(CategoryEntity::class, ["id"], ["categoryId"], ForeignKey.RESTRICT),
    ],
    indices = [Index("bookId"), Index("categoryId"), Index(value = ["bookId", "sortOrder"])],
)
data class BudgetItemEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val categoryId: String,
    val name: String,
    val normalizedName: String,
    val quantityText: String? = null,
    val unit: String? = null,
    val unitPriceRupiah: Long? = null,
    val plannedTotalRupiah: Long,
    val plannedDateEpochDay: Long? = null,
    val aliases: String = "",
    val amountSource: String = "USER_TOTAL",
    val formulaDifferenceRupiah: Long = 0,
    val sortOrder: Int,
    val archived: Boolean = false,
    val importBatchId: String? = null,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long = createdAtEpochMillis,
)

@Entity(
    tableName = "financial_events",
    foreignKeys = [
        ForeignKey(BusinessEntity::class, ["id"], ["businessId"], ForeignKey.CASCADE),
        ForeignKey(ProjectEntity::class, ["id"], ["projectId"], ForeignKey.CASCADE),
        ForeignKey(BookEntity::class, ["id"], ["bookId"], ForeignKey.CASCADE),
    ],
    indices = [
        Index("businessId"),
        Index("projectId"),
        Index("bookId"),
        Index("categoryId"),
        Index("budgetItemId"),
        Index("obligationId"),
        Index("importBatchId"),
        Index("reversesEventId"),
    ],
)
data class FinancialEventEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val projectId: String,
    val bookId: String,
    val type: String,
    val amountRupiah: Long,
    val dateEpochDay: Long,
    val description: String,
    val category: String,
    val categoryId: String? = null,
    val counterparty: String? = null,
    val reference: String? = null,
    val dueDateEpochDay: Long? = null,
    val quantityText: String? = null,
    val unit: String? = null,
    val unitPriceRupiah: Long? = null,
    val budgetItemId: String? = null,
    val obligationId: String? = null,
    val importBatchId: String? = null,
    val cashFlowRole: String,
    val status: String = "POSTED",
    val reversesEventId: String? = null,
    val reversedByEventId: String? = null,
    val createdAtEpochMillis: Long,
)

@Entity(
    tableName = "journal_entries",
    foreignKeys = [ForeignKey(FinancialEventEntity::class, ["id"], ["sourceEventId"], ForeignKey.CASCADE)],
    indices = [Index(value = ["sourceEventId"], unique = true), Index("reversalOfJournalId")],
)
data class JournalEntryEntity(
    @PrimaryKey val id: String,
    val sourceEventId: String,
    val dateEpochDay: Long,
    val description: String,
    val reversalOfJournalId: String? = null,
)

@Entity(
    tableName = "postings",
    foreignKeys = [ForeignKey(JournalEntryEntity::class, ["id"], ["journalId"], ForeignKey.CASCADE)],
    indices = [Index("journalId"), Index("account")],
)
data class PostingEntity(
    @PrimaryKey val id: String,
    val journalId: String,
    val account: String,
    val side: String,
    val amountRupiah: Long,
)

@Entity(
    tableName = "obligations",
    foreignKeys = [
        ForeignKey(BookEntity::class, ["id"], ["bookId"], ForeignKey.CASCADE),
        ForeignKey(FinancialEventEntity::class, ["id"], ["sourceEventId"], ForeignKey.CASCADE),
    ],
    indices = [Index("bookId"), Index(value = ["sourceEventId"], unique = true), Index("dueDateEpochDay")],
)
data class ObligationEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val kind: String,
    val sourceEventId: String,
    val counterparty: String,
    val description: String,
    val issueDateEpochDay: Long,
    val dueDateEpochDay: Long? = null,
    val originalAmountRupiah: Long,
    val reversed: Boolean = false,
    val createdAtEpochMillis: Long,
)

@Entity(
    tableName = "settlements",
    foreignKeys = [
        ForeignKey(ObligationEntity::class, ["id"], ["obligationId"], ForeignKey.CASCADE),
        ForeignKey(FinancialEventEntity::class, ["id"], ["eventId"], ForeignKey.CASCADE),
    ],
    indices = [Index("obligationId"), Index(value = ["eventId"], unique = true)],
)
data class SettlementEntity(
    @PrimaryKey val id: String,
    val obligationId: String,
    val eventId: String,
    val amountRupiah: Long,
    val createdAtEpochMillis: Long,
)

@Entity(
    tableName = "scenario_assumptions",
    foreignKeys = [ForeignKey(BookEntity::class, ["id"], ["bookId"], ForeignKey.CASCADE)],
    indices = [Index(value = ["bookId"], unique = true)],
)
data class ScenarioAssumptionEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val openingCashRupiah: Long = 0,
    val ownerCapitalRupiah: Long = 0,
    val loanAmountRupiah: Long = 0,
    val interestRateBasisPoints: Int = 0,
    val loanDisbursementEpochDay: Long? = null,
    val loanPaymentEpochDay: Long? = null,
    val otherFinancingCostRupiah: Long = 0,
    val outputQuantityText: String? = null,
    val outputUnit: String? = null,
    val outputPriceRupiah: Long? = null,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "import_batches",
    // Not unique: a reversed batch keeps its row as audit history, and re-importing the same file
    // afterwards (typically into the other book) is a legitimate correction. `hasImported` is the
    // duplicate guard, and it only counts batches still COMMITTED (D-076).
    indices = [Index("sourceSha256"), Index("bookId")],
)
data class ImportBatchEntity(
    @PrimaryKey val id: String,
    val sourceName: String,
    val sourceSha256: String,
    val importerVersion: String,
    val state: String,
    val committedAtEpochMillis: Long,
    val businessId: String? = null,
    val projectId: String? = null,
    val bookId: String? = null,
    val mappingVersion: String? = null,
    val recordCount: Int = 0,
    val totalAmountRupiah: Long = 0,
    val reversedAtEpochMillis: Long? = null,
)

@Entity(
    tableName = "import_notices",
    foreignKeys = [ForeignKey(ImportBatchEntity::class, ["id"], ["batchId"], ForeignKey.CASCADE)],
    indices = [Index("batchId")],
)
data class ImportNoticeEntity(
    @PrimaryKey val id: String,
    val batchId: String,
    val severity: String,
    val title: String,
    val explanation: String,
    val amountRupiah: Long?,
    val sourceEvidence: String,
    val reviewed: Boolean = false,
    val action: String? = null,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "provenance_records",
    foreignKeys = [ForeignKey(ImportBatchEntity::class, ["id"], ["batchId"], ForeignKey.CASCADE)],
    indices = [Index("batchId"), Index("targetEventId"), Index("targetBudgetItemId")],
)
data class ProvenanceEntity(
    @PrimaryKey val id: String,
    val batchId: String,
    val targetEventId: String? = null,
    val targetBudgetItemId: String? = null,
    val targetField: String? = null,
    val classification: String,
    val sourceEvidence: String,
    val sourceSheet: String? = null,
    val sourceCell: String? = null,
    val sourceValue: String? = null,
    val rationale: String,
    val inferredFields: String,
    val confidenceBasisPoints: Int? = null,
    val ruleVersion: String = "v2",
)
