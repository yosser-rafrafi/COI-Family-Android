package tn.esprit.coidam.data.models.Alerts

import tn.esprit.coidam.data.models.Location

data class CreateAlertRequest(
    val blindUserId: String,
    val companionId: String,
    val type: String,
    val location: Location? = null,
    val voiceMessage: String? = null
)
