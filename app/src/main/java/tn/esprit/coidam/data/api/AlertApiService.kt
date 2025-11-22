package tn.esprit.coidam.data.api

import retrofit2.Response
import retrofit2.http.*
import tn.esprit.coidam.data.models.*
import tn.esprit.coidam.data.models.Alerts.AlertStats
import tn.esprit.coidam.data.models.Alerts.CreateAlertRequest

interface AlertApiService {

    @POST("alerts")
    suspend fun createAlert(
        @Body request: CreateAlertRequest
    ): Response<Alert>

    @GET("alerts")
    suspend fun getAllAlerts(): Response<List<Alert>>

    @GET("alerts/{id}")
    suspend fun getAlert(@Path("id") id: String): Response<Alert>

    @GET("alerts/companion/{companionId}")
    suspend fun getAlertsByCompanion(
        @Path("companionId") companionId: String,
        @Query("status") status: String? = null
    ): Response<List<Alert>>

    @GET("alerts/blind/{blindUserId}")
    suspend fun getAlertsByBlindUser(
        @Path("blindUserId") blindUserId: String
    ): Response<List<Alert>>

    @PATCH("alerts/{id}/acknowledge")
    suspend fun acknowledgeAlert(@Path("id") id: String): Response<Alert>

    @PATCH("alerts/{id}/resolve")
    suspend fun resolveAlert(@Path("id") id: String): Response<Alert>

    @DELETE("alerts/{id}")
    suspend fun deleteAlert(@Path("id") id: String): Response<Unit>

    @GET("alerts/stats/{companionId}")
    suspend fun getStats(@Path("companionId") companionId: String): Response<AlertStats>
}