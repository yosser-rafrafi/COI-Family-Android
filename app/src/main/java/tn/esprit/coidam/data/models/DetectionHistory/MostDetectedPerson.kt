package tn.esprit.coidam.data.models.DetectionHistory

data class MostDetectedPerson(
    val name: String,
    val count: Int,
    val relation: String? = null
)
