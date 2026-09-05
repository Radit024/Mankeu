package com.arinahub.umkm.mankeu.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arinahub.umkm.mankeu.presentation.LocalMotion

data class NavAction(
    val label: String,
    val icon: ImageVector,
    val selected: Boolean,
    val onClick: () -> Unit,
)

/**
 * One always-visible button that opens a short vertical stack of labelled destination buttons,
 * replacing the persistent bottom navigation bar. Requested directly by the owner: "daripada
 * navbar yang sticky, mending kaya satu tombol yang ketika dipencet muncul tombol-tombol lain
 * gitu... jadi lebih clean".
 *
 * Design notes:
 * - Only one control occupies the content area at rest, so long lists are not permanently
 *   shortened by a bar across the bottom.
 * - Each expanded item keeps a text label next to its icon; an icon-only radial menu would be
 *   less legible for the intended audience, not more.
 * - Opening dims and blocks the content behind it, so tapping anywhere outside closes the menu
 *   rather than hitting something by accident.
 * - All motion honours the owner's Gerakan setting via [LocalMotion]; with motion off the menu
 *   simply appears and disappears.
 */
@Composable
fun ExpandingNav(
    actions: List<NavAction>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    openLabel: String,
    closeLabel: String,
    modifier: Modifier = Modifier,
) {
    val motion = LocalMotion.current
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = tween(durationMillis = motion.fastMillis),
        label = "nav-rotation",
    )

    Box(modifier.fillMaxSize()) {
        // Scrim: only present while open, and swallows taps so "tap outside to close" works.
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(motion.fastMillis)),
            exit = fadeOut(tween(motion.fastMillis)),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onExpandedChange(false) },
            )
        }

        Column(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            actions.forEachIndexed { index, action ->
                AnimatedVisibility(
                    visible = expanded,
                    enter = fadeIn(tween(motion.fastMillis, delayMillis = index * (motion.fastMillis / 4))) +
                        scaleIn(tween(motion.fastMillis, delayMillis = index * (motion.fastMillis / 4)), initialScale = 0.8f),
                    exit = fadeOut(tween(motion.fastMillis)) + scaleOut(tween(motion.fastMillis), targetScale = 0.8f),
                ) {
                    NavActionRow(action) {
                        action.onClick()
                        onExpandedChange(false)
                    }
                }
            }

            FloatingActionButton(
                onClick = { onExpandedChange(!expanded) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
            ) {
                Icon(
                    if (expanded) Icons.Default.Close else Icons.Default.Menu,
                    contentDescription = if (expanded) closeLabel else openLabel,
                    modifier = Modifier.rotate(if (expanded) 0f else rotation),
                )
            }
        }
    }
}

@Composable
private fun NavActionRow(action: NavAction, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shadowElevation = 3.dp,
        ) {
            Text(
                action.label,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(Modifier.width(12.dp))
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = if (action.selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = if (action.selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
            shadowElevation = 4.dp,
            modifier = Modifier.size(52.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(action.icon, contentDescription = action.label)
            }
        }
    }
}

