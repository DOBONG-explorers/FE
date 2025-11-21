package kr.ac.duksung.dobongzip.data.repository

import kr.ac.duksung.dobongzip.data.models.PlaceReviewSummaryDto
import kr.ac.duksung.dobongzip.data.models.ReviewRequest
import kr.ac.duksung.dobongzip.data.network.PlaceReviewApi
import kr.ac.duksung.dobongzip.data.network.RetrofitProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReviewRepository(
    private val api: PlaceReviewApi = RetrofitProvider.placeReviewApi
) {

    suspend fun getReviews(placeId: String, limit: Int? = null): PlaceReviewSummaryDto =
        withContext(Dispatchers.IO) {
            val response = api.getReviews(placeId, limit)
            if (response.success && response.data != null) {
                response.data
            } else {
                throw kotlin.IllegalStateException(response.message ?: "리뷰를 불러오지 못했습니다.")
            }
        }

    suspend fun createReview(placeId: String, rating: Double, text: String) =
        withContext(Dispatchers.IO) {
            val response = api.createReview(placeId, ReviewRequest(rating, text))
            if (!response.success) {
                throw kotlin.IllegalStateException(response.message ?: "리뷰 작성에 실패했습니다.")
            }
        }

    suspend fun updateReview(placeId: String, reviewId: Long, rating: Double, text: String) =
        withContext(Dispatchers.IO) {
            val response = api.updateReview(placeId, reviewId, ReviewRequest(rating, text))
            if (!response.success) {
                throw kotlin.IllegalStateException(response.message ?: "리뷰 수정에 실패했습니다.")
            }
        }

    suspend fun deleteReview(placeId: String, reviewId: Long) =
        withContext(Dispatchers.IO) {
            val response = api.deleteReview(placeId, reviewId)
            if (!response.success) {
                throw kotlin.IllegalStateException(response.message ?: "리뷰 삭제에 실패했습니다.")
            }
        }
}

