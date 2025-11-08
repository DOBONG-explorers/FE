package kr.ac.duksung.dobongzip.data.network

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


    val placesApi: PlacesApi by lazy {
        retrofit.create(PlacesApi::class.java)
    }

    val placeLikeApi: PlaceLikeApi by lazy {
        retrofit.create(PlaceLikeApi::class.java)
    }

    val heritageApi: HeritageApi by lazy {
        retrofit.create(HeritageApi::class.java)
    }

}
