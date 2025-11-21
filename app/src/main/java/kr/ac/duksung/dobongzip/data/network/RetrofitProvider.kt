package kr.ac.duksung.dobongzip.data.network

import android.util.Log
import kr.ac.duksung.dobongzip.data.auth.AuthSession
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitProvider {

    private const val BASE_URL = "https://dobongzip.com/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val builder = original.newBuilder()
        val token = AuthSession.getToken()
        if (!token.isNullOrBlank()) {
            builder.addHeader("Authorization", "Bearer $token")
            Log.d("RetrofitProvider", "Added auth token to ${original.url}")
        }
        chain.proceed(builder.build())
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
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


    val placesApi: PlacesApi by lazy {
        retrofit.create(PlacesApi::class.java)
    }

    val placeLikeApi: PlaceLikeApi by lazy {
        retrofit.create(PlaceLikeApi::class.java)
    }

    val placeReviewApi: PlaceReviewApi by lazy {
        retrofit.create(PlaceReviewApi::class.java)
    }

    val heritageApi: HeritageApi by lazy {
        retrofit.create(HeritageApi::class.java)
    }

    val noticeApi: NoticeApi by lazy {
        retrofit.create(NoticeApi::class.java)
    }

}
