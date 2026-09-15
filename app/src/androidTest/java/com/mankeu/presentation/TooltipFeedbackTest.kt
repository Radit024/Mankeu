package com.mankeu.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mankeu.presentation.components.MankeuTooltip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The tooltip contract (D-084) and its haptic (D-085), checked with a recording [HapticFeedback]:
 * the emulator has no vibrator, so a vibration cannot be observed there, but the call that would
 * produce one can.
 */
@RunWith(AndroidJUnit4::class)
class TooltipFeedbackTest {
    @get:Rule
    val compose = createComposeRule()

    private class RecordingHaptics : HapticFeedback {
        val events = mutableListOf<HapticFeedbackType>()
        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
            events += hapticFeedbackType
        }
    }

    private var clicks = 0
    private val haptics = RecordingHaptics()

    private fun setAnchor() = compose.setContent {
        CompositionLocalProvider(LocalHapticFeedback provides haptics) {
            CompositionLocalProvider(LocalFeedback provides rememberFeedback(haptics = true, sound = false)) {
                MaterialTheme {
                    Box(Modifier.padding(96.dp)) {
                        MankeuTooltip("Berkas dan Data") {
                            IconButton(onClick = { clicks++ }, modifier = Modifier.testTag("anchor")) {
                                Icon(Icons.Default.FolderOpen, contentDescription = "Data")
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun longPressShowsTooltipWithLongPressHapticAndDoesNotRunTheAction() {
        setAnchor()

        compose.onNodeWithTag("anchor").performTouchInput { longClick() }

        compose.onNodeWithText("Berkas dan Data").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals("A long-press must not also click", 0, clicks)
            assertTrue(HapticFeedbackType.LongPress in haptics.events)
        }
    }

    @Test
    fun tapRunsTheActionWithoutTooltipHaptic() {
        setAnchor()

        compose.onNodeWithTag("anchor").performClick()

        compose.runOnIdle {
            assertEquals(1, clicks)
            assertFalse(HapticFeedbackType.LongPress in haptics.events)
        }
    }
}
