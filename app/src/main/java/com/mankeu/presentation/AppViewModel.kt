package com.mankeu.presentation

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mankeu.backup.EncryptedBackupManager
import com.mankeu.backup.BackupPreview
import com.mankeu.backup.PreparedRestore
import com.mankeu.data.BookEntity
import com.mankeu.data.BudgetItemDraft
import com.mankeu.data.BudgetItemEntity
import com.mankeu.data.BusinessEntity
import com.mankeu.data.CategoryEntity
import com.mankeu.data.EventDraft
import com.mankeu.data.FinanceRepository
import com.mankeu.data.ImportNoticeEntity
import com.mankeu.data.ImportBatchEntity
import com.mankeu.data.ObligationView
import com.mankeu.data.ProjectEntity
import com.mankeu.data.ScopeRemoval
import com.mankeu.data.ScenarioAssumptionEntity
import com.mankeu.data.CategoryTotalRow
import com.mankeu.domain.CashFlowRecord
import com.mankeu.domain.BookMode
import com.mankeu.domain.sha256Hex
import com.mankeu.domain.PlanBudgetLine
import com.mankeu.domain.CategoryRole
import com.mankeu.domain.FinancialEventType
import com.mankeu.domain.FinancialSummary
import com.mankeu.domain.MoneyEvent
import com.mankeu.domain.PostingReportCalculator
import com.mankeu.domain.TransactionCalculation
import com.mankeu.importer.FinanceWorkbookImporter
import com.mankeu.importer.WorkbookImportPreview
import com.mankeu.exporter.MsmeTemplateExporter
import com.mankeu.R
import com.mankeu.auth.FirebaseAuthManager
import com.mankeu.auth.AuthUser
import com.mankeu.backup.cloud.GoogleDriveBackupClient
import com.mankeu.backup.cloud.DriveBackupFile
import com.google.firebase.auth.AuthCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.UUID

private val EMPTY_SUMMARY = PostingReportCalculator.calculate(emptyList())

data class AppUiState(
    val businesses: List<BusinessEntity> = emptyList(),
    val projects: List<ProjectEntity> = emptyList(),
    val books: List<BookEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val budgetItems: List<BudgetItemEntity> = emptyList(),
    val selectedBusinessId: String? = null,
    val selectedProjectId: String? = null,
    val selectedBookMode: BookMode = BookMode.REALIZATION,
    val selectedBookId: String? = null,
    /** The newest slice of the ledger, for display. Never the basis of a reported total. */
    val events: List<MoneyEvent> = emptyList(),
    /** How many rows the book really holds, regardless of how many [events] carries. */
    val eventCount: Int = 0,
    /** Narrow rows behind the cash-flow schedule, and category totals summed by the database. */
    val cashFlowRecords: List<CashFlowRecord> = emptyList(),
    val categoryTotals: List<CategoryTotalRow> = emptyList(),
    val obligations: List<ObligationView> = emptyList(),
    val summary: FinancialSummary = EMPTY_SUMMARY,
    val projectionSummary: FinancialSummary = EMPTY_SUMMARY,
    val realizationSummary: FinancialSummary = EMPTY_SUMMARY,
    val projectionBudgetTotalRupiah: Long = 0,
    val realizationBudgetTotalRupiah: Long = 0,
    val assumptions: ScenarioAssumptionEntity? = null,
    val notices: List<ImportNoticeEntity> = emptyList(),
    val importBatches: List<ImportBatchEntity> = emptyList(),
    /**
     * True only for the initial placeholder emitted before the first database read completes.
     * Screens use it to show skeleton placeholders instead of briefly rendering a real-looking
     * but empty state (which would read as "you have no records" during a cold start).
     */
    val isLoading: Boolean = true,
) {
    /**
     * The selected book's RAB as the domain sees it: an amount, the role of the category it sits
     * on, and the period it is planned for. Defined once here rather than in each screen, because
     * Beranda and Laporan must not disagree about which category role a plan line carries.
     *
     * An unknown category role falls back to EXPENSE, matching what `commitImport` does when it
     * creates a category from a workbook name.
     */
    fun planBudgetLines(): List<PlanBudgetLine> {
        val roles = categories.associate { it.id to it.role }
        return budgetItems.map { item ->
            PlanBudgetLine(
                plannedTotalRupiah = item.plannedTotalRupiah,
                role = runCatching { CategoryRole.valueOf(roles[item.categoryId].orEmpty()) }
                    .getOrDefault(CategoryRole.EXPENSE),
                plannedDate = item.plannedDateEpochDay?.let(LocalDate::ofEpochDay),
            )
        }
    }
}

sealed interface ImportUiState {
    data object Idle : ImportUiState
    data object Loading : ImportUiState
    data class Ready(val preview: WorkbookImportPreview) : ImportUiState
    data class Committed(val count: Int, val enrichmentCount: Int) : ImportUiState
    data class Failure(val message: UiText) : ImportUiState
}

sealed interface TransferUiState {
    data object Idle : TransferUiState
    data class Working(val label: UiText) : TransferUiState
    data class Success(val message: UiText) : TransferUiState
    data class Failure(val message: UiText) : TransferUiState
}

