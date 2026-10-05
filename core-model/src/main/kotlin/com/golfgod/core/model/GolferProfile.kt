package com.golfgod.core.model

enum class GolferHandedness(val label: String, val shortCode: String) {
    RIGHT_HANDED("Right-Handed", "RH"),
    LEFT_HANDED("Left-Handed", "LH")
}

enum class WatchWrist(val label: String) {
    LEAD_WRIST("Lead Wrist (Recommended)"),
    TRAIL_WRIST("Trail Wrist")
}

data class GolferProfile(
    val handedness: GolferHandedness = GolferHandedness.RIGHT_HANDED,
    val watchWrist: WatchWrist = WatchWrist.LEAD_WRIST,
    val targetTempoRatio: Float = 3.0f,
    val driverLengthInches: Float = 45.5f
) {
    val isLeadWrist: Boolean
        get() = watchWrist == WatchWrist.LEAD_WRIST

    val gyroSignMultiplier: Float
        get() = if (handedness == GolferHandedness.RIGHT_HANDED) 1.0f else -1.0f
}
