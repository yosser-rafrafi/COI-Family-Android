package tn.esprit.coidam.data.api

import retrofit2.Response
import retrofit2.http.*
import tn.esprit.coidam.data.models.*
import tn.esprit.coidam.data.models.DetectionHistory.RecognizeRequest
import tn.esprit.coidam.data.models.FaceRecognition.ProcessAllResponse
import tn.esprit.coidam.data.models.FaceRecognition.RecognitionResponse

interface FaceRecognitionApiService {

    // ✅ VÉRIFIER LA SANTÉ DU SERVICE
    @GET("face-recognition/health")
    suspend fun checkHealth(
        @Header("Authorization") token: String
    ): Response<Map<String, String>>

    // ✅ TRAITER TOUTES LES PERSONNES CONNUES (extraire les encodings)
    @POST("face-recognition/process-all")
    suspend fun processAllKnownPersons(
        @Header("Authorization") token: String
    ): Response<ProcessAllResponse>

    // ✅ RECONNAÎTRE LES VISAGES DANS UNE IMAGE (avec sauvegarde dans l'historique)
    @POST("face-recognition/recognize-base64")
    suspend fun recognizeFaces(
        @Header("Authorization") token: String,
        @Body request: RecognizeRequest
    ): Response<RecognitionResponse>
}