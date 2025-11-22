package tn.esprit.coidam.data.models

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

