package tn.esprit.coidam.data.repository

import android.content.Context
import android.util.Log
import tn.esprit.coidam.data.api.ApiClient
import tn.esprit.coidam.data.local.TokenManager
import tn.esprit.coidam.data.models.*

class AuthRepository(private val context: Context) {
    private val apiService = ApiClient.authApiService
    private val tokenManager = TokenManager(context)
    private val TAG = "AuthRepository"

    suspend fun signUp(
        email: String,
        password: String
    ): Result<SignUpResponse> {
        return try {
            val dto = SignUpDto(email, password, "companion")
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

    suspend fun loginAs(userId: String, userType: String): Result<AuthResponse> {
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

    suspend fun forgotPassword(email: String): Result<String> {
        return try {
            val dto = ForgotPasswordDto(email)
            val response = apiService.forgotPassword(dto)

            if (response.isSuccessful) {
                // Explicitly cast the value from the map to a String
                val message = response.body()?.get("message") as? String ?: "Password reset email sent"
                Result.success(message)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to send reset email"
                Log.e(TAG, "Forgot password error: $errorMessage")
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

    suspend fun getProfile(): Result<ProfileResponse> {
        return try {
            val token = tokenManager.getTokenSync()
            if (token == null) {
                return Result.failure(Exception("No authentication token found"))
            }
            
            val response = apiService.getProfile("Bearer $token")
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to get profile"
                Log.e(TAG, "Get profile error: $errorMessage")
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
        university: String,
        phoneNumber: String
    ): Result<ProfileResponse> {
        return try {
            val token = tokenManager.getTokenSync()
            if (token == null) {
                return Result.failure(Exception("No authentication token found"))
            }
            
            val dto = UpdateProfileDto(firstName, lastName, email, university, phoneNumber)
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
    }

    suspend fun getToken(): String? {
        return tokenManager.getTokenSync()
    }

    suspend fun isLoggedIn(): Boolean {
        return tokenManager.getTokenSync() != null
    }
}

