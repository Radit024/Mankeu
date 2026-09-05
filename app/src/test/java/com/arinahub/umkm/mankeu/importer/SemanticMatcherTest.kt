package com.arinahub.umkm.mankeu.importer

import com.arinahub.umkm.mankeu.domain.CategoryRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SemanticMatcherTest {
    @Test
    fun `noise words do not hide a clear Indonesian label match`() {
        val score = FuzzySemanticMatcher.similarity("Jasa Pengolahan Tanah TR 2", "Pembayaran jasa pengolahan tanah (TR 2)")
        assertTrue("score=$score", score >= 0.8)
    }

    @Test
    fun `unrelated labels stay below automatic threshold`() {
        val score = FuzzySemanticMatcher.similarity("Sewa lahan", "Pembelian benih padi")
        assertTrue("score=$score", score < 0.43)
    }

    @Test
    fun `typos and slight spelling mistakes in Indonesian agricultural terms match with high confidence`() {
        // "persmaian" instead of "persemaian" / "bibit"
        val score1 = FuzzySemanticMatcher.similarity("Upah persmaian sistem dapog", "Upah persemaian sistem dapog")
        assertTrue("score1=$score1", score1 >= 0.85)

        // "herbisda" instead of "herbisida" with extra word
        val score2 = FuzzySemanticMatcher.similarity("Pembelian obat herbisda", "Pembelian herbisida")
        assertTrue("score2=$score2", score2 >= 0.60)

        // "trnsplanter" instead of "transplanter"
        val score3 = FuzzySemanticMatcher.similarity("Jasa trnsplanter", "Jasa transplanter")
        assertTrue("score3=$score3", score3 >= 0.85)
    }

    @Test
    fun `normalized Levenshtein and Jaro-Winkler functions compute accurate similarities`() {
        val simExact = FuzzySemanticMatcher.tokenSimilarity("pupuk", "pupuk")
        assertEquals(1.0, simExact, 0.001)

        val simTypo = FuzzySemanticMatcher.tokenSimilarity("pupuk", "pupk")
        assertTrue("simTypo=$simTypo", simTypo >= 0.80)

        val simDistinct = FuzzySemanticMatcher.tokenSimilarity("pupuk", "traktor")
        assertTrue("simDistinct=$simDistinct", simDistinct < 0.50)
    }

    @Test
    fun `CategoryClassifier correctly categorizes rows even with typos and casing variations`() {
        // Labour with typo
        val catLabour = CategoryClassifier.classify("Upah borongn tanam padi", CategoryRole.EXPENSE)
        assertEquals("Tenaga Kerja", catLabour)

        // Machine service with typo
        val catMachine = CategoryClassifier.classify("Byr jasa trnsplanter", CategoryRole.EXPENSE)
        assertEquals("Jasa Alsintan", catMachine)

        // Fertilizer with typo
        val catFertilizer = CategoryClassifier.classify("Beli pupk urea 50kg", CategoryRole.EXPENSE)
        assertEquals("Pupuk", catFertilizer)

        // Pesticide with typo
        val catPesticide = CategoryClassifier.classify("Semprot pestisda ulat", CategoryRole.EXPENSE)
        assertEquals("Obat dan Pestisida", catPesticide)

        // Revenue with aliases
        val catRevenue = CategoryClassifier.classify("Penjualan gabah panen 2 ton", CategoryRole.REVENUE)
        assertEquals("Hasil Panen", catRevenue)
    }
}

