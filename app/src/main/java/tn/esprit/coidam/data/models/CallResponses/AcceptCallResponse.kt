package tn.esprit.coidam.data.models.CallResponses

import tn.esprit.coidam.data.models.Call

data class AcceptCallResponse(
    val call: Call,
    val agoraCredentials: AgoraCredentials
)

