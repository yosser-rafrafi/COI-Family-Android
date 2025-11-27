package tn.esprit.coidam.data.repository

import android.content.Context
import android.util.Log
import tn.esprit.coidam.data.api.ApiClient
import tn.esprit.coidam.data.local.TokenManager
import tn.esprit.coidam.data.models.*
import tn.esprit.coidam.data.models.CallDto.AcceptCallDto
import tn.esprit.coidam.data.models.CallDto.EndCallDto
import tn.esprit.coidam.data.models.CallDto.RejectCallDto
import tn.esprit.coidam.data.models.CallDto.StartCallDto
import tn.esprit.coidam.data.models.CallResponses.AgoraCredentials
import tn.esprit.coidam.data.models.CallResponses.CallHistoryResponse
import tn.esprit.coidam.data.models.CallResponses.CallStatsResponse
import tn.esprit.coidam.data.models.CallResponses.StartCallResponse

class CallRepository(private val context: Context) {
    private val apiService = ApiClient.callApiService
    private val tokenManager = TokenManager(context)
    private val TAG = "CallRepository"

    /**
     * Démarre un nouvel appel
     */
    suspend fun startCall(
        blindUserId: String,
        companionId: String,
        callType: String = "video",
        initiatedBy: String = "blind"
    ): Result<StartCallResponse> {
        return try {
            val dto = StartCallDto(
                blindUserId = blindUserId,
                companionId = companionId,
                callType = callType,
                initiatedBy = initiatedBy
            )

            val response = apiService.startCall(dto)

            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "✅ Call started successfully")
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = errorBody ?: "Failed to start call"
                Log.e(TAG, "❌ Start call error: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Start call exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Accepte un appel
     */
    suspend fun acceptCall(callId: String): Result<Call> {
        return try {
            val dto = AcceptCallDto(callId)
            val response = apiService.acceptCall(dto)

            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "✅ Call accepted")
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to accept call"
                Log.e(TAG, "❌ Accept call error: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Accept call exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Rejette un appel
     */
    suspend fun rejectCall(callId: String, reason: String? = null): Result<Call> {
        return try {
            val dto = RejectCallDto(callId, reason)
            val response = apiService.rejectCall(dto)

            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "✅ Call rejected")
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to reject call"
                Log.e(TAG, "❌ Reject call error: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Reject call exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Termine un appel
     */
    suspend fun endCall(
        callId: String,
        endedBy: String,
        endReason: String? = null
    ): Result<Call> {
        return try {
            val dto = EndCallDto(callId, endedBy, endReason)
            val response = apiService.endCall(dto)

            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "✅ Call ended")
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to end call"
                Log.e(TAG, "❌ End call error: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ End call exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Récupère un appel par ID
     */
    suspend fun getCall(callId: String): Result<Call> {
        return try {
            val response = apiService.getCall(callId)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to get call"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Get call exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Récupère l'historique des appels
     */
    suspend fun getCallHistory(
        userId: String,
        userType: String,
        limit: Int = 20,
        skip: Int = 0
    ): Result<CallHistoryResponse> {
        return try {
            val response = apiService.getCallHistory(userId, userType, limit, skip)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to get call history"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Get call history exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Récupère les appels actifs
     */
    suspend fun getActiveCalls(userId: String, userType: String): Result<List<Call>> {
        return try {
            val response = apiService.getActiveCalls(userId, userType)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to get active calls"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Get active calls exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Régénère un token Agora
     */
    suspend fun regenerateToken(callId: String): Result<AgoraCredentials> {
        return try {
            val response = apiService.regenerateToken(callId)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to regenerate token"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Regenerate token exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Marque un appel comme manqué
     */
    suspend fun markAsMissed(callId: String): Result<Call> {
        return try {
            val response = apiService.markAsMissed(callId)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to mark as missed"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Mark as missed exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Récupère les statistiques d'appels
     */
    suspend fun getCallStats(userId: String, userType: String): Result<CallStatsResponse> {
        return try {
            val response = apiService.getCallStats(userId, userType)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to get call stats"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Get call stats exception: ${e.message}", e)
            Result.failure(e)
        }
    }
}