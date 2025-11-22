package tn.esprit.coidam.data.models.AuthDto

data class UpdatePasswordDto(
    val currentPassword: String,
    val newPassword: String
)