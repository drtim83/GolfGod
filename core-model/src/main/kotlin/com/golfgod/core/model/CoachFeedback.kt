package com.golfgod.core.model

data class CoachFeedback(
    val headline: String,
    val scoreGrade: String, // "A", "B+", "C", etc.
    val mainIssue: String,
    val whyItHurtsYourShot: String,
    val keyAdjustment: String,
    val stepByStepDrill: String,
    val proComparison: String
)
