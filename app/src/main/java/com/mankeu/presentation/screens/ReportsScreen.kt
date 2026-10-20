package com.mankeu.presentation.screens

import com.mankeu.presentation.FeedbackEvent
import com.mankeu.presentation.LocalFeedback
import com.mankeu.presentation.components.MankeuTooltip

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import java.math.BigDecimal
import java.math.RoundingMode
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mankeu.data.toAnalysisAssumptions
import com.mankeu.domain.AnalysisAssumptions
import com.mankeu.domain.BookMode
import com.mankeu.domain.BusinessAnalysisCalculator
import com.mankeu.domain.MonthlyCashFlowCalculator
import com.mankeu.domain.ObligationKind
import com.mankeu.domain.ObligationStatus
import com.mankeu.R
import com.mankeu.presentation.AppUiState
import com.mankeu.presentation.LocalMankeuSemanticColors
import com.mankeu.presentation.LocalMotion
import com.mankeu.presentation.appLocale
import com.mankeu.presentation.components.MankeuSheet
import com.mankeu.presentation.components.ComparisonRow
import com.mankeu.presentation.components.DateField
import com.mankeu.presentation.components.MoneyField
import com.mankeu.presentation.components.sanitizeDecimal
import com.mankeu.presentation.components.EmptyCard
import com.mankeu.presentation.components.MetricTone
import com.mankeu.presentation.components.ReportCard
import com.mankeu.presentation.components.TourTarget
import com.mankeu.presentation.components.tourTarget
import com.mankeu.presentation.components.ReportRow
import com.mankeu.presentation.components.SectionTitle
import com.mankeu.presentation.toDisplayDate
import com.mankeu.domain.BudgetPlanCalculator
import com.mankeu.domain.TransactionCalculation
import com.mankeu.presentation.components.FeasibilityCard
import com.mankeu.presentation.toDisplayQuantity
import com.mankeu.presentation.toRupiah
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.mankeu.presentation.components.charts.MonthlyCashFlowChart

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
    onSaveAssumptions: (Long, Long, Long, String?, String?, Long?, Long?, Long?) -> Unit,
    onRecordTransaction: () -> Unit = {},
    isSaving: Boolean = false,
    onStartTour: (() -> Unit)? = null,
) {
    var section by rememberSaveable { mutableStateOf(ReportSection.FINANCIALS) }
    var assumptionsOpen by remember { mutableStateOf(false) }
    val analysisInputs = state.assumptions.toAnalysisAssumptions()
    // Same declared source class as Beranda: a plan book with a costed RAB and no transactions
    // reports from the RAB, and says so; anything else reports from postings. See
    // BudgetPlanCalculator.
    val planFromBudget = state.selectedBookMode == BookMode.PROJECTION &&
        state.eventCount == 0 && state.budgetItems.isNotEmpty()
    val planLines = state.planBudgetLines()
    val summary = if (planFromBudget) BudgetPlanCalculator.summarize(planLines) else state.summary
    val monthlyCashFlow = if (planFromBudget) {
        BudgetPlanCalculator.schedule(planLines, analysisInputs.openingCashRupiah)
    } else {
        MonthlyCashFlowCalculator.calculateFromRecords(state.cashFlowRecords, analysisInputs.openingCashRupiah)
    }
    val analysis = BusinessAnalysisCalculator.calculate(summary, analysisInputs, monthlyCashFlow)

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
            }
        }
        item {
            val feedback = LocalFeedback.current
            PrimaryTabRow(
                selectedTabIndex = ReportSection.entries.indexOf(section),
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth().tourTarget(TourTarget.REPORTS_TABS),
            ) {
                ReportSection.entries.forEach { item ->
                    Tab(
                        selected = section == item,
                        onClick = {
                            if (section != item) feedback(FeedbackEvent.TAP)
                            section = item
                        },
                        text = {
                            // Two lines, not one with an ellipsis: the tab names say which
                            // statement each section holds ("Laba Rugi dan Arus Kas"), and a name
                            // clipped to "Laba Rug…" would defeat the point of naming them at all.
                            // Fixed rather than scrollable so all three stay visible at once.
                            Text(
                                stringResource(item.labelRes),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (section == item) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                    )
                }
            }
        }

        // Nothing recorded and nothing planned: every card below would render a column of Rp0,
        // which reads as "your business made nothing" rather than "you have not told me anything
        // yet". Say which, and offer the one action that fixes it.
        if (!state.isLoading && state.eventCount == 0 && state.budgetItems.isEmpty()) {
            item {
                val planMode = state.selectedBookMode == BookMode.PROJECTION
                EmptyCard(
                    title = stringResource(
                        if (planMode) R.string.reports_empty_plan_title else R.string.reports_empty_title,
                    ),
                    body = stringResource(
                        if (planMode) R.string.reports_empty_plan_body else R.string.reports_empty_body,
                    ),
                    action = stringResource(if (planMode) R.string.reports_feasibility_cta else R.string.reports_empty_action),
                    onAction = if (planMode) { { assumptionsOpen = true } } else onRecordTransaction,
                )
            }
            return@LazyColumn
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
                        ReportSection.FINANCIALS -> FinancialsSection(state, summary, monthlyCashFlow)
                        ReportSection.FEASIBILITY -> FeasibilitySection(
                            bookMode = state.selectedBookMode,
                            analysisInputs = analysisInputs,
                            analysis = analysis,
                            summary = summary,
                            derivedFromBudget = planFromBudget,
                            onEditAssumptions = { assumptionsOpen = true },
                        )
                        ReportSection.COMPARISON -> ComparisonSection(state)
                    }
                }
            }
        }
    }
    if (assumptionsOpen) {
        AssumptionsDialog(state, isSaving, { assumptionsOpen = false }) { opening, capital, other, quantity, unit, price, fc, vc ->
            onSaveAssumptions(opening, capital, other, quantity, unit, price, fc, vc)
            assumptionsOpen = false
        }
    }
}

