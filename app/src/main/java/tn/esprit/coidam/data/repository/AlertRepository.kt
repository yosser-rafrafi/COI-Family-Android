// data/repository/AlertRepository.kt
package tn.esprit.coidam.data.repository

import android.content.Context
import android.util.Log
import tn.esprit.coidam.data.api.AlertApiService
import tn.esprit.coidam.data.api.ApiClient
import tn.esprit.coidam.data.models.*
import tn.esprit.coidam.data.models.Alerts.AlertStats
import tn.esprit.coidam.data.models.Alerts.CreateAlertRequest
import tn.esprit.coidam.data.models.Enums.AlertStatus
import tn.esprit.coidam.data.models.Enums.AlertType

class AlertRepository(private val context: Context) {
    private val apiService: AlertApiService = ApiClient.alertApiService

    // MARK: - Create Alert
    suspend fun createAlert(
        blindUserId: String,
        companionId: String,
        type: AlertType,
        location: Location? = null
    ): Result<Alert> {
        return try {
            val request = CreateAlertRequest(
                blindUserId = blindUserId,
                companionId = companionId,
                type = type.value,
                location = location
            )

            Log.d("AlertRepository", "Creating alert: $request")

            val response = apiService.createAlert(request)

            if (response.isSuccessful && response.body() != null) {
                Log.d("AlertRepository", "✅ Alert created: ${response.body()?.id}")
                Result.success(response.body()!!)
            } else {
                val errorMsg = "Failed to create alert: ${response.code()}"
                Log.e("AlertRepository", "❌ $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("AlertRepository", "❌ Error creating alert", e)
            Result.failure(e)
        }
    }

    // MARK: - Get All Alerts
    suspend fun getAllAlerts(): Result<List<Alert>> {
        return try {
            val response = apiService.getAllAlerts()

            if (response.isSuccessful && response.body() != null) {
                Log.d("AlertRepository", "✅ Loaded ${response.body()?.size} alerts")
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to load alerts"))
            }
        } catch (e: Exception) {
            Log.e("AlertRepository", "❌ Error loading alerts", e)
            Result.failure(e)
        }
    }

    // MARK: - Get Alerts by Companion
    suspend fun getAlertsByCompanion(
        companionId: String,
        status: AlertStatus? = null
    ): Result<List<Alert>> {
        return try {
            val response = apiService.getAlertsByCompanion(
                companionId = companionId,
                status = status?.value
            )

            if (response.isSuccessful && response.body() != null) {
                Log.d("AlertRepository", "✅ Loaded ${response.body()?.size} alerts for companion")
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to load companion alerts"))
            }
        } catch (e: Exception) {
            Log.e("AlertRepository", "❌ Error loading companion alerts", e)
            Result.failure(e)
        }
    }

    // MARK: - Get Alerts by Blind User
    suspend fun getAlertsByBlindUser(blindUserId: String): Result<List<Alert>> {
        return try {
            val response = apiService.getAlertsByBlindUser(blindUserId)

            if (response.isSuccessful && response.body() != null) {
                Log.d("AlertRepository", "✅ Loaded ${response.body()?.size} alerts for blind user")
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to load blind user alerts"))
            }
        } catch (e: Exception) {
            Log.e("AlertRepository", "❌ Error loading blind user alerts", e)
            Result.failure(e)
        }
    }

    // MARK: - Get One Alert
    suspend fun getAlert(id: String): Result<Alert> {
        return try {
            val response = apiService.getAlert(id)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Alert not found"))
            }
        } catch (e: Exception) {
            Log.e("AlertRepository", "❌ Error loading alert", e)
            Result.failure(e)
        }
    }

    // MARK: - Acknowledge Alert
    suspend fun acknowledgeAlert(id: String): Result<Alert> {
        return try {
            val response = apiService.acknowledgeAlert(id)

            if (response.isSuccessful && response.body() != null) {
                Log.d("AlertRepository", "✅ Alert acknowledged: $id")
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to acknowledge alert"))
            }
        } catch (e: Exception) {
            Log.e("AlertRepository", "❌ Error acknowledging alert", e)
            Result.failure(e)
        }
    }

    // MARK: - Resolve Alert
    suspend fun resolveAlert(id: String): Result<Alert> {
        return try {
            val response = apiService.resolveAlert(id)

            if (response.isSuccessful && response.body() != null) {
                Log.d("AlertRepository", "✅ Alert resolved: $id")
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to resolve alert"))
            }
        } catch (e: Exception) {
            Log.e("AlertRepository", "❌ Error resolving alert", e)
            Result.failure(e)
        }
    }

    // MARK: - Delete Alert
    suspend fun deleteAlert(id: String): Result<Unit> {
        return try {
            val response = apiService.deleteAlert(id)

            if (response.isSuccessful) {
                Log.d("AlertRepository", "✅ Alert deleted: $id")
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete alert"))
            }
        } catch (e: Exception) {
            Log.e("AlertRepository", "❌ Error deleting alert", e)
            Result.failure(e)
        }
    }

    // MARK: - Get Stats
    suspend fun getStats(companionId: String): Result<AlertStats> {
        return try {
            val response = apiService.getStats(companionId)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to load stats"))
            }
        } catch (e: Exception) {
            Log.e("AlertRepository", "❌ Error loading stats", e)
            Result.failure(e)
        }
    }
}