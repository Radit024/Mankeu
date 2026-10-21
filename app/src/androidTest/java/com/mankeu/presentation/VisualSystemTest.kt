package com.mankeu.presentation

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.ContextThemeWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import com.mankeu.R
import com.mankeu.presentation.components.EmptyCard
import com.mankeu.presentation.components.OperationKind
import com.mankeu.presentation.components.OperationSnackbar
import com.mankeu.presentation.components.OperationSnackbarVisuals
import com.mankeu.presentation.components.StateArtwork
import com.mankeu.presentation.components.StateIllustration
import com.mankeu.presentation.components.operationKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Native integration checks for DEC-019, with local assets and fictional UI text only. */
@RunWith(AndroidJUnit4::class)
class VisualSystemTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun changingThemeAppliesBrandFontsAndPreservesAnUnsubmittedForm() {
        var dark by mutableStateOf(false)
        var draft by mutableStateOf("")
        var observed: ColorScheme? = null
        compose.setContent {
            MankeuTheme(darkTheme = dark, motion = MankeuMotion.Off) {
                val scheme = MaterialTheme.colorScheme
                val typography = MaterialTheme.typography
                SideEffect {
                    observed = scheme
                    assertEquals(Sora, typography.headlineLarge.fontFamily)
                    assertEquals(Sora, typography.titleMedium.fontFamily)
                    assertEquals(PlusJakartaSans, typography.bodyMedium.fontFamily)
                    assertEquals(PlusJakartaSans, typography.labelLarge.fontFamily)
                }
                OutlinedTextField(draft, { draft = it }, modifier = Modifier.testTag("draft"))
            }
        }
        compose.onNodeWithTag("draft").performTextInput("Kedai contoh")
        compose.runOnIdle {
            assertEquals(Color(0xFF2D6A4F), observed!!.primary)
            assertEquals(Color(0xFFFAFAF8), observed!!.background)
            assertEquals(Color.White, observed!!.surfaceContainerLow)
            dark = true
        }
        compose.onNodeWithTag("draft").assertTextContains("Kedai contoh")
        compose.runOnIdle {
            assertEquals(Color(0xFF52B788), observed!!.primary)
            assertEquals(Color(0xFF1E2620), observed!!.background)
            assertEquals(Color(0xFF262D28), observed!!.surfaceContainerLow)
        }
    }

    @Test
    fun localCompositionsParseAndTheirHeldFramesAreVisible() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        StateArtwork.entries.forEach { artwork ->
            val result = LottieCompositionFactory.fromRawResSync(context, artwork.resource)
            assertNotNull("Local asset ${artwork.name}: ${result.exception}", result.value)
            val composition = result.value!!
            val drawable = LottieDrawable()
            drawable.setComposition(composition)
            drawable.setBounds(0, 0, 128, 128)
            drawable.progress = artwork.stillProgress
            val bitmap = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
            drawable.draw(Canvas(bitmap))
            val pixels = IntArray(128 * 128)
            bitmap.getPixels(pixels, 0, 128, 0, 0, 128, 128)
            assertTrue("${artwork.name} final frame must draw visible shapes", pixels.count { (it ushr 24) > 0 } > 100)
            assertTrue(composition.durationFrames > 0f)
            bitmap.recycle()
        }
    }

    @Test
    fun theSplashShowsTheArinaMarkInsideTheSafeZoneInBothThemes() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val themed = ContextThemeWrapper(context, R.style.Theme_Mankeu_Starting)
        // The attribute comes from the splash-screen library, so it is merged into the app package
        // rather than into the app's own R class; look it up by name to read what the theme set.
        val attrId = context.resources.getIdentifier("windowSplashScreenAnimatedIcon", "attr", context.packageName)
        assertTrue("The splash icon attribute must exist", attrId != 0)
        val attributes = themed.obtainStyledAttributes(intArrayOf(attrId))
        val iconId = attributes.getResourceId(0, 0)
        attributes.recycle()
        assertEquals("The splash icon must be the Arina mark", R.drawable.ic_arina_logo_inset, iconId)

        val bgAttrId = context.resources.getIdentifier("windowSplashScreenBackground", "attr", context.packageName)
        assertTrue("The splash background attribute must exist", bgAttrId != 0)
        val bgAttributes = themed.obtainStyledAttributes(intArrayOf(bgAttrId))
        val bgId = bgAttributes.getResourceId(0, 0)
        bgAttributes.recycle()
        assertEquals("The splash background must be splash_screen_background", R.color.splash_screen_background, bgId)

        val light = renderSplashMark(context, night = false)
        val night = renderSplashMark(context, night = true)
        assertTrue("The splash mark must draw ink", light.opaque > 500)
        assertTrue("The splash mark must stay inside the mask safe zone", light.outerRingInk == 0)
        assertTrue(
            "The splash mark must use the Arina brand green",
            light.opaquePixels.any { it == light.expected },
        )
        assertTrue(
            "The splash mark remains the Arina brand green on the white background",
            night.opaquePixels.any { it == night.expected },
        )
    }

    /** Draws the splash icon the way the system does and measures the ink it leaves. */
    private fun renderSplashMark(context: Context, night: Boolean): SplashMark {
        val base = if (night) {
            val configuration = Configuration(context.resources.configuration).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or Configuration.UI_MODE_NIGHT_YES
            }
            context.createConfigurationContext(configuration)
        } else {
            context
        }
        val themed = ContextThemeWrapper(base, R.style.Theme_Mankeu_Starting)
        val drawable = ContextCompat.getDrawable(themed, R.drawable.ic_arina_logo_inset)!!
        val size = 240
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(Canvas(bitmap))
        val pixels = IntArray(size * size)
        bitmap.getPixels(pixels, 0, size, 0, 0, size, size)
        val margin = size / 10
        var outerRingInk = 0
        val opaquePixels = mutableListOf<Int>()
        for (y in 0 until size) {
            for (x in 0 until size) {
                val pixel = pixels[y * size + x]
                if ((pixel ushr 24) == 255) opaquePixels.add(pixel)
                if (x < margin || y < margin || x >= size - margin || y >= size - margin) {
                    if ((pixel ushr 24) > 0) outerRingInk++
                }
            }
        }
        bitmap.recycle()
        return SplashMark(
            opaque = opaquePixels.size,
            opaquePixels = opaquePixels,
            outerRingInk = outerRingInk,
            expected = ContextCompat.getColor(themed, R.color.arina_brand_green),
        )
    }

    private data class SplashMark(
        val opaque: Int,
        val opaquePixels: List<Int>,
        val outerRingInk: Int,
        val expected: Int,
    )

    @Test fun reducedMotionKeepsIllustrationStillAndEmptyStateActionUsable() = staticIllustration(MankeuMotion.Reduced)
    @Test fun offMotionKeepsIllustrationStillAndEmptyStateActionUsable() = staticIllustration(MankeuMotion.Off)

    private fun staticIllustration(motion: MankeuMotion) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // Prewarm the same raw-resource cache the composable uses; no download or fixture server.
        LottieCompositionFactory.fromRawResSync(context, R.raw.mankeu_empty)
        var clicks = 0
        compose.setContent {
            MankeuTheme(darkTheme = false, motion = motion) {
                Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    Box(Modifier.size(96.dp).testTag("art")) {
                        StateIllustration(StateArtwork.EMPTY, Modifier.fillMaxSize())
                    }
                    EmptyCard("Belum ada catatan", "Mulai dengan catatan pertama.", "Catat contoh", { clicks++ }, artwork = StateArtwork.EMPTY)
                }
            }
        }
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(3000)
        val first = compose.onNodeWithTag("art").captureToImage().asAndroidBitmap()
        compose.mainClock.advanceTimeBy(3000)
        val second = compose.onNodeWithTag("art").captureToImage().asAndroidBitmap()
        assertTrue("Reduced/Off must not create ongoing decorative animation", first.sameAs(second))
        compose.onNodeWithText("Belum ada catatan").assertIsDisplayed()
        compose.onNodeWithText("Catat contoh").performClick()
        compose.runOnIdle { assertEquals(1, clicks) }
    }

    @Test
    fun snackbarRetainsItsMessageActionAndDismissCallbacks() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var actions = 0
        var dismissals = 0
        val data = object : SnackbarData {
            override val visuals: SnackbarVisuals = OperationSnackbarVisuals(
                message = "Catatan contoh tersimpan", kind = OperationKind.SUCCESS,
                actionLabel = "Lihat contoh", withDismissAction = true,
            )
            override fun performAction() { actions++ }
            override fun dismiss() { dismissals++ }
        }
        compose.setContent {
            MankeuTheme(darkTheme = true, motion = MankeuMotion.Off) { OperationSnackbar(data) }
        }
        compose.onNodeWithText("Catatan contoh tersimpan").assertIsDisplayed()
        compose.onNodeWithText("Lihat contoh").performClick()
        compose.onNodeWithContentDescription(context.getString(R.string.action_close)).performClick()
        compose.runOnIdle {
            assertEquals(1, actions)
            assertEquals(1, dismissals)
        }
    }

    @Test
    fun instructionsAndFailuresNeverReceiveASuccessTick() {
        assertEquals(OperationKind.SUCCESS, UiText.of(R.string.msg_event_saved).operationKind())
        assertEquals(OperationKind.INFO, UiText.of(R.string.entry_err_no_project).operationKind())
        assertEquals(OperationKind.INFO, UiText.Raw("A neutral instruction").operationKind())
        assertEquals(OperationKind.ERROR, UiText.Raw("A refusal", isError = true).operationKind())
    }

    @Test
    fun systemAndOwnerMotionSettingsDisableDecorativePlayback() {
        assertTrue(MankeuMotion.resolve(MotionMode.FULL, 1f).expressive)
        assertFalse(MankeuMotion.resolve(MotionMode.REDUCED, 1f).expressive)
        assertFalse(MankeuMotion.resolve(MotionMode.OFF, 1f).expressive)
        assertEquals(MankeuMotion.Off, MankeuMotion.resolve(MotionMode.FULL, 0f))
        assertEquals(MankeuMotion.Reduced, MankeuMotion.resolve(MotionMode.FULL, 0.5f))
    }
}
