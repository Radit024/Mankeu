package com.mankeu.presentation.components

import androidx.annotation.RawRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.mankeu.R
import com.mankeu.presentation.LocalMotion

enum class StateArtwork(@param:RawRes val resource: Int) {
    EMPTY(R.raw.mankeu_empty),
    SUCCESS(R.raw.mankeu_success),
}

/**
 * A decorative, offline-only composition. Text beside it carries the meaning for accessibility.
 * Playback is one-shot and stops off-screen. Reduced/Off show the complete final frame; a local
 * resource that cannot load falls back to its Material icon instead of leaving a blank hole.
 */
@Composable
fun StateIllustration(artwork: StateArtwork, modifier: Modifier = Modifier) {
    val result = rememberLottieComposition(LottieCompositionSpec.RawRes(artwork.resource))
    val motion = LocalMotion.current
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    Box(modifier.clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
        val composition = result.value
        if (composition == null || result.isFailure) {
            Icon(
                imageVector = if (artwork == StateArtwork.SUCCESS) Icons.Default.CheckCircleOutline else Icons.Default.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxSize(0.6f),
            )
        } else {
            // Don't even create an animation clock in Reduced/Off mode.
            val progress = if (motion.expressive) {
                val animated by animateLottieCompositionAsState(
                    composition = composition,
                    isPlaying = lifecycleState.isAtLeast(Lifecycle.State.STARTED),
                    iterations = 1,
                )
                animated
            } else 1f
            LottieAnimation(composition = composition, progress = { progress }, modifier = Modifier.fillMaxSize())
        }
    }
}
