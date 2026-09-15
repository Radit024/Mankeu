package com.mankeu.presentation.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import com.mankeu.presentation.FeedbackEvent
import com.mankeu.presentation.LocalFeedback
import kotlinx.coroutines.flow.filter

/**
 * A long-press tooltip, and nothing but Material 3's own: [TooltipBox] with a [PlainTooltip].
 *
 * `TooltipBox` owns the gesture, the placement (above the anchor, flipped below when there is no
 * room, kept inside the window), the dismissal (finger lifted, outside tap or timeout), the
 * accessibility semantics, and it consumes the release of a long press so holding an icon to read
 * its label does not also run the action. The only thing added here is the long-press haptic at the
 * moment the tooltip appears, which `TooltipBox` does not fire itself (D-084).
 *
 * It reacts to touch and stylus pointers only. `adb shell input` injects pointers of unknown type,
 * which is why a scripted long-press shows no tooltip and clicks through; verify with a Compose UI
 * test or a real (or emulator-window) touch instead.
 *
 * Use it for icon-only actions, status badges and figures that can be truncated — anything whose
 * meaning is not fully spelled out on screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MankeuTooltip(
    text: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val state = rememberTooltipState()
    val feedback = LocalFeedback.current
    LaunchedEffect(state, feedback) {
        snapshotFlow { state.isVisible }.filter { it }.collect { feedback(FeedbackEvent.LONG_PRESS) }
    }
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(text) } },
        state = state,
        modifier = modifier,
        content = content,
    )
}
