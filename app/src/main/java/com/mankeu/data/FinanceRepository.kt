package com.mankeu.data

import androidx.room.withTransaction
import com.mankeu.domain.BookMode
import com.mankeu.domain.BusinessSector
import com.mankeu.domain.BusinessTemplate
import com.mankeu.domain.CashFlowRole
import com.mankeu.domain.CategoryRole
import com.mankeu.domain.FinancialEventType
import com.mankeu.domain.JournalEntry
import com.mankeu.domain.LedgerAccount
import com.mankeu.domain.LedgerPolicy
import com.mankeu.domain.MoneyEvent
import com.mankeu.domain.ObligationBalance
import com.mankeu.domain.ObligationKind
import com.mankeu.domain.ObligationStatus
import com.mankeu.domain.Posting
import com.mankeu.domain.PostedRecord
import com.mankeu.domain.PostingSide
import com.mankeu.domain.PostingReportCalculator
import com.mankeu.domain.RecordStatus
import com.mankeu.domain.TransactionCalculation
import com.mankeu.exporter.FinanceExportBook
import com.mankeu.exporter.FinanceExportBundle
import com.mankeu.exporter.FinancePdfExporter
import com.mankeu.exporter.FinanceWorkbookExporter
import com.mankeu.importer.ImportClassification
import com.mankeu.importer.WorkbookImportPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.Locale
import java.util.UUID

data class EventDraft(
    val businessId: String,
    val projectId: String,
    val bookId: String,
    val type: FinancialEventType,
    val amountRupiah: Long,
    val date: LocalDate,
    val description: String,
    val category: String,
    val categoryId: String? = null,
    val counterparty: String? = null,
    val reference: String? = null,
    val dueDate: LocalDate? = null,
    val quantityText: String? = null,
    val unit: String? = null,
    val unitPriceRupiah: Long? = null,
    val budgetItemId: String? = null,
    val importBatchId: String? = null,
)

data class BudgetItemDraft(
    val bookId: String,
    val categoryId: String,
    val name: String,
    val quantityText: String? = null,
    val unit: String? = null,
    val unitPriceRupiah: Long? = null,
    val plannedTotalRupiah: Long? = null,
    val plannedDate: LocalDate? = null,
    val aliases: List<String> = emptyList(),
)

data class ObligationView(
    val id: String,
    val kind: ObligationKind,
    val counterparty: String,
    val description: String,
    val issueDate: LocalDate,
    val dueDate: LocalDate?,
    val originalAmountRupiah: Long,
    val settledAmountRupiah: Long,
    val outstandingAmountRupiah: Long,
    val status: ObligationStatus,
)

/** What [FinanceRepository.removeProject]/[FinanceRepository.removeBusiness] actually did. */
enum class ScopeRemoval { DELETED, ARCHIVED }

/** Which business/project the shell should select on launch. Nulls mean "nothing exists yet". */
data class WorkspaceSelection(val businessId: String?, val projectId: String?)

class FinanceRepository(private val database: AppDatabase) {
    private val dao = database.financeDao()

    val businesses: Flow<List<BusinessEntity>> = dao.observeBusinesses()
    val notices: Flow<List<ImportNoticeEntity>> = dao.observeNotices()

    fun projects(businessId: String): Flow<List<ProjectEntity>> = dao.observeProjects(businessId)
    fun books(projectId: String): Flow<List<BookEntity>> = dao.observeBooks(projectId)
    fun categories(businessId: String): Flow<List<CategoryEntity>> = dao.observeCategories(businessId)
    fun units(businessId: String): Flow<List<UnitEntity>> = dao.observeUnits(businessId)
    fun budgetItems(bookId: String): Flow<List<BudgetItemEntity>> = dao.observeBudgetItems(bookId)
    fun assumptions(bookId: String): Flow<ScenarioAssumptionEntity?> = dao.observeAssumptions(bookId)
    fun importBatches(projectId: String): Flow<List<ImportBatchEntity>> = dao.observeImportBatches(projectId)

    fun events(businessId: String, projectId: String): Flow<List<MoneyEvent>> =
        dao.observeEvents(businessId, projectId).map { entities -> entities.map { it.toDomain() } }

    fun eventsForBook(bookId: String): Flow<List<MoneyEvent>> =
        dao.observeEventsForBook(bookId).map { entities -> entities.map { it.toDomain() } }

    fun obligationBalances(bookId: String): Flow<List<ObligationBalance>> =
        dao.observeObligationBalances(bookId).map { rows -> rows.map { it.toDomain() } }

    fun obligations(bookId: String): Flow<List<ObligationView>> =
        dao.observeObligationBalances(bookId).map { rows ->
            rows.map { row ->
                val balance = row.toDomain()
                ObligationView(
                    id = row.id,
                    kind = balance.kind,
                    counterparty = row.counterparty,
                    description = row.description,
                    issueDate = LocalDate.ofEpochDay(row.issueDateEpochDay),
                    dueDate = balance.dueDate,
                    originalAmountRupiah = balance.originalAmountRupiah,
                    settledAmountRupiah = balance.settledAmountRupiah,
                    outstandingAmountRupiah = balance.outstandingAmountRupiah,
                    status = balance.status(LocalDate.now()),
                )
            }
        }

    fun accountingSummary(bookId: String) = combine(
        dao.observePostingRows(bookId),
        obligationBalances(bookId),
    ) { rows, obligations ->
        PostingReportCalculator.calculate(
            rows.map {
                PostedRecord(
                    eventId = it.eventId,
                    cashFlowRole = CashFlowRole.valueOf(it.cashFlowRole),
                    account = LedgerAccount.valueOf(it.account),
                    side = PostingSide.valueOf(it.side),
                    amountRupiah = it.amountRupiah,
                )
            },
            obligations,
        )
    }

