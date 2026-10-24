package com.mankeu.presentation.components

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import com.mankeu.presentation.MankeuMotion

/**
 * Reusable motion patterns adhering to the Mankeu Motion Contract.
 *
 * Rules:
 * - Full: expressive transitions (fade-through with sequential phases, shared-axis with spatial displacement, scale + fade).
 * - Reduced: fade-only transitions (no spatial travel, displacement, scale, or expand/shrink), max 140ms.
 * - Off: instant transition (EnterTransition.None togetherWith ExitTransition.None, zero delay/duration).
 */
object MankeuTransitions {
    fun fadeThrough(motion: MankeuMotion): ContentTransform = when {
        !motion.enabled -> EnterTransition.None togetherWith ExitTransition.None
        !motion.expressive -> {
            // Reduced: Sequential fade without spatial travel, total <= 140ms
            fadeIn(
                animationSpec = tween(durationMillis = 90, delayMillis = 50, easing = FastOutSlowInEasing),
            ) togetherWith fadeOut(
                animationSpec = tween(durationMillis = 50, easing = FastOutSlowInEasing),
            )
        }
        else -> {
            // Full: Fade-through: exit 90ms, enter 180ms after exit phase (delay 90ms), total 270ms. No spatial travel.
            fadeIn(
                animationSpec = tween(durationMillis = 180, delayMillis = 90, easing = FastOutSlowInEasing),
            ) togetherWith fadeOut(
                animationSpec = tween(durationMillis = 90, easing = FastOutSlowInEasing),
            )
        }
    }

    fun sharedAxisX(
        motion: MankeuMotion,
        forward: Boolean,
        slideDistancePx: Int = 0,
    ): ContentTransform = when {
        !motion.enabled -> EnterTransition.None togetherWith ExitTransition.None
        !motion.expressive -> {
            // Reduced: fade only, max 140ms, no displacement
            fadeIn(
                animationSpec = tween(durationMillis = motion.mediumMillis, easing = FastOutSlowInEasing),
            ) togetherWith fadeOut(
                animationSpec = tween(durationMillis = motion.fastMillis, easing = FastOutSlowInEasing),
            )
        }
        else -> {
            // Full: Shared-axis X with slide + fade. Enter 320ms, exit 180ms.
            val enterOffset = if (forward) slideDistancePx else -slideDistancePx
            val exitOffset = if (forward) -slideDistancePx else slideDistancePx
            (slideInHorizontally(
                animationSpec = tween(durationMillis = motion.mediumMillis, easing = FastOutSlowInEasing),
            ) { enterOffset } + fadeIn(
                animationSpec = tween(durationMillis = motion.mediumMillis, easing = FastOutSlowInEasing),
            )) togetherWith (slideOutHorizontally(
                animationSpec = tween(durationMillis = motion.fastMillis, easing = FastOutSlowInEasing),
            ) { exitOffset } + fadeOut(
                animationSpec = tween(durationMillis = motion.fastMillis, easing = FastOutSlowInEasing),
            ))
        }
    }

    fun sharedAxisZ(
        motion: MankeuMotion,
        opening: Boolean,
    ): ContentTransform = when {
        !motion.enabled -> EnterTransition.None togetherWith ExitTransition.None
        !motion.expressive -> {
            // Reduced: fade only, max 140ms, no scale or vertical slide
            fadeIn(
                animationSpec = tween(durationMillis = motion.mediumMillis, easing = FastOutSlowInEasing),
            ) togetherWith fadeOut(
                animationSpec = tween(durationMillis = motion.fastMillis, easing = FastOutSlowInEasing),
            )
        }
        else -> {
            // Full: Maintain scale + fade + slight vertical shift, 320/180 ms; reverse on return
            val enterMillis = motion.mediumMillis
            val exitMillis = motion.fastMillis
            if (opening) {
                (slideInVertically(tween(enterMillis, easing = FastOutSlowInEasing)) { full -> full / 5 } +
                    fadeIn(tween(enterMillis, easing = FastOutSlowInEasing))) togetherWith (
                    fadeOut(tween(exitMillis, easing = FastOutSlowInEasing)) +
                        scaleOut(tween(exitMillis, easing = FastOutSlowInEasing), targetScale = 0.97f)
                )
            } else {
                (fadeIn(tween(enterMillis, easing = FastOutSlowInEasing)) +
                    scaleIn(tween(enterMillis, easing = FastOutSlowInEasing), initialScale = 0.97f)) togetherWith (
                    slideOutVertically(tween(exitMillis, easing = FastOutSlowInEasing)) { full -> full / 5 } +
                        fadeOut(tween(exitMillis, easing = FastOutSlowInEasing))
                )
            }
        }
    }

