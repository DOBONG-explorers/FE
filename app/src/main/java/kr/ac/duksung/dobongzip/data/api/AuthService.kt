// kr/ac/duksung/dobongzip/data/api/AuthService.kt
package kr.ac.duksung.dobongzip.data.api

import retrofit2.http.*

/** 공통 래퍼 */
data class CommonResponse<T>(
    val success: Boolean,
    val httpStatus: Int,
    val message: String,
    val data: T?
)

/** 회원가입 Step1 응답 데이터 */
data class SignupData(
    val accessToken: String?,
    val name: String?,
    val nickname: String?,
    val loginType: String?,       // "APP"
    val profileCompleted: Boolean
)

/** 회원가입 Step1 요청 */
data class SignupRequest(
    val email: String,
    val password: String,
    val phoneNumber: String
)

/** 프로필 등록(공통) 요청 */
data class ProfileRequest(
    val name: String,
    val nickname: String,
    val gender: String,   // "MALE" | "FEMALE" 등 서버 스펙대로
    val birth: String     // "YYYY-MM-DD"
)

interface AuthService {
    /** 앱 회원가입 */
    @POST("/api/v1/auth/signup")
    suspend fun signup(@Body body: SignupRequest): CommonResponse<SignupData>

    /** 회원가입 공통 (앱/소셜) - 프로필 입력 */
    @POST("/api/v1/auth/profile")
    suspend fun submitProfile(
        @Query("email") email: String,
        @Query("loginType") loginType: String = "APP",
        @Body body: ProfileRequest
    ): CommonResponse<String>
}
