package tn.esprit.coidam.data.models.Enums

import com.google.gson.annotations.SerializedName

enum class CallStatus {
    @SerializedName("pending") PENDING,
    @SerializedName("ringing") RINGING,
    @SerializedName("active") ACTIVE,
    @SerializedName("completed") COMPLETED,
    @SerializedName("missed") MISSED,
    @SerializedName("rejected") REJECTED,
    @SerializedName("failed") FAILED;

    fun displayName(): String = when (this) {
        PENDING -> "En attente"
        RINGING -> "Sonne"
        ACTIVE -> "En cours"
        COMPLETED -> "Terminé"
        MISSED -> "Manqué"
        REJECTED -> "Rejeté"
        FAILED -> "Échoué"
    }
}