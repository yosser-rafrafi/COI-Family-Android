package tn.esprit.coidam.data.models.Alerts


import com.google.gson.annotations.SerializedName

// Alert User Info (populated)
data class AlertUserInfo(
    @SerializedName("_id")
    val id: String? = null,
    val email: String? = null,
    val firstName: String? = null,
    val lastName: String? = null
) {
    fun fullName(): String {
        return if (firstName != null && lastName != null) {
            "$firstName $lastName"
        } else {
            email ?: "Utilisateur"
        }
    }
}