package com.arinahub.umkm.mankeu.presentation

import android.media.AudioManager
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView

/**
 * Touch feedback: a short vibration and a short sound, on the few moments that deserve one.
 *
 * Both go through the platform rather than a vibrator or an audio asset of our own, and that is the
 * whole design. `View.performHapticFeedback` and `View.playSoundEffect` already obey the phone's own
 * "Touch vibration" and "Touch sounds" settings, the silent/vibrate ringer mode, and the
 * accessibility switches — so the system setting is honoured for free and cannot drift out of sync
 * with it. The in-app switches can only make this app quieter than the phone already is, never
 * louder, which is the same rule [ArinaMotion.resolve] applies to animation.
 *
 * No audio files ship with the app. No `VIBRATE` permission is needed: haptic feedback routed
 * through a `View` is granted to any app for its own views.
 */
enum class FeedbackEvent {
    /** A choice was made — a tab, a segment, a category chip. */
    TAP,

    /** Something was written: a transaction saved, an import committed, a backup created. */
    CONFIRM,

    /** The app refused: validation failed, a password was wrong. */
    REJECT,
}

class ArinaFeedback(
    private val view: View?,
    private val haptics: Boolean,
    private val sound: Boolean,
) {
    operator fun invoke(event: FeedbackEvent) {
        val target = view ?: return
        if (haptics) target.performHapticFeedback(event.hapticConstant())
        if (sound) target.playSoundEffect(event.soundConstant())
    }

    private fun FeedbackEvent.hapticConstant(): Int = when (this) {
        // CONFIRM and REJECT are the API 30 additions that actually feel different from a tap —
        // a double tick versus a heavier buzz. Below that there is no equivalent, so the closest
        // long-standing constants stand in rather than inventing a pattern with a Vibrator.
        FeedbackEvent.TAP -> HapticFeedbackConstants.KEYBOARD_TAP
        FeedbackEvent.CONFIRM ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM
            else HapticFeedbackConstants.VIRTUAL_KEY
        FeedbackEvent.REJECT ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.REJECT
            else HapticFeedbackConstants.LONG_PRESS
    }

    private fun FeedbackEvent.soundConstant(): Int = when (this) {
        FeedbackEvent.TAP, FeedbackEvent.CONFIRM -> SoundEffectConstants.CLICK
        // There is no "error" effect in SoundEffectConstants; the navigation blip is the closest
        // thing the platform offers and is still quieter than shipping an audio file.
        FeedbackEvent.REJECT -> AudioManager.FX_KEYPRESS_INVALID
    }
}

val LocalFeedback = staticCompositionLocalOf { ArinaFeedback(null, haptics = false, sound = false) }

@Composable
fun rememberFeedback(haptics: Boolean, sound: Boolean): ArinaFeedback {
    val view = LocalView.current
    return remember(view, haptics, sound) { ArinaFeedback(view, haptics, sound) }
}
