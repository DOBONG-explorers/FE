// kr/ac/duksung/dobongzip/data/api/ApiClient.kt
package kr.ac.duksung.dobongzip.data.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import kr.ac.duksung.dobongzip.data.auth.AuthSession

object ApiClient {
    private const val BASE_URL = "https://dobongzip.com/"

    private val logger = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
        redactHeader("Authorization")
    }

    private fun authHeaderInterceptor() = Interceptor { chain ->
        val original = chain.request()
        val builder = original.newBuilder()

        AuthSession.getToken()?.takeIf { it.isNotBlank() }?.let { token ->
            builder.addHeader("Authorization", "Bearer $token")
        }
        chain.proceed(builder.build())
    }

    private fun buildOkHttp(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authHeaderInterceptor())
            .addInterceptor(logger)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()

    @Volatile private var _retrofit: Retrofit? = null
    val retrofit: Retrofit get() = _retrofit ?: rebuild()

    fun rebuild(): Retrofit {
        val client = buildOkHttp()
        val r = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        _retrofit = r
        return r
    }

    val myPageService: MyPageService get() = retrofit.create(MyPageService::class.java)
    val authService: AuthService get() = retrofit.create(AuthService::class.java)
}
