package com.example.tokenshowcase

data class PredictionResponse(
    val predicted_emotion: String,
    val confidence_score: Double,
    val wellbeing_score: Int,
    val wellbeing_breakdown: WellbeingBreakdown,
    val recommendations: List<Recommendation>,
    val risk_factors: List<String>,
    val positive_factors: List<String>,
    val next_check_in: String
)

data class WellbeingBreakdown(
    val sleep: Double,
    val activity: Double,
    val phys_health: Double,
    val music_mood: Double,
    val digital_wellness: Double
)

data class Recommendation(
    val category: String,
    val text: String,
    val priority: Int,
    val impact_score: Double,
    val time_to_implement: String
)
