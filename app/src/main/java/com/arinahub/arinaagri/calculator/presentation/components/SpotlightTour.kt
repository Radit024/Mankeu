package com.arinahub.arinaagri.calculator.presentation.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import kotlinx.coroutines.delay
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.UploadFile
import androidx.activity.compose.BackHandler
import androidx.compose.ui.zIndex
import com.arinahub.arinaagri.calculator.R
import com.arinahub.arinaagri.calculator.presentation.Destination
import com.arinahub.arinaagri.calculator.presentation.LocalMotion
import com.arinahub.arinaagri.calculator.presentation.Overlay

/**
 * A real spotlight tour: a dimmed scrim with a genuine hole punched through it over the live UI,
 * not a modal card floating on a dark rectangle.
 *
 * How the hole is made: the scrim is drawn into an offscreen layer
 * ([CompositingStrategy.Offscreen]) and the highlight shape is then drawn on top of it with
 * [BlendMode.Clear]. Clear only erases pixels within the layer it is drawn into, so without the
 * offscreen layer the blend would either be ignored or punch through the whole window. Coordinates
 * come from the real composables via [Modifier.tourTarget], so the hole tracks whatever the target
 * actually measures to on this device — no hardcoded fractions of the screen.
 */
enum class TourTarget {
    // Tier 1: Core Business Workflow Tour
    SCOPE_BUSINESS,
    SCOPE_BOOK,
    HERO_SUMMARY,
    HOME_COST_CHART,
    NAV_ENTRY,
    NAV_REPORTS,
    NAV_MORE,

    // Tier 2: Contextual Per-Page Feature Tours
    SETTINGS_DRIVE,
    SETTINGS_SECURITY,
    SETTINGS_DISPLAY,
    SCOPE_ADD_BUSINESS,
    SCOPE_ADD_FIELD,
    IMPORT_FILE_PICKER,
    IMPORT_TARGET_BOOK,
    DATA_EXPORT,
    ENTRY_TABS,
    ENTRY_KINDS,
    ENTRY_HISTORY,
    REPORTS_TABS,
    REPORTS_PROFIT,
    REPORTS_CASHFLOW,
    REPORTS_FEASIBILITY,
}

/**
 * Root-space bounds and relocation requesters of every composable that has registered itself as a
 * tour target. Written from [Modifier.tourTarget] and read by [SpotlightTour].
 */
class TourTargetRegistry {
    val bounds = mutableStateMapOf<TourTarget, Rect>()
    val requesters = mutableMapOf<TourTarget, BringIntoViewRequester>()
}

val LocalTourTargets = staticCompositionLocalOf { TourTargetRegistry() }

