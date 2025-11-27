package tn.esprit.coidam.data.models.CallResponses

import tn.esprit.coidam.data.models.Call

data class CallHistoryResponse(
    val calls: List<Call>,
    val total: Int,
    val limit: Int,
    val skip: Int
)
