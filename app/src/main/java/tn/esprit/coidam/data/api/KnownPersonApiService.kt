package tn.esprit.coidam.data.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*
import tn.esprit.coidam.data.models.KnownPerson
import tn.esprit.coidam.data.models.KnownPersonDto.CreateKnownPersonDto
import tn.esprit.coidam.data.models.KnownPersonDto.UpdateKnownPersonDto

interface KnownPersonApiService {
    @POST("known-person")
    suspend fun create(
        @Header("Authorization") token: String,
        @Body dto: CreateKnownPersonDto
    ): Response<KnownPerson>

    @Multipart
    @POST("known-person")
    suspend fun createWithImage(
        @Header("Authorization") token: String,
        @Part("name") name: RequestBody,
        @Part("relation") relation: RequestBody?,
        @Part("phone") phone: RequestBody?,
        @Part image: MultipartBody.Part
    ): Response<KnownPerson>

    @Multipart
    @POST("known-person/upload")
    suspend fun uploadImage(
        @Header("Authorization") token: String,
        @Part image: MultipartBody.Part
    ): Response<Map<String, String>>

    @GET("known-person")
    suspend fun findAll(
        @Header("Authorization") token: String
    ): Response<List<KnownPerson>>

    @GET("known-person/{id}")
    suspend fun findOne(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<KnownPerson>

    @PATCH("known-person/{id}")
    suspend fun update(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Body dto: UpdateKnownPersonDto
    ): Response<KnownPerson>

    @DELETE("known-person/{id}")
    suspend fun remove(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<Map<String, String>>
}

