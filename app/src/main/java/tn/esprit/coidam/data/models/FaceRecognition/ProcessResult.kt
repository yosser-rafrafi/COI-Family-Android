package tn.esprit.coidam.data.models.FaceRecognition

data class ProcessResult(
    val personId: String,
    val name: String,
    val success: Boolean,
    val error: String? = null,
    val message: String? = null
)
