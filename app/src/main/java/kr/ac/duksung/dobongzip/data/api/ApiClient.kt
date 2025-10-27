// kr/ac/duksung/dobongzip/data/api/ApiClient.kt
package kr.ac.duksung.dobongzip.data.api

import kr.ac.duksung.dobongzip.data.auth.TokenHolder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val BASE_URL = "https://dobongzip.com/"

    private val logger = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val authHeaderInterceptor = Interceptor { chain ->
        val original = chain.request()
        val builder = original.newBuilder()
        TokenHolder.accessToken?.let { token ->
            if (token.isNotBlank()) builder.addHeader("Authorization", "Bearer $token")
        }
        chain.proceed(builder.build())
    }

    private val okHttp = OkHttpClient.Builder()
        .addInterceptor(authHeaderInterceptor) // ← 추가
        .addInterceptor(logger)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val authService: AuthService by lazy { retrofit.create(AuthService::class.java) }
}