    suspend fun buildProjectExportBundle(projectId: String): FinanceExportBundle = database.withTransaction {
            val project = dao.getProject(projectId) ?: error("Proyek tidak ditemukan.")
            val business = dao.getBusiness(project.businessId) ?: error("Usaha tidak ditemukan.")
            val categories = dao.getCategories(business.id)
            val exportBooks = ensureBookPair(project.id).values.map { book ->
                val obligations = dao.getObligationBalances(book.id)
                val summary = PostingReportCalculator.calculate(
                    dao.getPostingRows(book.id).map {
                        PostedRecord(
                            eventId = it.eventId,
                            cashFlowRole = CashFlowRole.valueOf(it.cashFlowRole),
                            account = LedgerAccount.valueOf(it.account),
                            side = PostingSide.valueOf(it.side),
                            amountRupiah = it.amountRupiah,
                        )
                    },
                    obligations.map { it.toDomain() },
                )
                FinanceExportBook(
                    book = book,
                    budgets = dao.getBudgetItems(book.id),
                    events = dao.getEventsForBook(book.id),
                    obligations = obligations,
                    summary = summary,
                    assumptions = dao.getAssumptions(book.id),
                )
            }
            FinanceExportBundle(
                business = business,
                project = project,
                categories = categories,
                books = exportBooks,
                importBatches = dao.getImportBatches(project.id),
                notices = dao.getImportNotices(project.id),
                provenance = dao.getProvenance(project.id),
            )
        }

    suspend fun exportProjectWorkbook(projectId: String): ByteArray =
        FinanceWorkbookExporter().export(buildProjectExportBundle(projectId))

    suspend fun exportProjectPdf(projectId: String): ByteArray =
        FinancePdfExporter().export(buildProjectExportBundle(projectId))

    /**
     * The workspace the app should open into, or nulls when the database is genuinely empty.
     *
     * Replaces `ensureStarterWorkspace()`, which used to *write* a business named "Usaha Saya" and
     * a project named "Umum" during startup so the shell always had something selected. That made
     * a first run look like an account someone else had already set up, and left the owner
     * renaming placeholders instead of naming their own farm. Deciding what exists is the owner's
     * call; this function only reads.
     *
     * The two `ensure...` calls below are deliberately kept: category/unit master data and the
     * Proyeksi/Realisasi book pair are structural rows the domain requires for a business or
     * project that the owner *did* create, not named entities that appear in the scope selectors.
     */
    suspend fun currentWorkspace(): WorkspaceSelection = database.withTransaction {
        val business = dao.getBusinesses().firstOrNull()
            ?: return@withTransaction WorkspaceSelection(null, null)
        ensureDefaultMasterData(business.id, BusinessTemplate.AGRICULTURE_TEMPLATE)
        val project = dao.getProjects(business.id).firstOrNull()
        if (project != null) ensureBookPair(project.id)
        WorkspaceSelection(business.id, project?.id)
    }

    /**
     * Creates only the business. It no longer also inserts a general "Umum" project: a field or
     * season is a real accounting scope with its own two books, and inventing one on the owner's
     * behalf is the same failure as the old starter workspace, one level down. The caller shows an
     * explicit "Tambah Lahan" step instead.
     */
    suspend fun createBusiness(
        name: String,
        sector: BusinessSector = BusinessSector.GENERAL_MSME,
    ): String = database.withTransaction {
        val cleanName = name.trim().also { require(it.isNotBlank()) { "Nama usaha wajib diisi." } }
        val now = System.currentTimeMillis()
        val business = BusinessEntity(UUID.randomUUID().toString(), cleanName, createdAtEpochMillis = now)
        dao.insertBusiness(business)
        ensureDefaultMasterData(business.id, BusinessTemplate.forSector(sector))
        business.id
    }

    suspend fun createProject(businessId: String, name: String): String = database.withTransaction {
        createProjectInternal(
            businessId = businessId,
            name = name.trim().also { require(it.isNotBlank()) { "Nama proyek wajib diisi." } },
            isGeneral = false,
        ).id
    }

    suspend fun renameBusiness(id: String, name: String) = database.withTransaction {
        val cleanName = name.trim().also { require(it.isNotBlank()) { "Nama usaha wajib diisi." } }
        val business = dao.getBusiness(id) ?: error("Usaha tidak ditemukan.")
        dao.updateBusiness(business.copy(name = cleanName, updatedAtEpochMillis = System.currentTimeMillis()))
    }

    suspend fun renameProject(id: String, name: String) = database.withTransaction {
        val cleanName = name.trim().also { require(it.isNotBlank()) { "Nama lahan wajib diisi." } }
        val project = dao.getProject(id) ?: error("Lahan tidak ditemukan.")
        dao.updateProject(project.copy(name = cleanName, updatedAtEpochMillis = System.currentTimeMillis()))
    }

    /**
     * Edits the whole field, not just its name.
     *
     * `description` and the two season dates were columns the schema has always carried and the UI
     * never wrote — a field could be named but not described or dated. The web equivalent
     * (`FinanceProject`) has always had a season label and a start/end date, so this closes a gap
     * rather than inventing a new concept.
     */
    suspend fun updateProjectDetails(
        id: String,
        name: String,
        description: String,
        startDate: LocalDate?,
        endDate: LocalDate?,
    ) = database.withTransaction {
        val cleanName = name.trim().also { require(it.isNotBlank()) { "Nama lahan wajib diisi." } }
        require(startDate == null || endDate == null || !endDate.isBefore(startDate)) {
            "Tanggal selesai tidak boleh mendahului tanggal mulai."
        }
        val project = dao.getProject(id) ?: error("Lahan tidak ditemukan.")
        dao.updateProject(
            project.copy(
                name = cleanName,
                description = description.trim(),
                startDateEpochDay = startDate?.toEpochDay(),
                endDateEpochDay = endDate?.toEpochDay(),
                updatedAtEpochMillis = System.currentTimeMillis(),
            ),
        )
    }

