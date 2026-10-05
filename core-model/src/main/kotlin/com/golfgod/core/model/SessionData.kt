package com.golfgod.core.model

data class SessionData(
    val id: String,
    val startTime: Long,
    val endTime: Long,
    val totalSwings: Int,
    val clubBreakdown: Map<String, Int>,
    val averageTempoRatio: Float,
    val averageClubSpeedMph: Float,
    val perfectTempoCount: Int,
    val swings: List<SwingRecord> = emptyList()
)