/**
 * Marks this composable as the highlight area for [target] and connects auto-scroll relocation.
 * Safe to apply unconditionally: when no tour is running the registry is simply never read.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.tourTarget(target: TourTarget): Modifier {
    val registry = LocalTourTargets.current
    val requester = remember(target) { BringIntoViewRequester() }
    LaunchedEffect(target, requester) {
        registry.requesters[target] = requester
    }
    return this
        .bringIntoViewRequester(requester)
        .onGloballyPositioned { coordinates ->
            if (coordinates.isAttached) registry.bounds[target] = coordinates.boundsInRoot()
        }
}

data class TourStep(
    val target: TourTarget,
    val titleRes: Int,
    val descRes: Int,
    val icon: ImageVector,
    val destination: Destination? = null,
    val overlay: Overlay? = null,
    val manageScope: Boolean = false,
)

val CoreTourSteps = listOf(
    // 1. Beranda - Usaha & Lahan
    TourStep(TourTarget.SCOPE_BUSINESS, R.string.tour_title_scope_business, R.string.tour_desc_scope_business, Icons.Default.Storefront, destination = Destination.HOME),
    // 2. Beranda - Proyeksi vs Realisasi
    TourStep(TourTarget.SCOPE_BOOK, R.string.tour_title_scope_book, R.string.tour_desc_scope_book, Icons.Default.SwapHoriz, destination = Destination.HOME),
    // 3. Beranda - Saldo Kas & Kesehatan Usaha
    TourStep(TourTarget.HERO_SUMMARY, R.string.tour_title_hero_summary, R.string.tour_desc_hero_summary, Icons.Default.AccountBalanceWallet, destination = Destination.HOME),
    // 4. Beranda - Distribusi Biaya 9 Kategori
    TourStep(TourTarget.HOME_COST_CHART, R.string.tour_title_cost_chart, R.string.tour_desc_cost_chart, Icons.Default.PieChart, destination = Destination.HOME),
    // 5. Catatan - Tab Switcher Buat vs Riwayat
    TourStep(TourTarget.ENTRY_TABS, R.string.tour_entry_tabs_title, R.string.tour_entry_tabs_desc, Icons.Default.SwapHoriz, destination = Destination.ENTRY),
    // 6. Catatan - Jenis Transaksi & Jurnal Otomatis
    TourStep(TourTarget.ENTRY_KINDS, R.string.tour_entry_kinds_title, R.string.tour_entry_kinds_desc, Icons.AutoMirrored.Filled.ReceiptLong, destination = Destination.ENTRY),
    // 7. Catatan - Riwayat, Koreksi & Hapus
    TourStep(TourTarget.ENTRY_HISTORY, R.string.tour_entry_history_title, R.string.tour_entry_history_desc, Icons.Default.History, destination = Destination.ENTRY),
    // 8. Laporan - Tiga Sudut Pandang
    TourStep(TourTarget.REPORTS_TABS, R.string.tour_reports_tabs_title, R.string.tour_reports_tabs_desc, Icons.Default.Assessment, destination = Destination.REPORTS),
    // 9. Laporan - Laba Rugi Operasional
    TourStep(TourTarget.REPORTS_PROFIT, R.string.tour_reports_profit_title, R.string.tour_reports_profit_desc, Icons.Default.Calculate, destination = Destination.REPORTS),
    // 10. Laporan - Arus Kas Operasi / Investasi / Pendanaan
    TourStep(TourTarget.REPORTS_CASHFLOW, R.string.tour_reports_cashflow_title, R.string.tour_reports_cashflow_desc, Icons.AutoMirrored.Filled.ShowChart, destination = Destination.REPORTS),
    // 11. Laporan - Kelayakan Agribisnis (BEP, HPP, B/C Ratio)
    TourStep(TourTarget.REPORTS_FEASIBILITY, R.string.tour_reports_feasibility_title, R.string.tour_reports_feasibility_desc, Icons.Default.Insights, destination = Destination.REPORTS),
    // 12. Berkas & Data - Impor Excel & Semantic Matcher
    TourStep(TourTarget.IMPORT_FILE_PICKER, R.string.tour_import_picker_title, R.string.tour_import_picker_desc, Icons.Default.UploadFile, overlay = Overlay.DATA),
    // 13. Berkas & Data - Target Buku Impor
    TourStep(TourTarget.IMPORT_TARGET_BOOK, R.string.tour_import_target_title, R.string.tour_import_target_desc, Icons.Default.Layers, overlay = Overlay.DATA),
    // 14. Berkas & Data - Ekspor PDF/Excel & Cadangan Terenkripsi
    TourStep(TourTarget.DATA_EXPORT, R.string.tour_data_export_title, R.string.tour_data_export_desc, Icons.Default.FileDownload, overlay = Overlay.DATA),
    // 15. Pengaturan - Cloud Sync Google Drive (drive.appdata)
    TourStep(TourTarget.SETTINGS_DRIVE, R.string.tour_settings_drive_title, R.string.tour_settings_drive_desc, Icons.Default.CloudUpload, overlay = Overlay.SETTINGS),
    // 16. Pengaturan - Enkripsi AES-256-GCM & Reset
    TourStep(TourTarget.SETTINGS_SECURITY, R.string.tour_settings_security_title, R.string.tour_settings_security_desc, Icons.Default.Security, overlay = Overlay.SETTINGS),
    // 17. Pengaturan - Tampilan, Bahasa & Gerakan
    TourStep(TourTarget.SETTINGS_DISPLAY, R.string.tour_settings_display_title, R.string.tour_settings_display_desc, Icons.Default.Palette, overlay = Overlay.SETTINGS),
)

/**
 * Catat. The four things a farmer has to decide for every row, in the order the form asks them.
 */
