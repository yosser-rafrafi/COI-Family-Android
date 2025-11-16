package tn.esprit.coidam.data.models

data class CreateKnownPersonDto(
    val name: String,
    val relation: String? = null,
    val phone: String? = null,
    val image: String // Required, not nullable
)

data class UpdateKnownPersonDto(
    val name: String? = null,
    val relation: String? = null,
    val phone: String? = null,
    val image: String? = null
)

data class KnownPerson(
    val _id: String? = null,
    val id: String? = null,
    val name: String,
    val relation: String? = null,
    val phone: String? = null,
    val image: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
) {
    fun resolvedId(): String {
        return _id ?: id ?: ""
    }
}

