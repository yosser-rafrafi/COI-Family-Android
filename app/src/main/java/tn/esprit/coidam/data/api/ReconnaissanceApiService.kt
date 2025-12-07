package tn.esprit.coidam.data.api

import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

data class DetectionResponse(
    val image: String? = null,
    val imagePath: String? = null,
    val detections: List<Detection>,
    val count: Int
)

data class Detection(
    @SerializedName("class") val `class`: Int,
    val className: String,
    val confidence: Double,
    val bbox: BoundingBox
)

data class BoundingBox(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
    val x1: Double,
    val y1: Double,
    val x2: Double,
    val y2: Double
)

data class DetectFromPathRequest(
    val imagePath: String,
    val minConfidence: Double? = null
)

data class ClassesResponse(
    val classes: List<String>
)

interface ReconnaissanceApiService {
    
    @Multipart
    @POST("reconnaissance/detect")
    suspend fun detectObjects(
        @Part image: MultipartBody.Part,
        @Part("minConfidence") minConfidence: RequestBody?
    ): Response<DetectionResponse>
    
    @POST("reconnaissance/detect-from-path")
    suspend fun detectObjectsFromPath(
        @Body body: DetectFromPathRequest
    ): Response<DetectionResponse>
    
    @GET("reconnaissance/classes")
    suspend fun getAvailableClasses(): Response<ClassesResponse>
}