val EntryTourSteps = listOf(
    TourStep(TourTarget.ENTRY_TABS, R.string.tour_entry_tabs_title, R.string.tour_entry_tabs_desc, Icons.Default.SwapHoriz),
    TourStep(TourTarget.ENTRY_KINDS, R.string.tour_entry_kinds_title, R.string.tour_entry_kinds_desc, Icons.AutoMirrored.Filled.ReceiptLong),
    TourStep(TourTarget.ENTRY_HISTORY, R.string.tour_entry_history_title, R.string.tour_entry_history_desc, Icons.Default.History),
)

/**
 * Laporan. This is the tour that most needs to exist: BEP, HPP and B/C Ratio are the numbers the
 * app computes that a farmer cannot get from a notebook, and they are useless unless the meaning
 * and the action attached to each one are spelled out.
 */
val ReportsTourSteps = listOf(
    TourStep(TourTarget.REPORTS_TABS, R.string.tour_reports_tabs_title, R.string.tour_reports_tabs_desc, Icons.Default.Assessment),
    TourStep(TourTarget.REPORTS_PROFIT, R.string.tour_reports_profit_title, R.string.tour_reports_profit_desc, Icons.Default.Calculate),
    TourStep(TourTarget.REPORTS_CASHFLOW, R.string.tour_reports_cashflow_title, R.string.tour_reports_cashflow_desc, Icons.AutoMirrored.Filled.ShowChart),
    TourStep(TourTarget.REPORTS_FEASIBILITY, R.string.tour_reports_feasibility_title, R.string.tour_reports_feasibility_desc, Icons.Default.Insights),
)

val SettingsTourSteps = listOf(
    TourStep(TourTarget.SETTINGS_DRIVE, R.string.tour_settings_drive_title, R.string.tour_settings_drive_desc, Icons.Default.CloudUpload),
    TourStep(TourTarget.SETTINGS_SECURITY, R.string.tour_settings_security_title, R.string.tour_settings_security_desc, Icons.Default.Security),
    TourStep(TourTarget.SETTINGS_DISPLAY, R.string.tour_settings_display_title, R.string.tour_settings_display_desc, Icons.Default.Palette),
)

val ScopeTourSteps = listOf(
    TourStep(TourTarget.SCOPE_ADD_BUSINESS, R.string.tour_scope_business_title, R.string.tour_scope_business_desc, Icons.Default.AddBusiness),
    TourStep(TourTarget.SCOPE_ADD_FIELD, R.string.tour_scope_field_title, R.string.tour_scope_field_desc, Icons.Default.Landscape),
)

val ImportTourSteps = listOf(
    TourStep(TourTarget.IMPORT_FILE_PICKER, R.string.tour_import_picker_title, R.string.tour_import_picker_desc, Icons.Default.UploadFile),
    TourStep(TourTarget.IMPORT_TARGET_BOOK, R.string.tour_import_target_title, R.string.tour_import_target_desc, Icons.Default.Layers),
    TourStep(TourTarget.DATA_EXPORT, R.string.tour_data_export_title, R.string.tour_data_export_desc, Icons.Default.FileDownload),
)

/** Padding around the measured target so the cutout frames the component instead of clipping it. */
private const val HOLE_INSET_DP = 6f
private const val HOLE_CORNER_DP = 20f
private const val CARD_GAP_DP = 16f

/**
 * Gate for [SpotlightTourContent].
 *
 * Targets publish their bounds from `onGloballyPositioned`, which runs *after* the first layout
 * pass. Composing the overlay before that lost a race twice over: the step list was built from an
 * empty registry (so it briefly said "1 of 3", then silently became "1 of 2" once the real targets
 * arrived), and the cutout's position animations started from 0 and swept in from the top-left
 * corner. Waiting for settled frames costs nothing the user can perceive and removes both.
 */
@Composable
fun SpotlightTour(
    steps: List<TourStep> = CoreTourSteps,
    onStepChange: ((TourStep) -> Unit)? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (steps.isEmpty()) return
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        // Two frames: the first lets layout run, the second lets every onGloballyPositioned
        // callback from that pass land in the registry before the tour begins.
        withFrameNanos {}
        withFrameNanos {}
        ready = true
    }
    if (!ready) return
    SpotlightTourContent(steps, onStepChange, onDismiss, modifier)
}

