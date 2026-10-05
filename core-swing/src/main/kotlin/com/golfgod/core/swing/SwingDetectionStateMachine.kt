package com.golfgod.core.swing

import com.golfgod.core.model.Club
import com.golfgod.core.model.GolferHandedness
import com.golfgod.core.model.GolferProfile
import com.golfgod.core.model.StandardBag
import com.golfgod.core.model.SwingRecord
import com.golfgod.core.model.WatchWrist
import java.util.UUID

enum class DetectorState {
    WAITING_FOR_ADDRESS,
    ADDRESS_LOCKED,
    TAKEAWAY,
    BACKSWING,
    TOP_OF_SWING,
    DOWNSWING,
    IMPACT_CONFIRMED,
    FOLLOW_THROUGH
}

class SwingDetectionStateMachine(
    private var currentClub: Club = StandardBag.CLUBS[0],
    var profile: GolferProfile = GolferProfile(),
    private val onSwingDetected: (SwingRecord) -> Unit
) {
    var state: DetectorState = DetectorState.WAITING_FOR_ADDRESS
        private set

    private var addressStartTimeNs = 0L
    private var takeawayTimeNs = 0L
    private var topTimeNs = 0L
    private var impactTimeNs = 0L

    private var peakGyroDuringDownswing = 0f
    private var peakAccelDuringImpact = 0f

    // Stillness monitoring at address
    private var stillnessSampleCount = 0
    private val STILLNESS_GYRO_THRESHOLD = 0.55f // rad/s
    private val REQUIRED_STILLNESS_SAMPLES = 15 // ~300ms at 50Hz

    // Motion thresholds
    private val TAKEAWAY_GYRO_THRESHOLD = 1.4f // rad/s
    private val DOWNSWING_GYRO_THRESHOLD = 4.5f // rad/s
    private val IMPACT_ACCEL_THRESHOLD_G = 5.5f // 5.5g spike

    fun setClub(club: Club) {
        this.currentClub = club
    }

    fun updateHandedness(handedness: GolferHandedness, wrist: WatchWrist) {
        this.profile = profile.copy(handedness = handedness, watchWrist = wrist)
        resetState()
    }

    fun processSample(sample: ImuSample) {
        // Gyro magnitude is invariant to rotation direction, but handedness orientation affects relative lever dynamics
        val gyroMag = sample.gyroMagnitude
        val accelG = sample.accelMagnitude / 9.80665f

        when (state) {
            DetectorState.WAITING_FOR_ADDRESS -> {
                if (gyroMag < STILLNESS_GYRO_THRESHOLD) {
                    stillnessSampleCount++
                    if (stillnessSampleCount >= REQUIRED_STILLNESS_SAMPLES) {
                        state = DetectorState.ADDRESS_LOCKED
                        addressStartTimeNs = sample.timestampNs
                    }
                } else {
                    stillnessSampleCount = 0
                }
            }

            DetectorState.ADDRESS_LOCKED -> {
                if (gyroMag > STILLNESS_GYRO_THRESHOLD * 2.0f && gyroMag < TAKEAWAY_GYRO_THRESHOLD) {
                    stillnessSampleCount = 0
                    state = DetectorState.WAITING_FOR_ADDRESS
                } else if (gyroMag >= TAKEAWAY_GYRO_THRESHOLD) {
                    takeawayTimeNs = sample.timestampNs
                    state = DetectorState.TAKEAWAY
                }
            }

            DetectorState.TAKEAWAY -> {
                val elapsedMs = (sample.timestampNs - takeawayTimeNs) / 1_000_000L
                if (elapsedMs > 150L && gyroMag > 2.0f) {
                    state = DetectorState.BACKSWING
                } else if (elapsedMs > 1200L) {
                    resetState()
                }
            }

            DetectorState.BACKSWING -> {
                val backswingDurationMs = (sample.timestampNs - takeawayTimeNs) / 1_000_000L

                if (gyroMag < 1.8f && backswingDurationMs in 400L..1400L) {
                    topTimeNs = sample.timestampNs
                    state = DetectorState.TOP_OF_SWING
                    peakGyroDuringDownswing = 0f
                } else if (backswingDurationMs > 1800L) {
                    resetState()
                }
            }

            DetectorState.TOP_OF_SWING -> {
                val transitionMs = (sample.timestampNs - topTimeNs) / 1_000_000L
                if (gyroMag >= DOWNSWING_GYRO_THRESHOLD) {
                    state = DetectorState.DOWNSWING
                    peakGyroDuringDownswing = gyroMag
                } else if (transitionMs > 450L) {
                    resetState()
                }
            }

            DetectorState.DOWNSWING -> {
                if (gyroMag > peakGyroDuringDownswing) {
                    peakGyroDuringDownswing = gyroMag
                }
                if (accelG > peakAccelDuringImpact) {
                    peakAccelDuringImpact = accelG
                }

                val downswingMs = (sample.timestampNs - topTimeNs) / 1_000_000L

                if (accelG >= IMPACT_ACCEL_THRESHOLD_G && downswingMs in 150L..500L) {
                    impactTimeNs = sample.timestampNs
                    state = DetectorState.IMPACT_CONFIRMED
                    completeSwing()
                } else if (downswingMs > 650L) {
                    if (peakGyroDuringDownswing > 8.0f) {
                        impactTimeNs = sample.timestampNs
                        completeSwing()
                    } else {
                        resetState()
                    }
                }
            }

            DetectorState.IMPACT_CONFIRMED, DetectorState.FOLLOW_THROUGH -> {
                val elapsedPostImpact = (sample.timestampNs - impactTimeNs) / 1_000_000L
                if (elapsedPostImpact > 800L) {
                    resetState()
                }
            }
        }
    }

    private fun completeSwing() {
        val backswingMs = ((topTimeNs - takeawayTimeNs) / 1_000_000L).coerceIn(350L, 1400L)
        val downswingMs = ((impactTimeNs - topTimeNs) / 1_000_000L).coerceIn(160L, 480L)
        val tempoRatio = TempoEngine.calculateTempoRatio(backswingMs, downswingMs)
        val tempoRating = TempoEngine.evaluateTempo(tempoRatio, currentClub.defaultTargetTempo)

        val wristMultiplier = if (profile.isLeadWrist) 1.0f else 0.94f

        val (handSpeedMph, clubSpeedMph) = ClubSpeedEstimator.estimateSpeedMph(
            peakGyroRadPerSec = peakGyroDuringDownswing.coerceAtLeast(6.0f),
            peakAccelG = peakAccelDuringImpact.coerceAtLeast(5.0f),
            club = currentClub,
            leadWristMultiplier = wristMultiplier
        )

        val score = TempoEngine.calculateQualityScore(tempoRatio, peakAccelDuringImpact, clubSpeedMph)

        val faults = mutableListOf<String>()
        if (tempoRatio < 2.5f) faults.add("Rushed downswing (quick hands from the top)")
        if (tempoRatio > 3.6f) faults.add("Slow downswing deceleration")
        if (backswingMs < 550L) faults.add("Rushed backswing")

        val record = SwingRecord(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            clubCode = currentClub.code,
            backswingTimeMs = backswingMs,
            downswingTimeMs = downswingMs,
            tempoRatio = tempoRatio,
            tempoRating = tempoRating,
            peakHandSpeedMph = handSpeedMph,
            estimatedClubSpeedMph = clubSpeedMph,
            peakImpactAccelG = peakAccelDuringImpact,
            qualityScore = score,
            detectedFaults = faults,
            notes = "${profile.handedness.shortCode} Golfer (${if (profile.isLeadWrist) "Lead" else "Trail"} Wrist)"
        )

        onSwingDetected(record)
        state = DetectorState.FOLLOW_THROUGH
    }

    fun resetState() {
        state = DetectorState.WAITING_FOR_ADDRESS
        stillnessSampleCount = 0
        peakGyroDuringDownswing = 0f
        peakAccelDuringImpact = 0f
    }
}
