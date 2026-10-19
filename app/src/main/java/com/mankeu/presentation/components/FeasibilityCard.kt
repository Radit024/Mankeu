package com.mankeu.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mankeu.R
import com.mankeu.domain.BusinessAnalysis
import com.mankeu.presentation.LocalMankeuSemanticColors
import com.mankeu.presentation.LocalMotion
import com.mankeu.presentation.toDisplayQuantity
import com.mankeu.presentation.toRupiah
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Feasibility as two meters instead of two more numbers in a list.
 *
 * Break-even and B/C ratio were rendered as sentences — "BEP Produksi (Titik Impas): 4673.214
 * botol", "Rasio Kelayakan (B/C Ratio): 1.284" — among a dozen similar lines. Both are answers to
 * a yes/no question the owner is actually asking ("will this cover its costs?"), and a bar that
 * fills past a marked threshold answers it before the number is read. The figures stay, because
 * the owner also has to be able to check them against the workbook.
 *
 * Shared by Beranda (plan mode) and Laporan so the two never drift apart.
 */

enum class FeasibilityStatus { FEASIBLE, WATCH, NOT_FEASIBLE, UNKNOWN }

/**
 * Below 1,00 the business does not cover its costs. The band between 0,95 and 1,00 is called out
 * separately rather than lumped in with a clear loss: at that distance a single price change or one
 * more month of sales settles it, and telling the owner "Tidak Layak" would overstate what the
 * number knows.
 */
fun feasibilityStatusOf(analysis: BusinessAnalysis): FeasibilityStatus {
    if (analysis.feasible == null) return FeasibilityStatus.UNKNOWN
    val rc = analysis.revenueCostRatio ?: analysis.benefitCostRatio
    return when {
        analysis.feasible == true -> FeasibilityStatus.FEASIBLE
        rc != null && rc >= BigDecimal("0.95") -> FeasibilityStatus.WATCH
        else -> FeasibilityStatus.NOT_FEASIBLE
    }
}

fun feasibilityStatusOf(benefitCostRatio: BigDecimal?): FeasibilityStatus = when {
    benefitCostRatio == null -> FeasibilityStatus.UNKNOWN
    benefitCostRatio >= BigDecimal.ONE -> FeasibilityStatus.FEASIBLE
    benefitCostRatio >= BigDecimal("0.95") -> FeasibilityStatus.WATCH
    else -> FeasibilityStatus.NOT_FEASIBLE
}

