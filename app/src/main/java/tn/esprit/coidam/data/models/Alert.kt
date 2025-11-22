package tn.esprit.coidam.data.models

import com.google.gson.annotations.SerializedName
import tn.esprit.coidam.data.models.Alerts.AlertUserInfo
import tn.esprit.coidam.data.models.Enums.AlertStatus
import tn.esprit.coidam.data.models.Enums.AlertType

data class Alert(
    @SerializedName("_id")
    val id: String,
    val blindUserId: String,
    val companionId: String,
    val type: AlertType,
    val status: AlertStatus,
    val location: Location? = null,
    val voiceMessage: String? = null,
    val createdAt: String,
    val acknowledgedAt: String? = null,
    val resolvedAt: String? = null,

    // Ces champs peuvent être des objets si le backend fait populate()
    val blindUser: AlertUserInfo? = null,
    val companionUser: AlertUserInfo? = null
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
}
