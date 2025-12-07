package tn.esprit.coidam.data.models

import com.google.gson.annotations.SerializedName

data class Battery(
    @SerializedName("_id")
    val id: String,
    val blindUserId: String,
    val companionId: String? = null,
    val level: Int,
    val isCharging: Boolean = false,
    val note: String? = null,
    val createdAt: String
) {
    // Helper pour le temps écoulé
    fun timeAgo(): String {
        try {
            val formatter = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
            formatter.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val date = formatter.parse(createdAt) ?: return "Récemment"

            val now = java.util.Date()
            val diff = now.time - date.time
            val seconds = diff / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24

            return when {
                minutes < 1 -> "À l'instant"
                minutes < 60 -> "Il y a $minutes min"
                hours < 24 -> "Il y a ${hours}h"
                else -> "Il y a ${days}j"
            }
        } catch (e: Exception) {
            return "Récemment"
        }
    }

    fun formattedTime(): String {
        try {
            val inputFormatter = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
            inputFormatter.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val date = inputFormatter.parse(createdAt)

            val outputFormatter = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            return outputFormatter.format(date)
        } catch (e: Exception) {
            return ""
        }
    }

    fun formattedDate(): String {
        try {
            val inputFormatter = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
            inputFormatter.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val date = inputFormatter.parse(createdAt)

            val outputFormatter = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            return outputFormatter.format(date)
        } catch (e: Exception) {
            return ""
        }
    }

    fun getBatteryStatusText(): String {
        return when {
            isCharging -> "En charge - $level%"
            level <= 20 -> "Faible - $level%"
            level <= 50 -> "Moyen - $level%"
            else -> "Bon - $level%"
        }
    }

    fun getBatteryColor(): String {
        return when {
            isCharging -> "#4CAF50" // Green
            level <= 20 -> "#F44336" // Red
            level <= 50 -> "#FF9800" // Orange
            else -> "#4CAF50" // Green
        }
    }
}

data class BatteryReportRequest(
    val level: Int,
    val isCharging: Boolean? = null,
    val note: String? = null
)

data class BatteryReportResponse(
    val ok: Boolean,
    val saved: Battery
)

data class BatteryRecentResponse(
    val results: List<Battery>
)