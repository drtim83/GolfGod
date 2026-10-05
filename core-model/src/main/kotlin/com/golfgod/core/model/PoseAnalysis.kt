package com.golfgod.core.model

enum class SwingPhase {
    ADDRESS,
    TAKEAWAY,
    TOP_OF_BACKSWING,
    TRANSITION,
    IMPACT,
    FOLLOW_THROUGH,
    FINISH
}

enum class FaultSeverity {
    EXCELLENT,
    MINOR,
    MAJOR
}

data class SwingFault(
    val id: String,
    val name: String,
    val severity: FaultSeverity,
    val detectedPhase: SwingPhase,
    val diagnosticSummary: String,
    val biomechanicalCause: String,
    val drillRecommendation: String,
    val drillName: String
)

data class BiomechanicalMetrics(
    val spineAngleAddressDeg: Float,
    val spineAngleImpactDeg: Float,
    val spineRetentionDeltaDeg: Float,
    val shoulderTurnAtTopDeg: Float,
    val hipTurnAtTopDeg: Float,
    val xFactorStretchDeg: Float,
    val earlyExtensionInches: Float,
    val headSwayInches: Float,
    val headLiftInches: Float,
    val leadArmAngleAtTopDeg: Float
)

data class PoseAnalysisReport(
    val swingScore: Int, // 0 - 100
    val phase: SwingPhase,
    val metrics: BiomechanicalMetrics,
    val faults: List<SwingFault>,
    val coachVerdict: String
)