@Composable
private fun SpotlightTourContent(
    tourSteps: List<TourStep>,
    onStepChange: ((TourStep) -> Unit)?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val registry = LocalTourTargets.current
    val motion = LocalMotion.current
    val density = LocalDensity.current

    val steps = tourSteps
    var stepIndex by remember { mutableIntStateOf(0) }
    val index = stepIndex.coerceIn(0, steps.lastIndex)
    val step = steps[index]
    val isLast = index == steps.lastIndex

    // Notify parent on step change for multi-screen live navigation
    LaunchedEffect(index) {
        onStepChange?.invoke(step)
    }

    // Auto-scroll target into view if outside viewport
    LaunchedEffect(step.target) {
        try {
            withFrameNanos {}
            registry.requesters[step.target]?.bringIntoView()
        } catch (_: Exception) {}
    }

    var overlayOrigin by remember { mutableStateOf(Offset.Zero) }
    var overlayHeightPx by remember { mutableStateOf(0f) }
    var cardHeightPx by remember { mutableStateOf(0f) }
    var isDismissing by remember { mutableStateOf(false) }
    var entered by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        entered = true
    }

    fun triggerDismiss() {
        if (motion.enabled && !isDismissing) {
            isDismissing = true
        } else {
            onDismiss()
        }
    }

    BackHandler(enabled = true) {
        triggerDismiss()
    }

    LaunchedEffect(isDismissing) {
        if (isDismissing) {
            delay(maxOf(motion.mediumMillis, 1).toLong())
            onDismiss()
        }
    }

    val inset = with(density) { HOLE_INSET_DP.dp.toPx() }
    val target = registry.bounds[step.target]
    val hole = target?.let {
        Rect(
            left = it.left - overlayOrigin.x - inset,
            top = it.top - overlayOrigin.y - inset,
            right = it.right - overlayOrigin.x + inset,
            bottom = it.bottom - overlayOrigin.y + inset,
        )
    }

    // Motion specs with easing curves
    val animDuration = if (motion.enabled) maxOf(motion.mediumMillis, 1) else 0
    val motionSpec = tween<Float>(durationMillis = animDuration, easing = FastOutSlowInEasing)

    // Smooth hole coordinates
    val left by animateFloatAsState(hole?.left ?: 0f, motionSpec, label = "holeLeft")
    val top by animateFloatAsState(hole?.top ?: 0f, motionSpec, label = "holeTop")
    val right by animateFloatAsState(hole?.right ?: 0f, motionSpec, label = "holeRight")
    val bottom by animateFloatAsState(hole?.bottom ?: 0f, motionSpec, label = "holeBottom")

    // Overlay alpha and card scale for enter/exit
    val scrimAlpha by animateFloatAsState(
        targetValue = if (entered && !isDismissing) 0.72f else 0f,
        animationSpec = motionSpec,
        label = "scrimAlpha",
    )
    val cardAlpha by animateFloatAsState(
        targetValue = if (entered && !isDismissing) 1f else 0f,
        animationSpec = motionSpec,
        label = "cardAlpha",
    )
    val cardScale by animateFloatAsState(
        targetValue = if (entered && !isDismissing) 1f else 0.92f,
        animationSpec = motionSpec,
        label = "cardScale",
    )

    val pulse = if (motion.enabled && !isDismissing) {
        rememberInfiniteTransition(label = "spotlightPulse").animateFloat(
            initialValue = 0.35f,
            targetValue = 0.9f,
            animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
            label = "spotlightPulseAlpha",
        ).value
    } else {
        0.7f
    }

    val ringColor = MaterialTheme.colorScheme.primary
    val corner = with(density) { HOLE_CORNER_DP.dp.toPx() }
    val gapPx = with(density) { CARD_GAP_DP.dp.toPx() }
    val minTopPaddingPx = with(density) { 56.dp.toPx() }
    val minBottomPaddingPx = with(density) { 56.dp.toPx() }

    // Compute continuous target Y position for the card so it smoothly glides without alignment hops
    val targetCenterY = hole?.let { (it.top + it.bottom) / 2f } ?: (overlayHeightPx / 2f)
    val placeBelow = targetCenterY < (overlayHeightPx / 2f)

    val targetCardY = if (hole == null || overlayHeightPx <= 0f) {
        ((overlayHeightPx - cardHeightPx) / 2f).coerceAtLeast(minTopPaddingPx)
    } else if (placeBelow) {
        val preferred = hole.bottom + gapPx
        preferred.coerceIn(minTopPaddingPx, (overlayHeightPx - cardHeightPx - minBottomPaddingPx).coerceAtLeast(minTopPaddingPx))
    } else {
        val preferred = hole.top - gapPx - cardHeightPx
        preferred.coerceIn(minTopPaddingPx, (overlayHeightPx - cardHeightPx - minBottomPaddingPx).coerceAtLeast(minTopPaddingPx))
    }

    val cardY by animateFloatAsState(
        targetValue = targetCardY,
        animationSpec = motionSpec,
        label = "tourCardY",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(100f)
            .onGloballyPositioned {
                overlayOrigin = it.positionInRoot()
                overlayHeightPx = it.size.height.toFloat()
            },
    ) {
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            event.changes.forEach { it.consume() }
                        }
                    }
                },
        ) {
            drawRect(Color.Black.copy(alpha = scrimAlpha))
            if (hole != null && scrimAlpha > 0f) {
                val topLeft = Offset(left, top)
                val size = Size((right - left).coerceAtLeast(0f), (bottom - top).coerceAtLeast(0f))
                drawRoundRect(
                    color = Color.Black,
                    topLeft = topLeft,
                    size = size,
                    cornerRadius = CornerRadius(corner, corner),
                    blendMode = BlendMode.Clear,
                )
                drawRoundRect(
                    color = ringColor.copy(alpha = (pulse * (scrimAlpha / 0.72f)).coerceIn(0f, 1f)),
                    topLeft = topLeft,
                    size = size,
                    cornerRadius = CornerRadius(corner, corner),
                    style = Stroke(width = 3.dp.toPx()),
                )
            }
        }

        TourCard(
            step = step,
            stepNumber = index + 1,
            stepCount = steps.size,
            isLast = isLast,
            onBack = { if (index > 0) stepIndex = index - 1 },
            onNext = { if (isLast) triggerDismiss() else stepIndex = index + 1 },
            onSkip = { triggerDismiss() },
            motionMillis = animDuration,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .offset { IntOffset(0, cardY.roundToInt()) }
                .graphicsLayer {
                    alpha = cardAlpha
                    scaleX = cardScale
                    scaleY = cardScale
                }
                .onGloballyPositioned {
                    cardHeightPx = it.size.height.toFloat()
                },
        )
    }
}

