package com.arinahub.umkm.mankeu.presentation.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arinahub.umkm.mankeu.data.ObligationView
import com.arinahub.umkm.mankeu.domain.AnalysisAssumptions
import com.arinahub.umkm.mankeu.domain.BookMode
import com.arinahub.umkm.mankeu.domain.BusinessAnalysisCalculator
import com.arinahub.umkm.mankeu.domain.MonthlyCashFlowCalculator
import com.arinahub.umkm.mankeu.domain.ObligationKind
import com.arinahub.umkm.mankeu.domain.ObligationStatus
import com.arinahub.umkm.mankeu.R
import com.arinahub.umkm.mankeu.presentation.AppUiState
import com.arinahub.umkm.mankeu.presentation.LocalArinaSemanticColors
import com.arinahub.umkm.mankeu.presentation.LocalMotion
import com.arinahub.umkm.mankeu.presentation.appLocale
import com.arinahub.umkm.mankeu.presentation.components.ArinaSheet
import com.arinahub.umkm.mankeu.presentation.components.ComparisonRow
import com.arinahub.umkm.mankeu.presentation.components.DateField
import com.arinahub.umkm.mankeu.presentation.components.MoneyField
import com.arinahub.umkm.mankeu.presentation.components.sanitizeDecimal
import com.arinahub.umkm.mankeu.presentation.components.EmptyCard
import com.arinahub.umkm.mankeu.presentation.components.MetricTone
import com.arinahub.umkm.mankeu.presentation.components.ReportCard
import com.arinahub.umkm.mankeu.presentation.components.TourTarget
import com.arinahub.umkm.mankeu.presentation.components.tourTarget
import com.arinahub.umkm.mankeu.presentation.components.ReportRow
import com.arinahub.umkm.mankeu.presentation.components.SectionTitle
import com.arinahub.umkm.mankeu.presentation.components.SimpleMetric
import com.arinahub.umkm.mankeu.presentation.components.SimpleMetricGrid
import com.arinahub.umkm.mankeu.presentation.toDisplayDate
import com.arinahub.umkm.mankeu.presentation.toRupiah
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private enum class ReportSection(val labelRes: Int) {
    FINANCIALS(R.string.reports_tab_financials),
    FEASIBILITY(R.string.reports_tab_feasibility),
    COMPARISON(R.string.reports_tab_comparison),
}

/**
 * The Laporan (reports) destination. Every number here is re-derived from posted journal
 * entries (see `docs/DOMAIN_MODEL.md`) — this screen never edits source data, only presents it.
 */
