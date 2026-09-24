package com.siaa.core.algorithm

import kotlin.math.max

object ProductionScoring {
    fun speakingSimilarity(target: String, transcript: String): Double {
        val a = tokens(target)
        val b = tokens(transcript)
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val edit = levenshtein(a, b)
        val sequence = 1.0 - edit.toDouble() / max(a.size, b.size).coerceAtLeast(1)
        val aset = a.toSet(); val bset = b.toSet()
        val overlap = (aset intersect bset).size.toDouble() / (aset union bset).size.coerceAtLeast(1)
        val length = minOf(a.size, b.size).toDouble() / max(a.size, b.size).coerceAtLeast(1)
        return (0.60 * sequence + 0.30 * overlap + 0.10 * length).coerceIn(0.0, 1.0)
    }

    fun writingScore(text: String, keywords: List<String>, minWords: Int): Double {
        val t = tokens(text)
        if (t.isEmpty()) return 0.0
        val key = keywords.flatMap(::tokens).distinct()
        val coverage = if (key.isEmpty()) 1.0 else key.count { it in t }.toDouble() / key.size
        val length = (t.size.toDouble() / minWords.coerceAtLeast(1)).coerceIn(0.0, 1.0)
        val diversity = t.distinct().size.toDouble() / t.size
        return (0.55 * coverage + 0.30 * length + 0.15 * diversity).coerceIn(0.0, 1.0)
    }

    private fun tokens(text: String): List<String> = text.lowercase()
        .replace(Regex("[^a-z0-9']+"), " ")
        .trim().split(Regex("\\s+")).filter { it.isNotBlank() }

    private fun levenshtein(a: List<String>, b: List<String>): Int {
        var previous = IntArray(b.size + 1) { it }
        for (i in a.indices) {
            val current = IntArray(b.size + 1)
            current[0] = i + 1
            for (j in b.indices) {
                current[j + 1] = minOf(
                    current[j] + 1,
                    previous[j + 1] + 1,
                    previous[j] + if (a[i] == b[j]) 0 else 1
                )
            }
            previous = current
        }
        return previous[b.size]
    }
}
