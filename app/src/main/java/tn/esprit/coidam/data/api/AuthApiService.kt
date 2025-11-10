package tn.esprit.coidam.data.api

import retrofit2.Response
import retrofit2.http.*
import tn.esprit.coidam.data.models.*

interface AuthApiService {
    @POST("auth/signup")
    suspend fun signUp(@Body dto: SignUpDto): Response<SignUpResponse>

    @POST("auth/signin")
    suspend fun signIn(@Body dto: SignInDto): Response<AuthResponse>

    @POST("auth/login-as")
    suspend fun loginAs(@Body dto: LoginAsDto): Response<AuthResponse>

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body dto: ForgotPasswordDto): Response<Map<String, Any>>

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body dto: ResetPasswordDto): Response<Map<String, Any>>

    @PUT("auth/update-password")
    suspend fun updatePassword(
        @Header("Authorization") token: String,
        @Body dto: UpdatePasswordDto
    ): Response<Map<String, Any>>

    @GET("auth/me")
    suspend fun getProfile(@Header("Authorization") token: String): Response<ProfileResponse>

    @PUT("auth/profile")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Body dto: UpdateProfileDto
    ): Response<ProfileResponse>
}

