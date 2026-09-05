package com.mankeu.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mankeu.R
import com.mankeu.domain.CategoryRole
import com.mankeu.domain.FinancialEventType
import com.mankeu.presentation.AppUiState
import com.mankeu.presentation.ChoiceMenu
import com.mankeu.presentation.LocalMankeuSemanticColors
import com.mankeu.presentation.LocalMotion
import com.mankeu.presentation.components.DateField
import com.mankeu.domain.TransactionCalculation
import com.mankeu.presentation.components.EmptyCard
import com.mankeu.presentation.components.EventCard
import com.mankeu.presentation.components.MoneyField
import com.mankeu.presentation.components.TourTarget
import com.mankeu.presentation.components.tourTarget
import com.mankeu.presentation.components.SectionTitle
import com.mankeu.presentation.components.sanitizeDecimal
import com.mankeu.presentation.components.sanitizeRupiah
import com.mankeu.presentation.toRupiah
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.launch

private data class EntryKind(
    val type: FinancialEventType,
    val labelRes: Int,
    val helperRes: Int,
    val icon: ImageVector,
    val credit: Boolean = false,
    /** The two everyday cases get large primary buttons; the rest are tucked behind "Jenis lain". */
    val primary: Boolean = false,
)

// Wording comes from BusinessWords/strings.xml, which follow everyday business vocabulary
// rather than accounting terms. Helper lines describe what happened, not what the
// journal does ("Barang sudah diterima, bayar menyusul" instead of "Biaya sekarang, kas saat
// dilunasi").
private val entryKinds = listOf(
    EntryKind(FinancialEventType.CASH_INCOME, R.string.kind_cash_in, R.string.kind_cash_in_help, Icons.Default.ArrowDownward, primary = true),
    EntryKind(FinancialEventType.CASH_EXPENSE, R.string.kind_cash_out, R.string.kind_cash_out_help, Icons.Default.ArrowUpward, primary = true),
    EntryKind(FinancialEventType.CREDIT_SALE, R.string.kind_sale_unpaid, R.string.kind_sale_unpaid_help, Icons.AutoMirrored.Filled.ReceiptLong, credit = true),
    EntryKind(FinancialEventType.CREDIT_PURCHASE_EXPENSE, R.string.kind_buy_unpaid, R.string.kind_buy_unpaid_help, Icons.Default.Payments, credit = true),
    EntryKind(FinancialEventType.CREDIT_PURCHASE_ASSET, R.string.kind_tool_unpaid, R.string.kind_tool_unpaid_help, Icons.Default.Storefront, credit = true),
    EntryKind(FinancialEventType.OWNER_CAPITAL, R.string.kind_capital_in, R.string.kind_capital_in_help, Icons.Default.Savings),
    EntryKind(FinancialEventType.OWNER_DRAWING, R.string.kind_capital_out, R.string.kind_capital_out_help, Icons.Default.AccountBalance),
    EntryKind(FinancialEventType.LOAN_RECEIVED, R.string.kind_loan_in, R.string.kind_loan_in_help, Icons.Default.AttachMoney),
    EntryKind(FinancialEventType.LOAN_PRINCIPAL_REPAID, R.string.kind_loan_principal, R.string.kind_loan_principal_help, Icons.AutoMirrored.Filled.Redo),
    EntryKind(FinancialEventType.LOAN_INTEREST_PAID, R.string.kind_loan_interest, R.string.kind_loan_interest_help, Icons.Default.Description),
    EntryKind(FinancialEventType.ASSET_PURCHASE_CASH, R.string.kind_tool_cash, R.string.kind_tool_cash_help, Icons.Default.Storefront),
)

