package com.mankeu.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mankeu.presentation.components.fadeThrough
import com.mankeu.presentation.components.sharedAxisX
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationMotionTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun fadeThroughFixtureReachesTargetAndHandlesInterruption() {
        var screen by mutableStateOf("A")
        var actionCount = 0

        compose.setContent {
            MaterialTheme {
                AnimatedContent(
                    targetState = screen,
                    transitionSpec = { fadeThrough(MankeuMotion.Full) },
                    label = "testFadeThrough",
                ) { currentScreen ->
                    Box(Modifier.fillMaxSize()) {
                        Button(
                            onClick = { actionCount++ },
                            modifier = Modifier.testTag("actionBtn_$currentScreen"),
                        ) {
                            Text("Screen $currentScreen")
                        }
                    }
                }
            }
        }

        // Initially Screen A
        compose.onNodeWithTag("actionBtn_A").assertIsDisplayed().performClick()
        assertEquals(1, actionCount)

        // Switch with controlled clock
        compose.mainClock.autoAdvance = false
        screen = "B"
        compose.mainClock.advanceTimeByFrame()

        // Interrupt back to A before completion: A -> B -> A
        screen = "A"
        compose.mainClock.advanceTimeBy(300)
        compose.mainClock.autoAdvance = true

        compose.onNodeWithTag("actionBtn_A").assertIsDisplayed().performClick()
        assertEquals(2, actionCount)
    }

    @Test
    fun instantTransitionAtMotionOff() {
        var screen by mutableStateOf("Tab1")

        compose.setContent {
            MaterialTheme {
                AnimatedContent(
                    targetState = screen,
                    transitionSpec = { sharedAxisX(MankeuMotion.Off, forward = true) },
                    label = "testOff",
                ) { currentScreen ->
                    Text(text = "Content: $currentScreen", modifier = Modifier.testTag(currentScreen))
                }
            }
        }

        compose.onNodeWithTag("Tab1").assertIsDisplayed()

        screen = "Tab2"
        // At Off, target is rendered without waiting for time advancement
        compose.waitForIdle()
        compose.onNodeWithTag("Tab2").assertIsDisplayed()
        compose.onNodeWithTag("Tab1").assertDoesNotExist()
    }
}
