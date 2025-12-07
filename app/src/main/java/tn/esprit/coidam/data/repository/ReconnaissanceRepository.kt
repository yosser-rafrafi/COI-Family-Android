package tn.esprit.coidam.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import tn.esprit.coidam.data.api.ApiClient
import tn.esprit.coidam.data.api.DetectionResponse
import tn.esprit.coidam.data.local.TokenManager

class ReconnaissanceRepository(private val context: Context) {

    private val reconnaissanceApiService = ApiClient.reconnaissanceApiService
    private val tokenManager = TokenManager(context)
    private val TAG = "ReconnaissanceRepository"

    suspend fun detectObjects(
        imageUri: Uri,
        minConfidence: Double = 0.25
    ) = runCatching {
        val imageBytes = context.contentResolver.openInputStream(imageUri)?.use { it.readBytes() }
            ?: throw Exception("Failed to open image stream")

        // Determine content type based on file extension
        val fileName = imageUri.lastPathSegment ?: "image.jpg"
        val contentType = when {
            fileName.lowercase().endsWith(".png") -> "image/png"
            fileName.lowercase().endsWith(".gif") -> "image/gif"
            else -> "image/jpeg"
        }

        val imagePart = MultipartBody.Part.createFormData(
            "image",
            fileName,
            imageBytes.toRequestBody(contentType.toMediaTypeOrNull())
        )

        val minConfidenceBody = minConfidence.toString().toRequestBody("text/plain".toMediaTypeOrNull())

        Log.d(TAG, "Starting object detection with minConfidence: $minConfidence")
        val response = reconnaissanceApiService.detectObjects(
            image = imagePart,
            minConfidence = minConfidenceBody
        )

        Log.d(TAG, "Detection response code: ${response.code()}")
        if (response.isSuccessful && response.body() != null) {
            val detectionResponse = response.body()!!
            Log.d(TAG, "✅ Object detection successful: ${detectionResponse.count} objects detected")
            detectionResponse
        } else {
            val errorBody = response.errorBody()?.string()
            val errorMessage = errorBody ?: "Detection failed with code ${response.code()}"
            Log.e(TAG, "❌ Object detection error (${response.code()}): $errorMessage")
            throw Exception(errorMessage)
        }
    }

    suspend fun getAvailableClasses() = runCatching {
        Log.d(TAG, "Fetching available classes...")
        val response = reconnaissanceApiService.getAvailableClasses()

        Log.d(TAG, "Classes response code: ${response.code()}")
        if (response.isSuccessful && response.body() != null) {
            val classesResponse = response.body()!!
            Log.d(TAG, "✅ Successfully fetched ${classesResponse.classes.size} classes")
            classesResponse.classes
        } else {
            val errorBody = response.errorBody()?.string()
            val errorMessage = errorBody ?: "Failed to fetch classes with code ${response.code()}"
            Log.e(TAG, "❌ Get classes error (${response.code()}): $errorMessage")
            throw Exception(errorMessage)
        }
    }
}



