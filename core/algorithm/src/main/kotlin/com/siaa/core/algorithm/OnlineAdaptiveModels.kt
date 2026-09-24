package com.siaa.core.algorithm

import com.siaa.core.model.ExerciseType
import kotlin.math.roundToLong
import kotlin.random.Random

/**
 * Estado online ligero y reproducible. La aleatoriedad se deriva de una semilla base + clave,
 * evitando que el orden accidental de llamadas cambie la secuencia tras process death.
 */
class OnlineAdaptiveModels(
    private val baseSeed: Long = DEFAULT_SEED
) {
    private val memoryModels = mutableMapOf<String, MemoryEnsemble>()
    private val particleFilters = mutableMapOf<String, ParticleMasteryFilter>()
    private val banditArms = mutableMapOf<String, BanditArm>()

    fun recallProbability(kcId: String, elapsedHours: Double, halfLifeHours: Double): Double =
        memoryModels.getOrPut(kcId) { MemoryEnsemble() }
            .predict(elapsedHours, halfLifeHours)
            .probability

    fun observeMemory(kcId: String, elapsedHours: Double, halfLifeHours: Double, remembered: Boolean) {
        val model = memoryModels.getOrPut(kcId) { MemoryEnsemble() }
        val prediction = model.predict(elapsedHours, halfLifeHours)
        model.observe(prediction, remembered)
    }

    fun latentPosterior(
        kcId: String,
        priorMastery: Double,
        priorHalfLifeHours: Double
    ): ParticlePosterior = particleFilters.getOrPut(kcId) {
        ParticleMasteryFilter(
            initialMastery = priorMastery,
            initialHalfLifeHours = priorHalfLifeHours,
            random = Random(seedFor("particle::$kcId"))
        )
    }.posterior()

    fun observeLatentState(
        kcId: String,
        priorMastery: Double,
        priorHalfLifeHours: Double,
        correct: Boolean,
        elapsedHours: Double
    ): ParticlePosterior {
        val filter = particleFilters.getOrPut(kcId) {
            ParticleMasteryFilter(
                initialMastery = priorMastery,
                initialHalfLifeHours = priorHalfLifeHours,
                random = Random(seedFor("particle::$kcId"))
            )
        }
        return filter.observe(correct, elapsedHours)
    }

    /** Thompson sampling restringido y reproducible para un estado de brazo dado. */
    fun strategySample(kcId: String, type: ExerciseType): Double {
        val key = armKey(kcId, type)
        val arm = banditArms.getOrPut(key) { BanditArm(key) }
        val seed = seedFor("sample::$key::${arm.alpha.quantized()}::${arm.beta.quantized()}")
        return MathUtils.betaSample(arm.alpha, arm.beta, Random(seed))
    }

    fun observeStrategy(kcId: String, type: ExerciseType, reward: Double) {
        val key = armKey(kcId, type)
        val prior = banditArms.getOrPut(key) { BanditArm(key) }
        val seed = seedFor("update::$key::${prior.alpha.quantized()}::${prior.beta.quantized()}::${reward.quantized()}")
        banditArms[key] = ThompsonBandit(Random(seed)).update(prior, reward)
    }

    fun strategyMean(kcId: String, type: ExerciseType): Double {
        val arm = banditArms[armKey(kcId, type)] ?: return 0.5
        return arm.alpha / (arm.alpha + arm.beta)
    }

    private fun armKey(kcId: String, type: ExerciseType) = "$kcId::${type.name}"

    private fun seedFor(key: String): Long {
        var h = baseSeed xor 0x9E3779B97F4A7C15UL.toLong()
        for (c in key) h = (h xor c.code.toLong()) * 0x100000001B3L
        return h
    }

    private fun Double.quantized(): Long = (this * 1_000_000.0).roundToLong()

    companion object {
        const val DEFAULT_SEED: Long = 0x51AA2026L
    }
}
