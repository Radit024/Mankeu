package com.mankeu.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mankeu.presentation.components.crossFade
import com.mankeu.presentation.components.itemMotion
import com.mankeu.presentation.components.sharedAxisX
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SectionMotionTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun tabSwitchPreservesDraftWithSaveableStateHolder() {
        var selectedTab by mutableStateOf("TabA")

        compose.setContent {
            MaterialTheme {
                val stateHolder = rememberSaveableStateHolder()
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = { sharedAxisX(MankeuMotion.Full, forward = true) },
                    label = "testTabSwitch",
                ) { tab ->
                    stateHolder.SaveableStateProvider(tab) {
                        if (tab == "TabA") {
                            var draft by rememberSaveable { mutableStateOf("") }
                            OutlinedTextField(
                                value = draft,
                                onValueChange = { draft = it },
                                modifier = Modifier.testTag("draftField"),
                            )
                        } else {
                            Text("Content B", modifier = Modifier.testTag("tabBContent"))
                        }
                    }
                }
            }
        }

        // Fill draft on TabA
        compose.onNodeWithTag("draftField").performTextInput("Draft text")
        compose.onNodeWithTag("draftField").assertTextContains("Draft text")

        // Switch to TabB
        selectedTab = "TabB"
        compose.waitForIdle()
        compose.onNodeWithTag("tabBContent").assertIsDisplayed()

        // Switch back to TabA
        selectedTab = "TabA"
        compose.waitForIdle()
        compose.onNodeWithTag("draftField").assertIsDisplayed()
        compose.onNodeWithTag("draftField").assertTextContains("Draft text")
    }

    @Test
    fun conditionalFieldMotionHonorsReducedAndOff() {
        var visible by mutableStateOf(false)
        var motionMode by mutableStateOf(MankeuMotion.Off)

        compose.setContent {
            MaterialTheme {
                val enter = remember(motionMode) {
                    when {
                        !motionMode.enabled -> EnterTransition.None
                        !motionMode.expressive -> fadeIn(tween(motionMode.fastMillis))
                        else -> expandVertically(tween(motionMode.mediumMillis, easing = FastOutSlowInEasing)) +
                            fadeIn(tween(motionMode.mediumMillis))
                    }
                }
                val exit = remember(motionMode) {
                    when {
                        !motionMode.enabled -> ExitTransition.None
                        !motionMode.expressive -> fadeOut(tween(motionMode.fastMillis))
                        else -> shrinkVertically(tween(motionMode.fastMillis, easing = FastOutSlowInEasing)) +
                            fadeOut(tween(motionMode.fastMillis))
                    }
                }

                AnimatedVisibility(visible = visible, enter = enter, exit = exit) {
                    Text("Conditional Detail", modifier = Modifier.testTag("conditionalText"))
                }
            }
        }

        // Initially hidden
        compose.onNodeWithTag("conditionalText").assertDoesNotExist()

        // Off: directly displayed when visible toggled
        visible = true
        compose.waitForIdle()
        compose.onNodeWithTag("conditionalText").assertIsDisplayed()

        visible = false
        compose.waitForIdle()
        compose.onNodeWithTag("conditionalText").assertDoesNotExist()
    }

    @Test
    fun listItemsAnimatePlacementAndRemoval() {
        val itemsList = mutableStateListOf("Item1", "Item2", "Item3")

        compose.setContent {
            MaterialTheme {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(itemsList, key = { it }) { item ->
                        Box(itemMotion(MankeuMotion.Full)) {
                            Text(text = item, modifier = Modifier.testTag(item))
                        }
                    }
                }
            }
        }

        compose.onNodeWithTag("Item1").assertIsDisplayed()
        compose.onNodeWithTag("Item2").assertIsDisplayed()
        compose.onNodeWithTag("Item3").assertIsDisplayed()

        // Remove middle item
        itemsList.remove("Item2")
        compose.waitForIdle()

        compose.onNodeWithTag("Item1").assertIsDisplayed()
        compose.onNodeWithTag("Item2").assertDoesNotExist()
        compose.onNodeWithTag("Item3").assertIsDisplayed()
    }
}
