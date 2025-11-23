package tn.esprit.coidam.data.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    // TODO: Change this to your backend URL
   //
    const val BASE_URL = "http://192.168.0.148:3001/"
    // private const val BASE_URL = "http://10.0.2.2:3000/" // For Android Emulator
    // For physical device, use your computer's IP: "http://192.168.x.x:3000/"

    //private const val BASE_URL = "http://192.168.0.148:3000/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val authApiService: AuthApiService = retrofit.create(AuthApiService::class.java)
    val knownPersonApiService: KnownPersonApiService = retrofit.create(KnownPersonApiService::class.java)

    val alertApiService: AlertApiService = retrofit.create(AlertApiService::class.java)
}