/**
 * The Catat (record transaction) destination: pick an event kind, fill amount/description/date,
 * and — for credit events — counterparty and due date. Advanced fields (quantity/unit/reference/
 * budget link) are collapsed by default to keep the common case fast.
 *
 * The detail form opens as a [ModalBottomSheet] once a kind is picked, instead of expanding
 * inline underneath the picker (the previous behaviour). Inline expansion pushed the amount/
 * description/save controls further down the same scrolling list the kind buttons live in — on a
 * phone screen the save button routinely ended up below the fold, so picking a kind "did
 * something" with no visible result until the owner scrolled down to find it. A bottom sheet is
 * the correct native pattern here: it's the same one-thing-at-a-time flow (pick kind -> fill
 * fields -> save) but the form is never fighting the picker for scroll position, and dismissing
 * it (swipe down, tap the scrim, or a successful save) unambiguously returns to the picker.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryScreen(
    state: AppUiState,
    onRecord: (
        FinancialEventType, Long, String, String?, String, LocalDate, String?, LocalDate?, String?, String?, String?, Long?, String?,
    ) -> Unit,
    onReverse: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onOpenData: () -> Unit,
    onStartTour: (() -> Unit)? = null,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var selected by rememberSaveable { mutableStateOf<FinancialEventType?>(null) }
    var amount by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var categoryId by rememberSaveable { mutableStateOf<String?>(null) }
    var counterparty by rememberSaveable { mutableStateOf("") }
    var dueDate by rememberSaveable { mutableStateOf("") }
    var reference by rememberSaveable { mutableStateOf("") }
    var quantity by rememberSaveable { mutableStateOf("") }
    var unit by rememberSaveable { mutableStateOf("") }
    var unitPrice by rememberSaveable { mutableStateOf("") }
    var budgetItemId by rememberSaveable { mutableStateOf<String?>(null) }
    var advanced by rememberSaveable { mutableStateOf(false) }
    var otherKinds by rememberSaveable { mutableStateOf(false) }
    var localError by rememberSaveable { mutableStateOf<String?>(null) }
    var isAutoCalculated by rememberSaveable { mutableStateOf(false) }
    var isUnitPriceAutoDerived by rememberSaveable { mutableStateOf(false) }

    // History filter states
    var ledgerSearch by rememberSaveable { mutableStateOf("") }
    var ledgerDirection by rememberSaveable { mutableStateOf("ALL") }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sheetScope = rememberCoroutineScope()

    fun closeSheet() {
        sheetScope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) selected = null
        }
    }

    val errAmount = stringResource(R.string.entry_err_amount)
    val errDescription = stringResource(R.string.entry_err_description)
    val errDate = stringResource(R.string.entry_err_date)
    val errCategory = stringResource(R.string.entry_err_category)
    val errCounterparty = stringResource(R.string.entry_err_counterparty)
    val errDue = stringResource(R.string.entry_err_due)
    val errQuantity = stringResource(R.string.entry_err_quantity)
    val errNoProject = stringResource(R.string.entry_err_no_project)

    val kind = entryKinds.firstOrNull { it.type == selected }
    val compatibleCategories = state.categories.filter { category ->
        val role = runCatching { CategoryRole.valueOf(category.role) }.getOrNull()
        when (selected) {
            FinancialEventType.CASH_INCOME, FinancialEventType.CREDIT_SALE -> role == CategoryRole.REVENUE
            FinancialEventType.CASH_EXPENSE, FinancialEventType.CREDIT_PURCHASE_EXPENSE -> role == CategoryRole.EXPENSE
            FinancialEventType.CREDIT_PURCHASE_ASSET, FinancialEventType.ASSET_PURCHASE_CASH -> role == CategoryRole.ASSET
            else -> true
        }
    }
    LaunchedEffect(selected, compatibleCategories) {
        if (compatibleCategories.none { it.id == categoryId }) categoryId = compatibleCategories.firstOrNull()?.id
    }

    val visibleEvents = state.events.filter { event ->
        val matchesText = ledgerSearch.isBlank() || listOf(event.description, event.category, event.counterparty, event.reference)
            .filterNotNull().any { it.contains(ledgerSearch, ignoreCase = true) }
        val incoming = event.type in setOf(
            FinancialEventType.CASH_INCOME,
            FinancialEventType.CREDIT_SALE,
            FinancialEventType.RECEIVABLE_COLLECTION,
            FinancialEventType.OWNER_CAPITAL,
            FinancialEventType.LOAN_RECEIVED,
        )
        matchesText && when (ledgerDirection) { "IN" -> incoming; "OUT" -> !incoming; else -> true }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionTitle(
                    title = stringResource(R.string.entry_title),
                    subtitle = stringResource(R.string.entry_subtitle),
                    modifier = Modifier.weight(1f),
                )
                if (onStartTour != null) {
                    TextButton(onClick = onStartTour) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.action_learn_page), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // Top Switcher: Buat Catatan vs Daftar Catatan
        item {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val showIcons = maxWidth >= 340.dp
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().tourTarget(TourTarget.ENTRY_TABS)) {
                    SegmentedButton(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            if (showIcons) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(
                                stringResource(R.string.entry_tab_new),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    SegmentedButton(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        modifier = Modifier.tourTarget(TourTarget.ENTRY_HISTORY),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            if (showIcons) {
                                Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(
                                stringResource(R.string.entry_tab_history) + " (${state.events.size})",
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        if (selectedTab == 0) {
            // TAB 0: BUAT CATATAN
            if (state.selectedProjectId == null || state.projects.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.entry_no_project_banner_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                            Text(
                                stringResource(R.string.entry_no_project_banner_body),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Button(
                                onClick = onOpenData,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError,
                                ),
                            ) {
                                Text(stringResource(R.string.entry_action_add_project), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    Modifier.fillMaxWidth().tourTarget(TourTarget.ENTRY_KINDS),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    entryKinds.filter { it.primary }.forEach { item ->
                        PrimaryKindButton(
                            kind = item,
                            selected = selected == item.type,
                            incoming = item.type == FinancialEventType.CASH_INCOME,
                            modifier = Modifier.weight(1f),
                        ) {
                            selected = item.type
                            localError = null
                        }
                    }
                }
            }

            item {
                TextButton(onClick = { otherKinds = !otherKinds }) {
                    Text(stringResource(R.string.entry_more_kinds))
                    Icon(
                        if (otherKinds) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                    )
                }
            }

            item {
                val motion = LocalMotion.current
                AnimatedVisibility(
                    visible = otherKinds,
                    enter = expandVertically(tween(maxOf(motion.mediumMillis, 1), easing = FastOutSlowInEasing)) +
                        fadeIn(tween(maxOf(motion.mediumMillis, 1))),
                    exit = shrinkVertically(tween(maxOf(motion.fastMillis, 1), easing = FastOutSlowInEasing)) +
                        fadeOut(tween(maxOf(motion.fastMillis, 1))),
                ) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(entryKinds.filterNot { it.primary }, key = { it.type.name }) { item ->
                            EntryKindCard(item, selected == item.type) {
                                selected = item.type
                                localError = null
                            }
                        }
                    }
                }
            }
        } else {
            // TAB 1: DAFTAR CATATAN (HISTORY FEED)
            if (state.events.isNotEmpty()) {
                item {
                    OutlinedTextField(
                        value = ledgerSearch,
                        onValueChange = { ledgerSearch = it },
                        label = { Text(stringResource(R.string.home_search_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    val filters = listOf(
                        "ALL" to stringResource(R.string.money_all),
                        "IN" to stringResource(R.string.money_in_short),
                        "OUT" to stringResource(R.string.money_out_short),
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filters) { (value, label) ->
                            FilterChip(
                                selected = ledgerDirection == value,
                                onClick = { ledgerDirection = value },
                                label = { Text(label) },
                            )
                        }
                    }
                }
            }

            if (state.events.isEmpty()) {
                item {
                    EmptyCard(
                        stringResource(R.string.home_empty_title),
                        stringResource(R.string.home_empty_body),
                        stringResource(R.string.home_empty_action),
                    ) {
                        selectedTab = 0
                    }
                }
            } else if (visibleEvents.isEmpty()) {
                item {
                    EmptyCard(
                        stringResource(R.string.home_no_match_title),
                        stringResource(R.string.home_no_match_body),
                    )
                }
            } else {
                items(visibleEvents, key = { it.id }) { event ->
                    EventCard(
                        event = event,
                        onReverse = onReverse,
                        onDelete = onDelete,
                    )
                }
            }
        }
    }

    // Detail form lives in a bottom sheet
    if (kind != null) {
        val sheetMotion = LocalMotion.current
        ModalBottomSheet(
            onDismissRequest = { selected = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // imePadding before the scroll: the keyboard shrinks the sheet, and Compose's
                    // own bring-into-view then scrolls the focused field into what is left, instead
                    // of the field ending up behind the keyboard.
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        Icon(kind.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(8.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(stringResource(kind.labelRes), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                        Text(stringResource(kind.helperRes), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                MoneyField(
                    label = stringResource(R.string.entry_amount),
                    value = amount,
                    supportingText = {
                        val parsedAmt = amount.toLongOrNull()
                        val parsedQty = TransactionCalculation.parseQuantity(quantity)
                        val parsedPrice = unitPrice.toLongOrNull()
                        if (isAutoCalculated && parsedQty != null && parsedPrice != null && parsedPrice > 0 && parsedAmt != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    Icons.Default.Calculate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp),
                                )
                                Text(
                                    stringResource(
                                        R.string.entry_auto_calculated,
                                        quantity,
                                        unit.ifBlank { stringResource(R.string.entry_unit_default) },
                                        parsedPrice.toRupiah(),
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        } else if (parsedAmt != null) {
                            Text(parsedAmt.toRupiah())
                        } else if (amount.isNotBlank()) {
                            Text(stringResource(R.string.entry_amount_invalid), color = MaterialTheme.colorScheme.error)
                        }
                    },
                    onValueChange = { raw ->
                        val clean = sanitizeRupiah(raw)
                        amount = clean
                        isAutoCalculated = false
                        val parsedAmt = clean.toLongOrNull()
                        val parsedQty = TransactionCalculation.parseQuantity(quantity)
                        if (parsedQty != null && parsedAmt != null && parsedAmt > 0) {
                            val derived = TransactionCalculation.computeUnitPriceRupiah(parsedAmt, parsedQty)
                            unitPrice = derived.toString()
                            isUnitPriceAutoDerived = true
                        } else if (parsedAmt == null || parsedAmt == 0L) {
                            if (isUnitPriceAutoDerived) {
                                unitPrice = ""
                                isUnitPriceAutoDerived = false
                            }
                        }
                    },
                )

                // Quick Amount Chips
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val increments = listOf(
                        "+50 rb" to 50_000L,
                        "+100 rb" to 100_000L,
                        "+500 rb" to 500_000L,
                        "+1 jt" to 1_000_000L,
                        "+5 jt" to 5_000_000L,
                    )
                    items(increments) { (label, value) ->
                        androidx.compose.material3.SuggestionChip(
                            onClick = {
                                val current = amount.toLongOrNull() ?: 0L
                                val newAmt = current + value
                                amount = sanitizeRupiah(newAmt.toString())
                                isAutoCalculated = false
                                val parsedQty = TransactionCalculation.parseQuantity(quantity)
                                if (parsedQty != null && newAmt > 0) {
                                    val derived = TransactionCalculation.computeUnitPriceRupiah(newAmt, parsedQty)
                                    unitPrice = derived.toString()
                                    isUnitPriceAutoDerived = true
                                }
                            },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                        )
                    }
                    item {
                        androidx.compose.material3.SuggestionChip(
                            onClick = {
                                amount = ""
                                isAutoCalculated = false
                                if (isUnitPriceAutoDerived) {
                                    unitPrice = ""
                                    isUnitPriceAutoDerived = false
                                }
                            },
                            label = { Text(stringResource(R.string.entry_clear), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error) },
                        )
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.entry_description)) },
                    placeholder = {
                        Text(
                            if (selected == FinancialEventType.CASH_INCOME) {
                                stringResource(R.string.entry_hint_income)
                            } else {
                                stringResource(R.string.entry_hint_expense)
                            },
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                ChoiceMenu(
                    label = compatibleCategories.firstOrNull { it.id == categoryId }?.name
                        ?: stringResource(R.string.entry_category_pick),
                    options = compatibleCategories.map { it.id to it.name },
                    onSelected = { categoryId = it },
                )
                DateField(
                    value = date,
                    label = stringResource(R.string.entry_date),
                    onValueChange = { date = it },
                )
                if (kind.credit) {
                    OutlinedTextField(
                        value = counterparty,
                        onValueChange = { counterparty = it },
                        label = { Text(stringResource(R.string.entry_counterparty)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    DateField(
                        value = dueDate,
                        label = stringResource(R.string.entry_due),
                        clearable = true,
                        onValueChange = { dueDate = it },
                    )
                }
                TextButton(onClick = { advanced = !advanced }) {
                    Text(stringResource(if (advanced) R.string.entry_hide_detail else R.string.entry_show_detail))
                }
                AnimatedVisibility(
                    visible = advanced,
                    enter = expandVertically(tween(maxOf(sheetMotion.mediumMillis, 1), easing = FastOutSlowInEasing)) +
                        fadeIn(tween(maxOf(sheetMotion.mediumMillis, 1))),
                    exit = shrinkVertically(tween(maxOf(sheetMotion.fastMillis, 1), easing = FastOutSlowInEasing)) +
                        fadeOut(tween(maxOf(sheetMotion.fastMillis, 1))),
                ) {
                  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    OutlinedTextField(value = reference, onValueChange = { reference = it }, label = { Text(stringResource(R.string.entry_reference)) }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { raw ->
                                val clean = sanitizeDecimal(raw)
                                quantity = clean
                                val parsedQty = TransactionCalculation.parseQuantity(clean)
                                val parsedPrice = unitPrice.toLongOrNull()
                                val parsedAmt = amount.toLongOrNull()
                                if (parsedQty != null && parsedPrice != null && parsedPrice > 0) {
                                    val calculated = TransactionCalculation.computeTotalRupiah(parsedQty, parsedPrice)
                                    amount = calculated.toString()
                                    isAutoCalculated = true
                                    isUnitPriceAutoDerived = false
                                } else if (parsedQty != null && parsedAmt != null && parsedAmt > 0 && (parsedPrice == null || parsedPrice == 0L || isUnitPriceAutoDerived)) {
                                    val derived = TransactionCalculation.computeUnitPriceRupiah(parsedAmt, parsedQty)
                                    unitPrice = derived.toString()
                                    isUnitPriceAutoDerived = true
                                    isAutoCalculated = false
                                } else if (parsedQty == null) {
                                    if (isAutoCalculated) isAutoCalculated = false
                                    if (isUnitPriceAutoDerived) {
                                        unitPrice = ""
                                        isUnitPriceAutoDerived = false
                                    }
                                }
                            },
                            label = { Text(stringResource(R.string.entry_quantity)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            label = { Text(stringResource(R.string.entry_unit)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    MoneyField(
                        label = stringResource(R.string.entry_unit_price),
                        value = unitPrice,
                        supportingText = {
                            val parsedPrice = unitPrice.toLongOrNull()
                            val parsedQty = TransactionCalculation.parseQuantity(quantity)
                            val parsedAmt = amount.toLongOrNull()
                            if (isUnitPriceAutoDerived && parsedQty != null && parsedAmt != null && parsedPrice != null) {
                                Text(
                                    stringResource(
                                        R.string.entry_auto_unit_price,
                                        parsedPrice.toRupiah(),
                                        unit.ifBlank { stringResource(R.string.entry_unit_default) },
                                    ),
                                    color = MaterialTheme.colorScheme.tertiary,
                                    fontWeight = FontWeight.Medium,
                                )
                            } else if (parsedPrice != null && unit.isNotBlank()) {
                                Text("${parsedPrice.toRupiah()} / $unit")
                            }
                        },
                        onValueChange = { raw ->
                            val clean = sanitizeRupiah(raw)
                            unitPrice = clean
                            isUnitPriceAutoDerived = false
                            val parsedPrice = clean.toLongOrNull()
                            val parsedQty = TransactionCalculation.parseQuantity(quantity)
                            if (parsedQty != null && parsedPrice != null && parsedPrice > 0) {
                                val calculated = TransactionCalculation.computeTotalRupiah(parsedQty, parsedPrice)
                                amount = calculated.toString()
                                isAutoCalculated = true
                            } else if (parsedPrice == null || parsedPrice == 0L) {
                                if (isAutoCalculated) isAutoCalculated = false
                            }
                        },
                    )
                    if (state.budgetItems.isNotEmpty()) {
                        ChoiceMenu(
                            label = state.budgetItems.firstOrNull { it.id == budgetItemId }?.name
                                ?: stringResource(R.string.entry_link_plan),
                            options = listOf("" to stringResource(R.string.entry_link_none)) + state.budgetItems.map { it.id to it.name },
                            onSelected = { budgetItemId = it.ifBlank { null } },
                        )
                    }
                  }
                }
                localError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
                Button(
                    onClick = {
                        if (state.selectedProjectId == null) {
                            localError = errNoProject
                            return@Button
                        }
                        val parsedAmount = amount.toLongOrNull()
                        val parsedDate = runCatching { LocalDate.parse(date) }.getOrNull()
                        val parsedDue = dueDate.takeIf(String::isNotBlank)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                        val parsedQuantity = quantity.takeIf(String::isNotBlank)?.let { TransactionCalculation.parseQuantity(it) }
                        val category = compatibleCategories.firstOrNull { it.id == categoryId }
                        localError = when {
                            parsedAmount == null || parsedAmount <= 0 -> errAmount
                            description.isBlank() -> errDescription
                            parsedDate == null -> errDate
                            category == null -> errCategory
                            kind.credit && counterparty.isBlank() -> errCounterparty
                            dueDate.isNotBlank() && parsedDue == null -> errDue
                            quantity.isNotBlank() && parsedQuantity == null -> errQuantity
                            else -> null
                        }
                        if (localError == null && parsedAmount != null && parsedDate != null && category != null) {
                            val reconciled = TransactionCalculation.reconcile(
                                amountRupiah = parsedAmount,
                                quantityText = quantity.ifBlank { null },
                                unitPriceRupiah = unitPrice.toLongOrNull(),
                            )
                            onRecord(
                                kind.type,
                                reconciled.totalRupiah,
                                description,
                                category.id,
                                category.name,
                                parsedDate,
                                counterparty.ifBlank { null },
                                parsedDue,
                                reference.ifBlank { null },
                                reconciled.quantityText,
                                unit.ifBlank { null },
                                reconciled.unitPriceRupiah,
                                budgetItemId,
                            )
                            amount = ""
                            description = ""
                            counterparty = ""
                            dueDate = ""
                            reference = ""
                            quantity = ""
                            unit = ""
                            unitPrice = ""
                            budgetItemId = null
                            isAutoCalculated = false
                            isUnitPriceAutoDerived = false
                            closeSheet()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Text(stringResource(R.string.entry_save), fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

/**
 * Full-width, high-contrast button for the two kinds a business owner records almost every day. Uses a
 * filled colour block, a large icon and a shadow so it reads unmistakably as a control — the
 * previous flat card looked like a static tile to the owner. Colour follows the money direction
 * (green in, coral out) so the choice is recognisable before reading the label.
 */
