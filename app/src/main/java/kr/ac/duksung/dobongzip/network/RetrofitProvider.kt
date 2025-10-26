package kr.ac.duksung.dobongzip.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitProvider {
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    val api: DobongApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://dobongzip.com/") // 백엔드 기본 주소
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(DobongApi::class.java)
    }
}
