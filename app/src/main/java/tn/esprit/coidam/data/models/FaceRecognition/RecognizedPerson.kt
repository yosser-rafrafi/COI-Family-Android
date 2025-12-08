package tn.esprit.coidam.data.models.FaceRecognition

import com.google.gson.annotations.SerializedName

data class RecognizedPerson(
    @SerializedName("person_id")
    val personId: String? = null,
    val name: String,
    val relation: String? = null,
    val phone: String? = null,
    val confidence: Double,
    val status: String,
    @SerializedName("face_location")
    val faceLocation: FaceLocation
) {
    val isRecognized: Boolean
        get() = status == "recognized"

    val confidencePercentage: Int
        get() = (confidence * 100).toInt()
}