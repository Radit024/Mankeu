package com.arinahub.umkm.mankeu.importer

import java.text.Normalizer

fun interface SemanticMatcher {
    fun similarity(left: String, right: String): Double
}

/**
 * Deterministic offline semantic and fuzzy string matcher.
 *
 * Combines token normalization, Indonesian agricultural/financial stemming and aliases,
 * character-level distance metrics (Levenshtein & Jaro-Winkler), and soft-token containment
 * scoring. Zero external model dependencies, zero runtime bloat, and fully deterministic.
 */
object FuzzySemanticMatcher : SemanticMatcher {
    private val ignored = setOf(
        "pembelian", "pembayaran", "biaya", "harga", "rp", "rupiah", "menggunakan",
        "dan", "yang", "untuk", "usaha", "tani", "transaksi",
    )

    private val aliases = mapOf(
        "upah" to "tenaga",
        "pekerja" to "tenaga",
        "buruh" to "tenaga",
        "hok" to "tenaga",
        "borongan" to "tenaga",
        "alsintan" to "mesin",
        "traktor" to "mesin",
        "transplanter" to "mesin",
        "combine" to "mesin",
        "harvester" to "mesin",
        "pendapatan" to "pemasukan",
        "penerimaan" to "pemasukan",
        "penjualan" to "pemasukan",
        "hasil" to "pemasukan",
        "pengeluaran" to "beban",
        "ongkos" to "beban",
    )

    override fun similarity(left: String, right: String): Double {
        val a = tokens(left)
        val b = tokens(right)
        if (a.isEmpty() || b.isEmpty()) return 0.0
        if (a == b) return 1.0

        val exactIntersection = a.intersect(b)
        var totalIntersection = exactIntersection.size.toDouble()

        val remainingA = a - exactIntersection
        val remainingB = (b - exactIntersection).toMutableSet()

        for (tokenA in remainingA) {
            var bestScore = 0.0
            var bestTokenB: String? = null
            for (tokenB in remainingB) {
                val score = tokenSimilarity(tokenA, tokenB)
                if (score > bestScore) {
                    bestScore = score
                    bestTokenB = tokenB
                }
            }
            if (bestScore >= 0.80 && bestTokenB != null) {
                totalIntersection += bestScore
                remainingB.remove(bestTokenB)
            }
        }

        val totalSize = (a.size + b.size).toDouble()
        val union = (totalSize - totalIntersection).coerceAtLeast(1.0)
        val jaccard = if (union == 0.0) 0.0 else totalIntersection / union
        val minSize = minOf(a.size, b.size).toDouble()
        val containment = if (minSize == 0.0) 0.0 else totalIntersection / minSize

        return (jaccard * 0.55 + containment * 0.45).coerceIn(0.0, 1.0)
    }

    fun normalized(value: String): String = tokens(value).sorted().joinToString(" ")

    fun tokens(value: String): Set<String> {
        val ascii = Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[^a-z0-9]+"), " ")
        return ascii.split(' ')
            .asSequence()
            .filter { it.length > 1 && it !in ignored }
            .map { token ->
                aliases[token]
                    ?: aliases.entries.firstOrNull { (k, _) -> tokenSimilarity(k, token) >= 0.85 }?.value
                    ?: token
            }
            .toSet()
    }

    fun tokenSimilarity(s1: String, s2: String): Double {
        if (s1 == s2) return 1.0
        if (s1.isEmpty() || s2.isEmpty()) return 0.0
        val lev = normalizedLevenshtein(s1, s2)
        val jw = jaroWinklerSimilarity(s1, s2)
        return maxOf(lev, jw)
    }

    fun levenshteinDistance(s1: String, s2: String): Int {
        if (s1 == s2) return 0
        if (s1.isEmpty()) return s2.length
        if (s2.isEmpty()) return s1.length
        val len1 = s1.length
        val len2 = s2.length
        var prev = IntArray(len2 + 1) { it }
        var curr = IntArray(len2 + 1)
        for (i in 0 until len1) {
            curr[0] = i + 1
            for (j in 0 until len2) {
                val cost = if (s1[i] == s2[j]) 0 else 1
                curr[j + 1] = minOf(curr[j] + 1, prev[j + 1] + 1, prev[j] + cost)
            }
            val temp = prev
            prev = curr
            curr = temp
        }
        return prev[len2]
    }

    fun normalizedLevenshtein(s1: String, s2: String): Double {
        if (s1 == s2) return 1.0
        val maxLen = maxOf(s1.length, s2.length)
        if (maxLen == 0) return 1.0
        val distance = levenshteinDistance(s1, s2)
        return (1.0 - distance.toDouble() / maxLen).coerceIn(0.0, 1.0)
    }

    fun jaroSimilarity(s1: String, s2: String): Double {
        if (s1 == s2) return 1.0
        if (s1.isEmpty() || s2.isEmpty()) return 0.0
        val matchDistance = (maxOf(s1.length, s2.length) / 2) - 1
        val s1Matches = BooleanArray(s1.length)
        val s2Matches = BooleanArray(s2.length)
        var matches = 0
        for (i in s1.indices) {
            val start = maxOf(0, i - matchDistance)
            val end = minOf(i + matchDistance + 1, s2.length)
            for (j in start until end) {
                if (s2Matches[j] || s1[i] != s2[j]) continue
                s1Matches[i] = true
                s2Matches[j] = true
                matches++
                break
            }
        }
        if (matches == 0) return 0.0
        var k = 0
        var transpositions = 0
        for (i in s1.indices) {
            if (!s1Matches[i]) continue
            while (!s2Matches[k]) k++
            if (s1[i] != s2[k]) transpositions++
            k++
        }
        val m = matches.toDouble()
        return (m / s1.length + m / s2.length + (m - (transpositions / 2.0)) / m) / 3.0
    }

    fun jaroWinklerSimilarity(s1: String, s2: String, prefixScale: Double = 0.1): Double {
        val jaro = jaroSimilarity(s1, s2)
        if (jaro < 0.7) return jaro
        var prefix = 0
        val maxPrefix = minOf(4, minOf(s1.length, s2.length))
        for (i in 0 until maxPrefix) {
            if (s1[i] == s2[i]) prefix++ else break
        }
        return (jaro + prefix * prefixScale * (1.0 - jaro)).coerceIn(0.0, 1.0)
    }
}

