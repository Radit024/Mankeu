package com.mankeu.presentation.screens

import com.mankeu.presentation.guide.HelpArticle
import com.mankeu.presentation.components.rememberTourListState
import com.mankeu.presentation.components.crossFade
import com.mankeu.presentation.components.itemMotion

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.RoundedCornerShape
import com.mankeu.domain.FinancialSummary
import com.mankeu.domain.TransactionCalculation
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.TextAutoSize
import com.mankeu.R
import com.mankeu.domain.BookMode
import com.mankeu.domain.CategoryRole
import com.mankeu.domain.FinancialEventType
import com.mankeu.domain.PlanAnalysis
import com.mankeu.domain.RecordStatus
import com.mankeu.presentation.AppUiState
import com.mankeu.presentation.Destination
import com.mankeu.presentation.SemanticIntent
import com.mankeu.presentation.LocalMankeuSemanticColors
import com.mankeu.presentation.isDarkScheme
import com.mankeu.presentation.components.CardDetails
import com.mankeu.presentation.components.EmptyCard
import com.mankeu.presentation.components.MankeuStatusBadge
import com.mankeu.presentation.components.ReportRow
import com.mankeu.presentation.components.StateArtwork
import com.mankeu.presentation.components.ActualMonthCard
import com.mankeu.presentation.components.PlanFeasibilityCard
import com.mankeu.presentation.components.NoticeCard
import com.mankeu.presentation.components.SectionTitle
import com.mankeu.presentation.components.SkeletonHero
import com.mankeu.presentation.components.SkeletonListItem
import com.mankeu.presentation.components.SkeletonMetricGrid
import com.mankeu.presentation.components.TourTarget
import com.mankeu.presentation.components.tourTarget
import com.mankeu.presentation.LocalMotion
import com.mankeu.presentation.appLocale
import com.mankeu.presentation.toRupiah
import java.time.format.DateTimeFormatter

/**
 * The Beranda (home) destination: cash headline, the four key metrics, pending obligations,
 * import notices, and executive summary with quick shortcut to Catatan.
 */
