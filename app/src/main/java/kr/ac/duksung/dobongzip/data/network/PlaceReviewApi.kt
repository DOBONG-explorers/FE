package kr.ac.duksung.dobongzip.data.network

import kr.ac.duksung.dobongzip.data.models.ApiResponse
import kr.ac.duksung.dobongzip.data.models.PlaceReviewDto
import kr.ac.duksung.dobongzip.data.models.PlaceReviewSummaryDto
import kr.ac.duksung.dobongzip.data.models.ReviewRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface PlaceReviewApi {

    @GET("/api/v1/places/{placeId}/reviews")
    suspend fun getReviews(
        @Path("placeId") placeId: String,
        @Query("limit") limit: Int? = null
    ): ApiResponse<PlaceReviewSummaryDto>

    @POST("/api/v1/places/{placeId}/reviews")
    suspend fun createReview(
        @Path("placeId") placeId: String,
        @Body request: ReviewRequest
    ): ApiResponse<PlaceReviewDto?>

    @PUT("/api/v1/places/{placeId}/reviews/{reviewId}")
    suspend fun updateReview(
        @Path("placeId") placeId: String,
        @Path("reviewId") reviewId: Long,
        @Body request: ReviewRequest
    ): ApiResponse<PlaceReviewDto?>

    @DELETE("/api/v1/places/{placeId}/reviews/{reviewId}")
    suspend fun deleteReview(
        @Path("placeId") placeId: String,
        @Path("reviewId") reviewId: Long
    ): ApiResponse<Unit>
}

