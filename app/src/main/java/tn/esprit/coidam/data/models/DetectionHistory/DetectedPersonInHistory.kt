package tn.esprit.coidam.data.models.DetectionHistory

import tn.esprit.coidam.data.models.FaceRecognition.FaceLocation

data class DetectedPersonInHistory(
    val personId: String? = null,
    val name: String,
    val relation: String? = null,
    val phone: String? = null,
    val confidence: Double,
    val status: String,
    val faceLocation: FaceLocation
) {
    val isRecognized: Boolean
        get() = status == "recognized"

    val confidencePercentage: Int
        get() = (confidence * 100).toInt()
}