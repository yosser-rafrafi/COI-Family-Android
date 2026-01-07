package tn.esprit.coidam.data.models.AuthDto

data class UpdateBlindProfileDto(
    val blindUserId: String,
    val firstName: String,
    val lastName: String? = null,
    val phoneNumber: String? = null,
    val relation: String? = null,
    val profileImage: String? = null // URL de l'image de profil
)