@Composable
private fun PrimaryKindButton(
    kind: EntryKind,
    selected: Boolean,
    incoming: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val semantic = LocalMankeuSemanticColors.current
    val accent = if (incoming) semantic.success else semantic.danger
    val container = if (selected) accent else if (incoming) semantic.successContainer else semantic.dangerContainer
    val contentColor = if (selected) {
        if (incoming) semantic.onSuccess else semantic.onDanger
    } else {
        if (incoming) semantic.onSuccessContainer else semantic.onDangerContainer
    }
    Surface(
        onClick = onClick,
        modifier = modifier.height(112.dp),
        shape = MaterialTheme.shapes.medium,
        color = container,
        contentColor = contentColor,
        shadowElevation = if (selected) 8.dp else 2.dp,
        border = if (selected) null else androidx.compose.foundation.BorderStroke(1.5.dp, accent.copy(alpha = 0.45f)),
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(kind.icon, contentDescription = null, modifier = Modifier.size(28.dp))
            Text(
                stringResource(kind.labelRes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun EntryKindCard(item: EntryKind, selected: Boolean, onClick: () -> Unit) {
    Card(
        // Fixed height, not intrinsic: these live in a LazyRow, which measures each child on its
        // own, so a kind whose helper text wraps to two lines used to stand taller than the rest.
        // The height fits the longest helper at the largest supported font scale.
        modifier = Modifier.width(176.dp).height(168.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        ),
        border = if (selected) CardDefaults.outlinedCardBorder() else null,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background,
            ) {
                Icon(
                    item.icon,
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(8.dp),
                )
            }
            Text(stringResource(item.labelRes), fontWeight = FontWeight.Black, maxLines = 2)
            Text(
                stringResource(item.helperRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
