package tn.esprit.coidam.data.models.FaceRecognition

import com.google.gson.annotations.SerializedName

data class RecognitionSummary(
    @SerializedName("total_faces")
    val totalFaces: Int,
    @SerializedName("known_faces")
    val knownFaces: Int,
    @SerializedName("unknown_faces")
    val unknownFaces: Int
)
