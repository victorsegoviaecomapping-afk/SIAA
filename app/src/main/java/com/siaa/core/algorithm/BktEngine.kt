package com.siaa.core.algorithm

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

/**
 * Bayesian Knowledge Tracing (BKT) and Spaced Memory Stability engine.
 * Computes posterior probability of latent skill mastery P(L_t) given learner response,
 * and tracks decay/retention half-life.
 */
class BktEngine(
    val defaultPInit: Double = 0.25,
    val defaultPTransit: Double = 0.15,
    val defaultPGuess: Double = 0.25,
    val defaultPSlip: Double = 0.10
) {

    /**
     * Updates knowledge mastery given whether the response was correct.
     * @param currentMastery Prior mastery probability P(L_t-1)
     * @param correct Whether learner answered correctly
     * @param pTransit Learning transition rate
     * @param pGuess Guessing probability
     * @param pSlip Slip probability
     * @return Posterior mastery probability P(L_t)
     */
    fun updateMastery(
        currentMastery: Double,
        correct: Boolean,
        pTransit: Double = defaultPTransit,
        pGuess: Double = defaultPGuess,
        pSlip: Double = defaultPSlip
    ): Double {
        val pL = currentMastery.coerceIn(0.01, 0.99)

        val pPosterior = if (correct) {
            // P(L | Correct) = (P(L) * (1 - P(S))) / (P(L) * (1 - P(S)) + (1 - P(L)) * P(G))
            val num = pL * (1.0 - pSlip)
            val den = num + (1.0 - pL) * pGuess
            num / den
        } else {
            // P(L | Incorrect) = (P(L) * P(S)) / (P(L) * P(S)) + (1 - P(L)) * (1 - P(G))
            val num = pL * pSlip
            val den = num + (1.0 - pL) * (1.0 - pGuess)
            num / den
        }

        // P(L_t) = P(L_t-1 | Obs) + (1 - P(L_t-1 | Obs)) * P(T)
        val nextMastery = pPosterior + (1.0 - pPosterior) * pTransit
        return nextMastery.coerceIn(0.05, 0.99)
    }

    /**
     * Calculates exponential memory retention based on elapsed hours and stability.
     * R(t) = exp(-ln(2) * (elapsedHours / halfLifeHours))
     */
    fun computeRetention(elapsedHours: Double, halfLifeHours: Double = 48.0): Double {
        val safeHalfLife = max(1.0, halfLifeHours)
        val factor = -ln(2.0) * (elapsedHours / safeHalfLife)
        return exp(factor).coerceIn(0.0, 1.0)
    }

    /**
     * Determines whether a Knowledge Component is considered fully mastered (threshold >= 0.85).
     */
    fun isMastered(masteryProbability: Double): Boolean {
        return masteryProbability >= 0.85
    }
}