sealed interface RestoreUiState {
    data object Idle : RestoreUiState
    data class Ready(val preview: BackupPreview) : RestoreUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(
    private val repository: FinanceRepository,
    private val importer: FinanceWorkbookImporter,
    private val backupManager: EncryptedBackupManager,
    private val authService: FirebaseAuthManager,
    private val cloudBackupClient: GoogleDriveBackupClient,
) : ViewModel() {
    private val selectedBusinessId = MutableStateFlow<String?>(null)
    private val selectedProjectId = MutableStateFlow<String?>(null)
    private val selectedBookMode = MutableStateFlow(BookMode.REALIZATION)

    val authUser: StateFlow<AuthUser?> = authService.currentUser
    val driveBackups = MutableStateFlow<List<DriveBackupFile>>(emptyList())
    val isDriveLoading = MutableStateFlow(false)

    val importState = MutableStateFlow<ImportUiState>(ImportUiState.Idle)
    val transferState = MutableStateFlow<TransferUiState>(TransferUiState.Idle)
    val restoreState = MutableStateFlow<RestoreUiState>(RestoreUiState.Idle)
    val recoveryAvailable = MutableStateFlow(backupManager.hasLatestRecovery())
    val message = MutableStateFlow<UiText?>(null)
    /**
     * True while a record-changing operation is in flight.
     *
     * Every mutation was a bare `viewModelScope.launch`, so the UI had no way to know one was
     * running and every save button stayed live through it. On a slow phone the write takes long
     * enough to tap three times, and each tap inserted its own transaction — three identical rows
     * in the ledger, three journal entries, a cash balance wrong by twice the amount.
     */
    val isSaving = MutableStateFlow(false)

    private var preparedRestore: PreparedRestore? = null
    private var recoveryPassword: CharArray? = null

    /**
     * The sharing policy for the per-book query flows.
     *
     * `Eagerly` kept all nineteen flows collecting from the moment the ViewModel was built until
     * the process died — so a book sat open in the background re-ran its queries on every write,
     * draining battery for a screen nobody was looking at. `WhileSubscribed` lets them stop five
     * seconds after the last collector leaves, which covers a configuration change without tearing
     * anything down.
     *
     * Applied to the fourteen flows that are genuinely per-book reads. The four selection flows
     * keep `Eagerly` for the reason given where they are declared.
     *
     * The five-second timeout is the visible half. The half that matters for correctness is
     * `replayExpirationMillis`, which defaults to `Long.MAX_VALUE`: the cached value is *never*
     * reset to the initial one when a flow goes cold. `record`, `createBudgetItem` and
     * `saveAssumptions` read `selectedBookId.value` and `uiState.value` directly, and a reset
     * cache would hand them a null book id and silently drop the user's transaction.
     */
    private val KeepWarm = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000)

