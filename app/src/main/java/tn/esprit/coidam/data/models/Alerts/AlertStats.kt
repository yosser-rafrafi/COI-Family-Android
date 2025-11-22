package tn.esprit.coidam.data.models.Alerts

// Alert Stats
data class AlertStats(
    val total: Int,
    val pending: Int,
    val acknowledged: Int,
    val resolved: Int
)
