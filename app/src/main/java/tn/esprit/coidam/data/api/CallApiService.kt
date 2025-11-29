package tn.esprit.coidam.data.api

import retrofit2.Response
import retrofit2.http.*
import tn.esprit.coidam.data.models.*
import tn.esprit.coidam.data.models.CallDto.AcceptCallDto
import tn.esprit.coidam.data.models.CallDto.EndCallDto
import tn.esprit.coidam.data.models.CallDto.RejectCallDto
import tn.esprit.coidam.data.models.CallDto.StartCallDto
import tn.esprit.coidam.data.models.CallResponses.AcceptCallResponse
import tn.esprit.coidam.data.models.CallResponses.AgoraCredentials
import tn.esprit.coidam.data.models.CallResponses.CallHistoryResponse
import tn.esprit.coidam.data.models.CallResponses.CallStatsResponse
import tn.esprit.coidam.data.models.CallResponses.StartCallResponse

interface CallApiService {

    @POST("video-call/start")
    suspend fun startCall(@Body dto: StartCallDto): Response<StartCallResponse>

    @POST("video-call/accept")
    suspend fun acceptCall(@Body dto: AcceptCallDto): Response<AcceptCallResponse>

    @POST("video-call/reject")
    suspend fun rejectCall(@Body dto: RejectCallDto): Response<Call>

    @POST("video-call/end")
    suspend fun endCall(@Body dto: EndCallDto): Response<Call>

    @GET("video-call/{id}")
    suspend fun getCall(@Path("id") callId: String): Response<Call>

    @GET("video-call/history/{userId}")
    suspend fun getCallHistory(
        @Path("userId") userId: String,
        @Query("userType") userType: String,
        @Query("limit") limit: Int = 20,
        @Query("skip") skip: Int = 0
    ): Response<CallHistoryResponse>

    @GET("video-call/active/{userId}")
    suspend fun getActiveCalls(
        @Path("userId") userId: String,
        @Query("userType") userType: String
    ): Response<List<Call>>

    @POST("video-call/{id}/regenerate-token")
    suspend fun regenerateToken(@Path("id") callId: String): Response<AgoraCredentials>

    @PATCH("video-call/{id}/missed")
    suspend fun markAsMissed(@Path("id") callId: String): Response<Call>

    @GET("video-call/stats/{userId}")
    suspend fun getCallStats(
        @Path("userId") userId: String,
        @Query("userType") userType: String
    ): Response<CallStatsResponse>
}

// Ajoutez dans ApiClient.kt :
// val callApiService: CallApiService = retrofit.create(CallApiService::class.java)