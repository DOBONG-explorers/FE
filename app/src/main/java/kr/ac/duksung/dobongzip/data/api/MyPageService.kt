package kr.ac.duksung.dobongzip.data.api

import okhttp3.MultipartBody
import retrofit2.http.*

// --------------------
// 📦 데이터 모델 (DTO)
// --------------------
data class MyPageProfile(
    val nickname: String?,
    val birth: String?,
    val email: String?
)

data class MyPageProfilePatchReq(
    val nickname: String?,
    val birth: String?,
    val email: String?
)

// ✅ 이미지 업로드 관련 DTO (서버 응답과 일치)
data class UploadStage1Data(   // 1단계: 업로드 성공 후 반환되는 objectKey
    val objectKey: String
)

data class ImageObjectKey(     // 2단계: objectKey 전달용
    val objectKey: String
)

data class ImageUrlData(       // 최종 반영 후 서버가 반환하는 이미지 URL
    val imageUrl: String?
)

// ✅ 비밀번호 변경 요청
data class PasswordChangeReq(
    val currentPassword: String,
    val newPassword: String,
    val confirmPassword: String
)

// --------------------
// 🌐 Retrofit API 정의
// --------------------
interface MyPageService {

    /** ✅ 개인정보 조회 */
    @GET("/api/v1/mypage/profile")
    suspend fun getProfile(): CommonResponse<MyPageProfile>

    /** ✅ 개인정보 수정 */
    @PATCH("/api/v1/mypage/profile")
    suspend fun patchProfile(
        @Body req: MyPageProfilePatchReq
    ): CommonResponse<String>

    /** ✅ 프로필 이미지 조회 */
    @GET("/api/v1/mypage/profile-image")
    suspend fun getProfileImage(): CommonResponse<ImageUrlData>

    /** ✅ 프로필 이미지 삭제 */
    @DELETE("/api/v1/mypage/profile-image")
    suspend fun deleteProfileImage(): CommonResponse<String>

    /** ✅ 업로드 1단계: 임시 업로드 → objectKey 리턴 */
    @Multipart
    @POST("/api/v1/mypage/profile-image/upload")
    suspend fun uploadProfileImageStage1(
        @Part file: MultipartBody.Part
    ): CommonResponse<UploadStage1Data>

    /** ✅ 업로드 2단계: objectKey 최종 반영 → imageUrl 리턴 */
    @POST("/api/v1/mypage/profile-image/finalize")
    suspend fun finalizeProfileImage(
        @Body body: ImageObjectKey
    ): CommonResponse<ImageUrlData>

    /** ✅ 비밀번호 변경 */
    @PATCH("/api/v1/mypage/password")
    suspend fun changePassword(
        @Body req: PasswordChangeReq
    ): CommonResponse<String>
}
