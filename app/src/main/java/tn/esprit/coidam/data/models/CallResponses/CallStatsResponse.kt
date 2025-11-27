package tn.esprit.coidam.data.models.CallResponses

data class CallStatsResponse(
    val totalCalls: Int,
    val completedCalls: Int,
    val missedCalls: Int,
    val rejectedCalls: Int,
    val totalDuration: Int,
    val averageDuration: Int
)