@Composable
fun FeasibilityStatusChip(status: FeasibilityStatus) {
    val semantic = LocalMankeuSemanticColors.current
    val (labelRes, container, content) = when (status) {
        FeasibilityStatus.FEASIBLE ->
            Triple(R.string.feasibility_status_feasible, semantic.successContainer, semantic.onSuccessContainer)
        FeasibilityStatus.WATCH ->
            Triple(R.string.feasibility_status_watch, semantic.warningContainer, semantic.onWarningContainer)
        FeasibilityStatus.NOT_FEASIBLE ->
            Triple(R.string.feasibility_status_not_feasible, semantic.dangerContainer, semantic.onDangerContainer)
        FeasibilityStatus.UNKNOWN -> Triple(
            R.string.feasibility_status_unknown,
            MaterialTheme.colorScheme.surfaceContainerHighest,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    // A status badge, not a chip: an AssistChip with a no-op onClick drew a ripple and read as a
    // button that did nothing. Long-press explains what the status is based on.
    MankeuTooltip(stringResource(R.string.tooltip_feasibility_status)) {
        Surface(shape = RoundedCornerShape(50), color = container, contentColor = content) {
            Text(
                stringResource(labelRes),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
        }
    }
}

/** A labelled meter: caption on the left, figure on the right, bar underneath. */
@Composable
fun MeterRow(
    label: String,
    value: String,
    progress: Float,
    trackColor: Color,
    barColor: Color,
    supporting: String? = null,
) {
    val motion = LocalMotion.current
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(maxOf(motion.mediumMillis, 1), easing = FastOutSlowInEasing),
        label = "meter",
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, maxLines = 1)
        }
        LinearProgressIndicator(
            progress = { animated },
            modifier = Modifier.fillMaxWidth().height(10.dp),
            color = barColor,
            trackColor = trackColor,
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
        if (supporting != null) {
            Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * @param targetQuantity the output the book is planning for (or has already sold), which is what
 *   the break-even figure has to be compared against for the meter to mean anything.
 * @param sourceNote states which source class the numbers came from when it is not the obvious one
 *   — see `docs/DOMAIN_MODEL.md`, "A report must declare which source class it uses".
 */
@Composable
fun FeasibilityCard(
    analysis: BusinessAnalysis,
    targetQuantity: BigDecimal?,
    modifier: Modifier = Modifier,
    sourceNote: String? = null,
    onEditAssumptions: (() -> Unit)? = null,
) {
    val semantic = LocalMankeuSemanticColors.current
    val status = feasibilityStatusOf(analysis)
    val barColor = when (status) {
        FeasibilityStatus.FEASIBLE -> semantic.success
        FeasibilityStatus.WATCH -> MaterialTheme.colorScheme.tertiary
        FeasibilityStatus.NOT_FEASIBLE -> semantic.danger
        FeasibilityStatus.UNKNOWN -> MaterialTheme.colorScheme.outline
    }
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val unit = analysis.outputUnit.orEmpty()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(
                        Icons.Default.Insights,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(6.dp).size(20.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(R.string.feasibility_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f),
                )
                FeasibilityStatusChip(status)
            }

            val breakEven = analysis.breakEvenQuantity
            val breakEvenRevenue = analysis.breakEvenRevenueRupiah
            val displayUnit = if (unit.isNotBlank()) " $unit" else ""

            if (breakEven != null && breakEven > BigDecimal.ZERO) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                stringResource(R.string.feasibility_bep_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            analysis.contributionMarginRupiah?.let { cm ->
                                Text(
                                    "Margin: ${cm.toRupiah()}$displayUnit",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            // BEP Unit
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    stringResource(R.string.feasibility_bep_unit_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    "${breakEven.toDisplayQuantity()}$displayUnit",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                )
                                Text(
                                    stringResource(R.string.feasibility_bep_unit_sub),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            // BEP Uang (Rupiah)
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    stringResource(R.string.feasibility_bep_money_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    breakEvenRevenue?.toRupiah() ?: "-",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                )
                                Text(
                                    stringResource(R.string.feasibility_bep_money_sub),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        if (targetQuantity != null && targetQuantity > BigDecimal.ZERO) {
                            val fraction = targetQuantity.divide(breakEven, 4, RoundingMode.HALF_UP).toFloat()
                            val reached = targetQuantity >= breakEven
                            val gap = (if (reached) targetQuantity - breakEven else breakEven - targetQuantity)
                                .setScale(3, RoundingMode.HALF_UP)
                            MeterRow(
                                label = stringResource(R.string.feasibility_bep_target_vs_actual),
                                value = stringResource(
                                    R.string.feasibility_bep_progress,
                                    targetQuantity.toDisplayQuantity(),
                                    breakEven.toDisplayQuantity(),
                                    unit,
                                ),
                                progress = fraction,
                                trackColor = track,
                                barColor = if (reached) semantic.success else semantic.danger,
                                supporting = stringResource(
                                    if (reached) R.string.feasibility_bep_reached else R.string.feasibility_bep_short,
                                    gap.toDisplayQuantity(),
                                    unit,
                                ),
                            )
                        } else {
                            Text(
                                stringResource(
                                    R.string.feasibility_bep_explanation,
                                    breakEven.toDisplayQuantity(),
                                    unit,
                                    breakEvenRevenue?.toRupiah() ?: "-",
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            stringResource(R.string.feasibility_bep_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            stringResource(R.string.feasibility_bep_missing),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            val rcRatio = analysis.revenueCostRatio ?: analysis.benefitCostRatio
            rcRatio?.let { ratio ->
                // The bar is scaled so 1,00 — the decision threshold — sits at two thirds. A bar
                // that filled only at some arbitrary maximum would put "just feasible" and "very
                // feasible" in visually indistinguishable places.
                val displayRatio = if (ratio < BigDecimal.ONE && analysis.benefitCostRatio != null && analysis.benefitCostRatio >= BigDecimal.ONE) {
                    analysis.benefitCostRatio
                } else ratio
                MeterRow(
                    label = stringResource(R.string.feasibility_rc_caption, displayRatio.toDisplayQuantity()),
                    value = analysis.netProfitMarginPercent?.let { "${it.toDisplayQuantity()}%" }.orEmpty(),
                    progress = displayRatio.divide(BigDecimal("1.5"), 4, RoundingMode.HALF_UP).toFloat(),
                    trackColor = track,
                    barColor = barColor,
                    supporting = stringResource(R.string.feasibility_rc_help),
                )
            }

            if (analysis.variableCostPerUnitRupiah != null && analysis.benefitCostRatio != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.feasibility_bcr_caption, analysis.benefitCostRatio.toDisplayQuantity()),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        if (analysis.benefitCostRatio >= BigDecimal.ZERO) "Untung" else "Rugi",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (analysis.benefitCostRatio >= BigDecimal.ZERO) semantic.success else semantic.danger,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CompactFigure(
                    label = stringResource(R.string.reports_total_cost),
                    value = analysis.totalCostRupiah.toRupiah(),
                    modifier = Modifier.weight(1f),
                )
                CompactFigure(
                    label = stringResource(R.string.plan_metric_revenue),
                    value = analysis.estimatedRevenueRupiah?.toRupiah() ?: "-",
                    modifier = Modifier.weight(1f),
                )
                CompactFigure(
                    label = stringResource(R.string.plan_metric_unit_cost),
                    value = analysis.costPerOutputRupiah?.toRupiah() ?: "-",
                    modifier = Modifier.weight(1f),
                )
            }

            if (sourceNote != null) {
                Text(sourceNote, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (onEditAssumptions != null) {
                FilledTonalButton(
                    onClick = onEditAssumptions,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 12.dp),
                ) {
                    Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_edit_assumptions), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CompactFigure(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(0.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Black,
            maxLines = 1,
        )
    }
}
