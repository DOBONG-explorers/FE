package kr.ac.duksung.dobongzip.data.network

import kr.ac.duksung.dobongzip.data.models.ApiResponse
import kr.ac.duksung.dobongzip.data.models.LikeCardDto
import retrofit2.http.*

data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val errorMessage: String?
)

data class LikeResponse(
    val success: Boolean,
    val message: String?
)

interface PlaceLikeApi {
    // 장소 좋아요 추가
    @POST("/api/v1/places/{placeId}/like")
    suspend fun like(
        @Path("placeId") placeId: String
    ): ApiResponse<LikeResponse>

    // 장소 좋아요 취소
    @DELETE("/api/v1/places/{placeId}/like")
    suspend fun unlike(
        @Path("placeId") placeId: String
    ): ApiResponse<LikeResponse>

    // 내가 좋아요한 목록 조회
    @GET("/api/v1/places/likes/me")
    suspend fun getMyLikes(
        @Query("size") size: Int = 30,
        @Query("order") order: String = "latest"
    ): ApiResponse<List<LikeCardDto>>
}
