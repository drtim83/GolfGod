package com.golfgod.core.swing

import kotlin.math.sqrt

data class ImuSample(
    val timestampNs: Long,
    val ax: Float, // m/s^2 (linear acceleration or total accel)
    val ay: Float,
    val az: Float,
    val gx: Float, // rad/s (gyroscope)
    val gy: Float,
    val gz: Float
) {
    val accelMagnitude: Float
        get() = sqrt(ax * ax + ay * ay + az * az)

    val gyroMagnitude: Float
        get() = sqrt(gx * gx + gy * gy + gz * gz)
}
