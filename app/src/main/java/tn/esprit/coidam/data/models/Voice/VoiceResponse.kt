package tn.esprit.coidam.data.models.Voice

data class VoiceResponse(
    val success: Boolean,
    val action: String? = null,
    val message: String,
    val speakText: String? = null,
    val navigation: String? = null,
    val transcription: String? = null
)