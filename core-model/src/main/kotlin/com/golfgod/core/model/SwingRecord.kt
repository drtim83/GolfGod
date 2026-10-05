package com.golfgod.core.model

enum class TempoRating(val label: String, val colorHex: String) {
    PERFECT("Tour Ideal (3:1)", "#2D9F4E"),
    GOOD("Good Rhythm", "#3498DB"),
    FAST_DOWNSWING("Fast Downswing / Rushed", "#E67E22"),
    SLOW_DOWNSWING("Slow Downswing", "#E74C3C"),
    RUSHED_TRANSITION("Rushed Transition", "#E74C3C")
}

data class SwingRecord(
    val id: String,
    val timestamp: Long,
    val clubCode: String,
    val backswingTimeMs: Long,
    val downswingTimeMs: Long,
    val tempoRatio: Float,
    val tempoRating: TempoRating,
    val peakHandSpeedMph: Float,
    val estimatedClubSpeedMph: Float,
    val peakImpactAccelG: Float,
    val qualityScore: Int, // 1 to 100
    val detectedFaults: List<String> = emptyList(),
    val notes: String = ""
)
