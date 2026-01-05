package tn.esprit.coidam.data.models.Voice

data class VoiceInstruction(
    val action: String,
    val text: String,
    val navigation: String? = null
)