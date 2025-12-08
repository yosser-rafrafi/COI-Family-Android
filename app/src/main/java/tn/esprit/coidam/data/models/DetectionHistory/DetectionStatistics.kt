package tn.esprit.coidam.data.models.DetectionHistory

data class DetectionStatistics(
    val totalDetections: Int,
    val totalFaces: Int,
    val totalKnown: Int,
    val totalUnknown: Int,
    val recognitionRate: Double,
    val mostDetectedPersons: List<MostDetectedPerson>
)

