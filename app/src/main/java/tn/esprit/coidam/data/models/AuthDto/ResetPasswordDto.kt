package tn.esprit.coidam.data.models.AuthDto

data class ResetPasswordDto(
    val code: String,
    val newPassword: String
)