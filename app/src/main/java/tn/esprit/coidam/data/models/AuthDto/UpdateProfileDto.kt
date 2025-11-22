package tn.esprit.coidam.data.models.AuthDto


data class UpdateProfileDto(
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String
)