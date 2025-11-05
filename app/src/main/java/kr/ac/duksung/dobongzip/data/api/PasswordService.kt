package kr.ac.duksung.dobongzip.data.api

import retrofit2.http.Body
import retrofit2.http.POST

data class ResetPasswordReq(
    val email: String,
    val newPassword: String,
    val confirmPassword: String
)

interface PasswordService {
    @POST("/api/v1/auth/password/reset")
    suspend fun resetPassword(@Body req: ResetPasswordReq): CommonResponse<String>
}
