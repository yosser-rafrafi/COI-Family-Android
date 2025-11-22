package tn.esprit.coidam.data.models.Enums

import com.google.gson.annotations.SerializedName

// Alert Type Enum
enum class AlertType(val value: String) {
    @SerializedName("emergency")
    EMERGENCY("emergency"),

    @SerializedName("assistance")
    ASSISTANCE("assistance");

    fun displayName(): String = when (this) {
        EMERGENCY -> "Urgence"
        ASSISTANCE -> "Assistance"
    }

    fun icon(): String = when (this) {
        EMERGENCY -> "emergency"
        ASSISTANCE -> "assistance"
    }
}