package tn.esprit.coidam.data.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query
import tn.esprit.coidam.data.models.Photo

interface PhotoApiService {

    @GET("photos")
    suspend fun getPhotos(
        @Header("Authorization") token: String,
        @Query("userId") userId: String
    ): Response<List<Photo>>

    @Multipart
    @POST("photos")
    suspend fun uploadPhoto(
        @Header("Authorization") token: String,
        @Part file: MultipartBody.Part,
        @Part("caption") caption: RequestBody?,
        @Part("ownerId") ownerId: RequestBody?,
        @Part("lat") lat: RequestBody?,
        @Part("lng") lng: RequestBody?,
        @Part("isPublic") isPublic: RequestBody?
    ): Response<Photo>
}
