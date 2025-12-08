package tn.esprit.coidam.data.models.DetectionHistory

data class RecognizeRequest(
    val image: String,
    val saveToHistory: Boolean = true
)
