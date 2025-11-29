package tn.esprit.coidam.data.models.CallResponses

data class AgoraCredentials(
    val token: String,
    val channelName: String,
    val uid: Int,
    val expiresAt: Long? = null // Optional, not always returned by backend
)
