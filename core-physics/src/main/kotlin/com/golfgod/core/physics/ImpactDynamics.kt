package com.golfgod.core.physics

import com.golfgod.core.model.Club
import kotlin.math.roundToInt

object ImpactDynamics {

    /**
     * Calculates estimated initial ball launch parameters from club head speed and club specs.
     */
    fun calculateLaunchMetrics(
        clubHeadSpeedMph: Float,
        club: Club,
        attackAngleDeg: Float = getDefaultAttackAngle(club)
    ): InitialBallState {
        // Ball speed = Clubhead speed * Smash Factor
        val ballSpeedMph = clubHeadSpeedMph * club.standardSmashFactor

        // Dynamic loft = Static Loft + shaft lean effect + attack angle influence
        val dynamicLoft = when (club.code) {
            "DR" -> club.loftDegrees + 2.5f + (attackAngleDeg * 0.4f)
            "3W", "5W" -> club.loftDegrees - 1.5f + (attackAngleDeg * 0.3f)
            else -> club.loftDegrees - 3.5f + (attackAngleDeg * 0.25f) // Forward shaft lean compression on irons
        }

        // Launch angle is typically 65-75% of dynamic loft
        val launchAngleDeg = dynamicLoft * 0.72f + (attackAngleDeg * 0.2f)

        // Spin rate (RPM) is proportional to oblique impact friction: V_club * sin(spin_loft)
        val spinLoft = (dynamicLoft - attackAngleDeg).coerceAtLeast(5f)
        val spinRpm = (clubHeadSpeedMph * spinLoft * getSpinCoefficient(club)).roundToInt()

        return InitialBallState(
            ballSpeedMph = ballSpeedMph,
            launchAngleDeg = launchAngleDeg.coerceIn(5f, 50f),
            backspinRpm = spinRpm.coerceIn(1200, 11500),
            sidespinRpm = 0
        )
    }

    private fun getDefaultAttackAngle(club: Club): Float {
        return when (club.code) {
            "DR" -> 2.5f // Upward angle of attack for Driver
            "3W", "5W" -> -1.0f // Level / slightly descending
            else -> -3.5f // Descending strike into turf for irons
        }
    }

    private fun getSpinCoefficient(club: Club): Float {
        return when (club.code) {
            "DR" -> 2.1f // Low spin driver (~2400 rpm)
            "3W", "5W" -> 2.8f
            "3H", "4I", "5I" -> 3.2f
            "6I", "7I", "8I" -> 3.8f // 7i ~6500-7000 rpm
            "9I", "PW" -> 4.4f
            "GW", "SW", "LW" -> 5.0f // Wedges ~9000+ rpm
            else -> 1.0f
        }
    }
}

data class InitialBallState(
    val ballSpeedMph: Float,
    val launchAngleDeg: Float,
    val backspinRpm: Int,
    val sidespinRpm: Int
)