@Composable
fun ReportsScreen(
    state: AppUiState,
    onSettle: (String, Long, LocalDate, String) -> Unit,
    onSaveAssumptions: (Long, Long, Long, Int, Long, String?, String?, Long?) -> Unit,
    onStartTour: (() -> Unit)? = null,
) {
    var section by rememberSaveable { mutableStateOf(ReportSection.FINANCIALS) }
    var settlement by remember { mutableStateOf<ObligationView?>(null) }
    var assumptionsOpen by remember { mutableStateOf(false) }
    val analysisInputs = state.assumptions?.let {
        AnalysisAssumptions(
            openingCashRupiah = it.openingCashRupiah,
            ownerCapitalRupiah = it.ownerCapitalRupiah,
            loanAmountRupiah = it.loanAmountRupiah,
            interestRateBasisPoints = it.interestRateBasisPoints,
            otherFinancingCostRupiah = it.otherFinancingCostRupiah,
            outputQuantityText = it.outputQuantityText,
            outputUnit = it.outputUnit,
            outputPriceRupiah = it.outputPriceRupiah,
        )
    } ?: AnalysisAssumptions()
    val monthlyCashFlow = MonthlyCashFlowCalculator.calculate(state.events, analysisInputs.openingCashRupiah)
    val analysis = BusinessAnalysisCalculator.calculate(state.summary, analysisInputs, monthlyCashFlow)

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
                    title = stringResource(R.string.reports_title),
                    subtitle = stringResource(R.string.reports_subtitle),
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
        item {
            PrimaryTabRow(
                selectedTabIndex = ReportSection.entries.indexOf(section),
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth().tourTarget(TourTarget.REPORTS_TABS),
            ) {
                ReportSection.entries.forEach { item ->
                    Tab(
                        selected = section == item,
                        onClick = { section = item },
                        text = {
                            Text(
                                stringResource(item.labelRes),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (section == item) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                    )
                }
            }
        }

        // One AnimatedContent for the whole section body, so switching tab slides the new
        // report in rather than swapping it in place. It has to be a single LazyColumn item:
        // items are emitted into the list scope and cannot themselves be wrapped in a transition.
        // The sections are short enough to compose eagerly; the ledger-length lists all live on
        // Beranda, not here.
        item {
            val motion = LocalMotion.current
            AnimatedContent(
                targetState = section,
                transitionSpec = {
                    val forward = ReportSection.entries.indexOf(targetState) >= ReportSection.entries.indexOf(initialState)
                    val enterMillis = maxOf(motion.mediumMillis, 1)
                    val exitMillis = maxOf(motion.fastMillis, 1)
                    (slideInHorizontally(tween(enterMillis, easing = FastOutSlowInEasing)) { full ->
                        if (forward) full / 8 else -full / 8
                    } + fadeIn(tween(enterMillis)))
                        .togetherWith(fadeOut(tween(exitMillis)))
                        .using(SizeTransform(clip = false))
                },
                label = "reportSection",
            ) { current ->
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    when (current) {
                        ReportSection.FINANCIALS -> FinancialsSection(state, monthlyCashFlow)
                        ReportSection.FEASIBILITY -> FeasibilitySection(analysisInputs, analysis) { assumptionsOpen = true }
                        ReportSection.COMPARISON -> ComparisonSection(state) { settlement = it }
                    }
                }
            }
        }
    }
    settlement?.let { obligation ->
        SettlementDialog(obligation, { settlement = null }) { amount, date, description ->
            onSettle(obligation.id, amount, date, description)
            settlement = null
        }
    }
    if (assumptionsOpen) {
        AssumptionsDialog(state, { assumptionsOpen = false }) { opening, capital, loan, interest, other, quantity, unit, price ->
            onSaveAssumptions(opening, capital, loan, interest, other, quantity, unit, price)
            assumptionsOpen = false
        }
    }
}

