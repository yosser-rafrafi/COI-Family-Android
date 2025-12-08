package tn.esprit.coidam.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import tn.esprit.coidam.data.api.ApiClient
import tn.esprit.coidam.data.local.TokenManager
import tn.esprit.coidam.data.models.*
import tn.esprit.coidam.data.models.DetectionHistory.RecognizeRequest
import tn.esprit.coidam.data.models.FaceRecognition.ProcessAllResponse
import tn.esprit.coidam.data.models.FaceRecognition.RecognitionResponse
import java.io.ByteArrayOutputStream

class FaceRecognitionRepository(private val context: Context) {
    private val apiService = ApiClient.faceRecognitionApiService
    private val tokenManager = TokenManager(context)
    private val TAG = "FaceRecognitionRepo"

    // ✅ VÉRIFIER LA SANTÉ DU SERVICE
    suspend fun checkHealth(): Result<Boolean> {
        return try {
            val token = tokenManager.getTokenSync()
            if (token == null) {
                return Result.failure(Exception("No authentication token found"))
            }

            val response = apiService.checkHealth("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                val status = response.body()?.get("status")
                Result.success(status == "ok")
            } else {
                Result.failure(Exception("Service unavailable"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Health check exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ✅ TRAITER TOUTES LES PERSONNES CONNUES (extraire leurs encodings)
    suspend fun processAllKnownPersons(): Result<ProcessAllResponse> {
        return try {
            val token = tokenManager.getTokenSync()
            if (token == null) {
                return Result.failure(Exception("No authentication token found"))
            }

            Log.d(TAG, "📊 Processing all known persons...")

            val response = apiService.processAllKnownPersons("Bearer $token")

            if (response.isSuccessful && response.body() != null) {
                val result = response.body()!!
                Log.d(TAG, "✅ Process All Response: Total=${result.total}, Processed=${result.processed}, Failed=${result.failedCount}")
                Result.success(result)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to process persons"
                Log.e(TAG, "Process all error: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Process all exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ✅ RECONNAÎTRE LES VISAGES DANS UNE IMAGE
    suspend fun recognizeFaces(
        bitmap: Bitmap,
        saveToHistory: Boolean = true
    ): Result<RecognitionResponse> {
        return try {
            val token = tokenManager.getTokenSync()
            if (token == null) {
                return Result.failure(Exception("No authentication token found"))
            }

            // Préparer l'image (compression si nécessaire)
            val processedBitmap = prepareImage(bitmap)

            // Convertir en base64
            val base64Image = bitmapToBase64(processedBitmap)
            val imageBase64 = "data:image/jpeg;base64,$base64Image"

            Log.d(TAG, "🔍 Recognizing faces in image (saveToHistory: $saveToHistory)...")
            Log.d(TAG, "📸 Image size: ${base64Image.length} bytes")

            val request = RecognizeRequest(
                image = imageBase64,
                saveToHistory = saveToHistory
            )

            val response = apiService.recognizeFaces("Bearer $token", request)

            if (response.isSuccessful && response.body() != null) {
                val result = response.body()!!
                Log.d(TAG, "✅ Recognition Response: ${result.facesDetected} faces detected")

                if (result.facesDetected == 0) {
                    Log.w(TAG, "⚠️ No faces detected in the image")
                } else if (saveToHistory) {
                    Log.d(TAG, "💾 Detection saved to history")
                }

                Result.success(result)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to recognize faces"
                Log.e(TAG, "Recognition error: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Recognition exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ✅ PRÉPARER L'IMAGE (redimensionner si trop grande)
    private fun prepareImage(bitmap: Bitmap): Bitmap {
        val maxDimension = 1920
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxDimension && height <= maxDimension) {
            return bitmap
        }

        val ratio = minOf(
            maxDimension.toFloat() / width,
            maxDimension.toFloat() / height
        )

        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()

        Log.d(TAG, "📐 Image resized from ${width}x${height} to ${newWidth}x${newHeight}")

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    // ✅ CONVERTIR BITMAP EN BASE64
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()

        // Compression adaptative
        var quality = 80
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)

        // Si trop grand, réduire la qualité
        while (outputStream.size() > 5_000_000 && quality > 30) {
            outputStream.reset()
            quality -= 10
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            Log.d(TAG, "📉 Reducing compression to $quality%")
        }

        val byteArray = outputStream.toByteArray()
        Log.d(TAG, "📸 Final image size: ${byteArray.size} bytes, quality: $quality%")

        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}