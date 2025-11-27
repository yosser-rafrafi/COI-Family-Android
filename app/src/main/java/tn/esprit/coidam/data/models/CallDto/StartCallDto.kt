package tn.esprit.coidam.data.models.CallDto

data class StartCallDto(
    val blindUserId: String,
    val companionId: String,
    val callType: String,
    val initiatedBy: String
)
