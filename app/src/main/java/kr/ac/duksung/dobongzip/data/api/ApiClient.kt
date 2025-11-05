// kr/ac/duksung/dobongzip/data/api/ApiClient.kt
package kr.ac.duksung.dobongzip.data.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.UUID
import java.util.concurrent.TimeUnit
import android.util.Log
import kr.ac.duksung.dobongzip.data.auth.AuthSession

object ApiClient {
    private const val BASE_URL = "https://dobongzip.com/"
    private const val TAG = "ApiClient"

    // ─ BODY 로깅 ─
    private val logger = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
        redactHeader("Authorization")
    }

    // ─ 요청 ID 헤더 (서버/클라 로그 상관관계) ─
    private fun requestIdInterceptor(kind: String) = Interceptor { chain ->
        val reqId = UUID.randomUUID().toString()
        val original = chain.request()
        val newReq = original.newBuilder()
            .addHeader("X-Debug-Request-Id", reqId)
            .build()
        Log.d(TAG, "[$kind] ${original.method} ${original.url}  X-Debug-Request-Id=$reqId")
        chain.proceed(newReq)
    }

    // ─ Authorization 헤더 (인증 클라 전용) ─
    private fun authHeaderInterceptor() = Interceptor { chain ->
        val original = chain.request()
        val builder = original.newBuilder()
        val token = AuthSession.getToken()
        if (!token.isNullOrBlank()) {
            builder.addHeader("Authorization", "Bearer $token")
        }
        val req = builder.build()
        Log.d(TAG, "[AUTH] ${req.method} ${req.url}  hasAuthHeader=${!token.isNullOrBlank()}")
        chain.proceed(req)
    }

    // ─ OkHttp (인증 필요) ─
    private fun buildOkHttp(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(requestIdInterceptor("AUTH"))
            .addInterceptor(authHeaderInterceptor())
            .addInterceptor(logger)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()

    // ─ OkHttp (인증 불필요: 로그인/비번 재설정 등) ─
    private fun buildOkHttpPublic(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(requestIdInterceptor("PUBLIC"))
            .addInterceptor(logger) // Authorization 미첨부
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()

    // ─ Retrofit (인증 필요) ─
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

    // ─ Retrofit (인증 불필요) ─
    @Volatile private var _retrofitPublic: Retrofit? = null
    val retrofitPublic: Retrofit get() = _retrofitPublic ?: rebuildPublic()

    fun rebuildPublic(): Retrofit {
        val client = buildOkHttpPublic()
        val r = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        _retrofitPublic = r
        return r
    }

    // ─ Helpers ─
    inline fun <reified T> create(): T = retrofit.create(T::class.java)
    inline fun <reified T> createPublic(): T = retrofitPublic.create(T::class.java)

    // ─ Services (인증 필요) ─
    val myPageService: MyPageService get() = create()
    val authService: AuthService get() = create()

    // ─ Services (인증 불필요) ─
    val authServicePublic: AuthService get() = createPublic()
    val passwordServicePublic: PasswordService get() = createPublic()
}
