package com.mankeu.presentation.guide

import com.mankeu.domain.BookMode
import com.mankeu.presentation.Destination
import com.mankeu.presentation.Overlay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** The guide's invariants: tours that can be versioned and replayed, and help that can always show its place. */
class GuidesTest {
    @Test
    fun `every tour has a unique id, a version and distinct targets`() {
        assertEquals(Tour.entries.size, Tour.entries.map { it.id }.toSet().size)
        Tour.entries.forEach { tour ->
            assertTrue("${tour.name} has no steps", tour.steps.isNotEmpty())
            assertTrue("${tour.name} version", tour.version >= 1)
            assertEquals("${tour.name} repeats a target", tour.steps.size, tour.steps.map { it.target }.toSet().size)
        }
    }

    @Test
    fun `each page maps to its tour and the plan page is not the ledger`() {
        assertEquals(Tour.PLAN, tourFor(Destination.ENTRY, BookMode.PROJECTION, Overlay.NONE))
        assertEquals(Tour.ENTRY, tourFor(Destination.ENTRY, BookMode.REALIZATION, Overlay.NONE))
        assertEquals(Tour.DATA, tourFor(Destination.HOME, BookMode.REALIZATION, Overlay.DATA))
        assertEquals(null, tourFor(Destination.HOME, BookMode.REALIZATION, Overlay.HELP))
    }

    /**
     * "Tunjukkan di aplikasi" needs a screenshot of every place it shows. A new article or target
     * without one means `scripts/capture_help_screenshots.ps1` was not run.
     */
    @Test
    fun `every help article has its screenshot in the app`() {
        val images = File("src/main/res/drawable-nodpi").listFiles().orEmpty().map { it.nameWithoutExtension }.toSet()
        val missing = HelpArticle.entries.map { it.show.imageName }.filterNot { it in images }.distinct()
        assertTrue("Missing screenshots: $missing", missing.isEmpty())
    }

    @Test
    fun `plan articles open the plan page and overlay articles keep the page`() {
        HelpArticle.entries.forEach { article ->
            val spec = article.show
            if (spec.bookMode == BookMode.PROJECTION) assertEquals(article.name, Destination.ENTRY, spec.destination)
            if (spec.overlay != Overlay.NONE) assertEquals(article.name, null, spec.bookMode)
        }
        assertTrue(HelpGroup.entries.all { group -> HelpArticle.entries.any { it.group == group } })
    }
}
