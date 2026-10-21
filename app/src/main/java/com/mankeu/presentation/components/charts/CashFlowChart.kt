package com.mankeu.presentation.components.charts

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mankeu.domain.MonthlyCashFlow
import com.mankeu.presentation.LocalMankeuSemanticColors
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.ColumnCartesianLayerModel
import com.patrykandpatrick.vico.compose.cartesian.data.columnModel
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.Insets
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.data.ExtraStore
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Monthly net cash as a Vico cartesian chart.
 *
 * Replaces a hand-rolled `Canvas` bar chart that had three problems no amount of tuning fixes:
 * it divided the available width by the number of bars, so a year of data drew slivers and two
 * years drew nothing legible; it had no scrolling, so the earliest months were simply unreachable;
 * and it was inert — the owner could see that March was the bad month but not by how much without
 * scrolling down to the figures underneath.
 *
 * Vico gives all three for free: columns keep a fixed width and the chart scrolls horizontally
 * once they overflow, and pressing a column raises a marker with the rupiah value. The only thing
 * this file adds is the app's own semantics — a surplus month is drawn in the success tone and a
 * deficit month in the danger tone, matching every other figure on the screen — and Indonesian
 * number formatting on both axes and in the marker.
 */
@Composable
fun MonthlyCashFlowChart(
    months: List<MonthlyCashFlow>,
    modifier: Modifier = Modifier,
    locale: Locale = Locale.forLanguageTag("id-ID"),
) {
    if (months.isEmpty()) return

    val semantic = LocalMankeuSemanticColors.current
    val positiveColumn = rememberLineComponent(
        fill = Fill(semantic.success),
        thickness = 18.dp,
        shape = RectangleShape,
    )
    val negativeColumn = rememberLineComponent(
        fill = Fill(semantic.danger),
        thickness = 18.dp,
        shape = RectangleShape,
    )
    // A surplus and a deficit are not two series, they are one series whose sign carries meaning,
    // so the colour is chosen per entry rather than per series.
    val columnProvider = remember(positiveColumn, negativeColumn) {
        object : ColumnCartesianLayer.ColumnProvider {
            override fun getColumn(
                entry: ColumnCartesianLayerModel.Entry,
                extraStore: ExtraStore,
            ) = if (entry.y < 0.0) negativeColumn else positiveColumn

            override fun getWidestSeriesColumn(
                seriesKey: Any,
                seriesIndex: Int,
                extraStore: ExtraStore,
            ) = positiveColumn
        }
    }

    val monthLabels = remember(months, locale) {
        months.map { it.month.format(DateTimeFormatter.ofPattern("MMM yy", locale)) }
    }
    val bottomFormatter = remember(monthLabels) {
        CartesianValueFormatter { _, value, _ ->
            monthLabels.getOrElse(value.toInt()) { "" }
        }
    }
    // Axis labels are compacted — "Rp12,3 jt" — because a full rupiah figure repeated five times
    // down the side of a phone-width chart leaves no room for the chart.
    val startFormatter = remember(locale) {
        CartesianValueFormatter { _, value, _ -> compactRupiah(value, locale) }
    }

    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(months) {
        modelProducer.runTransaction {
            columnModel { series(months.map { it.netRupiah }) }
        }
    }

    val markerLabel = rememberTextComponent(
        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
        padding = Insets(8.dp, 4.dp),
        background = rememberShapeComponent(
            fill = Fill(MaterialTheme.colorScheme.surfaceContainerHighest),
            shape = MaterialTheme.shapes.extraSmall,
        ),
    )
    val markerFormatter = remember(months, monthLabels, locale) {
        DefaultCartesianMarker.ValueFormatter { _, targets ->
            val index = targets.firstOrNull()?.x?.toInt() ?: return@ValueFormatter ""
            val month = months.getOrNull(index) ?: return@ValueFormatter ""
            "${monthLabels.getOrElse(index) { "" }}: ${fullRupiah(month.netRupiah, locale)}"
        }
    }
    val marker = rememberDefaultCartesianMarker(
        label = markerLabel,
        valueFormatter = markerFormatter,
        labelPosition = DefaultCartesianMarker.LabelPosition.AbovePoint,
    )

    ProvideVicoTheme(rememberM3VicoTheme()) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberColumnCartesianLayer(columnProvider = columnProvider),
                startAxis = VerticalAxis.rememberStart(valueFormatter = startFormatter),
                bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = bottomFormatter),
                marker = marker,
            ),
            modelProducer = modelProducer,
            modifier = modifier.fillMaxWidth(),
            // Vico scrolls horizontally on its own once the columns outgrow the width, which is
            // the behaviour the old chart could not have at any width.
            scrollState = rememberVicoScrollState(scrollEnabled = true),
        )
    }
}

/** "Rp12,3 jt" / "Rp450 rb" — short enough to repeat down an axis. */
private fun compactRupiah(value: Double, locale: Locale): String {
    val magnitude = kotlin.math.abs(value)
    val sign = if (value < 0) "-" else ""
    return when {
        magnitude >= 1_000_000_000 -> sign + "Rp" + String.format(locale, "%.1f", magnitude / 1_000_000_000) + " M"
        magnitude >= 1_000_000 -> sign + "Rp" + String.format(locale, "%.1f", magnitude / 1_000_000) + " jt"
        magnitude >= 1_000 -> sign + "Rp" + String.format(locale, "%.0f", magnitude / 1_000) + " rb"
        else -> sign + "Rp" + String.format(locale, "%.0f", magnitude)
    }
}

private fun fullRupiah(value: Long, locale: Locale): String =
    (if (value < 0) "-" else "") + "Rp" + String.format(locale, "%,d", kotlin.math.abs(value))
