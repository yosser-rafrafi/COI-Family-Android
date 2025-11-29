package tn.esprit.coidam.data.models.CallDto

import tn.esprit.coidam.data.models.Call
import tn.esprit.coidam.data.models.CallResponses.AgoraCredentials

data class IncomingCallDto(
    val call: Call,
    val agoraCredentials: AgoraCredentials?
)
