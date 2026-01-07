package tn.esprit.coidam.data.models

import com.google.gson.annotations.SerializedName
import java.util.Date

data class Photo(
    @SerializedName("_id") val id: String,
    val filename: String,
    val caption: String?,
    @SerializedName("ownerId") val ownerId: String,
    @SerializedName("sharedWith") val sharedWith: List<String>?,
    val location: PhotoLocation?,
    @SerializedName("createdAt") val createdAt: Date?,
    @SerializedName("updatedAt") val updatedAt: Date?,
    val detections: List<Detection>?
)

data class Detection(
    val className: String?,
    val label: String?,
    val confidence: Float?,
    val box: List<Float>?
)

/**
 * Represents a geographical location.
 * Renamed from Location to avoid conflicts with android.location.Location.
 */
data class PhotoLocation(
    val lat: Double,
    val lng: Double
)
