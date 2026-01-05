package tn.esprit.coidam.data.repository

import android.content.Context
import android.util.Log
import tn.esprit.coidam.data.api.ApiClient
import tn.esprit.coidam.data.local.TokenManager
import tn.esprit.coidam.data.models.Battery
import tn.esprit.coidam.data.models.BatteryReportRequest
import tn.esprit.coidam.data.models.BatteryReportResponse
import tn.esprit.coidam.data.models.BatteryRecentResponse

class BatteryRepository(private val context: Context) {
    private val apiService = ApiClient.batteryApiService
    private val tokenManager = TokenManager(context)
    private val TAG = "BatteryRepository"

    suspend fun reportBattery(
        level: Int,
        isCharging: Boolean = false,
        note: String? = null
    ): Result<BatteryReportResponse> {
        return try {
            val token = tokenManager.getTokenSync()
            if (token.isNullOrEmpty()) {
                return Result.failure(Exception("No authentication token found"))
            }

            val request = BatteryReportRequest(level, isCharging, note)
            val response = apiService.reportBattery("Bearer $token", request)

            if (response.isSuccessful && response.body() != null) {
                val batteryResponse = response.body()!!
                Log.d(TAG, "Battery report successful: ${batteryResponse.saved.level}%")
                Result.success(batteryResponse)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = errorBody ?: "Failed to report battery level"
                Log.e(TAG, "Battery report error: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Battery report exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getRecentBatteryReports(blindUserId: String): Result<List<Battery>> {
        return try {
            val token = tokenManager.getTokenSync()
            if (token.isNullOrEmpty()) {
                return Result.failure(Exception("No authentication token found"))
            }

            val response = apiService.getRecentBatteryReports("Bearer $token", blindUserId)

            if (response.isSuccessful && response.body() != null) {
                val batteryResponse = response.body()!!
                Log.d(TAG, "Retrieved ${batteryResponse.results.size} battery reports")
                Result.success(batteryResponse.results)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = errorBody ?: "Failed to get battery reports"
                Log.e(TAG, "Battery reports error: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Battery reports exception: ${e.message}", e)
            Result.failure(e)
        }
    }
}
