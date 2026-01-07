package tn.esprit.coidam.data.api

import retrofit2.Response
import retrofit2.http.*
import tn.esprit.coidam.data.models.*
import tn.esprit.coidam.data.models.AuthDto.ForgotPasswordDto
import tn.esprit.coidam.data.models.AuthDto.LoginAsDto
import tn.esprit.coidam.data.models.AuthDto.ResetPasswordDto
import tn.esprit.coidam.data.models.AuthDto.SignInDto
import tn.esprit.coidam.data.models.AuthDto.SignUpDto
import tn.esprit.coidam.data.models.AuthDto.UpdatePasswordDto
import tn.esprit.coidam.data.models.AuthDto.UpdateProfileDto
import tn.esprit.coidam.data.models.AuthDto.UpdateBlindProfileDto

interface AuthApiService {
    @POST("auth/signup")
    suspend fun signUp(@Body dto: SignUpDto): Response<SignUpResponse>

    @POST("auth/google-signin")
    suspend fun signInWithGoogle(
        @Body dto: Map<String, String> // on envoie { "idToken": "..." }
    ): Response<AuthResponse>

    @POST("auth/signin")
    suspend fun signIn(@Body dto: SignInDto): Response<AuthResponse>
    // Ajoute ce GET pour tester le réseau

    @GET("auth/signin")
    suspend fun testGet(): Response<String>

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
    suspend fun getProfile(@Header("Authorization") token: String): Response<UserResponse>

    @PUT("auth/profile")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Body dto: UpdateProfileDto
    ): Response<UserResponse>

    @PUT("auth/blind/profile")
    suspend fun updateBlindProfile(
        @Header("Authorization") token: String,
        @Body dto: UpdateBlindProfileDto
    ): Response<UserResponse>
}

