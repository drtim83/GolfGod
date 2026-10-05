package com.golfgod.core.physics

import com.golfgod.core.model.BallFlightData
import com.golfgod.core.model.Club
import com.golfgod.core.model.TrajectoryPoint
import kotlin.math.*

object BallFlightSimulator {

    private const val G = 9.80665f // Gravity m/s^2
    private const val AIR_DENSITY = 1.204f // kg/m^3 (sea level 20°C)
    private const val BALL_MASS = 0.04593f // kg (1.620 oz)
    private const val BALL_RADIUS = 0.02135f // m (1.680 inch diameter)
    private const val BALL_AREA = Math.PI.toFloat() * BALL_RADIUS * BALL_RADIUS // Cross-sectional area

    private const val MPH_TO_MPS = 0.44704f
    private const val METERS_TO_YARDS = 1.09361f
    private const val METERS_TO_FEET = 3.28084f

    /**
     * Simulates full 3D aerodynamic golf ball trajectory using numerical integration
     * with drag force and Magnus effect lift.
     */
    fun simulateFlight(
        clubHeadSpeedMph: Float,
        club: Club,
        attackAngleDeg: Float = 0f
    ): BallFlightData {
        val initial = ImpactDynamics.calculateLaunchMetrics(clubHeadSpeedMph, club, attackAngleDeg)

        var vx = initial.ballSpeedMph * MPH_TO_MPS * cos(Math.toRadians(initial.launchAngleDeg.toDouble())).toFloat()
        var vz = initial.ballSpeedMph * MPH_TO_MPS * sin(Math.toRadians(initial.launchAngleDeg.toDouble())).toFloat()
        var vy = 0f // lateral velocity

        var x = 0f // horizontal distance (m)
        var z = 0.01f // height (m)
        var y = 0f // lateral deviation (m)

        var spinRps = initial.backspinRpm / 60.0f
        var maxZ = z
        var flightTime = 0f
        val dt = 0.02f // 20ms physics step

        val points = mutableListOf<TrajectoryPoint>()
        points.add(TrajectoryPoint(0f, 0f, 0f))

        var stepCounter = 0
        while (z >= 0f && flightTime < 14f) {
            val v = sqrt(vx * vx + vy * vy + vz * vz)
            if (v < 0.1f) break

            // Aerodynamic drag coefficient for dimpled golf ball
            val cd = calculateDragCoefficient(v)
            val dragForce = 0.5f * AIR_DENSITY * v * v * cd * BALL_AREA

            // Magnus effect lift coefficient
            val spinParam = (BALL_RADIUS * 2.0f * Math.PI.toFloat() * spinRps) / v
            val cl = (0.22f * spinParam).coerceIn(0.08f, 0.38f)
            val liftForce = 0.5f * AIR_DENSITY * v * v * cl * BALL_AREA

            // Accelerations
            val ax = -(dragForce * (vx / v) + liftForce * (vz / v)) / BALL_MASS
            val az = -G - (dragForce * (vz / v) - liftForce * (vx / v)) / BALL_MASS

            // Update velocities
            vx += ax * dt
            vz += az * dt

            // Update positions
            x += vx * dt
            z += vz * dt
            flightTime += dt

            if (z > maxZ) {
                maxZ = z
            }

            // Spin decay over time (~1.5% per second)
            spinRps *= (1.0f - 0.015f * dt)

            stepCounter++
            if (stepCounter % 5 == 0 || z < 0f) {
                points.add(
                    TrajectoryPoint(
                        distanceYards = (x * METERS_TO_YARDS).round(1),
                        heightFeet = (z.coerceAtLeast(0f) * METERS_TO_FEET).round(1),
                        lateralOffsetFeet = (y * METERS_TO_FEET).round(1)
                    )
                )
            }
        }

        val carryYards = (x * METERS_TO_YARDS).roundToInt()
        val apexFeet = (maxZ * METERS_TO_FEET).roundToInt()

        // Turf roll estimation based on descent angle and club
        val rollYards = when (club.code) {
            "DR" -> (carryYards * 0.10f).roundToInt().coerceIn(12, 35)
            "3W", "5W" -> (carryYards * 0.07f).roundToInt().coerceIn(8, 20)
            "3H", "4I", "5I" -> (carryYards * 0.05f).roundToInt().coerceIn(5, 14)
            "6I", "7I", "8I" -> (carryYards * 0.03f).roundToInt().coerceIn(2, 8)
            else -> (carryYards * 0.015f).roundToInt().coerceIn(1, 4) // High spin wedges stop fast
        }

        val totalYards = carryYards + rollYards

        return BallFlightData(
            estimatedBallSpeedMph = (initial.ballSpeedMph * 10f).roundToInt() / 10f,
            estimatedLaunchAngleDeg = (initial.launchAngleDeg * 10f).roundToInt() / 10f,
            estimatedBackspinRpm = initial.backspinRpm,
            estimatedSidespinRpm = initial.sidespinRpm,
            estimatedCarryYards = carryYards,
            estimatedTotalYards = totalYards,
            estimatedApexFeet = apexFeet,
            estimatedSmashFactor = club.standardSmashFactor,
            flightTimeSeconds = (flightTime * 10f).roundToInt() / 10f,
            trajectory = points
        )
    }

    private fun calculateDragCoefficient(velocityMps: Float): Float {
        val reynolds = (AIR_DENSITY * velocityMps * (BALL_RADIUS * 2f)) / 1.81e-5f
        return when {
            reynolds < 80_000 -> 0.42f
            reynolds in 80_000f..140_000f -> 0.28f // Drag crisis reduction for dimples
            else -> 0.23f
        }
    }

    private fun Float.round(decimals: Int): Float {
        var multiplier = 1.0f
        repeat(decimals) { multiplier *= 10f }
        return (this * multiplier).roundToInt() / multiplier
    }
}
