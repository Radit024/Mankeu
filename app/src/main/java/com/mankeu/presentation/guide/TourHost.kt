package com.mankeu.presentation.guide

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mankeu.presentation.components.SpotlightTour

/**
 * Offer, seen and "not now" state of page tours, shared by the shell and by sheets that carry a
 * tour of their own (the entry form, Usaha dan Unit). "Nanti" lasts until the app is opened again.
 */
class TourController(
    private val shouldOffer: (Tour) -> Boolean,
    private val markSeen: (Tour) -> Unit,
    private val disableOffers: () -> Unit,
    val openHelp: (HelpArticle) -> Unit,
    private val later: MutableCollection<Tour>,
) {
    fun offer(tour: Tour): Boolean = shouldOffer(tour) && tour !in later
    fun seen(tour: Tour) = markSeen(tour)
    fun notNow(tour: Tour) { later += tour }
    fun never() = disableOffers()
}

val LocalTourController = staticCompositionLocalOf<TourController?> { null }

/** A tour that lives inside a bottom sheet. [running] is also set by the sheet's own ? button. */
class SheetTourState(val tour: Tour) {
    var running by mutableStateOf(false)
    var offerHidden by mutableStateOf(false)
}

@Composable
fun rememberSheetTour(tour: Tour) = remember(tour) { SheetTourState(tour) }

/** Place inside the sheet's root `Box`, after its content: the offer card on top, then the tour. */
@Composable
fun BoxScope.SheetTour(state: SheetTourState) {
    val controller = LocalTourController.current ?: return
    val tour = state.tour
    if (state.running) {
        SpotlightTour(
            steps = tour.steps,
            onDismiss = { state.running = false; controller.seen(tour) },
            modifier = Modifier.matchParentSize(),
            isBottomSheet = true,
            onLearnMore = { article -> state.running = false; controller.seen(tour); controller.openHelp(article) },
        )
    } else if (!state.offerHidden && controller.offer(tour)) {
        TourOfferCard(
            stepCount = tour.steps.size,
            onStart = { state.running = true },
            onLater = { controller.notNow(tour); state.offerHidden = true },
            onNever = { controller.never(); state.offerHidden = true },
            modifier = Modifier.align(Alignment.TopCenter).padding(horizontal = 12.dp),
        )
    }
}
