package tn.esprit.coidam.data.models.FaceRecognition

data class ProcessAllResponse(
    val success: Boolean,
    val total: Int,
    val processed: Int,
    val failed: Int? = 0,
    val results: List<ProcessResult>? = null,
    val message: String? = null
) {
    val failedCount: Int
        get() = failed ?: 0
}
