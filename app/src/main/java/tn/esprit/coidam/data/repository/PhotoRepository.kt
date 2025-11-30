package tn.esprit.coidam.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import tn.esprit.coidam.data.api.ApiClient
import tn.esprit.coidam.data.local.TokenManager

class PhotoRepository(private val context: Context) {

    private val photoApiService = ApiClient.photoApiService
    private val tokenManager = TokenManager(context)
    private val TAG = "PhotoRepository"

    suspend fun getPhotos() = runCatching {
        val token = tokenManager.getTokenSync()
        if (token == null) {
            Log.e(TAG, "❌ No authentication token found")
            throw Exception("No authentication token found")
        }

        val userId = tokenManager.getUserIdSync()
        if (userId == null) {
            Log.e(TAG, "❌ No user ID found")
            throw Exception("User ID not found, cannot fetch photos")
        }

        Log.d(TAG, "📥 Fetching photos for userId: $userId")
        
        // Pass both token and userId to the API service
        val response = photoApiService.getPhotos("Bearer $token", userId)
        
        Log.d(TAG, "Get photos response code: ${response.code()}")
        if (response.isSuccessful && response.body() != null) {
            val photos = response.body()!!
            Log.d(TAG, "✅ Successfully fetched ${photos.size} photos")
        } else {
            val errorBody = response.errorBody()?.string()
            Log.e(TAG, "❌ Get photos error (${response.code()}): $errorBody")
        }
        
        response
    }

    suspend fun uploadPhoto(
        imageUri: Uri,
        caption: String?,
        lat: Double?,
        lng: Double?,
        isPublic: Boolean? = false // Default to false if not provided
    ) = runCatching {
        val token = tokenManager.getTokenSync()
        if (token == null) {
            throw Exception("No authentication token found")
        }

        val imageBytes = context.contentResolver.openInputStream(imageUri)?.use { it.readBytes() }
            ?: throw Exception("Failed to open image stream")

        val imagePart = MultipartBody.Part.createFormData(
            "file",
            "image.jpg",
            imageBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
        )

        val ownerId = tokenManager.getUserId().first()

        val ownerIdBody = ownerId?.toRequestBody("text/plain".toMediaTypeOrNull())
        val captionBody = caption?.toRequestBody("text/plain".toMediaTypeOrNull())
        val latBody = lat?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
        val lngBody = lng?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
        val isPublicBody = isPublic?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())

        Log.d(TAG, "Starting photo upload...")
        val response = photoApiService.uploadPhoto(
            token = "Bearer $token",
            file = imagePart,
            caption = captionBody,
            ownerId = ownerIdBody,
            lat = latBody,
            lng = lngBody,
            isPublic = isPublicBody
        )

        Log.d(TAG, "Upload response code: ${response.code()}")
        if (response.isSuccessful && response.body() != null) {
            val photo = response.body()!!
            Log.d(TAG, "✅ Photo uploaded successfully: ${photo.id}")
            photo
        } else {
            val errorBody = response.errorBody()?.string()
            val errorMessage = errorBody ?: "Upload failed with code ${response.code()}"
            Log.e(TAG, "❌ Upload photo error (${response.code()}): $errorMessage")
            throw Exception(errorMessage)
        }
    }
}
