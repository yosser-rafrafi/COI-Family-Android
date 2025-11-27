package tn.esprit.coidam.data.models.CallDto

data class RejectCallDto(
    val callId: String,
    val reason: String? = null
)