@Composable
fun HomeScreen(
    state: AppUiState,
    onNavigate: (Destination) -> Unit,
    onReverse: (String, String) -> Unit,
    onOpenData: () -> Unit,
    onOpenPlan: () -> Unit = {},
    expanded: Boolean = false,
) {
    // Proyeksi reads the business plan (DEC-014) whenever the unit has one; postings otherwise.
    // Both produce the same summary and schedule shapes, so every card below reads one code path.
    val plan = state.plan.takeIf { state.selectedBookMode == BookMode.PROJECTION }
    val summary = plan?.summary ?: state.summary
    val motion = LocalMotion.current

    if (state.isLoading) {
        LazyColumn(
            state = rememberTourListState(),
            contentPadding = PaddingValues(
                start = if (expanded) 24.dp else 16.dp,
                end = if (expanded) 24.dp else 16.dp,
                top = 12.dp,
                bottom = 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { SkeletonHero() }
            item { SkeletonMetricGrid() }
            items(3) { SkeletonListItem() }
        }
        return
    }

    // No business or field yet: render empty card filling the screen downwards with content centered
    if (state.businesses.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = if (expanded) 24.dp else 16.dp,
                    end = if (expanded) 24.dp else 16.dp,
                    top = 12.dp,
                    bottom = 16.dp,
                ),
        ) {
            EmptyCard(
                title = stringResource(R.string.setup_business_title),
                body = stringResource(R.string.setup_business_body),
                action = stringResource(R.string.scope_add_business),
                onAction = onOpenData,
                learn = HelpArticle.START_SCOPE,
                fillMaxHeight = true,
                artwork = StateArtwork.BUSINESS_PLAN,
            )
        }
        return
    }

    if (state.projects.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = if (expanded) 24.dp else 16.dp,
                    end = if (expanded) 24.dp else 16.dp,
                    top = 12.dp,
                    bottom = 16.dp,
                ),
        ) {
            EmptyCard(
                title = stringResource(R.string.setup_project_title),
                body = stringResource(R.string.setup_project_body),
                action = stringResource(R.string.scope_add_project),
                onAction = onOpenData,
                learn = HelpArticle.START_SCOPE,
                fillMaxHeight = true,
                artwork = StateArtwork.BUSINESS_PLAN,
            )
        }
        return
    }

    LazyColumn(
        state = rememberTourListState(),
        contentPadding = PaddingValues(
            start = if (expanded) 24.dp else 16.dp,
            end = if (expanded) 24.dp else 16.dp,
            top = 12.dp,
            // Clears the speed dial. Scaffold pads around a FAB for its own content slot, but the
            // list scrolls under it, so without this the last card is permanently half-covered.
            bottom = 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            HeroCard(
                mode = state.selectedBookMode,
                amount = summary.cashBalanceRupiah,
                plan = plan,
                projectId = state.selectedProjectId,
                modifier = Modifier.tourTarget(TourTarget.HERO_SUMMARY),
                onClick = { onNavigate(Destination.REPORTS) },
            )
        }
        item {
            val detailsKey = "${state.selectedBusinessId}:${state.selectedProjectId}:${state.selectedBookMode}"
            val projection = state.selectedBookMode == BookMode.PROJECTION
            val locale = appLocale()
            val periodText = if (projection && plan != null && plan.months.isNotEmpty()) {
                val formatter = DateTimeFormatter.ofPattern("MMM yyyy", locale)
                stringResource(
                    R.string.scope_project_period,
                    plan.months.first().format(formatter),
                    plan.months.last().format(formatter),
                )
            } else {
                stringResource(R.string.home_period_all_records)
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(tween(maxOf(LocalMotion.current.mediumMillis, 1), easing = FastOutSlowInEasing)),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = stringResource(if (projection) R.string.home_cash_profit_plan else R.string.home_cash_profit),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = periodText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (summary.profitRupiah < 0) {
                            MankeuStatusBadge(
                                label = stringResource(R.string.status_deficit),
                                intent = SemanticIntent.DANGER,
                            )
                        }
                    }
                    val semantic = LocalMankeuSemanticColors.current
                    Text(
                        text = summary.profitRupiah.toRupiah(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (summary.profitRupiah < 0) semantic.danger else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        autoSize = TextAutoSize.StepBased(minFontSize = 18.sp, maxFontSize = MaterialTheme.typography.headlineMedium.fontSize),
                    )
                    HorizontalDivider()
                    ReportRow(stringResource(R.string.reports_operating_income), summary.operatingIncomeRupiah)
                    ReportRow(stringResource(R.string.reports_operating_expense), summary.operatingExpenseRupiah)
                    CardDetails(
                        stateKey = detailsKey,
                        showLabel = stringResource(R.string.card_show_calculation),
                        hideLabel = stringResource(R.string.card_hide_calculation),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ReportRow(stringResource(R.string.home_cash_flow), summary.operatingCashFlowRupiah)
                            Text(
                                stringResource(R.string.home_cash_profit_help),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        item {
            ExpenseDistributionCard(
                state = state,
            )
        }
        item {
            StockCard(items = state.stockItems, onManage = { onNavigate(Destination.STOCK) }, compact = true)
        }
        // Feasibility is the reason the plan exists, so it gets its own card with a meter rather
        // than more figures in the grid. Realisasi asks the same question of this month.
        if (state.selectedBookMode == BookMode.PROJECTION) {
            item {
                if (plan != null) {
                    PlanFeasibilityCard(
                        plan,
                        onOpenPlan = onOpenPlan,
                        detailsKey = "${state.selectedBusinessId}:${state.selectedProjectId}:${state.selectedBookMode}:plan",
                    )
                } else {
                    EmptyCard(
                        stringResource(R.string.plan_empty_title),
                        stringResource(R.string.plan_empty_body),
                        stringResource(R.string.plan_open),
                        onOpenPlan,
                        learn = HelpArticle.PLAN_CREATE,
                        artwork = StateArtwork.BUSINESS_PLAN,
                    )
                }
            }
        } else {
            state.actualMonth?.let { actual ->
                item {
                    ActualMonthCard(
                        actual,
                        state.products,
                        Modifier.tourTarget(TourTarget.HOME_BEP),
                        detailsKey = "${state.selectedBusinessId}:${state.selectedProjectId}:${state.selectedBookMode}:${actual.month}",
                    )
                }
            }
        }
        if (state.notices.isNotEmpty()) {
            item {
                SectionTitle(
                    stringResource(R.string.home_import_notice_title),
                    stringResource(R.string.home_import_notice_sub),
                )
            }
            items(state.notices.take(3), key = { it.id }) { notice ->
                Box(itemMotion(motion)) {
                    NoticeCard(notice.title, notice.explanation, Icons.Default.Info)
                }
            }
        }
    }
}

/**
 * Cash headline. Switching Proyeksi <-> Realisasi is not a different screen — it is the same
 * question asked of a different book — so the card stays put and only its wording and figure are
 * exchanged, sliding in the direction of travel. Previously the label and the number were swapped
 * in a single frame, which read as a glitch rather than as a deliberate change of scope.
 */
@Composable
private fun HeroCard(
    mode: BookMode,
    amount: Long,
    modifier: Modifier = Modifier,
    plan: PlanAnalysis? = null,
    projectId: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val motion = LocalMotion.current
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = modifier.fillMaxWidth(),
    ) {
        // A single branded fill keeps every balance label readable. In dark mode primary is a
        // light green, so use the deeper primary container with its matching foreground. A loss
        // uses the expense container and its on-colour, never a hardcoded gradient endpoint.
        val dark = isDarkScheme()
        val semantic = LocalMankeuSemanticColors.current
        val isNegative = amount < 0
        val targetHeroStart = when {
            isNegative -> semantic.dangerContainer
            dark -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.primary
        }
        val targetOnHero = when {
            isNegative -> semantic.onDangerContainer
            dark -> MaterialTheme.colorScheme.onPrimaryContainer
            else -> MaterialTheme.colorScheme.onPrimary
        }

        val colorAnimationSpec = tween<Color>(
            durationMillis = maxOf(motion.mediumMillis, 1),
            easing = FastOutSlowInEasing,
        )
        val heroStart by animateColorAsState(targetHeroStart, colorAnimationSpec, label = "heroStart")
        val onHero by animateColorAsState(targetOnHero, colorAnimationSpec, label = "onHero")
        Column(
            Modifier
                .fillMaxWidth()
                .background(
                    heroStart,
                    MaterialTheme.shapes.extraLarge,
                )
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            role = androidx.compose.ui.semantics.Role.Button,
                            onClickLabel = stringResource(R.string.home_cash_details),
                            onClick = onClick,
                        )
                    } else Modifier
                )
                .padding(20.dp)
                .animateContentSize(tween(maxOf(motion.mediumMillis, 1), easing = FastOutSlowInEasing)),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AnimatedContent(
                targetState = Pair(mode, projectId ?: ""),
                transitionSpec = { crossFade(motion) },
                label = "heroBalance",
            ) { (currentMode, _) ->
                val projection = currentMode == BookMode.PROJECTION
                val locale = appLocale()
                val periodText = if (projection && plan != null && plan.months.isNotEmpty()) {
                    val formatter = DateTimeFormatter.ofPattern("MMM yyyy", locale)
                    stringResource(
                        R.string.scope_project_period,
                        plan.months.first().format(formatter),
                        plan.months.last().format(formatter),
                    )
                } else {
                    stringResource(R.string.home_period_all_records)
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(if (projection) R.string.home_balance_plan else R.string.home_balance_actual),
                            color = onHero,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (isNegative) {
                            MankeuStatusBadge(
                                label = stringResource(R.string.status_deficit),
                                intent = SemanticIntent.DANGER,
                            )
                        }
                    }
                    Text(
                        amount.toRupiah(),
                        color = onHero,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        autoSize = TextAutoSize.StepBased(minFontSize = 18.sp, maxFontSize = MaterialTheme.typography.headlineLarge.fontSize),
                    )
                    Text(
                        periodText,
                        color = onHero.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (onClick != null) {
                        Spacer(Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                stringResource(R.string.home_cash_details) + " \u2192",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = onHero.copy(alpha = 0.8f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpenseDistributionCard(
    state: AppUiState,
    modifier: Modifier = Modifier,
) {
    val motion = LocalMotion.current
    val otherLabel = stringResource(R.string.category_other)
    val expenseCategories = remember(
        state.categoryTotals,
        state.budgetItems,
        state.categories,
        state.selectedBookMode,
        otherLabel,
    ) {
        val categoryExpenses = mutableMapOf<String, Long>()
        state.categoryTotals.forEach { row ->
            val cat = row.category.ifBlank { otherLabel }
            categoryExpenses[cat] = (categoryExpenses[cat] ?: 0L) + row.totalRupiah
        }
        if (categoryExpenses.isEmpty() && state.selectedBookMode == BookMode.PROJECTION) {
            val catMap = state.categories.associateBy { it.id }
            state.budgetItems.forEach { item ->
                val role = catMap[item.categoryId]?.role
                if (role != CategoryRole.REVENUE.name) {
                    val catName = catMap[item.categoryId]?.name ?: otherLabel
                    categoryExpenses[catName] = (categoryExpenses[catName] ?: 0L) + item.plannedTotalRupiah
                }
            }
        }
        if (categoryExpenses.isEmpty()) {
            emptyList()
        } else {
            categoryExpenses.entries
                .sortedWith(compareByDescending<Map.Entry<String, Long>> { it.value }.thenBy { it.key })
                .map { entry ->
                    ExpenseSlice(
                        category = entry.key,
                        amount = entry.value,
                    )
                }
        }
    }

    if (expenseCategories.isNotEmpty()) {
        val totalExpense = expenseCategories.sumOf { it.amount }
        val detailsKey = "expenses:${state.selectedBusinessId}:${state.selectedProjectId}:${state.selectedBookMode}"
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            shape = MaterialTheme.shapes.large,
            modifier = modifier
                .fillMaxWidth()
                .animateContentSize(tween(maxOf(motion.mediumMillis, 1), easing = FastOutSlowInEasing))
                .tourTarget(TourTarget.HOME_COST_CHART),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.home_expense_mix),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        totalExpense.toRupiah(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                HorizontalDivider()

                expenseCategories.take(3).forEach { slice ->
                    ReportRow(slice.category, slice.amount, highlightNegative = false)
                }

                if (expenseCategories.size > 3) {
                    CardDetails(
                        stateKey = detailsKey,
                        showLabel = stringResource(R.string.card_show_all_categories),
                        hideLabel = stringResource(R.string.card_show_fewer_categories),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            expenseCategories.drop(3).forEach { slice ->
                                ReportRow(slice.category, slice.amount, highlightNegative = false)
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class ExpenseSlice(
    val category: String,
    val amount: Long,
)

