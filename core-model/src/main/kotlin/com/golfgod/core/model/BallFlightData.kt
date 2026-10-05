package com.golfgod.core.model

data class TrajectoryPoint(
    val distanceYards: Float,
    val heightFeet: Float,
    val lateralOffsetFeet: Float
)

data class BallFlightData(
    val estimatedBallSpeedMph: Float,
    val estimatedLaunchAngleDeg: Float,
    val estimatedBackspinRpm: Int,
    val estimatedSidespinRpm: Int,
    val estimatedCarryYards: Int,
    val estimatedTotalYards: Int,
    val estimatedApexFeet: Int,
    val estimatedSmashFactor: Float,
    val flightTimeSeconds: Float,
    val trajectory: List<TrajectoryPoint> = emptyList()
)