    // ---- Selection chain: stays eager, deliberately. ------------------------------------------
    //
    // These four decide *which* book every flow below reads, and two of them are collected
    // permanently by `init` to reconcile the selection — so they can never go cold anyway and
    // `WhileSubscribed` would save nothing on them.
    //
    // It would also cost correctness. Each `stateIn` carries a placeholder initial value, and the
    // reconcilers in `init` cannot tell a placeholder empty list from a database that really is
    // empty. Made lazy, the placeholder is consumed at subscribe time — during startup — and the
    // reconciler nulls the selection out from under a workspace that `currentWorkspace()` had
    // already resolved. Observed on device: a cold start rendered Rp0 across a book holding
    // Rp27.050.000, and only a second launch showed the real figures. A book that looks empty is
    // the most alarming thing this app can show, so the selection chain is not the place to
    // economise (D-071).
    private val businesses = repository.businesses.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val projects = selectedBusinessId.flatMapLatest { id ->
        id?.let(repository::projects) ?: flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val books = selectedProjectId.flatMapLatest { id ->
        id?.let(repository::books) ?: flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val selectedBookId = combine(books, selectedBookMode) { items, mode ->
        items.firstOrNull { it.mode == mode.name }?.id
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // ---- Everything below is a per-book live query, and does stop when nobody is looking. ------
    private val categories = selectedBusinessId.flatMapLatest { id ->
        id?.let(repository::categories) ?: flowOf(emptyList())
    }.stateIn(viewModelScope, KeepWarm, emptyList())
    private val events = selectedBookId.flatMapLatest { id ->
        id?.let(repository::eventsForBook) ?: flowOf(emptyList())
    }.stateIn(viewModelScope, KeepWarm, emptyList())
    private val eventCount = selectedBookId.flatMapLatest { id ->
        id?.let(repository::eventCount) ?: flowOf(0)
    }.stateIn(viewModelScope, KeepWarm, 0)
    private val cashFlowRecords = selectedBookId.flatMapLatest { id ->
        id?.let(repository::cashFlowRecords) ?: flowOf(emptyList())
    }.stateIn(viewModelScope, KeepWarm, emptyList())
    private val categoryTotals = selectedBookId.flatMapLatest { id ->
        id?.let(repository::categoryTotals) ?: flowOf(emptyList())
    }.stateIn(viewModelScope, KeepWarm, emptyList())
    private val budgetItems = selectedBookId.flatMapLatest { id ->
        id?.let(repository::budgetItems) ?: flowOf(emptyList())
    }.stateIn(viewModelScope, KeepWarm, emptyList())
    private val obligations = selectedBookId.flatMapLatest { id ->
        id?.let(repository::obligations) ?: flowOf(emptyList())
    }.stateIn(viewModelScope, KeepWarm, emptyList())
    private val summary = selectedBookId.flatMapLatest { id ->
        id?.let(repository::accountingSummary) ?: flowOf(EMPTY_SUMMARY)
    }.stateIn(viewModelScope, KeepWarm, EMPTY_SUMMARY)
    private val assumptions = selectedBookId.flatMapLatest { id ->
        id?.let(repository::assumptions) ?: flowOf(null)
    }.stateIn(viewModelScope, KeepWarm, null)
    private val importBatches = selectedProjectId.flatMapLatest { id ->
        id?.let(repository::importBatches) ?: flowOf(emptyList())
    }.stateIn(viewModelScope, KeepWarm, emptyList())

    private val projectionSummary = books.flatMapLatest { items ->
        items.firstOrNull { it.mode == BookMode.PROJECTION.name }?.id
            ?.let(repository::accountingSummary) ?: flowOf(EMPTY_SUMMARY)
    }.stateIn(viewModelScope, KeepWarm, EMPTY_SUMMARY)
    private val realizationSummary = books.flatMapLatest { items ->
        items.firstOrNull { it.mode == BookMode.REALIZATION.name }?.id
            ?.let(repository::accountingSummary) ?: flowOf(EMPTY_SUMMARY)
    }.stateIn(viewModelScope, KeepWarm, EMPTY_SUMMARY)
    // "Total RAB" means total planned *cost*. Summing every line put a revenue target into it —
    // a plan with a Rp168 juta sales target read as Rp309 juta of budget — so the comparison row
    // measured nothing. Revenue-role lines are excluded here; they are the target the comparison's
    // own income row already carries.
    private fun budgetCostTotal(mode: BookMode): Flow<Long> = books.flatMapLatest { items ->
        val bookId = items.firstOrNull { it.mode == mode.name }?.id ?: return@flatMapLatest flowOf(0L)
        combine(repository.budgetItems(bookId), categories) { budgets, categoryRows ->
            val roles = categoryRows.associate { it.id to it.role }
            budgets.filterNot { roles[it.categoryId] == CategoryRole.REVENUE.name }
                .sumOf { it.plannedTotalRupiah }
        }
    }

    private val projectionBudgetTotal = budgetCostTotal(BookMode.PROJECTION)
        .stateIn(viewModelScope, KeepWarm, 0L)
    private val realizationBudgetTotal = budgetCostTotal(BookMode.REALIZATION)
        .stateIn(viewModelScope, KeepWarm, 0L)

    private val scopeContent = combine(businesses, projects, books, categories) {
            businessItems, projectItems, bookItems, categoryItems ->
        ScopeContent(businessItems, projectItems, bookItems, categoryItems)
    }
    private val financeContent = combine(events, budgetItems, obligations, summary, assumptions) {
            eventItems, budgetItemItems, obligationItems, calculatedSummary, scenarioAssumptions ->
        FinanceContent(eventItems, budgetItemItems, obligationItems, calculatedSummary, scenarioAssumptions)
    }
    // `combine` only has typed overloads up to five flows, and the top-level state needs six
    // groups, so the ledger aggregates ride along with the finance content rather than pushing the
    // whole thing onto the untyped vararg overload.
    private val ledgerAggregates = combine(eventCount, cashFlowRecords, categoryTotals) { count, records, totals ->
        LedgerAggregates(count, records, totals)
    }
    private val financeAndAggregates = combine(financeContent, ledgerAggregates) { finance, aggregates ->
        finance to aggregates
    }
    private val selections = combine(selectedBusinessId, selectedProjectId, selectedBookMode, selectedBookId) {
            businessId, projectId, mode, bookId ->
        SelectionContent(businessId, projectId, mode, bookId)
    }
    private val comparison = combine(projectionSummary, realizationSummary, projectionBudgetTotal, realizationBudgetTotal) {
            projection, realization, projectionBudget, realizationBudget ->
        ComparisonContent(projection, realization, projectionBudget, realizationBudget)
    }
    private val importAudit = combine(repository.notices, importBatches) { notices, batches ->
        ImportAudit(notices, batches)
    }

    val uiState = combine(scopeContent, financeAndAggregates, importAudit, selections, comparison) {
            scope, financePair, audit, selection, comparison ->
        val (finance, aggregates) = financePair
        AppUiState(
            businesses = scope.businesses,
            projects = scope.projects,
            books = scope.books,
            categories = scope.categories,
            budgetItems = finance.budgetItems,
            selectedBusinessId = selection.businessId,
            selectedProjectId = selection.projectId,
            selectedBookMode = selection.bookMode,
            selectedBookId = selection.bookId,
            events = finance.events,
            eventCount = aggregates.eventCount,
            cashFlowRecords = aggregates.cashFlowRecords,
            categoryTotals = aggregates.categoryTotals,
            obligations = finance.obligations,
            summary = finance.summary,
            projectionSummary = comparison.projection,
            realizationSummary = comparison.realization,
            projectionBudgetTotalRupiah = comparison.projectionBudget,
            realizationBudgetTotalRupiah = comparison.realizationBudget,
            assumptions = finance.assumptions,
            notices = audit.notices,
            importBatches = audit.batches,
            // Any emission from this combine means the underlying flows have produced real data.
            isLoading = false,
        )
    }.stateIn(viewModelScope, KeepWarm, AppUiState(isLoading = true))

    init {
        viewModelScope.launch {
            // Reads only. A first run now legitimately resolves to (null, null) and the shell
            // asks the owner to name their own usaha and lahan, instead of the app writing
            // "Usaha Saya"/"Umum" behind their back and presenting it as their data.
            val selection = repository.currentWorkspace()
            selectedBusinessId.value = selection.businessId
            selectedProjectId.value = selection.projectId
        }
        viewModelScope.launch {
            businesses.collect { items ->
                if (items.isNotEmpty() && items.none { it.id == selectedBusinessId.value }) {
                    selectedBusinessId.value = items.first().id
                } else if (items.isEmpty()) {
                    selectedBusinessId.value = null
                }
            }
        }
        viewModelScope.launch {
            projects.collect { items ->
                if (items.isNotEmpty() && items.none { it.id == selectedProjectId.value }) {
                    selectedProjectId.value = items.first().id
                } else if (items.isEmpty()) {
                    selectedProjectId.value = null
                }
            }
        }
    }

    fun selectBusiness(id: String) {
        selectedProjectId.value = null
        selectedBusinessId.value = id
    }

    fun selectProject(id: String) {
        selectedProjectId.value = id
    }

    fun selectBookMode(mode: BookMode) {
        selectedBookMode.value = mode
    }

    /**
     * Runs a record-changing block with [isSaving] held, and drops the call entirely if one is
     * already running.
     *
     * Disabling the button is the visible half and it is not sufficient on its own: two taps
     * inside one frame both dispatch before any recomposition can disable anything. The
     * `compareAndSet` is what actually makes a double tap impossible; the flag is what makes it
     * look deliberate.
     */
    private fun mutating(block: suspend () -> Unit) = viewModelScope.launch {
        if (!isSaving.compareAndSet(expect = false, update = true)) return@launch
        try {
            block()
        } finally {
            isSaving.value = false
        }
    }

    fun createBusiness(name: String) = mutating {
        runCatching { repository.createBusiness(name) }
            .onSuccess { businessId ->
                selectedBusinessId.value = businessId
                selectedProjectId.value = null
                message.value = UiText.of(R.string.msg_business_created)
            }
            .onFailure { message.value = UiText.fromError(it, R.string.msg_business_create_failed) }
    }

    fun createProject(name: String) = mutating {
        val businessId = selectedBusinessId.value ?: return@mutating
        runCatching { repository.createProject(businessId, name) }
            .onSuccess { projectId ->
                selectedProjectId.value = projectId
                message.value = UiText.of(R.string.msg_project_created)
            }
            .onFailure { message.value = UiText.fromError(it, R.string.msg_project_create_failed) }
    }

    fun renameBusiness(name: String) = mutating {
        val id = selectedBusinessId.value ?: return@mutating
        runCatching { repository.renameBusiness(id, name) }
            .onSuccess { message.value = UiText.of(R.string.msg_business_renamed) }
            .onFailure { message.value = UiText.fromError(it, R.string.msg_business_rename_failed) }
    }

    fun renameProject(name: String) = mutating {
        val id = selectedProjectId.value ?: return@mutating
        runCatching { repository.renameProject(id, name) }
            .onSuccess { message.value = UiText.of(R.string.msg_project_renamed) }
            .onFailure { message.value = UiText.fromError(it, R.string.msg_project_rename_failed) }
    }

    fun updateProjectDetails(
        id: String,
        name: String,
        description: String,
        startDate: LocalDate?,
        endDate: LocalDate?,
    ) = mutating {
        runCatching { repository.updateProjectDetails(id, name, description, startDate, endDate) }
            .onSuccess { message.value = UiText.of(R.string.msg_project_updated) }
            .onFailure { message.value = UiText.fromError(it, R.string.msg_project_update_failed) }
    }

    /**
     * Archiving the active scope leaves nothing selected; the `businesses`/`projects` collectors in
     * `init` notice the selection has gone and fall back to the first remaining row, or to null when
     * there is none — which is what puts the explicit "Tambah Usaha"/"Tambah Lahan" state back on
     * screen rather than leaving a dangling id.
     */
    fun removeBusiness(id: String) = mutating {
        runCatching { repository.removeBusiness(id) }
            .onSuccess { outcome ->
                if (selectedBusinessId.value == id) selectedBusinessId.value = null
                message.value = UiText.of(
                    if (outcome == ScopeRemoval.DELETED) R.string.msg_business_deleted
                    else R.string.msg_business_archived,
                )
            }
            .onFailure { message.value = UiText.fromError(it, R.string.msg_business_archive_failed) }
    }

    fun removeProject(id: String) = mutating {
        runCatching { repository.removeProject(id) }
            .onSuccess { outcome ->
                if (selectedProjectId.value == id) selectedProjectId.value = null
                message.value = UiText.of(
                    if (outcome == ScopeRemoval.DELETED) R.string.msg_project_deleted
                    else R.string.msg_project_archived,
                )
            }
            .onFailure { message.value = UiText.fromError(it, R.string.msg_project_archive_failed) }
    }

    fun createCategory(name: String, role: CategoryRole) = mutating {
        val businessId = selectedBusinessId.value ?: return@mutating
        runCatching { repository.createCategory(businessId, name, role, aliases = listOf(name)) }
            .onSuccess { message.value = UiText.of(R.string.msg_category_added, name) }
            .onFailure { message.value = UiText.fromError(it, R.string.msg_category_add_failed) }
    }

    fun archiveCategory(id: String) = mutating {
        runCatching { repository.archiveCategory(id) }
            .onSuccess { message.value = UiText.of(R.string.msg_category_archived) }
            .onFailure { message.value = UiText.fromError(it, R.string.msg_category_archive_failed) }
    }

    fun record(
        type: FinancialEventType,
        amountRupiah: Long,
        description: String,
        categoryId: String?,
        category: String,
        date: LocalDate,
        counterparty: String? = null,
        dueDate: LocalDate? = null,
        reference: String? = null,
        quantityText: String? = null,
        unit: String? = null,
        unitPriceRupiah: Long? = null,
        budgetItemId: String? = null,
    ) = mutating {
        val state = uiState.value
        val businessId = state.selectedBusinessId
        val projectId = state.selectedProjectId
        val bookId = state.selectedBookId
        if (businessId == null || projectId == null || bookId == null) {
            message.value = UiText.of(R.string.entry_err_no_project)
            return@mutating
        }
        runCatching {
            val reconciled = TransactionCalculation.reconcile(
                amountRupiah = amountRupiah,
                quantityText = quantityText,
                unitPriceRupiah = unitPriceRupiah,
            )
            repository.recordEvent(
                EventDraft(
                    businessId = businessId,
                    projectId = projectId,
                    bookId = bookId,
                    type = type,
                    amountRupiah = reconciled.totalRupiah,
                    date = date,
                    description = description,
                    category = category,
                    categoryId = categoryId,
                    counterparty = counterparty,
                    dueDate = dueDate,
                    reference = reference,
                    quantityText = reconciled.quantityText,
                    unit = unit,
                    unitPriceRupiah = reconciled.unitPriceRupiah,
                    budgetItemId = budgetItemId,
                ),
            )
        }.onSuccess {
            message.value = UiText.of(
                when (type) {
                    FinancialEventType.CREDIT_SALE -> R.string.msg_credit_sale
                    FinancialEventType.CREDIT_PURCHASE_EXPENSE,
                    FinancialEventType.CREDIT_PURCHASE_ASSET,
                    -> R.string.msg_credit_purchase
                    else -> R.string.msg_event_saved
                },
            )
        }.onFailure {
            message.value = UiText.fromError(it, R.string.msg_event_save_failed)
        }
    }

    fun deleteTransaction(eventId: String) = mutating {
        runCatching {
            repository.deleteEvent(eventId)
        }.onSuccess {
            message.value = UiText.of(R.string.msg_event_deleted)
        }.onFailure {
            message.value = UiText.fromError(it, R.string.msg_event_save_failed)
        }
    }

    fun createBudgetItem(
        categoryId: String,
        name: String,
        quantityText: String?,
        unit: String?,
        unitPriceRupiah: Long?,
        plannedTotalRupiah: Long?,
        plannedDate: LocalDate?,
    ) = mutating {
        val bookId = selectedBookId.value ?: return@mutating
        val reconciled = TransactionCalculation.reconcile(
            amountRupiah = plannedTotalRupiah ?: 0L,
            quantityText = quantityText,
            unitPriceRupiah = unitPriceRupiah,
        )
        runCatching {
            repository.createBudgetItem(
                BudgetItemDraft(
                    bookId = bookId,
                    categoryId = categoryId,
                    name = name,
                    quantityText = reconciled.quantityText,
                    unit = unit,
                    unitPriceRupiah = reconciled.unitPriceRupiah,
                    plannedTotalRupiah = reconciled.totalRupiah.takeIf { it > 0 } ?: plannedTotalRupiah,
                    plannedDate = plannedDate,
                    aliases = listOf(name),
                ),
            )
        }.onSuccess {
            message.value = UiText.of(R.string.msg_budget_added, selectedBookMode.value.label())
        }.onFailure {
            message.value = UiText.fromError(it, R.string.msg_budget_add_failed)
        }
    }

    fun archiveBudgetItem(id: String) = mutating {
        runCatching { repository.archiveBudgetItem(id) }
            .onSuccess { message.value = UiText.of(R.string.msg_budget_archived) }
            .onFailure { message.value = UiText.fromError(it, R.string.msg_budget_archive_failed) }
    }

    fun updateBudgetItem(
        id: String,
        categoryId: String,
        name: String,
        quantityText: String?,
        unit: String?,
        unitPriceRupiah: Long?,
        plannedTotalRupiah: Long?,
        plannedDate: LocalDate?,
    ) = mutating {
        val bookId = selectedBookId.value ?: return@mutating
        val reconciled = TransactionCalculation.reconcile(
            amountRupiah = plannedTotalRupiah ?: 0L,
            quantityText = quantityText,
            unitPriceRupiah = unitPriceRupiah,
        )
        runCatching {
            repository.updateBudgetItem(
                id,
                BudgetItemDraft(
                    bookId = bookId,
                    categoryId = categoryId,
                    name = name,
                    quantityText = reconciled.quantityText,
                    unit = unit,
                    unitPriceRupiah = reconciled.unitPriceRupiah,
                    plannedTotalRupiah = reconciled.totalRupiah.takeIf { it > 0 } ?: plannedTotalRupiah,
                    plannedDate = plannedDate,
                    aliases = listOf(name),
                ),
            )
        }.onSuccess { message.value = UiText.of(R.string.msg_budget_updated) }
            .onFailure { message.value = UiText.fromError(it, R.string.msg_budget_update_failed) }
    }

    fun settleObligation(id: String, amountRupiah: Long, date: LocalDate, description: String) = mutating {
        runCatching { repository.settleObligation(id, amountRupiah, date, description) }
            .onSuccess { message.value = UiText.of(R.string.msg_settlement_saved) }
            .onFailure { message.value = UiText.fromError(it, R.string.msg_settlement_failed) }
    }

    fun reverseEvent(id: String, reason: String) = mutating {
        runCatching { repository.reverseEvent(id, LocalDate.now(), reason) }
            .onSuccess { message.value = UiText.of(R.string.msg_event_reversed) }
            .onFailure { message.value = UiText.fromError(it, R.string.msg_event_reverse_failed) }
    }

    fun saveAssumptions(
        openingCashRupiah: Long,
        ownerCapitalRupiah: Long,
        loanAmountRupiah: Long,
        interestRateBasisPoints: Int,
        otherFinancingCostRupiah: Long,
        outputQuantityText: String?,
        outputUnit: String?,
        outputPriceRupiah: Long?,
    ) = mutating {
        val bookId = selectedBookId.value ?: return@mutating
        val existing = uiState.value.assumptions
        runCatching {
            repository.saveAssumptions(
                ScenarioAssumptionEntity(
                    id = existing?.id ?: UUID.randomUUID().toString(),
                    bookId = bookId,
                    openingCashRupiah = openingCashRupiah,
                    ownerCapitalRupiah = ownerCapitalRupiah,
                    loanAmountRupiah = loanAmountRupiah,
                    interestRateBasisPoints = interestRateBasisPoints,
                    loanDisbursementEpochDay = existing?.loanDisbursementEpochDay,
                    loanPaymentEpochDay = existing?.loanPaymentEpochDay,
                    otherFinancingCostRupiah = otherFinancingCostRupiah,
                    outputQuantityText = outputQuantityText,
                    outputUnit = outputUnit,
                    outputPriceRupiah = outputPriceRupiah,
                    updatedAtEpochMillis = System.currentTimeMillis(),
                ),
            )
        }.onSuccess { message.value = UiText.of(R.string.msg_assumptions_saved) }
            .onFailure { message.value = UiText.fromError(it, R.string.msg_assumptions_failed) }
    }

    fun previewWorkbook(contentResolver: ContentResolver, uri: Uri) = viewModelScope.launch {
        importState.value = ImportUiState.Loading
        val result = runCatching {
            withContext(Dispatchers.IO) {
                val name = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                } ?: "workbook.xlsx"
                val bytes = contentResolver.openInputStream(uri)?.use { stream ->
                    stream.readBytes()
                } ?: error("File tidak dapat dibuka.")
                require(bytes.isNotEmpty()) { "File Excel kosong (0 byte)." }
                require(bytes.size <= 25 * 1024 * 1024) { "Ukuran workbook melebihi batas aman 25 MB." }
                importer.preview(name, bytes)
            }
        }
        importState.value = result.fold(
            onSuccess = { preview ->
                if (preview.errors.isNotEmpty() && !preview.canCommit) {
                    ImportUiState.Failure(UiText.Raw(preview.errors.joinToString("\n")))
                } else {
                    ImportUiState.Ready(preview)
                }
            },
            onFailure = { ImportUiState.Failure(UiText.fromError(it, R.string.msg_import_failed)) },
        )
    }

    /**
     * @param targetBook which book the rows land in. Null keeps whatever the file says, which only
     * a Mankeu export does; for everything else the screen passes the book the user is looking at.
     */
    fun commitImport(targetBook: BookMode? = null) = mutating {
        val preview = (importState.value as? ImportUiState.Ready)?.preview ?: return@mutating
        importState.value = ImportUiState.Loading
        val result = runCatching {
            withContext(Dispatchers.IO) {
                // One transaction for the scope it may create and the rows it writes. Creating the
                // business and field first, separately, left an empty "Lahan Utama" behind every
                // time the commit itself failed — an import that was supposed to be atomic.
                repository.inTransaction {
                    val businessId = selectedBusinessId.value ?: repository.createBusiness(
                        preview.sourceName.substringBeforeLast(".").trim()
                            .takeUnless { it.isBlank() || it.equals("workbook", ignoreCase = true) }
                            ?: "Usaha Tani",
                    )
                    val projectId = selectedProjectId.value?.takeIf { businessId == selectedBusinessId.value }
                        ?: repository.createProject(businessId, "Lahan Utama")
                    val destination = if (preview.carriesBookMode) null else (targetBook ?: selectedBookMode.value)
                    Triple(businessId, projectId, repository.commitImport(businessId, projectId, preview, destination))
                }
            }
        }
        result.onSuccess { (businessId, projectId, _) ->
            selectedBusinessId.value = businessId
            selectedProjectId.value = projectId
        }
        importState.value = result.fold(
            onSuccess = { ImportUiState.Committed(it.third, preview.enrichmentCount) },
            onFailure = { ImportUiState.Failure(UiText.fromError(it, R.string.msg_import_commit_failed)) },
        )
    }

    fun resetImportState() {
        importState.value = ImportUiState.Idle
    }

    fun reverseImportBatch(id: String) = mutating {
        runCatching { repository.reverseImportBatch(id) }
            .onSuccess { count -> message.value = UiText.of(R.string.msg_batch_reversed, count) }
            .onFailure { message.value = UiText.fromError(it, R.string.msg_batch_reverse_failed) }
    }

    fun exportWorkbook(contentResolver: ContentResolver, uri: Uri) = viewModelScope.launch {
        val projectId = selectedProjectId.value ?: return@launch
        transferState.value = TransferUiState.Working(UiText.of(R.string.work_building_workbook))
        val result = runCatching {
            withContext(Dispatchers.IO) {
                val bytes = repository.exportProjectWorkbook(projectId)
                contentResolver.openOutputStream(uri, "w")?.use { output ->
                    output.write(bytes)
                    output.flush()
                } ?: error("Tujuan ekspor tidak dapat dibuka.")
                bytes.size
            }
        }
        transferState.value = result.fold(
            onSuccess = { TransferUiState.Success(UiText.of(R.string.msg_export_xlsx_ok, it / 1024)) },
            onFailure = { TransferUiState.Failure(UiText.fromError(it, R.string.msg_export_xlsx_failed)) },
        )
    }

    fun exportMsmeTemplate(contentResolver: ContentResolver, uri: Uri) = viewModelScope.launch {
        transferState.value = TransferUiState.Working(UiText.of(R.string.work_building_workbook))
        val result = runCatching {
            withContext(Dispatchers.IO) {
                val bytes = MsmeTemplateExporter().generateTemplate()
                contentResolver.openOutputStream(uri, "w")?.use { output ->
                    output.write(bytes)
                    output.flush()
                } ?: error("Tujuan ekspor tidak dapat dibuka.")
                bytes.size
            }
        }
        transferState.value = result.fold(
            onSuccess = { TransferUiState.Success(UiText.of(R.string.msg_template_export_ok, it / 1024)) },
            onFailure = { TransferUiState.Failure(UiText.fromError(it, R.string.msg_export_xlsx_failed)) },
        )
    }

    fun exportPdf(contentResolver: ContentResolver, uri: Uri) = viewModelScope.launch {
        val projectId = selectedProjectId.value ?: return@launch
        transferState.value = TransferUiState.Working(UiText.of(R.string.work_building_pdf))
        val result = runCatching {
            withContext(Dispatchers.IO) {
                val bytes = repository.exportProjectPdf(projectId)
                contentResolver.openOutputStream(uri, "w")?.use { output ->
                    output.write(bytes)
                    output.flush()
                } ?: error("Tujuan PDF tidak dapat dibuka.")
                bytes.size
            }
        }
        transferState.value = result.fold(
            onSuccess = { TransferUiState.Success(UiText.of(R.string.msg_export_pdf_ok, it / 1024)) },
            onFailure = { TransferUiState.Failure(UiText.fromError(it, R.string.msg_export_pdf_failed)) },
        )
    }

    fun createEncryptedBackup(contentResolver: ContentResolver, uri: Uri, password: CharArray) = viewModelScope.launch {
        transferState.value = TransferUiState.Working(UiText.of(R.string.work_encrypting_backup))
        val result = runCatching {
            withContext(Dispatchers.IO) {
                try {
                    val bytes = backupManager.create(password)
                    contentResolver.openOutputStream(uri, "w")?.use { output ->
                        output.write(bytes)
                        output.flush()
                    } ?: error("Tujuan cadangan tidak dapat dibuka.")
                    bytes.size
                } finally {
                    password.fill('\u0000')
                }
            }
        }
        transferState.value = result.fold(
            onSuccess = { TransferUiState.Success(UiText.of(R.string.msg_backup_ok, it / 1024)) },
            onFailure = { TransferUiState.Failure(UiText.fromError(it, R.string.msg_backup_failed)) },
        )
    }

    fun prepareEncryptedBackupRestore(contentResolver: ContentResolver, uri: Uri, password: CharArray) = viewModelScope.launch {
        cancelPreparedRestore()
        transferState.value = TransferUiState.Working(UiText.of(R.string.work_verifying_backup))
        val retainedPassword = password.copyOf()
        val result = runCatching {
            withContext(Dispatchers.IO) {
                try {
                    val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: error("Berkas cadangan tidak dapat dibuka.")
                    require(bytes.size <= 160 * 1024 * 1024) { "Berkas cadangan melebihi batas aman 160 MB." }
                    backupManager.prepareRestore(bytes, password)
                } finally {
                    password.fill('\u0000')
                }
            }
        }
        result.fold(
            onSuccess = { prepared ->
                preparedRestore = prepared
                recoveryPassword = retainedPassword
                restoreState.value = RestoreUiState.Ready(prepared.preview)
                transferState.value = TransferUiState.Idle
            },
            onFailure = {
                retainedPassword.fill('\u0000')
                transferState.value = TransferUiState.Failure(UiText.fromError(it, R.string.msg_backup_verify_failed))
            },
        )
    }

    fun confirmPreparedRestore() = viewModelScope.launch {
        val prepared = preparedRestore ?: return@launch
        val password = recoveryPassword ?: return@launch
        restoreState.value = RestoreUiState.Idle
        transferState.value = TransferUiState.Working(UiText.of(R.string.work_restoring_backup))
        val result = runCatching {
            withContext(Dispatchers.IO) { backupManager.restore(prepared, password) }
        }
        password.fill('\u0000')
        preparedRestore = null
        recoveryPassword = null
        transferState.value = result.fold(
            onSuccess = { restored ->
                recoveryAvailable.value = restored.recoveryCreated
                val selection = repository.currentWorkspace()
                selectedBusinessId.value = selection.businessId
                selectedProjectId.value = selection.projectId
                TransferUiState.Success(UiText.of(R.string.msg_restore_ok, restored.rowCount, restored.tableCount))
            },
            onFailure = { TransferUiState.Failure(UiText.fromError(it, R.string.msg_restore_failed)) },
        )
    }

    fun cancelPreparedRestore() {
        recoveryPassword?.fill('\u0000')
        recoveryPassword = null
        preparedRestore = null
        restoreState.value = RestoreUiState.Idle
    }

    fun exportLatestRecovery(contentResolver: ContentResolver, uri: Uri) = viewModelScope.launch {
        transferState.value = TransferUiState.Working(UiText.of(R.string.work_copying_recovery))
        val result = runCatching {
            withContext(Dispatchers.IO) {
                val bytes = backupManager.readLatestRecovery()
                contentResolver.openOutputStream(uri, "w")?.use { output ->
                    output.write(bytes)
                    output.flush()
                } ?: error("Tujuan salinan pemulihan tidak dapat dibuka.")
                bytes.size
            }
        }
        transferState.value = result.fold(
            onSuccess = { TransferUiState.Success(UiText.of(R.string.msg_recovery_export_ok, it / 1024)) },
            onFailure = { TransferUiState.Failure(UiText.fromError(it, R.string.msg_recovery_export_failed)) },
        )
    }

    fun signInWithGoogleIdToken(idToken: String) = viewModelScope.launch {
        transferState.value = TransferUiState.Working(UiText.of(R.string.work_restoring_backup))
        val result = authService.signInWithGoogleIdToken(idToken)
        transferState.value = result.fold(
            onSuccess = { user ->
                message.value = UiText.of(R.string.msg_sign_in_ok, user.displayName ?: user.email ?: "")
                TransferUiState.Idle
            },
            onFailure = {
                TransferUiState.Failure(UiText.fromError(it, R.string.msg_sign_in_failed))
            }
        )
    }

    fun signInWithCredential(credential: AuthCredential) = viewModelScope.launch {
        val result = authService.signInWithCredential(credential)
        transferState.value = result.fold(
            onSuccess = { user ->
                message.value = UiText.of(R.string.msg_sign_in_ok, user.displayName ?: user.email ?: "")
                TransferUiState.Idle
            },
            onFailure = {
                TransferUiState.Failure(UiText.fromError(it, R.string.msg_sign_in_failed))
            }
        )
    }

    fun setAuthenticatedUser(user: AuthUser?) {
        authService.setCustomUser(user)
    }

    fun signOut() {
        authService.signOut()
        driveBackups.value = emptyList()
        message.value = UiText.of(R.string.msg_sign_out_ok)
    }

    fun fetchDriveBackups(accessToken: String) = viewModelScope.launch {
        isDriveLoading.value = true
        val result = runCatching {
            cloudBackupClient.listBackups(accessToken)
        }
        isDriveLoading.value = false
        result.fold(
            onSuccess = { driveBackups.value = it },
            onFailure = { transferState.value = TransferUiState.Failure(UiText.fromError(it, R.string.msg_drive_list_failed)) }
        )
    }

    fun backupToDrive(password: CharArray, accessToken: String) = viewModelScope.launch {
        transferState.value = TransferUiState.Working(UiText.of(R.string.work_uploading_backup))
        val result = runCatching {
            withContext(Dispatchers.IO) {
                val bytes = backupManager.create(password)
                val sha256 = bytes.sha256Hex()
                val filename = "umkm-mankeu-backup-${LocalDate.now()}.aabk"
                cloudBackupClient.uploadBackup(accessToken, filename, bytes, sha256)
                bytes.size
            }
        }
        password.fill('\u0000')
        transferState.value = result.fold(
            onSuccess = { size ->
                fetchDriveBackups(accessToken)
                TransferUiState.Success(UiText.of(R.string.msg_drive_backup_ok, size / 1024))
            },
            onFailure = { TransferUiState.Failure(UiText.fromError(it, R.string.msg_drive_backup_failed)) }
        )
    }

    fun prepareDriveBackupRestore(fileId: String, password: CharArray, accessToken: String) = viewModelScope.launch {
        cancelPreparedRestore()
        transferState.value = TransferUiState.Working(UiText.of(R.string.work_downloading_backup))
        val retainedPassword = password.copyOf()
        val result = runCatching {
            withContext(Dispatchers.IO) {
                try {
                    val bytes = cloudBackupClient.downloadBackup(accessToken, fileId)
                    require(bytes.size <= 160 * 1024 * 1024) { "Berkas cadangan melebihi batas aman 160 MB." }
                    backupManager.prepareRestore(bytes, password)
                } finally {
                    password.fill('\u0000')
                }
            }
        }
        result.fold(
            onSuccess = { prepared ->
                preparedRestore = prepared
                recoveryPassword = retainedPassword
                restoreState.value = RestoreUiState.Ready(prepared.preview)
                transferState.value = TransferUiState.Idle
            },
            onFailure = {
                retainedPassword.fill('\u0000')
                transferState.value = TransferUiState.Failure(UiText.fromError(it, R.string.msg_drive_restore_failed))
            }
        )
    }

    fun clearTransferState() {
        transferState.value = TransferUiState.Idle
    }

    override fun onCleared() {
        cancelPreparedRestore()
        super.onCleared()
    }

    fun clearMessage() {
        message.value = null
    }

    private data class ScopeContent(
        val businesses: List<BusinessEntity>,
        val projects: List<ProjectEntity>,
        val books: List<BookEntity>,
        val categories: List<CategoryEntity>,
    )

    private data class LedgerAggregates(
        val eventCount: Int,
        val cashFlowRecords: List<CashFlowRecord>,
        val categoryTotals: List<CategoryTotalRow>,
    )

    private data class FinanceContent(
        val events: List<MoneyEvent>,
        val budgetItems: List<BudgetItemEntity>,
        val obligations: List<ObligationView>,
        val summary: FinancialSummary,
        val assumptions: ScenarioAssumptionEntity?,
    )

    private data class SelectionContent(
        val businessId: String?,
        val projectId: String?,
        val bookMode: BookMode,
        val bookId: String?,
    )

    private data class ImportAudit(
        val notices: List<ImportNoticeEntity>,
        val batches: List<ImportBatchEntity>,
    )

    private data class ComparisonContent(
        val projection: FinancialSummary,
        val realization: FinancialSummary,
        val projectionBudget: Long,
        val realizationBudget: Long,
    )
}

private fun BookMode.label() = if (this == BookMode.PROJECTION) "Proyeksi" else "Realisasi"
