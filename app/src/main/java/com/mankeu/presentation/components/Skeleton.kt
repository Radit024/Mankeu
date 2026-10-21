package com.mankeu.presentation.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mankeu.presentation.LocalMotion

/**
 * Placeholder blocks shown while the first database read is still in flight, instead of a blank
 * screen or a bare spinner. Respects the owner's motion setting: with animation off the blocks
 * are still shown (so the layout does not jump when data lands) but stop pulsing.
 */
@Composable
fun SkeletonBlock(
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
    cornerRadius: Dp = 8.dp,
) {
    val motion = LocalMotion.current
    val alpha by if (motion.expressive) {
        val transition = rememberInfiniteTransition(label = "skeleton")
        transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 0.75f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = (motion.mediumMillis * 3).coerceAtLeast(240)),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "skeleton-alpha",
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0.5f) }
    }

    Box(
        modifier
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha * 0.35f)),
    )
}

/** Skeleton stand-in for the Beranda hero card. */
@Composable
fun SkeletonHero(modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SkeletonBlock(Modifier.fillMaxWidth(0.5f), height = 14.dp)
            SkeletonBlock(Modifier.fillMaxWidth(0.75f), height = 34.dp, cornerRadius = 12.dp)
            SkeletonBlock(Modifier.fillMaxWidth(0.6f), height = 12.dp)
        }
    }
}

/** Skeleton stand-in for the two-by-two metric grid. */
@Composable
fun SkeletonMetricGrid(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(2) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(2) {
                    Card(Modifier.weight(1f)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SkeletonBlock(Modifier.fillMaxWidth(0.3f), height = 30.dp, cornerRadius = 12.dp)
                            SkeletonBlock(Modifier.fillMaxWidth(0.7f), height = 10.dp)
                            SkeletonBlock(Modifier.fillMaxWidth(0.55f), height = 18.dp)
                        }
                    }
                }
            }
        }
    }
}

/** Skeleton stand-in for a list of transaction rows. */
@Composable
fun SkeletonListItem(modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SkeletonBlock(Modifier.height(40.dp).fillMaxWidth(0.12f), height = 40.dp, cornerRadius = 20.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SkeletonBlock(Modifier.fillMaxWidth(0.7f), height = 14.dp)
                SkeletonBlock(Modifier.fillMaxWidth(0.45f), height = 10.dp)
            }
        }
    }
}
