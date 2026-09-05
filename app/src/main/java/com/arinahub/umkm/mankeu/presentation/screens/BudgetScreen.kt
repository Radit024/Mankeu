package com.arinahub.umkm.mankeu.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.arinahub.umkm.mankeu.R
import com.arinahub.umkm.mankeu.data.BudgetItemEntity
import com.arinahub.umkm.mankeu.data.CategoryEntity
import com.arinahub.umkm.mankeu.domain.BookMode
import com.arinahub.umkm.mankeu.domain.CategoryRole
import com.arinahub.umkm.mankeu.domain.TransactionCalculation
import com.arinahub.umkm.mankeu.presentation.AppUiState
import com.arinahub.umkm.mankeu.presentation.ChoiceMenu
import com.arinahub.umkm.mankeu.presentation.LocalArinaSemanticColors
import com.arinahub.umkm.mankeu.presentation.components.ArinaSheet
import com.arinahub.umkm.mankeu.presentation.components.EmptyCard
import com.arinahub.umkm.mankeu.presentation.components.MetricTone
import com.arinahub.umkm.mankeu.presentation.components.DateField
import com.arinahub.umkm.mankeu.presentation.components.MoneyField
import com.arinahub.umkm.mankeu.presentation.components.SectionTitle
import com.arinahub.umkm.mankeu.presentation.components.sanitizeDecimal
import com.arinahub.umkm.mankeu.presentation.components.sanitizeRupiah
import com.arinahub.umkm.mankeu.presentation.components.SimpleMetric
import com.arinahub.umkm.mankeu.presentation.components.SimpleMetricGrid
import com.arinahub.umkm.mankeu.presentation.toDisplayDate
import com.arinahub.umkm.mankeu.presentation.toRupiah
import java.time.LocalDate

/**
 * The Anggaran (budget) destination: planned income/expense items for the active book
 * (Proyeksi or Realisasi), each item's category, quantity, and reference total.
 */
@Composable
fun BudgetScreen(
    state: AppUiState,
    onCreate: (String, String, String?, String?, Long?, Long?, LocalDate?) -> Unit,
    onUpdate: (String, String, String, String?, String?, Long?, Long?, LocalDate?) -> Unit,
    onArchive: (String) -> Unit,
) {
    var addOpen by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<BudgetItemEntity?>(null) }
    val categoriesById = state.categories.associateBy { it.id }
    val income = state.budgetItems.filter { categoriesById[it.categoryId]?.role == CategoryRole.REVENUE.name }.sumOf { it.plannedTotalRupiah }
    val expense = state.budgetItems.filter { categoriesById[it.categoryId]?.role != CategoryRole.REVENUE.name }.sumOf { it.plannedTotalRupiah }

    val otherLabel = stringResource(R.string.category_other)
    val groupedItems = remember(state.budgetItems, categoriesById, otherLabel) {
        state.budgetItems.groupBy { categoriesById[it.categoryId]?.name ?: otherLabel }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SectionTitle(
                    stringResource(R.string.budget_title),
                    stringResource(
                        if (state.selectedBookMode == BookMode.PROJECTION) R.string.budget_book_plan
                        else R.string.budget_book_actual,
                    ),
                    Modifier.weight(1f),
                )
                Button(onClick = { addOpen = true }) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.budget_add)) }
            }
        }
        item {
            SimpleMetricGrid(
                listOf(
                    SimpleMetric(stringResource(R.string.budget_total_income), income, Icons.Default.ArrowDownward, MetricTone.SUCCESS),
                    SimpleMetric(stringResource(R.string.budget_total_expense), expense, Icons.Default.ArrowUpward, MetricTone.DANGER),
                ),
            )
        }
        if (state.budgetItems.isEmpty()) {
            item { EmptyCard(
                    stringResource(R.string.budget_empty_title),
                    stringResource(R.string.budget_empty_body),
                    stringResource(R.string.budget_empty_action),
                ) { addOpen = true } }
        } else {
            groupedItems.forEach { (catName, itemsInGroup) ->
                val groupTotal = itemsInGroup.sumOf { it.plannedTotalRupiah }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            catName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            groupTotal.toRupiah(),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(itemsInGroup, key = { it.id }) { item ->
                    BudgetItemCard(item, categoriesById[item.categoryId], onEdit = { editing = item }, onArchive = onArchive)
                }
            }
        }
    }
    if (addOpen) {
        BudgetDialog(
            categories = state.categories.filter { it.role == CategoryRole.REVENUE.name || it.role == CategoryRole.EXPENSE.name || it.role == CategoryRole.ASSET.name },
            units = state.units.map { it.symbol },
            onDismiss = { addOpen = false },
            onSave = { category, name, quantity, unit, price, total, plannedDate ->
                onCreate(category, name, quantity, unit, price, total, plannedDate)
                addOpen = false
            },
        )
    }
    editing?.let { item ->
        BudgetDialog(
            categories = state.categories.filter { it.role == CategoryRole.REVENUE.name || it.role == CategoryRole.EXPENSE.name || it.role == CategoryRole.ASSET.name },
            units = state.units.map { it.symbol },
            onDismiss = { editing = null },
            initial = item,
            onSave = { category, name, quantity, unit, price, total, plannedDate ->
                onUpdate(item.id, category, name, quantity, unit, price, total, plannedDate)
                editing = null
            },
        )
    }
}

