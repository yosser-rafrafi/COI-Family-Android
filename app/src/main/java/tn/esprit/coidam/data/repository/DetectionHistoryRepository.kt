package tn.esprit.coidam.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tn.esprit.coidam.data.api.ApiClient
import tn.esprit.coidam.data.local.TokenManager
import tn.esprit.coidam.data.models.*
import tn.esprit.coidam.data.models.DetectionHistory.DetectionHistory
import tn.esprit.coidam.data.models.DetectionHistory.DetectionStatistics
import java.net.URL

class DetectionHistoryRepository(private val context: Context) {
    private val apiService = ApiClient.detectionHistoryApiService
    private val tokenManager = TokenManager(context)
    private val TAG = "DetectionHistoryRepo"
    private val BASE_API_URL = ApiClient.BASE_URL.removeSuffix("/")  // Remove trailing slash for consistency


    // ✅ RÉCUPÉRER TOUT L'HISTORIQUE
    suspend fun getAll(limit: Int = 50, skip: Int = 0): Result<List<DetectionHistory>> {
        return try {
            val token = tokenManager.getTokenSync()
            if (token == null) {
                return Result.failure(Exception("No authentication token found"))
            }

            val response = apiService.getAll("Bearer $token", limit, skip)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to get detection history"
                Log.e(TAG, "Get all error: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Get all exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ✅ RÉCUPÉRER UNE DÉTECTION SPÉCIFIQUE
    suspend fun getOne(id: String): Result<DetectionHistory> {
        return try {
            val token = tokenManager.getTokenSync()
            if (token == null) {
                return Result.failure(Exception("No authentication token found"))
            }

            val response = apiService.getOne("Bearer $token", id)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Detection not found"
                Log.e(TAG, "Get one error: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Get one exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ✅ OBTENIR LES STATISTIQUES
    suspend fun getStatistics(): Result<DetectionStatistics> {
        return try {
            val token = tokenManager.getTokenSync()
            if (token == null) {
                return Result.failure(Exception("No authentication token found"))
            }

            val response = apiService.getStatistics("Bearer $token")

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to get statistics"
                Log.e(TAG, "Get statistics error: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Get statistics exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ✅ SUPPRIMER UNE DÉTECTION
    suspend fun delete(id: String): Result<String> {
        return try {
            val token = tokenManager.getTokenSync()
            if (token == null) {
                return Result.failure(Exception("No authentication token found"))
            }

            val response = apiService.delete("Bearer $token", id)

            if (response.isSuccessful) {
                Result.success("Detection deleted successfully")
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to delete detection"
                Log.e(TAG, "Delete error: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Delete exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ✅ CHARGER UNE IMAGE DEPUIS UNE URL
    suspend fun loadImage(imageUrl: String): Result<Bitmap> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Construire l'URL complète si nécessaire
            val fullUrl = if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
                imageUrl
            } else {
                val cleanPath = if (imageUrl.startsWith("/")) imageUrl else "/$imageUrl"
                "$BASE_API_URL$cleanPath"
            }

            Log.d(TAG, "Loading image from: $fullUrl")

            val url = URL(fullUrl)
            val connection = url.openConnection()
            connection.doInput = true
            connection.connect()

            val inputStream = connection.getInputStream()
            val bitmap = BitmapFactory.decodeStream(inputStream)

            if (bitmap != null) {
                Result.success(bitmap)
            } else {
                Result.failure(Exception("Failed to decode image"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Load image exception: ${e.message}", e)
            Result.failure(e)
        }
    }
}