    /**
     * Archives, never deletes. Every posted journal entry belongs to a book under a field under a
     * business; removing rows would break the audit trail the whole product is built on. Archiving
     * hides the scope from the pickers and leaves the history intact and exportable.
     */
    suspend fun archiveBusiness(id: String) = database.withTransaction {
        require(dao.getBusinesses().size > 1) { "Usaha terakhir tidak dapat diarsipkan." }
        val now = System.currentTimeMillis()
        dao.archiveProjectsOfBusiness(id, now)
        dao.archiveBusiness(id, now)
    }

    suspend fun archiveProject(id: String) = database.withTransaction {
        val project = dao.getProject(id) ?: error("Lahan tidak ditemukan.")
        dao.archiveProject(project.id, System.currentTimeMillis())
    }

    /**
     * Removes a scope in the only way that is safe for each case, and says which it did.
     *
     * A scope the owner created by mistake and never used carries no history, so deleting it
     * outright is what they mean by "remove" and leaves nothing behind. A scope with records is a
     * different thing: every posted journal entry hangs off a book under a field under a business,
     * and deleting those rows would tear a hole in the audit trail the product is built on — so it
     * is archived instead, and the caller tells the owner that is what happened. Deciding here
     * rather than in the UI keeps the rule in one place and means no screen has to count rows to
     * know which button to draw.
     */
    suspend fun removeProject(id: String): ScopeRemoval = database.withTransaction {
        val project = dao.getProject(id) ?: error("Lahan tidak ditemukan.")
        if (projectIsEmpty(project.id)) {
            dao.deleteProject(project.id)
            ScopeRemoval.DELETED
        } else {
            dao.archiveProject(project.id, System.currentTimeMillis())
            ScopeRemoval.ARCHIVED
        }
    }

    suspend fun removeBusiness(id: String): ScopeRemoval = database.withTransaction {
        require(dao.getBusinesses().size > 1) { "Usaha terakhir tidak dapat dihapus." }
        val projects = dao.getProjects(id)
        return@withTransaction if (projects.all { projectIsEmpty(it.id) }) {
            dao.deleteBusiness(id)
            ScopeRemoval.DELETED
        } else {
            val now = System.currentTimeMillis()
            dao.archiveProjectsOfBusiness(id, now)
            dao.archiveBusiness(id, now)
            ScopeRemoval.ARCHIVED
        }
    }

    private suspend fun projectIsEmpty(projectId: String): Boolean =
        dao.countEventsForProject(projectId) == 0 && dao.countBudgetItemsForProject(projectId) == 0

    suspend fun getOrCreateBook(projectId: String, mode: BookMode): BookEntity = database.withTransaction {
        ensureBookPair(projectId).getValue(mode)
    }

    suspend fun createCategory(
        businessId: String,
        name: String,
        role: CategoryRole,
        aliases: List<String> = emptyList(),
    ): String = database.withTransaction {
        val cleanName = name.trim().also { require(it.isNotBlank()) { "Nama kategori wajib diisi." } }
        val id = UUID.randomUUID().toString()
        dao.insertCategory(
            CategoryEntity(
                id = id,
                businessId = businessId,
                role = role.name,
                name = cleanName,
                normalizedName = normalize(cleanName),
                aliases = aliases.map(String::trim).filter(String::isNotBlank).distinct().joinToString("|"),
                createdAtEpochMillis = System.currentTimeMillis(),
            ),
        )
        id
    }

    suspend fun archiveCategory(id: String) = database.withTransaction { dao.archiveCategory(id) }

    suspend fun createUnit(businessId: String, name: String, symbol: String): String = database.withTransaction {
        val cleanName = name.trim().also { require(it.isNotBlank()) { "Nama satuan wajib diisi." } }
        val cleanSymbol = symbol.trim().also { require(it.isNotBlank()) { "Simbol satuan wajib diisi." } }
        val id = UUID.randomUUID().toString()
        dao.insertUnit(
            UnitEntity(
                id = id,
                businessId = businessId,
                name = cleanName,
                symbol = cleanSymbol,
                normalizedName = normalize(cleanName),
                createdAtEpochMillis = System.currentTimeMillis(),
            ),
        )
        id
    }

    suspend fun archiveUnit(id: String) = database.withTransaction { dao.archiveUnit(id) }

    suspend fun createBudgetItem(draft: BudgetItemDraft): String = database.withTransaction {
        val cleanName = draft.name.trim().also { require(it.isNotBlank()) { "Nama anggaran wajib diisi." } }
        require(draft.plannedTotalRupiah == null || draft.plannedTotalRupiah > 0) { "Total anggaran harus lebih dari nol." }
        require(draft.unitPriceRupiah == null || draft.unitPriceRupiah >= 0) { "Harga satuan tidak boleh negatif." }

        val book = dao.getBookById(draft.bookId) ?: error("Buku keuangan tidak ditemukan.")
        val project = dao.getProject(book.projectId) ?: error("Proyek tidak ditemukan.")
        val category = dao.getCategory(draft.categoryId) ?: error("Kategori tidak ditemukan.")
        require(category.businessId == project.businessId) { "Kategori dan buku berasal dari usaha yang berbeda." }

        val formulaTotal = calculateFormulaTotal(draft.quantityText, draft.unitPriceRupiah)
        val authoritativeTotal = draft.plannedTotalRupiah ?: formulaTotal
            ?: error("Isi total anggaran atau jumlah dan harga satuan.")
        val difference = if (formulaTotal == null) 0 else authoritativeTotal - formulaTotal
        val id = UUID.randomUUID().toString()
        val sortOrder = dao.getBudgetItems(draft.bookId).size + 1
        dao.insertBudgetItem(
            BudgetItemEntity(
                id = id,
                bookId = draft.bookId,
                categoryId = draft.categoryId,
                name = cleanName,
                normalizedName = normalize(cleanName),
                quantityText = draft.quantityText?.trim()?.ifBlank { null },
                unit = draft.unit?.trim()?.ifBlank { null },
                unitPriceRupiah = draft.unitPriceRupiah,
                plannedTotalRupiah = authoritativeTotal,
                plannedDateEpochDay = draft.plannedDate?.toEpochDay(),
                aliases = draft.aliases.map(String::trim).filter(String::isNotBlank).distinct().joinToString("|"),
                amountSource = if (draft.plannedTotalRupiah != null) "USER_TOTAL" else "QUANTITY_X_UNIT_PRICE",
                formulaDifferenceRupiah = difference,
                sortOrder = sortOrder,
                createdAtEpochMillis = System.currentTimeMillis(),
            ),
        )
        id
    }