@Composable
private fun TourCard(
    step: TourStep,
    stepNumber: Int,
    stepCount: Int,
    isLast: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    motionMillis: Int,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
    ) {
        AnimatedContent(
            targetState = step to stepNumber,
            transitionSpec = {
                val forward = targetState.second >= initialState.second
                val enter = fadeIn(tween(maxOf(motionMillis, 1))) +
                    slideInHorizontally(tween(maxOf(motionMillis, 1))) { full -> if (forward) full / 6 else -full / 6 }
                val exit = fadeOut(tween(maxOf(motionMillis / 2, 1)))
                enter togetherWith exit
            },
            label = "tourStep",
        ) { (current, number) ->
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(44.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                current.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.tour_step_counter, number, stepCount),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            stringResource(current.titleRes),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }

                Text(
                    stringResource(current.descRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                StepDots(current = number - 1, count = stepCount)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onSkip) { Text(stringResource(R.string.tour_btn_skip)) }
                    Spacer(Modifier.weight(1f))
                    if (number > 1) {
                        TextButton(onClick = onBack) { Text(stringResource(R.string.tour_btn_back)) }
                    }
                    Button(onClick = onNext) {
                        Text(
                            if (isLast) stringResource(R.string.tour_btn_finish) else stringResource(R.string.tour_btn_next),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Step indicators. Sized and coloured for a phone held outdoors: the previous 6dp
 * `outlineVariant` dots were effectively invisible at low brightness. The active step is a wide
 * pill in `primary`; inactive steps are 10dp dots in `onSurfaceVariant`, which is a real text-grade
 * contrast token rather than a hairline-divider token.
 */
@Composable
private fun StepDots(current: Int, count: Int) {
    val motion = LocalMotion.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            val active = index == current
            val width by animateFloatAsState(
                targetValue = if (active) 28f else 10f,
                animationSpec = tween(durationMillis = if (motion.enabled) maxOf(motion.fastMillis, 1) else 0),
                label = "dotWidth",
            )
            Box(
                modifier = Modifier
                    .height(10.dp)
                    .width(width.dp)
                    .clip(CircleShape)
                    .background(
                        if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                    ),
            )
        }
    }
}
