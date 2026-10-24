package com.mankeu.presentation

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mankeu.presentation.components.CardDetails
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CardDetailsTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun detailsAreHiddenUntilRequestedAndCanBeClosed() {
        compose.setContent {
            MankeuTheme(motion = MankeuMotion.Off) {
                CardDetails("unit-a:actual", "Lihat perhitungan", "Tutup") {
                    Text("Arus operasi Rp1.350.000")
                }
            }
        }
        compose.onNodeWithText("Arus operasi Rp1.350.000").assertDoesNotExist()
        compose.onNodeWithText("Lihat perhitungan").performClick()
        compose.onNodeWithText("Arus operasi Rp1.350.000").assertIsDisplayed()
        compose.onNodeWithText("Tutup").performClick()
        compose.onNodeWithText("Arus operasi Rp1.350.000").assertDoesNotExist()
    }

    @Test
    fun restorationPreservesDetailsWithinTheSameScope() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            MankeuTheme(motion = MankeuMotion.Off) {
                CardDetails("unit-a:actual", "Lihat", "Tutup") { Text("Rincian A") }
            }
        }
        compose.onNodeWithText("Lihat").performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Rincian A").assertIsDisplayed()
    }

    @Test
    fun changingScopeClosesDetails() {
        var scope by mutableStateOf("unit-a:actual")
        compose.setContent {
            MankeuTheme(motion = MankeuMotion.Off) {
                CardDetails(scope, "Lihat", "Tutup") { Text("Rincian") }
            }
        }
        compose.onNodeWithText("Lihat").performClick()
        compose.onNodeWithText("Rincian").assertIsDisplayed()
        compose.runOnIdle { scope = "unit-b:projection" }
        compose.onNodeWithText("Rincian").assertDoesNotExist()
    }
}