    fun crossFade(motion: MankeuMotion): ContentTransform = when {
        !motion.enabled -> EnterTransition.None togetherWith ExitTransition.None
        else -> {
            fadeIn(
                animationSpec = tween(durationMillis = maxOf(motion.fastMillis, 90), easing = FastOutSlowInEasing),
            ) togetherWith fadeOut(
                animationSpec = tween(durationMillis = maxOf(motion.fastMillis / 2, 45), easing = FastOutSlowInEasing),
            )
        }
    }
}

/** Extension helper on [AnimatedContentTransitionScope] for convenience. */
fun <S> AnimatedContentTransitionScope<S>.fadeThrough(motion: MankeuMotion): ContentTransform =
    MankeuTransitions.fadeThrough(motion)

fun <S> AnimatedContentTransitionScope<S>.sharedAxisX(
    motion: MankeuMotion,
    forward: Boolean,
    slideDistancePx: Int = 0,
): ContentTransform = MankeuTransitions.sharedAxisX(motion, forward, slideDistancePx)

fun <S> AnimatedContentTransitionScope<S>.sharedAxisZ(
    motion: MankeuMotion,
    opening: Boolean,
): ContentTransform = MankeuTransitions.sharedAxisZ(motion, opening)

fun <S> AnimatedContentTransitionScope<S>.crossFade(motion: MankeuMotion): ContentTransform =
    MankeuTransitions.crossFade(motion)

fun LazyItemScope.itemMotion(
    motion: MankeuMotion,
    modifier: Modifier = Modifier,
): Modifier = when {
    !motion.enabled -> modifier
    !motion.expressive -> modifier.animateItem(
        fadeInSpec = tween<Float>(durationMillis = motion.fastMillis, easing = FastOutSlowInEasing),
        placementSpec = null,
        fadeOutSpec = tween<Float>(durationMillis = motion.fastMillis, easing = FastOutSlowInEasing),
    )
    else -> modifier.animateItem(
        fadeInSpec = tween<Float>(durationMillis = motion.fastMillis, easing = FastOutSlowInEasing),
        placementSpec = tween<IntOffset>(durationMillis = motion.fastMillis, easing = FastOutSlowInEasing),
        fadeOutSpec = tween<Float>(durationMillis = motion.fastMillis / 2, easing = FastOutSlowInEasing),
    )
}

fun conditionalEnter(motion: MankeuMotion): EnterTransition = when {
    !motion.enabled -> EnterTransition.None
    !motion.expressive -> fadeIn(tween(motion.fastMillis, easing = FastOutSlowInEasing))
    else -> expandVertically(tween(motion.mediumMillis, easing = FastOutSlowInEasing)) +
        fadeIn(tween(motion.mediumMillis, easing = FastOutSlowInEasing))
}

fun conditionalExit(motion: MankeuMotion): ExitTransition = when {
    !motion.enabled -> ExitTransition.None
    !motion.expressive -> fadeOut(tween(motion.fastMillis, easing = FastOutSlowInEasing))
    else -> shrinkVertically(tween(motion.fastMillis, easing = FastOutSlowInEasing)) +
        fadeOut(tween(motion.fastMillis, easing = FastOutSlowInEasing))
}
