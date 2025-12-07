package tn.esprit.coidam.data.api

import retrofit2.Response
import retrofit2.http.*
import tn.esprit.coidam.data.models.BatteryReportRequest
import tn.esprit.coidam.data.models.BatteryReportResponse
import tn.esprit.coidam.data.models.BatteryRecentResponse

interface BatteryApiService {

    @POST("battery/report")
    suspend fun reportBattery(
        @Header("Authorization") token: String,
        @Body request: BatteryReportRequest
    ): Response<BatteryReportResponse>

    @GET("battery/recent/{blindUserId}")
    suspend fun getRecentBatteryReports(
        @Header("Authorization") token: String,
        @Path("blindUserId") blindUserId: String
    ): Response<BatteryRecentResponse>
}