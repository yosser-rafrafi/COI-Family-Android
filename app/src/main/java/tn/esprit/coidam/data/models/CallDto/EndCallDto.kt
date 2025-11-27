package tn.esprit.coidam.data.models.CallDto

data class EndCallDto(
    val callId: String,
    val endedBy: String,
    val endReason: String? = null
)
