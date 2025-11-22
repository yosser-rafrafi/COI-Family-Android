package tn.esprit.coidam.data.models

data class Location(
    val latitude: Double,
    val longitude: Double,
    val address: String? = null
)
