package tn.esprit.coidam.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.RequestBody
import tn.esprit.coidam.data.api.ApiClient
import tn.esprit.coidam.data.local.TokenManager
import tn.esprit.coidam.data.models.*
import tn.esprit.coidam.data.models.KnownPersonDto.CreateKnownPersonDto
import tn.esprit.coidam.data.models.KnownPersonDto.UpdateKnownPersonDto
import java.io.File
import java.io.FileOutputStream

class KnownPersonRepository(private val context: Context) {
    private val apiService = ApiClient.knownPersonApiService
    private val tokenManager = TokenManager(context)
    private val TAG = "KnownPersonRepository"

    suspend fun findAll(): Result<List<KnownPerson>> {
        return try {
            val token = tokenManager.getTokenSync()
            if (token == null) {
                return Result.failure(Exception("No authentication token found"))
            }

            val response = apiService.findAll("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to get known persons"
                Log.e(TAG, "Find all error: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Find all exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun findOne(id: String): Result<KnownPerson> {
        return try {
            val token = tokenManager.getTokenSync()
            if (token == null) {
                return Result.failure(Exception("No authentication token found"))
            }

            val response = apiService.findOne("Bearer $token", id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to get known person"
                Log.e(TAG, "Find one error: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Find one exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun create(name: String, relation: String?, phone: String?, imageUri: Uri?): Result<KnownPerson> {
        return try {
            val token = tokenManager.getTokenSync()
            if (token == null) {
                return Result.failure(Exception("No authentication token found"))
            }

            // Image is required - upload it first
            if (imageUri == null) {
                return Result.failure(Exception("Image is required"))
            }

            // Upload image first to get URL
            val uploadResult = uploadImage(imageUri)
            val imageUrl = uploadResult.getOrElse { exception ->
                Log.e(TAG, "Image upload failed: ${exception.message}")
                return Result.failure(Exception("Failed to upload image: ${exception.message}"))
            }

            // Ensure imageUrl is not empty
            val trimmedImageUrl = imageUrl.trim()
            if (trimmedImageUrl.isBlank()) {
                Log.e(TAG, "Image URL is empty after upload")
                return Result.failure(Exception("Image URL is empty after upload"))
            }

            Log.d(TAG, "Creating known person with image URL: $trimmedImageUrl")

            // Ensure name is not empty
            val trimmedName = name.trim()
            if (trimmedName.isBlank()) {
                return Result.failure(Exception("Name cannot be empty"))
            }

            val dto = CreateKnownPersonDto(
                name = trimmedName,
                relation = relation?.trim()?.takeIf { it.isNotBlank() },
                phone = phone?.trim()?.takeIf { it.isNotBlank() },
                image = trimmedImageUrl
            )

            Log.d(TAG, "Sending DTO: name='${dto.name}', relation='${dto.relation}', phone='${dto.phone}', image='${dto.image}'")

            val response = apiService.create("Bearer $token", dto)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val statusCode = response.code()
                val errorMessage = response.errorBody()?.string() ?: "Failed to create known person"
                Log.e(TAG, "Create error ($statusCode): $errorMessage")
                Log.e(TAG, "DTO sent: name='${dto.name}', relation='${dto.relation}', phone='${dto.phone}', image='${dto.image}'")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Create exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun update(id: String, name: String?, relation: String?, phone: String?, imageUri: Uri?): Result<KnownPerson> {
        return try {
            val token = tokenManager.getTokenSync()
            if (token == null) {
                return Result.failure(Exception("No authentication token found"))
            }

            var imageUrl: String? = null

            // Upload image if provided (for update, image is optional)
            imageUri?.let { uri ->
                val uploadResult = uploadImage(uri)
                imageUrl = uploadResult.getOrElse { exception ->
                    Log.e(TAG, "Image upload failed: ${exception.message}")
                    return Result.failure(Exception("Failed to upload image: ${exception.message}"))
                }
            }

            val dto = UpdateKnownPersonDto(
                name = name,
                relation = relation,
                phone = phone,
                image = imageUrl
            )

            val response = apiService.update("Bearer $token", id, dto)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to update known person"
                Log.e(TAG, "Update error: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Update exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun remove(id: String): Result<String> {
        return try {
            val token = tokenManager.getTokenSync()
            if (token == null) {
                return Result.failure(Exception("No authentication token found"))
            }

            val response = apiService.remove("Bearer $token", id)
            if (response.isSuccessful) {
                Result.success("Known person deleted successfully")
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to delete known person"
                Log.e(TAG, "Delete error: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Delete exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun uploadImage(uri: Uri): Result<String> {
        return try {
            val token = tokenManager.getTokenSync()
            if (token == null) {
                return Result.failure(Exception("No authentication token found"))
            }

            // Convert URI to File
            val inputStream = context.contentResolver.openInputStream(uri)
            val file = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            inputStream?.use { it.copyTo(outputStream) }
            outputStream.close()

            // Create multipart request
            val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("image", file.name, requestFile)

            val response = apiService.uploadImage("Bearer $token", body)
            if (response.isSuccessful && response.body() != null) {
                val responseBody = response.body()!!
                Log.d(TAG, "Upload response body: $responseBody")
                
                // Try different possible keys for the image URL
                val imageUrl = (responseBody["url"] as? String)
                    ?: (responseBody["imageUrl"] as? String)
                    ?: (responseBody["image"] as? String)
                    ?: (responseBody["path"] as? String)
                    ?: (responseBody["file"] as? String)
                    ?: (responseBody["filename"] as? String)
                
                if (imageUrl != null && imageUrl.isNotEmpty()) {
                    // Clean up temp file
                    file.delete()
                    Log.d(TAG, "Image uploaded successfully: $imageUrl")
                    Result.success(imageUrl)
                } else {
                    Log.e(TAG, "Image URL not found in response: $responseBody")
                    Log.e(TAG, "Response keys: ${responseBody.keys}")
                    file.delete()
                    Result.failure(Exception("Image URL not found in response. Response: $responseBody"))
                }
            } else {
                val statusCode = response.code()
                val errorMessage = response.errorBody()?.string() ?: "Failed to upload image"
                Log.e(TAG, "Upload image error ($statusCode): $errorMessage")
                file.delete()
                Result.failure(Exception("Failed to upload image ($statusCode): $errorMessage"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Upload image exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun createWithMultipart(
        token: String,
        name: String,
        relation: String?,
        phone: String?,
        imageUri: Uri
    ): Result<KnownPerson> {
        return try {
            // Convert URI to File
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val file = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            inputStream?.use { it.copyTo(outputStream) }
            outputStream.close()

            // Create multipart request
            val nameBody = name.toRequestBody("text/plain".toMediaTypeOrNull())
            val relationBody = relation?.toRequestBody("text/plain".toMediaTypeOrNull())
            val phoneBody = phone?.toRequestBody("text/plain".toMediaTypeOrNull())
            val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val imagePart = MultipartBody.Part.createFormData("image", file.name, requestFile)

            val response = apiService.createWithImage(
                "Bearer $token",
                nameBody,
                relationBody,
                phoneBody,
                imagePart
            )

            // Clean up temp file
            file.delete()

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to create known person"
                Log.e(TAG, "Create with multipart error: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Create with multipart exception: ${e.message}", e)
            Result.failure(e)
        }
    }
}

