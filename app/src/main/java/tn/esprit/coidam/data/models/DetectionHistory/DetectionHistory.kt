package tn.esprit.coidam.data.models.DetectionHistory

import com.google.gson.annotations.SerializedName

data class DetectionHistory(
    @SerializedName("_id")
    val id: String,
    val blindUserId: String,
    val companionUserId: String,
    val capturedImage: String,
    val totalFacesDetected: Int,
    val knownFacesCount: Int,
    val unknownFacesCount: Int,
    val detectedPersons: List<DetectedPersonInHistory>,
    val location: DetectionLocation? = null,
    val createdAt: String,
    val updatedAt: String
) {
    val recognitionRate: Double
        get() = if (totalFacesDetected > 0) {
            (knownFacesCount.toDouble() / totalFacesDetected.toDouble()) * 100
        } else 0.0

    fun getFormattedDate(): String {
        // Format: 2025-12-06T10:30:00.000Z -> 06/12/2025 10:30
        return try {
            val dateTime = createdAt.split("T")
            val date = dateTime[0].split("-")
            val time = dateTime[1].split(":")
            "${date[2]}/${date[1]}/${date[0]} ${time[0]}:${time[1]}"
        } catch (e: Exception) {
            createdAt
        }
    }
}