@Composable
private fun FinancialsSection(
    state: AppUiState,
    monthlyCashFlow: List<com.arinahub.umkm.mankeu.domain.MonthlyCashFlow>,
) {
    val cashOperatingTone = when {
        state.summary.operatingCashFlowRupiah < 0 -> MetricTone.DANGER
        state.summary.operatingCashFlowRupiah > 0 -> MetricTone.SUCCESS
        else -> MetricTone.PRIMARY
    }
    val netProfitTone = when {
        state.summary.profitRupiah < 0 -> MetricTone.DANGER
        state.summary.profitRupiah > 0 -> MetricTone.SUCCESS
        else -> MetricTone.PRIMARY
    }
    SimpleMetricGrid(
        listOf(
            // Was cashBalanceRupiah under a label that says operating cash flow — a different figure,
            // and the closing balance is already shown as its own row below. Identical today only
            // because this book has no financing or investing rows; the tile would have contradicted
            // its own label the first time an owner recorded capital or bought a tool.
            SimpleMetric(stringResource(R.string.reports_cash_operating), state.summary.operatingCashFlowRupiah, Icons.Default.AccountBalance, cashOperatingTone),
            SimpleMetric(stringResource(R.string.reports_net_profit), state.summary.profitRupiah, Icons.Default.Assessment, netProfitTone),
            SimpleMetric(stringResource(R.string.reports_receivable), state.summary.receivableOutstandingRupiah, Icons.Default.ArrowDownward, MetricTone.SUCCESS),
            SimpleMetric(stringResource(R.string.reports_payable), state.summary.payableOutstandingRupiah, Icons.Default.ArrowUpward, MetricTone.DANGER),
        ),
    )
    ReportCard(stringResource(R.string.reports_income_statement), Modifier.tourTarget(TourTarget.REPORTS_PROFIT)) {
        ReportRow(stringResource(R.string.reports_operating_income), state.summary.operatingIncomeRupiah)
        ReportRow(stringResource(R.string.reports_operating_expense), -state.summary.operatingExpenseRupiah)
        HorizontalDivider()
        ReportRow(stringResource(R.string.reports_net_profit), state.summary.profitRupiah, emphasized = true)
        Text(
            stringResource(R.string.reports_income_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    ReportCard(stringResource(R.string.reports_cash_flow), Modifier.tourTarget(TourTarget.REPORTS_CASHFLOW)) {
        ReportRow(stringResource(R.string.reports_cf_operating), state.summary.operatingCashFlowRupiah)
        ReportRow(stringResource(R.string.reports_cf_investing), state.summary.investingCashFlowRupiah)
        ReportRow(stringResource(R.string.reports_cf_financing), state.summary.financingCashFlowRupiah)
        HorizontalDivider()
        ReportRow(stringResource(R.string.reports_cf_closing), state.summary.cashBalanceRupiah, emphasized = true)
    }
    if (monthlyCashFlow.isNotEmpty()) {
        SectionTitle(stringResource(R.string.reports_monthly_title), stringResource(R.string.reports_monthly_subtitle))
        val locale = appLocale()
        monthlyCashFlow.forEach { month ->
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(month.month.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale)), fontWeight = FontWeight.Black)
                    ReportRow(stringResource(R.string.reports_monthly_operating), month.operatingRupiah)
                    ReportRow(stringResource(R.string.reports_monthly_investing), month.investingRupiah)
                    ReportRow(stringResource(R.string.reports_monthly_financing), month.financingRupiah)
                    HorizontalDivider()
                    ReportRow(stringResource(R.string.reports_monthly_net), month.netRupiah, emphasized = true)
                    ReportRow(stringResource(R.string.reports_monthly_cumulative), month.cumulativeRupiah, emphasized = true)
                }
            }
        }
    }
}

