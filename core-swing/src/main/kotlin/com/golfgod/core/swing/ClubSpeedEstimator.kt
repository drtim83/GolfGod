package com.golfgod.core.swing

import com.golfgod.core.model.Club
import kotlin.math.roundToInt

object ClubSpeedEstimator {

    private const val MPS_TO_MPH = 2.23694f
    private const val AVERAGE_ARM_RADIUS_METERS = 0.65f // Distance from shoulder rotation pivot to wrist

    /**
     * Estimates clubhead speed from peak wrist angular velocity and club shaft length.
     * V_club = V_wrist + (omega_wrist * L_shaft * release_efficiency)
     */
    fun estimateSpeedMph(
        peakGyroRadPerSec: Float,
        peakAccelG: Float,
        club: Club,
        leadWristMultiplier: Float = 1.0f
    ): Pair<Float, Float> {
        // Wrist speed (m/s) = omega * arm radius
        val wristSpeedMps = peakGyroRadPerSec * AVERAGE_ARM_RADIUS_METERS * 0.45f
        val wristSpeedMph = wristSpeedMps * MPS_TO_MPH

        // Total lever radius = arm radius + club shaft length
        val totalLeverMeters = AVERAGE_ARM_RADIUS_METERS + club.shaftLengthMeters

        // Wrist release snapping effect (efficient release multiplies angular velocity at clubhead)
        val releaseMultiplier = when {
            club.shaftLengthInches >= 43.0f -> 1.05f // Driver / 3-wood long lever
            club.shaftLengthInches >= 37.0f -> 0.95f // Mid irons
            else -> 0.88f // Wedges / short clubs
        }

        val estimatedClubSpeedMps = (peakGyroRadPerSec * totalLeverMeters * 0.62f * releaseMultiplier)
        val rawClubSpeedMph = estimatedClubSpeedMps * MPS_TO_MPH * leadWristMultiplier

        // Bound speed to realistic human capabilities per club
        val boundedClubSpeedMph = when (club.code) {
            "DR" -> rawClubSpeedMph.coerceIn(60f, 135f)
            "3W", "5W" -> rawClubSpeedMph.coerceIn(55f, 125f)
            "3H", "4I", "5I" -> rawClubSpeedMph.coerceIn(50f, 115f)
            "6I", "7I", "8I" -> rawClubSpeedMph.coerceIn(45f, 105f)
            "9I", "PW" -> rawClubSpeedMph.coerceIn(40f, 95f)
            "GW", "SW", "LW" -> rawClubSpeedMph.coerceIn(35f, 85f)
            else -> rawClubSpeedMph.coerceIn(10f, 40f)
        }

        val finalClubSpeed = ((boundedClubSpeedMph * 10f).roundToInt() / 10f)
        val finalHandSpeed = ((wristSpeedMph * 10f).roundToInt() / 10f)

        return Pair(finalHandSpeed, finalClubSpeed)
    }
}
