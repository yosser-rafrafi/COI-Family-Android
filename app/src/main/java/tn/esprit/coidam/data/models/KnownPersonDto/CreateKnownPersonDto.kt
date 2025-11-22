package tn.esprit.coidam.data.models.KnownPersonDto

data class CreateKnownPersonDto(
    val name: String,
    val relation: String? = null,
    val phone: String? = null,
    val image: String // Required, not nullable
)
