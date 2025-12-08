package tn.esprit.coidam.data.models.FaceRecognition

import com.google.gson.annotations.SerializedName

data class RecognitionResponse(
    val success: Boolean,
    @SerializedName("faces_detected")
    val facesDetected: Int,
    @SerializedName("recognized_persons")
    val recognizedPersons: List<RecognizedPerson>,
    val summary: RecognitionSummary? = null,
    val message: String? = null
)