@Composable
private fun FeasibilitySection(
    analysisInputs: AnalysisAssumptions,
    analysis: com.arinahub.umkm.mankeu.domain.BusinessAnalysis,
    onEditAssumptions: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.reports_financing_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                TextButton(onClick = onEditAssumptions) { Icon(Icons.Default.Edit, null); Text(" " + stringResource(R.string.reports_edit_assumptions)) }
            }
            ReportRow(stringResource(R.string.reports_opening_cash), analysisInputs.openingCashRupiah)
            ReportRow(stringResource(R.string.reports_financing_gap), analysis.financingGapRupiah, emphasized = true)
            ReportRow(stringResource(R.string.reports_own_capital), analysisInputs.ownerCapitalRupiah)
            ReportRow(stringResource(R.string.reports_loan_plan), analysisInputs.loanAmountRupiah)
            ReportRow(stringResource(R.string.reports_interest_estimate, (analysisInputs.interestRateBasisPoints / 100).toString()), -analysis.estimatedInterestRupiah)
            ReportRow(stringResource(R.string.reports_other_financing), -analysisInputs.otherFinancingCostRupiah)
            HorizontalDivider()
            ReportRow(stringResource(R.string.reports_cash_after_financing), analysis.endingCashAfterAssumptionsRupiah, emphasized = true)
        }
    }
    val unitFallback = stringResource(R.string.reports_unit_fallback)
    val undefined = stringResource(R.string.reports_undefined)
    ReportCard(stringResource(R.string.reports_feasibility_title), Modifier.tourTarget(TourTarget.REPORTS_FEASIBILITY)) {
        if (analysis.estimatedRevenueRupiah != null) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.reports_output_estimate), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "${analysisInputs.outputQuantityText} ${analysisInputs.outputUnit.orEmpty()} @ ${analysisInputs.outputPriceRupiah?.toRupiah() ?: "-"}",
                    fontWeight = FontWeight.Bold,
                )
            }
            ReportRow(stringResource(R.string.reports_revenue_estimate), analysis.estimatedRevenueRupiah)
            ReportRow(stringResource(R.string.reports_total_cost), analysis.totalCostRupiah)
            analysis.costPerOutputRupiah?.let {
                ReportRow(stringResource(R.string.reports_cost_per_output, analysis.outputUnit.orEmpty().ifBlank { unitFallback }), it)
            }
            HorizontalDivider()
            analysis.grossProfitRupiah?.let {
                ReportRow(stringResource(R.string.reports_gross_profit), it, emphasized = true)
            }
            analysis.grossProfitMarginPercent?.let {
                Text(
                    stringResource(R.string.reports_gross_profit_margin, it.stripTrailingZeros().toPlainString()),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            analysis.netProfitMarginPercent?.let {
                Text(
                    stringResource(R.string.reports_net_profit_margin, it.stripTrailingZeros().toPlainString()),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider()
            analysis.breakEvenRevenueRupiah?.let {
                Text(
                    stringResource(R.string.reports_bep_revenue, it.toRupiah()),
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                stringResource(
                    R.string.reports_break_even,
                    analysis.breakEvenQuantity?.stripTrailingZeros()?.toPlainString() ?: undefined,
                    analysis.outputUnit.orEmpty(),
                ),
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(
                    R.string.reports_bc_ratio,
                    analysis.benefitCostRatio?.stripTrailingZeros()?.toPlainString() ?: undefined,
                ),
                fontWeight = FontWeight.Bold,
            )
            val semantic = LocalArinaSemanticColors.current
            val feasibility = when (analysis.feasible) {
                true -> stringResource(R.string.reports_feasible_yes)
                false -> stringResource(R.string.reports_feasible_no)
                null -> stringResource(R.string.reports_feasible_unknown)
            }
            val feasibleColors = when (analysis.feasible) {
                true -> AssistChipDefaults.assistChipColors(containerColor = semantic.successContainer, labelColor = semantic.onSuccessContainer)
                false -> AssistChipDefaults.assistChipColors(containerColor = semantic.dangerContainer, labelColor = semantic.onDangerContainer)
                null -> AssistChipDefaults.assistChipColors()
            }
            AssistChip(onClick = {}, label = { Text(feasibility, fontWeight = FontWeight.Black) }, colors = feasibleColors)
        } else {
            Text(stringResource(R.string.reports_feasibility_missing), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onEditAssumptions, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.reports_feasibility_cta))
            }
        }
    }
}

@Composable
private fun ComparisonSection(state: AppUiState, onSettle: (ObligationView) -> Unit) {
    ReportCard(stringResource(R.string.reports_comparison_title)) {
        ComparisonRow(stringResource(R.string.reports_budget_total), state.projectionBudgetTotalRupiah, state.realizationBudgetTotalRupiah, higherIsBetter = false)
        ComparisonRow(stringResource(R.string.reports_income), state.projectionSummary.operatingIncomeRupiah, state.realizationSummary.operatingIncomeRupiah)
        ComparisonRow(stringResource(R.string.reports_operating_expense), state.projectionSummary.operatingExpenseRupiah, state.realizationSummary.operatingExpenseRupiah, higherIsBetter = false)
        ComparisonRow(stringResource(R.string.reports_profit_loss), state.projectionSummary.profitRupiah, state.realizationSummary.profitRupiah)
        ComparisonRow(stringResource(R.string.reports_cash_balance), state.projectionSummary.cashBalanceRupiah, state.realizationSummary.cashBalanceRupiah)
        ComparisonRow(stringResource(R.string.reports_receivable), state.projectionSummary.receivableOutstandingRupiah, state.realizationSummary.receivableOutstandingRupiah, higherIsBetter = false)
        ComparisonRow(stringResource(R.string.reports_payable), state.projectionSummary.payableOutstandingRupiah, state.realizationSummary.payableOutstandingRupiah, higherIsBetter = false)
    }
    SectionTitle(stringResource(R.string.reports_obligations_title), stringResource(R.string.reports_obligations_subtitle))
    if (state.obligations.isEmpty()) {
        EmptyCard(stringResource(R.string.reports_obligations_empty_title), stringResource(R.string.reports_obligations_empty_body))
    } else {
        state.obligations.forEach { obligation ->
            ObligationCard(obligation) { onSettle(obligation) }
        }
    }
}

