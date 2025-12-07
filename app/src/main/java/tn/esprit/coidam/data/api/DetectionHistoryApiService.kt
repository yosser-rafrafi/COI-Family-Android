package tn.esprit.coidam.data.api

import retrofit2.Response
import retrofit2.http.*
import tn.esprit.coidam.data.models.*
import tn.esprit.coidam.data.models.DetectionHistory.DetectionHistory
import tn.esprit.coidam.data.models.DetectionHistory.DetectionStatistics

interface DetectionHistoryApiService {


    // ✅ RÉCUPÉRER TOUT L'HI
    // STORIQUE
    @GET("detection-history")
    suspend fun getAll(
        @Header("Authorization") token: String,
        @Query("limit") limit: Int = 50,
        @Query("skip") skip: Int = 0
    ): Response<List<DetectionHistory>>

    // ✅ RÉCUPÉRER UNE DÉTECTION SPÉCIFIQUE
    @GET("detection-history/{id}")
    suspend fun getOne(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<DetectionHistory>

    // ✅ OBTENIR LES STATISTIQUES
    @GET("detection-history/statistics")
    suspend fun getStatistics(
        @Header("Authorization") token: String
    ): Response<DetectionStatistics>

    // ✅ SUPPRIMER UNE DÉTECTION
    @DELETE("detection-history/{id}")
    suspend fun delete(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<Map<String, Any>>
}