    suspend fun updateBudgetItem(id: String, draft: BudgetItemDraft) = database.withTransaction {
        val current = dao.getBudgetItem(id) ?: error("Item anggaran tidak ditemukan.")
        require(current.bookId == draft.bookId) { "Item anggaran berasal dari buku lain." }
        val cleanName = draft.name.trim().also { require(it.isNotBlank()) { "Nama anggaran wajib diisi." } }
        val category = dao.getCategory(draft.categoryId) ?: error("Kategori tidak ditemukan.")
        val book = dao.getBookById(draft.bookId) ?: error("Buku keuangan tidak ditemukan.")
        val project = dao.getProject(book.projectId) ?: error("Proyek tidak ditemukan.")
        require(category.businessId == project.businessId) { "Kategori dan buku berasal dari usaha yang berbeda." }
        val formulaTotal = calculateFormulaTotal(draft.quantityText, draft.unitPriceRupiah)
        val authoritativeTotal = draft.plannedTotalRupiah ?: formulaTotal
            ?: error("Isi total anggaran atau jumlah dan harga satuan.")
        require(authoritativeTotal > 0) { "Total anggaran harus lebih dari nol." }
        dao.updateBudgetItem(
            current.copy(
                categoryId = draft.categoryId,
                name = cleanName,
                normalizedName = normalize(cleanName),
                quantityText = draft.quantityText?.trim()?.ifBlank { null },
                unit = draft.unit?.trim()?.ifBlank { null },
                unitPriceRupiah = draft.unitPriceRupiah,
                plannedTotalRupiah = authoritativeTotal,
                plannedDateEpochDay = draft.plannedDate?.toEpochDay(),
                aliases = (current.aliases.split('|') + draft.aliases).map(String::trim).filter(String::isNotBlank).distinct().joinToString("|"),
                amountSource = if (draft.plannedTotalRupiah != null) "USER_TOTAL" else "QUANTITY_X_UNIT_PRICE",
                formulaDifferenceRupiah = if (formulaTotal == null) 0 else authoritativeTotal - formulaTotal,
                updatedAtEpochMillis = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun archiveBudgetItem(id: String) = database.withTransaction {
        dao.archiveBudgetItem(id, System.currentTimeMillis())
    }

    suspend fun saveAssumptions(assumptions: ScenarioAssumptionEntity) = database.withTransaction {
        require(assumptions.openingCashRupiah >= 0) { "Saldo kas awal tidak boleh negatif." }
        require(assumptions.ownerCapitalRupiah >= 0) { "Modal sendiri tidak boleh negatif." }
        require(assumptions.loanAmountRupiah >= 0) { "Nilai pinjaman tidak boleh negatif." }
        require(assumptions.interestRateBasisPoints >= 0) { "Bunga tidak boleh negatif." }
        require(assumptions.otherFinancingCostRupiah >= 0) { "Biaya pembiayaan tidak boleh negatif." }
        if (assumptions.loanDisbursementEpochDay != null && assumptions.loanPaymentEpochDay != null) {
            require(assumptions.loanPaymentEpochDay >= assumptions.loanDisbursementEpochDay) {
                "Tanggal pembayaran tidak boleh sebelum pencairan."
            }
        }
        dao.upsertAssumptions(assumptions.copy(updatedAtEpochMillis = System.currentTimeMillis()))
    }

    /** Compatibility entry point used by the original MVP UI. New screens pass an explicit book. */
    suspend fun recordEvent(
        businessId: String,
        projectId: String,
        type: FinancialEventType,
        amountRupiah: Long,
        date: LocalDate,
        description: String,
        category: String,
    ): String {
        val book = getOrCreateBook(projectId, BookMode.REALIZATION)
        return recordEvent(
            EventDraft(
                businessId = businessId,
                projectId = projectId,
                bookId = book.id,
                type = type,
                amountRupiah = amountRupiah,
                date = date,
                description = description,
                category = category,
            ),
        )
    }

    suspend fun recordEvent(draft: EventDraft): String = database.withTransaction {
        require(draft.amountRupiah > 0) { "Nominal harus lebih dari nol." }
        val book = dao.getBookById(draft.bookId) ?: error("Buku keuangan tidak ditemukan.")
        val project = dao.getProject(draft.projectId) ?: error("Proyek tidak ditemukan.")
        require(book.projectId == project.id && project.businessId == draft.businessId) {
            "Lingkup usaha, proyek, dan buku tidak konsisten."
        }
        if (draft.categoryId != null) {
            val category = dao.getCategory(draft.categoryId) ?: error("Kategori tidak ditemukan.")
            require(category.businessId == draft.businessId) { "Kategori berasal dari usaha yang berbeda." }
        }
        if (draft.type in CREDIT_EVENT_TYPES) {
            require(!draft.counterparty.isNullOrBlank()) { "Nama pihak wajib diisi untuk transaksi belum dibayar." }
        }
        val event = draft.toEvent()
        insertEventWithJournal(event)
        if (draft.type in CREDIT_EVENT_TYPES) {
            dao.insertObligation(
                ObligationEntity(
                    id = event.obligationId ?: error("Obligation id was not generated."),
                    bookId = event.bookId,
                    kind = if (draft.type == FinancialEventType.CREDIT_SALE) {
                        ObligationKind.RECEIVABLE.name
                    } else {
                        ObligationKind.PAYABLE.name
                    },
                    sourceEventId = event.id,
                    counterparty = draft.counterparty!!.trim(),
                    description = event.description,
                    issueDateEpochDay = event.date.toEpochDay(),
                    dueDateEpochDay = draft.dueDate?.toEpochDay(),
                    originalAmountRupiah = event.amountRupiah,
                    createdAtEpochMillis = System.currentTimeMillis(),
                ),
            )
        }
        event.id
    }

    suspend fun settleObligation(
        obligationId: String,
        amountRupiah: Long,
        date: LocalDate,
        description: String,
    ): String = database.withTransaction {
        require(amountRupiah > 0) { "Nominal pembayaran harus lebih dari nol." }
        val balance = dao.getObligationBalance(obligationId) ?: error("Piutang/utang tidak ditemukan.")
        require(!balance.reversed) { "Piutang/utang ini sudah dibatalkan." }
        val outstanding = (balance.originalAmountRupiah - balance.settledAmountRupiah).coerceAtLeast(0)
        require(outstanding > 0) { "Piutang/utang ini sudah lunas." }
        require(amountRupiah <= outstanding) { "Pembayaran melebihi sisa ${outstanding.toIdr()}." }
        val sourceEvent = dao.getEvent(balance.sourceEventId) ?: error("Transaksi sumber tidak ditemukan.")
        val eventId = UUID.randomUUID().toString()
        val event = MoneyEvent(
            id = eventId,
            businessId = sourceEvent.businessId,
            projectId = sourceEvent.projectId,
            bookId = sourceEvent.bookId,
            type = if (balance.kind == ObligationKind.RECEIVABLE.name) {
                FinancialEventType.RECEIVABLE_COLLECTION
            } else {
                FinancialEventType.PAYABLE_SETTLEMENT
            },
            amountRupiah = amountRupiah,
            date = date,
            description = description.trim().ifBlank {
                if (balance.kind == ObligationKind.RECEIVABLE.name) "Penerimaan piutang" else "Pembayaran utang"
            },
            category = sourceEvent.category,
            categoryId = sourceEvent.categoryId,
            counterparty = balance.counterparty,
            obligationId = obligationId,
        )
        insertEventWithJournal(event)
        dao.insertSettlement(
            SettlementEntity(
                id = UUID.randomUUID().toString(),
                obligationId = obligationId,
                eventId = eventId,
                amountRupiah = amountRupiah,
                createdAtEpochMillis = System.currentTimeMillis(),
            ),
        )
        eventId
    }

    suspend fun reverseEvent(eventId: String, date: LocalDate, reason: String): String = database.withTransaction {
        val originalEntity = dao.getEvent(eventId) ?: error("Transaksi tidak ditemukan.")
        require(originalEntity.status == RecordStatus.POSTED.name && originalEntity.reversedByEventId == null) {
            "Transaksi sudah dibalik."
        }
        val originalJournalEntity = dao.getJournalForEvent(eventId) ?: error("Jurnal sumber tidak ditemukan.")
        val originalPostings = dao.getPostings(originalJournalEntity.id)
        require(originalPostings.isNotEmpty()) { "Posting sumber tidak ditemukan." }

        val reversalId = UUID.randomUUID().toString()
        val reversalEvent = MoneyEvent(
            id = reversalId,
            businessId = originalEntity.businessId,
            projectId = originalEntity.projectId,
            bookId = originalEntity.bookId,
            type = FinancialEventType.REVERSAL,
            amountRupiah = originalEntity.amountRupiah,
            date = date,
            description = reason.trim().ifBlank { "Pembalikan: ${originalEntity.description}" },
            category = originalEntity.category,
            categoryId = originalEntity.categoryId,
            // A reversal must negate the source event in every report dimension,
            // including its operating/investing/financing cash-flow bucket.
            cashFlowRole = CashFlowRole.valueOf(originalEntity.cashFlowRole),
            reversesEventId = originalEntity.id,
        )
        val originalJournal = JournalEntry(
            id = originalJournalEntity.id,
            sourceEventId = originalJournalEntity.sourceEventId,
            date = LocalDate.ofEpochDay(originalJournalEntity.dateEpochDay),
            description = originalJournalEntity.description,
            postings = originalPostings.map {
                Posting(
                    account = LedgerAccount.valueOf(it.account),
                    side = PostingSide.valueOf(it.side),
                    amountRupiah = it.amountRupiah,
                )
            },
        )
        val reversalJournal = LedgerPolicy.reversalFor(originalJournal, reversalEvent)
        insertEventAndJournal(reversalEvent, reversalJournal)
        check(dao.markEventReversed(originalEntity.id, reversalId) == 1) { "Transaksi berubah sebelum pembalikan disimpan." }
        dao.markObligationReversedBySource(originalEntity.id)
        reversalId
    }

    /**
     * Hard deletes a financial transaction and its associated ledger postings.
     * If the transaction was a reversal, restores the original transaction status to POSTED.
     * Foreign keys on journal_entries, postings, obligations, and settlements CASCADE delete automatically.
     */
    suspend fun deleteEvent(eventId: String) = database.withTransaction {
        val originalEntity = dao.getEvent(eventId) ?: error("Transaksi tidak ditemukan.")
        dao.clearReversalReference(eventId)
        dao.deleteEvent(eventId)
    }

    /**
     * @param targetBook when non-null, every row lands in this book regardless of what the
     * candidate carries. Null means "the rows already know" — only true for a Mankeu export, which
     * has a sheet per book; overriding those would collapse Proyeksi and Realisasi into one.
     */
    suspend fun commitImport(
        businessId: String,
        projectId: String,
        preview: WorkbookImportPreview,
        targetBook: BookMode? = null,
    ): Int = database.withTransaction {
        require(preview.canCommit) { "Import preview is not committable." }
        require(!dao.hasImported(preview.sourceSha256)) { "Workbook ini sudah pernah diimpor." }
        val books = ensureBookPair(projectId)
        // The batch's own bookId stays the realization book: it is the batch's nominal home for
        // listing purposes. What each row actually lands in is `candidate.bookMode` below —
        // reversal keys off `importBatchId` on the rows, not off this field.
        val book = books.getValue(BookMode.REALIZATION)
        val batchId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        dao.insertImportBatch(
            ImportBatchEntity(
                id = batchId,
                sourceName = preview.sourceName,
                sourceSha256 = preview.sourceSha256,
                importerVersion = "native-2",
                state = "COMMITTED",
                committedAtEpochMillis = now,
                businessId = businessId,
                projectId = projectId,
                bookId = book.id,
                mappingVersion = "reference-five-view-v1",
                recordCount = preview.candidates.size + preview.budgetCandidates.size,
                totalAmountRupiah = preview.candidates.sumOf { it.amountRupiah } +
                    preview.budgetCandidates.sumOf { it.plannedTotalRupiah },
            ),
        )
        val importedObligations = mutableMapOf<String, String>()
        preview.candidates.forEach { candidate ->
            val destination = books.getValue(targetBook ?: candidate.bookMode)
            val isCredit = candidate.type in CREDIT_EVENT_TYPES
            val isSettlement = candidate.type in setOf(
                FinancialEventType.RECEIVABLE_COLLECTION,
                FinancialEventType.PAYABLE_SETTLEMENT,
            )
            val newObligationId = when {
                isCredit -> UUID.randomUUID().toString()
                isSettlement -> candidate.sourceObligationId?.let(importedObligations::get)
                    ?: error("Pembayaran impor tidak memiliki transaksi piutang/utang sumber yang aktif.")
                else -> null
            }
            val event = MoneyEvent(
                id = UUID.randomUUID().toString(),
                businessId = businessId,
                projectId = projectId,
                bookId = destination.id,
                type = candidate.type,
                amountRupiah = candidate.amountRupiah,
                date = candidate.date,
                description = candidate.description,
                category = candidate.category,
                counterparty = candidate.counterparty,
                reference = candidate.reference,
                dueDate = candidate.dueDate,
                obligationId = newObligationId,
                importBatchId = batchId,
            )
            insertEventWithJournal(event)
            if (isCredit) {
                val counterparty = candidate.counterparty?.trim().orEmpty()
                require(counterparty.isNotBlank()) { "Nama pihak wajib ada pada transaksi kredit impor." }
                dao.insertObligation(
                    ObligationEntity(
                        id = requireNotNull(newObligationId),
                        bookId = destination.id,
                        kind = if (candidate.type == FinancialEventType.CREDIT_SALE) ObligationKind.RECEIVABLE.name else ObligationKind.PAYABLE.name,
                        sourceEventId = event.id,
                        counterparty = counterparty,
                        description = event.description,
                        issueDateEpochDay = event.date.toEpochDay(),
                        dueDateEpochDay = candidate.dueDate?.toEpochDay(),
                        originalAmountRupiah = event.amountRupiah,
                        createdAtEpochMillis = now,
                    ),
                )
                candidate.sourceObligationId?.let { importedObligations[it] = newObligationId }
            } else if (isSettlement) {
                dao.insertSettlement(
                    SettlementEntity(
                        id = UUID.randomUUID().toString(),
                        obligationId = requireNotNull(newObligationId),
                        eventId = event.id,
                        amountRupiah = event.amountRupiah,
                        createdAtEpochMillis = now,
                    ),
                )
            }
            if (candidate.classification != ImportClassification.DIRECT) {
                val evidence = candidate.sourceEvidence.joinToString("; ")
                dao.insertProvenance(
                    ProvenanceEntity(
                        id = UUID.randomUUID().toString(),
                        batchId = batchId,
                        targetEventId = event.id,
                        classification = candidate.classification.name,
                        sourceEvidence = evidence,
                        rationale = candidate.rationale,
                        inferredFields = candidate.inferredFields.joinToString(","),
                    ),
                )
                dao.insertNotice(
                    ImportNoticeEntity(
                        id = UUID.randomUUID().toString(),
                        batchId = batchId,
                        severity = "AUTOMATIC_RECOVERY",
                        title = "${candidate.description} ${candidate.amountRupiah.toIdr()} dipulihkan dari workbook",
                        explanation = candidate.rationale + if (candidate.inferredFields.isNotEmpty()) {
                            " Atribut hasil inferensi: ${candidate.inferredFields.joinToString()}."
                        } else "",
                        amountRupiah = candidate.amountRupiah,
                        sourceEvidence = evidence,
                        action = "OPEN_PROVENANCE",
                    ),
                )
            }
        }
        // RAB lines. Imported as budget items rather than events: they post no journal and settle
        // nothing. Categories are matched by normalized name and created when absent, because a
        // workbook carries a category *name* and refusing the row would force the owner to
        // hand-recreate information the file already contains.
        val categoriesByName = dao.getCategories(businessId).associateBy { it.normalizedName }.toMutableMap()
        preview.budgetCandidates.forEach { candidate ->
            val normalized = normalize(candidate.categoryName)
            val category = categoriesByName[normalized] ?: run {
                val id = createCategory(
                    businessId = businessId,
                    name = candidate.categoryName,
                    role = CategoryRole.EXPENSE,
                    aliases = listOf(candidate.categoryName),
                )
                val created = requireNotNull(dao.getCategory(id))
                categoriesByName[normalized] = created
                created
            }
            val destination = books.getValue(targetBook ?: candidate.bookMode)
            dao.insertBudgetItem(
                BudgetItemEntity(
                    id = UUID.randomUUID().toString(),
                    bookId = destination.id,
                    categoryId = category.id,
                    name = candidate.name,
                    normalizedName = normalize(candidate.name),
                    quantityText = candidate.quantityText,
                    unit = candidate.unit,
                    unitPriceRupiah = candidate.unitPriceRupiah,
                    plannedTotalRupiah = candidate.plannedTotalRupiah,
                    plannedDateEpochDay = candidate.plannedDate?.toEpochDay(),
                    sortOrder = dao.getBudgetItems(destination.id).size + 1,
                    importBatchId = batchId,
                    createdAtEpochMillis = now,
                ),
            )
        }
        preview.candidates.size + preview.budgetCandidates.size
    }

    suspend fun reverseImportBatch(batchId: String): Int = database.withTransaction {
        val activeEvents = dao.getActiveEventsForImportBatch(batchId)
        activeEvents.forEach { event ->
            reverseEvent(
                eventId = event.id,
                date = LocalDate.now(),
                reason = "Pembalikan batch impor $batchId: ${event.description}",
            )
        }
        // Budget items have no journal to reverse, so they are archived instead. Without this the
        // batch would only be half-reversible: the transactions would neutralise but the imported
        // RAB lines would stay behind.
        val now = System.currentTimeMillis()
        val archivedBudgets = dao.getBudgetItemsForImportBatch(batchId)
        archivedBudgets.forEach { item -> dao.archiveBudgetItem(item.id, now) }
        check(dao.markImportBatchReversed(batchId, now) == 1) {
            "Batch impor tidak aktif atau telah dibalik."
        }
        activeEvents.size + archivedBudgets.size
    }

    private suspend fun createProjectInternal(businessId: String, name: String, isGeneral: Boolean): ProjectEntity {
        val now = System.currentTimeMillis()
        val project = ProjectEntity(
            id = UUID.randomUUID().toString(),
            businessId = businessId,
            name = name,
            isGeneral = isGeneral,
            createdAtEpochMillis = now,
        )
        dao.insertProject(project)
        ensureBookPair(project.id)
        return project
    }

    private suspend fun ensureBookPair(projectId: String): Map<BookMode, BookEntity> {
        val existing = dao.getBooks(projectId).associateBy { BookMode.valueOf(it.mode) }.toMutableMap()
        val now = System.currentTimeMillis()
        val missing = BookMode.entries.filterNot(existing::containsKey).map { mode ->
            BookEntity(
                id = UUID.randomUUID().toString(),
                projectId = projectId,
                mode = mode.name,
                createdAtEpochMillis = now,
            )
        }
        if (missing.isNotEmpty()) dao.insertBooks(missing)
        missing.forEach { existing[BookMode.valueOf(it.mode)] = it }
        return existing
    }

    private suspend fun ensureDefaultMasterData(
        businessId: String,
        template: BusinessTemplate = BusinessTemplate.AGRICULTURE_TEMPLATE,
    ) {
        if (dao.getCategories(businessId).isNotEmpty()) return
        val now = System.currentTimeMillis()
        template.defaultUnits.forEach { unit ->
            dao.insertUnit(
                UnitEntity(
                    id = UUID.randomUUID().toString(),
                    businessId = businessId,
                    name = unit.name,
                    symbol = unit.symbol,
                    normalizedName = normalize(unit.name),
                    createdAtEpochMillis = now,
                ),
            )
        }
        template.defaultCategories.forEach { category ->
            dao.insertCategory(
                CategoryEntity(
                    id = UUID.randomUUID().toString(),
                    businessId = businessId,
                    role = category.role.name,
                    name = category.name,
                    normalizedName = normalize(category.name),
                    templateOrigin = template.sector.name,
                    createdAtEpochMillis = now,
                ),
            )
        }
    }

    private suspend fun insertEventWithJournal(event: MoneyEvent) {
        insertEventAndJournal(event, LedgerPolicy.journalFor(event))
    }

    private suspend fun insertEventAndJournal(event: MoneyEvent, journal: JournalEntry) {
        dao.insertEvent(event.toEntity())
        dao.insertJournal(
            JournalEntryEntity(
                id = journal.id,
                sourceEventId = journal.sourceEventId,
                dateEpochDay = journal.date.toEpochDay(),
                description = journal.description,
                reversalOfJournalId = journal.reversalOfJournalId,
            ),
        )
        dao.insertPostings(journal.postings.mapIndexed { index, posting ->
            PostingEntity(
                id = "${journal.id}-$index",
                journalId = journal.id,
                account = posting.account.name,
                side = posting.side.name,
                amountRupiah = posting.amountRupiah,
            )
        })
    }

    private fun EventDraft.toEvent(): MoneyEvent {
        val obligationId = if (type in CREDIT_EVENT_TYPES) UUID.randomUUID().toString() else null
        return MoneyEvent(
            id = UUID.randomUUID().toString(),
            businessId = businessId,
            projectId = projectId,
            bookId = bookId,
            type = type,
            amountRupiah = amountRupiah,
            date = date,
            description = description.trim(),
            category = category.trim().ifBlank { defaultCategory(type) },
            categoryId = categoryId,
            counterparty = counterparty?.trim()?.ifBlank { null },
            reference = reference?.trim()?.ifBlank { null },
            dueDate = dueDate,
            quantityText = quantityText?.trim()?.ifBlank { null },
            unit = unit?.trim()?.ifBlank { null },
            unitPriceRupiah = unitPriceRupiah,
            budgetItemId = budgetItemId,
            obligationId = obligationId,
            importBatchId = importBatchId,
        )
    }

    private fun FinancialEventEntity.toDomain() = MoneyEvent(
        id = id,
        businessId = businessId,
        projectId = projectId,
        bookId = bookId,
        type = FinancialEventType.valueOf(type),
        amountRupiah = amountRupiah,
        date = LocalDate.ofEpochDay(dateEpochDay),
        description = description,
        category = category,
        categoryId = categoryId,
        counterparty = counterparty,
        reference = reference,
        dueDate = dueDateEpochDay?.let(LocalDate::ofEpochDay),
        quantityText = quantityText,
        unit = unit,
        unitPriceRupiah = unitPriceRupiah,
        budgetItemId = budgetItemId,
        obligationId = obligationId,
        importBatchId = importBatchId,
        cashFlowRole = CashFlowRole.valueOf(cashFlowRole),
        status = RecordStatus.valueOf(status),
        reversesEventId = reversesEventId,
        reversedByEventId = reversedByEventId,
    )

    private fun MoneyEvent.toEntity() = FinancialEventEntity(
        id = id,
        businessId = businessId,
        projectId = projectId,
        bookId = bookId,
        type = type.name,
        amountRupiah = amountRupiah,
        dateEpochDay = date.toEpochDay(),
        description = description,
        category = category,
        categoryId = categoryId,
        counterparty = counterparty,
        reference = reference,
        dueDateEpochDay = dueDate?.toEpochDay(),
        quantityText = quantityText,
        unit = unit,
        unitPriceRupiah = unitPriceRupiah,
        budgetItemId = budgetItemId,
        obligationId = obligationId,
        importBatchId = importBatchId,
        cashFlowRole = cashFlowRole.name,
        status = status.name,
        reversesEventId = reversesEventId,
        reversedByEventId = reversedByEventId,
        createdAtEpochMillis = System.currentTimeMillis(),
    )

    private fun ObligationBalanceRow.toDomain() = ObligationBalance(
        id = id,
        kind = ObligationKind.valueOf(kind),
        originalAmountRupiah = originalAmountRupiah,
        settledAmountRupiah = settledAmountRupiah,
        dueDate = dueDateEpochDay?.let(LocalDate::ofEpochDay),
        reversed = reversed,
    )

    private fun calculateFormulaTotal(quantityText: String?, unitPriceRupiah: Long?): Long? {
        if (quantityText.isNullOrBlank() || unitPriceRupiah == null || unitPriceRupiah <= 0) return null
        val quantity = TransactionCalculation.parseQuantity(quantityText) ?: error("Jumlah harus berupa angka yang valid dan lebih dari nol.")
        return TransactionCalculation.computeTotalRupiah(quantity, unitPriceRupiah)
    }

    private fun defaultCategory(type: FinancialEventType) = when (type) {
        FinancialEventType.CASH_INCOME,
        FinancialEventType.CREDIT_SALE,
        FinancialEventType.RECEIVABLE_COLLECTION,
        -> "Pendapatan"

        FinancialEventType.CASH_EXPENSE,
        FinancialEventType.CREDIT_PURCHASE_EXPENSE,
        FinancialEventType.PAYABLE_SETTLEMENT,
        -> "Operasional"

        FinancialEventType.CREDIT_PURCHASE_ASSET,
        FinancialEventType.ASSET_PURCHASE_CASH,
        -> "Peralatan dan aset"

        FinancialEventType.OWNER_CAPITAL -> "Modal pemilik"
        FinancialEventType.OWNER_DRAWING -> "Pengambilan pemilik"
        FinancialEventType.LOAN_RECEIVED -> "Pinjaman diterima"
        FinancialEventType.LOAN_PRINCIPAL_REPAID -> "Pokok pinjaman"
        FinancialEventType.LOAN_INTEREST_PAID -> "Bunga pinjaman"
        FinancialEventType.REVERSAL -> "Pembalikan"
    }

    private fun normalize(value: String): String = value.lowercase(Locale.forLanguageTag("id-ID"))
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()

    private fun Long.toIdr(): String = "Rp" + String.format(Locale.forLanguageTag("id-ID"), "%,d", this)

    companion object {
        private val CREDIT_EVENT_TYPES = setOf(
            FinancialEventType.CREDIT_SALE,
            FinancialEventType.CREDIT_PURCHASE_EXPENSE,
            FinancialEventType.CREDIT_PURCHASE_ASSET,
        )

        // Units and categories below are seeded only into a *fresh* database. They are taken from
        // the reference business workbook so a new user sees the vocabulary they already write on paper,
        // rather than generic bookkeeping buckets. Existing installs keep whatever they already seeded;
        // categories remain user-editable from Pengaturan → Kategori & satuan.
        private val DEFAULT_UNITS = listOf(
            "Kilogram" to "Kg",
            "Gram" to "g",
            "Ton" to "ton",
            "Liter" to "Liter",
            "Sak" to "Sak",
            "Hari Orang Kerja" to "HOK",
            "Paket" to "Paket",
            "Lembar" to "Lembar",
            "Buah" to "buah",
            "Unit" to "unit",
            "Jam" to "jam",
            "Hari" to "hari",
        )

        private val DEFAULT_CATEGORIES = listOf(
            // Money in — the workbook's "Pendapatan / Hasil Penjualan".
            "Hasil Panen" to CategoryRole.REVENUE,
            "Penjualan Lain" to CategoryRole.REVENUE,
            // Money out — the workbook's four expense groups.
            "Benih dan Bibit" to CategoryRole.EXPENSE,
            "Pupuk" to CategoryRole.EXPENSE,
            "Obat dan Pestisida" to CategoryRole.EXPENSE,
            "Tenaga Kerja" to CategoryRole.EXPENSE,
            "Jasa Alsintan" to CategoryRole.EXPENSE,
            "Sewa Lahan" to CategoryRole.EXPENSE,
            "Pengairan" to CategoryRole.EXPENSE,
            "Transport" to CategoryRole.EXPENSE,
            "Lain-lain" to CategoryRole.EXPENSE,
            "Alat dan Mesin" to CategoryRole.ASSET,
            "Modal Sendiri" to CategoryRole.EQUITY,
            "Pinjaman" to CategoryRole.LIABILITY,
        )
    }
}
