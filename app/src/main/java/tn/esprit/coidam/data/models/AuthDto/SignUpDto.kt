package tn.esprit.coidam.data.models.AuthDto

data class SignUpDto(
    val email: String,
    val password: String,
    val userType: String = "companion" // Backend only accepts 'companion'
)
