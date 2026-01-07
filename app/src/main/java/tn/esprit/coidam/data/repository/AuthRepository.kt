package tn.esprit.coidam.data.repository

import android.content.Context
import android.util.Log
import tn.esprit.coidam.data.api.ApiClient
import tn.esprit.coidam.data.api.VoiceWebSocketClient
import tn.esprit.coidam.data.local.TokenManager
import tn.esprit.coidam.data.models.*
import tn.esprit.coidam.data.models.AuthDto.ForgotPasswordDto
import tn.esprit.coidam.data.models.AuthDto.LoginAsDto
import tn.esprit.coidam.data.models.AuthDto.ResetPasswordDto
import tn.esprit.coidam.data.models.AuthDto.SignInDto
import tn.esprit.coidam.data.models.AuthDto.SignUpDto
import tn.esprit.coidam.data.models.AuthDto.UpdatePasswordDto
import tn.esprit.coidam.data.models.AuthDto.UpdateProfileDto
import tn.esprit.coidam.data.models.AuthDto.UpdateBlindProfileDto

class AuthRepository(private val context: Context) {
    private val apiService = ApiClient.authApiService
    private val tokenManager = TokenManager(context)
    private val TAG = "AuthRepository"

    suspend fun signUp(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        phoneNumber: String
    ): Result<SignUpResponse> {
        return try {
            val dto = SignUpDto(email, password, firstName, lastName, phoneNumber, "companion")
            val response = apiService.signUp(dto)
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = errorBody ?: "Sign up failed"
                Log.e(TAG, "Sign up error: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sign up exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun signIn(email: String, password: String): Result<AuthResponse> {
        return try {
            val dto = SignInDto(email, password)
            val response = apiService.signIn(dto)
            
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                
                // Check if response contains an error
                if (!authResponse.error.isNullOrEmpty()) {
                    Log.e(TAG, "Sign in error: ${authResponse.error}")
                    return Result.failure(Exception(authResponse.error))
                }
                
                // If we have options, user needs to choose which profile to use
                if (authResponse.options != null && authResponse.options.isNotEmpty()) {
                    return Result.success(authResponse)
                }
                
                // If we have a token, save it
                authResponse.access_token?.let { token ->
                    tokenManager.saveToken(token)
                    authResponse.user?.getUserId()?.let { userId ->
                        tokenManager.saveUserId(userId)
                    }
                    authResponse.user?.userType?.let { userType ->
                        tokenManager.saveUserType(userType)
                    }
                    authResponse.user?.email?.let { email ->
                        tokenManager.saveUserEmail(email)
                    }
                    authResponse.user?.linkedUserId?.let { linkedId ->
                        tokenManager.saveLinkedUserId(linkedId)
                        Log.d(TAG, "✅ Saved linkedUserId: $linkedId")
                    }
                }
                
                Result.success(authResponse)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = errorBody ?: "Sign in failed"
                Log.e(TAG, "Sign in error: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sign in exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun loginAs(userId: String?, userType: String?): Result<AuthResponse> {
        return try {
            val dto = LoginAsDto(userId, userType)
            val response = apiService.loginAs(dto)
            
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                
                // Check if response contains an error
                if (!authResponse.error.isNullOrEmpty()) {
                    Log.e(TAG, "Login as error: ${authResponse.error}")
                    return Result.failure(Exception(authResponse.error))
                }
                
                // Save token and user info
                authResponse.access_token?.let { token ->
                    tokenManager.saveToken(token)
                    // Use userId from response user object if available, otherwise use parameter
                    val finalUserId = authResponse.user?.getUserId() ?: userId
                    val finalUserType = authResponse.user?.userType ?: userType
                    tokenManager.saveUserId(finalUserId)
                    tokenManager.saveUserType(finalUserType)
                    authResponse.user?.email?.let { email ->
                        tokenManager.saveUserEmail(email)
                    }
                    authResponse.user?.linkedUserId?.let { linkedId ->
                        tokenManager.saveLinkedUserId(linkedId)
                        Log.d(TAG, "✅ Saved linkedUserId after loginAs: $linkedId")
                    }
                }
                
                Result.success(authResponse)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = errorBody ?: "Login as failed"
                Log.e(TAG, "Login as error: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Login as exception: ${e.message}", e)
            Result.failure(e)
        }
    }


    // ✅ NOUVELLE FONCTION POUR RÉCUPÉRER LE PROFIL AVEC linkedUserId
    suspend fun getProfileWithLinkedUser(): Result<UserResponse> {
        return try {
            val token = tokenManager.getTokenSync()
            if (token.isNullOrEmpty()) {
                return Result.failure(Exception("No token found"))
            }

            val response = apiService.getProfile("Bearer $token")

            if (response.isSuccessful && response.body() != null) {
                val profile = response.body()!!



                Result.success(profile)
            } else {
                Result.failure(Exception("Failed to get profile"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun forgotPassword(email: String): Result<String> {
        return try {
            val dto = ForgotPasswordDto(email)
            val response = apiService.forgotPassword(dto)

            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody != null) {
                    Log.d(TAG, "Forgot password response: $responseBody")
                }
                val message = when {
                    responseBody == null -> "Password reset email sent"
                    responseBody["message"] is String -> responseBody["message"] as String
                    responseBody["message"] != null -> {
                        // If message is an object, try to extract a meaningful string
                        val messageObj = responseBody["message"]
                        if (messageObj is Map<*, *>) {
                            (messageObj["message"] as? String) ?: 
                            (messageObj["text"] as? String) ?: 
                            messageObj.toString()
                        } else {
                            messageObj.toString()
                        }
                    }
                    else -> "Password reset email sent"
                }
                Result.success(message)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "Forgot password error response: $errorBody")
                val errorMessage = errorBody ?: "Failed to send reset email"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Forgot password exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun resetPassword(code: String, newPassword: String): Result<String> {
        return try {
            val dto = ResetPasswordDto(code, newPassword)
            val response = apiService.resetPassword(dto)

            if (response.isSuccessful) {
                val message = response.body()?.get("message") as? String ?: "Password reset successful"
                Result.success(message)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to reset password"
                Log.e(TAG, "Reset password error: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Reset password exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updatePassword(currentPassword: String, newPassword: String): Result<String> {
        return try {
            val token = tokenManager.getTokenSync()
            if (token == null) {
                return Result.failure(Exception("No authentication token found"))
            }

            val dto = UpdatePasswordDto(currentPassword, newPassword)
            val response = apiService.updatePassword("Bearer $token", dto)

            if (response.isSuccessful) {
                val message = response.body()?.get("message") as? String ?: "Password updated successfully"
                Result.success(message)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to update password"
                Log.e(TAG, "Update password error: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Update password exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(idToken: String): Result<AuthResponse> {
        return try {
            val dto = mapOf("idToken" to idToken)
            val response = apiService.signInWithGoogle(dto)
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                authResponse.access_token?.let { token ->
                    tokenManager.saveToken(token)
                    authResponse.user?.getUserId()?.let { userId ->
                        tokenManager.saveUserId(userId)
                    }
                    authResponse.user?.userType?.let { userType ->
                        tokenManager.saveUserType(userType)
                    }
                    authResponse.user?.email?.let { email ->
                        tokenManager.saveUserEmail(email)
                    }
                    // ✅ SAUVEGARDER linkedUserId
                    authResponse.user?.linkedUserId?.let { linkedId ->
                        tokenManager.saveLinkedUserId(linkedId)
                        Log.d(TAG, "✅ Saved linkedUserId from Google: $linkedId")
                    }
                }
                Result.success(authResponse)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Google Sign-In failed"
                val statusCode = response.code()
                Log.e(TAG, "Google Sign-In backend error ($statusCode): $errorBody")
                Result.failure(GoogleSignInException(statusCode, errorBody))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In exception: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    // Custom exception for Google Sign-In errors
    class GoogleSignInException(val statusCode: Int, message: String) : Exception(message)


    suspend fun getProfile(): Result<UserResponse> {
        return try {
            val token = tokenManager.getTokenSync()
            if (token == null) {
                return Result.failure(Exception("No authentication token found"))
            }

            val response = apiService.getProfile("Bearer $token")

            if (response.isSuccessful && response.body() != null) {
                val profile = response.body()!!

                // ✅ SAUVEGARDER linkedUserId du profil
                profile.linkedUserId?.let { linkedId ->
                    tokenManager.saveLinkedUserId(linkedId)
                    Log.d(TAG, "✅ Saved linkedUserId from profile: $linkedId")
                }

                Result.success(profile)
            } else {
                val statusCode = response.code()
                val errorMessage = response.errorBody()?.string() ?: "Failed to get profile"
                Log.e(TAG, "Get profile error ($statusCode): $errorMessage")

                if (statusCode == 404) {
                    Log.d(TAG, "Profile not found (404), creating minimal profile from stored data")
                    val userId = tokenManager.getUserIdSync()
                    val userType = tokenManager.getUserTypeSync()
                    val email = tokenManager.getUserEmailSync()
                    val linkedUserId = tokenManager.getLinkedUserIdSync() // ✅ Récupérer aussi
                    val minimalProfile = UserResponse(
                        _id = userId,
                        email = email,
                        firstName = null,
                        lastName = null,
                        phoneNumber = null,
                        userType = userType ?: "companion",
                        linkedUserId = linkedUserId // ✅ Inclure dans minimal profile
                    )
                    return Result.success(minimalProfile)
                }

                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Get profile exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateProfile(
        firstName: String,
        lastName: String,
        email: String,
        phoneNumber: String
    ): Result<UserResponse> {
        return try {
            val token = tokenManager.getTokenSync()
            if (token == null) {
                return Result.failure(Exception("No authentication token found"))
            }
            
            val dto = UpdateProfileDto(firstName, lastName, email, phoneNumber)
            val response = apiService.updateProfile("Bearer $token", dto)
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = errorBody ?: "Failed to update profile"
                Log.e(TAG, "Update profile error: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Update profile exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun logout() {
        tokenManager.clear()
        
        // ✅ CORRECTION: Nettoyer les singletons WebSocket
        WebSocketManager.resetInstance()
        VoiceWebSocketClient.resetInstance()
    }

    suspend fun getToken(): String? {
        return tokenManager.getTokenSync()
    }

    suspend fun isLoggedIn(): Boolean {
        return tokenManager.getTokenSync() != null
    }

    // ✅ Fonction pour mettre à jour le profil blind
    suspend fun updateBlindProfile(
        blindUserId: String,
        firstName: String,
        lastName: String? = null,
        phoneNumber: String? = null,
        relation: String? = null,
        profileImage: String? = null
    ): Result<UserResponse> {
        return try {
            val token = tokenManager.getTokenSync()
            if (token == null) {
                return Result.failure(Exception("No authentication token found"))
            }

            val dto = UpdateBlindProfileDto(
                blindUserId = blindUserId,
                firstName = firstName,
                lastName = lastName,
                phoneNumber = phoneNumber,
                relation = relation,
                profileImage = profileImage
            )

            val response = apiService.updateBlindProfile("Bearer $token", dto)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = errorBody ?: "Failed to update blind profile"
                Log.e(TAG, "Update blind profile error: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Update blind profile exception: ${e.message}", e)
            Result.failure(e)
        }
    }
}