@Composable
private fun FinancialsSection(
    state: AppUiState,
    summary: com.mankeu.domain.FinancialSummary,
    monthlyCashFlow: List<com.mankeu.domain.MonthlyCashFlow>,
) {
    ReportCard(stringResource(R.string.reports_income_statement), Modifier.tourTarget(TourTarget.REPORTS_PROFIT)) {
        // The income-versus-expense donut is gone with the hand-rolled charts. Two slices is not a
        // composition — it is one comparison, and the three rows immediately below already state
        // it exactly, with the figures the owner has to reconcile against a workbook. The cost
        // *breakdown* donut on Beranda, which really does have a dozen slices, stays.
        ReportRow(stringResource(R.string.reports_operating_income), summary.operatingIncomeRupiah)
        ReportRow(stringResource(R.string.reports_operating_expense), -summary.operatingExpenseRupiah)
        HorizontalDivider()
        ReportRow(stringResource(R.string.reports_net_profit), summary.profitRupiah, emphasized = true)
        Text(
            stringResource(R.string.reports_income_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    ReportCard(stringResource(R.string.reports_cash_flow), Modifier.tourTarget(TourTarget.REPORTS_CASHFLOW)) {
        ReportRow(stringResource(R.string.reports_cf_operating), summary.operatingCashFlowRupiah)
        ReportRow(stringResource(R.string.reports_cf_investing), summary.investingCashFlowRupiah)
        ReportRow(stringResource(R.string.reports_cf_financing), summary.financingCashFlowRupiah)
        HorizontalDivider()
        ReportRow(stringResource(R.string.reports_cf_closing), summary.cashBalanceRupiah, emphasized = true)
    }
    if (monthlyCashFlow.isNotEmpty()) {
        SectionTitle(stringResource(R.string.reports_monthly_title), stringResource(R.string.reports_monthly_subtitle))
        val locale = appLocale()
        
        MonthlyCashFlowChart(
            months = monthlyCashFlow,
            modifier = Modifier.padding(bottom = 16.dp),
            locale = locale,
        )
        
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
    bookMode: BookMode,
    analysisInputs: AnalysisAssumptions,
    analysis: com.mankeu.domain.BusinessAnalysis,
    summary: com.mankeu.domain.FinancialSummary,
    derivedFromBudget: Boolean,
    onEditAssumptions: () -> Unit,
) {
    // Executive summary first: the three meters answer "should this go ahead", and the ledger-level
    // financing detail follows for whoever needs to check the arithmetic.
    FeasibilityCard(
        analysis = analysis,
        targetQuantity = TransactionCalculation.parseQuantity(analysisInputs.outputQuantityText),
        modifier = Modifier.tourTarget(TourTarget.REPORTS_FEASIBILITY),
        sourceNote = if (derivedFromBudget) stringResource(R.string.feasibility_source_budget) else null,
        onEditAssumptions = onEditAssumptions,
    )

    if (bookMode == BookMode.PROJECTION) {
        ReportCard(stringResource(R.string.reports_financing_title)) {
            ReportRow(stringResource(R.string.reports_opening_cash), analysisInputs.openingCashRupiah)
            ReportRow(stringResource(R.string.reports_own_capital), analysisInputs.ownerCapitalRupiah)
            ReportRow(stringResource(R.string.reports_other_financing), -analysisInputs.otherFinancingCostRupiah)
            HorizontalDivider()
            ReportRow(stringResource(R.string.reports_cash_after_financing), analysis.endingCashAfterAssumptionsRupiah, emphasized = true)
        }
    } else {
        // The assumed-financing card used to be shown here too, and it lied: on the realization
        // book the owner's capital and the loan are already recorded events, so adding the same
        // assumed figures on top produced a "cash after financing" that was neither the balance
        // nor a plan. Actuals get the actual financing movements instead.
        ReportCard(stringResource(R.string.reports_financing_actual_title)) {
            Text(
                stringResource(R.string.reports_financing_actual_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ReportRow(stringResource(R.string.reports_cf_operating), summary.operatingCashFlowRupiah)
            ReportRow(stringResource(R.string.reports_cf_investing), summary.investingCashFlowRupiah)
            ReportRow(stringResource(R.string.reports_financing_net), summary.financingCashFlowRupiah)
            HorizontalDivider()
            ReportRow(stringResource(R.string.reports_cf_closing), summary.cashBalanceRupiah, emphasized = true)
        }
    }

    ReportCard(stringResource(R.string.reports_working_capital)) {
        ReportRow(stringResource(R.string.reports_financing_gap), analysis.financingGapRupiah, emphasized = true)
        Text(
            stringResource(R.string.reports_working_capital_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    val unitFallback = stringResource(R.string.reports_unit_fallback)
    val undefined = stringResource(R.string.reports_undefined)
    ReportCard(stringResource(R.string.reports_feasibility_title)) {
        if (analysis.breakEvenQuantity != null || analysis.estimatedRevenueRupiah != null) {
            val unitText = analysis.outputUnit.orEmpty().ifBlank { unitFallback }
            analysis.breakEvenQuantity?.let { bepUnit ->
                ReportRow(stringResource(R.string.feasibility_bep_unit_label), "${bepUnit.toDisplayQuantity()} $unitText")
            }
            analysis.breakEvenRevenueRupiah?.let { bepMoney ->
                ReportRow(stringResource(R.string.feasibility_bep_money_label), bepMoney, emphasized = true)
            }
            analysis.contributionMarginRupiah?.let { cm ->
                ReportRow("Margin Kontribusi Satuan", cm)
            }
            HorizontalDivider()
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
                    ReportRow(stringResource(R.string.reports_cost_per_output, unitText), it)
                }
                HorizontalDivider()
                analysis.grossProfitRupiah?.let {
                    ReportRow(stringResource(R.string.reports_gross_profit), it, emphasized = true)
                }
                analysis.grossProfitMarginPercent?.let {
                    Text(
                        stringResource(R.string.reports_gross_profit_margin, it.toDisplayQuantity()),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                analysis.netProfitMarginPercent?.let {
                    Text(
                        stringResource(R.string.reports_net_profit_margin, it.toDisplayQuantity()),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                HorizontalDivider()
            }
            analysis.revenueCostRatio?.let {
                Text(
                    stringResource(R.string.reports_rc_ratio, it.toDisplayQuantity()),
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                stringResource(R.string.reports_bc_ratio, analysis.benefitCostRatio?.toDisplayQuantity() ?: undefined),
                fontWeight = FontWeight.Bold,
            )
        } else {
            Text(stringResource(R.string.reports_feasibility_missing), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onEditAssumptions, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_edit_assumptions))
            }
        }
    }
}

@Composable
private fun ComparisonSection(state: AppUiState) {
    ReportCard(stringResource(R.string.reports_comparison_title)) {
        ComparisonRow(stringResource(R.string.reports_budget_total), state.projectionBudgetTotalRupiah, state.realizationBudgetTotalRupiah, higherIsBetter = false)
        ComparisonRow(stringResource(R.string.reports_income), state.projectionSummary.operatingIncomeRupiah, state.realizationSummary.operatingIncomeRupiah)
        ComparisonRow(stringResource(R.string.reports_operating_expense), state.projectionSummary.operatingExpenseRupiah, state.realizationSummary.operatingExpenseRupiah, higherIsBetter = false)
        ComparisonRow(stringResource(R.string.reports_profit_loss), state.projectionSummary.profitRupiah, state.realizationSummary.profitRupiah)
        ComparisonRow(stringResource(R.string.reports_cash_balance), state.projectionSummary.cashBalanceRupiah, state.realizationSummary.cashBalanceRupiah)
    }
}

@Composable
internal fun AssumptionsDialog(
    state: AppUiState,
    isSaving: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (Long, Long, Long, String?, String?, Long?, Long?, Long?) -> Unit,
) {
    val existing = state.assumptions
    var opening by rememberSaveable { mutableStateOf((existing?.openingCashRupiah ?: 0).toString()) }
    var capital by rememberSaveable { mutableStateOf((existing?.ownerCapitalRupiah ?: 0).toString()) }
    var other by rememberSaveable { mutableStateOf((existing?.otherFinancingCostRupiah ?: 0).toString()) }
    var quantity by rememberSaveable { mutableStateOf(existing?.outputQuantityText.orEmpty()) }
    var unit by rememberSaveable { mutableStateOf(existing?.outputUnit.orEmpty()) }
    var price by rememberSaveable { mutableStateOf(existing?.outputPriceRupiah?.toString().orEmpty()) }
    var fixedCost by rememberSaveable { mutableStateOf(existing?.fixedCostRupiah?.toString().orEmpty()) }
    var variableCost by rememberSaveable { mutableStateOf(existing?.variableCostPerUnitRupiah?.toString().orEmpty()) }

    // Live calculation for preview
    val parsedPrice = price.toLongOrNull() ?: 0L
    val parsedVc = variableCost.toLongOrNull() ?: 0L
    val fallbackFc = state.summary.operatingExpenseRupiah + (other.toLongOrNull() ?: 0L)
    val parsedFc = fixedCost.toLongOrNull() ?: fallbackFc
    val displayUnit = unit.ifBlank { "unit" }

    MankeuSheet(
        title = stringResource(
            if (state.selectedBookMode == BookMode.PROJECTION) R.string.assumptions_title_plan
            else R.string.assumptions_title_actual,
        ),
        onDismiss = onDismiss,
        confirmEnabled = !isSaving,
        onConfirm = {
            onSave(
                opening.toLongOrNull() ?: 0,
                capital.toLongOrNull() ?: 0,
                other.toLongOrNull() ?: 0,
                quantity.ifBlank { null },
                unit.ifBlank { null },
                price.toLongOrNull(),
                fixedCost.toLongOrNull(),
                variableCost.toLongOrNull(),
            )
        },
    ) {
        // Live BEP Preview Card
        if (parsedPrice > 0 && parsedPrice > parsedVc) {
            val cm = parsedPrice - parsedVc
            val cmPercent = ((cm.toDouble() / parsedPrice.toDouble()) * 100.0).toInt()
            val bepUnit = if (cm > 0) {
                BigDecimal.valueOf(parsedFc).divide(BigDecimal.valueOf(cm), 1, RoundingMode.HALF_UP)
            } else BigDecimal.ZERO
            val bepRupiah = bepUnit.multiply(BigDecimal.valueOf(parsedPrice)).setScale(0, RoundingMode.HALF_UP).longValueExact()

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.assumptions_preview_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Icon(
                            Icons.Default.Calculate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                .padding(10.dp),
                        ) {
                            Text(
                                stringResource(R.string.feasibility_bep_unit_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                "${bepUnit.toDisplayQuantity()} $displayUnit",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                .padding(10.dp),
                        ) {
                            Text(
                                stringResource(R.string.feasibility_bep_money_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                bepRupiah.toRupiah(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Text(
                        stringResource(R.string.assumptions_preview_margin, cm.toRupiah(), cmPercent),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        } else if (parsedPrice > 0 && parsedPrice <= parsedVc) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(R.string.assumptions_preview_warning, parsedPrice.toRupiah(), parsedVc.toRupiah()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp),
                )
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(R.string.assumptions_preview_prompt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }

        Text(
            stringResource(R.string.assumptions_bep_header),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        MoneyField(stringResource(R.string.assumptions_price_label), price) { price = it }
        MoneyField(stringResource(R.string.assumptions_variable_cost_label), variableCost) { variableCost = it }
        MoneyField(stringResource(R.string.assumptions_fixed_cost_label), fixedCost) { fixedCost = it }
        if (fixedCost.isBlank()) {
            Text(
                stringResource(R.string.assumptions_fixed_cost_supporting, fallbackFc.toRupiah()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedTextField(
            value = unit,
            onValueChange = { unit = it },
            label = { Text(stringResource(R.string.assumptions_unit_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = quantity,
            onValueChange = { quantity = sanitizeDecimal(it) },
            label = { Text(stringResource(R.string.assumptions_quantity_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        HorizontalDivider()

        Text(
            stringResource(R.string.assumptions_financing_header),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        MoneyField(stringResource(R.string.assumptions_opening_cash), opening) { opening = it }
        MoneyField(stringResource(R.string.assumptions_own_capital), capital) { capital = it }
        MoneyField(stringResource(R.string.assumptions_other_cost), other) { other = it }
    }
}
