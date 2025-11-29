package tn.esprit.coidam.data.models

import com.google.gson.annotations.SerializedName
import tn.esprit.coidam.data.models.Enums.CallStatus
import tn.esprit.coidam.data.models.Enums.CallType

data class Call(
    @SerializedName("_id") val id: String,
    val status: CallStatus,
    val callType: CallType,
    val agoraChannelName: String?,
    val agoraToken: String?,
    val agoraUid: Int?,
    val startedAt: String?,
    val endedAt: String?,
    val duration: Int?,
    val initiatedBy: String,
    val endedBy: String?,
    val endReason: String?,
    val createdAt: String,
    val updatedAt: String,
    val blindUser: UserResponse? = null,
    val companion: UserResponse? = null
) {
    fun timeAgo(): String {
        // Similar to Alert.timeAgo()
        val now = System.currentTimeMillis()
        val createdTime = parseIso8601(createdAt) ?: return "Unknown"
        val diff = now - createdTime

        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 0 -> "Il y a $days jour${if (days > 1) "s" else ""}"
            hours > 0 -> "Il y a $hours heure${if (hours > 1) "s" else ""}"
            minutes > 0 -> "Il y a $minutes minute${if (minutes > 1) "s" else ""}"
            else -> "À l'instant"
        }
    }

    fun formattedDuration(): String {
        if (duration == null) return "0:00"
        val mins = duration / 60
        val secs = duration % 60
        return "${mins}:${secs.toString().padStart(2, '0')}"
    }

    private fun parseIso8601(dateString: String): Long? {
        return try {
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }.parse(dateString)?.time
        } catch (e: Exception) {
            null
        }
    }
}