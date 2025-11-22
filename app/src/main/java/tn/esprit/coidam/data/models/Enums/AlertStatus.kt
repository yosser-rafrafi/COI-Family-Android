package tn.esprit.coidam.data.models.Enums

import com.google.gson.annotations.SerializedName

// Alert Status Enum
enum class AlertStatus(val value: String) {
    @SerializedName("pending")
    PENDING("pending"),

    @SerializedName("acknowledged")
    ACKNOWLEDGED("acknowledged"),

    @SerializedName("resolved")
    RESOLVED("resolved");

    fun displayName(): String = when (this) {
        PENDING -> "En attente"
        ACKNOWLEDGED -> "Vue"
        RESOLVED -> "Résolue"
    }
}