@Composable
private fun ObligationCard(obligation: ObligationView, onSettle: () -> Unit) {
    val semantic = LocalArinaSemanticColors.current
    val tone = if (obligation.kind == ObligationKind.RECEIVABLE) semantic.success to semantic.successContainer else semantic.danger to semantic.dangerContainer
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(14.dp), color = tone.second) {
                    Icon(
                        if (obligation.kind == ObligationKind.RECEIVABLE) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = tone.first,
                        modifier = Modifier.padding(8.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(obligation.counterparty, fontWeight = FontWeight.Black)
                    Text(obligation.description, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                }
                StatusChip(obligation.status)
            }
            ReportRow(stringResource(R.string.obligation_original), obligation.originalAmountRupiah)
            ReportRow(stringResource(R.string.obligation_settled), obligation.settledAmountRupiah)
            ReportRow(stringResource(R.string.obligation_outstanding), obligation.outstandingAmountRupiah, emphasized = true)
            obligation.dueDate?.let {
                Text(stringResource(R.string.obligation_due, it.toDisplayDate()), style = MaterialTheme.typography.bodySmall)
            }
            if (obligation.outstandingAmountRupiah > 0 && obligation.status != ObligationStatus.REVERSED) {
                Button(onClick = onSettle, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(
                            if (obligation.kind == ObligationKind.RECEIVABLE) R.string.obligation_record_receipt
                            else R.string.obligation_record_payment,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: ObligationStatus) {
    val semantic = LocalArinaSemanticColors.current
    val label = stringResource(
        when (status) {
            ObligationStatus.OPEN -> R.string.obligation_status_open
            ObligationStatus.PARTIAL -> R.string.obligation_status_partial
            ObligationStatus.PAID -> R.string.obligation_status_paid
            ObligationStatus.OVERDUE -> R.string.obligation_status_overdue
            ObligationStatus.REVERSED -> R.string.obligation_status_reversed
        },
    )
    val colors = when (status) {
        ObligationStatus.PAID -> AssistChipDefaults.assistChipColors(containerColor = semantic.successContainer, labelColor = semantic.onSuccessContainer)
        ObligationStatus.OVERDUE -> AssistChipDefaults.assistChipColors(containerColor = semantic.dangerContainer, labelColor = semantic.onDangerContainer)
        ObligationStatus.PARTIAL -> AssistChipDefaults.assistChipColors(containerColor = semantic.warningContainer, labelColor = semantic.onWarningContainer)
        else -> AssistChipDefaults.assistChipColors()
    }
    AssistChip(onClick = {}, label = { Text(label, fontWeight = FontWeight.Bold) }, colors = colors)
}

@Composable
private fun SettlementDialog(obligation: ObligationView, onDismiss: () -> Unit, onSave: (Long, LocalDate, String) -> Unit) {
    val receivable = obligation.kind == ObligationKind.RECEIVABLE
    var amount by rememberSaveable { mutableStateOf(obligation.outstandingAmountRupiah.toString()) }
    var date by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var description by rememberSaveable {
        mutableStateOf("")
    }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    // Resolved here because stringResource is @Composable and the validation runs in onClick.
    val defaultDescription = stringResource(
        if (receivable) R.string.settle_default_receipt else R.string.settle_default_payment,
        obligation.counterparty,
    )
    val errAmount = stringResource(R.string.settle_err_amount)
    val errOver = stringResource(R.string.settle_err_over)
    if (description.isEmpty()) description = defaultDescription

    ArinaSheet(
        title = stringResource(if (receivable) R.string.settle_receivable_title else R.string.settle_payable_title),
        onDismiss = onDismiss,
        onConfirm = {
            val parsedAmount = amount.toLongOrNull()
            val parsedDate = runCatching { LocalDate.parse(date) }.getOrNull()
            error = when {
                parsedAmount == null || parsedAmount <= 0 -> errAmount
                parsedAmount > obligation.outstandingAmountRupiah -> errOver
                else -> null
            }
            if (error == null && parsedAmount != null && parsedDate != null) onSave(parsedAmount, parsedDate, description)
        },
    ) {
        Text(stringResource(R.string.settle_hint, obligation.outstandingAmountRupiah.toRupiah()))
        MoneyField(
            label = stringResource(R.string.settle_amount),
            value = amount,
            onValueChange = { amount = it },
        )
        DateField(
            value = date,
            label = stringResource(R.string.settle_date),
            onValueChange = { date = it },
        )
        OutlinedTextField(
            description,
            { description = it },
            label = { Text(stringResource(R.string.settle_description)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun AssumptionsDialog(
    state: AppUiState,
    onDismiss: () -> Unit,
    onSave: (Long, Long, Long, Int, Long, String?, String?, Long?) -> Unit,
) {
    val existing = state.assumptions
    var opening by rememberSaveable { mutableStateOf((existing?.openingCashRupiah ?: 0).toString()) }
    var capital by rememberSaveable { mutableStateOf((existing?.ownerCapitalRupiah ?: 0).toString()) }
    var loan by rememberSaveable { mutableStateOf((existing?.loanAmountRupiah ?: 0).toString()) }
    var interest by rememberSaveable { mutableStateOf(((existing?.interestRateBasisPoints ?: 0) / 100.0).toString()) }
    var other by rememberSaveable { mutableStateOf((existing?.otherFinancingCostRupiah ?: 0).toString()) }
    var quantity by rememberSaveable { mutableStateOf(existing?.outputQuantityText.orEmpty()) }
    var unit by rememberSaveable { mutableStateOf(existing?.outputUnit.orEmpty()) }
    var price by rememberSaveable { mutableStateOf(existing?.outputPriceRupiah?.toString().orEmpty()) }
    ArinaSheet(
        title = stringResource(
            if (state.selectedBookMode == BookMode.PROJECTION) R.string.assumptions_title_plan
            else R.string.assumptions_title_actual,
        ),
        onDismiss = onDismiss,
        onConfirm = {
            val basisPoints = ((interest.replace(',', '.').toDoubleOrNull() ?: 0.0) * 100).toInt()
            onSave(
                opening.toLongOrNull() ?: 0,
                capital.toLongOrNull() ?: 0,
                loan.toLongOrNull() ?: 0,
                basisPoints,
                other.toLongOrNull() ?: 0,
                quantity.ifBlank { null },
                unit.ifBlank { null },
                price.toLongOrNull(),
            )
        },
    ) {
        MoneyField(stringResource(R.string.assumptions_opening_cash), opening) { opening = it }
        MoneyField(stringResource(R.string.assumptions_own_capital), capital) { capital = it }
        MoneyField(stringResource(R.string.assumptions_loan), loan) { loan = it }
        OutlinedTextField(
            interest,
            { interest = sanitizeDecimal(it) },
            label = { Text(stringResource(R.string.assumptions_interest)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        MoneyField(stringResource(R.string.assumptions_other_cost), other) { other = it }
        HorizontalDivider()
        Text(stringResource(R.string.assumptions_output_header), fontWeight = FontWeight.Bold)
        OutlinedTextField(
            quantity,
            { quantity = sanitizeDecimal(it) },
            label = { Text(stringResource(R.string.assumptions_output_quantity)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            unit,
            { unit = it },
            label = { Text(stringResource(R.string.assumptions_output_unit)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        MoneyField(stringResource(R.string.assumptions_output_price), price) { price = it }
    }
}
