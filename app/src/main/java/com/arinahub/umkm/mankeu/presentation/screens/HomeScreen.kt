package com.arinahub.umkm.mankeu.presentation.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arinahub.umkm.mankeu.R
import com.arinahub.umkm.mankeu.domain.BookMode
import com.arinahub.umkm.mankeu.domain.CategoryRole
import com.arinahub.umkm.mankeu.domain.FinancialEventType
import com.arinahub.umkm.mankeu.domain.ObligationStatus
import com.arinahub.umkm.mankeu.domain.RecordStatus
import com.arinahub.umkm.mankeu.presentation.AppUiState
import com.arinahub.umkm.mankeu.presentation.Destination
import com.arinahub.umkm.mankeu.presentation.LocalArinaSemanticColors
import com.arinahub.umkm.mankeu.presentation.isDarkScheme
import com.arinahub.umkm.mankeu.presentation.components.EmptyCard
import com.arinahub.umkm.mankeu.presentation.components.EventCard
import com.arinahub.umkm.mankeu.presentation.components.MetricTone
import com.arinahub.umkm.mankeu.presentation.components.NoticeCard
import com.arinahub.umkm.mankeu.presentation.components.SectionTitle
import com.arinahub.umkm.mankeu.presentation.components.SimpleMetric
import com.arinahub.umkm.mankeu.presentation.components.SimpleMetricGrid
import com.arinahub.umkm.mankeu.presentation.components.SkeletonHero
import com.arinahub.umkm.mankeu.presentation.components.SkeletonListItem
import com.arinahub.umkm.mankeu.presentation.components.SkeletonMetricGrid
import com.arinahub.umkm.mankeu.presentation.components.TourTarget
import com.arinahub.umkm.mankeu.presentation.components.tourTarget
import com.arinahub.umkm.mankeu.presentation.LocalMotion
import com.arinahub.umkm.mankeu.presentation.toRupiah

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
    expanded: Boolean,
) {
    LazyColumn(
        contentPadding = PaddingValues(
            start = if (expanded) 24.dp else 16.dp,
            end = if (expanded) 24.dp else 16.dp,
            top = 12.dp,
            bottom = if (expanded) 24.dp else 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (state.isLoading) {
            item { SkeletonHero() }
            item { SkeletonMetricGrid() }
            items(3) { SkeletonListItem() }
            return@LazyColumn
        }
        // No business or field yet: the app used to quietly create "Usaha Saya"/"Umum" during
        // startup, so a first-time owner met a dashboard for a farm they had never named. Now the
        // first screen states plainly what is missing and offers the one action that fixes it.
        if (state.businesses.isEmpty()) {
            item {
                EmptyCard(
                    stringResource(R.string.setup_business_title),
                    stringResource(R.string.setup_business_body),
                    stringResource(R.string.scope_add_business),
                    onOpenData,
                )
            }
            return@LazyColumn
        }
        if (state.projects.isEmpty()) {
            item {
                EmptyCard(
                    stringResource(R.string.setup_project_title),
                    stringResource(R.string.setup_project_body),
                    stringResource(R.string.scope_add_project),
                    onOpenData,
                )
            }
            return@LazyColumn
        }
        item {
            HeroCard(
                mode = state.selectedBookMode,
                amount = state.summary.cashBalanceRupiah,
                projectId = state.selectedProjectId,
                modifier = Modifier.tourTarget(TourTarget.HERO_SUMMARY),
            )
        }
        item {
            Column(
                modifier = Modifier.animateContentSize(tween(maxOf(LocalMotion.current.mediumMillis, 1), easing = FastOutSlowInEasing)),
            ) {
                val profitTone = when {
                    state.summary.profitRupiah < 0 -> MetricTone.DANGER
                    state.summary.profitRupiah > 0 -> MetricTone.SUCCESS
                    else -> MetricTone.PRIMARY
                }
                val cashFlowTone = when {
                    state.summary.operatingCashFlowRupiah < 0 -> MetricTone.DANGER
                    state.summary.operatingCashFlowRupiah > 0 -> MetricTone.SUCCESS
                    else -> MetricTone.PRIMARY
                }
                SimpleMetricGrid(
                    listOf(
                        SimpleMetric(stringResource(R.string.home_profit), state.summary.profitRupiah, Icons.Default.Assessment, profitTone),
                        SimpleMetric(stringResource(R.string.home_cash_flow), state.summary.operatingCashFlowRupiah, Icons.Default.SwapHoriz, cashFlowTone),
                        SimpleMetric(stringResource(R.string.home_receivable), state.summary.receivableOutstandingRupiah, Icons.Default.ArrowDownward, MetricTone.SUCCESS),
                        SimpleMetric(stringResource(R.string.home_payable), state.summary.payableOutstandingRupiah, Icons.Default.ArrowUpward, MetricTone.DANGER),
                    ),
                )
            }
        }
        item {
            ExpenseDistributionCard(
                state = state,
            )
        }
        if (state.obligations.any { it.status == ObligationStatus.OVERDUE }) {
            item {
                NoticeCard(
                    title = stringResource(R.string.home_overdue_title),
                    body = stringResource(
                        R.string.home_overdue_body,
                        state.obligations.count { it.status == ObligationStatus.OVERDUE },
                    ),
                    icon = Icons.Default.Notifications,
                    onClick = { onNavigate(Destination.REPORTS) },
                )
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
                NoticeCard(notice.title, notice.explanation, Icons.Default.Info)
            }
        }
        item {
            Column(
                modifier = Modifier.fillMaxWidth().tourTarget(TourTarget.NAV_ENTRY),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SectionTitle(
                    stringResource(R.string.home_recent),
                    stringResource(R.string.home_shortcut_records_sub),
                )
            }
        }
        if (state.events.isEmpty()) {
            item {
                EmptyCard(
                    stringResource(R.string.home_empty_title),
                    stringResource(R.string.home_empty_body),
                    stringResource(R.string.home_empty_action),
                ) {
                    onNavigate(Destination.ENTRY)
                }
            }
        } else {
            items(state.events.take(3), key = { it.id }) { event ->
                EventCard(event, onReverse)
            }
            item {
                FilledTonalButton(
                    onClick = { onNavigate(Destination.ENTRY) },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.home_shortcut_records) + " (${state.events.size})",
                        fontWeight = FontWeight.Bold,
                    )
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
    projectId: String? = null,
    modifier: Modifier = Modifier,
) {
    val motion = LocalMotion.current
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = modifier.fillMaxWidth(),
    ) {
        // The hero is the one place the app fills a large area with an accent colour, so its text
        // colour has to follow the fill. In light mode the deep primary/tertiary pair carries white
        // text at 6.4:1; in dark mode those roles are *light* greens and blues, where white text
        // would sit at about 2:1, so dark mode fills with the container tones and writes on them
        // with onPrimaryContainer (7.2:1 at both ends of the gradient).
        val dark = isDarkScheme()
        val semantic = LocalArinaSemanticColors.current
        val isNegative = amount < 0
        val targetHeroStart = when {
            isNegative && dark -> semantic.dangerContainer
            isNegative -> semantic.danger
            dark -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.primary
        }
        val targetHeroEnd = when {
            isNegative && dark -> Color(0xFF5D1805)
            isNegative -> Color(0xFF7C2E19)
            dark -> MaterialTheme.colorScheme.tertiaryContainer
            else -> MaterialTheme.colorScheme.tertiary
        }
        val targetOnHero = when {
            isNegative && dark -> semantic.onDangerContainer
            isNegative -> semantic.onDanger
            dark -> MaterialTheme.colorScheme.onPrimaryContainer
            else -> MaterialTheme.colorScheme.onPrimary
        }

        val colorAnimationSpec = tween<Color>(
            durationMillis = maxOf(motion.mediumMillis, 1),
            easing = FastOutSlowInEasing,
        )
        val heroStart by animateColorAsState(targetHeroStart, colorAnimationSpec, label = "heroStart")
        val heroEnd by animateColorAsState(targetHeroEnd, colorAnimationSpec, label = "heroEnd")
        val onHero by animateColorAsState(targetOnHero, colorAnimationSpec, label = "onHero")
        Column(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(listOf(heroStart, heroEnd)),
                    MaterialTheme.shapes.extraLarge,
                )
                .padding(20.dp)
                .animateContentSize(tween(maxOf(motion.mediumMillis, 1), easing = FastOutSlowInEasing)),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AnimatedContent(
                targetState = Pair(mode, projectId ?: ""),
                transitionSpec = {
                    val enterMillis = maxOf(motion.mediumMillis, 1)
                    val exitMillis = maxOf(motion.fastMillis, 1)
                    val downwards = targetState.first == BookMode.REALIZATION
                    (slideInVertically(tween(enterMillis, easing = FastOutSlowInEasing)) { full ->
                        if (downwards) full / 3 else -full / 3
                    } + fadeIn(tween(enterMillis)))
                        .togetherWith(
                            slideOutVertically(tween(exitMillis, easing = FastOutSlowInEasing)) { full ->
                                if (downwards) -full / 3 else full / 3
                            } + fadeOut(tween(exitMillis)),
                        )
                },
                label = "heroBalance",
            ) { (currentMode, _) ->
                val projection = currentMode == BookMode.PROJECTION
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(if (projection) R.string.home_balance_plan else R.string.home_balance_actual),
                        color = onHero.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        amount.toRupiah(),
                        color = onHero,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        stringResource(if (projection) R.string.home_balance_sub_plan else R.string.home_balance_sub_actual),
                        color = onHero.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
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
    // Six category hues, harmonised toward the brand green and re-toned per theme; see
    // ArinaSemanticColors.chart. The raw Material 2 hues that used to live here ignored the palette
    // and stayed identical in dark mode, where they glared. Read outside the remember block: it is
    // a composition-local read, and it has to re-run when the theme changes.
    val palette = LocalArinaSemanticColors.current.chart
    val expenseCategories = remember(state.events, state.budgetItems, state.selectedBookMode, otherLabel, palette) {
        val categoryExpenses = mutableMapOf<String, Long>()
        val expenseTypes = setOf(
            FinancialEventType.CASH_EXPENSE,
            FinancialEventType.CREDIT_PURCHASE_EXPENSE,
        )
        // Reversed and reversing rows are excluded exactly as `ReportCalculator` excludes them.
        // They were not before, so after an import batch was reversed the hero card correctly read
        // Rp0 while this chart still showed the whole Rp22.159.000 — two numbers on one screen
        // disagreeing about whether the money was spent.
        state.events.filter {
            it.type in expenseTypes &&
                it.status == RecordStatus.POSTED &&
                it.reversedByEventId == null
        }.forEach { event ->
            val cat = event.category.ifBlank { otherLabel }
            categoryExpenses[cat] = (categoryExpenses[cat] ?: 0L) + event.amountRupiah
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
        val total = categoryExpenses.values.sum()
        if (total == 0L) emptyList()
        else {
            categoryExpenses.entries
                .sortedByDescending { it.value }
                .mapIndexed { index, entry ->
                    ExpenseSlice(
                        category = entry.key,
                        amount = entry.value,
                        percentage = (entry.value.toDouble() / total.toDouble()) * 100.0,
                        color = palette[index % palette.size],
                    )
                }
        }
    }

    if (expenseCategories.isNotEmpty()) {
        val totalExpense = expenseCategories.sumOf { it.amount }
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
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.home_expense_mix),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        totalExpense.toRupiah(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Donut Chart Canvas
                    Box(
                        modifier = Modifier.size(100.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(modifier = Modifier.size(90.dp)) {
                            var startAngle = -90f
                            val strokeWidth = 20.dp.toPx()
                            expenseCategories.forEach { slice ->
                                val sweep = (slice.percentage / 100.0 * 360.0).toFloat()
                                drawArc(
                                    color = slice.color,
                                    startAngle = startAngle,
                                    sweepAngle = sweep,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth),
                                )
                                startAngle += sweep
                            }
                        }
                        Text(
                            stringResource(R.string.home_expense_mix_center, expenseCategories.size),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    // Legend
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        expenseCategories.take(4).forEach { slice ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(slice.color, CircleShape),
                                )
                                Text(
                                    slice.category,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    "${String.format(java.util.Locale.US, "%.1f", slice.percentage)}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
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
    val percentage: Double,
    val color: Color,
)

