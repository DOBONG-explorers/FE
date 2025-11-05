package kr.ac.duksung.dobongzip.data.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * RetrofitProvider
 * - BASE_URL: 백엔드 베이스 주소 (운영/개발에 맞게 교체)
 * - GsonConverterFactory 사용
 * - DobongApi: 상세 API (GET /api/v1/map/getPlaceDetail?placeId=)
 * - PlacesApi: 도봉 명소 목록 API (GET /api/v1/places/dobong)
 * - PlacesLikeApi: 좋아요/좋아요 취소
 */
object RetrofitProvider {

    private const val BASE_URL = "https://dobongzip.com/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()


    val dobongApi: DobongApi by lazy {
        retrofit.create(DobongApi::class.java)
    }

    val placesApi: PlacesApi by lazy {
        retrofit.create(PlacesApi::class.java)
    }

    // RetrofitProvider.kt
    val placeLikeApi: PlaceLikeApi by lazy {
        retrofit.create(PlaceLikeApi::class.java)
    }

}
