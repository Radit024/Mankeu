package com.mankeu.presentation.components

import androidx.annotation.RawRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.testTag
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.mankeu.R
import com.mankeu.presentation.LocalMotion
import com.mankeu.presentation.isDarkScheme

enum class StateArtwork(
    @param:RawRes val resource: Int,
    @param:RawRes val darkResource: Int = resource,
    val stillProgress: Float = 1f,
    val fallback: ImageVector = Icons.Default.Description,
) {
    EMPTY(R.raw.mankeu_empty),
    SUCCESS(R.raw.mankeu_success, fallback = Icons.Default.CheckCircleOutline),
    BUSINESS_PLAN(R.raw.arina_business_plan, R.raw.arina_business_plan_dark, 0.5f),
    REPORTS(R.raw.arina_growth_chart, R.raw.arina_growth_chart_dark, 0.5f, Icons.Default.Assessment),
    STOCK(R.raw.arina_stock_box, R.raw.arina_stock_box_dark, 0.5f, Icons.Default.Inventory2),
    IMPORT(R.raw.arina_file_import, R.raw.arina_file_import_dark, 0.5f, Icons.Default.UploadFile),
    CLOUD_BACKUP(R.raw.arina_cloud_backup, R.raw.arina_cloud_backup_dark, 0.5f, Icons.Default.CloudUpload),
}

/**
 * A decorative, offline-only composition. Text beside it carries the meaning for accessibility.
 * Playback is one-shot and pauses off-screen. Reduced/Off and completed one-shots hold a reviewed
 * nonblank frame (third-party loops can end on an empty reset frame); a local
 * resource that cannot load falls back to its Material icon instead of leaving a blank hole.
 */
@Composable
fun StateIllustration(artwork: StateArtwork, modifier: Modifier = Modifier) {
    val resource = if (isDarkScheme()) artwork.darkResource else artwork.resource
    val result = rememberLottieComposition(LottieCompositionSpec.RawRes(resource))
    val motion = LocalMotion.current
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val view = LocalView.current
    var visible by remember { mutableStateOf(false) }
    Box(
        modifier.onGloballyPositioned { coordinates ->
            val bounds = coordinates.boundsInWindow()
            val inWindow = bounds.width > 0 && bounds.height > 0 && bounds.right > 0 && bounds.bottom > 0 &&
                bounds.left < view.width && bounds.top < view.height
            if (visible != inWindow) visible = inWindow
        }.clearAndSetSemantics { testTag = "artwork-${artwork.name}" },
        contentAlignment = Alignment.Center,
    ) {
        val composition = result.value
        if (composition == null || result.isFailure) {
            Icon(
                imageVector = artwork.fallback,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxSize(0.6f),
            )
        } else {
            // Don't even create an animation clock in Reduced/Off mode.
            val progress = if (motion.expressive) {
                val animation = animateLottieCompositionAsState(
                    composition = composition,
                    isPlaying = visible && lifecycleState.isAtLeast(Lifecycle.State.STARTED),
                    iterations = 1,
                )
                if (animation.isAtEnd) artwork.stillProgress else animation.progress
            } else artwork.stillProgress
            LottieAnimation(composition = composition, progress = { progress }, modifier = Modifier.fillMaxSize())
        }
    }
}
