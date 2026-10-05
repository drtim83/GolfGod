package com.golfgod.core.swing

import com.golfgod.core.model.TempoRating
import kotlin.math.roundToInt

object TempoEngine {

    /**
     * Calculates the tempo ratio (Backswing time / Downswing time).
     * Standard PGA Tour average is 3.0 : 1 (e.g. 750ms backswing, 250ms downswing).
     */
    fun calculateTempoRatio(backswingMs: Long, downswingMs: Long): Float {
        if (downswingMs <= 0L) return 0f
        val ratio = backswingMs.toFloat() / downswingMs.toFloat()
        return (ratio * 10f).roundToInt() / 10f
    }

    fun evaluateTempo(tempoRatio: Float, targetTempo: Float = 3.0f): TempoRating {
        return when {
            tempoRatio in (targetTempo - 0.25f)..(targetTempo + 0.25f) -> TempoRating.PERFECT
            tempoRatio in (targetTempo - 0.45f)..(targetTempo + 0.45f) -> TempoRating.GOOD
            tempoRatio < (targetTempo - 0.45f) -> TempoRating.FAST_DOWNSWING
            else -> TempoRating.SLOW_DOWNSWING
        }
    }

    fun calculateQualityScore(tempoRatio: Float, impactG: Float, clubSpeedMph: Float): Int {
        val tempoScore = when {
            tempoRatio in 2.8f..3.2f -> 45
            tempoRatio in 2.5f..3.5f -> 35
            tempoRatio in 2.2f..3.8f -> 25
            else -> 15
        }
        val impactScore = when {
            impactG >= 15f -> 35
            impactG >= 10f -> 28
            impactG >= 6f -> 20
            else -> 10
        }
        val speedBonus = (clubSpeedMph * 0.2f).coerceIn(0f, 20f).toInt()
        return (tempoScore + impactScore + speedBonus).coerceIn(1, 100)
    }
}