@Composable
private fun BudgetItemCard(item: BudgetItemEntity, category: CategoryEntity?, onEdit: () -> Unit, onArchive: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.name, fontWeight = FontWeight.Black)
                Text(
                    listOfNotNull(category?.name, item.quantityText?.let { q -> "$q ${item.unit.orEmpty()}".trim() }, item.plannedDateEpochDay?.let { LocalDate.ofEpochDay(it).toDisplayDate() }).joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (item.formulaDifferenceRupiah != 0L) {
                    val semantic = LocalArinaSemanticColors.current
                    Text(
                        stringResource(R.string.budget_formula_gap, item.formulaDifferenceRupiah.toRupiah()),
                        color = semantic.danger,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(item.plannedTotalRupiah.toRupiah(), fontWeight = FontWeight.Black)
                Row {
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.budget_edit_item, item.name)) }
                    IconButton(onClick = { onArchive(item.id) }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = stringResource(R.string.budget_archive_item, item.name))
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetDialog(
    categories: List<CategoryEntity>,
    units: List<String>,
    onDismiss: () -> Unit,
    initial: BudgetItemEntity? = null,
    onSave: (String, String, String?, String?, Long?, Long?, LocalDate?) -> Unit,
) {
    // Default to the first *expense* category, not simply the first in the list. Ordered by role,
    // that was "Alat dan Mesin" — an asset — which is the wrong opening guess for a budget line:
    // an RAB is overwhelmingly costs, and a wrong preselected category is the kind of error that
    // saves silently and only shows up later in a report.
    var categoryId by remember(initial?.id) {
        mutableStateOf(
            initial?.categoryId
                ?: categories.firstOrNull { it.role == CategoryRole.EXPENSE.name }?.id
                ?: categories.firstOrNull()?.id.orEmpty(),
        )
    }
    var name by remember(initial?.id) { mutableStateOf(initial?.name.orEmpty()) }
    var quantity by remember(initial?.id) { mutableStateOf(initial?.quantityText.orEmpty()) }
    var unit by remember(initial?.id) { mutableStateOf(initial?.unit ?: units.firstOrNull().orEmpty()) }
    var unitPrice by remember(initial?.id) { mutableStateOf(initial?.unitPriceRupiah?.toString().orEmpty()) }
    var total by remember(initial?.id) { mutableStateOf(initial?.plannedTotalRupiah?.toString().orEmpty()) }
    var date by remember(initial?.id) { mutableStateOf(initial?.plannedDateEpochDay?.let(LocalDate::ofEpochDay)?.toString().orEmpty()) }
    var isAutoCalculated by remember(initial?.id) { mutableStateOf(false) }
    var isUnitPriceAutoDerived by remember(initial?.id) { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val errCategory = stringResource(R.string.budget_err_category)
    val errName = stringResource(R.string.budget_err_name)
    val errAmount = stringResource(R.string.budget_err_amount)
    ArinaSheet(
        title = stringResource(if (initial == null) R.string.budget_dialog_add else R.string.budget_dialog_edit),
        onDismiss = onDismiss,
        onConfirm = {
            val parsedDate = date.takeIf(String::isNotBlank)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            val parsedTotal = total.toLongOrNull() ?: 0L
            val parsedQty = TransactionCalculation.parseQuantity(quantity)
            val parsedPrice = unitPrice.toLongOrNull()

            error = when {
                categoryId.isBlank() -> errCategory
                name.isBlank() -> errName
                parsedTotal <= 0L && (parsedQty == null || parsedPrice == null || parsedPrice <= 0L) -> errAmount
                else -> null
            }
            if (error == null) {
                val reconciled = TransactionCalculation.reconcile(
                    amountRupiah = parsedTotal,
                    quantityText = quantity.ifBlank { null },
                    unitPriceRupiah = unitPrice.toLongOrNull(),
                )
                onSave(
                    categoryId,
                    name,
                    reconciled.quantityText,
                    unit.ifBlank { null },
                    reconciled.unitPriceRupiah,
                    reconciled.totalRupiah.takeIf { it > 0 },
                    parsedDate,
                )
            }
        },
    ) {
        ChoiceMenu(label = categories.firstOrNull { it.id == categoryId }?.name ?: stringResource(R.string.entry_category_pick), fieldLabel = stringResource(R.string.entry_category), options = categories.map { it.id to it.name }, onSelected = { categoryId = it })
        OutlinedTextField(
            name,
            { name = it },
            label = { Text(stringResource(R.string.budget_field_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                quantity,
                { raw ->
                    val clean = sanitizeDecimal(raw)
                    quantity = clean
                    val parsedQty = TransactionCalculation.parseQuantity(clean)
                    val parsedPrice = unitPrice.toLongOrNull()
                    val parsedTotal = total.toLongOrNull()
                    if (parsedQty != null && parsedPrice != null && parsedPrice > 0) {
                        val calculated = TransactionCalculation.computeTotalRupiah(parsedQty, parsedPrice)
                        total = calculated.toString()
                        isAutoCalculated = true
                        isUnitPriceAutoDerived = false
                    } else if (parsedQty != null && parsedTotal != null && parsedTotal > 0 && (parsedPrice == null || parsedPrice == 0L || isUnitPriceAutoDerived)) {
                        val derived = TransactionCalculation.computeUnitPriceRupiah(parsedTotal, parsedQty)
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
                label = { Text(stringResource(R.string.budget_field_quantity)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                unit,
                { unit = it },
                label = { Text(stringResource(R.string.budget_field_unit)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        MoneyField(
            label = stringResource(R.string.budget_field_unit_price),
            value = unitPrice,
            supportingText = {
                val parsedPrice = unitPrice.toLongOrNull()
                val parsedQty = TransactionCalculation.parseQuantity(quantity)
                val parsedTotal = total.toLongOrNull()
                if (isUnitPriceAutoDerived && parsedQty != null && parsedTotal != null && parsedPrice != null) {
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
                    total = calculated.toString()
                    isAutoCalculated = true
                } else if (parsedPrice == null || parsedPrice == 0L) {
                    if (isAutoCalculated) isAutoCalculated = false
                }
            },
        )
        MoneyField(
            label = stringResource(R.string.budget_field_total),
            value = total,
            supportingText = {
                val parsedTotal = total.toLongOrNull()
                val parsedQty = TransactionCalculation.parseQuantity(quantity)
                val parsedPrice = unitPrice.toLongOrNull()
                if (isAutoCalculated && parsedQty != null && parsedPrice != null && parsedPrice > 0 && parsedTotal != null) {
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
                } else {
                    Text(stringResource(R.string.budget_field_total_help))
                }
            },
            onValueChange = { raw ->
                val clean = sanitizeRupiah(raw)
                total = clean
                isAutoCalculated = false
                val parsedTotal = clean.toLongOrNull()
                val parsedQty = TransactionCalculation.parseQuantity(quantity)
                if (parsedQty != null && parsedTotal != null && parsedTotal > 0) {
                    val derived = TransactionCalculation.computeUnitPriceRupiah(parsedTotal, parsedQty)
                    unitPrice = derived.toString()
                    isUnitPriceAutoDerived = true
                } else if (parsedTotal == null || parsedTotal == 0L) {
                    if (isUnitPriceAutoDerived) {
                        unitPrice = ""
                        isUnitPriceAutoDerived = false
                    }
                }
            },
        )
        DateField(
            value = date,
            label = stringResource(R.string.budget_field_date),
            clearable = true,
            onValueChange = { date = it },
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
