// kr/ac/duksung/dobongzip/data/api/AuthService.kt
package kr.ac.duksung.dobongzip.data.api

import okhttp3.MultipartBody
import retrofit2.http.*

/** 공통 래퍼 */
data class CommonResponse<T>(
    val success: Boolean,
    val httpStatus: Int,
    val message: String,
    val data: T?
)

/** 회원가입/로그인 공통 응답 데이터 */
data class SignupData(
    val accessToken: String?,
    val name: String?,
    val nickname: String?,
    val loginType: String?,       // "APP" | "KAKAO" | "GOOGLE"
    val profileCompleted: Boolean,
    val token: String? = null
) {
    /** 서버가 accessToken 또는 token 어느 쪽을 주든 JWT를 한 줄로 꺼내기 위한 헬퍼 */
    fun jwt(): String? = accessToken ?: token
}

/** 회원가입 Step1 요청 */
data class SignupRequest(
    val email: String,
    val password: String,
    val phoneNumber: String
)

/** ✅ 로그인 요청 */
data class LoginRequest(
    val email: String,
    val password: String
)

/** 프로필 등록(공통) 요청 */
data class ProfileRequest(
    val name: String,
    val nickname: String,
    val gender: String,   // "MALE" | "FEMALE" 등 서버 스펙대로
    val birth: String     // "YYYY-MM-DD"
)

/** ✅ OIDC 요청 바디: Kakao/Google에서 받은 id_token을 서버로 보낼 때 사용 */
data class IdTokenRequest(
    val idToken: String,
    val nonce: String? = null
)

interface AuthService {

    /** 앱 회원가입 */
    @POST("/api/v1/auth/signup")
    suspend fun signup(@Body body: SignupRequest): CommonResponse<SignupData>

    /** ✅ 일반 로그인 */
    @POST("/api/v1/auth/login")
    suspend fun login(@Body body: LoginRequest): CommonResponse<SignupData>

    /** 🔐 Kakao OIDC 로그인 (id_token 필수) */
    @POST("/api/v1/auth/kakao/oidc")
    suspend fun kakaoOidc(@Body body: IdTokenRequest): CommonResponse<SignupData>

    /** 🔐 Google OIDC 로그인 (id_token 필수, serverClientId=웹 클라ID로 발급) */
    @POST("/api/v1/auth/google/oidc")
    suspend fun googleOidc(@Body body: IdTokenRequest): CommonResponse<SignupData>

    /** 회원가입 공통 (앱/소셜) - 프로필 입력 */
    @POST("/api/v1/auth/profile")
    suspend fun submitProfile(
        @Query("email") email: String,
        @Query("loginType") loginType: String = "APP",
        @Body body: ProfileRequest
    ): CommonResponse<String>

    /** 프로필 이미지 업로드 (멀티파트) */
    @Multipart
    @POST("/api/v1/auth/profile/image")
    suspend fun uploadProfileImage(
        @Part file: MultipartBody.Part
    ): CommonResponse<String>
}
